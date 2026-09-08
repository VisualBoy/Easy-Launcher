package com.example.model

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isEmergencyContact: Boolean = false,
    val isQuickDial: Boolean = false,
    val relationship: String = ""
)

data class CapturedNotification(
    val id: String,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasReplyAction: Boolean = false,
    val isMissedCall: Boolean = false,
    val isWhatsApp: Boolean = false
)

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING_TOOL,
    SPEAKING,
    ERROR
}

data class VoiceAssistantUiState(
    val state: VoiceState = VoiceState.IDLE,
    val spokenText: String = "",
    val assistantResponse: String = "",
    val activeToolName: String = "",
    val errorMessage: String = "",
    val rmsLevel: Float = 0f
)

data class SosState(
    val isActive: Boolean = false,
    val countdownSeconds: Int = 5,
    val isRecordingVoice: Boolean = false,
    val voiceCountdownSeconds: Int = 10,
    val transcribedText: String = "",
    val audioFilePath: String? = null,
    val audioFileUri: String? = null,
    val isSendingWhatsApp: Boolean = false,
    val isCompleted: Boolean = false,
    val rmsLevel: Float = 0f,
    val statusMessage: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUrl: String = ""
)
