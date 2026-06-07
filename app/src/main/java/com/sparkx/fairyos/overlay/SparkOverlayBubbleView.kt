package com.sparkx.fairyos.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.sparkx.fairyos.domain.mood.SparkMood

import kotlin.math.cos
import kotlin.math.sin

class SparkOverlayBubbleView(context: Context) : View(context) {
    var currentMood: SparkMood = SparkMood.IDLE
        set(value) {
            field = value
            invalidate()
        }

    var isSpeaking: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var isFreeRoam: Boolean = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var centerX = 0f
    private var centerY = 0f
    private var animPhase = 0f
    private var lastTouchTime = 0L

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        // Small fairy bubble size
        layoutParams = WindowManager.LayoutParams(
            220,
            220,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            x = 100
            y = 300
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val time = System.currentTimeMillis() / 16 % 360
        animPhase = time / 360f * (if (isSpeaking) 2.5f else 1f)

        val moodColor = currentMood.color
        val size = width.coerceAtMost(height) / 2f * 0.85f

        // Aura / glow (pulsing)
        val pulse = if (isSpeaking) 1.15f + sin(animPhase * 6) * 0.08f else 1f + sin(animPhase * 3) * 0.05f
        for (i in 3 downTo 0) {
            paint.color = Color.argb((40 + i * 15), moodColor.red.toInt(), moodColor.green.toInt(), moodColor.blue.toInt())
            canvas.drawCircle(centerX, centerY, size * (0.6f + i * 0.15f) * pulse, paint)
        }

        // Wings (flapping when speaking or idle gentle)
        val flap = if (isSpeaking) sin(animPhase * 8) * 25f else sin(animPhase * 2) * 12f
        paint.color = Color.argb(200, 180, 220, 255)
        // Left wing
        val wingPath = Path()
        wingPath.moveTo(centerX - size * 0.3f, centerY)
        wingPath.quadTo(centerX - size * 0.9f, centerY - size * 0.4f + flap, centerX - size * 0.5f, centerY + size * 0.3f)
        wingPath.close()
        canvas.drawPath(wingPath, paint)
        // Right wing
        wingPath.reset()
        wingPath.moveTo(centerX + size * 0.3f, centerY)
        wingPath.quadTo(centerX + size * 0.9f, centerY - size * 0.4f - flap, centerX + size * 0.5f, centerY + size * 0.3f)
        wingPath.close()
        canvas.drawPath(wingPath, paint)

        // Body (holo core)
        paint.color = moodColor
        canvas.drawOval(centerX - size * 0.35f, centerY - size * 0.25f, centerX + size * 0.35f, centerY + size * 0.35f, paint)

        // Head
        paint.color = Color.argb(255, 255, 240, 245)
        canvas.drawCircle(centerX, centerY - size * 0.15f, size * 0.32f, paint)

        // Crown
        paint.color = Color(0xFFFFD700)
        val crownPath = Path()
        crownPath.moveTo(centerX - size * 0.25f, centerY - size * 0.4f)
        crownPath.lineTo(centerX, centerY - size * 0.65f)
        crownPath.lineTo(centerX + size * 0.25f, centerY - size * 0.4f)
        crownPath.close()
        canvas.drawPath(crownPath, paint)
        // Crown gems
        paint.color = Color.CYAN
        canvas.drawCircle(centerX, centerY - size * 0.55f, size * 0.06f, paint)

        // Eyes based on mood
        paint.color = Color(0xFF2C3E50)
        val eyeY = centerY - size * 0.18f
        when (currentMood) {
            SparkMood.SLEEPY -> {
                // Closed eyes
                canvas.drawLine(centerX - size * 0.15f, eyeY, centerX - size * 0.05f, eyeY, paint)
                canvas.drawLine(centerX + size * 0.05f, eyeY, centerX + size * 0.15f, eyeY, paint)
            }
            SparkMood.ALERT -> {
                canvas.drawCircle(centerX - size * 0.12f, eyeY, size * 0.07f, paint)
                canvas.drawCircle(centerX + size * 0.12f, eyeY, size * 0.07f, paint)
            }
            else -> {
                // Happy/default eyes
                canvas.drawCircle(centerX - size * 0.12f, eyeY, size * 0.06f, paint)
                canvas.drawCircle(centerX + size * 0.12f, eyeY, size * 0.06f, paint)
            }
        }

        // Mouth - animates when speaking
        paint.color = Color(0xFF8B0000)
        val mouthY = centerY + size * 0.05f
        if (isSpeaking) {
            val open = (sin(animPhase * 10) * 0.5f + 0.5f) * size * 0.12f
            canvas.drawOval(centerX - size * 0.12f, mouthY - open * 0.5f, centerX + size * 0.12f, mouthY + open, paint)
        } else {
            // Gentle smile
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawArc(centerX - size * 0.12f, mouthY - size * 0.08f, centerX + size * 0.12f, mouthY + size * 0.1f, 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        }

        // Orbiting particles when speaking or happy
        if (isSpeaking || currentMood == SparkMood.HAPPY) {
            paint.color = Color.argb(180, 255, 255, 200)
            for (i in 0..4) {
                val angle = animPhase * 4 + i * 72f
                val r = size * 0.55f + sin(animPhase * 3 + i) * 8f
                val px = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * r
                val py = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * r * 0.6f
                canvas.drawCircle(px, py, 4f, paint)
            }
        }

        // Core glow
        paint.color = Color.argb(if (isSpeaking) 220 else 160, 255, 200, 100)
        canvas.drawCircle(centerX, centerY + size * 0.05f, size * 0.12f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - lastTouchTime
                if (duration > 600) {
                    // Long press -> toggle free roam
                    isFreeRoam = !isFreeRoam
                    // In real controller would start/stop wander animator
                } else {
                    // Short tap -> toggle mood or speak hello
                    // Handled by controller usually
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}