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
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var longPressed = false

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

                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    longPressRunnable = Runnable {
                        longPressed = true
                        moved = true
                        isFreeRoam = !isFreeRoam
                        Toast.makeText(
                            this,
                            if (isFreeRoam) "Free-roam mode enabled" else "Free-roam disabled",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (isFreeRoam) {
                            startWandering()
                        } else {
                            stopWandering()
                        }
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

                        try {
                            windowManager?.updateViewLayout(view, p)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { mainHandler.removeCallbacks(it) }

                    if (!moved && !longPressed) {
                        openHome()
                    } else if (!isFreeRoam) {
                        snapToEdge()
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

    private fun startWandering() {
        stopWandering()

        wanderRunnable = object : Runnable {
            override fun run() {
                if (!isFreeRoam || bubbleView == null || params == null) return

                val p = params ?: return
                val view = bubbleView ?: return
                val display = resources.displayMetrics

                val dx = (-36..36).random()
                val dy = (-24..24).random()

                p.x = (p.x + dx).coerceIn(0, display.widthPixels - p.width)
                p.y = (p.y + dy).coerceIn(80, display.heightPixels - p.height - 80)

                if ((0..5).random() == 0) {
                    p.x = if (p.x < display.widthPixels / 2) 8 else display.widthPixels - p.width - 8
                }

                try {
                    windowManager?.updateViewLayout(view, p)
                } catch (_: Exception) {
                }

                mainHandler.postDelayed(this, 1200L)
            }
        }

        mainHandler.postDelayed(wanderRunnable!!, 900L)
    }

    private fun stopWandering() {
        wanderRunnable?.let { mainHandler.removeCallbacks(it) }
        wanderRunnable = null
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