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
 * Humanoid holographic fairy renderer for the floating overlay.
 * Pure Canvas. Adult proportions. Large wings behind body.
 * No childish mascot bubble.
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
        val skin: Int,
        val hair: Int,
        val shadow: Int,
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

    // === Public API (must remain unchanged) ===
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

        val breath = sin(t * 2.1f) * 0.03f
        val speakPulse = if (isSpeaking) 1f + sin(t * 16f) * 0.07f else 1f
        val bob = sin(t * 1.6f) * 1.8f * unit + motionVy * 0.06f
        val bodyCx = cx + motionVx * 0.05f
        val bodyCy = cy + bob

        // New humanoid draw order
        drawAtmosphericAura(canvas, palette, t)
        drawMotionDustTrail(canvas, palette, t, bodyCx, bodyCy)
        drawHumanoidWings(canvas, bodyCx, bodyCy, palette, t)
        drawHumanoidLegs(canvas, bodyCx, bodyCy, palette, t)
        drawHumanoidArms(canvas, bodyCx, bodyCy, palette, t)
        drawHumanoidTorso(canvas, bodyCx, bodyCy, palette, t)
        drawHumanoidHeadAndHair(canvas, bodyCx, bodyCy, palette, t)
        drawAdultFace(canvas, bodyCx, bodyCy, palette, t, speakPulse)
        drawMagicHandOrb(canvas, bodyCx, bodyCy, palette, t, speakPulse)
        drawTapBurst(canvas, palette, t)

        postInvalidateOnAnimation()
    }

    private fun paletteFor(mood: SparkMood): MoodPalette {
        return when (mood) {
            SparkMood.HAPPY -> MoodPalette(
                aura = Color.rgb(0, 229, 255),
                aura2 = Color.rgb(255, 110, 199),
                wing = Color.rgb(180, 240, 255),
                wingEdge = Color.rgb(0, 255, 220),
                dress = Color.rgb(140, 80, 255),
                core = Color.rgb(255, 215, 80),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(70, 40, 120),
                shadow = Color.rgb(45, 25, 80),
                eye = Color.rgb(30, 40, 90),
                accent = Color.rgb(255, 110, 199)
            )
            SparkMood.THINKING -> MoodPalette(
                aura = Color.rgb(140, 110, 255),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(190, 205, 255),
                wingEdge = Color.rgb(160, 140, 255),
                dress = Color.rgb(85, 70, 180),
                core = Color.rgb(120, 255, 255),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(60, 50, 110),
                shadow = Color.rgb(40, 30, 85),
                eye = Color.rgb(140, 130, 255),
                accent = Color.rgb(0, 229, 255)
            )
            SparkMood.LISTENING -> MoodPalette(
                aura = Color.rgb(0, 255, 160),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(170, 255, 230),
                wingEdge = Color.rgb(0, 255, 170),
                dress = Color.rgb(60, 170, 150),
                core = Color.rgb(0, 255, 160),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(40, 90, 85),
                shadow = Color.rgb(25, 60, 55),
                eye = Color.rgb(0, 255, 170),
                accent = Color.rgb(0, 229, 255)
            )
            SparkMood.ALERT -> MoodPalette(
                aura = Color.rgb(255, 70, 110),
                aura2 = Color.rgb(255, 180, 80),
                wing = Color.rgb(255, 200, 215),
                wingEdge = Color.rgb(255, 70, 110),
                dress = Color.rgb(180, 55, 100),
                core = Color.rgb(255, 80, 100),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(120, 40, 50),
                shadow = Color.rgb(80, 25, 35),
                eye = Color.rgb(255, 80, 100),
                accent = Color.rgb(255, 180, 80)
            )
            SparkMood.SLEEPY -> MoodPalette(
                aura = Color.rgb(100, 110, 200),
                aura2 = Color.rgb(80, 60, 140),
                wing = Color.rgb(155, 165, 215),
                wingEdge = Color.rgb(110, 105, 200),
                dress = Color.rgb(75, 55, 140),
                core = Color.rgb(140, 145, 255),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(80, 70, 130),
                shadow = Color.rgb(55, 45, 95),
                eye = Color.rgb(140, 150, 240),
                accent = Color.rgb(180, 170, 255)
            )
            SparkMood.SPEAKING -> MoodPalette(
                aura = Color.rgb(255, 100, 190),
                aura2 = Color.rgb(0, 229, 255),
                wing = Color.rgb(200, 240, 255),
                wingEdge = Color.rgb(255, 100, 190),
                dress = Color.rgb(160, 70, 200),
                core = Color.rgb(255, 200, 110),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(90, 50, 140),
                shadow = Color.rgb(60, 30, 100),
                eye = Color.rgb(0, 229, 255),
                accent = Color.rgb(255, 220, 110)
            )
            else -> MoodPalette(
                aura = Color.rgb(0, 229, 255),
                aura2 = Color.rgb(140, 110, 255),
                wing = Color.rgb(180, 225, 255),
                wingEdge = Color.rgb(0, 229, 255),
                dress = Color.rgb(95, 70, 145),
                core = Color.rgb(255, 200, 110),
                skin = Color.rgb(255, 226, 205),
                hair = Color.rgb(65, 45, 115),
                shadow = Color.rgb(40, 28, 85),
                eye = Color.rgb(0, 229, 255),
                accent = Color.rgb(140, 110, 255)
            )
        }
    }

    // === Humanoid Drawing Functions ===

    private fun drawAtmosphericAura(canvas: Canvas, p: MoodPalette, t: Float) {
        val pulse = 1f + sin(t * if (isSpeaking) 6f else 1.8f) * if (isSpeaking) 0.08f else 0.03f
        val r = 58f * unit * pulse

        glow.style = Paint.Style.FILL
        glow.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                withAlpha(p.aura, if (isSpeaking) 90 else 55),
                withAlpha(p.aura2, if (isSpeaking) 45 else 28),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, glow)
        glow.shader = null
    }

    private fun drawMotionDustTrail(canvas: Canvas, p: MoodPalette, t: Float, x: Float, y: Float) {
        paint.style = Paint.Style.FILL
        val trailDirX = if (abs(motionVx) > 1.5f) -motionVx.signCompat() * 0.6f else -0.4f
        val trailDirY = if (abs(motionVy) > 1.5f) -motionVy.signCompat() * 0.4f else 0.25f

        repeat(16) { i ->
            val drift = i * 2.8f * unit
            val wave = sin(t * 2.8f + i) * 2.2f * unit
            val px = x + trailDirX * drift + wave * 0.6f
            val py = y + 22f * unit + trailDirY * drift * 0.35f + cos(t * 1.9f + i) * 1.8f * unit
            val twinkle = ((sin(t * 5.5f + i * 1.4f) + 1f) * 0.5f)
            val alpha = (30 + twinkle * 160).toInt().coerceIn(0, 190)

            paint.color = withAlpha(if (i % 2 == 0) p.core else p.accent, alpha)
            canvas.drawCircle(px, py, (0.7f + twinkle * 1.3f) * unit, paint)
        }
    }

    private fun drawHumanoidWings(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val flap = sin(t * if (isSpeaking) 8f else 3.2f) * 4.2f * unit
        drawHumanoidWingSide(canvas, x, y, p, left = true, flap = flap)
        drawHumanoidWingSide(canvas, x, y, p, left = false, flap = -flap)
    }

    private fun drawHumanoidWingSide(
        canvas: Canvas,
        x: Float,
        y: Float,
        p: MoodPalette,
        left: Boolean,
        flap: Float
    ) {
        val s = if (left) -1f else 1f
        val shoulderX = x + s * 7f * unit
        val shoulderY = y - 8f * unit

        // Upper wing
        path.reset()
        path.moveTo(shoulderX, shoulderY)
        path.cubicTo(
            x + s * 14f * unit, y - 38f * unit + flap * 0.6f,
            x + s * 38f * unit, y - 42f * unit + flap * 0.3f,
            x + s * 52f * unit, y - 18f * unit + flap
        )
        path.cubicTo(
            x + s * 42f * unit, y - 8f * unit,
            x + s * 22f * unit, y + 6f * unit,
            shoulderX, shoulderY
        )

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x + s * 28f * unit, y - 22f * unit, 38f * unit,
            intArrayOf(withAlpha(p.wing, 165), withAlpha(p.aura, 70), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.1f * unit
        stroke.color = withAlpha(p.wingEdge, 155)
        canvas.drawPath(path, stroke)

        // Veins
        stroke.strokeWidth = 0.6f * unit
        stroke.color = withAlpha(p.wingEdge, 90)
        for (i in 0..4) {
            val frac = 0.18f + i * 0.16f
            val vx = shoulderX + (x + s * 52f * unit - shoulderX) * frac
            val vy = shoulderY + (y - 18f * unit + flap - shoulderY) * frac + sin(i + flap * 0.02f) * unit
            canvas.drawLine(shoulderX, shoulderY, vx, vy, stroke)
        }

        // Lower wing (smaller)
        path.reset()
        path.moveTo(shoulderX + s * 3f * unit, shoulderY + 8f * unit)
        path.cubicTo(
            x + s * 18f * unit, y + 22f * unit,
            x + s * 32f * unit, y + 38f * unit,
            x + s * 28f * unit, y + 48f * unit
        )
        path.cubicTo(
            x + s * 18f * unit, y + 32f * unit,
            x + s * 8f * unit, y + 18f * unit,
            shoulderX + s * 3f * unit, shoulderY + 8f * unit
        )

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x + s * 18f * unit, y + 28f * unit, 26f * unit,
            intArrayOf(withAlpha(p.wing, 130), withAlpha(p.aura, 55), Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        stroke.strokeWidth = 0.9f * unit
        stroke.color = withAlpha(p.wingEdge, 120)
        canvas.drawPath(path, stroke)
    }

    private fun drawHumanoidLegs(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val kick = sin(t * 1.7f) * 1.8f * unit
        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeWidth = 2.4f * unit
        stroke.color = withAlpha(p.skin, 210)

        canvas.drawLine(x - 5f * unit, y + 38f * unit, x - 15f * unit, y + 62f * unit + kick, stroke)
        canvas.drawLine(x + 5f * unit, y + 38f * unit, x + 16f * unit, y + 60f * unit - kick, stroke)

        // Small feet
        stroke.strokeWidth = 2.0f * unit
        canvas.drawLine(x - 15f * unit, y + 62f * unit + kick, x - 20f * unit, y + 65f * unit + kick * 0.5f, stroke)
        canvas.drawLine(x + 16f * unit, y + 60f * unit - kick, x + 21f * unit, y + 63f * unit - kick * 0.5f, stroke)
    }

    private fun drawHumanoidArms(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val sway = sin(t * 2.3f) * 2.2f * unit
        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeWidth = 2.1f * unit
        stroke.color = withAlpha(p.skin, 215)

        canvas.drawLine(x - 7f * unit, y - 5f * unit, x - 24f * unit, y + 9f * unit + sway, stroke)
        canvas.drawLine(x + 7f * unit, y - 5f * unit, x + 23f * unit, y + 8f * unit - sway, stroke)
    }

    private fun drawHumanoidTorso(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val breathe = 1f + sin(t * 2f) * 0.022f

        path.reset()
        path.moveTo(x - 7f * unit, y - 12f * unit)
        path.cubicTo(x - 14f * unit, y - 2f * unit, x - 15f * unit, y + 18f * unit, x - 9f * unit, y + 40f * unit)
        path.quadTo(x, y + 48f * unit, x + 9f * unit, y + 40f * unit)
        path.cubicTo(x + 15f * unit, y + 18f * unit, x + 14f * unit, y - 2f * unit, x + 7f * unit, y - 12f * unit)
        path.close()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            x, y - 12f * unit, x, y + 48f * unit,
            intArrayOf(withAlpha(Color.WHITE, 115), withAlpha(p.dress, 248), withAlpha(p.shadow, 245)),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.0f * unit
        stroke.color = withAlpha(p.wingEdge, 110)
        canvas.drawPath(path, stroke)
    }

    private fun drawHumanoidHeadAndHair(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float) {
        val headY = y - 20f * unit
        val headR = 15f * unit

        // Head (oval for more adult look)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x - 3f * unit, headY - 5f * unit, headR * 1.35f,
            intArrayOf(withAlpha(Color.WHITE, 250), withAlpha(p.skin, 245), withAlpha(p.shadow, 180)),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(x - headR * 0.92f, headY - headR * 1.05f, x + headR * 0.92f, headY + headR * 0.95f, paint)
        paint.shader = null

        // Hair
        paint.color = withAlpha(p.hair, 235)
        canvas.drawOval(x - 11f * unit, headY - 18f * unit, x + 11f * unit, headY + 2f * unit, paint)

        path.reset()
        path.moveTo(x - 11f * unit, headY - 6f * unit)
        path.cubicTo(x - 7f * unit, headY - 17f * unit, x + 7f * unit, headY - 17f * unit, x + 11f * unit, headY - 6f * unit)
        path.cubicTo(x + 5f * unit, headY - 2f * unit, x - 5f * unit, headY - 2f * unit, x - 11f * unit, headY - 6f * unit)
        canvas.drawPath(path, paint)
    }

    private fun drawAdultFace(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val faceY = y - 20f * unit
        val blink = if ((t % 3.8f) > 3.55f && (t % 3.8f) < 3.68f) 0.18f else 1f
        val sleepy = currentMood == SparkMood.SLEEPY

        // Eyes (smaller, more adult)
        paint.color = withAlpha(p.eye, 245)
        if (sleepy) {
            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = 1.6f * unit
            stroke.color = withAlpha(p.eye, 230)
            canvas.drawLine(x - 5.5f * unit, faceY - 1f * unit, x - 2.5f * unit, faceY - 1f * unit, stroke)
            canvas.drawLine(x + 2.5f * unit, faceY - 1f * unit, x + 5.5f * unit, faceY - 1f * unit, stroke)
        } else {
            val eyeScale = if (currentMood == SparkMood.ALERT) 1.15f else 1f
            canvas.drawOval(x - 5.8f * unit, faceY - 2.2f * unit * eyeScale * blink, x - 2.2f * unit, faceY + 2.0f * unit * eyeScale * blink, paint)
            canvas.drawOval(x + 2.2f * unit, faceY - 2.2f * unit * eyeScale * blink, x + 5.8f * unit, faceY + 2.0f * unit * eyeScale * blink, paint)

            // Eye shine
            paint.color = Color.WHITE
            canvas.drawCircle(x - 4.8f * unit, faceY - 0.9f * unit, 0.9f * unit, paint)
            canvas.drawCircle(x + 3.2f * unit, faceY - 0.9f * unit, 0.9f * unit, paint)
        }

        // Subtle mouth
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = 1.3f * unit
        stroke.color = withAlpha(p.shadow, 200)

        when {
            isSpeaking || currentMood == SparkMood.SPEAKING -> {
                val open = 2.8f * unit * speakPulse
                canvas.drawOval(x - 2.8f * unit, faceY + 5.5f * unit, x + 2.8f * unit, faceY + 9.5f * unit, stroke)
            }
            currentMood == SparkMood.HAPPY -> {
                canvas.drawArc(x - 3.5f * unit, faceY + 4.5f * unit, x + 3.5f * unit, faceY + 9.5f * unit, 15f, 150f, false, stroke)
            }
            else -> {
                canvas.drawLine(x - 2.2f * unit, faceY + 7f * unit, x + 2.2f * unit, faceY + 7f * unit, stroke)
            }
        }
    }

    private fun drawMagicHandOrb(canvas: Canvas, x: Float, y: Float, p: MoodPalette, t: Float, speakPulse: Float) {
        val pulse = 1f + sin(t * if (isSpeaking) 11f else 3.2f) * 0.14f
        val orbY = y + 11f * unit

        glow.style = Paint.Style.FILL
        glow.maskFilter = BlurMaskFilter(9f * unit, BlurMaskFilter.Blur.NORMAL)
        glow.color = withAlpha(p.core, if (isSpeaking) 180 else 120)
        canvas.drawCircle(x, orbY, 7.5f * unit * pulse * speakPulse, glow)
        glow.maskFilter = null

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            x - 1.5f * unit, orbY - 1.5f * unit, 7f * unit,
            intArrayOf(Color.WHITE, withAlpha(p.core, 245), withAlpha(p.aura2, 200)),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, orbY, 3.8f * unit * pulse, paint)
        paint.shader = null
    }

    private fun drawTapBurst(canvas: Canvas, p: MoodPalette, t: Float) {
        val age = System.currentTimeMillis() - lastTapBurstTime
        if (age !in 0..420) return

        val progress = age / 420f
        val radius = (16f + 36f * progress) * unit
        val alpha = ((1f - progress) * 165).toInt().coerceIn(0, 165)

        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = (2.2f * (1f - progress)).coerceAtLeast(0.6f) * unit
        stroke.color = withAlpha(p.accent, alpha)
        canvas.drawCircle(cx, cy, radius, stroke)

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(p.core, alpha)
        repeat(7) { i ->
            val a = (i / 7f) * (PI * 2f).toFloat() + t * 1.8f
            val px = cx + cos(a) * radius
            val py = cy + sin(a) * radius
            canvas.drawCircle(px, py, 1.8f * unit * (1f - progress), paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun Float.signCompat(): Float = if (this >= 0f) 1f else -1f
}