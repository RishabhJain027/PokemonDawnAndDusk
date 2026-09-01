package com.dawnanddusk.ui.capture

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.core.AudioService
import com.dawnanddusk.domain.model.*
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.domain.usecase.ResolveCaptureUseCase
import com.dawnanddusk.services.LocationService
import com.dawnanddusk.services.SensorOrientation
import com.dawnanddusk.services.SensorService
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

sealed interface EncounterPhase {
    data object Aiming : EncounterPhase
    data class ThrowInFlight(val progress: Float, val endOffset: Offset) : EncounterPhase
    data class Shaking(val shakeCount: Int) : EncounterPhase
    data class Success(val creature: Creature, val message: String) : EncounterPhase
    data class Escape(val creature: Creature, val message: String) : EncounterPhase
    data class BrokeFree(val message: String) : EncounterPhase
}

data class EncounterUiState(
    val spawn: Spawn? = null,
    val creature: Creature? = null,
    val phase: EncounterPhase = EncounterPhase.Aiming,
    val pokeballsLeft: Int = 10,
    val sensorOrientation: SensorOrientation = SensorOrientation(),
    val isArMode: Boolean = true,
    val lastThrowResult: ThrowResult? = null
)

class EncounterViewModel(
    private val spawnId: String,
    private val sensorService: SensorService = DawnAndDuskApp.instance.sensorService,
    private val locationService: LocationService = DawnAndDuskApp.instance.locationService,
    private val resolveCaptureUseCase: ResolveCaptureUseCase = DawnAndDuskApp.instance.resolveCaptureUseCase,
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase,
    private val audioService: AudioService = DawnAndDuskApp.instance.audioService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncounterUiState())
    val uiState: StateFlow<EncounterUiState> = _uiState.asStateFlow()

    private var activeEncounter: Encounter? = null
    private var currentPlayerId: Long = 1L

    init {
        loadEncounter()
        observeSensors()
    }

    private fun loadEncounter() {
        viewModelScope.launch {
            val player = getSessionUseCase()
            if (player != null) {
                currentPlayerId = player.id
            }

            val spawn = DawnAndDuskApp.instance.spawnRepository.getSpawnById(spawnId)
            if (spawn != null && spawn.creature != null) {
                activeEncounter = Encounter(
                    id = java.util.UUID.randomUUID().toString(),
                    spawn = spawn,
                    creature = spawn.creature
                )
                _uiState.value = _uiState.value.copy(
                    spawn = spawn,
                    creature = spawn.creature,
                    phase = EncounterPhase.Aiming
                )
            }
        }
    }

    private fun observeSensors() {
        sensorService.startListening()
        viewModelScope.launch {
            sensorService.orientation.collect { orientation ->
                _uiState.value = _uiState.value.copy(sensorOrientation = orientation)
            }
        }
    }

    fun onThrowReleased(
        startOffset: Offset,
        endOffset: Offset,
        durationMs: Long,
        targetCenter: Offset,
        targetRadius: Float,
        timingScore: Float
    ) {
        val encounter = activeEncounter ?: return
        if (_uiState.value.phase !is EncounterPhase.Aiming || _uiState.value.pokeballsLeft <= 0) return

        val dx = endOffset.x - startOffset.x
        val dy = endOffset.y - startOffset.y
        val duration = max(50L, durationMs)

        // Calculate velocity
        val velocityX = dx / duration
        val velocityY = dy / duration

        // Check distance from landing point to creature target center
        val distToTarget = (endOffset - targetCenter).getDistance()
        val hitAccuracy = max(0f, 1f - (distToTarget / (targetRadius * 1.8f)))

        val throwResult = ThrowResult(
            velocityX = velocityX,
            velocityY = velocityY,
            durationMs = duration,
            hitAccuracy = hitAccuracy,
            timingBonus = timingScore
        )

        val newBalls = _uiState.value.pokeballsLeft - 1
        _uiState.value = _uiState.value.copy(
            pokeballsLeft = newBalls,
            phase = EncounterPhase.ThrowInFlight(0f, endOffset),
            lastThrowResult = throwResult
        )

        audioService.playThrowSound()

        // Simulate flight animation then resolve
        viewModelScope.launch {
            // Animate flight
            for (step in 1..10) {
                delay(35L)
                _uiState.value = _uiState.value.copy(
                    phase = EncounterPhase.ThrowInFlight(step / 10f, endOffset)
                )
            }

            // If miss
            if (hitAccuracy < 0.15f) {
                delay(400L)
                _uiState.value = _uiState.value.copy(phase = EncounterPhase.BrokeFree("Missed the target!"))
                delay(1000L)
                _uiState.value = _uiState.value.copy(phase = EncounterPhase.Aiming)
                return@launch
            }

            // Simulate ball capture shaking
            for (shake in 1..3) {
                audioService.playBallShakeSound()
                _uiState.value = _uiState.value.copy(phase = EncounterPhase.Shaking(shake))
                delay(700L)
            }

            // Resolve encounter outcome
            val curLoc = locationService.currentLocation.value
            val resolution = resolveCaptureUseCase(
                playerId = currentPlayerId,
                encounter = encounter,
                throwResult = throwResult,
                playerLatitude = curLoc.latitude,
                playerLongitude = curLoc.longitude
            )

            when (resolution.outcome) {
                CaptureOutcome.CAPTURED -> {
                    audioService.playCaptureSuccessSound()
                    _uiState.value = _uiState.value.copy(
                        phase = EncounterPhase.Success(encounter.creature, resolution.message)
                    )
                }
                CaptureOutcome.ESCAPED -> {
                    audioService.playEscapeSound()
                    _uiState.value = _uiState.value.copy(
                        phase = EncounterPhase.Escape(encounter.creature, resolution.message)
                    )
                }
                CaptureOutcome.BROKE_FREE, CaptureOutcome.MISSED -> {
                    audioService.playEscapeSound()
                    _uiState.value = _uiState.value.copy(
                        phase = EncounterPhase.BrokeFree(resolution.message)
                    )
                    delay(1200L)
                    _uiState.value = _uiState.value.copy(phase = EncounterPhase.Aiming)
                }
            }
        }
    }

    fun onPanManual(dPitch: Float, dRoll: Float) {
        sensorService.applyManualPan(dPitch, dRoll)
    }

    override fun onCleared() {
        super.onCleared()
        sensorService.stopListening()
    }
}

@Composable
fun EncounterScreen(
    spawnId: String,
    viewModel: EncounterViewModel = androidx.lifecycle.viewmodel.compose.viewModel { EncounterViewModel(spawnId) },
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val creature = state.creature

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DawnAmber.copy(alpha = 0.5f),
                        DuskMidnight,
                        DeepNavy
                    )
                )
            )
    ) {
        if (creature != null) {
            // Interactive Projectile Physics and Gyroscope/Touch AR Area
            EncounterPlayground(
                creature = creature,
                phase = state.phase,
                sensorOrientation = state.sensorOrientation,
                onThrow = { start, end, duration, target, radius, timing ->
                    viewModel.onThrowReleased(start, end, duration, target, radius, timing)
                },
                onManualPan = { dp, dr -> viewModel.onPanManual(dp, dr) }
            )

            // Top Status Bar: Creature Name, CP, Rarity, Back button
            EncounterTopBar(
                creature = creature,
                pokeballsLeft = state.pokeballsLeft,
                onBack = onFinished
            )

            // Bottom Pokeball Counter
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PokéRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Poké Balls: x${state.pokeballsLeft}",
                            color = DawnGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Outcome Dialogs
            when (val phase = state.phase) {
                is EncounterPhase.Success -> {
                    CaptureSuccessDialog(
                        creature = phase.creature,
                        message = phase.message,
                        onContinue = onFinished
                    )
                }
                is EncounterPhase.Escape -> {
                    CaptureFleeDialog(
                        creature = phase.creature,
                        message = phase.message,
                        onContinue = onFinished
                    )
                }
                is EncounterPhase.BrokeFree -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DuskViolet.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = phase.message,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                else -> Unit
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DawnGold
            )
        }
    }
}

@Composable
fun EncounterTopBar(
    creature: Creature,
    pokeballsLeft: Int,
    onBack: () -> Unit
) {
    Surface(
        color = DuskMidnight.copy(alpha = 0.85f),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Run Away", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = creature.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = DawnGold
                    )
                )
                Text(
                    text = "${creature.category} Pokémon  •  HP ${creature.hp}",
                    style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                )
            }

            SuggestionChip(
                onClick = {},
                label = { Text(creature.rarity.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = when (creature.rarity) {
                        Rarity.LEGENDARY -> Color(0xFFFFD700)
                        Rarity.RARE -> Color(0xFFFF4081)
                        Rarity.UNCOMMON -> Color(0xFF00E5FF)
                        Rarity.COMMON -> Color(0xFF76FF03)
                    },
                    labelColor = DeepNavy
                )
            )
        }
    }
}

@Composable
fun EncounterPlayground(
    creature: Creature,
    phase: EncounterPhase,
    sensorOrientation: SensorOrientation,
    onThrow: (Offset, Offset, Long, Offset, Float, Float) -> Unit,
    onManualPan: (Float, Float) -> Unit
) {
    var touchStart by remember { mutableStateOf(Offset.Zero) }
    var touchStartTime by remember { mutableLongStateOf(0L) }
    var currentTouch by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Contracting Ring Animation (Timing score: 1.0 when contracted, 0.0 when expanded)
    val infiniteTransition = rememberInfiniteTransition(label = "RingContract")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimingRing"
    )

    // Ball Shake Animation during Shaking phase
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BallShake"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(phase) {
                if (phase is EncounterPhase.Aiming) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchStart = offset
                            currentTouch = offset
                            touchStartTime = System.currentTimeMillis()
                            isDragging = true
                        },
                        onDrag = { change, _ ->
                            currentTouch = change.position
                        },
                        onDragEnd = {
                            val duration = System.currentTimeMillis() - touchStartTime
                            val targetCenter = Offset(size.width / 2f + (sensorOrientation.roll * 4f), size.height * 0.42f - (sensorOrientation.pitch * 4f))
                            val targetRadius = 70f
                            val timingScore = 1f - ringScale

                            onThrow(touchStart, currentTouch, duration, targetCenter, targetRadius, timingScore)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Creature target center adjusted by Gyroscope Orientation
            val targetX = canvasW / 2f + (sensorOrientation.roll * 4f)
            val targetY = canvasH * 0.42f - (sensorOrientation.pitch * 4f)
            val creatureTarget = Offset(targetX, targetY)
            val creatureRadius = 65f

            // Draw Target Contracting Aiming Ring
            if (phase is EncounterPhase.Aiming) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = creatureRadius + 20f,
                    center = creatureTarget,
                    style = Stroke(width = 3f)
                )

                val ringColor = when {
                    ringScale < 0.4f -> Color(0xFF76FF03) // Excellent
                    ringScale < 0.7f -> DawnGold          // Great
                    else -> Color(0xFFFF5252)             // Nice
                }

                drawCircle(
                    color = ringColor,
                    radius = (creatureRadius + 20f) * ringScale,
                    center = creatureTarget,
                    style = Stroke(width = 4f)
                )
            }

            // Render Creature Placeholder / Sprite
            if (phase !is EncounterPhase.Shaking) {
                // Creature base aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(creature.primaryType.colorHex).copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    radius = creatureRadius * 1.5f,
                    center = creatureTarget
                )

                // Creature Core Body
                drawCircle(
                    color = Color(creature.primaryType.colorHex),
                    radius = creatureRadius,
                    center = creatureTarget
                )

                // Inner features
                drawCircle(
                    color = Color.White,
                    radius = creatureRadius * 0.25f,
                    center = Offset(creatureTarget.x - creatureRadius * 0.35f, creatureTarget.y - creatureRadius * 0.2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = creatureRadius * 0.25f,
                    center = Offset(creatureTarget.x + creatureRadius * 0.35f, creatureTarget.y - creatureRadius * 0.2f)
                )
            }

            // Render Ball in flight or in shake
            val ballOrigin = Offset(canvasW / 2f, canvasH * 0.84f)

            when (phase) {
                is EncounterPhase.Aiming -> {
                    val ballPos = if (isDragging) currentTouch else ballOrigin
                    drawPokeball(ballPos, 32f, 0f)
                }
                is EncounterPhase.ThrowInFlight -> {
                    val currentPos = Offset(
                        ballOrigin.x + (phase.endOffset.x - ballOrigin.x) * phase.progress,
                        ballOrigin.y + (phase.endOffset.y - ballOrigin.y) * phase.progress - (150f * sin(phase.progress * Math.PI).toFloat())
                    )
                    val ballScale = 32f * (1f - phase.progress * 0.35f)
                    drawPokeball(currentPos, ballScale, phase.progress * 720f)
                }
                is EncounterPhase.Shaking -> {
                    drawPokeball(creatureTarget, 24f, shakeRotation)
                }
                else -> Unit
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPokeball(
    center: Offset,
    radius: Float,
    rotationDeg: Float
) {
    // Top half Red
    drawArc(
        color = PokéRed,
        startAngle = 180f + rotationDeg,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Bottom half White
    drawArc(
        color = PokéWhite,
        startAngle = 0f + rotationDeg,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )

    // Center Black Divider Band
    drawLine(
        color = DeepNavy,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = radius * 0.2f
    )

    // Center Button Outer
    drawCircle(
        color = DeepNavy,
        radius = radius * 0.35f,
        center = center
    )

    // Center Button Core
    drawCircle(
        color = Color.White,
        radius = radius * 0.22f,
        center = center
    )
}

@Composable
fun CaptureSuccessDialog(
    creature: Creature,
    message: String,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        containerColor = DuskMidnight,
        title = {
            Text(
                text = "🎉 Gotcha!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = DawnGold
                )
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${creature.name} was successfully captured and added to your Pokédex!",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "+100 Trainer XP",
                    color = Color(0xFF76FF03),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = DawnAmber)
            ) {
                Text("RETURN TO MAP", color = DeepNavy, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CaptureFleeDialog(
    creature: Creature,
    message: String,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        containerColor = DuskMidnight,
        title = {
            Text(
                text = "💨 Oh no!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PokéRed
                )
            )
        },
        text = {
            Text(
                text = "${creature.name} fled into the wild!",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
            )
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = DuskViolet)
            ) {
                Text("RETURN TO MAP", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
