package com.faceguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CAF7D),
    secondary = Color(0xFF2E7D5B),
    background = Color(0xFF0E1512),
    surface = Color(0xFF16201C),
    error = Color(0xFFCF6679)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    secondary = Color(0xFF4CAF7D),
    background = Color(0xFFF6FAF8),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E)
)

@Composable
fun FaceGuardTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
