package com.sparkx.fairyos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkHoloColors = darkColorScheme(
    primary = Color(0xFF9C7BFF),
    secondary = Color(0xFF00E5FF),
    tertiary = Color(0xFFFF6EC7),
    background = Color(0xFF0D0B1A),
    surface = Color(0xFF1A1530),
    onPrimary = Color.White,
    onBackground = Color(0xFFE0D4FF)
)

@Composable
fun SparkXFairyOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkHoloColors,
        typography = Typography(),
        content = content
    )
}