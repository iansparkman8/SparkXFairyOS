package com.sparkx.fairyos.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
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
 * Premium asset-backed overlay avatar renderer.
 *
 * This displays Spark Baby from transparent PNG/WebP assets instead of
 * drawing a weak procedural Canvas mascot.
 */
class SparkOverlayAvatarView(context: Context) : FrameLayout(context) {

    private val glowView = SparkOverlayGlowView(context)
    private val fairyImage = ImageView(context)
    private val particleView = SparkOverlayParticleView(context)

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
        fairyImage.setPadding(2, 2, 2, 2)

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
        pulseSmall()
    }

    fun updateMotion(vx: Float, vy: Float) {
        particleView.updateMotion(vx, vy)

        fairyImage.rotation = (vx / 40f * 5f).coerceIn(-7f, 7f)
        fairyImage.translationX = (vx / 40f * 5f).coerceIn(-6f, 6f)
        fairyImage.translationY = (vy / 40f * 3f).coerceIn(-4f, 4f)
    }

    fun setFreeRoamActive(active: Boolean) {
        glowView.setFreeRoamActive(active)
        particleView.setFreeRoamActive(active)
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
        } else {
            fairyImage.setImageResource(android.R.drawable.star_big_on)
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
    private var speaking: Boolean = false
    private var freeRoam: Boolean = false
    private var touched: Boolean = false

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

        val time = SystemClock.uptimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f

        val base = when {
            speaking -> Color.rgb(255, 120, 220)
            mood == SparkMood.HAPPY -> Color.rgb(255, 140, 235)
            mood == SparkMood.THINKING -> Color.rgb(170, 130, 255)
            mood == SparkMood.LISTENING -> Color.rgb(0, 255, 215)
            mood == SparkMood.ALERT -> Color.rgb(255, 90, 120)
            mood == SparkMood.SLEEPY -> Color.rgb(150, 155, 255)
            else -> Color.rgb(0, 229, 255)
        }

        val pulse = 1f + sin(time * if (speaking) 6f else 2f) * 0.055f

        val alpha = when {
            touched -> 120
            freeRoam -> 105
            speaking -> 115
            else -> 72
        }

        paint.shader = RadialGradient(
            cx,
            cy,
            width * 0.47f * pulse,
            intArrayOf(
                Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb(alpha / 2, 160, 95, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, width * 0.47f * pulse, paint)
        paint.shader = null

        postInvalidateOnAnimation()
    }
}

private class SparkOverlayParticleView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mood: SparkMood = SparkMood.IDLE
    private var speaking: Boolean = false
    private var freeRoam: Boolean = false
    private var motionVx: Float = 0f
    private var motionVy: Float = 0f
    private var burstAt: Long = 0L

    private data class ParticleSeed(
        val angle: Float,
        val radius: Float,
        val speed: Float,
        val size: Float
    )

    private val seeds = List(32) {
        ParticleSeed(
            angle = Random.nextFloat() * 6.283f,
            radius = 0.14f + Random.nextFloat() * 0.42f,
            speed = 0.45f + Random.nextFloat() * 1.9f,
            size = 1.0f + Random.nextFloat() * 2.8f
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

        val time = SystemClock.uptimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f

        val color = when {
            speaking -> Color.rgb(255, 145, 230)
            mood == SparkMood.ALERT -> Color.rgb(255, 120, 120)
            mood == SparkMood.LISTENING -> Color.rgb(0, 255, 210)
            mood == SparkMood.SLEEPY -> Color.rgb(180, 180, 255)
            mood == SparkMood.HAPPY -> Color.rgb(255, 185, 245)
            else -> Color.rgb(155, 235, 255)
        }

        val count = when {
            speaking -> 30
            freeRoam -> 26
            else -> 18
        }

        for (i in 0 until count.coerceAtMost(seeds.size)) {
            val seed = seeds[i]
            val angle = seed.angle + time * seed.speed
            val radius = width * seed.radius

            val driftX = -motionVx * 0.22f
            val driftY = -motionVy * 0.12f

            val x = cx + cos(angle) * radius + driftX
            val y = cy + sin(angle * 0.86f) * radius + driftY

            val twinkle = ((sin(time * 5f + i) + 1f) * 0.5f)
            val alpha = (55 + twinkle * 170).toInt()

            paint.color = Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )

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