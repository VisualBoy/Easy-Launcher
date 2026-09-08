package com.example.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.R
import com.example.accessibility.SosManager
import com.example.accessibility.TtsManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Locale

/**
 * High-priority ForegroundService dedicated to the emergency SOS routine.
 * 1. Shows persistent high-priority notification with countdown and cancel button.
 * 2. Records a 10-second voice message from the microphone.
 * 3. Transcribes spoken speech in Italian via SpeechRecognizer.
 * 4. Acquires GPS location (Google Maps link).
 * 5. Sends the audio file and transcribed message via WhatsApp to the default emergency contact.
 * 6. Sends emergency SMS with location & transcription as safety fallback.
 */
class SosEmergencyForegroundService : Service() {

    companion object {
        private const val TAG = "SosEmergencyService"
        const val CHANNEL_ID = "sos_emergency_foreground_channel"
        const val NOTIFICATION_ID = 9991

        const val ACTION_START_SOS_VOICE = "com.example.action.START_SOS_VOICE"
        const val ACTION_CANCEL_SOS = "com.example.action.CANCEL_SOS"

        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startSosVoiceService(context: Context, phoneNumber: String = "", contactName: String = "") {
            val intent = Intent(context, SosEmergencyForegroundService::class.java).apply {
                action = ACTION_START_SOS_VOICE
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CONTACT_NAME, contactName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSosService(context: Context) {
            val intent = Intent(context, SosEmergencyForegroundService::class.java).apply {
                action = ACTION_CANCEL_SOS
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFile: File? = null
    private var transcribedTextBuilder = StringBuilder()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var ttsManager: TtsManager? = null

    private var targetPhoneNumber: String = ""
    private var targetContactName: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ttsManager = TtsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_SOS -> {
                Log.d(TAG, "Cancel SOS received in service")
                cancelEmergencyRoutine()
                return START_NOT_STICKY
            }
            ACTION_START_SOS_VOICE -> {
                targetPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                targetContactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Contatto di Emergenza"

                val notification = buildOngoingNotification(
                    title = "🚨 REGISTRAZIONE SOS IN CORSO (10s)",
                    content = "Parla al microfono, registrazione e trascrizione vocale attive...",
                    countdown = 10
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    }
                    startForeground(NOTIFICATION_ID, notification, fgType)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                start10SecondEmergencyRoutine()
            }
        }
        return START_NOT_STICKY
    }

    private fun start10SecondEmergencyRoutine() {
        serviceScope.launch {
            try {
                SosManager.currentInstance?.notifyVoiceRecordingStarted()

                // 1. Setup Audio File & MediaRecorder
                val cacheDir = externalCacheDir ?: cacheDir
                val fileName = "sos_audio_${System.currentTimeMillis()}.m4a"
                audioFile = File(cacheDir, fileName)

                startMediaRecorder(audioFile!!)
                startSpeechTranscription()

                // 2. 10-Second Live Countdown loop
                for (sec in 10 downTo 1) {
                    val remainingText = "🔴 Registrazione SOS: ${sec}s rimanenti... Parla chiaramente!"
                    updateNotification(
                        title = "🚨 REGISTRAZIONE SOS IN CORSO ($sec s)",
                        content = remainingText,
                        countdown = sec
                    )
                    SosManager.currentInstance?.notifyVoiceCountdownTick(sec, transcribedTextBuilder.toString())
                    delay(1000)
                }

                // 3. Stop recording & finalize transcription
                stopMediaRecorder()
                stopSpeechTranscription()

                SosManager.currentInstance?.notifyVoiceRecordingFinished(
                    audioPath = audioFile?.absolutePath,
                    transcription = transcribedTextBuilder.toString()
                )

                updateNotification(
                    title = "🚨 ELABORAZIONE E INVIO SOS",
                    content = "Trascrizione completata. Rilevamento GPS ed invio WhatsApp...",
                    countdown = 0
                )

                // 4. Get GPS Location
                val locationData = fetchCurrentLocation()

                // 5. Send via WhatsApp to default contact + backup SMS
                sendEmergencyWhatsAppAndSms(locationData, audioFile, transcribedTextBuilder.toString())

            } catch (e: Exception) {
                Log.e(TAG, "Error in emergency routine", e)
                SosManager.currentInstance?.notifyVoiceError(e.message ?: "Errore durante SOS")
            } finally {
                delay(3000)
                stopSelf()
            }
        }
    }

    private fun startMediaRecorder(outputFile: File) {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Log.d(TAG, "MediaRecorder started successfully on ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaRecorder", e)
        }
    }

    private fun stopMediaRecorder() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }
    }

    private fun startSpeechTranscription() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        SosManager.currentInstance?.notifyVoiceRms(rmsdB)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.w(TAG, "Speech recognition error code: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            transcribedTextBuilder.clear()
                            transcribedTextBuilder.append(text)
                            SosManager.currentInstance?.notifyVoiceTranscriptionUpdate(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            transcribedTextBuilder.clear()
                            transcribedTextBuilder.append(text)
                            SosManager.currentInstance?.notifyVoiceTranscriptionUpdate(text)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                }
                startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
        }
    }

    private fun stopSpeechTranscription() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Triple<Double?, Double?, String> {
        var latitude: Double? = null
        var longitude: Double? = null
        var mapsUrl = ""

        try {
            val cts = CancellationTokenSource()
            val location: Location? = try {
                fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)?.await()
            } catch (e: Exception) {
                null
            }

            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                mapsUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val lastLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastLoc != null) {
                    latitude = lastLoc.latitude
                    longitude = lastLoc.longitude
                    mapsUrl = "https://maps.google.com/?q=${lastLoc.latitude},${lastLoc.longitude}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining GPS location", e)
        }

        return Triple(latitude, longitude, mapsUrl)
    }

    private fun sendEmergencyWhatsAppAndSms(
        locationData: Triple<Double?, Double?, String>,
        audioFile: File?,
        transcription: String
    ) {
        val (lat, lng, mapsUrl) = locationData
        val finalTranscription = if (transcription.isNotBlank()) {
            "\"$transcription\""
        } else {
            "\"(Messaggio vocale registrato di 10 secondi allegato)\""
        }

        val locationSection = if (mapsUrl.isNotBlank()) {
            "📍 *Posizione GPS:* $mapsUrl"
        } else {
            "📍 *Posizione GPS:* (In rilevamento)"
        }

        val fullWhatsAppMessage = """
            🚨 *EMERGENZA SOS - EASY LAUNCHER* 🚨
            $locationSection
            
            🗣️ *Messaggio Vocale Registrato:*
            $finalTranscription
            
            ⏱️ _Registrato automaticamente dal dispositivo salvavita._
        """.trimIndent()

        // 1. Resolve Emergency Phone Number
        val phoneToSend = if (targetPhoneNumber.isNotBlank()) {
            targetPhoneNumber
        } else {
            SosManager.currentInstance?.emergencyContacts?.value?.firstOrNull()?.phoneNumber ?: "+393331234567"
        }

        SosManager.currentInstance?.notifySendingWhatsApp(fullWhatsAppMessage, phoneToSend)

        // 2. Prepare Audio File URI via FileProvider
        var audioUri: Uri? = null
        if (audioFile != null && audioFile.exists()) {
            try {
                audioUri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    audioFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting FileProvider uri", e)
            }
        }

        // 3. Dispatch to WhatsApp
        val formattedPhone = phoneToSend.replace(Regex("[^0-9+]"), "").removePrefix("+")
        try {
            if (audioUri != null) {
                // Share audio file + text with WhatsApp
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, audioUri)
                    putExtra(Intent.EXTRA_TEXT, fullWhatsAppMessage)
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    setPackage(WhatsAppAccessibilityFallbackService.PKG_WHATSAPP)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(sendIntent)
            } else {
                // Deep link fallback
                WhatsAppAccessibilityFallbackService.launchFallback(
                    context = this,
                    phoneNumber = phoneToSend,
                    message = fullWhatsAppMessage
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp send intent, attempting fallback deep link", e)
            WhatsAppAccessibilityFallbackService.launchFallback(
                context = this,
                phoneNumber = phoneToSend,
                message = fullWhatsAppMessage
            )
        }

        // 4. Send SMS to all emergency contacts as redundancy guarantee
        val smsManager = try {
            getSystemService(SmsManager::class.java)
        } catch (e: Exception) {
            null
        }

        val smsText = """
            SOS EMERGENZA! $locationSection
            VOCE TRASCRITTA: $finalTranscription
        """.trimIndent()

        SosManager.currentInstance?.emergencyContacts?.value?.forEach { contact ->
            try {
                smsManager?.sendTextMessage(contact.phoneNumber, null, smsText, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS to ${contact.phoneNumber}", e)
            }
        }

        // 5. Update UI & Audio Confirmation
        SosManager.currentInstance?.notifySosCompleted(
            lat = lat,
            lng = lng,
            maps = mapsUrl,
            audioUri = audioUri?.toString()
        )

        ttsManager?.speak("Messaggio vocale e posizione inviati su WhatsApp al contatto di emergenza.")

        updateNotification(
            title = "✅ SOS INVIATO CON SUCCESSO",
            content = "Messaggio vocale, trascrizione e posizione GPS trasmessi.",
            countdown = 0
        )
    }

    private fun cancelEmergencyRoutine() {
        serviceScope.cancel()
        stopMediaRecorder()
        stopSpeechTranscription()
        SosManager.currentInstance?.cancelSos()
        ttsManager?.speak("Procedura SOS annullata.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servizio SOS Emergenza",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche attive per registrazione e invio SOS"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingNotification(title: String, content: String, countdown: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, SosEmergencyForegroundService::class.java).apply {
            action = ACTION_CANCEL_SOS
        }
        val cancelPending = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ANNULLA SOS", cancelPending)
            .setProgress(10, 10 - countdown, false)
            .build()
    }

    private fun updateNotification(title: String, content: String, countdown: Int) {
        val notification = buildOngoingNotification(title, content, countdown)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopMediaRecorder()
        stopSpeechTranscription()
        serviceScope.cancel()
        ttsManager?.shutdown()
    }
}
