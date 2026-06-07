package com.sparkx.fairyos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7B68EE),      // Medium slate blue - holographic
    secondary = Color(0xFF00CED1),    // Dark turquoise
    tertiary = Color(0xFFFF69B4),     // Hot pink accent
    background = Color(0xFF0A0A12),
    surface = Color(0xFF1A1A2E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0FF),
    onSurface = Color(0xFFE0E0FF)
)

@Composable
fun SparkXFairyOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}