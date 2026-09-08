package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Fallback AccessibilityService to automate UI interactions in WhatsApp
 * and provide global system navigation for motor-impaired and senior users.
 */
class WhatsAppAccessibilityFallbackService : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityFallback"
        const val PKG_WHATSAPP = "com.whatsapp"
        const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        private const val ID_ENTRY_FIELD = "com.whatsapp:id/entry"
        private const val ID_SEND_BUTTON = "com.whatsapp:id/send"

        @Volatile
        var instance: WhatsAppAccessibilityFallbackService? = null
            private set

        @Volatile
        var pendingMessage: String? = null

        @Volatile
        var isAutomationActive: Boolean = false

        /**
         * Checks if Accessibility Service is enabled in Android Settings.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${WhatsAppAccessibilityFallbackService::class.java.canonicalName}"
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
         * Opens the target chat directly via deep link and triggers fallback automation.
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
                // Fallback to generic browser/intent if WhatsApp direct package fails
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
        Log.d(TAG, "WhatsAppAccessibilityFallbackService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomationActive || event == null) return

        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: ""

        if (packageName == PKG_WHATSAPP || packageName == PKG_WHATSAPP_BUSINESS) {
            automateSendSequence(rootNode)
        }
    }

    private fun automateSendSequence(rootNode: AccessibilityNodeInfo) {
        val message = pendingMessage ?: return

        // 1. Locate message input field by view ID or EditText class
        var inputNode: AccessibilityNodeInfo? = null
        val inputNodes = rootNode.findAccessibilityNodeInfosByViewId(ID_ENTRY_FIELD)
        if (!inputNodes.isNullOrEmpty()) {
            inputNode = inputNodes[0]
        } else {
            inputNode = findFirstNodeByClassName(rootNode, "android.widget.EditText")
        }

        if (inputNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            }
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // 2. Locate and execute click on the send button
        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId(ID_SEND_BUTTON)
        var sendNode: AccessibilityNodeInfo? = null
        if (!sendNodes.isNullOrEmpty()) {
            sendNode = sendNodes[0]
        } else {
            // Find ImageButton with send description
            sendNode = findNodeByDescription(rootNode, "Invia", "Send")
        }

        if (sendNode != null) {
            val clickSuccess = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clickSuccess) {
                isAutomationActive = false
                pendingMessage = null
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun findFirstNodeByClassName(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (node.className?.toString() == className) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstNodeByClassName(child, className)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByDescription(node: AccessibilityNodeInfo, vararg descriptions: String): AccessibilityNodeInfo? {
        val nodeDesc = node.contentDescription?.toString()
        if (nodeDesc != null && descriptions.any { nodeDesc.contains(it, ignoreCase = true) }) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByDescription(child, *descriptions)
            if (found != null) return found
        }
        return null
    }

    fun triggerGlobalHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun triggerGlobalBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun triggerGlobalNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

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
