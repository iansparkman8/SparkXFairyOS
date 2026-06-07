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
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sparkx.fairyos.R
import com.sparkx.fairyos.domain.mood.SparkMood

class SparkOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: SparkOverlayBubbleView? = null
    private var params: WindowManager.LayoutParams? = null

    private var currentMood = SparkMood.IDLE
    private var isSpeaking = false
    private var isFreeRoam = false

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
            width = 220
            height = 220
        }

        // Long press to toggle free roam
        bubbleView?.setOnLongClickListener {
            isFreeRoam = !isFreeRoam
            Toast.makeText(this, if (isFreeRoam) "Free-roam mode enabled" else "Free-roam disabled", Toast.LENGTH_SHORT).show()
            if (isFreeRoam) startWandering()
            true
        }

        // Drag support (simple)
        bubbleView?.setOnTouchListener { v, event ->
            // Basic drag implementation omitted for brevity in v7; full version would track ACTION_MOVE and update params.x/y
            // For now, tap to open main app
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val intent = Intent(this, com.sparkx.fairyos.MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            false
        }

        try {
            windowManager?.addView(bubbleView, params)
        } catch (e: Exception) {
            // Overlay permission not granted
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
            .setSmallIcon(android.R.drawable.star_on) // Replace with proper icon in v8
            .setOngoing(true)
            .addAction(0, "Open", openIntent)
            .addAction(0, "Hide", hideIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startWandering() {
        // Simple wandering thread for v7
        Thread {
            while (isFreeRoam && bubbleView != null) {
                Thread.sleep(1200)
                params?.let { p ->
                    p.x = (p.x + (-40..40).random()).coerceIn(0, 800)
                    p.y = (p.y + (-30..30).random()).coerceIn(100, 1200)
                    // Edge snap logic could be added
                    windowManager?.updateViewLayout(bubbleView, p)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager?.removeView(it) }
        bubbleView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}