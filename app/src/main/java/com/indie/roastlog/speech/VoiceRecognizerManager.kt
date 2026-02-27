package com.indie.roastlog.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceRecognitionState {
    data object Idle : VoiceRecognitionState()
    data object Listening : VoiceRecognitionState()
    data class Success(val number: Float) : VoiceRecognitionState()
    data class Error(val message: String) : VoiceRecognitionState()
}

class VoiceRecognizerManager(context: Context) {

    private val speechRecognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(context)
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceRecognitionState.Listening
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Error audio"
                SpeechRecognizer.ERROR_CLIENT -> "Error client"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission tidak cukup"
                SpeechRecognizer.ERROR_NETWORK -> "Error network"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada hasil"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer sibuk"
                SpeechRecognizer.ERROR_SERVER -> "Error server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu habis, coba lagi"
                else -> "Error tidak diketahui"
            }
            _state.value = VoiceRecognitionState.Error(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                val number = extractNumber(text)
                if (number != null) {
                    _state.value = VoiceRecognitionState.Success(number)
                } else {
                    _state.value = VoiceRecognitionState.Error("Hasil bukan angka: '$text'")
                }
            } else {
                _state.value = VoiceRecognitionState.Error("Tidak ada hasil")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    fun startListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceRecognitionState.Error("Speech recognition tidak tersedia")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Katakan suhu dalam angka")
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = VoiceRecognitionState.Error("Gagal memulai: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun resetState() {
        _state.value = VoiceRecognitionState.Idle
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }

    private fun extractNumber(text: String): Float? {
        val cleanedText = text.lowercase()
            .replace("celcius", "")
            .replace("c", "")
            .replace("derajat", "")
            .replace("suhu", "")
            .replace(",", ".")
            .trim()

        val numberRegex = Regex("""^\d+(\.\d+)?$""")
        val match = numberRegex.find(cleanedText)

        return match?.value?.toFloatOrNull()
    }
}
