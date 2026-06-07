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
                    onError("Speech error: $error")
                    onMoodChange(SparkMood.IDLE)
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onCommandRecognized(matches[0])
                    }
                    onMoodChange(SparkMood.IDLE)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (speechRecognizer == null) initialize()
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
        val utteranceId = "spark_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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