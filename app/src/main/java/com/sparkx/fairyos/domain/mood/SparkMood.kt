package com.sparkx.fairyos.domain.mood

import androidx.compose.ui.graphics.Color

enum class SparkMood(val displayName: String, val color: Color, val emoji: String) {
    IDLE("Idle", Color(0xFF7B68EE), "🦋"),
    HAPPY("Happy", Color(0xFF00CED1), "😊"),
    THINKING("Thinking", Color(0xFF9B59B6), "🤔"),
    LISTENING("Listening", Color(0xFF3498DB), "🎙️"),
    ALERT("Alert", Color(0xFFE74C3C), "⚠️"),
    SLEEPY("Sleepy", Color(0xFF95A5A6), "😴")
}