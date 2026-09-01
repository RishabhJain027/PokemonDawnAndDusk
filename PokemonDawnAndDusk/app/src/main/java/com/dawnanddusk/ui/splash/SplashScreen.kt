package com.dawnanddusk.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashNavigationState {
    data object Loading : SplashNavigationState
    data class NavigateToMap(val playerId: Long) : SplashNavigationState
    data object NavigateToLogin : SplashNavigationState
}

class SplashViewModel(
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase
) : ViewModel() {

    private val _navState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navState = _navState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Give time for splash animation and database initialization
            delay(1600L)
            val activePlayer = getSessionUseCase()
            if (activePlayer != null) {
                _navState.value = SplashNavigationState.NavigateToMap(activePlayer.id)
            } else {
                _navState.value = SplashNavigationState.NavigateToLogin
            }
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToMap: (Long) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val navState by viewModel.navState.collectAsState()

    LaunchedEffect(navState) {
        when (val state = navState) {
            is SplashNavigationState.NavigateToMap -> onNavigateToMap(state.playerId)
            is SplashNavigationState.NavigateToLogin -> onNavigateToLogin()
            SplashNavigationState.Loading -> Unit
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PokeballRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DawnAmber.copy(alpha = 0.8f),
                        DuskPurple,
                        DuskMidnight
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Dawn & Dusk Poké Ball Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    DawnGold,
                                    DawnAmber,
                                    DuskViolet,
                                    DuskPurple,
                                    DawnGold
                                )
                            )
                        )
                )

                // Outer spinning ring
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(DeepNavy)
                )

                // Center core
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DawnGold)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "POKÉMON",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = DawnSky
                )
            )

            Text(
                text = "DAWN & DUSK",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Real-World Location Adventure",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = DawnGold,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
