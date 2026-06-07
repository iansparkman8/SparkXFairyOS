package com.sparkx.fairyos.domain.command

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import com.sparkx.fairyos.domain.memory.TeachGrowEntry
import com.sparkx.fairyos.domain.memory.TeachGrowRepository
import com.sparkx.fairyos.domain.mood.SparkMood
import com.sparkx.fairyos.overlay.SparkOverlayController

/**
 * Parses voice/text commands and routes to actions.
 * Owner Mode actions require explicit confirmation in UI layer.
 */
class SparkCommandRouter(
    private val context: Context,
    private val teachGrowRepo: TeachGrowRepository,
    private val onMoodChange: (SparkMood) -> Unit,
    private val onSpeak: (String) -> Unit,
    private val onShowOverlay: () -> Unit,
    private val onHideOverlay: () -> Unit
) {

    data class CommandResult(
        val spokenReply: String,
        val newMood: SparkMood = SparkMood.IDLE,
        val requiresConfirmation: Boolean = false,
        val confirmationAction: (() -> Unit)? = null,
        val actionDescription: String = ""
    )

    fun processCommand(raw: String, isOwnerMode: Boolean): CommandResult {
        val cmd = raw.lowercase().trim()

        return when {
            cmd.contains("hello") || cmd.contains("hi spark") -> {
                onMoodChange(SparkMood.HAPPY)
                CommandResult("Hello! I'm Spark Baby, your holographic companion. How can I help today?", SparkMood.HAPPY)
            }
            cmd.contains("happy") -> {
                onMoodChange(SparkMood.HAPPY)
                CommandResult("Switching to happy mood!", SparkMood.HAPPY)
            }
            cmd.contains("think") || cmd.contains("thinking") -> {
                onMoodChange(SparkMood.THINKING)
                CommandResult("Thinking mode activated. What's on your mind?", SparkMood.THINKING)
            }
            cmd.contains("sleep") || cmd.contains("sleepy") -> {
                onMoodChange(SparkMood.SLEEPY)
                CommandResult("Going to sleep mode. Sweet dreams.", SparkMood.SLEEPY)
            }
            cmd.contains("alert") -> {
                onMoodChange(SparkMood.ALERT)
                CommandResult("Alert mode on. I'm watching out for you.", SparkMood.ALERT)
            }
            cmd.contains("show overlay") -> {
                onShowOverlay()
                CommandResult("Showing floating Spark Baby overlay.", SparkMood.HAPPY)
            }
            cmd.contains("hide overlay") -> {
                onHideOverlay()
                CommandResult("Hiding the overlay bubble.", SparkMood.IDLE)
            }
            cmd.startsWith("open ") -> {
                val appName = cmd.removePrefix("open ").trim()
                val launched = launchAppByName(appName)
                if (launched) {
                    CommandResult("Opening $appName for you.", SparkMood.HAPPY)
                } else {
                    CommandResult("I couldn't find an app matching '$appName'. Try the app drawer.", SparkMood.THINKING)
                }
            }
            cmd.contains("set timer") || cmd.contains("timer for") -> {
                // Extract minutes if possible, default 5
                val minutes = Regex("(\d+)").find(cmd)?.groupValues?.get(1)?.toIntOrNull() ?: 5
                if (isOwnerMode) {
                    CommandResult(
                        "Setting a $minutes minute timer. Confirm?",
                        requiresConfirmation = true,
                        actionDescription = "Set $minutes min timer",
                        confirmationAction = { setTimer(minutes) }
                    )
                } else {
                    CommandResult("Owner Mode is required for setting timers. Enable it in Settings.", SparkMood.ALERT)
                }
            }
            cmd.contains("remember this") || cmd.contains("save this") -> {
                val note = raw.substringAfter("remember this", raw.substringAfter("save this", "")).trim()
                if (note.isNotBlank()) {
                    kotlinx.coroutines.runBlocking {
                        teachGrowRepo.addEntry(TeachGrowEntry(title = "Quick Memory", content = note, type = "memory"))
                    }
                    CommandResult("Saved to Teach & Grow as memory.", SparkMood.HAPPY)
                } else CommandResult("What would you like me to remember?", SparkMood.THINKING)
            }
            cmd.contains("teach") -> {
                val lesson = raw.substringAfter("teach", "").trim()
                if (lesson.isNotBlank()) {
                    kotlinx.coroutines.runBlocking {
                        teachGrowRepo.addEntry(TeachGrowEntry(title = "Lesson", content = lesson, type = "lesson"))
                    }
                    CommandResult("Added to Teach & Grow. Thank you for teaching me!", SparkMood.HAPPY)
                } else CommandResult("Tell me what to learn.", SparkMood.THINKING)
            }
            cmd.contains("summarize") -> {
                CommandResult("For summarization, please enable a cloud AI provider in the AI Console and ask again. Or save the note in Teach & Grow.", SparkMood.THINKING)
            }
            else -> {
                CommandResult("I heard: $raw. Try commands like 'open youtube', 'happy', 'remember this [note]', or enable cloud AI for deeper help.", SparkMood.IDLE)
            }
        }
    }

    private fun launchAppByName(name: String): Boolean {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
        val match = apps.firstOrNull { 
            it.loadLabel(pm).toString().lowercase().contains(name) || 
            it.activityInfo.packageName.lowercase().contains(name)
        }
        return if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } else false
        } else false
    }

    private fun setTimer(minutes: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Spark Baby timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Fallback
        }
    }
}