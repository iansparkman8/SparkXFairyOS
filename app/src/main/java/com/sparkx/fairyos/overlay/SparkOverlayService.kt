package com.sparkx.fairyos.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sparkx.fairyos.R
import com.sparkx.fairyos.domain.mood.SparkMood
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SparkOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: SparkOverlayBubbleView? = null
    private var params: WindowManager.LayoutParams? = null

    private var currentMood = SparkMood.IDLE
    private var isSpeaking = false
    private var isFreeRoam = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var wanderRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null

    // Touch & drag state
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var longPressed = false
    private var isUserDragging = false
    private var lastMoveRawX = 0f
    private var lastMoveRawY = 0f
    private var lastMoveTime = 0L

    // Smooth free-roam state (float precision)
    private var roamFloatX = 100f
    private var roamFloatY = 300f
    private var roamTargetX = 0f
    private var roamTargetY = 0f
    private var roamVx = 0f
    private var roamVy = 0f
    private var roamStuckFrames = 0
    private var lastAssignedX = Int.MIN_VALUE
    private var lastAssignedY = Int.MIN_VALUE

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        bubbleView = SparkOverlayBubbleView(this)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
            width = dp(144)
            height = dp(144)
        }

        installTouchControls()

        try {
            windowManager?.addView(bubbleView, params)
        } catch (e: Exception) {
            // Overlay permission not granted
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun installTouchControls() {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        bubbleView?.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener true
            val view = bubbleView ?: return@setOnTouchListener true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = p.x
                    startY = p.y
                    moved = false
                    longPressed = false
                    isUserDragging = true
                    lastMoveRawX = event.rawX
                    lastMoveRawY = event.rawY
                    lastMoveTime = System.currentTimeMillis()

                    roamFloatX = p.x.toFloat()
                    roamFloatY = p.y.toFloat()

                    stopWanderingTickOnly()
                    bubbleView?.setUserTouchActive(true)

                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    longPressRunnable = Runnable {
                        longPressed = true
                        moved = true
                        toggleFreeRoam()

                        Toast.makeText(
                            this,
                            if (isFreeRoam) "Free-roam mode enabled" else "Free-roam disabled",
                            Toast.LENGTH_SHORT
                        ).show()

                        val nm = getSystemService(NotificationManager::class.java)
                        nm.notify(1, createNotification())
                    }
                    mainHandler.postDelayed(longPressRunnable!!, 550L)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        moved = true
                        longPressRunnable?.let { mainHandler.removeCallbacks(it) }

                        val display = resources.displayMetrics
                        p.x = (startX + dx.toInt()).coerceIn(0, display.widthPixels - p.width)
                        p.y = (startY + dy.toInt()).coerceIn(0, display.heightPixels - p.height)

                        val now = System.currentTimeMillis()
                        val dt = (now - lastMoveTime).coerceAtLeast(1).toFloat()
                        val vx = ((event.rawX - lastMoveRawX) / dt * 16f).coerceIn(-40f, 40f)
                        val vy = ((event.rawY - lastMoveRawY) / dt * 16f).coerceIn(-40f, 40f)

                        bubbleView?.updateMotion(vx, vy)

                        lastMoveRawX = event.rawX
                        lastMoveRawY = event.rawY
                        lastMoveTime = now

                        try {
                            windowManager?.updateViewLayout(view, p)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    isUserDragging = false
                    bubbleView?.setUserTouchActive(false)
                    bubbleView?.updateMotion(0f, 0f)

                    if (!moved && !longPressed) {
                        bubbleView?.triggerTapBurst()
                        openHome()
                    } else if (!isFreeRoam) {
                        snapToEdge()
                    } else {
                        roamFloatX = p.x.toFloat()
                        roamFloatY = p.y.toFloat()
                        chooseNewRoamTarget()
                        startWandering()
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun openHome() {
        val intent = Intent(this, com.sparkx.fairyos.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun snapToEdge() {
        val p = params ?: return
        val view = bubbleView ?: return
        val display = resources.displayMetrics
        val mid = display.widthPixels / 2

        p.x = if (p.x + p.width / 2 < mid) {
            8
        } else {
            display.widthPixels - p.width - 8
        }

        try {
            windowManager?.updateViewLayout(view, p)
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "spark_fairy_overlay",
                "Spark Baby Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "START_OVERLAY" -> {
                startForeground(1, createNotification())
            }
            "STOP_OVERLAY" -> {
                stopSelf()
            }
            "UPDATE_MOOD" -> {
                val moodName = intent.getStringExtra("mood")
                currentMood = try { SparkMood.valueOf(moodName ?: "IDLE") } catch (e: Exception) { SparkMood.IDLE }
                bubbleView?.updateState(currentMood, isSpeaking)
            }
            "UPDATE_SPEAKING" -> {
                isSpeaking = intent.getBooleanExtra("speaking", false)
                bubbleView?.updateState(currentMood, isSpeaking)
            }
            "TOGGLE_FREE_ROAM" -> {
                toggleFreeRoam()
            }
            "SET_FREE_ROAM" -> {
                setFreeRoamEnabled(intent.getBooleanExtra("enabled", false))
            }
        }

        if (action == "START_OVERLAY" || action == null) {
            startForeground(1, createNotification())
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.sparkx.fairyos.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SparkOverlayService::class.java).apply { action = "STOP_OVERLAY" },
            PendingIntent.FLAG_IMMUTABLE
        )

        val roamIntent = PendingIntent.getService(
            this, 2,
            Intent(this, SparkOverlayService::class.java).apply {
                action = "TOGGLE_FREE_ROAM"
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "spark_fairy_overlay")
            .setContentTitle("Spark Baby is with you")
            .setContentText(
                if (isFreeRoam) {
                    "Free-roam mode active"
                } else {
                    "Tap bubble to open SparkX Home"
                }
            )
            .setSmallIcon(android.R.drawable.star_on)
            .setOngoing(true)
            .addAction(0, "Open", openIntent)
            .addAction(0, if (isFreeRoam) "Stop Roam" else "Free Roam", roamIntent)
            .addAction(0, "Hide", hideIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ==================== FREE-ROAM HELPERS ====================

    private fun toggleFreeRoam() {
        setFreeRoamEnabled(!isFreeRoam)
    }

    private fun setFreeRoamEnabled(enabled: Boolean) {
        isFreeRoam = enabled
        bubbleView?.setFreeRoamActive(isFreeRoam)

        if (isFreeRoam) {
            params?.let {
                roamFloatX = it.x.toFloat()
                roamFloatY = it.y.toFloat()
            }
            chooseNewRoamTarget()
            startWandering()
        } else {
            stopWandering()
            snapToEdge()
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, createNotification())
    }

    // ==================== SMOOTH FLOAT-BASED FREE-ROAM ====================

    private fun startWandering() {
        stopWandering()

        val p = params ?: return
        roamFloatX = p.x.toFloat()
        roamFloatY = p.y.toFloat()
        roamVx = 0f
        roamVy = 0f
        roamStuckFrames = 0
        lastAssignedX = p.x
        lastAssignedY = p.y

        chooseNewRoamTarget()

        wanderRunnable = object : Runnable {
            override fun run() {
                if (!isFreeRoam || bubbleView == null || params == null) return

                if (!isUserDragging) {
                    val p = params ?: return
                    val view = bubbleView ?: return
                    val display = resources.displayMetrics

                    val safeLeft = 16f
                    val safeTop = 96f
                    val safeRight = (display.widthPixels - p.width - 16).coerceAtLeast(16).toFloat()
                    val safeBottom = (display.heightPixels - p.height - 210).coerceAtLeast(96).toFloat()

                    val dx = roamTargetX - roamFloatX
                    val dy = roamTargetY - roamFloatY
                    val dist = sqrt(dx * dx + dy * dy)

                    if (dist < 22f || roamStuckFrames > 90) {
                        chooseNewRoamTarget()
                        roamStuckFrames = 0
                    } else {
                        val accel = 0.026f
                        val damping = 0.90f

                        roamVx = (roamVx * damping + dx * accel).coerceIn(-8.0f, 8.0f)
                        roamVy = (roamVy * damping + dy * accel).coerceIn(-6.5f, 6.5f)

                        // Minimum visual movement floor so it cannot silently round to zero
                        if (abs(roamVx) < 0.65f && abs(dx) > 24f) {
                            roamVx = if (dx > 0f) 0.65f else -0.65f
                        }
                        if (abs(roamVy) < 0.50f && abs(dy) > 24f) {
                            roamVy = if (dy > 0f) 0.50f else -0.50f
                        }

                        roamFloatX = (roamFloatX + roamVx).coerceIn(safeLeft, safeRight)
                        roamFloatY = (roamFloatY + roamVy).coerceIn(safeTop, safeBottom)

                        val nextX = roamFloatX.roundToInt()
                        val nextY = roamFloatY.roundToInt()

                        if (nextX == lastAssignedX && nextY == lastAssignedY) {
                            roamStuckFrames++
                        } else {
                            roamStuckFrames = 0
                        }

                        p.x = nextX
                        p.y = nextY
                        lastAssignedX = nextX
                        lastAssignedY = nextY

                        bubbleView?.updateMotion(roamVx * 5f, roamVy * 5f)

                        try {
                            windowManager?.updateViewLayout(view, p)
                        } catch (_: Exception) {
                        }
                    }
                }

                mainHandler.postDelayed(this, 33L)
            }
        }

        mainHandler.post(wanderRunnable!!)
    }

    private fun stopWandering() {
        wanderRunnable?.let { mainHandler.removeCallbacks(it) }
        wanderRunnable = null
        roamVx = 0f
        roamVy = 0f
        bubbleView?.updateMotion(0f, 0f)
    }

    private fun stopWanderingTickOnly() {
        wanderRunnable?.let { mainHandler.removeCallbacks(it) }
        wanderRunnable = null
    }

    private fun chooseNewRoamTarget() {
        val p = params ?: return
        val display = resources.displayMetrics

        val safeLeft = 24
        val safeTop = 110
        val safeRight = (display.widthPixels - p.width - 24).coerceAtLeast(safeLeft)
        val safeBottom = (display.heightPixels - p.height - 210).coerceAtLeast(safeTop)

        val currentX = p.x
        val currentY = p.y

        var candidateX: Int
        var candidateY: Int
        var attempts = 0

        do {
            candidateX = (safeLeft..safeRight).random()
            candidateY = (safeTop..safeBottom).random()
            attempts++
        } while (
            attempts < 8 &&
            abs(candidateX - currentX) < 120 &&
            abs(candidateY - currentY) < 90
        )

        roamTargetX = candidateX.toFloat()
        roamTargetY = candidateY.toFloat()
    }

    override fun onDestroy() {
        stopWandering()
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable = null

        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }

        bubbleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}