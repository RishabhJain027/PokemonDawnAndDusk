package com.dawnanddusk.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.core.GeoPoint
import com.dawnanddusk.core.GeoUtils
import com.dawnanddusk.domain.model.Player
import com.dawnanddusk.domain.model.Spawn
import com.dawnanddusk.domain.usecase.EncounterValidationResult
import com.dawnanddusk.domain.usecase.GenerateSpawnsUseCase
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.domain.usecase.ValidateEncounterUseCase
import com.dawnanddusk.services.LocationService
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class MapUiState(
    val player: Player? = null,
    val currentLocation: GeoPoint = GeoPoint(35.6895, 139.6917),
    val activeSpawns: List<Spawn> = emptyList(),
    val isSimulatedMode: Boolean = false,
    val selectedSpawn: Spawn? = null,
    val distanceToSelectedMeters: Double = 0.0,
    val feedbackMessage: String? = null,
    val isLoading: Boolean = false
)

class MapViewModel(
    private val locationService: LocationService = DawnAndDuskApp.instance.locationService,
    private val generateSpawnsUseCase: GenerateSpawnsUseCase = DawnAndDuskApp.instance.generateSpawnsUseCase,
    private val validateEncounterUseCase: ValidateEncounterUseCase = DawnAndDuskApp.instance.validateEncounterUseCase,
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadPlayer()
        observeLocation()
        startPeriodicSpawnLoop()
    }

    private fun loadPlayer() {
        viewModelScope.launch {
            val player = getSessionUseCase()
            _uiState.value = _uiState.value.copy(player = player)
        }
    }

    private fun observeLocation() {
        locationService.startListening()
        viewModelScope.launch {
            locationService.currentLocation.collect { loc ->
                _uiState.value = _uiState.value.copy(currentLocation = loc)
                refreshSpawns(loc.latitude, loc.longitude)
            }
        }
        viewModelScope.launch {
            locationService.isSimulatedMode.collect { sim ->
                _uiState.value = _uiState.value.copy(isSimulatedMode = sim)
            }
        }
    }

    fun toggleSimulatedMode() {
        val next = !_uiState.value.isSimulatedMode
        locationService.setSimulatedMode(next)
    }

    fun moveJoystick(deltaXNorth: Double, deltaYEast: Double) {
        locationService.moveSimulated(deltaXNorth, deltaYEast)
    }

    fun refreshSpawns(lat: Double = _uiState.value.currentLocation.latitude, lon: Double = _uiState.value.currentLocation.longitude) {
        viewModelScope.launch {
            val spawns = generateSpawnsUseCase(lat, lon)
            _uiState.value = _uiState.value.copy(activeSpawns = spawns)
        }
    }

    private fun startPeriodicSpawnLoop() {
        viewModelScope.launch {
            while (true) {
                delay(45000L) // Refresh every 45s
                refreshSpawns()
            }
        }
    }

    fun onSpawnTapped(spawn: Spawn) {
        val curLoc = _uiState.value.currentLocation
        val dist = GeoUtils.calculateDistanceMeters(curLoc.latitude, curLoc.longitude, spawn.latitude, spawn.longitude)
        _uiState.value = _uiState.value.copy(
            selectedSpawn = spawn,
            distanceToSelectedMeters = dist
        )
    }

    fun dismissSelectedSpawn() {
        _uiState.value = _uiState.value.copy(selectedSpawn = null)
    }

    fun attemptEncounter(
        spawn: Spawn,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val loc = _uiState.value.currentLocation
            when (val res = validateEncounterUseCase(loc.latitude, loc.longitude, spawn.id)) {
                is EncounterValidationResult.Allowed -> {
                    DawnAndDuskApp.instance.audioService.playEncounterSound()
                    _uiState.value = _uiState.value.copy(selectedSpawn = null)
                    onSuccess(spawn.id)
                }
                EncounterValidationResult.TooFar -> {
                    val dist = GeoUtils.calculateDistanceMeters(loc.latitude, loc.longitude, spawn.latitude, spawn.longitude).toInt()
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "Too far! You are ${dist}m away. Move closer within 60m to start encounter."
                    )
                }
                EncounterValidationResult.Expired -> {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "The wild Pokémon has vanished!",
                        selectedSpawn = null
                    )
                    refreshSpawns()
                }
                EncounterValidationResult.NotFound -> {
                    _uiState.value = _uiState.value.copy(feedbackMessage = "Pokémon not found")
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}

@Composable
fun MapScreen(
    playerId: Long,
    viewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToEncounter: (String) -> Unit,
    onNavigateToPokedex: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // High-performance Canvas Map
        InteractiveMapCanvas(
            playerLocation = state.currentLocation,
            spawns = state.activeSpawns,
            selectedSpawn = state.selectedSpawn,
            onSpawnClicked = { viewModel.onSpawnTapped(it) }
        )

        // Top Status Bar (Trainer info & Mode Switch)
        TopMapBar(
            player = state.player,
            isSimulatedMode = state.isSimulatedMode,
            onToggleSimulated = { viewModel.toggleSimulatedMode() },
            onRefresh = { viewModel.refreshSpawns() }
        )

        // Feedback Banner
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp, start = 16.dp, end = 16.dp)
        ) {
            state.feedbackMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DuskViolet),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DawnGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearFeedback() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Virtual Walk Joystick (When Simulated Mode is active)
        if (state.isSimulatedMode) {
            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 100.dp),
                onMove = { dx, dy -> viewModel.moveJoystick(dx, dy) }
            )
        }

        // Spawn Encounter Popup (When a Pokémon marker is clicked)
        state.selectedSpawn?.let { spawn ->
            SpawnEncounterDialog(
                spawn = spawn,
                distanceMeters = state.distanceToSelectedMeters,
                canEncounter = state.distanceToSelectedMeters <= 60.0,
                onDismiss = { viewModel.dismissSelectedSpawn() },
                onStartEncounter = {
                    viewModel.attemptEncounter(spawn) { spawnId ->
                        onNavigateToEncounter(spawnId)
                    }
                }
            )
        }

        // Bottom Navigation Action Bar
        BottomActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            onPokedexClick = onNavigateToPokedex,
            onHistoryClick = onNavigateToHistory,
            onProfileClick = onNavigateToProfile
        )
    }
}

@Composable
fun InteractiveMapCanvas(
    playerLocation: GeoPoint,
    spawns: List<Spawn>,
    selectedSpawn: Spawn?,
    onSpawnClicked: (Spawn) -> Unit
) {
    var canvasCenter by remember { mutableStateOf(Offset.Zero) }
    // Scale: 1 meter = 1.8 pixels on screen
    val meterToPx = 1.8f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(spawns, playerLocation) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Check if clicked near any spawn
                        spawns.forEach { spawn ->
                            val dLat = spawn.latitude - playerLocation.latitude
                            val dLon = spawn.longitude - playerLocation.longitude
                            val northMeters = GeoUtils.metersToLatitudeDegrees(1.0).let { if (it > 0) dLat / it else 0.0 }
                            val eastMeters = GeoUtils.metersToLongitudeDegrees(1.0, playerLocation.latitude).let { if (it > 0) dLon / it else 0.0 }

                            val spawnScreenX = canvasCenter.x + (eastMeters * meterToPx).toFloat()
                            val spawnScreenY = canvasCenter.y - (northMeters * meterToPx).toFloat()

                            val distToTouch = Offset(spawnScreenX - offset.x, spawnScreenY - offset.y).getDistance()
                            if (distToTouch <= 45f) {
                                onSpawnClicked(spawn)
                            }
                        }
                    },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        canvasCenter = Offset(size.width / 2f, size.height / 2f)

        // Draw Map Grid / Pathways
        val gridSpacing = 80f
        for (x in 0..(size.width / gridSpacing).toInt() + 1) {
            drawLine(
                color = Color(0xFF1E1545),
                start = Offset(x * gridSpacing, 0f),
                end = Offset(x * gridSpacing, size.height),
                strokeWidth = 1.5f
            )
        }
        for (y in 0..(size.height / gridSpacing).toInt() + 1) {
            drawLine(
                color = Color(0xFF1E1545),
                start = Offset(0f, y * gridSpacing),
                end = Offset(size.width, y * gridSpacing),
                strokeWidth = 1.5f
            )
        }

        // Draw Interaction Radius Circle (60m)
        drawCircle(
            color = DawnGold.copy(alpha = 0.12f),
            radius = 60f * meterToPx,
            center = canvasCenter
        )
        drawCircle(
            color = DawnGold.copy(alpha = 0.5f),
            radius = 60f * meterToPx,
            center = canvasCenter,
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // Draw Spawns
        spawns.forEach { spawn ->
            val dLat = spawn.latitude - playerLocation.latitude
            val dLon = spawn.longitude - playerLocation.longitude
            val northMeters = GeoUtils.metersToLatitudeDegrees(1.0).let { if (it > 0) dLat / it else 0.0 }
            val eastMeters = GeoUtils.metersToLongitudeDegrees(1.0, playerLocation.latitude).let { if (it > 0) dLon / it else 0.0 }

            val spawnX = canvasCenter.x + (eastMeters * meterToPx).toFloat()
            val spawnY = canvasCenter.y - (northMeters * meterToPx).toFloat()

            val isSelected = spawn.id == selectedSpawn?.id
            val rarityColor = when (spawn.creature?.rarity?.code) {
                "L" -> Color(0xFFFFD700)
                "R" -> Color(0xFFFF4081)
                "I" -> Color(0xFF00E5FF)
                else -> Color(0xFF76FF03)
            }

            // Spawn beacon pulse
            drawCircle(
                color = rarityColor.copy(alpha = if (isSelected) 0.5f else 0.25f),
                radius = if (isSelected) 30f else 22f,
                center = Offset(spawnX, spawnY)
            )

            // Spawn core marker
            drawCircle(
                color = rarityColor,
                radius = if (isSelected) 14f else 10f,
                center = Offset(spawnX, spawnY)
            )

            // White center dot
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(spawnX, spawnY)
            )
        }

        // Draw Player Marker at center
        drawCircle(
            color = CyanGlow.copy(alpha = 0.3f),
            radius = 28f,
            center = canvasCenter
        )
        drawCircle(
            color = DawnGold,
            radius = 12f,
            center = canvasCenter
        )
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = canvasCenter
        )
    }
}

@Composable
fun TopMapBar(
    player: Player?,
    isSimulatedMode: Boolean,
    onToggleSimulated: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = DuskMidnight.copy(alpha = 0.92f),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DawnGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (player?.avatarGender == "F") "👒" else "🧢",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = player?.displayName ?: "Trainer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Lv. ${player?.level ?: 1}  •  ${player?.totalCaptures ?: 0} Caught",
                        style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                    )
                }
            }

            Row {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Spawns", tint = DawnGold)
                }
                FilledTonalButton(
                    onClick = onToggleSimulated,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSimulatedMode) DawnAmber else DuskViolet
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isSimulatedMode) "Joystick ON" else "GPS Mode",
                        fontSize = 12.sp,
                        color = if (isSimulatedMode) DeepNavy else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onMove: (Double, Double) -> Unit
) {
    val stepMeters = 8.0

    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = DuskMidnight.copy(alpha = 0.85f)),
        modifier = modifier.size(130.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Up / North
            IconButton(
                onClick = { onMove(stepMeters, 0.0) },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "North", tint = DawnGold)
            }

            // Down / South
            IconButton(
                onClick = { onMove(-stepMeters, 0.0) },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "South", tint = DawnGold)
            }

            // Left / West
            IconButton(
                onClick = { onMove(0.0, -stepMeters) },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "West", tint = DawnGold)
            }

            // Right / East
            IconButton(
                onClick = { onMove(0.0, stepMeters) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "East", tint = DawnGold)
            }

            // Center Dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DawnGold)
            )
        }
    }
}

@Composable
fun SpawnEncounterDialog(
    spawn: Spawn,
    distanceMeters: Double,
    canEncounter: Boolean,
    onDismiss: () -> Unit,
    onStartEncounter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DuskMidnight,
        title = {
            Text(
                text = "Wild ${spawn.creature?.name ?: "Pokémon"}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DawnGold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Rarity: ${spawn.creature?.rarity?.displayName ?: "Common"} (${spawn.creature?.primaryType?.displayName})",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Distance: ${distanceMeters.toInt()} meters away",
                    color = if (canEncounter) Color(0xFF76FF03) else Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!canEncounter) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Move closer (< 60m) to encounter!",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartEncounter,
                enabled = canEncounter,
                colors = ButtonDefaults.buttonColors(containerColor = DawnAmber)
            ) {
                Text(text = "ENCOUNTER", color = DeepNavy, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = DawnSky)
            }
        }
    )
}

@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    onPokedexClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = DuskMidnight.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = onPokedexClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Pokédex", tint = DawnGold)
                }
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PokéRed, DawnAmber))
                    )
                    .clickable { onHistoryClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = "Capture Map", tint = Color.White)
            }

            IconButton(onClick = onProfileClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Trainer Profile", tint = DawnGold)
                }
            }
        }
    }
}
