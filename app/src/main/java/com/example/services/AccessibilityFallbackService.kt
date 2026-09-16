package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.accessibility.GestureController
import com.example.accessibility.ScreenDumpService
import com.example.accessibility.UIElement

/**
 * Universal System Automation & Accessibility Service for Easy Launcher.
 * Upgraded from WhatsApp-specific fallback to a full-featured automation controller,
 * providing universal screen tree dumps, coordinate gesture injection, and global navigation.
 */
class AccessibilityFallbackService : AccessibilityService() {

    lateinit var gestureController: GestureController
        private set

    companion object {
        private const val TAG = "AccessibilityFallback"
        const val PKG_WHATSAPP = "com.whatsapp"
        const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        @Volatile
        var instance: AccessibilityFallbackService? = null
            private set

        @Volatile
        var pendingMessage: String? = null

        @Volatile
        var isAutomationActive: Boolean = false

        /**
         * Checks if the Accessibility Service is enabled in Android Settings.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AccessibilityFallbackService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Opens System Accessibility Settings.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /**
         * Opens WhatsApp chat directly via deep link and triggers automated send.
         */
        fun launchFallback(context: Context, phoneNumber: String, message: String) {
            pendingMessage = message
            isAutomationActive = true

            val formattedPhone = phoneNumber.replace(Regex("[^0-9+]"), "").removePrefix("+")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(PKG_WHATSAPP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        gestureController = GestureController(this)
        Log.d(TAG, "AccessibilityFallbackService connected with GestureController.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomationActive || event == null) return

        val packageName = event.packageName?.toString().orEmpty()
        if (packageName == PKG_WHATSAPP || packageName == PKG_WHATSAPP_BUSINESS) {
            automateSendSequence()
        }
    }

    /**
     * Dumps the active screen as structured UI elements for AI processing.
     */
    fun dumpScreen(): List<UIElement> {
        return ScreenDumpService.dumpScreen(this)
    }

    /**
     * Automates message typing and clicking Send in WhatsApp using universal screen dump and coordinate gestures.
     */
    private fun automateSendSequence() {
        val message = pendingMessage ?: return
        val elements = dumpScreen()
        if (elements.isEmpty()) return

        // 1. Locate and fill the input field
        val inputElement = elements.firstOrNull { it.isEditable }
        if (inputElement != null) {
            gestureController.typeText(message)
        }

        // 2. Locate Send button: search by text, description, or common action labels
        val sendLabels = listOf("invia", "send", "rispondi", "inviare", "rispondere")
        val sendElement = elements.firstOrNull { elem ->
            elem.isClickable && sendLabels.any { label ->
                elem.text.contains(label, ignoreCase = true) ||
                elem.contentDescription.contains(label, ignoreCase = true)
            }
        }

        if (sendElement != null) {
            // Tap send via coordinate centroid
            val clickSuccess = gestureController.clickAt(sendElement)
            if (clickSuccess) {
                isAutomationActive = false
                pendingMessage = null
                triggerGlobalHome()
            }
        } else {
            // Fallback: try pressing Enter via soft-keyboard IME action
            val enterSuccess = gestureController.pressEnter()
            if (enterSuccess) {
                isAutomationActive = false
                pendingMessage = null
                triggerGlobalHome()
            }
        }
    }

    fun triggerGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun triggerGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun triggerGlobalNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    override fun onInterrupt() {
        isAutomationActive = false
        pendingMessage = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}

/**
 * Backward compatibility alias for WhatsAppAccessibilityFallbackService.
 */
typealias WhatsAppAccessibilityFallbackService = AccessibilityFallbackService
