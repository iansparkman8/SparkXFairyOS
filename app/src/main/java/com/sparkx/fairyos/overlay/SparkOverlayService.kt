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

    // Smooth free-roam state
    private var roamTargetX = 0f
    private var roamTargetY = 0f
    private var roamVx = 0f
    private var roamVy = 0f

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
            width = 280
            height = 280
        }

        installTouchControls()

        try {
            windowManager?.addView(bubbleView, params)
        } catch (e: Exception) {
            // Overlay permission not granted
        }
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

                    stopWanderingTickOnly()
                    bubbleView?.setUserTouchActive(true)

                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    longPressRunnable = Runnable {
                        longPressed = true
                        moved = true
                        isFreeRoam = !isFreeRoam
                        bubbleView?.setFreeRoamActive(isFreeRoam)

                        Toast.makeText(
                            this,
                            if (isFreeRoam) "Free-roam mode enabled" else "Free-roam disabled",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (isFreeRoam) {
                            chooseNewRoamTarget()
                            startWandering()
                        } else {
                            stopWandering()
                        }

                        // Refresh notification
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

                        // Calculate velocity for motion illusion
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
                        openHome()
                    } else if (!isFreeRoam) {
                        snapToEdge()
                    } else {
                        // Resume smooth roaming after user drag
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

        return NotificationCompat.Builder(this, "spark_fairy_overlay")
            .setContentTitle("Spark Baby is with you")
            .setContentText(if (isFreeRoam) "Free-roam mode active • Long-press bubble to stop" else "Tap bubble to open SparkX Home")
            .setSmallIcon(android.R.drawable.star_on)
            .setOngoing(true)
            .addAction(0, "Open", openIntent)
            .addAction(0, "Hide", hideIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ==================== SMOOTH FREE-ROAM ====================

    private fun startWandering() {
        stopWandering()

        chooseNewRoamTarget()

        wanderRunnable = object : Runnable {
            override fun run() {
                if (!isFreeRoam || bubbleView == null || params == null || isUserDragging) {
                    mainHandler.postDelayed(this, 16L)
                    return
                }

                val p = params ?: return
                val view = bubbleView ?: return
                val display = resources.displayMetrics

                val safeLeft = 16
                val safeTop = 96
                val safeRight = display.widthPixels - p.width - 16
                val safeBottom = display.heightPixels - p.height - 180

                val dx = roamTargetX - p.x
                val dy = roamTargetY - p.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < 18f) {
                    chooseNewRoamTarget()
                } else {
                    val speed = 0.035f
                    roamVx = (roamVx * 0.86f + dx * speed).coerceIn(-8f, 8f)
                    roamVy = (roamVy * 0.86f + dy * speed).coerceIn(-6f, 6f)

                    p.x = (p.x + roamVx.toInt()).coerceIn(safeLeft, safeRight)
                    p.y = (p.y + roamVy.toInt()).coerceIn(safeTop, safeBottom)

                    bubbleView?.updateMotion(roamVx * 3f, roamVy * 3f)

                    try {
                        windowManager?.updateViewLayout(view, p)
                    } catch (_: Exception) {
                    }
                }

                mainHandler.postDelayed(this, 16L)
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
        val safeBottom = (display.heightPixels - p.height - 190).coerceAtLeast(safeTop)

        val centerBiasX = display.widthPixels * 0.5f
        val centerBiasY = display.heightPixels * 0.42f

        val randomX = (safeLeft..safeRight).random().toFloat()
        val randomY = (safeTop..safeBottom).random().toFloat()

        roamTargetX = (randomX * 0.72f + centerBiasX * 0.28f).coerceIn(safeLeft.toFloat(), safeRight.toFloat())
        roamTargetY = (randomY * 0.72f + centerBiasY * 0.28f).coerceIn(safeTop.toFloat(), safeBottom.toFloat())
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