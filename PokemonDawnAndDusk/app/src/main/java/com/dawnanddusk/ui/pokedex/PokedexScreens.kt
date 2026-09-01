package com.dawnanddusk.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.domain.model.Capture
import com.dawnanddusk.domain.model.Creature
import com.dawnanddusk.domain.model.CreatureType
import com.dawnanddusk.domain.usecase.GetCaptureHistoryUseCase
import com.dawnanddusk.domain.usecase.GetPokedexUseCase
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.domain.usecase.PokedexEntry
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PokedexUiState(
    val entries: List<PokedexEntry> = emptyList(),
    val filteredEntries: List<PokedexEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: CreatureType? = null,
    val totalCapturedSpecies: Int = 0,
    val totalSpeciesCount: Int = 151,
    val isLoading: Boolean = true
)

class PokedexViewModel(
    private val getPokedexUseCase: GetPokedexUseCase = DawnAndDuskApp.instance.getPokedexUseCase,
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokedexUiState())
    val uiState: StateFlow<PokedexUiState> = _uiState.asStateFlow()

    init {
        loadPokedex()
    }

    fun loadPokedex() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val player = getSessionUseCase()
            val playerId = player?.id ?: 1L
            val entries = getPokedexUseCase(playerId)
            val capturedCount = entries.count { it.isCaptured }

            _uiState.value = _uiState.value.copy(
                entries = entries,
                filteredEntries = entries,
                totalCapturedSpecies = capturedCount,
                totalSpeciesCount = entries.size,
                isLoading = false
            )
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onTypeFilterSelected(type: CreatureType?) {
        _uiState.value = _uiState.value.copy(selectedTypeFilter = type)
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        val filtered = current.entries.filter { entry ->
            val matchesQuery = current.searchQuery.isBlank() ||
                    entry.creature.name.contains(current.searchQuery, ignoreCase = true) ||
                    entry.creature.id.toString() == current.searchQuery.trim()

            val matchesType = current.selectedTypeFilter == null ||
                    entry.creature.primaryType == current.selectedTypeFilter ||
                    entry.creature.secondaryType == current.selectedTypeFilter

            matchesQuery && matchesType
        }
        _uiState.value = current.copy(filteredEntries = filtered)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(
    viewModel: PokedexViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onCreatureClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pokédex", fontWeight = FontWeight.Bold, color = DawnGold)
                        Text(
                            "Caught: ${state.totalCapturedSpecies} / ${state.totalSpeciesCount}",
                            style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DuskMidnight)
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search & Progress
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search Pokémon by name or #ID") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DawnGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DawnGold,
                        unfocusedBorderColor = Color(0xFF3B2A70)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { if (state.totalSpeciesCount > 0) state.totalCapturedSpecies.toFloat() / state.totalSpeciesCount else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = DawnGold,
                    trackColor = DuskMidnight
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DawnGold)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredEntries, key = { it.creature.id }) { entry ->
                        PokedexGridCard(
                            entry = entry,
                            onClick = {
                                if (entry.isCaptured) {
                                    onCreatureClick(entry.creature.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PokedexGridCard(
    entry: PokedexEntry,
    onClick: () -> Unit
) {
    val creature = entry.creature
    val typeColor = Color(creature.primaryType.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCaptured) DuskMidnight else Color(0xFF140D2E)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isCaptured, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#%03d".format(creature.id),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (entry.isCaptured) DawnSky else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Creature Icon Placeholder / Circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.isCaptured) typeColor.copy(alpha = 0.25f) else Color(0xFF22164A)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (entry.isCaptured) {
                    Text(
                        text = "🐾",
                        fontSize = 24.sp
                    )
                } else {
                    Text(
                        text = "?",
                        fontSize = 26.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (entry.isCaptured) creature.name else "???",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isCaptured) Color.White else Color.White.copy(alpha = 0.4f)
                ),
                maxLines = 1
            )

            if (entry.isCaptured && entry.captureCount > 0) {
                Text(
                    text = "x${entry.captureCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DawnGold,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatureDetailScreen(
    creatureId: Int,
    onNavigateBack: () -> Unit
) {
    var creature by remember { mutableStateOf<Creature?>(null) }
    var captures by remember { mutableStateOf<List<Capture>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(creatureId) {
        val app = DawnAndDuskApp.instance
        creature = app.creatureRepository.getCreatureById(creatureId)
        val player = app.sessionRepository.getActivePlayerId() ?: 1L
        captures = app.captureRepository.getCapturesForPlayer(player).filter { it.creatureId == creatureId }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(creature?.name ?: "Pokémon Details", fontWeight = FontWeight.Bold, color = DawnGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DuskMidnight)
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        if (isLoading || creature == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DawnGold)
            }
        } else {
            val c = creature!!
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Main Creature Card Header
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "#%03d ${c.name.uppercase()}".format(c.id),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = DawnGold
                            )
                        )
                        Text(
                            text = "${c.category} Pokémon",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DawnSky)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Type Badges
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(c.primaryType.displayName, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(c.primaryType.colorHex),
                                    labelColor = Color.White
                                )
                            )
                            c.secondaryType?.let { sec ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(sec.displayName, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color(sec.colorHex),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.9f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Breakdown
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Attributes & Base Stats", fontWeight = FontWeight.Bold, color = DawnGold)
                        Spacer(modifier = Modifier.height(12.dp))

                        StatRow("Height", "${c.heightMeters} m")
                        StatRow("Weight", "${c.weightKg} kg")
                        StatRow("HP", "${c.hp}")
                        StatRow("Attack", "${c.attack}")
                        StatRow("Defense", "${c.defense}")
                        StatRow("Speed", "${c.speed}")
                        StatRow("Rarity", c.rarity.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Capture History
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Capture History (${captures.size} Caught)", fontWeight = FontWeight.Bold, color = DawnGold)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (captures.isEmpty()) {
                            Text("No captures recorded yet.", color = Color.White.copy(alpha = 0.6f))
                        } else {
                            captures.forEach { cap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(cap.capturedAt)),
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                    )
                                    Text(
                                        text = "Lat: %.4f, Lon: %.4f".format(cap.latitude, cap.longitude),
                                        style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
    }
}
