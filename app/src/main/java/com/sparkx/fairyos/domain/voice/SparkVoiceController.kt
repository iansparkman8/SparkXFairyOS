package com.sparkx.fairyos.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.sparkx.fairyos.domain.mood.SparkMood
import java.util.Locale

/**
 * Manages local Android SpeechRecognizer and TextToSpeech.
 * Updates mood and speaking state via callbacks.
 */
class SparkVoiceController(
    private val context: Context,
    private val onMoodChange: (SparkMood) -> Unit,
    private val onSpeakingChange: (Boolean) -> Unit,
    private val onCommandRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var isSpeaking = false

    fun initialize() {
        // TTS
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.92f)
                textToSpeech?.setPitch(1.08f)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        onSpeakingChange(true)
                        onMoodChange(SparkMood.SPEAKING)
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingChange(false)
                        onMoodChange(SparkMood.IDLE)
                    }
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingChange(false)
                    }
                })
            }
        }

        // Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true; onMoodChange(SparkMood.LISTENING) }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }

                override fun onError(error: Int) {
                    isListening = false
                    onError(speechErrorMessage(error))
                    onMoodChange(SparkMood.IDLE)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                    val best = if (!matches.isNullOrEmpty()) {
                        if (scores != null && scores.size == matches.size) {
                            matches.zip(scores.toList())
                                .maxByOrNull { it.second }
                                ?.first
                                ?: matches.first()
                        } else {
                            matches.first()
                        }
                    } else {
                        null
                    }

                    if (!best.isNullOrBlank()) {
                        onCommandRecognized(best)
                    } else {
                        onError("I didn't catch that.")
                    }

                    onMoodChange(SparkMood.IDLE)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun speechErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "I had trouble with the microphone audio."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition had a client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "I need microphone permission to listen."
            SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error. Local recognition may not be available."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Try saying it a little slower."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "My listener is busy. Try again in a second."
            SpeechRecognizer.ERROR_SERVER -> "Speech service error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
            else -> "Speech error code: $error"
        }
    }

    fun startListening() {
        if (speechRecognizer == null) initialize()

        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 650L)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 700L)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Talk to Spark Baby")
        }

        try {
            speechRecognizer?.startListening(intent)
            onMoodChange(SparkMood.LISTENING)
        } catch (e: Exception) {
            onError("Could not start listening: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        onMoodChange(SparkMood.IDLE)
    }

    fun speak(text: String) {
        if (textToSpeech == null) initialize()

        val cleaned = text
            .replace("SparkX", "Spark X")
            .replace("APK", "A P K")
            .replace("AI", "A I")
            .replace("TTS", "text to speech")
            .trim()

        val utteranceId = "spark_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        isSpeaking = false
        isListening = false
    }

    fun isCurrentlySpeaking(): Boolean = isSpeaking
}