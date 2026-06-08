package com.sparkx.fairyos.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
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
    private var currentAssetName: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // === Idle Animation State ===
    private var isIdleFrameCycling = false
    private var idleFrameIndex = 0
    private val idleFrameRateMs = 280L

    // Idle frame names (base + optional numbered frames for breathing cycle)
    private val idleFrameNames = listOf(
        "spark_fairy_idle",
        "spark_fairy_idle_01",
        "spark_fairy_idle_02",
        "spark_fairy_idle_03"
    )

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
        startContinuousFloatAnimation()
    }

    // region Public API

    fun updateState(mood: SparkMood, speaking: Boolean) {
        updateState(mood, speaking, currentForm)
    }

    fun updateState(
        mood: SparkMood,
        speaking: Boolean,
        form: SparkForm
    ) {
        val moodOrSpeakingChanged = mood != currentMood || speaking != isSpeaking

        currentMood = mood
        isSpeaking = speaking
        currentForm = form

        glowView.updateMood(mood, speaking)
        particleView.updateMood(mood, speaking)

        if (moodOrSpeakingChanged) {
            applyMoodAsset()
            pulseSmall()

            if (mood == SparkMood.IDLE && !speaking) {
                startIdleFrameCycling()
            } else {
                stopIdleFrameCycling()
            }
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

    // endregion

    // region Asset & Mood Handling

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

        if (assetName != currentAssetName) {
            currentAssetName = assetName
            fairyImage.alpha = 0.18f

            if (resId != 0) {
                fairyImage.setImageResource(resId)
                fairyImage.contentDescription = "Spark Baby ${currentMood.name.lowercase()} avatar"
            } else {
                fairyImage.setImageResource(android.R.drawable.star_big_on)
                fairyImage.contentDescription = "Missing Spark Baby asset: $assetName"
            }

            ObjectAnimator.ofFloat(fairyImage, View.ALPHA, 0.18f, 1f).apply {
                duration = 240L
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        } else if (resId == 0) {
            fairyImage.setImageResource(android.R.drawable.star_big_on)
        }
    }

    // endregion

    // region Animation System

    /**
     * Continuous gentle floating/bobbing animation (always running).
     * This gives Spark Baby a living, breathing presence even without frame changes.
     */
    private fun startContinuousFloatAnimation() {
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
                    .withEndAction { startContinuousFloatAnimation() }
                    .start()
            }
            .start()
    }

    /**
     * Starts cycling through idle frames for breathing effect.
     * Only runs when mood is IDLE and not speaking.
     */
    private fun startIdleFrameCycling() {
        if (isIdleFrameCycling) return
        isIdleFrameCycling = true
        idleFrameIndex = 0
        scheduleNextIdleFrame()
    }

    private fun stopIdleFrameCycling() {
        isIdleFrameCycling = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun scheduleNextIdleFrame() {
        if (!isIdleFrameCycling || currentMood != SparkMood.IDLE || isSpeaking) {
            isIdleFrameCycling = false
            return
        }

        mainHandler.postDelayed({
            if (isIdleFrameCycling && currentMood == SparkMood.IDLE && !isSpeaking) {
                idleFrameIndex = (idleFrameIndex + 1) % idleFrameNames.size

                val frameName = idleFrameNames[idleFrameIndex]
                val resId = resources.getIdentifier(frameName, "drawable", context.packageName)

                if (resId != 0) {
                    fairyImage.setImageResource(resId)
                }

                scheduleNextIdleFrame()
            }
        }, idleFrameRateMs)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopIdleFrameCycling()
    }

    // endregion
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
            speaking -> Color.rgb(125, 211, 252)
            mood == SparkMood.HAPPY -> Color.rgb(229, 231, 235)
            mood == SparkMood.THINKING -> Color.rgb(96, 165, 250)
            mood == SparkMood.LISTENING -> Color.rgb(56, 189, 248)
            mood == SparkMood.ALERT -> Color.rgb(59, 130, 246)
            mood == SparkMood.SLEEPY -> Color.rgb(148, 163, 184)
            else -> Color.rgb(125, 211, 252)
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
                Color.argb(alpha / 2, 191, 199, 213),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, width * 0.47f * pulse, paint)
        paint.shader = null

        val ringPulse = (sin(time * 3.2f) + 1f) * 0.5f
        val ringAlpha = when {
            speaking -> 160
            mood == SparkMood.LISTENING -> 140
            mood == SparkMood.THINKING -> 115
            mood == SparkMood.ALERT -> 150
            else -> 72
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (speaking) 3.4f else 2.2f
        paint.color = Color.argb(
            ringAlpha,
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
        canvas.drawCircle(cx, cy, width * (0.34f + ringPulse * 0.035f), paint)

        if (speaking || mood == SparkMood.LISTENING) {
            paint.strokeWidth = 1.4f
            paint.color = Color.argb(95, 191, 199, 213)
            canvas.drawCircle(cx, cy, width * (0.41f + ringPulse * 0.055f), paint)
        }

        if (mood == SparkMood.THINKING) {
            paint.strokeWidth = 1.8f
            paint.color = Color.argb(120, 125, 211, 252)
            val orbit = time * 2.2f
            repeat(3) { index ->
                val angle = orbit + index * 2.094f
                val x = cx + cos(angle) * width * 0.30f
                val y = cy + sin(angle) * width * 0.30f
                canvas.drawCircle(x, y, 4.2f, paint)
            }
        }

        paint.style = Paint.Style.FILL

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
            speaking -> Color.rgb(125, 211, 252)
            mood == SparkMood.ALERT -> Color.rgb(59, 130, 246)
            mood == SparkMood.LISTENING -> Color.rgb(56, 189, 248)
            mood == SparkMood.SLEEPY -> Color.rgb(148, 163, 184)
            mood == SparkMood.HAPPY -> Color.rgb(229, 231, 235)
            mood == SparkMood.THINKING -> Color.rgb(96, 165, 250)
            else -> Color.rgb(125, 211, 252)
        }

        val count = when {
            speaking -> 32
            mood == SparkMood.LISTENING -> 30
            mood == SparkMood.THINKING -> 28
            freeRoam -> 26
            mood == SparkMood.SLEEPY -> 14
            else -> 20
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
        if (age !in 0..620) return

        val progress = age / 620f
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * (0.13f + 0.42f * progress)
        val alpha = ((1f - progress) * 220).toInt().coerceIn(0, 220)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (5.5f * (1f - progress)).coerceAtLeast(0.7f)
        paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL
    }
}