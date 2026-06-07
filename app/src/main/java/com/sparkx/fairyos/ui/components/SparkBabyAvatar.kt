package com.sparkx.fairyos.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.sparkx.fairyos.domain.mood.SparkMood
import kotlin.math.*

@Composable
fun SparkBabyAvatar(
    mood: SparkMood,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fairy")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 180 else 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "speaking"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = size.toPx() / 2
        val cy = size.toPx() / 2 + 20f
        val s = size.toPx() * 0.38f

        // Aura
        val auraR = s * (if (isSpeaking) 1.35f else 1.15f) + sin(phase * 0.8f).toFloat() * 12f
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = if (isSpeaking) 0.35f else 0.18f),
            radius = auraR,
            center = Offset(cx, cy)
        )

        // Wings flap
        val flap = if (isSpeaking || mood == SparkMood.HAPPY) sin(speakingPhase * 1.8f).toFloat() * 0.25f else sin(phase * 0.6f).toFloat() * 0.12f
        drawWing(this, cx - s * 0.3f, cy, s, flap, left = true)
        drawWing(this, cx + s * 0.3f, cy, s, flap, left = false)

        // Ethereal body
        drawOval(
            color = Color(0xFF7B4C9A),
            topLeft = Offset(cx - s * 0.28f, cy - s * 0.1f),
            size = androidx.compose.ui.geometry.Size(s * 0.56f, s * 0.7f)
        )

        // Head
        val headY = cy - s * 0.35f
        drawCircle(color = Color(0xFFFFE8D0), radius = s * 0.32f, center = Offset(cx, headY))

        // Crown
        val crownColor = if (isSpeaking) Color(0xFFFFD700) else Color(0xFFB39DDB)
        drawCircle(color = crownColor, radius = s * 0.14f, center = Offset(cx, headY - s * 0.28f))
        // Crown spikes
        for (i in 0 until 5) {
            val ang = (i * 72 - 90) * PI / 180f
            val px = cx + cos(ang).toFloat() * s * 0.22f
            val py = headY - s * 0.28f + sin(ang).toFloat() * s * 0.1f
            drawLine(crownColor, Offset(cx, headY - s * 0.28f), Offset(px, py - 12f), strokeWidth = 5f)
        }

        // Eyes
        val eyeY = headY - s * 0.08f
        val eyeDist = s * 0.15f
        when (mood) {
            SparkMood.SLEEPY -> {
                drawLine(Color.Black, Offset(cx - eyeDist, eyeY), Offset(cx - eyeDist + s*0.12f, eyeY), strokeWidth = 6f)
                drawLine(Color.Black, Offset(cx + eyeDist - s*0.12f, eyeY), Offset(cx + eyeDist, eyeY), strokeWidth = 6f)
            }
            SparkMood.ALERT -> {
                drawCircle(Color.Black, radius = s * 0.09f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.09f, center = Offset(cx + eyeDist, eyeY))
            }
            else -> {
                drawCircle(Color.Black, radius = s * 0.07f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.07f, center = Offset(cx + eyeDist, eyeY))
            }
        }

        // Mouth animation when speaking
        val mouthOpen = if (isSpeaking) (sin(speakingPhase * 4f).toFloat() * 0.5f + 0.5f) * s * 0.1f else s * 0.025f
        drawOval(
            color = Color(0xFF5D2E2E),
            topLeft = Offset(cx - s * 0.1f, headY + s * 0.08f - mouthOpen / 2),
            size = androidx.compose.ui.geometry.Size(s * 0.2f, mouthOpen)
        )

        // Orbiting particles
        if (isSpeaking || mood == SparkMood.THINKING) {
            for (i in 0..6) {
                val ang = phase * 1.2f + i * 0.9f
                val r = s * 0.55f + sin(phase + i).toFloat() * 8f
                val px = cx + cos(ang).toFloat() * r
                val py = cy + sin(ang).toFloat() * r * 0.7f
                drawCircle(Color(0xFF7CF8FF), radius = 5f, center = Offset(px, py))
            }
        }

        // Core pulse
        val pulse = if (isSpeaking) sin(speakingPhase * 3.5f).toFloat() * 0.2f + 0.9f else sin(phase * 0.7f).toFloat() * 0.12f + 0.88f
        drawCircle(
            color = Color(0xFFFF6EC7).copy(alpha = 0.6f * pulse),
            radius = s * 0.15f * pulse,
            center = Offset(cx, cy + s * 0.08f)
        )
    }
}

private fun drawWing(scope: DrawScope, x: Float, y: Float, s: Float, flap: Float, left: Boolean) {
    val sign = if (left) -1f else 1f
    val path = Path().apply {
        moveTo(x, y)
        quadraticTo(
            x + sign * s * 0.6f, y - s * (0.4f + flap),
            x + sign * s * 0.25f, y + s * 0.15f
        )
        close()
    }
    scope.drawPath(path, color = Color(0xFF9EC8FF).copy(alpha = 0.75f))
}