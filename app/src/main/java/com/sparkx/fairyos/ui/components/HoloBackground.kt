package com.sparkx.fairyos.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sparkx.fairyos.domain.mood.SparkMood
import kotlin.math.cos
import kotlin.math.sin

enum class SparkWorldTheme {
    HOLO_DREAM,
    ENCHANTED_FOREST,
    MUSHROOM_GROVE,
    MOONLIT_CAVE,
    CRYSTAL_PHONE,
    ROYAL_AURA,
    SHADOW_GARDEN,
    CYBER_SKY
}

@Composable
fun HoloBackground(modifier: Modifier = Modifier) {
    HoloBackground(
        modifier = modifier,
        worldTheme = SparkWorldTheme.HOLO_DREAM,
        mood = SparkMood.IDLE,
        intensity = 1f,
        ownerMode = false
    )
}

@Composable
fun HoloBackground(
    modifier: Modifier = Modifier,
    worldTheme: SparkWorldTheme = SparkWorldTheme.HOLO_DREAM,
    mood: SparkMood = SparkMood.IDLE,
    intensity: Float = 1f,
    ownerMode: Boolean = false
) {
    val phase by rememberInfiniteTransition(label = "fairy_world_bg").animateFloat(
        initialValue = 0f,
        targetValue = 10_000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fairy_world_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase

        when (worldTheme) {
            SparkWorldTheme.HOLO_DREAM -> drawHoloDream(this, w, h, t, intensity)
            SparkWorldTheme.ENCHANTED_FOREST -> drawEnchantedForest(this, w, h, t, intensity)
            SparkWorldTheme.MUSHROOM_GROVE -> drawMushroomGrove(this, w, h, t, intensity)
            SparkWorldTheme.MOONLIT_CAVE -> drawMoonlitCave(this, w, h, t, intensity)
            SparkWorldTheme.CRYSTAL_PHONE -> drawCrystalPhone(this, w, h, t, intensity)
            SparkWorldTheme.ROYAL_AURA -> drawRoyalAura(this, w, h, t, intensity)
            SparkWorldTheme.SHADOW_GARDEN -> drawShadowGarden(this, w, h, t, intensity)
            SparkWorldTheme.CYBER_SKY -> drawHoloDream(this, w, h, t, intensity)
        }

        // Subtle vignette for readability
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x66000000)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = (w.coerceAtLeast(h)) * 0.85f
            )
        )
    }
}

private fun drawHoloDream(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        // Deep navy-purple base
        drawRect(Color(0xFF0A0818))

        // Aurora layers
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF1A0F3D).copy(alpha = 0.6f * intensity),
                    Color(0xFF0D1B3D).copy(alpha = 0.3f * intensity),
                    Color.Transparent
                )
            )
        )

        // Soft cyan/pink aurora waves
        for (i in 0..2) {
            val phase = t * (0.6f + i * 0.2f) + i
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.08f + sin(phase) * 0.03f),
                        Color(0xFFFF6EC7).copy(alpha = 0.06f + cos(phase * 0.7f) * 0.025f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, h * (0.1f + i * 0.15f)),
                size = androidx.compose.ui.geometry.Size(w, h * 0.55f)
            )
        }

        // Floating sparkles
        for (i in 0..28) {
            val x = (w * 0.1f + (i * 37f + sin(t * 0.8f + i) * 40f) % (w * 0.8f))
            val y = (h * 0.15f + (i * 53f + cos(t * 0.6f + i * 1.3f) * 60f) % (h * 0.7f))
            val alpha = (0.4f + sin(t * 3f + i) * 0.35f).coerceIn(0.15f, 0.85f)
            drawCircle(Color(0xFF00E5FF), 1.8f + sin(t + i) * 0.8f, Offset(x, y), alpha = alpha * intensity)
        }

        // Subtle grid
        val gridColor = Color(0xFF6B4C9A).copy(alpha = 0.12f * intensity)
        for (x in 0..(w / 42).toInt()) {
            drawLine(gridColor, Offset(x * 42f, 0f), Offset(x * 42f, h), 1f)
        }
        for (y in 0..(h / 42).toInt()) {
            drawLine(gridColor, Offset(0f, y * 42f), Offset(w, y * 42f), 1f)
        }
    }
}

private fun drawEnchantedForest(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF0D1A12))

        // Soft green-gold gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF1A3A22).copy(alpha = 0.7f),
                    Color(0xFF0D1A12).copy(alpha = 0.4f),
                    Color.Transparent
                )
            )
        )

        // Drifting fireflies
        for (i in 0..22) {
            val x = ((i * 67f + sin(t * 0.4f + i) * 80f) % w).coerceIn(0f, w)
            val y = (h * 0.2f + (i * 41f + cos(t * 0.55f + i) * 90f) % (h * 0.6f))
            val alpha = (0.5f + sin(t * 4f + i) * 0.4f).coerceIn(0.2f, 0.9f)
            drawCircle(Color(0xFF9CFF8C), 2.2f + sin(t * 2f + i) * 0.6f, Offset(x, y), alpha = alpha * intensity)
        }

        // Subtle leaf silhouettes at bottom
        for (i in 0..5) {
            val lx = w * (0.1f + i * 0.16f)
            drawCircle(Color(0xFF1E3A28).copy(alpha = 0.35f), 28f + (i % 3) * 8f, Offset(lx, h * 0.92f))
        }
    }
}

private fun drawMushroomGrove(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF1A140F))
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3A2A1F).copy(alpha = 0.5f), Color.Transparent)
            )
        )

        // Warm spores
        for (i in 0..18) {
            val x = ((i * 51f + cos(t * 0.3f + i) * 60f) % w)
            val y = h * (0.35f + sin(t * 0.4f + i * 1.1f) * 0.25f)
            drawCircle(Color(0xFFFFD27A), 1.6f + sin(t * 3f + i) * 0.5f, Offset(x, y), alpha = (0.4f + sin(t * 2f + i) * 0.35f) * intensity)
        }
    }
}

private fun drawMoonlitCave(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF0A0F1F))
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color(0xFF1A2A4A).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.35f),
                radius = w * 0.9f
            )
        )

        // Reflective water shimmer
        for (i in 0..7) {
            val y = h * (0.78f + sin(t * 0.6f + i) * 0.04f)
            drawLine(
                Color(0xFF7EC8FF).copy(alpha = 0.15f * intensity),
                Offset(0f, y),
                Offset(w, y + sin(t * 1.2f + i) * 3f),
                1.5f
            )
        }
    }
}

private fun drawCrystalPhone(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF0A0C14))

        // Cyan glass reflections
        for (i in 0..4) {
            val x = w * (0.15f + i * 0.18f) + sin(t * 0.7f + i) * 25f
            drawLine(
                Color(0xFF00E5FF).copy(alpha = 0.12f * intensity),
                Offset(x, 0f),
                Offset(x + 40f, h),
                2f
            )
        }

        // Scanlines
        for (y in 0..(h / 18).toInt()) {
            drawLine(
                Color(0xFF00E5FF).copy(alpha = 0.06f),
                Offset(0f, y * 18f),
                Offset(w, y * 18f),
                0.8f
            )
        }
    }
}

private fun drawRoyalAura(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF120D1F))
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color(0xFF3A1F5C).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.85f
            )
        )

        // Gold dust
        for (i in 0..14) {
            val x = w * (0.2f + (i * 0.047f + sin(t * 0.5f + i) * 0.08f) % 0.6f)
            val y = h * (0.25f + cos(t * 0.6f + i * 0.9f) * 0.35f)
            drawCircle(Color(0xFFFFD166), 1.4f + sin(t * 4f + i) * 0.5f, Offset(x, y), alpha = (0.5f + sin(t * 3f + i) * 0.35f) * intensity)
        }
    }
}

private fun drawShadowGarden(scope: DrawScope, w: Float, h: Float, t: Float, intensity: Float) {
    with(scope) {
        drawRect(Color(0xFF0D0A12))
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2A1A2F).copy(alpha = 0.6f), Color.Transparent)
            )
        )

        // Red-violet accent particles
        for (i in 0..11) {
            val x = w * (0.15f + (i * 0.067f) % 0.7f)
            val y = h * (0.3f + sin(t * 0.8f + i) * 0.4f)
            drawCircle(Color(0xFFFF4D6D), 1.8f + sin(t * 5f + i) * 0.6f, Offset(x, y), alpha = (0.35f + sin(t * 4f + i) * 0.4f) * intensity)
        }
    }
}