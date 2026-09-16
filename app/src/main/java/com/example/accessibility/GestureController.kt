package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Universal Coordinate & Node Gesture Controller ported from private-agent.
 * Enables coordinate-based gesture injection, soft-keyboard IME submission,
 * editable field focus, and text entry without relying on brittle view IDs.
 */
class GestureController(private val service: AccessibilityService) {

    private val ownPackageName: String = service.packageName

    /**
     * Injects a tap gesture at exact screen coordinates (X, Y).
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Taps the spatial centroid of a UI element.
     */
    fun clickAt(element: UIElement): Boolean {
        return clickAtCoordinates(element.bounds.centerX.toFloat(), element.bounds.centerY.toFloat())
    }

    /**
     * Dispatches a swipe gesture from start coordinates to end coordinates.
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Injects a long-press gesture at coordinates (default 1000ms).
     */
    fun longPressAt(x: Float, y: Float, durationMs: Long = 1000L): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Finds and clicks a node by its text or content description.
     */
    fun clickByText(
        targetText: String,
        exactOnly: Boolean = false,
        skipEditable: Boolean = true
    ): Boolean {
        val windows = service.windows
        if (windows.isNullOrEmpty()) {
            val root = service.rootInActiveWindow ?: return false
            if (root.packageName?.toString() == ownPackageName) {
                root.recycle()
                return false
            }
            val success = findAndClickNode(root, targetText, exactOnly, skipEditable)
            root.recycle()
            return success
        }

        for (window in windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() == ownPackageName) {
                root.recycle()
                continue
            }
            val result = findAndClickNode(root, targetText, true, true) ||
                    findAndClickNode(root, targetText, false, true) ||
                    findAndClickNode(root, targetText, true, false) ||
                    findAndClickNode(root, targetText, false, false)
            root.recycle()
            if (result) return true
        }
        return false
    }

    private fun findAndClickNode(
        node: AccessibilityNodeInfo,
        targetText: String,
        exactOnly: Boolean,
        skipEditable: Boolean
    ): Boolean {
        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()

        val exactMatch = text.equals(targetText, ignoreCase = true) || desc.equals(targetText, ignoreCase = true)
        val containsMatch = text.contains(targetText, ignoreCase = true) || desc.contains(targetText, ignoreCase = true)
        val matches = if (exactOnly) exactMatch else containsMatch

        if (matches && (!skipEditable || !node.isEditable) && clickNodeOrCentroid(node)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNode(child, targetText, exactOnly, skipEditable)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun clickNodeOrCentroid(node: AccessibilityNodeInfo): Boolean {
        var clickTarget: AccessibilityNodeInfo? = node
        while (clickTarget != null && !clickTarget.isClickable) {
            clickTarget = clickTarget.parent
        }
        if (clickTarget?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
            return true
        }

        val rect = Rect()
        node.getBoundsInScreen(rect)
        return !rect.isEmpty && clickAtCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
    }

    /**
     * Focuses the target or first editable field and injects text via ACTION_SET_TEXT.
     */
    fun typeText(text: String, fieldHint: String? = null): Boolean {
        val windows = service.windows
        if (windows.isNullOrEmpty()) {
            val root = service.rootInActiveWindow ?: return false
            val success = setEditTextInTree(root, text, fieldHint)
            root.recycle()
            return success
        }

        for (window in windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() == ownPackageName) {
                root.recycle()
                continue
            }
            if (setEditTextInTree(root, text, fieldHint)) {
                root.recycle()
                return true
            }
            root.recycle()
        }
        return false
    }

    private fun setEditTextInTree(
        root: AccessibilityNodeInfo,
        text: String,
        fieldHint: String?
    ): Boolean {
        var editNode = findEditableNode(root, fieldHint)
        if (editNode == null && !fieldHint.isNullOrEmpty()) {
            editNode = findEditableNode(root, null)
        }

        if (editNode != null) {
            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            editNode.recycle()
            return success
        }
        return false
    }

    private fun findEditableNode(node: AccessibilityNodeInfo, hint: String?): AccessibilityNodeInfo? {
        if (node.isEditable) {
            if (hint == null) return AccessibilityNodeInfo.obtain(node)
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.toString().orEmpty()
            } else ""

            if (text.contains(hint, ignoreCase = true) ||
                desc.contains(hint, ignoreCase = true) ||
                hintText.contains(hint, ignoreCase = true)
            ) {
                return AccessibilityNodeInfo.obtain(node)
            }
            if (hint.isEmpty()) return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child, hint)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    /**
     * Submits the focused input via 3-tier fallback strategy:
     * 1. ACTION_IME_ENTER on focused input (Android 11+)
     * 2. Search keyboard action labels (e.g. "send", "invia", "cerca", "enter")
     * 3. Coordinate tap on bottom-right of TYPE_INPUT_METHOD window (soft keyboard).
     */
    fun pressEnter(): Boolean {
        // Tier 1: Action IME Enter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windows = service.windows
            if (!windows.isNullOrEmpty()) {
                for (window in windows) {
                    val root = window.root ?: continue
                    if (root.packageName?.toString() == ownPackageName) {
                        root.recycle()
                        continue
                    }
                    val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    val submitted = focused?.performAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
                    ) == true
                    focused?.recycle()
                    root.recycle()
                    if (submitted) return true
                }
            }
        }

        // Tier 2: Search keyboard action nodes
        val windows = service.windows
        if (!windows.isNullOrEmpty()) {
            for (window in windows) {
                val root = window.root ?: continue
                if (root.packageName?.toString() == ownPackageName) {
                    root.recycle()
                    continue
                }
                val actionNode = findKeyboardActionNode(root)
                val submitted = actionNode != null && clickNodeOrCentroid(actionNode)
                actionNode?.recycle()
                root.recycle()
                if (submitted) return true
            }

            // Tier 3: Tap inside the soft keyboard input method window
            for (window in windows) {
                if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) {
                    val x = bounds.right - (bounds.width() * 0.10f)
                    val y = bounds.bottom - (bounds.height() * 0.14f)
                    return clickAtCoordinates(x, y)
                }
            }
        }

        return false
    }

    private fun findKeyboardActionNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.text?.toString() ?: node.contentDescription?.toString()).orEmpty().trim().lowercase()
        val actionLabels = setOf("search", "cerca", "enter", "invio", "invia", "go", "vai", "done", "fatto", "send", "next", "avanti")
        if (node.isClickable && (label in actionLabels || label.endsWith(" search") || label.endsWith(" invia"))) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findKeyboardActionNode(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    /**
     * Scrolls forward or backward on the first scrollable element or target text node.
     */
    fun scroll(direction: String, targetText: String? = null): Boolean {
        val windows = service.windows ?: return false
        for (window in windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() == ownPackageName) {
                root.recycle()
                continue
            }
            val scrollNode = findScrollableNode(root, targetText)
            if (scrollNode != null) {
                val action = when (direction.lowercase()) {
                    "down", "forward", "giù", "avanti" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    "up", "backward", "su", "indietro" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                val success = scrollNode.performAction(action)
                scrollNode.recycle()
                root.recycle()
                return success
            }
            root.recycle()
        }
        return false
    }

    private fun findScrollableNode(
        node: AccessibilityNodeInfo,
        targetText: String?
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            if (targetText == null) return AccessibilityNodeInfo.obtain(node)
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            if (text.contains(targetText, ignoreCase = true) || desc.contains(targetText, ignoreCase = true)) {
                return AccessibilityNodeInfo.obtain(node)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child, targetText)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    /**
     * Triggers the global Back button action.
     */
    fun pressBack(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Triggers the global Home button action.
     */
    fun pressHome(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}
