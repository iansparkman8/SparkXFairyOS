package com.sparkx.fairyos.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sparkx.fairyos.FairyForm
import com.sparkx.fairyos.palette
import com.sparkx.fairyos.domain.mood.SparkMood
import kotlin.math.*

@Composable
fun SparkBabyAvatar(
    mood: SparkMood,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    reactionKey: Int = 0,
    visualIntensity: Float = 1f,
    form: FairyForm = FairyForm.Androgynous
) {
    val fairyPalette = form.palette()

    val infiniteTransition = rememberInfiniteTransition(label = "fairy")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 160 else 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaking"
    )

    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )
    val isBlinking = blinkPhase > 0.93f && mood != SparkMood.ALERT && mood != SparkMood.SLEEPY

    val reactionScale by animateFloatAsState(
        targetValue = if (reactionKey % 2 == 0) 1f else 1.04f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "reaction"
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = size.toPx() / 2
        val baseCy = size.toPx() / 2 + 18f
        val bob = sin(phase * 0.85f).toFloat() * 6f * visualIntensity
        val cy = baseCy + bob

        val s = size.toPx() * 0.38f * reactionScale

        // Aura with form color
        val auraR = s * (if (isSpeaking) 1.38f else 1.18f) + sin(phase * 0.7f).toFloat() * 14f
        drawCircle(
            color = fairyPalette.aura.copy(alpha = if (isSpeaking) 0.32f else 0.16f),
            radius = auraR,
            center = Offset(cx, cy)
        )

        // Wings with form palette
        val flap = if (isSpeaking || mood == SparkMood.HAPPY) sin(speakingPhase * 1.9f).toFloat() * 0.28f else sin(phase * 0.55f).toFloat() * 0.11f
        drawWing(this, cx - s * 0.32f, cy, s, flap, left = true, palette = fairyPalette)
        drawWing(this, cx + s * 0.32f, cy, s, flap, left = false, palette = fairyPalette)

        // Body
        drawOval(
            color = fairyPalette.shadow,
            topLeft = Offset(cx - s * 0.29f, cy - s * 0.12f),
            size = androidx.compose.ui.geometry.Size(s * 0.58f, s * 0.72f)
        )

        // Head
        val headY = cy - s * 0.36f
        drawCircle(color = Color(0xFFFFE8D0), radius = s * 0.33f, center = Offset(cx, headY))

        // Crown
        val crownColor = if (isSpeaking) fairyPalette.accent else fairyPalette.aura
        drawCircle(color = crownColor, radius = s * 0.145f, center = Offset(cx, headY - s * 0.29f))

        // Eyes
        val eyeY = headY - s * 0.09f
        val eyeDist = s * 0.155f
        when {
            mood == SparkMood.SLEEPY -> {
                drawLine(Color(0xFF3D2A1F), Offset(cx - eyeDist, eyeY), Offset(cx - eyeDist + s*0.13f, eyeY), strokeWidth = 7f)
                drawLine(Color(0xFF3D2A1F), Offset(cx + eyeDist - s*0.13f, eyeY), Offset(cx + eyeDist, eyeY), strokeWidth = 7f)
            }
            isBlinking -> {
                drawLine(Color(0xFF3D2A1F), Offset(cx - eyeDist, eyeY), Offset(cx - eyeDist + s*0.12f, eyeY), strokeWidth = 5f)
                drawLine(Color(0xFF3D2A1F), Offset(cx + eyeDist - s*0.12f, eyeY), Offset(cx + eyeDist, eyeY), strokeWidth = 5f)
            }
            mood == SparkMood.ALERT -> {
                drawCircle(Color.Black, radius = s * 0.095f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.095f, center = Offset(cx + eyeDist, eyeY))
            }
            else -> {
                drawCircle(Color.Black, radius = s * 0.075f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.075f, center = Offset(cx + eyeDist, eyeY))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = s * 0.025f, center = Offset(cx - eyeDist + s*0.02f, eyeY - s*0.02f))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = s * 0.025f, center = Offset(cx + eyeDist + s*0.02f, eyeY - s*0.02f))
            }
        }

        // Mouth
        val mouthOpen = if (isSpeaking) (sin(speakingPhase * 4.2f).toFloat() * 0.55f + 0.45f) * s * 0.11f else s * 0.022f
        drawOval(
            color = Color(0xFF5D2E2E),
            topLeft = Offset(cx - s * 0.105f, headY + s * 0.085f - mouthOpen / 2),
            size = androidx.compose.ui.geometry.Size(s * 0.21f, mouthOpen)
        )

        // Idle sparkles + form accent
        val particleCount = when (mood) {
            SparkMood.HAPPY -> 13
            SparkMood.SPEAKING -> 15
            SparkMood.THINKING -> 11
            else -> 7
        }
        for (i in 0 until particleCount) {
            val ang = phase * 1.15f + i * 0.95f
            val r = s * 0.58f + sin(phase + i).toFloat() * 9f
            val px = cx + cos(ang).toFloat() * r
            val py = cy + sin(ang).toFloat() * r * 0.68f
            val alpha = if (mood == SparkMood.HAPPY || isSpeaking) 0.85f else 0.55f
            drawCircle(fairyPalette.accent, radius = 3.5f, center = Offset(px, py), alpha = alpha * visualIntensity)
        }

        // Core pulse with form core color
        val pulse = if (isSpeaking) sin(speakingPhase * 3.8f).toFloat() * 0.22f + 0.92f else sin(phase * 0.65f).toFloat() * 0.14f + 0.86f
        drawCircle(
            color = fairyPalette.core.copy(alpha = 0.55f * pulse),
            radius = s * 0.155f * pulse,
            center = Offset(cx, cy + s * 0.09f)
        )
    }
}

private fun drawWing(
    scope: DrawScope,
    x: Float,
    y: Float,
    s: Float,
    flap: Float,
    left: Boolean,
    palette: com.sparkx.fairyos.FairyPalette
) {
    val sign = if (left) -1f else 1f
    val path = Path().apply {
        moveTo(x, y)
        quadraticTo(
            x + sign * s * 0.62f, y - s * (0.42f + flap),
            x + sign * s * 0.26f, y + s * 0.16f
        )
        close()
    }
    scope.drawPath(path, color = palette.wingA.copy(alpha = 0.78f))
}