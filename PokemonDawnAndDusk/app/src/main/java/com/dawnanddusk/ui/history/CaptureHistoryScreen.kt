package com.dawnanddusk.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.core.GeoUtils
import com.dawnanddusk.domain.model.Capture
import com.dawnanddusk.domain.usecase.GetCaptureHistoryUseCase
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HistoryUiState(
    val captures: List<Capture> = emptyList(),
    val selectedCapture: Capture? = null,
    val isLoading: Boolean = true
)

class CaptureHistoryViewModel(
    private val getCaptureHistoryUseCase: GetCaptureHistoryUseCase = DawnAndDuskApp.instance.getCaptureHistoryUseCase,
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val player = getSessionUseCase()
            val playerId = player?.id ?: 1L
            val list = getCaptureHistoryUseCase(playerId)
            _uiState.value = _uiState.value.copy(
                captures = list,
                selectedCapture = list.firstOrNull(),
                isLoading = false
            )
        }
    }

    fun onSelectCapture(capture: Capture) {
        _uiState.value = _uiState.value.copy(selectedCapture = capture)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureHistoryScreen(
    viewModel: CaptureHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture History Map", fontWeight = FontWeight.Bold, color = DawnGold) },
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
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DawnGold)
            }
        } else if (state.captures.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No captures recorded yet.", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Explore the map and catch Pokémon to view your history here!", color = DawnSky, fontSize = 13.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Interactive Radar History Canvas (Top Half)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(16.dp)
                ) {
                    HistoryMapCanvas(
                        captures = state.captures,
                        selectedCapture = state.selectedCapture
                    )
                }

                Text(
                    text = "All Captures (${state.captures.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DawnGold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // List of Past Captures (Bottom Half)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.captures, key = { it.id }) { cap ->
                        val isSelected = cap.id == state.selectedCapture?.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DuskViolet else DuskMidnight
                            ),
                            onClick = { viewModel.onSelectCapture(cap) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(DawnGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cap.creature?.name ?: "Pokémon #${cap.creatureId}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = dateFormat.format(Date(cap.capturedAt)),
                                        style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                                    )
                                }
                                Text(
                                    text = "Lat: %.3f\nLon: %.3f".format(cap.latitude, cap.longitude),
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryMapCanvas(
    captures: List<Capture>,
    selectedCapture: Capture?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw radar rings
        drawCircle(color = Color(0xFF281A54), radius = size.width * 0.45f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        drawCircle(color = Color(0xFF281A54), radius = size.width * 0.30f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        drawCircle(color = Color(0xFF281A54), radius = size.width * 0.15f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

        // Center Origin Marker
        drawCircle(color = CyanGlow, radius = 6f, center = center)

        val reference = selectedCapture ?: captures.firstOrNull()
        if (reference != null) {
            captures.forEach { cap ->
                val dLat = cap.latitude - reference.latitude
                val dLon = cap.longitude - reference.longitude
                val northMeters = GeoUtils.metersToLatitudeDegrees(1.0).let { if (it > 0) dLat / it else 0.0 }
                val eastMeters = GeoUtils.metersToLongitudeDegrees(1.0, reference.latitude).let { if (it > 0) dLon / it else 0.0 }

                val px = center.x + (eastMeters * 0.8f).toFloat()
                val py = center.y - (northMeters * 0.8f).toFloat()

                val isSelected = cap.id == selectedCapture?.id
                drawCircle(
                    color = if (isSelected) PokéRed else DawnGold,
                    radius = if (isSelected) 10f else 6f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
