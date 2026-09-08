package com.example.accessibility

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TtsManager with voice playback disabled per user request.
 */
class TtsManager(private val context: Context) {

    companion object {
        private const val TAG = "TtsManager"
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        // Voice reading completely disabled per user directive
        Log.d(TAG, "Voice reading suppressed: $text")
        onComplete?.invoke()
    }

    fun stop() {
        _isSpeaking.value = false
    }

    fun shutdown() {
        _isSpeaking.value = false
    }
}

