# Integration Plan: private-agent Screen Parsing Logic into Easy Launcher

## Executive Summary

**private-agent** (`orailnoor/private-agent`) uses a **production-grade UI accessibility tree parser** in Kotlin that is significantly more robust than Easy Launcher's current screenshot-OCR approach. This document outlines a step-by-step migration strategy to adopt private-agent's coordinate-based screen reading and interaction system while maintaining Easy Launcher's **Gemini Nano offline-first philosophy** and accessibility focus.

---

## 1. Current State Analysis

### Easy Launcher's Current Approach
- **Architecture**: Kotlin + Jetpack Compose + Accessibility Service
- **Screen Parsing**: Manual `AccessibilityNodeInfo` hierarchy traversal + fallback `takeScreenshot()` with ML Kit OCR
- **Limitations**:
  - Manual view ID lookups (WhatsApp hardcoded: `com.whatsapp:id/entry`, `com.whatsapp:id/send`)
  - OCR fallback only triggered on complete node tree failure
  - No structured dump of screen hierarchy for AI analysis
  - Limited coordinate-based gesture support
  - Missing edge cases: custom Canvas-based UIs, WebViews, complex React Native apps

### private-agent's Approach
- **Language**: Flutter/Dart + native Kotlin plugin (`AgentAccessibilityService.kt`)
- **Screen Parsing**: 
  - **Core method**: `dumpScreen()` → traverses entire accessibility tree → returns flat list of interactive elements with coordinates
  - **Filtering**: Removes hidden/zero-size nodes, focuses on clickable/editable/scrollable elements
  - **Coordinates**: Includes `bounds` for each element (left, top, right, bottom)
  - **Fallback**: Screenshot capture (`takeScreenshot()` with Base64 encoding) when direct access fails
- **Strengths**:
  - Universal element discovery (no hardcoding)
  - Structured data suitable for AI analysis
  - Coordinate-based gesture injection (`clickAtCoordinates()`)
  - Proven multi-app automation (Telegram integration, complex task chains)

---

## 2. Key Components to Migrate

### Phase 1: Core Screen Parsing Engine (HIGH PRIORITY)

#### Component: `ScreenDumpService`
**Source**: `orailnoor/private-agent` → `AgentAccessibilityService.kt:87-168` (method `dumpScreen()` + `traverseNode()`)

**What it does**:
- Iterates all accessibility windows
- Recursively traverses node tree
- Filters out invisible/zero-size elements
- Returns structured map of all interactive elements

**Migration Steps**:
1. Create new Kotlin file: `app/src/main/java/com/example/accessibility/ScreenDumpService.kt`
2. Copy the `dumpScreen()` and `traverseNode()` logic from private-agent
3. Adapt to Easy Launcher's logging/context patterns
4. Add as a reusable method in `WhatsAppAccessibilityFallbackService`

**Code Template**:
```kotlin
// NEW FILE: ScreenDumpService.kt
package com.example.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect

object ScreenDumpService {
    
    /**
     * Dump the entire active screen as a structured list of interactive elements.
     * Output suitable for AI analysis and coordinate-based interactions.
     */
    fun dumpScreen(rootNode: AccessibilityNodeInfo): List<Map<String, Any?>> {
        val nodes = mutableListOf<Map<String, Any?>>()
        traverseNode(rootNode, nodes, depth = 0)
        return nodes
    }
    
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        nodes: MutableList<Map<String, Any?>>,
        depth: Int
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        val isZeroSize = rect.width() <= 0 || rect.height() <= 0
        if (!node.isVisibleToUser || isZeroSize) {
            // Skip invisible/zero-size, but continue traversing children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverseNode(child, nodes, depth + 1)
                child.recycle()
            }
            return
        }
        
        // Include if has text, description, or is interactive
        if (node.text?.toString()?.isNotEmpty() == true ||
            node.contentDescription?.toString()?.isNotEmpty() == true ||
            node.isClickable || node.isEditable || node.isScrollable
        ) {
            nodes.add(mapOf(
                "index" to nodes.size,
                "text" to (node.text?.toString() ?: ""),
                "contentDescription" to (node.contentDescription?.toString() ?: ""),
                "className" to (node.className?.toString()?.substringAfterLast('.') ?: ""),
                "viewId" to (node.viewIdResourceName ?: ""),
                "isClickable" to node.isClickable,
                "isEditable" to node.isEditable,
                "isScrollable" to node.isScrollable,
                "bounds" to mapOf(
                    "left" to rect.left,
                    "top" to rect.top,
                    "right" to rect.right,
                    "bottom" to rect.bottom,
                    "centerX" to rect.centerX(),
                    "centerY" to rect.centerY()
                ),
                "depth" to depth
            ))
        }
        
        // Traverse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, nodes, depth + 1)
            child.recycle()
        }
    }
}
```

**Integration Point**: Call from `WhatsAppAccessibilityFallbackService.onAccessibilityEvent()` before attempting manual node lookups.

---

### Phase 2: Coordinate-Based Gesture System (HIGH PRIORITY)

#### Component: `CoordinateGestureController`
**Source**: `orailnoor/private-agent` → `AgentAccessibilityService.kt:273-483`

**What it does**:
- `clickAtCoordinates(x, y)` → constructs `Path` and dispatches gesture
- `swipe(startX, startY, endX, endY)` → swipe gesture
- `longPressAt(x, y)` → long-press gesture
- `pressEnter()` → IME submission with keyboard-aware fallbacks

**Current Easy Launcher Status**: Partial support exists via `dispatchGesture()`, but not systematized.

**Migration Steps**:
1. Create `app/src/main/java/com/example/accessibility/GestureController.kt`
2. Consolidate all gesture operations from private-agent
3. Integrate with existing `WhatsAppAccessibilityFallbackService`

**Code Template**:
```kotlin
// NEW FILE: GestureController.kt
package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi

class GestureController(private val service: AccessibilityService) {
    
    /**
     * Click at specific screen coordinates using gesture injection.
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Swipe from (startX, startY) to (endX, endY).
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Long press at coordinates.
     */
    fun longPressAt(x: Float, y: Float, durationMs: Long = 1000): Boolean {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Type text into the currently focused editable field, with hint support.
     */
    fun typeText(text: String, fieldHint: String? = null): Boolean {
        for (window in service.windows ?: emptyList()) {
            val root = window.root ?: continue
            var editNode = findEditableNode(root, fieldHint)
            if (editNode == null && !fieldHint.isNullOrEmpty()) {
                editNode = findEditableNode(root, null)
            }
            
            if (editNode != null) {
                editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                val success = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                root.recycle()
                return success
            }
            root.recycle()
        }
        return false
    }
    
    /**
     * Press Enter / IME action on focused field.
     */
    fun pressEnter(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (window in service.windows ?: emptyList()) {
                val root = window.root ?: continue
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                val submitted = focused?.performAction(
                    AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
                ) == true
                focused?.recycle()
                root.recycle()
                if (submitted) return true
            }
        }
        
        for (window in service.windows ?: emptyList()) {
            val root = window.root ?: continue
            val actionNode = findKeyboardActionNode(root)
            val submitted = actionNode != null && clickNodeOrParent(actionNode)
            actionNode?.recycle()
            root.recycle()
            if (submitted) return true
        }
        
        return false
    }
    
    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
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
    
    private fun findKeyboardActionNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = (node.text?.toString().orEmpty()).trim().lowercase()
        val actionLabels = setOf("search", "enter", "go", "done", "send", "next")
        if (node.isClickable && (label in actionLabels || label.endsWith(" search"))) {
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
    
    private fun findEditableNode(node: AccessibilityNodeInfo, hint: String?): AccessibilityNodeInfo? {
        if (node.isEditable) {
            if (hint == null) return node
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val hintText = node.hintText?.toString() ?: ""
            if (text.contains(hint, ignoreCase = true) ||
                desc.contains(hint, ignoreCase = true) ||
                hintText.contains(hint, ignoreCase = true)
            ) {
                return node
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child, hint)
            if (found != null) return found
            child.recycle()
        }
        return null
    }
}
```

**Integration Point**: Inject into `WhatsAppAccessibilityFallbackService` or create as singleton dependency.

---

### Phase 3: AI-Ready Screen Analysis (MEDIUM PRIORITY)

#### Component: `ScreenAnalyzerForAI`
**Purpose**: Convert `dumpScreen()` output into JSON suitable for Gemini Nano analysis.

**Migration Steps**:
1. Create `app/src/main/java/com/example/ai/ScreenAnalyzerForAI.kt`
2. Serialize screen dump to compact JSON
3. Include optional Base64 screenshot for visual fallback

**Code Template**:
```kotlin
// NEW FILE: ScreenAnalyzerForAI.kt
package com.example.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UIElement(
    val index: Int,
    val text: String,
    val contentDescription: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Bounds
) {
    @Serializable
    data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val centerX: Int,
        val centerY: Int
    )
}

@Serializable
data class ScreenAnalysis(
    val timestamp: Long,
    val packageName: String,
    val elements: List<UIElement>,
    val screenshot: String? = null  // Base64 encoded JPEG
)

object ScreenAnalyzerForAI {
    private val json = Json { prettyPrint = true }
    
    fun analyzeScreenForAI(
        screenDump: List<Map<String, Any?>>,
        packageName: String,
        screenshot: String? = null
    ): String {
        val elements = screenDump.mapNotNull { dump ->
            val bounds = (dump["bounds"] as? Map<String, Any?>)?.let { b ->
                UIElement.Bounds(
                    left = (b["left"] as? Number)?.toInt() ?: 0,
                    top = (b["top"] as? Number)?.toInt() ?: 0,
                    right = (b["right"] as? Number)?.toInt() ?: 0,
                    bottom = (b["bottom"] as? Number)?.toInt() ?: 0,
                    centerX = (b["centerX"] as? Number)?.toInt() ?: 0,
                    centerY = (b["centerY"] as? Number)?.toInt() ?: 0
                )
            } ?: return@mapNotNull null
            
            UIElement(
                index = (dump["index"] as? Number)?.toInt() ?: -1,
                text = dump["text"].toString(),
                contentDescription = dump["contentDescription"].toString(),
                className = dump["className"].toString(),
                isClickable = dump["isClickable"] as? Boolean ?: false,
                isEditable = dump["isEditable"] as? Boolean ?: false,
                isScrollable = dump["isScrollable"] as? Boolean ?: false,
                bounds = bounds
            )
        }
        
        val analysis = ScreenAnalysis(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            elements = elements,
            screenshot = screenshot
        )
        
        return json.encodeToString(ScreenAnalysis.serializer(), analysis)
    }
}
```

---

### Phase 4: Screenshot-to-OCR Fallback (MEDIUM PRIORITY)

#### Component: Enhanced `OCRFallbackManager`
**Source**: Enhance existing ML Kit integration with private-agent's `takeScreenshot()` method.

**Current State**: Easy Launcher has `takeScreenshot()` in docs, but implementation is incomplete.

**Enhancement**:
```kotlin
// ENHANCEMENT: In WhatsAppAccessibilityFallbackService.kt

@RequiresApi(Build.VERSION_CODES.R)
private fun takeScreenshotForOCRFallback(callback: (Bitmap?) -> Unit) {
    // Use private-agent's Base64 encoding approach for cloud AI fallback
    takeScreenshot(
        Display.DEFAULT_DISPLAY,
        mainExecutor,
        object : TakeScreenshotCallback {
            override fun onSuccess(screenshotResult: ScreenshotResult) {
                val hardwareBuffer = screenshotResult.hardwareBuffer
                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
                hardwareBuffer.close()
                callback(bitmap)
            }
            
            override fun onFailure(errorCode: Int) {
                callback(null)
            }
        }
    )
}
```

---

## 3. Integration Architecture

### New Flow: Hybrid Parsing Strategy

```
┌─────────────────────────────────────────────────────────────┐
│ UI Interaction Request (Voice Command → NLU Output)         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │   WhatsAppAccessibilityFB    │
        │   (Enhanced)                 │
        └──────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼ (STEP 1)                    │
┌──────────────────────┐              │
│  ScreenDumpService   │              │
│  .dumpScreen()       │── SUCCESS──┐  │
└──────────────────────┘            │  │
        │                           │  │
     FAIL                           │  │
        │                           │  │
        ▼ (STEP 2)                  │  │
┌──────────────────────────┐        │  │
│ takeScreenshot() +       │        │  │
│ ML Kit OCR               │        │  │
└──────────────────────────┘        │  │
        │                           │  │
        └───────────┬───────────────┘  │
                    ▼                  ▼
            ┌──────────────────────────────────┐
            │  ScreenAnalyzerForAI              │
            │  (Struct. JSON or Base64 Image)  │
            └──────────────────────────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  GeminiNanoOrchestrator    │
        │  (Analyze + Decide Action) │
        └───────────────────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  GestureController         │
        │  .clickAtCoordinates()     │
        │  .typeText()               │
        │  .pressEnter()             │
        └───────────────────────────┘
                    │
                    ▼
            ┌─────────────────┐
            │ Action Executed │
            │ (UI Automated)  │
            └─────────────────┘
```

---

## 4. Phase-by-Phase Implementation Timeline

### Phase 1: Foundation (Week 1-2) — **CRITICAL PATH**
- [ ] Create `ScreenDumpService.kt`
- [ ] Integrate into `WhatsAppAccessibilityFallbackService`
- [ ] Write unit tests for dump accuracy
- [ ] Verify backward compatibility with existing automation

**Deliverable**: Structured screen dumps flowing into existing workflow.

---

### Phase 2: Gesture Enhancement (Week 2-3)
- [ ] Create `GestureController.kt`
- [ ] Replace manual gesture calls with new controller
- [ ] Add swipe/long-press support to test suite
- [ ] Integration testing on real devices

**Deliverable**: Reliable coordinate-based interaction engine.

---

### Phase 3: AI Bridge (Week 3-4)
- [ ] Create `ScreenAnalyzerForAI.kt`
- [ ] Feed structured JSON to Gemini Nano prompts
- [ ] Add decision logging for debugging
- [ ] Baseline accuracy metrics

**Deliverable**: AI can reason about screen state from structured data.

---

### Phase 4: Fallback OCR (Week 4-5)
- [ ] Implement screenshot capture
- [ ] Integrate ML Kit OCR as fallback
- [ ] Add base64 encoding for cloud fallback
- [ ] Performance benchmarking

**Deliverable**: Graceful degradation when accessibility tree is unavailable.

---

### Phase 5: Polish & Documentation (Week 5-6)
- [ ] Remove hardcoded view IDs from WhatsApp service
- [ ] Generalize automation for multi-app scenarios
- [ ] Add Telegram remote control support (optional)
- [ ] Write architecture guide for contributors

**Deliverable**: Production-ready, multi-app automation system.

---

## 5. Code Diff Outline

### Changes to `WhatsAppAccessibilityFallbackService.kt`

```kotlin
// BEFORE: Manual node lookups, hardcoded IDs
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (!isAutomationActive || event == null) return
    val rootNode = rootInActiveWindow ?: return
    val packageName = event.packageName?.toString() ?: ""
    
    if (packageName == PKG_WHATSAPP || packageName == PKG_WHATSAPP_BUSINESS) {
        automateSendSequence(rootNode)  // Hardcoded view ID lookups
    }
}

// AFTER: Universal screen parsing
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (!isAutomationActive || event == null) return
    val rootNode = rootInActiveWindow ?: return
    val packageName = event.packageName?.toString() ?: ""
    
    // Step 1: Dump entire screen structure
    val screenDump = ScreenDumpService.dumpScreen(rootNode)
    
    // Step 2: Analyze for AI
    val screenJson = ScreenAnalyzerForAI.analyzeScreenForAI(screenDump, packageName)
    
    // Step 3: Decide action via Gemini Nano
    val action = geminiBridge.analyzeAndDecideAction(screenJson, currentTask)
    
    // Step 4: Execute via gesture controller
    executeAction(action)
}

private fun executeAction(action: AIAction) {
    when (action.type) {
        "click" -> gestureController.clickAtCoordinates(action.x, action.y)
        "type" -> gestureController.typeText(action.text)
        "enter" -> gestureController.pressEnter()
        "swipe" -> gestureController.swipe(action.startX, action.startY, action.endX, action.endY)
    }
}
```

---

## 6. Compatibility & Risk Mitigation

### Backward Compatibility
- **Keep existing `findAccessibilityNodeInfosByViewId()` calls** as fast path (no allocation overhead)
- **Use `dumpScreen()` as fallback** when ID lookup fails
- **No breaking changes** to external APIs

### Device Compatibility
- Test on Android 8.0 (API 26) minimum ✓
- `takeScreenshot()` requires API 31 (R) → guard with `@RequiresApi`
- Fall back to gesture-only if screenshot unavailable

### Performance
- `dumpScreen()` traversal is ~50-100ms on typical screens
- Parallelize with AI inference (non-blocking)
- Cache screen dumps within same event cycle

---

## 7. Testing Strategy

### Unit Tests
- `ScreenDumpService` on mock `AccessibilityNodeInfo` hierarchy
- `GestureController` coordinate calculations
- JSON serialization in `ScreenAnalyzerForAI`

### Integration Tests
- Real app automation: WhatsApp, SMS, Telegram
- Screenshot OCR fallback scenarios
- Multi-step task chains (open app → navigate → send message → close)

### Real Device Testing
- Nexus 5X (API 26, low-end device) → verify graceful degradation
- Pixel 6 (API 31+) → full feature set
- Gesture reliability under different screen densities

---

## 8. Future Extensions (Post-v1.0)

### Telegram Integration (from private-agent)
```kotlin
// Leverage new ScreenDumpService for remote task execution
object TelegramAgentBridge {
    fun executeRemoteTask(screenDump: List<Map<String, Any?>>, instruction: String) {
        val analysis = ScreenAnalyzerForAI.analyzeScreenForAI(screenDump, packageName)
        val action = geminiBridge.decideAction(analysis, instruction)
        gestureController.executeAction(action)
    }
}
```

### Multi-Step Automation Chains
```kotlin
// Sequence-based task execution
data class TaskSequence(
    val steps: List<TaskStep>,
    val retryOnFailure: Boolean = true
)

data class TaskStep(
    val instruction: String,
    val targetPackage: String? = null,
    val timeout: Long = 5000
)
```

---

## 9. Migration Checklist

- [ ] Fork & review `AgentAccessibilityService.kt` from private-agent
- [ ] Implement `ScreenDumpService.kt`
- [ ] Create `GestureController.kt`
- [ ] Write unit tests (>80% coverage)
- [ ] Create `ScreenAnalyzerForAI.kt` + JSON serialization tests
- [ ] Enhance `WhatsAppAccessibilityFallbackService` with new flow
- [ ] Integration test on 2+ real devices
- [ ] Document in-code with JavaDoc
- [ ] Update README with new architecture
- [ ] Performance baseline (dump time, gesture latency)
- [ ] Publish as PR to Easy-Launcher

---

## 10. References & Resources

| Resource | Link | Purpose |
|----------|------|---------|
| private-agent AccessibilityService | `android/app/src/main/.../AgentAccessibilityService.kt` | Core reference implementation |
| Easy Launcher Accessibility Service | `app/src/main/java/.../WhatsAppAccessibilityFallbackService.kt` | Current baseline |
| Android Accessibility API | https://developer.android.com/guide/topics/ui/accessibility | Official docs |
| AccessibilityNodeInfo Reference | https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo | Node traversal API |
| GestureDescription API | https://developer.android.com/reference/android/accessibilityservice/GestureDescription | Gesture injection |

---

## Conclusion

Integrating private-agent's screen parsing logic will **dramatically improve Easy Launcher's reliability** across diverse apps without breaking Easy Launcher's core strength: **Gemini Nano offline inference**. The migration is **low-risk** (backward compatible), **phased** (deliverables every 1-2 weeks), and **testable** (comprehensive test coverage at each phase).

**Next Step**: Review this plan with the Easy Launcher team, then begin Phase 1 implementation.
