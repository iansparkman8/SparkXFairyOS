package com.sparkx.fairyos.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.sparkx.fairyos.domain.mood.SparkMood
import com.sparkx.fairyos.domain.personality.SparkForm
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Asset-backed overlay avatar renderer.
 *
 * This replaces the legacy procedural Canvas fairy.
 * Real game-quality depends on transparent WebP/PNG/Rive assets.
 */
class SparkOverlayAvatarView(context: Context) : FrameLayout(context) {

    private val particleView = SparkOverlayParticleView(context)
    private val glowView = SparkOverlayGlowView(context)
    private val fairyImage = ImageView(context)

    private var currentMood: SparkMood = SparkMood.IDLE
    private var isSpeaking: Boolean = false
    private var currentForm: SparkForm = SparkForm.DEFAULT_FAIRY

    init {
        clipChildren = false
        clipToPadding = false
        setWillNotDraw(false)
        isClickable = true
        isLongClickable = true

        addView(
            glowView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        addView(
            fairyImage,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        )

        addView(
            particleView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        fairyImage.scaleType = ImageView.ScaleType.FIT_CENTER
        fairyImage.adjustViewBounds = true
        fairyImage.setPadding(4, 4, 4, 4)

        applyMoodAsset()
        startIdleFloat()
    }

    fun updateState(mood: SparkMood, speaking: Boolean) {
        updateState(mood, speaking, currentForm)
    }

    fun updateState(
        mood: SparkMood,
        speaking: Boolean,
        form: SparkForm
    ) {
        val changed = mood != currentMood || speaking != isSpeaking || form != currentForm

        currentMood = mood
        isSpeaking = speaking
        currentForm = form

        glowView.updateMood(mood, speaking)
        particleView.updateMood(mood, speaking)

        if (changed) {
            applyMoodAsset()
            pulseSmall()
        }
    }

    fun updateForm(form: SparkForm) {
        currentForm = form
        applyMoodAsset()
    }

    fun updateMotion(vx: Float, vy: Float) {
        particleView.updateMotion(vx, vy)

        fairyImage.rotation = (vx / 40f * 6f).coerceIn(-7f, 7f)
        fairyImage.translationX = (vx / 40f * 5f).coerceIn(-6f, 6f)
    }

    fun setFreeRoamActive(active: Boolean) {
        particleView.setFreeRoamActive(active)
        glowView.setFreeRoamActive(active)
    }

    fun setUserTouchActive(active: Boolean) {
        glowView.setUserTouchActive(active)
        if (active) pulseSmall()
    }

    fun triggerTapBurst() {
        particleView.triggerBurst()
        pulseBig()
    }

    private fun applyMoodAsset() {
        val assetName = when {
            isSpeaking || currentMood == SparkMood.SPEAKING -> "spark_fairy_speaking"
            currentMood == SparkMood.HAPPY -> "spark_fairy_happy"
            currentMood == SparkMood.THINKING -> "spark_fairy_thinking"
            currentMood == SparkMood.LISTENING -> "spark_fairy_listening"
            currentMood == SparkMood.ALERT -> "spark_fairy_alert"
            currentMood == SparkMood.SLEEPY -> "spark_fairy_sleepy"
            else -> "spark_fairy_idle"
        }

        val resId = resources.getIdentifier(assetName, "drawable", context.packageName)

        if (resId != 0) {
            fairyImage.setImageResource(resId)
            fairyImage.background = null
        } else {
            // Temporary fallback so the APK builds before real art assets exist.
            // Replace with transparent WebP/PNG assets ASAP.
            fairyImage.setImageDrawable(null)
            fairyImage.background = placeholderDrawable()
        }
    }

    private fun placeholderDrawable(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(210, 245, 230, 255),
                Color.argb(230, 160, 95, 255),
                Color.argb(220, 40, 20, 85)
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(2, Color.argb(160, 220, 245, 255))
        }
    }

    private fun startIdleFloat() {
        fairyImage.animate()
            .translationY(-10f)
            .scaleX(1.035f)
            .scaleY(1.035f)
            .setDuration(1450L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                fairyImage.animate()
                    .translationY(8f)
                    .scaleX(0.985f)
                    .scaleY(0.985f)
                    .setDuration(1450L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { startIdleFloat() }
                    .start()
            }
            .start()
    }

    private fun pulseSmall() {
        val sx = ObjectAnimator.ofFloat(fairyImage, View.SCALE_X, fairyImage.scaleX, 1.06f, 1f)
        val sy = ObjectAnimator.ofFloat(fairyImage, View.SCALE_Y, fairyImage.scaleY, 1.06f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 260L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun pulseBig() {
        val sx = ObjectAnimator.ofFloat(fairyImage, View.SCALE_X, fairyImage.scaleX, 1.16f, 1f)
        val sy = ObjectAnimator.ofFloat(fairyImage, View.SCALE_Y, fairyImage.scaleY, 1.16f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 340L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}

private class SparkOverlayGlowView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mood: SparkMood = SparkMood.IDLE
    private var speaking = false
    private var freeRoam = false
    private var touched = false

    fun updateMood(mood: SparkMood, speaking: Boolean) {
        this.mood = mood
        this.speaking = speaking
        invalidate()
    }

    fun setFreeRoamActive(active: Boolean) {
        freeRoam = active
        invalidate()
    }

    fun setUserTouchActive(active: Boolean) {
        touched = active
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val t = SystemClock.uptimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f

        val base = when (mood) {
            SparkMood.HAPPY -> Color.rgb(255, 110, 210)
            SparkMood.THINKING -> Color.rgb(160, 125, 255)
            SparkMood.LISTENING -> Color.rgb(0, 255, 190)
            SparkMood.ALERT -> Color.rgb(255, 80, 110)
            SparkMood.SLEEPY -> Color.rgb(115, 120, 220)
            SparkMood.SPEAKING -> Color.rgb(255, 110, 210)
            else -> Color.rgb(0, 229, 255)
        }

        val pulse = 1f + sin(t * if (speaking) 6f else 2f) * 0.05f
        val alpha = when {
            touched -> 120
            freeRoam -> 100
            speaking -> 115
            else -> 72
        }

        paint.shader = RadialGradient(
            cx,
            cy,
            width * 0.46f * pulse,
            intArrayOf(
                Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb(alpha / 2, 150, 95, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, width * 0.46f * pulse, paint)
        paint.shader = null

        postInvalidateOnAnimation()
    }
}

private class SparkOverlayParticleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mood: SparkMood = SparkMood.IDLE
    private var speaking = false
    private var freeRoam = false
    private var motionVx = 0f
    private var motionVy = 0f
    private var burstAt = 0L

    private data class ParticleSeed(
        val angle: Float,
        val radius: Float,
        val speed: Float,
        val size: Float
    )

    private val seeds = List(28) {
        ParticleSeed(
            angle = Random.nextFloat() * 6.283f,
            radius = 0.18f + Random.nextFloat() * 0.38f,
            speed = 0.5f + Random.nextFloat() * 1.8f,
            size = 1.0f + Random.nextFloat() * 2.4f
        )
    }

    fun updateMood(mood: SparkMood, speaking: Boolean) {
        this.mood = mood
        this.speaking = speaking
        invalidate()
    }

    fun updateMotion(vx: Float, vy: Float) {
        motionVx = vx.coerceIn(-40f, 40f)
        motionVy = vy.coerceIn(-40f, 40f)
        invalidate()
    }

    fun setFreeRoamActive(active: Boolean) {
        freeRoam = active
        invalidate()
    }

    fun triggerBurst() {
        burstAt = System.currentTimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val t = SystemClock.uptimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f

        val color = when (mood) {
            SparkMood.ALERT -> Color.rgb(255, 120, 120)
            SparkMood.LISTENING -> Color.rgb(0, 255, 190)
            SparkMood.SLEEPY -> Color.rgb(180, 180, 255)
            SparkMood.HAPPY -> Color.rgb(255, 170, 235)
            else -> Color.rgb(150, 235, 255)
        }

        val count = when {
            speaking -> 28
            freeRoam -> 24
            else -> 16
        }

        for (i in 0 until count.coerceAtMost(seeds.size)) {
            val seed = seeds[i]
            val a = seed.angle + t * seed.speed
            val r = width * seed.radius
            val driftX = -motionVx * 0.22f
            val driftY = -motionVy * 0.12f

            val x = cx + cos(a) * r + driftX
            val y = cy + sin(a * 0.86f) * r + driftY
            val twinkle = ((sin(t * 5f + i) + 1f) * 0.5f)
            val alpha = (55 + twinkle * 170).toInt()

            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawCircle(x, y, seed.size * twinkle + 0.8f, paint)
        }

        drawBurst(canvas, color)

        postInvalidateOnAnimation()
    }

    private fun drawBurst(canvas: Canvas, color: Int) {
        val age = System.currentTimeMillis() - burstAt
        if (age !in 0..420) return

        val progress = age / 420f
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * (0.15f + 0.32f * progress)
        val alpha = ((1f - progress) * 220).toInt().coerceIn(0, 220)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (4f * (1f - progress)).coerceAtLeast(0.6f)
        paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL
    }
}