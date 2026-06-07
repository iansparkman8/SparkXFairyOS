package com.sparkx.fairyos.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

@Composable
fun HoloBackground(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "holo").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Subtle grid
        val gridColor = Color(0xFF2A1F4A)
        val spacing = 48f
        for (x in 0..(size.width / spacing).toInt()) {
            drawLine(gridColor, Offset(x * spacing, 0f), Offset(x * spacing, size.height), strokeWidth = 1f)
        }
        for (y in 0..(size.height / spacing).toInt()) {
            drawLine(gridColor, Offset(0f, y * spacing), Offset(size.width, y * spacing), strokeWidth = 1f)
        }

        // Floating particles
        for (i in 0..18) {
            val px = (size.width * ((i * 0.37f + phase * 0.2f) % 1f))
            val py = (size.height * ((i * 0.61f + phase * 0.15f + sin(i + phase).toFloat() * 0.1f) % 1f))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.15f), radius = 3f + sin(phase * 3 + i).toFloat() * 1.5f, center = Offset(px, py))
        }

        // Vignette edges
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF0D0B1A).copy(alpha = 0.6f)),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.minDimension * 0.85f
            )
        )
    }
}