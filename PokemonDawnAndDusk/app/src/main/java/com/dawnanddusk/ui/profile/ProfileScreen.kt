package com.dawnanddusk.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawnanddusk.app.DawnAndDuskApp
import com.dawnanddusk.domain.model.Player
import com.dawnanddusk.domain.usecase.GetProfileStatsUseCase
import com.dawnanddusk.domain.usecase.GetSessionUseCase
import com.dawnanddusk.domain.usecase.LogoutUserUseCase
import com.dawnanddusk.domain.usecase.ProfileData
import com.dawnanddusk.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel(
    private val getSessionUseCase: GetSessionUseCase = DawnAndDuskApp.instance.getSessionUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase = DawnAndDuskApp.instance.getProfileStatsUseCase,
    private val logoutUserUseCase: LogoutUserUseCase = DawnAndDuskApp.instance.logoutUserUseCase
) : ViewModel() {

    private val _profileData = MutableStateFlow<ProfileData?>(null)
    val profileData: StateFlow<ProfileData?> = _profileData.asStateFlow()

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val player = getSessionUseCase()
            if (player != null) {
                val data = getProfileStatsUseCase(player.id)
                _profileData.value = data
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUserUseCase()
            _isLoggedOut.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val profileData by viewModel.profileData.collectAsState()
    val isLoggedOut by viewModel.isLoggedOut.collectAsState()

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainer Profile", fontWeight = FontWeight.Bold, color = DawnGold) },
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
        if (profileData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DawnGold)
            }
        } else {
            val player = profileData!!.player
            val stats = profileData!!.stats
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(DawnGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (player.avatarGender == "F") "👒" else "🧢",
                                fontSize = 48.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = player.displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = DawnGold
                            )
                        )
                        Text(
                            text = "@${player.username}  •  Joined ${dateFormat.format(Date(player.createdAt))}",
                            style = MaterialTheme.typography.bodySmall.copy(color = DawnSky)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Level Badge & XP
                        Surface(
                            color = DuskViolet,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRAINER LEVEL ${player.level}",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${player.xp} Total XP",
                                    color = DawnGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exploration Statistics
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DuskMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Capture Statistics", fontWeight = FontWeight.Bold, color = DawnGold)
                        Spacer(modifier = Modifier.height(12.dp))

                        StatRow("Total Pokémon Captured", "${stats.totalCaptures}")
                        StatRow("Unique Species Registered", "${stats.uniqueSpecies} / 151")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = PokéRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOGOUT", fontWeight = FontWeight.Bold, color = Color.White)
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.8f))
        Text(value, color = DawnGold, fontWeight = FontWeight.Bold)
    }
}
