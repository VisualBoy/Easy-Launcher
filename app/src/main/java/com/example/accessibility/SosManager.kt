package com.example.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.example.model.ContactItem
import com.example.model.SosState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SosManager(private val context: Context) {

    companion object {
        private const val TAG = "SosManager"

        @Volatile
        var currentInstance: SosManager? = null
            private set
    }

    init {
        currentInstance = this
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _sosState = MutableStateFlow(SosState())
    val sosState: StateFlow<SosState> = _sosState.asStateFlow()

    private var countdownJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    // Default Emergency Contacts list (editable in Settings)
    val emergencyContacts = MutableStateFlow(
        listOf(
            ContactItem(
                id = "sos_1",
                name = "Marco (Figlio)",
                phoneNumber = "+393331234567",
                isEmergencyContact = true,
                relationship = "Figlio"
            ),
            ContactItem(
                id = "sos_2",
                name = "Chiara (Figlia)",
                phoneNumber = "+393479876543",
                isEmergencyContact = true,
                relationship = "Figlia"
            )
        )
    )

    fun startSosSequence(coroutineScope: CoroutineScope, onCountdownTick: ((Int) -> Unit)? = null) {
        if (_sosState.value.isActive) return

        countdownJob?.cancel()
        _sosState.value = SosState(
            isActive = true,
            countdownSeconds = 5,
            statusMessage = "Invio SOS tra 5 secondi. Premi ANNULLA per fermare."
        )

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Could not init ToneGenerator", e)
        }

        countdownJob = coroutineScope.launch(Dispatchers.Main) {
            for (i in 5 downTo 1) {
                _sosState.value = _sosState.value.copy(
                    countdownSeconds = i,
                    statusMessage = "Invio SOS tra $i secondi..."
                )
                onCountdownTick?.invoke(i)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                delay(1000)
            }

            // Initial 5s countdown finished -> Launch Foreground Service for 10s voice message & WhatsApp
            _sosState.value = _sosState.value.copy(
                countdownSeconds = 0,
                isRecordingVoice = true,
                voiceCountdownSeconds = 10,
                statusMessage = "Avvio registrazione vocale di emergenza (10s)..."
            )

            val primaryContact = emergencyContacts.value.firstOrNull()
            com.example.services.SosEmergencyForegroundService.startSosVoiceService(
                context = context,
                phoneNumber = primaryContact?.phoneNumber ?: "",
                contactName = primaryContact?.name ?: "Contatto di Emergenza"
            )
        }
    }

    fun startInstantVoiceSos() {
        countdownJob?.cancel()
        _sosState.value = SosState(
            isActive = true,
            countdownSeconds = 0,
            isRecordingVoice = true,
            voiceCountdownSeconds = 10,
            statusMessage = "Registrazione messaggio vocale SOS in corso (10s)..."
        )
        val primaryContact = emergencyContacts.value.firstOrNull()
        com.example.services.SosEmergencyForegroundService.startSosVoiceService(
            context = context,
            phoneNumber = primaryContact?.phoneNumber ?: "",
            contactName = primaryContact?.name ?: "Contatto di Emergenza"
        )
    }

    fun notifyVoiceRecordingStarted() {
        _sosState.value = _sosState.value.copy(
            isRecordingVoice = true,
            voiceCountdownSeconds = 10,
            statusMessage = "🔴 Registrazione attiva (10s): Parla pure ora..."
        )
    }

    fun notifyVoiceCountdownTick(remainingSeconds: Int, partialTranscription: String) {
        _sosState.value = _sosState.value.copy(
            voiceCountdownSeconds = remainingSeconds,
            transcribedText = partialTranscription,
            statusMessage = "🔴 Parla adesso: ancora $remainingSeconds secondi..."
        )
    }

    fun notifyVoiceRms(rms: Float) {
        _sosState.value = _sosState.value.copy(
            rmsLevel = rms
        )
    }

    fun notifyVoiceTranscriptionUpdate(transcription: String) {
        _sosState.value = _sosState.value.copy(
            transcribedText = transcription
        )
    }

    fun notifyVoiceRecordingFinished(audioPath: String?, transcription: String) {
        _sosState.value = _sosState.value.copy(
            isRecordingVoice = false,
            voiceCountdownSeconds = 0,
            audioFilePath = audioPath,
            transcribedText = transcription,
            statusMessage = "Trascrizione ultimata. Acquisizione posizione GPS..."
        )
    }

    fun notifySendingWhatsApp(message: String, phone: String) {
        _sosState.value = _sosState.value.copy(
            isSendingWhatsApp = true,
            statusMessage = "Invio messaggio vocale e trascrizione su WhatsApp a $phone..."
        )
    }

    fun notifySosCompleted(lat: Double?, lng: Double?, maps: String, audioUri: String?) {
        _sosState.value = _sosState.value.copy(
            isSendingWhatsApp = false,
            isCompleted = true,
            latitude = lat,
            longitude = lng,
            mapsUrl = maps,
            audioFileUri = audioUri,
            statusMessage = "✅ SOS Inviato! Audio, trascrizione e posizione trasmessi su WhatsApp e SMS."
        )
    }

    fun notifyVoiceError(errorMessage: String) {
        _sosState.value = _sosState.value.copy(
            statusMessage = "Attenzione: $errorMessage"
        )
    }

    fun cancelSos() {
        countdownJob?.cancel()
        countdownJob = null
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ToneGenerator", e)
        }

        try {
            com.example.services.SosEmergencyForegroundService.stopSosService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SOS service", e)
        }

        _sosState.value = SosState(
            isActive = false,
            countdownSeconds = 5,
            isRecordingVoice = false,
            voiceCountdownSeconds = 10,
            transcribedText = "",
            isSendingWhatsApp = false,
            isCompleted = false,
            statusMessage = "SOS Annullato."
        )
    }

    private fun makeDirectCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make emergency call", e)
        }
    }
}
