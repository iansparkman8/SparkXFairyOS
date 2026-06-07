package com.sparkx.fairyos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared SparkX glass palette.
 *
 * Product direction:
 * black / silver / electric blue / adult premium.
 */
object SparkGlass {
    val Black = Color(0xFF05070B)
    val BlackElevated = Color(0xFF0D1118)
    val Gunmetal = Color(0xFF151B24)

    val Silver = Color(0xFFBFC7D5)
    val SilverSoft = Color(0xFF8B95A5)
    val Chrome = Color(0xFFE5E7EB)

    val Blue = Color(0xFF3B82F6)
    val IceBlue = Color(0xFF7DD3FC)
    val NeonBlue = Color(0xFF00B8FF)

    val Panel = Color(0xFF101722).copy(alpha = 0.86f)
    val PanelStrong = Color(0xFF151D2A).copy(alpha = 0.92f)
    val Stroke = Color(0xFF7DD3FC).copy(alpha = 0.34f)
    val StrokeHot = Color(0xFFBFC7D5).copy(alpha = 0.42f)

    val Cyan = IceBlue
    val Pink = Silver
    val Violet = Blue
    val Gold = Silver
    val Green = Color(0xFF6EE7B7)
    val Danger = Color(0xFFFF6B6B)
    val Mars = Color(0xFF94A3B8)

    val BackgroundDark = Black
    val TextPrimary = Chrome
    val TextSecondary = Silver
}