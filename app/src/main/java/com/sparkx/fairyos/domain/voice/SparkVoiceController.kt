package com.sparkx.fairyos.domain.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.sparkx.fairyos.domain.memory.TeachGrowRepository
import com.sparkx.fairyos.domain.mode.SparkModeManager
import com.sparkx.fairyos.domain.mood.SparkMood
import com.sparkx.fairyos.overlay.SparkOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SparkVoiceController(
    private val context: Context,
    private val modeManager: SparkModeManager
) {
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentMood = MutableStateFlow(SparkMood.IDLE)
    val currentMood: StateFlow<SparkMood> = _currentMood.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    var lastRecognizedText: String = ""
        private set

    private var speechRecognizer: SpeechRecognizer? = null

    // Injected later or simple
    var overlayController: SparkOverlayController? = null
    var teachGrowRepo: TeachGrowRepository? = null

    fun startListening(onResult: (String) -> Unit = {}) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        _isListening.value = true
        _currentMood.value = SparkMood.LISTENING

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Spark Baby...")
        }

        // Simple one-shot for v7
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onError(error: Int) {
                _isListening.value = false
                _currentMood.value = SparkMood.IDLE
                Toast.makeText(context, "Listen error: $error", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                lastRecognizedText = text
                if (text.isNotBlank()) {
                    handleCommand(text, onResult)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
        if (_currentMood.value == SparkMood.LISTENING) _currentMood.value = SparkMood.IDLE
    }

    private fun handleCommand(text: String, onResult: (String) -> Unit) {
        val lower = text.lowercase().trim()
        var response = "I'm here, but I didn't understand. Try 'hello', 'happy', or enable AI."
        var newMood = _currentMood.value

        when {
            "hello" in lower -> {
                response = "Hello! I'm Spark Baby, your privacy-first fairy companion. How can I brighten your day?"
                newMood = SparkMood.HAPPY
            }
            "happy" in lower -> {
                newMood = SparkMood.HAPPY
                response = "Yay! Switching to happy mode."
            }
            "think" in lower || "thinking" in lower -> {
                newMood = SparkMood.THINKING
                response = "Deep in thought... What shall we ponder?"
            }
            "sleep" in lower || "sleepy" in lower -> {
                newMood = SparkMood.SLEEPY
                response = "Going to sleepy mode. Wake me if you need me."
            }
            "alert" in lower -> {
                newMood = SparkMood.ALERT
                response = "Alert mode activated! What's happening?"
            }
            "show overlay" in lower || "show fairy" in lower -> {
                overlayController?.show()
                response = "Overlay bubble shown."
            }
            "hide overlay" in lower || "hide fairy" in lower -> {
                overlayController?.hide()
                response = "Overlay hidden."
            }
            "teach" in lower || "teach grow" in lower -> {
                // Caller should navigate
                response = "Opening Teach & Grow lab for you."
            }
            "remember this" in lower || "save this" in lower || "memory" in lower -> {
                teachGrowRepo?.let { repo ->
                    scope.launch {
                        repo.addEntry("Voice Memory", text, "memory")
                    }
                }
                response = "Got it! Saved to your Teach & Grow memories."
            }
            // Owner Mode powerful commands
            "open " in lower -> {
                if (isOwnerMode()) {
                    val app = lower.substringAfter("open ").trim()
                    launchApp(app)
                    response = "Opening $app for you."
                } else {
                    response = "That command requires Owner Mode. Enable it safely in Settings."
                }
            }
            "timer" in lower || "set timer" in lower || "alarm" in lower -> {
                if (isOwnerMode()) {
                    setSimpleTimer()
                    response = "Timer started via Android Clock."
                } else {
                    response = "Timers require Owner Mode."
                }
            }
            "settings" in lower -> {
                if (isOwnerMode()) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    response = "Opening device Settings."
                } else {
                    response = "Opening settings requires Owner Mode."
                }
            }
            else -> {
                // If cloud AI enabled, it would be handled in UI layer or here
                response = "Hmm... I heard: '$text'. Teach me or enable Cloud AI in AI Providers screen for smarter replies!"
            }
        }

        _currentMood.value = newMood
        speak(response)
        onResult(response)
    }

    private fun isOwnerMode(): Boolean {
        // Simple sync check for demo; in prod use flow
        return false // Placeholder - real impl would collect flow but for command sync we use simple
        // For v7 demo, Owner commands work if user enabled (we can improve with shared pref direct)
    }

    private fun launchApp(query: String) {
        val pkg = when {
            "youtube" in query -> "com.google.android.youtube"
            "settings" in query -> return // already handled
            "chrome" in query || "browser" in query -> "com.android.chrome"
            "camera" in query -> "com.android.camera2"
            else -> null
        }
        if (pkg != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "App not found: $pkg", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Fallback web search or ignore
            Toast.makeText(context, "Try 'open youtube' or 'open settings'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSimpleTimer() {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, 5 * 60) // 5 min default
            putExtra(AlarmClock.EXTRA_MESSAGE, "Spark Baby timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set timer", Toast.LENGTH_SHORT).show()
        }
    }

    fun speak(text: String) {
        _isSpeaking.value = true
        _currentMood.value = SparkMood.HAPPY // or keep
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spark_baby_utterance")
        // Note: In real, register tts progress listener to set false when done
        // For v7, simple delay reset
        scope.launch {
            kotlinx.coroutines.delay(3000 + text.length * 40L)
            _isSpeaking.value = false
            if (_currentMood.value == SparkMood.HAPPY) _currentMood.value = SparkMood.IDLE
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        stopListening()
    }

    // Placeholder for future AI integration in handleCommand
    fun setAIEnabled(enabled: Boolean) { /* used by UI */ }
}