package com.dawnanddusk.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dawnanddusk.ui.auth.LoginScreen
import com.dawnanddusk.ui.auth.RegisterScreen
import com.dawnanddusk.ui.capture.EncounterScreen
import com.dawnanddusk.ui.history.CaptureHistoryScreen
import com.dawnanddusk.ui.map.MapScreen
import com.dawnanddusk.ui.pokedex.CreatureDetailScreen
import com.dawnanddusk.ui.pokedex.PokedexScreen
import com.dawnanddusk.ui.profile.ProfileScreen
import com.dawnanddusk.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Map : Screen("map/{playerId}") {
        fun createRoute(playerId: Long) = "map/$playerId"
    }
    data object Encounter : Screen("encounter/{spawnId}") {
        fun createRoute(spawnId: String) = "encounter/$spawnId"
    }
    data object Pokedex : Screen("pokedex")
    data object CreatureDetail : Screen("creature_detail/{creatureId}") {
        fun createRoute(creatureId: Int) = "creature_detail/$creatureId"
    }
    data object Profile : Screen("profile")
    data object History : Screen("history")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToMap = { playerId ->
                    navController.navigate(Screen.Map.createRoute(playerId)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { playerId ->
                    navController.navigate(Screen.Map.createRoute(playerId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { playerId ->
                    navController.navigate(Screen.Map.createRoute(playerId)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Map.route,
            arguments = listOf(navArgument("playerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: 1L
            MapScreen(
                playerId = playerId,
                onNavigateToEncounter = { spawnId ->
                    navController.navigate(Screen.Encounter.createRoute(spawnId))
                },
                onNavigateToPokedex = {
                    navController.navigate(Screen.Pokedex.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        composable(
            route = Screen.Encounter.route,
            arguments = listOf(navArgument("spawnId") { type = NavType.StringType })
        ) { backStackEntry ->
            val spawnId = backStackEntry.arguments?.getString("spawnId") ?: ""
            EncounterScreen(
                spawnId = spawnId,
                onFinished = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Pokedex.route) {
            PokedexScreen(
                onCreatureClick = { creatureId ->
                    navController.navigate(Screen.CreatureDetail.createRoute(creatureId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.CreatureDetail.route,
            arguments = listOf(navArgument("creatureId") { type = NavType.IntType })
        ) { backStackEntry ->
            val creatureId = backStackEntry.arguments?.getInt("creatureId") ?: 1
            CreatureDetailScreen(
                creatureId = creatureId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.History.route) {
            CaptureHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
