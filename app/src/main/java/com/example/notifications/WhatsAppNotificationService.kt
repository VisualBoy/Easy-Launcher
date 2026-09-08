package com.example.notifications

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.accessibility.NotificationRepository
import com.example.model.CapturedNotification
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to intercept incoming WhatsApp, SMS, Telegram, and Call notifications
 * and process automated inline replies via RemoteInput.
 */
class WhatsAppNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "WhatsAppNotification"
        const val PKG_WHATSAPP = "com.whatsapp"
        const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        const val PKG_TELEGRAM = "org.telegram.messenger"
        const val PKG_MESSAGES = "com.google.android.apps.messaging"
        const val PKG_DIALER = "com.google.android.dialer"

        // In-memory cache of active RemoteInput actions keyed by notificationKey
        val activeReplyActions = ConcurrentHashMap<String, Notification.Action>()

        /**
         * Checks if the NotificationListenerService permission has been granted by user in System Settings.
         */
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(pkgName) == true
        }

        /**
         * Opens System Settings to let user grant Notification Access.
         */
        fun openNotificationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /**
         * Dispatches a reply directly into the notification RemoteInput bundle without opening WhatsApp.
         */
        fun sendReply(context: Context, replyAction: Notification.Action, replyText: String): Boolean {
            val remoteInputs = replyAction.remoteInputs ?: return false
            val intent = Intent()
            val bundle = Bundle()

            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }

            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

            return try {
                replyAction.actionIntent.send(context, 0, intent)
                Log.d(TAG, "RemoteInput inline reply dispatched successfully.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send RemoteInput reply intent", e)
                false
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return

        val isWhatsApp = packageName == PKG_WHATSAPP || packageName == PKG_WHATSAPP_BUSINESS
        val isSms = packageName == PKG_MESSAGES || packageName.contains("mms") || packageName.contains("messaging")
        val isCall = packageName == PKG_DIALER || packageName.contains("dialer") || packageName.contains("telecom")
        val isTelegram = packageName == PKG_TELEGRAM

        if (!isWhatsApp && !isSms && !isCall && !isTelegram) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: "Messaggio"

        val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        val replyAction = findInlineReplyAction(notification)
        if (replyAction != null) {
            activeReplyActions[sbn.key] = replyAction
        }

        val appName = when {
            isWhatsApp -> "WhatsApp"
            isTelegram -> "Telegram"
            isSms -> "SMS"
            isCall -> "Telefono"
            else -> "Notifica"
        }

        val isMissedCall = isCall || messageText.contains("chiamata persa", ignoreCase = true) ||
                sender.contains("chiamata persa", ignoreCase = true)

        val captured = CapturedNotification(
            id = sbn.id.toString(),
            notificationKey = sbn.key,
            packageName = packageName,
            appName = appName,
            sender = sender,
            message = messageText,
            timestamp = sbn.postTime,
            hasReplyAction = replyAction != null,
            isMissedCall = isMissedCall,
            isWhatsApp = isWhatsApp
        )

        Log.d(TAG, "Captured notification from $sender [$appName]: $messageText")
        NotificationRepository.addNotification(captured)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        activeReplyActions.remove(sbn.key)
        NotificationRepository.removeNotification(sbn.key)
    }

    private fun findInlineReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.firstOrNull { action ->
            action.remoteInputs?.any { remoteInput -> remoteInput.allowFreeFormInput } == true
        }
    }
}
