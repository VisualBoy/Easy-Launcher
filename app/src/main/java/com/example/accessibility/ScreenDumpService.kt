package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream

data class ScreenBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val centerX: Int = (left + right) / 2,
    val centerY: Int = (top + bottom) / 2
)

data class UIElement(
    val index: Int,
    val text: String,
    val contentDescription: String,
    val className: String,
    val viewId: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    val isFocused: Boolean = false,
    val bounds: ScreenBounds,
    val depth: Int
)

/**
 * Universal Accessibility Tree Parser based on the production-grade private-agent architecture.
 * Recursively traverses all active windows, eliminates zero-size/invisible nodes,
 * extracts spatial bounds and formats compressed text representations for Gemini Nano.
 */
object ScreenDumpService {

    /**
     * Dumps the entire active screen hierarchy as a flat list of interactive/descriptive UI elements.
     */
    fun dumpScreen(service: AccessibilityService, ownPackageName: String? = null): List<UIElement> {
        val appPkg = ownPackageName ?: service.packageName
        val elements = mutableListOf<UIElement>()
        val allWindows = service.windows

        if (allWindows.isNullOrEmpty()) {
            val root = service.rootInActiveWindow ?: return emptyList()
            if (root.packageName?.toString() != appPkg) {
                traverseNode(root, elements, 0)
            }
            root.recycle()
            return elements
        }

        for (window in allWindows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() == appPkg) {
                root.recycle()
                continue
            }
            traverseNode(root, elements, 0)
            root.recycle()
        }

        return elements
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        elements: MutableList<UIElement>,
        depth: Int
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val isZeroSize = rect.width() <= 0 || rect.height() <= 0
        if (!node.isVisibleToUser || isZeroSize) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverseNode(child, elements, depth + 1)
                child.recycle()
            }
            return
        }

        val text = node.text?.toString().orEmpty()
        val contentDesc = node.contentDescription?.toString().orEmpty()
        val className = node.className?.toString().orEmpty().substringAfterLast('.')
        val viewId = node.viewIdResourceName.orEmpty()

        if (text.isNotEmpty() || contentDesc.isNotEmpty() ||
            node.isClickable || node.isEditable || node.isScrollable
        ) {
            elements.add(
                UIElement(
                    index = elements.size,
                    text = text,
                    contentDescription = contentDesc,
                    className = className,
                    viewId = viewId,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    isCheckable = node.isCheckable,
                    isChecked = node.isChecked,
                    isFocused = node.isFocused,
                    bounds = ScreenBounds(
                        left = rect.left,
                        top = rect.top,
                        right = rect.right,
                        bottom = rect.bottom
                    ),
                    depth = depth
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, elements, depth + 1)
            child.recycle()
        }
    }

    /**
     * Formats the screen dump into a token-efficient text representation for LLM prompt ingestion.
     * Highlights elements matching target query keywords to guide attention.
     */
    fun getCompressedScreenDescription(
        elements: List<UIElement>,
        taskGoal: String = ""
    ): String {
        if (elements.isEmpty()) return "SCREEN_STATE: (empty or blocked)"

        val keywords = taskGoal.lowercase()
            .replace(Regex("[^a-zàèéìòù0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }

        val buffer = StringBuilder()
        buffer.appendLine("SCREEN_STATE:")

        for (elem in elements) {
            val label = elem.text.ifEmpty { elem.contentDescription }
            if (label.isEmpty() && !elem.isClickable && !elem.isEditable) continue

            val truncatedLabel = if (label.length > 45) "${label.take(45)}..." else label
            val tags = mutableListOf<String>()
            if (elem.isClickable) tags.add("tap")
            if (elem.isEditable) tags.add("edit")
            if (elem.isScrollable) tags.add("scroll")
            if (elem.isFocused) tags.add("focused")

            val isTarget = keywords.any { kw -> label.lowercase().contains(kw) }
            val mark = if (isTarget) "*" else " "

            buffer.appendLine(
                "[$mark${elem.index}] \"$truncatedLabel\" (${tags.joinToString(",")}) at [${elem.bounds.centerX},${elem.bounds.centerY}]"
            )
        }

        return buffer.toString()
    }

    /**
     * Captures a Base64-encoded compressed JPEG screenshot as visual fallback (API 30+).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotBase64(
        service: AccessibilityService,
        callback: (String?) -> Unit
    ) {
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                    val hardwareBuffer = screenshotResult.hardwareBuffer
                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                        ?.copy(Bitmap.Config.ARGB_8888, false)
                    hardwareBuffer.close()

                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        callback(base64String)
                    } else {
                        callback(null)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    callback(null)
                }
            }
        )
    }
}
