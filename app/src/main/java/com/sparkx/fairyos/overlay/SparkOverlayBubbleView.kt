package com.sparkx.fairyos.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.sparkx.fairyos.domain.mood.SparkMood

import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom View for the floating fairy bubble.
 * Procedural drawing - no assets. Supports mood and speaking animation.
 */
class SparkOverlayBubbleView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentMood: SparkMood = SparkMood.IDLE
    private var isSpeaking: Boolean = false
    private var animationPhase: Float = 0f

    private var centerX = 0f
    private var centerY = 0f

    fun updateState(mood: SparkMood, speaking: Boolean) {
        currentMood = mood
        isSpeaking = speaking
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val size = width.coerceAtMost(height) * 0.9f
        val time = System.currentTimeMillis() / 200f

        // Aura / glow
        glowPaint.style = Paint.Style.FILL
        val auraAlpha = if (isSpeaking) 180 else 90
        glowPaint.color = Color.argb(auraAlpha, 100, 200, 255)
        val auraRadius = size * (if (isSpeaking) 0.65f else 0.55f) + sin(time * 0.1f).toFloat() * 8f
        canvas.drawCircle(centerX, centerY, auraRadius, glowPaint)

        // Wings (gentle flap when speaking or happy)
        paint.color = Color.argb(180, 180, 220, 255)
        paint.style = Paint.Style.FILL
        val wingFlap = if (isSpeaking || currentMood == SparkMood.HAPPY) sin(time * 0.8f).toFloat() * 0.3f else 0f

        // Left wing
        val wingPath = Path()
        wingPath.moveTo(centerX - size * 0.15f, centerY)
        wingPath.quadTo(
            centerX - size * 0.55f, centerY - size * (0.35f + wingFlap),
            centerX - size * 0.25f, centerY + size * 0.1f
        )
        canvas.drawPath(wingPath, paint)

        // Right wing
        wingPath.reset()
        wingPath.moveTo(centerX + size * 0.15f, centerY)
        wingPath.quadTo(
            centerX + size * 0.55f, centerY - size * (0.35f + wingFlap),
            centerX + size * 0.25f, centerY + size * 0.1f
        )
        canvas.drawPath(wingPath, paint)

        // Body (ethereal dress)
        paint.color = Color.argb(220, 120, 80, 200)
        canvas.drawOval(
            centerX - size * 0.22f, centerY - size * 0.05f,
            centerX + size * 0.22f, centerY + size * 0.45f,
            paint
        )

        // Head
        paint.color = Color.argb(255, 255, 220, 200)
        val headY = centerY - size * 0.15f
        canvas.drawCircle(centerX, headY, size * 0.28f, paint)

        // Crown / tiara glow
        paint.color = if (isSpeaking) Color.argb(255, 255, 215, 0) else Color.argb(200, 200, 180, 255)
        val crownY = headY - size * 0.22f
        canvas.drawCircle(centerX, crownY, size * 0.12f, paint)
        // Crown points
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        for (i in 0..4) {
            val angle = Math.PI * 2 * i / 5 - Math.PI / 2
            val px = centerX + cos(angle).toFloat() * size * 0.18f
            val py = crownY + sin(angle).toFloat() * size * 0.08f
            canvas.drawLine(centerX, crownY, px, py - 8f, paint)
        }
        paint.style = Paint.Style.FILL

        // Eyes
        paint.color = Color.BLACK
        val eyeY = headY - size * 0.05f
        val eyeOffset = size * 0.12f
        val eyeSize = size * 0.06f
        when (currentMood) {
            SparkMood.SLEEPY -> {
                // Closed eyes
                canvas.drawLine(centerX - eyeOffset, eyeY, centerX - eyeOffset + eyeSize * 1.5f, eyeY, paint)
                canvas.drawLine(centerX + eyeOffset - eyeSize * 1.5f, eyeY, centerX + eyeOffset, eyeY, paint)
            }
            SparkMood.ALERT -> {
                canvas.drawCircle(centerX - eyeOffset, eyeY, eyeSize * 1.2f, paint)
                canvas.drawCircle(centerX + eyeOffset, eyeY, eyeSize * 1.2f, paint)
            }
            else -> {
                canvas.drawCircle(centerX - eyeOffset, eyeY, eyeSize, paint)
                canvas.drawCircle(centerX + eyeOffset, eyeY, eyeSize, paint)
            }
        }

        // Mouth - animates when speaking
        paint.color = Color.argb(200, 80, 40, 40)
        val mouthY = headY + size * 0.12f
        val mouthOpen = if (isSpeaking) (sin(time * 3f).toFloat() * 0.5f + 0.5f) * size * 0.08f else size * 0.02f
        canvas.drawOval(
            centerX - size * 0.08f, mouthY - mouthOpen / 2,
            centerX + size * 0.08f, mouthY + mouthOpen / 2,
            paint
        )

        // Orbiting particles when speaking or thinking
        if (isSpeaking || currentMood == SparkMood.THINKING) {
            paint.color = Color.argb(200, 150, 255, 255)
            for (i in 0..5) {
                val angle = time * 0.5f + i * 1.2f
                val px = centerX + cos(angle).toFloat() * (size * 0.4f + sin(time + i).toFloat() * 5f)
                val py = centerY + sin(angle).toFloat() * size * 0.35f
                canvas.drawCircle(px, py, 4f, paint)
            }
        }

        // Core glow (faster pulse when speaking)
        val pulse = if (isSpeaking) sin(time * 2.5f).toFloat() * 0.15f + 0.85f else sin(time * 0.6f).toFloat() * 0.1f + 0.9f
        glowPaint.color = Color.argb((120 * pulse).toInt(), 255, 100, 200)
        canvas.drawCircle(centerX, centerY + size * 0.1f, size * 0.12f * pulse, glowPaint)
    }
}