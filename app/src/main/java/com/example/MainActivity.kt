package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.accessibility.SosManager
import com.example.accessibility.TorchController
import com.example.accessibility.TtsManager
import com.example.ai.GeminiAssistantManager
import com.example.ai.NativeActionExecutor
import com.example.model.VoiceAssistantUiState
import com.example.model.VoiceState
import com.example.ui.MainLauncherScreen
import com.example.ui.components.VoiceAssistantOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var torchController: TorchController
    private lateinit var sosManager: SosManager
    private lateinit var ttsManager: TtsManager
    private lateinit var actionExecutor: NativeActionExecutor
    private lateinit var geminiManager: GeminiAssistantManager

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "==> [LIFECYCLE] onCreate() start")
        enableEdgeToEdge()

        try {
            Log.d(TAG, "[INIT] Initializing TorchController...")
            torchController = TorchController(this)
            Log.d(TAG, "[INIT] Initializing SosManager...")
            sosManager = SosManager(this)
            Log.d(TAG, "[INIT] Initializing TtsManager...")
            ttsManager = TtsManager(this)
            Log.d(TAG, "[INIT] Core controllers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "[INIT ERROR] Failed during controller initialization in onCreate", e)
        }

        Log.d(TAG, "[UI] Calling setContent...")
        setContent {
            val coroutineScope = rememberCoroutineScope()
            var voiceUiState by remember { mutableStateOf(VoiceAssistantUiState()) }
            var showVoiceOverlay by remember { mutableStateOf(false) }

            // Multiple permissions launcher
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val audioGranted = perms[Manifest.permission.RECORD_AUDIO] ?: false
                if (audioGranted) {
                    startSpeechCapture(
                        onRecognized = { text ->
                            processVoiceInput(text, coroutineScope, { voiceUiState = it })
                        },
                        onStateUpdate = { voiceUiState = it }
                    )
                }
            }

            LaunchedEffect(Unit) {
                actionExecutor = NativeActionExecutor(
                    context = this@MainActivity,
                    torchController = torchController,
                    sosManager = sosManager,
                    coroutineScope = coroutineScope
                )
                geminiManager = GeminiAssistantManager(actionExecutor)

                val required = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
                val missing = required.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isNotEmpty()) {
                    permissionsLauncher.launch(required)
                }
            }

            fun requestPermsAndStartVoice() {
                showVoiceOverlay = true
                voiceUiState = VoiceAssistantUiState(state = VoiceState.LISTENING)

                val required = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )

                val allGranted = required.all {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    startSpeechCapture(
                        onRecognized = { text ->
                            processVoiceInput(text, coroutineScope, { voiceUiState = it })
                        },
                        onStateUpdate = { voiceUiState = it }
                    )
                } else {
                    permissionsLauncher.launch(required)
                }
            }

            MyApplicationTheme {
                MainLauncherScreen(
                    torchController = torchController,
                    sosManager = sosManager,
                    ttsManager = ttsManager,
                    onStartVoiceRecognition = {
                        requestPermsAndStartVoice()
                    }
                )

                if (showVoiceOverlay) {
                    VoiceAssistantOverlay(
                        uiState = voiceUiState,
                        onSpeakAgain = {
                            requestPermsAndStartVoice()
                        },
                        onDismiss = {
                            stopSpeechCapture()
                            ttsManager.stop()
                            showVoiceOverlay = false
                            voiceUiState = VoiceAssistantUiState(state = VoiceState.IDLE)
                        }
                    )
                }
            }
        }
    }

    private fun startSpeechCapture(
        onRecognized: (String) -> Unit,
        onStateUpdate: (VoiceAssistantUiState) -> Unit
    ) {
        runOnUiThread {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    Log.w(TAG, "SpeechRecognizer not available on this device/emulator")
                    onStateUpdate(
                        VoiceAssistantUiState(
                            state = VoiceState.IDLE,
                            spokenText = "Come posso aiutarti?",
                            assistantResponse = "Assistente attivo. Inserisci un comando vocale o usa i tasti grandi rapidi."
                        )
                    )
                    return@runOnUiThread
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onStateUpdate(VoiceAssistantUiState(state = VoiceState.LISTENING))
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onStateUpdate(
                            VoiceAssistantUiState(
                                state = VoiceState.LISTENING,
                                rmsLevel = rmsdB
                            )
                        )
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        onStateUpdate(VoiceAssistantUiState(state = VoiceState.PROCESSING))
                    }

                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nessuna voce riconosciuta. Riprova."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo scaduto. Tocca per parlare."
                            SpeechRecognizer.ERROR_AUDIO -> "Errore microfono."
                            else -> "Non ho capito, tocca il microfono e riprova."
                        }
                        onStateUpdate(VoiceAssistantUiState(state = VoiceState.ERROR, errorMessage = msg))
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onRecognized(text)
                        } else {
                            onStateUpdate(VoiceAssistantUiState(state = VoiceState.ERROR, errorMessage = "Nessuna voce rilevata."))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.toString())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla adesso...")
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
                onStateUpdate(VoiceAssistantUiState(state = VoiceState.ERROR, errorMessage = "Riconoscimento vocale non disponibile."))
            }
        }
    }

    private fun stopSpeechCapture() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
    }

    private fun processVoiceInput(
        text: String,
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        onStateUpdate: (VoiceAssistantUiState) -> Unit
    ) {
        onStateUpdate(
            VoiceAssistantUiState(
                state = VoiceState.PROCESSING,
                spokenText = text
            )
        )

        coroutineScope.launch {
            try {
                val response = geminiManager.processIntent(text) { toolName ->
                    onStateUpdate(
                        VoiceAssistantUiState(
                            state = VoiceState.EXECUTING_TOOL,
                            spokenText = text,
                            activeToolName = toolName
                        )
                    )
                }

                onStateUpdate(
                    VoiceAssistantUiState(
                        state = VoiceState.IDLE,
                        spokenText = text,
                        assistantResponse = response
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice input", e)
                val errMsg = "Errore durante l'elaborazione. Riprova."
                onStateUpdate(
                    VoiceAssistantUiState(
                        state = VoiceState.ERROR,
                        spokenText = text,
                        errorMessage = errMsg
                    )
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "==> [LIFECYCLE] onStart() called - Activity is visible")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "==> [LIFECYCLE] onResume() called - Activity is in foreground & interactive")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "==> [LIFECYCLE] onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "==> [LIFECYCLE] onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "==> [LIFECYCLE] onDestroy() called - Cleaning up resources")
        try {
            speechRecognizer?.destroy()
            ttsManager.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "[CLEANUP ERROR] Error during onDestroy cleanup", e)
        }
    }
}
