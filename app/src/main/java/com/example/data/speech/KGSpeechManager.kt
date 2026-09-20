package com.example.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SpeechState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val partialText: String = "",
    val error: String? = null,
    val isAvailable: Boolean = false
)

class KGSpeechManager(private val context: Context) {

    private val _speechState = MutableStateFlow(
        SpeechState(isAvailable = SpeechRecognizer.isRecognitionAvailable(context))
    )
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = _speechState.value.copy(
                isAvailable = false,
                error = "Speech recognition service is not available on this device"
            )
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = _speechState.value.copy(isListening = true, error = null)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _speechState.value = _speechState.value.copy(isListening = false)
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network required for online speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        _speechState.value = _speechState.value.copy(isListening = false, error = message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            val current = _speechState.value.recognizedText
                            val updated = if (current.isBlank()) text else "$current $text"
                            _speechState.value = _speechState.value.copy(
                                recognizedText = updated,
                                partialText = "",
                                isListening = false
                            )
                        } else {
                            _speechState.value = _speechState.value.copy(isListening = false)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _speechState.value = _speechState.value.copy(partialText = text)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            _speechState.value = _speechState.value.copy(isAvailable = true)
        } catch (e: Exception) {
            _speechState.value = _speechState.value.copy(
                isAvailable = false,
                error = "Failed to initialize SpeechRecognizer: ${e.message}"
            )
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _speechState.value = _speechState.value.copy(isListening = true, error = null)
        } catch (e: Exception) {
            _speechState.value = _speechState.value.copy(
                isListening = false,
                error = "Cannot start listening: ${e.message}"
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _speechState.value = _speechState.value.copy(isListening = false)
        } catch (e: Exception) {}
    }

    fun clearText() {
        _speechState.value = _speechState.value.copy(recognizedText = "", partialText = "", error = null)
    }

    fun appendCommand(prefix: String) {
        val current = _speechState.value.recognizedText.trim()
        val textWithoutOldPrefix = current.replace(Regex("^/(tp|say|give|locate|time|weather)\\s*"), "")
        val updated = "$prefix $textWithoutOldPrefix".trim()
        _speechState.value = _speechState.value.copy(recognizedText = updated)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}
    }
}
