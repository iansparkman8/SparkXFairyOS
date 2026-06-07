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
    modifier: Modifier = Modifier,
    mood: SparkMood,
    isSpeaking: Boolean,
    reactionKey: Int = 0,
    form: FairyForm = FairyForm.Androgynous,
    visualIntensity: Float = 1f
) {
    val fairyPalette = form.palette()

    val infiniteTransition = rememberInfiniteTransition(label = "fairy")

    // Main breathing + idle phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Speaking animation phase
    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 140 else 920, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaking"
    )

    // Blink cycle
    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    val isBlinking = blinkPhase > 0.91f && mood != SparkMood.ALERT && mood != SparkMood.SLEEPY

    // Tap reaction scale
    val reactionScale by animateFloatAsState(
        targetValue = if (reactionKey % 2 == 0) 1f else 1.065f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "reaction"
    )

    // Extra idle shimmer phase for visual life
    val idleShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleShimmer"
    )

    Canvas(modifier = modifier.size(280.dp)) {
        val cx = size.width / 2
        val baseCy = size.height / 2 + 16f

        // === Stronger organic breathing + body bob ===
        val breath = sin(phase * 0.92f) * 7.5f * visualIntensity
        val bodyBob = sin(phase * 0.75f) * 2.2f
        val cy = baseCy + breath + bodyBob

        val s = size.width * 0.37f * reactionScale

        // === Aura (mood + speaking reactive) ===
        val auraIntensity = when {
            isSpeaking -> 0.38f
            mood == SparkMood.HAPPY -> 0.28f
            mood == SparkMood.ALERT -> 0.32f
            else -> 0.18f
        }
        val auraR = s * (if (isSpeaking) 1.42f else 1.22f) + sin(phase * 0.65f) * 16f
        drawCircle(
            color = fairyPalette.aura.copy(alpha = auraIntensity * visualIntensity),
            radius = auraR,
            center = Offset(cx, cy)
        )

        // === Wings with idle micro-flutter ===
        val baseFlap = sin(phase * 0.6f) * 0.09f
        val activeFlap = if (isSpeaking || mood == SparkMood.HAPPY) sin(speakingPhase * 2.1f) * 0.32f else 0f
        val flap = baseFlap + activeFlap

        drawWing(this, cx - s * 0.33f, cy, s, flap, left = true, palette = fairyPalette)
        drawWing(this, cx + s * 0.33f, cy, s, flap, left = false, palette = fairyPalette)

        // === Body ===
        drawOval(
            color = fairyPalette.shadow,
            topLeft = Offset(cx - s * 0.30f, cy - s * 0.13f),
            size = androidx.compose.ui.geometry.Size(s * 0.60f, s * 0.74f)
        )

        // === Head ===
        val headY = cy - s * 0.37f
        drawCircle(
            color = Color(0xFFFFE8D0),
            radius = s * 0.335f,
            center = Offset(cx, headY)
        )

        // === Crown / Halo ===
        val crownAlpha = if (isSpeaking) 0.95f else 0.75f
        drawCircle(
            color = fairyPalette.accent.copy(alpha = crownAlpha),
            radius = s * 0.15f,
            center = Offset(cx, headY - s * 0.30f)
        )

        // === Eyes (highly expressive) ===
        val eyeY = headY - s * 0.085f
        val eyeDist = s * 0.16f

        when {
            mood == SparkMood.SLEEPY -> {
                // Sleepy eyes
                drawLine(Color(0xFF3D2A1F), Offset(cx - eyeDist, eyeY), Offset(cx - eyeDist + s * 0.14f, eyeY), strokeWidth = 6.5f)
                drawLine(Color(0xFF3D2A1F), Offset(cx + eyeDist - s * 0.14f, eyeY), Offset(cx + eyeDist, eyeY), strokeWidth = 6.5f)
            }
            isBlinking -> {
                drawLine(Color(0xFF3D2A1F), Offset(cx - eyeDist, eyeY), Offset(cx - eyeDist + s * 0.13f, eyeY), strokeWidth = 5f)
                drawLine(Color(0xFF3D2A1F), Offset(cx + eyeDist - s * 0.13f, eyeY), Offset(cx + eyeDist, eyeY), strokeWidth = 5f)
            }
            mood == SparkMood.ALERT -> {
                drawCircle(Color.Black, radius = s * 0.10f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.10f, center = Offset(cx + eyeDist, eyeY))
            }
            else -> {
                // Normal expressive eyes
                drawCircle(Color.Black, radius = s * 0.078f, center = Offset(cx - eyeDist, eyeY))
                drawCircle(Color.Black, radius = s * 0.078f, center = Offset(cx + eyeDist, eyeY))
                // Highlights
                drawCircle(Color.White.copy(alpha = 0.65f), radius = s * 0.026f, center = Offset(cx - eyeDist + s * 0.022f, eyeY - s * 0.022f))
                drawCircle(Color.White.copy(alpha = 0.65f), radius = s * 0.026f, center = Offset(cx + eyeDist + s * 0.022f, eyeY - s * 0.022f))
            }
        }

        // === Mouth ===
        val mouthOpen = if (isSpeaking) {
            (sin(speakingPhase * 4.4f) * 0.58f + 0.48f) * s * 0.115f
        } else {
            s * 0.024f
        }
        drawOval(
            color = Color(0xFF5D2E2E),
            topLeft = Offset(cx - s * 0.11f, headY + s * 0.09f - mouthOpen / 2),
            size = androidx.compose.ui.geometry.Size(s * 0.22f, mouthOpen)
        )

        // === Dynamic particles (mood + speaking reactive) ===
        val particleCount = when {
            isSpeaking -> 16
            mood == SparkMood.HAPPY -> 14
            mood == SparkMood.THINKING -> 11
            else -> 8
        }

        for (i in 0 until particleCount) {
            val speed = if (isSpeaking) 1.35f else 0.95f
            val ang = phase * speed + i * 0.92f
            val radiusMod = if (isSpeaking) 1.15f else 1.0f
            val r = s * 0.62f * radiusMod + sin(phase + i * 1.3f) * 11f
            val px = cx + cos(ang) * r
            val py = cy + sin(ang) * r * 0.72f

            val alpha = when {
                isSpeaking -> 0.9f
                mood == SparkMood.HAPPY -> 0.82f
                else -> 0.55f + idleShimmer * 0.25f
            }
            drawCircle(
                fairyPalette.accent,
                radius = 3.8f,
                center = Offset(px, py),
                alpha = alpha * visualIntensity
            )
        }

        // === Core pulse (very alive) ===
        val corePulse = if (isSpeaking) {
            sin(speakingPhase * 4.1f) * 0.26f + 0.88f
        } else {
            sin(phase * 0.7f) * 0.16f + 0.84f
        }

        drawCircle(
            color = fairyPalette.core.copy(alpha = 0.6f * corePulse),
            radius = s * 0.16f * corePulse,
            center = Offset(cx, cy + s * 0.095f)
        )

        // === Subtle tap burst effect ===
        if (reactionKey % 2 == 1) {
            val burstAlpha = 0.45f * (1f - ((reactionKey % 10) / 10f))
            drawCircle(
                fairyPalette.accent.copy(alpha = burstAlpha),
                radius = s * 0.55f,
                center = Offset(cx, cy)
            )
        }
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
            x + sign * s * 0.64f, y - s * (0.44f + flap),
            x + sign * s * 0.27f, y + s * 0.17f
        )
        close()
    }
    scope.drawPath(path, color = palette.wingA.copy(alpha = 0.82f))
}