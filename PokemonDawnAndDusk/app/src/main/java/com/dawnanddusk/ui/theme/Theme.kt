package com.dawnanddusk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dawn & Dusk Palette
val DawnGold = Color(0xFFFFB300)
val DawnAmber = Color(0xFFFF8F00)
val DawnSky = Color(0xFFFFE082)
val DuskPurple = Color(0xFF4A148C)
val DuskViolet = Color(0xFF7B1FA2)
val DuskMidnight = Color(0xFF1A103C)
val DeepNavy = Color(0xFF0D0826)
val PokéRed = Color(0xFFE53935)
val PokéWhite = Color(0xFFF5F5F5)
val EmeraldGreen = Color(0xFF2E7D32)
val CyanGlow = Color(0xFF00E5FF)

private val DarkColorScheme = darkColorScheme(
    primary = DawnGold,
    onPrimary = DeepNavy,
    primaryContainer = DuskViolet,
    onPrimaryContainer = Color.White,
    secondary = CyanGlow,
    onSecondary = DeepNavy,
    tertiary = PokéRed,
    background = DeepNavy,
    onBackground = Color.White,
    surface = DuskMidnight,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF281A54),
    onSurfaceVariant = Color(0xFFDDD0F5)
)

private val LightColorScheme = lightColorScheme(
    primary = DuskViolet,
    onPrimary = Color.White,
    primaryContainer = DawnSky,
    onPrimaryContainer = DuskMidnight,
    secondary = DawnAmber,
    onSecondary = DeepNavy,
    tertiary = PokéRed,
    background = Color(0xFFF7F5FC),
    onBackground = DuskMidnight,
    surface = Color.White,
    onSurface = DuskMidnight,
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = DuskMidnight
)

@Composable
fun PokemonDawnAndDuskTheme(
    darkTheme: Boolean = true, // Default to rich immersive dark Dawn & Dusk theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
