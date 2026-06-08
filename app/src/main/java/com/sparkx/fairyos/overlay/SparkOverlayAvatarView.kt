package com.sparkx.fairyos.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
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
 * Premium asset-backed overlay avatar renderer with procedural iridescent wings.
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

    private var isIdleFrameCycling = false
    private var idleFrameIndex = 0
    private val idleFrameRateMs = 280L

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

        addView(glowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        addView(
            fairyImage,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        )

        addView(particleView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

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

    fun updateState(mood: SparkMood, speaking: Boolean, form: SparkForm) {
        val changed = mood != currentMood || speaking != isSpeaking

        currentMood = mood
        isSpeaking = speaking
        currentForm = form

        glowView.updateMood(mood, speaking)
        particleView.updateMood(mood, speaking)

        if (changed) {
            applyMoodAsset()
            pulseSmall()

            if (mood == SparkMood.IDLE && !speaking) startIdleFrameCycling()
            else stopIdleFrameCycling()
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

    // region Asset Handling

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
            fairyImage.alpha = 0.15f

            fairyImage.setImageResource(if (resId != 0) resId else android.R.drawable.star_big_on)

            ObjectAnimator.ofFloat(fairyImage, View.ALPHA, 0.15f, 1f).apply {
                duration = 220L
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    // endregion

    // region Animation

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
                val resId = resources.getIdentifier(idleFrameNames[idleFrameIndex], "drawable", context.packageName)
                if (resId != 0) fairyImage.setImageResource(resId)
                scheduleNextIdleFrame()
            }
        }, idleFrameRateMs)
    }

    private fun pulseSmall() {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(fairyImage, View.SCALE_X, fairyImage.scaleX, 1.06f, 1f),
                ObjectAnimator.ofFloat(fairyImage, View.SCALE_Y, fairyImage.scaleY, 1.06f, 1f)
            )
            duration = 260L
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun pulseBig() {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(fairyImage, View.SCALE_X, fairyImage.scaleX, 1.16f, 1f),
                ObjectAnimator.ofFloat(fairyImage, View.SCALE_Y, fairyImage.scaleY, 1.16f, 1f)
            )
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

    // region Drawing - Procedural Iridescent Wings

    override fun dispatchDraw(canvas: Canvas) {
        // Draw procedural iridescent wings behind the body
        val cx = width / 2f
        val cy = height / 2f + 18f

        ProceduralWingRenderer.drawWings(
            canvas = canvas,
            cx = cx,
            cy = cy,
            width = width.toFloat(),
            height = height.toFloat(),
            mood = currentMood,
            isSpeaking = isSpeaking,
            time = SystemClock.uptimeMillis() / 1000f
        )

        super.dispatchDraw(canvas)
    }

    // endregion
}

// ========================================================
// PROCEDURAL IRIDESCENT WING RENDERER (Shader-based)
// ========================================================

object ProceduralWingRenderer {

    private val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun drawWings(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        mood: SparkMood,
        isSpeaking: Boolean,
        time: Float
    ) {
        val unit = width / 220f
        val phase = time * 2.1f

        val colorA = when {
            isSpeaking -> Color.rgb(100, 200, 255)
            mood == SparkMood.HAPPY -> Color.rgb(170, 210, 255)
            mood == SparkMood.THINKING -> Color.rgb(130, 180, 255)
            mood == SparkMood.LISTENING -> Color.rgb(90, 210, 255)
            mood == SparkMood.ALERT -> Color.rgb(140, 150, 255)
            mood == SparkMood.SLEEPY -> Color.rgb(150, 170, 220)
            else -> Color.rgb(140, 195, 255)
        }

        val colorB = when {
            isSpeaking -> Color.rgb(200, 130, 255)
            mood == SparkMood.HAPPY -> Color.rgb(220, 170, 255)
            mood == SparkMood.THINKING -> Color.rgb(110, 200, 255)
            mood == SparkMood.LISTENING -> Color.rgb(80, 230, 255)
            mood == SparkMood.ALERT -> Color.rgb(230, 120, 200)
            mood == SparkMood.SLEEPY -> Color.rgb(170, 180, 230)
            else -> Color.rgb(160, 200, 255)
        }

        val alphaMult = when {
            isSpeaking -> 0.92f
            mood == SparkMood.HAPPY -> 0.85f
            mood == SparkMood.SLEEPY -> 0.60f
            else -> 0.75f
        }

        drawIridescentWing(canvas, cx - 42f * unit, cy - 6f * unit, -1f, 1f, colorA, colorB, alphaMult, phase, unit, mood, isSpeaking)
        drawIridescentWing(canvas, cx + 42f * unit, cy - 6f * unit,  1f, 1f, colorA, colorB, alphaMult, phase, unit, mood, isSpeaking)
    }

    private fun drawIridescentWing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scaleX: Float,
        scaleY: Float,
        colorA: Int,
        colorB: Int,
        alphaMult: Float,
        phase: Float,
        unit: Float,
        mood: SparkMood,
        isSpeaking: Boolean
    ) {
        val p = Path()

        p.moveTo(cx, cy)
        p.cubicTo(
            cx + 92f * scaleX * unit, cy - 52f * scaleY * unit,
            cx + 128f * scaleX * unit, cy - 105f * scaleY * unit,
            cx + 98f * scaleX * unit, cy - 158f * scaleY * unit
        )
        p.cubicTo(
            cx + 68f * scaleX * unit, cy - 188f * scaleY * unit,
            cx + 22f * scaleX * unit, cy - 168f * scaleY * unit,
            cx, cy - 122f * scaleY * unit
        )
        p.close()

        // Base gradient
        val baseGradient = LinearGradient(
            cx - 50f * scaleX * unit, cy - 170f * scaleY * unit,
            cx + 115f * scaleX * unit, cy + 15f * scaleY * unit,
            intArrayOf(
                Color.argb((255 * alphaMult * 0.65f).toInt(), Color.red(colorA), Color.green(colorA), Color.blue(colorA)),
                Color.argb((255 * alphaMult * 0.92f).toInt(), Color.red(colorB), Color.green(colorB), Color.blue(colorB)),
                Color.argb((255 * alphaMult * 0.70f).toInt(), Color.red(colorA), Color.green(colorA), Color.blue(colorA))
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        // Animated iridescence layer
        val shift = sin(phase) * 28f * unit
        val iridescentGradient = LinearGradient(
            cx - 30f * scaleX * unit + shift, cy - 155f * scaleY * unit,
            cx + 95f * scaleX * unit + shift * 0.6f, cy + 25f * scaleY * unit,
            intArrayOf(
                Color.argb(0, 255, 255, 255),
                Color.argb((255 * alphaMult * 0.38f).toInt(), 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        wingPaint.shader = ComposeShader(baseGradient, iridescentGradient, PorterDuff.Mode.SRC_OVER)
        wingPaint.style = Paint.Style.FILL
        canvas.drawPath(p, wingPaint)

        // Edge highlight
        wingPaint.shader = null
        wingPaint.color = Color.argb((255 * alphaMult * 0.55f).toInt(), 255, 255, 255)
        wingPaint.style = Paint.Style.STROKE
        wingPaint.strokeWidth = 2.8f * unit
        canvas.drawPath(p, wingPaint)

        // Veins
        veinPaint.color = Color.argb((255 * alphaMult * 0.32f).toInt(), 210, 235, 255)
        veinPaint.strokeWidth = 1.15f * unit
        veinPaint.style = Paint.Style.STROKE

        for (i in 0..5) {
            val t = i / 5f
            val startX = cx + (18f + t * 38f) * scaleX * unit
            val startY = cy - (28f + t * 68f) * scaleY * unit
            val endX = cx + (52f + t * 28f) * scaleX * unit
            val endY = cy - (85f + t * 38f) * scaleY * unit
            canvas.drawLine(startX, startY, endX, endY, veinPaint)
        }

        // Inner glow
        glowPaint.shader = RadialGradient(
            cx + 48f * scaleX * unit,
            cy - 95f * scaleY * unit,
            92f * unit,
            Color.argb((255 * alphaMult * 0.28f).toInt(), Color.red(colorB), Color.green(colorB), Color.blue(colorB)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        glowPaint.style = Paint.Style.FILL
        canvas.drawPath(p, glowPaint)

        // Extra shimmer
        if (isSpeaking || mood == SparkMood.HAPPY) {
            val shimmerPhase = phase * 3.4f
            glowPaint.shader = LinearGradient(
                cx - 25f * scaleX * unit + cos(shimmerPhase) * 55f * unit,
                cy - 150f * scaleY * unit,
                cx + 85f * scaleX * unit + sin(shimmerPhase) * 35f * unit,
                cy + 20f * scaleY * unit,
                Color.argb(38, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(p, glowPaint)
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
            cx, cy, width * 0.47f * pulse,
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
        paint.color = Color.argb(ringAlpha, Color.red(base), Color.green(base), Color.blue(base))
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
            repeat(3) { i ->
                val angle = orbit + i * 2.094f
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

    private data class ParticleSeed(val angle: Float, val radius: Float, val speed: Float, val size: Float)

    private val seeds = List(32) {
        ParticleSeed(
            Random.nextFloat() * 6.283f,
            0.14f + Random.nextFloat() * 0.42f,
            0.45f + Random.nextFloat() * 1.9f,
            1.0f + Random.nextFloat() * 2.8f
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

            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawCircle(x, y, seed.size * twinkle + 0.8f, paint)
        }

        val age = System.currentTimeMillis() - burstAt
        if (age in 0..620) {
            val progress = age / 620f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (5.5f * (1f - progress)).coerceAtLeast(0.7f)
            paint.color = Color.argb(((1f - progress) * 220).toInt(), Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawCircle(cx, cy, width * (0.13f + 0.42f * progress), paint)
            paint.style = Paint.Style.FILL
        }

        postInvalidateOnAnimation()
    }
}