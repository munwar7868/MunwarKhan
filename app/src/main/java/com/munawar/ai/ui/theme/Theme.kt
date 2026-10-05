package com.munawar.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MunawarPurple = Color(0xFF7B5CFA)
val MunawarPink = Color(0xFFFF5FA2)
val MunawarTeal = Color(0xFF35E6C4)
val MunawarBgDark = Color(0xFF0E0B1A)
val MunawarBgLight = Color(0xFFF7F5FF)

private val DarkColors = darkColorScheme(
    primary = MunawarPurple,
    secondary = MunawarPink,
    tertiary = MunawarTeal,
    background = MunawarBgDark,
    surface = Color(0xFF171331)
)

private val LightColors = lightColorScheme(
    primary = MunawarPurple,
    secondary = MunawarPink,
    tertiary = MunawarTeal,
    background = MunawarBgLight,
    surface = Color.White
)

@Composable
fun MunawarAITheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
