package com.sparkx.fairyos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object SparkXColors {
    val SpaceBlack = Color(0xFF070A12)
    val DeepNavy = Color(0xFF0D1324)
    val Panel = Color(0xDD12182A)
    val PanelSoft = Color(0xAA18213A)

    val Cyan = Color(0xFF39D7FF)
    val Mint = Color(0xFF7CF7D4)
    val Violet = Color(0xFF9C7CFF)
    val Gold = Color(0xFFFFD36E)

    val TextPrimary = Color(0xFFF4F7FF)
    val TextSecondary = Color(0xFFB7C2E6)
    val TextMuted = Color(0xFF7F8DB8)
}

fun sparkBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            SparkXColors.SpaceBlack,
            SparkXColors.DeepNavy,
            Color(0xFF101227)
        )
    )
}

fun sparkAccentBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            SparkXColors.Cyan,
            SparkXColors.Mint,
            SparkXColors.Violet
        )
    )
}

@Composable
fun SparkGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SparkXColors.Panel),
        border = BorderStroke(1.dp, Color(0x5539D7FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun SparkHeroPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3339D7FF),
                        Color(0x2212182A),
                        Color(0x0012182A)
                    )
                ),
                shape = RoundedCornerShape(34.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SparkSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = SparkXColors.TextPrimary
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SparkXColors.TextSecondary
            )
        }
    }
}

@Composable
fun SparkPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = Color(0x2219E6FF),
        border = BorderStroke(1.dp, Color(0x5539D7FF))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            text = text,
            color = SparkXColors.TextPrimary
        )
    }
}