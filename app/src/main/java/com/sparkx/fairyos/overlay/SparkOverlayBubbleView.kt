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
        drawSparkles(canvas, palette, t)
        drawWings(canvas, bodyCx, bodyCy, palette, t, breath)
        drawBodyAndDress(canvas, bodyCx, bodyCy, palette, t, speakPulse)
        drawArms(canvas, bodyCx, bodyCy, palette, t)
        drawHeadHairFace(canvas, bodyCx, bodyCy, palette, t, speakPulse)
        drawCrownAndHalo(canvas, bodyCx, bodyCy, palette, t)
        drawCore(canvas, bodyCx, bodyCy, palette, t, speakPulse)

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

    private fun drawArms(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val wave = if (isSpeaking || currentMood == SparkMood.HAPPY) sin(t * 8f) * 2.7f * unit else 0f
        val motionWave = motionVx * 0.25f

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 2.1f * unit
        stroke.color = Color.argb(205, 255, 220, 200)

        path.reset()
        path.moveTo(x - 10f * unit, y + 5f * unit)
        path.cubicTo(x - 19f * unit, y + 10f * unit, x - 23f * unit, y + 18f * unit, x - 27f * unit, y + 22f * unit + wave + motionWave)
        canvas.drawPath(path, stroke)

        path.reset()
        path.moveTo(x + 10f * unit, y + 5f * unit)
        path.cubicTo(x + 19f * unit, y + 10f * unit, x + 23f * unit, y + 18f * unit, x + 27f * unit, y + 22f * unit - wave - motionWave)
        canvas.drawPath(path, stroke)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(220, 255, 225, 205)
        canvas.drawCircle(x - 27f * unit, y + 22f * unit + wave + motionWave, 1.8f * unit, paint)
        canvas.drawCircle(x + 27f * unit, y + 22f * unit - wave - motionWave, 1.8f * unit, paint)

        paint.color = withAlpha(p.accent, if (isSpeaking) 185 else 95)
        canvas.drawCircle(x + 31f * unit, y + 18f * unit - wave, 1.5f * unit, paint)
    }

    private fun drawHeadHairFace(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val headY = y - 22f * unit
        val headR = 16f * unit
        val skinLight = Color.rgb(255, 226, 205)
        val skinShadow = Color.rgb(208, 155, 180)

        path.reset()
        path.moveTo(x - 17f * unit, headY - 9f * unit)
        path.cubicTo(x - 25f * unit, headY - 4f * unit, x - 23f * unit, headY + 17f * unit, x - 15f * unit, headY + 21f * unit)
        path.cubicTo(x - 10f * unit, headY + 31f * unit, x + 10f * unit, headY + 31f * unit, x + 15f * unit, headY + 21f * unit)
        path.cubicTo(x + 23f * unit, headY + 17f * unit, x + 25f * unit, headY - 4f * unit, x + 17f * unit, headY - 9f * unit)
        path.cubicTo(x + 11f * unit, headY - 22f * unit, x - 11f * unit, headY - 22f * unit, x - 17f * unit, headY - 9f * unit)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            x,
            headY - 23f * unit,
            x,
            headY + 32f * unit,
            intArrayOf(
                Color.argb(245, 45, 23, 80),
                Color.argb(240, 95, 40, 145),
                Color.argb(220, 255, 110, 199)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x - 5f * unit,
            headY - 6f * unit,
            headR * 1.45f,
            intArrayOf(skinLight, Color.rgb(247, 198, 190), skinShadow),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(
            x - headR * 0.9f,
            headY - headR,
            x + headR * 0.9f,
            headY + headR * 1.05f,
            paint
        )
        paint.shader = null

        paint.color = Color.argb(235, 70, 28, 115)
        path.reset()
        path.moveTo(x - 15f * unit, headY - 10f * unit)
        path.cubicTo(x - 7f * unit, headY - 22f * unit, x + 6f * unit, headY - 22f * unit, x + 15f * unit, headY - 11f * unit)
        path.cubicTo(x + 5f * unit, headY - 8f * unit, x - 2f * unit, headY - 5f * unit, x - 10f * unit, headY - 1f * unit)
        path.cubicTo(x - 9f * unit, headY - 5f * unit, x - 12f * unit, headY - 8f * unit, x - 15f * unit, headY - 10f * unit)
        canvas.drawPath(path, paint)

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 0.75f * unit
        stroke.color = Color.argb(120, 255, 160, 220)
        for (i in -2..2) {
            val sx = x + i * 4f * unit
            path.reset()
            path.moveTo(sx, headY - 17f * unit)
            path.cubicTo(sx + i * unit, headY - 5f * unit, sx - 2f * unit, headY + 9f * unit, sx + i * 1.3f * unit, headY + 22f * unit)
            canvas.drawPath(path, stroke)
        }

        drawEyes(canvas, x, headY, p, t)
        drawMouth(canvas, x, headY, p, t, speakPulse)
    }

    private fun drawEyes(canvas: Canvas, x: Float, headY: Float, p: MoodPalette, t: Float) {
        val eyeY = headY - 1f * unit
        val leftX = x - 5.8f * unit
        val rightX = x + 5.8f * unit

        when (currentMood) {
            SparkMood.SLEEPY -> {
                stroke.style = Paint.Style.STROKE
                stroke.strokeWidth = 1.25f * unit
                stroke.color = Color.argb(210, 45, 25, 55)
                canvas.drawArc(leftX - 4f * unit, eyeY - 2f * unit, leftX + 4f * unit, eyeY + 4f * unit, 15f, 145f, false, stroke)
                canvas.drawArc(rightX - 4f * unit, eyeY - 2f * unit, rightX + 4f * unit, eyeY + 4f * unit, 20f, 145f, false, stroke)
                return
            }
            SparkMood.ALERT -> {
                drawSingleEye(canvas, leftX, eyeY - 0.6f * unit, p.eye, scale = 1.18f)
                drawSingleEye(canvas, rightX, eyeY - 0.6f * unit, p.eye, scale = 1.18f)
            }
            else -> {
                val blink = if ((sin(t * 1.1f) > 0.985f) && !isSpeaking) 0.22f else 1f
                drawSingleEye(canvas, leftX, eyeY, p.eye, scale = blink)
                drawSingleEye(canvas, rightX, eyeY, p.eye, scale = blink)
            }
        }
    }

    private fun drawSingleEye(canvas: Canvas, x: Float, y: Float, color: Int, scale: Float) {
        val w = 4.1f * unit
        val h = 5.0f * unit * scale

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(238, 255, 255, 255)
        canvas.drawOval(x - w, y - h, x + w, y + h, paint)

        paint.shader = RadialGradient(
            x,
            y,
            4.5f * unit,
            intArrayOf(Color.WHITE, color, Color.rgb(20, 30, 55)),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, 2.55f * unit * scale.coerceAtLeast(0.4f), paint)
        paint.shader = null

        paint.color = Color.rgb(10, 10, 20)
        canvas.drawCircle(x, y, 1.25f * unit * scale.coerceAtLeast(0.4f), paint)

        paint.color = Color.WHITE
        canvas.drawCircle(x - 0.9f * unit, y - 1.1f * unit, 0.55f * unit, paint)
    }

    private fun drawMouth(canvas: Canvas, x: Float, headY: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val mouthY = headY + 8.6f * unit

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(190, 120, 40, 70)

        when {
            isSpeaking -> {
                val open = (2.2f + (sin(t * 20f) + 1f) * 2.1f) * unit * speakPulse
                canvas.drawOval(
                    x - 3.4f * unit,
                    mouthY - open * 0.55f,
                    x + 3.4f * unit,
                    mouthY + open,
                    paint
                )
                paint.color = withAlpha(p.accent, 120)
                canvas.drawOval(
                    x - 2.2f * unit,
                    mouthY - open * 0.1f,
                    x + 2.2f * unit,
                    mouthY + open * 0.5f,
                    paint
                )
            }
            currentMood == SparkMood.HAPPY -> {
                stroke.style = Paint.Style.STROKE
                stroke.strokeWidth = 1.05f * unit
                stroke.color = Color.argb(180, 110, 45, 65)
                canvas.drawArc(
                    x - 4f * unit,
                    mouthY - 2f * unit,
                    x + 4f * unit,
                    mouthY + 4f * unit,
                    15f,
                    150f,
                    false,
                    stroke
                )
            }
            currentMood == SparkMood.ALERT -> {
                canvas.drawCircle(x, mouthY, 1.6f * unit, paint)
            }
            else -> {
                stroke.style = Paint.Style.STROKE
                stroke.strokeWidth = 0.85f * unit
                stroke.color = Color.argb(145, 110, 45, 65)
                canvas.drawLine(x - 2.6f * unit, mouthY, x + 2.6f * unit, mouthY, stroke)
            }
        }
    }

    private fun drawCrownAndHalo(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val headY = y - 22f * unit
        val crownY = headY - 18f * unit
        val pulse = 1f + sin(t * 5f) * 0.08f

        canvas.save()
        canvas.rotate(t * 16f, x, crownY)
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1f * unit
        stroke.color = withAlpha(p.accent, if (isSpeaking) 175 else 105)
        rect.set(
            x - 13f * unit * pulse,
            crownY - 5f * unit,
            x + 13f * unit * pulse,
            crownY + 5f * unit
        )
        canvas.drawOval(rect, stroke)
        canvas.restore()

        stroke.strokeWidth = 1.6f * unit
        stroke.color = withAlpha(Color.rgb(255, 225, 120), 210)
        path.reset()
        path.moveTo(x - 9f * unit, crownY + 5f * unit)
        path.lineTo(x - 5f * unit, crownY - 3f * unit)
        path.lineTo(x, crownY + 2f * unit)
        path.lineTo(x + 5f * unit, crownY - 3f * unit)
        path.lineTo(x + 9f * unit, crownY + 5f * unit)
        canvas.drawPath(path, stroke)

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(p.core, 220)
        canvas.drawCircle(x, crownY - 2f * unit, 1.9f * unit, paint)
        canvas.drawCircle(x - 5f * unit, crownY - 2f * unit, 1.25f * unit, paint)
        canvas.drawCircle(x + 5f * unit, crownY - 2f * unit, 1.25f * unit, paint)
    }

    private fun drawCore(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val coreX = x
        val coreY = y + 11f * unit
        val pulse = (1f + sin(t * if (isSpeaking) 13f else 3.5f) * 0.16f) * speakPulse

        glow.style = Paint.Style.FILL
        glow.maskFilter = BlurMaskFilter(12f * unit, BlurMaskFilter.Blur.NORMAL)
        glow.color = withAlpha(p.core, if (isSpeaking) 210 else 150)
        canvas.drawCircle(coreX, coreY, 7f * unit * pulse, glow)
        glow.maskFilter = null

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            coreX - 1f * unit,
            coreY - 1f * unit,
            7f * unit,
            intArrayOf(Color.WHITE, p.core, withAlpha(p.aura2, 230)),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(coreX, coreY, 4.4f * unit * pulse, paint)
        paint.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 0.8f * unit
        stroke.color = withAlpha(Color.WHITE, 130)
        canvas.drawCircle(coreX, coreY, 6.2f * unit * pulse, stroke)
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