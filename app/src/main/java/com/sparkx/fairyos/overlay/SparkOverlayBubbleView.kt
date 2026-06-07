package com.sparkx.fairyos.overlay

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.sparkx.fairyos.domain.mood.SparkMood
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Premium procedural overlay fairy renderer.
 *
 * Pure Canvas. No assets. No 3D engine.
 * Designed for a small floating WindowManager overlay.
 * Prioritizes clear readable face + character at small overlay size.
 */
class SparkOverlayBubbleView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private var currentMood: SparkMood = SparkMood.IDLE
    private var isSpeaking: Boolean = false
    private var currentForm = com.sparkx.fairyos.domain.personality.SparkForm.DEFAULT_FAIRY

    // Motion & interaction state for v11
    private var motionVx = 0f
    private var motionVy = 0f
    private var isFreeRoamActive = false
    private var isUserTouchActive = false
    private var lastTapBurstTime = 0L

    private var cx = 0f
    private var cy = 0f
    private var unit = 0f

    private data class MoodPalette(
        val aura: Int,
        val aura2: Int,
        val wing: Int,
        val wingEdge: Int,
        val dress: Int,
        val core: Int,
        val eye: Int,
        val accent: Int
    )

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        isLongClickable = true

        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
    }

    fun updateState(mood: SparkMood, speaking: Boolean) {
        updateState(mood, speaking, currentForm)
    }

    fun updateState(mood: SparkMood, speaking: Boolean, form: com.sparkx.fairyos.domain.personality.SparkForm) {
        currentMood = mood
        isSpeaking = speaking
        currentForm = form
        postInvalidateOnAnimation()
    }

    fun updateForm(form: com.sparkx.fairyos.domain.personality.SparkForm) {
        currentForm = form
        postInvalidateOnAnimation()
    }

    // v11 motion & interaction hooks
    fun updateMotion(vx: Float, vy: Float) {
        motionVx = vx.coerceIn(-40f, 40f)
        motionVy = vy.coerceIn(-40f, 40f)
        postInvalidateOnAnimation()
    }

    fun setFreeRoamActive(active: Boolean) {
        isFreeRoamActive = active
        postInvalidateOnAnimation()
    }

    fun setUserTouchActive(active: Boolean) {
        isUserTouchActive = active
        postInvalidateOnAnimation()
    }

    fun triggerTapBurst() {
        lastTapBurstTime = System.currentTimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        unit = min(w, h) / 100f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val now = SystemClock.uptimeMillis()
        val t = now / 1000f
        val palette = paletteFor(currentMood)

        val breath = sin(t * 2.1f) * 0.035f
        val speakPulse = if (isSpeaking) 1f + sin(t * 18f) * 0.08f else 1f
        val bob = sin(t * 1.7f) * 2.4f * unit + motionVy * 0.08f
        val bodyCx = cx + motionVx * 0.06f
        val bodyCy = cy + bob

        drawOuterAura(canvas, palette, t)
        drawGlassBubble(canvas, palette, t)
        drawOrbitRings(canvas, palette, t)
        drawWings(canvas, bodyCx, bodyCy, palette, t, breath)
        drawSparkles(canvas, palette, t)

        // Keep body subtle so it doesn't fight the face
        drawBodyAndDress(canvas, bodyCx, bodyCy, palette, t, speakPulse)

        // Clear readable mini character face (priority layer)
        drawMiniFace(canvas, bodyCx, bodyCy, palette, t, speakPulse)
        drawAntenna(canvas, bodyCx, bodyCy, palette, t)
        drawTapBurst(canvas, palette, t)

        postInvalidateOnAnimation()
    }

    private fun paletteFor(mood: SparkMood): MoodPalette {
        return when (mood) {
            SparkMood.HAPPY -> MoodPalette(
                aura = Color.rgb(0, 229, 255),
                aura2 = Color.rgb(255, 110, 199),
                wing = Color.rgb(190, 245, 255),
                wingEdge = Color.rgb(0, 255, 220),
                dress = Color.rgb(160, 92, 255),
                core = Color.rgb(255, 220, 90),
                eye = Color.rgb(0, 235, 255),
                accent = Color.rgb(255, 110, 199)
            )
            SparkMood.THINKING -> MoodPalette(
                aura = Color.rgb(156, 123, 255),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(190, 210, 255),
                wingEdge = Color.rgb(180, 150, 255),
                dress = Color.rgb(95, 77, 190),
                core = Color.rgb(140, 255, 255),
                eye = Color.rgb(155, 145, 255),
                accent = Color.rgb(0, 229, 255)
            )
            SparkMood.LISTENING -> MoodPalette(
                aura = Color.rgb(0, 255, 156),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(180, 255, 235),
                wingEdge = Color.rgb(0, 255, 156),
                dress = Color.rgb(70, 190, 160),
                core = Color.rgb(0, 255, 156),
                eye = Color.rgb(0, 255, 156),
                accent = Color.rgb(0, 229, 255)
            )
            SparkMood.ALERT -> MoodPalette(
                aura = Color.rgb(255, 80, 120),
                aura2 = Color.rgb(255, 190, 90),
                wing = Color.rgb(255, 205, 220),
                wingEdge = Color.rgb(255, 80, 120),
                dress = Color.rgb(190, 60, 110),
                core = Color.rgb(255, 60, 90),
                eye = Color.rgb(255, 60, 90),
                accent = Color.rgb(255, 190, 90)
            )
            SparkMood.SLEEPY -> MoodPalette(
                aura = Color.rgb(110, 120, 210),
                aura2 = Color.rgb(80, 60, 140),
                wing = Color.rgb(160, 170, 220),
                wingEdge = Color.rgb(120, 110, 210),
                dress = Color.rgb(80, 60, 150),
                core = Color.rgb(150, 150, 255),
                eye = Color.rgb(150, 160, 255),
                accent = Color.rgb(190, 180, 255)
            )
            SparkMood.SPEAKING -> MoodPalette(
                aura = Color.rgb(255, 110, 199),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(210, 245, 255),
                wingEdge = Color.rgb(255, 110, 199),
                dress = Color.rgb(175, 80, 220),
                core = Color.rgb(255, 110, 199),
                eye = Color.rgb(0, 229, 255),
                accent = Color.rgb(255, 230, 120)
            )
            else -> MoodPalette(
                aura = Color.rgb(0, 229, 255),
                aura2 = Color.rgb(156, 123, 255),
                wing = Color.rgb(185, 230, 255),
                wingEdge = Color.rgb(0, 229, 255),
                dress = Color.rgb(105, 76, 154),
                core = Color.rgb(255, 110, 199),
                eye = Color.rgb(0, 229, 255),
                accent = Color.rgb(156, 123, 255)
            )
        }
    }

    private fun drawOuterAura(canvas: Canvas, p: MoodPalette, t: Float) {
        val pulse = 1f + sin(t * if (isSpeaking) 7.5f else 2f) * if (isSpeaking) 0.09f else 0.035f
        val r = 47f * unit * pulse

        glow.style = Paint.Style.FILL
        glow.shader = RadialGradient(
            cx,
            cy,
            r,
            intArrayOf(
                withAlpha(p.aura, if (isSpeaking) 115 else 70),
                withAlpha(p.aura2, if (isSpeaking) 55 else 34),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, glow)
        glow.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.4f * unit
        stroke.color = withAlpha(p.aura, 70)
        canvas.drawCircle(cx, cy, 41f * unit + sin(t * 2.2f) * 1.4f * unit, stroke)

        stroke.strokeWidth = 0.8f * unit
        stroke.color = withAlpha(p.aura2, 55)
        canvas.drawCircle(cx, cy, 34f * unit + cos(t * 1.8f) * 1.2f * unit, stroke)
    }

    private fun drawGlassBubble(canvas: Canvas, p: MoodPalette, t: Float) {
        val pulse = 1f + sin(t * 1.8f) * 0.025f
        val radius = 52f * unit * pulse

        // Subtle glass rim
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2.2f * unit
        stroke.color = withAlpha(Color.WHITE, if (isFreeRoamActive) 55 else 35)
        canvas.drawCircle(cx, cy, radius, stroke)

        // Soft inner refraction highlight
        stroke.strokeWidth = 1.1f * unit
        stroke.color = withAlpha(p.aura, if (isSpeaking || isFreeRoamActive) 70 else 40)
        canvas.drawArc(
            cx - radius * 0.6f, cy - radius * 0.6f,
            cx + radius * 0.6f, cy + radius * 0.6f,
            200f, 80f, false, stroke
        )
    }

    private fun drawOrbitRings(canvas: Canvas, p: MoodPalette, t: Float) {
        canvas.save()
        canvas.rotate(t * 18f, cx, cy)

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 0.9f * unit
        stroke.color = withAlpha(p.aura, 65)
        rect.set(cx - 37f * unit, cy - 22f * unit, cx + 37f * unit, cy + 22f * unit)
        canvas.drawOval(rect, stroke)

        canvas.rotate(68f, cx, cy)
        stroke.color = withAlpha(p.aura2, 45)
        rect.set(cx - 34f * unit, cy - 18f * unit, cx + 34f * unit, cy + 18f * unit)
        canvas.drawOval(rect, stroke)

        canvas.restore()
    }

    private fun drawSparkles(canvas: Canvas, p: MoodPalette, t: Float) {
        paint.style = Paint.Style.FILL

        val count = if (isSpeaking || currentMood == SparkMood.THINKING || isFreeRoamActive) 22 else 13
        for (i in 0 until count) {
            val base = i * 2.399963f
            val spin = t * (0.55f + i * 0.013f) + motionVx * 0.008f
            val radius = (26f + (i % 5) * 4.1f + sin(t * 1.3f + i) * 2.5f) * unit
            val x = cx + cos(base + spin) * radius + motionVx * 0.15f
            val y = cy + sin(base + spin * 0.75f) * radius * 0.82f + motionVy * 0.12f
            val twinkle = ((sin(t * 5.5f + i) + 1f) * 0.5f)
            val alpha = (70 + twinkle * 145).toInt()
            val size = (0.75f + twinkle * 1.6f) * unit

            paint.color = withAlpha(if (i % 2 == 0) p.aura else p.accent, alpha)
            canvas.drawCircle(x, y, size, paint)

            if (i % 4 == 0) {
                stroke.style = Paint.Style.STROKE
                stroke.strokeWidth = 0.55f * unit
                stroke.color = withAlpha(p.aura2, alpha / 2)
                canvas.drawLine(x - size * 2f, y, x + size * 2f, y, stroke)
                canvas.drawLine(x, y - size * 2f, x, y + size * 2f, stroke)
            }
        }
    }

    private fun drawWings(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, breath: Float) {
        val flap = sin(t * if (isSpeaking) 11f else 4.2f) * (if (isSpeaking) 5.5f else 3.2f) * unit
        val sleepyDrop = if (currentMood == SparkMood.SLEEPY) 4f * unit else 0f
        val motionLag = motionVx * 0.12f

        drawWingSide(canvas, x + motionLag * 0.3f, y, p, left = true, flap = flap + motionLag * 0.4f, sleepyDrop = sleepyDrop, breath = breath)
        drawWingSide(canvas, x - motionLag * 0.3f, y, p, left = false, flap = flap - motionLag * 0.4f, sleepyDrop = sleepyDrop, breath = breath)
    }

    private fun drawWingSide(
        canvas: Canvas,
        x: Float,
        y: Float,
        p: MoodPalette,
        left: Boolean,
        flap: Float,
        sleepyDrop: Float,
        breath: Float
    ) {
        val s = if (left) -1f else 1f
        val shoulderX = x + s * 8.5f * unit
        val shoulderY = y - 6f * unit

        val tipX = x + s * (44f * unit + abs(flap) * 0.4f)
        val tipY = y - 29f * unit + flap - sleepyDrop
        val outerX = x + s * 31f * unit
        val outerY = y + 20f * unit + sleepyDrop

        path.reset()
        path.moveTo(shoulderX, shoulderY)
        path.cubicTo(
            x + s * 18f * unit, y - 39f * unit + flap,
            x + s * 39f * unit, y - 42f * unit + flap * 0.7f,
            tipX, tipY
        )
        path.cubicTo(
            x + s * 51f * unit, y - 1f * unit + flap * 0.25f,
            x + s * 30f * unit, y + 35f * unit,
            outerX, outerY
        )
        path.cubicTo(
            x + s * 21f * unit, y + 11f * unit,
            x + s * 14f * unit, y + 3f * unit,
            shoulderX, shoulderY
        )

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x + s * 25f * unit,
            y - 6f * unit,
            42f * unit,
            intArrayOf(
                withAlpha(p.wing, 150),
                withAlpha(p.aura, 76),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.2f * unit
        stroke.color = withAlpha(p.wingEdge, 145)
        canvas.drawPath(path, stroke)

        stroke.strokeWidth = 0.65f * unit
        stroke.color = withAlpha(p.wingEdge, 85)

        for (i in 0..3) {
            val fraction = 0.22f + i * 0.18f
            val vx = shoulderX + (tipX - shoulderX) * fraction
            val vy = shoulderY + (tipY - shoulderY) * fraction + sin(i + breath) * unit
            canvas.drawLine(shoulderX, shoulderY, vx, vy, stroke)
        }

        stroke.color = withAlpha(Color.WHITE, 70)
        stroke.strokeWidth = 0.55f * unit
        canvas.drawLine(
            shoulderX,
            shoulderY,
            x + s * 31f * unit,
            y + 16f * unit,
            stroke
        )
    }

    private fun drawBodyAndDress(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val breathe = 1f + sin(t * 2f) * 0.025f

        glow.style = Paint.Style.FILL
        glow.shader = RadialGradient(
            x,
            y + 14f * unit,
            24f * unit * speakPulse,
            intArrayOf(withAlpha(p.dress, 125), withAlpha(p.core, 40), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(
            x - 17f * unit * breathe,
            y - 2f * unit,
            x + 17f * unit * breathe,
            y + 39f * unit,
            glow
        )
        glow.shader = null

        path.reset()
        path.moveTo(x - 9f * unit, y - 5f * unit)
        path.cubicTo(x - 17f * unit, y + 9f * unit, x - 19f * unit, y + 28f * unit, x - 25f * unit, y + 43f * unit)
        path.quadTo(x, y + 51f * unit, x + 25f * unit, y + 43f * unit)
        path.cubicTo(x + 19f * unit, y + 28f * unit, x + 17f * unit, y + 9f * unit, x + 9f * unit, y - 5f * unit)
        path.cubicTo(x + 4f * unit, y - 10f * unit, x - 4f * unit, y - 10f * unit, x - 9f * unit, y - 5f * unit)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            x,
            y - 10f * unit,
            x,
            y + 48f * unit,
            intArrayOf(
                withAlpha(Color.WHITE, 105),
                withAlpha(p.dress, 235),
                withAlpha(Color.rgb(35, 18, 70), 238)
            ),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1f * unit
        stroke.color = withAlpha(p.aura, 115)
        canvas.drawPath(path, stroke)

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(Color.WHITE, 55)
        canvas.drawOval(x - 6f * unit, y - 2f * unit, x + 6f * unit, y + 20f * unit, paint)

        stroke.strokeWidth = 2.2f * unit
        stroke.color = withAlpha(Color.rgb(230, 210, 255), 130)
        canvas.drawLine(x - 6f * unit, y + 39f * unit, x - 9f * unit, y + 48f * unit, stroke)
        canvas.drawLine(x + 6f * unit, y + 39f * unit, x + 9f * unit, y + 48f * unit, stroke)
    }

    // ============================================================
    // CLEAR MINI CHARACTER FACE (priority for small overlay size)
    // ============================================================

    private fun drawMiniFace(
        canvas: Canvas,
        x: Float,
        y: Float,
        p: MoodPalette,
        t: Float,
        speakPulse: Float
    ) {
        val headR = 19f * unit
        val faceY = y - 4f * unit

        // Face glow / head
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x - 5f * unit,
            faceY - 7f * unit,
            headR * 1.4f,
            intArrayOf(
                withAlpha(Color.WHITE, 235),
                withAlpha(p.core, 230),
                withAlpha(p.dress, 230)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, faceY, headR, paint)
        paint.shader = null

        // Mood blink / sleepy eyes
        val blink = if ((t % 4f) > 3.72f && (t % 4f) < 3.86f) 0.15f else 1f
        val sleepy = currentMood == SparkMood.SLEEPY
        val alert = currentMood == SparkMood.ALERT
        val happy = currentMood == SparkMood.HAPPY
        val thinking = currentMood == SparkMood.THINKING

        val eyeY = faceY - 3f * unit
        val eyeRx = if (alert) 3.4f * unit else 3.0f * unit
        val eyeRy = if (sleepy) 0.9f * unit else 4.2f * unit * blink

        paint.color = Color.rgb(12, 16, 35)

        if (sleepy) {
            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = 1.7f * unit
            stroke.color = Color.rgb(12, 16, 35)
            val left = Path().apply {
                moveTo(x - 9f * unit, eyeY)
                quadTo(x - 6.5f * unit, eyeY + 2f * unit, x - 4f * unit, eyeY)
            }
            val right = Path().apply {
                moveTo(x + 4f * unit, eyeY)
                quadTo(x + 6.5f * unit, eyeY + 2f * unit, x + 9f * unit, eyeY)
            }
            canvas.drawPath(left, stroke)
            canvas.drawPath(right, stroke)
        } else {
            canvas.drawOval(
                x - 9f * unit - eyeRx,
                eyeY - eyeRy,
                x - 9f * unit + eyeRx,
                eyeY + eyeRy,
                paint
            )
            canvas.drawOval(
                x + 9f * unit - eyeRx,
                eyeY - eyeRy,
                x + 9f * unit + eyeRx,
                eyeY + eyeRy,
                paint
            )

            // Eye shine
            paint.color = Color.WHITE
            canvas.drawCircle(x - 7.8f * unit, eyeY - 1.6f * unit, 1.1f * unit, paint)
            canvas.drawCircle(x + 10.2f * unit, eyeY - 1.6f * unit, 1.1f * unit, paint)
        }

        // Mouth
        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeWidth = 1.8f * unit
        stroke.color = Color.rgb(12, 16, 35)

        if (isSpeaking || currentMood == SparkMood.SPEAKING) {
            paint.color = Color.rgb(12, 16, 35)
            canvas.drawOval(
                x - 4.5f * unit * speakPulse,
                faceY + 7f * unit,
                x + 4.5f * unit * speakPulse,
                faceY + 12f * unit,
                paint
            )
        } else {
            val smileDepth = when {
                happy -> 6.5f * unit
                thinking -> 2.5f * unit
                alert -> 1.0f * unit
                sleepy -> 1.5f * unit
                else -> 4f * unit
            }

            path.reset()
            path.moveTo(x - 6f * unit, faceY + 7f * unit)
            path.quadTo(x, faceY + 7f * unit + smileDepth, x + 6f * unit, faceY + 7f * unit)
            canvas.drawPath(path, stroke)
        }

        // Cheeks
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(p.accent, if (happy) 95 else 50)
        canvas.drawCircle(x - 13f * unit, faceY + 5f * unit, 2.4f * unit, paint)
        canvas.drawCircle(x + 13f * unit, faceY + 5f * unit, 2.4f * unit, paint)
    }

    private fun drawAntenna(
        canvas: Canvas,
        x: Float,
        y: Float,
        p: MoodPalette,
        t: Float
    ) {
        val tilt = sin(t * 2.2f) * 2.5f * unit
        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeWidth = 1.7f * unit
        stroke.color = withAlpha(p.accent, 210)

        canvas.drawLine(
            x,
            y - 24f * unit,
            x + tilt,
            y - 39f * unit,
            stroke
        )

        paint.style = Paint.Style.FILL
        paint.color = p.accent
        canvas.drawCircle(x + tilt, y - 41f * unit, 3.5f * unit, paint)

        glow.style = Paint.Style.FILL
        glow.maskFilter = BlurMaskFilter(8f * unit, BlurMaskFilter.Blur.NORMAL)
        glow.color = withAlpha(p.accent, 120)
        canvas.drawCircle(x + tilt, y - 41f * unit, 6f * unit, glow)
        glow.maskFilter = null
    }

    private fun drawTapBurst(canvas: Canvas, p: MoodPalette, t: Float) {
        val age = System.currentTimeMillis() - lastTapBurstTime
        if (age !in 0..450) return

        val progress = age / 450f
        val radius = (18f + 38f * progress) * unit
        val alpha = ((1f - progress) * 170).toInt().coerceIn(0, 170)

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = (2.4f * (1f - progress)).coerceAtLeast(0.5f) * unit
        stroke.color = withAlpha(p.accent, alpha)

        canvas.drawCircle(cx, cy, radius, stroke)

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(p.core, alpha)
        repeat(8) { i ->
            val a = (i / 8f) * (PI * 2f).toFloat() + t * 2f
            val px = cx + cos(a) * radius
            val py = cy + sin(a) * radius
            canvas.drawCircle(px, py, 2f * unit * (1f - progress), paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}