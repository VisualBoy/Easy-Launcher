package com.example.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.telephony.SmsManager
import android.util.Log
import com.example.accessibility.NotificationRepository
import com.example.accessibility.SosManager
import com.example.accessibility.TorchController
import com.example.notifications.WhatsAppNotificationService
import com.example.services.WhatsAppAccessibilityFallbackService
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Executes native Android system capabilities based on Gemini tool dispatching.
 */
class NativeActionExecutor(
    private val context: Context,
    private val torchController: TorchController,
    private val sosManager: SosManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "NativeActionExecutor"
    }

    fun sendSms(phoneNumber: String, message: String): JSONObject {
        val result = JSONObject()
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            result.put("status", "success")
            result.put("details", "SMS inviato con successo al numero $phoneNumber")
            result
        } catch (e: SecurityException) {
            result.put("status", "error")
            result.put("reason", "Permesso invio SMS non concesso")
            result
        } catch (e: Exception) {
            result.put("status", "error")
            result.put("reason", e.localizedMessage ?: "Errore sconosciuto nell'invio SMS")
            result
        }
    }

    fun sendWhatsAppMessage(recipient: String, message: String): JSONObject {
        val result = JSONObject()
        try {
            // 1. Try finding an active RemoteInput action from WhatsApp notification cache
            val activeNotifs = NotificationRepository.notifications.value
            val match = activeNotifs.firstOrNull {
                it.isWhatsApp && (it.sender.contains(recipient, ignoreCase = true) || recipient.contains(it.sender, ignoreCase = true))
            }

            if (match != null && match.hasReplyAction) {
                val replyAction = WhatsAppNotificationService.activeReplyActions[match.notificationKey]
                if (replyAction != null) {
                    val sent = WhatsAppNotificationService.sendReply(context, replyAction, message)
                    if (sent) {
                        result.put("status", "success")
                        result.put("method", "RemoteInput")
                        result.put("details", "Risposta WhatsApp inviata direttamente a ${match.sender}")
                        return result
                    }
                }
            }

            // 2. Fallback: launch deep link / Accessibility Service automated send
            val phone = if (recipient.any { it.isDigit() }) recipient else {
                // If it's a name, check SOS contacts or use fallback
                val contact = sosManager.emergencyContacts.value.firstOrNull {
                    it.name.contains(recipient, ignoreCase = true)
                }
                contact?.phoneNumber ?: recipient
            }

            WhatsAppAccessibilityFallbackService.launchFallback(context, phone, message)

            result.put("status", "success")
            result.put("method", "AccessibilityFallback")
            result.put("details", "Apertura WhatsApp e invio messaggio a $recipient in corso...")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message", e)
            result.put("status", "error")
            result.put("reason", e.localizedMessage ?: "Impossibile inviare messaggio WhatsApp")
            return result
        }
    }

    fun triggerEmergencySos(): JSONObject {
        val result = JSONObject()
        sosManager.startSosSequence(coroutineScope)
        result.put("status", "success")
        result.put("action", "SOS avviato. Invio coordinate GPS e chiamata di soccorso in corso.")
        return result
    }

    fun toggleTorch(enable: Boolean? = null): JSONObject {
        val result = JSONObject()
        val newState = if (enable != null) {
            torchController.setTorch(enable)
            enable
        } else {
            val next = !torchController.isTorchOn.value
            torchController.setTorch(next)
            next
        }
        result.put("status", "success")
        result.put("torchState", if (newState) "accesa" else "spenta")
        result.put("details", "La torcia è ora ${if (newState) "accesa" else "spenta"}")
        return result
    }

    fun makePhoneCall(recipientOrNumber: String): JSONObject {
        val result = JSONObject()
        try {
            val phone = if (recipientOrNumber.any { it.isDigit() }) {
                recipientOrNumber
            } else {
                val contact = sosManager.emergencyContacts.value.firstOrNull {
                    it.name.contains(recipientOrNumber, ignoreCase = true)
                }
                contact?.phoneNumber ?: recipientOrNumber
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                result.put("status", "success")
                result.put("details", "Chiamata avviata verso $recipientOrNumber")
            } catch (e: SecurityException) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                result.put("status", "success")
                result.put("details", "Apertura tastierino per chiamare $recipientOrNumber")
            }
        } catch (e: Exception) {
            result.put("status", "error")
            result.put("reason", e.localizedMessage ?: "Impossibile effettuare la chiamata")
        }
        return result
    }

    fun readLatestNotifications(): JSONObject {
        val result = JSONObject()
        val notifs = NotificationRepository.notifications.value
        if (notifs.isEmpty()) {
            result.put("status", "success")
            result.put("count", 0)
            result.put("summary", "Non ci sono nuove notifiche o chiamate perse.")
            return result
        }

        val summaryList = notifs.take(5).map {
            "${it.appName} da ${it.sender}: ${it.message}"
        }

        result.put("status", "success")
        result.put("count", notifs.size)
        result.put("summary", "Hai ${notifs.size} notifiche: " + summaryList.joinToString("; "))
        return result
    }

    fun openApp(appName: String): JSONObject {
        val result = JSONObject()
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        var targetPkg: String? = null
        for (pkg in packages) {
            val label = pm.getApplicationLabel(pkg).toString()
            if (label.contains(appName, ignoreCase = true) || pkg.packageName.contains(appName, ignoreCase = true)) {
                targetPkg = pkg.packageName
                break
            }
        }

        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                result.put("status", "success")
                result.put("details", "Apertura app $appName riuscita.")
                return result
            }
        }

        // Common default apps lookup
        when (appName.lowercase()) {
            "fotocamera", "camera", "foto" -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                result.put("status", "success")
                result.put("details", "Fotocamera aperta.")
                return result
            }
            "whatsapp" -> {
                val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    result.put("status", "success")
                    result.put("details", "WhatsApp aperto.")
                    return result
                }
            }
        }

        result.put("status", "error")
        result.put("reason", "Applicazione '$appName' non trovata sul dispositivo.")
        return result
    }

    fun getDeviceStatus(): JSONObject {
        val result = JSONObject()
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val time = SimpleDateFormat("HH:mm", Locale.ITALIAN).format(Date())
        val date = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date())

        result.put("status", "success")
        result.put("time", time)
        result.put("date", date)
        result.put("batteryLevel", "$batLevel%")
        result.put("torch", if (torchController.isTorchOn.value) "accesa" else "spenta")
        result.put("unreadNotifications", NotificationRepository.notifications.value.size)
        return result
    }
}
