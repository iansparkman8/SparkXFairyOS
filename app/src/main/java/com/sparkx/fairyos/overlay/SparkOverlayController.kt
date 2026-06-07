package com.sparkx.fairyos.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object SparkOverlayController {

    fun startOverlay(context: Context) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "START_OVERLAY"
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopOverlay(context: Context) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "STOP_OVERLAY"
        }
        context.startService(intent)
    }

    fun updateMood(context: Context, mood: com.sparkx.fairyos.domain.mood.SparkMood) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "UPDATE_MOOD"
            putExtra("mood", mood.name)
        }
        context.startService(intent)
    }

    fun updateSpeaking(context: Context, speaking: Boolean) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "UPDATE_SPEAKING"
            putExtra("speaking", speaking)
        }
        context.startService(intent)
    }

    // === Free-roam controls ===
    fun toggleFreeRoam(context: Context) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "TOGGLE_FREE_ROAM"
        }
        context.startService(intent)
    }

    fun setFreeRoam(context: Context, enabled: Boolean) {
        val intent = Intent(context, SparkOverlayService::class.java).apply {
            action = "SET_FREE_ROAM"
            putExtra("enabled", enabled)
        }
        context.startService(intent)
    }
}