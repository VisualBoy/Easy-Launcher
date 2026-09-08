package com.example.accessibility

import com.example.model.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationRepository {

    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    private val _missedCallsCount = MutableStateFlow(2) // Initial sample count
    val missedCallsCount: StateFlow<Int> = _missedCallsCount.asStateFlow()

    private val _unreadWhatsAppCount = MutableStateFlow(1) // Initial sample count
    val unreadWhatsAppCount: StateFlow<Int> = _unreadWhatsAppCount.asStateFlow()

    init {
        // Add realistic sample captured notifications for instant testing & accessibility demonstration
        _notifications.value = listOf(
            CapturedNotification(
                id = "sample_wa_1",
                notificationKey = "sample_wa_key_1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                sender = "Maria",
                message = "Ciao nonno! A che ora arrivi per pranzo?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                hasReplyAction = true,
                isWhatsApp = true
            ),
            CapturedNotification(
                id = "sample_call_1",
                notificationKey = "sample_call_key_1",
                packageName = "com.google.android.dialer",
                appName = "Telefono",
                sender = "Marco (Figlio)",
                message = "Chiamata persa alle 16:15",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                hasReplyAction = false,
                isMissedCall = true
            ),
            CapturedNotification(
                id = "sample_call_2",
                notificationKey = "sample_call_key_2",
                packageName = "com.google.android.dialer",
                appName = "Telefono",
                sender = "Dott. Rossi",
                message = "Chiamata persa alle 15:40",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 50,
                hasReplyAction = false,
                isMissedCall = true
            )
        )
    }

    fun addNotification(item: CapturedNotification) {
        val current = _notifications.value.filter { it.notificationKey != item.notificationKey }.toMutableList()
        current.add(0, item)
        _notifications.value = current
        recalculateCounts()
    }

    fun removeNotification(notificationKey: String) {
        _notifications.value = _notifications.value.filter { it.notificationKey != notificationKey }
        recalculateCounts()
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _missedCallsCount.value = 0
        _unreadWhatsAppCount.value = 0
    }

    private fun recalculateCounts() {
        var wa = 0
        var calls = 0
        for (item in _notifications.value) {
            if (item.isWhatsApp) wa++
            if (item.isMissedCall) calls++
        }
        _unreadWhatsAppCount.value = wa
        _missedCallsCount.value = calls
    }
}
