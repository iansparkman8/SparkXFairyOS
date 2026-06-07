package com.sparkx.fairyos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Adult SparkX visual identity.
 *
 * Black, gunmetal, silver, and electric blue.
 * Avoid toy-like purple/pink defaults.
 */
private val AdultNightColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),        // electric ice blue
    secondary = Color(0xFFBFC7D5),      // silver
    tertiary = Color(0xFF3B82F6),       // deep system blue
    background = Color(0xFF05070B),     // near black
    surface = Color(0xFF0D1118),        // gunmetal
    surfaceVariant = Color(0xFF151B24), // raised gunmetal
    onPrimary = Color(0xFF02050A),
    onSecondary = Color(0xFF05070B),
    onTertiary = Color.White,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFFBFC7D5),
    outline = Color(0xFF5E6A78),
    error = Color(0xFFFF6B6B)
)

@Composable
fun SparkXFairyOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AdultNightColors,
        typography = Typography(),
        content = content
    )
}