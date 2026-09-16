# Dart/Flutter Bridge Pattern: ScreenAutomationService

## Overview

**private-agent** separates concerns into two layers:
1. **Kotlin Native Layer** (`AgentAccessibilityService.kt`) — Direct Android Accessibility API access
2. **Dart/Flutter Layer** (`ScreenAutomationService.dart`) — Clean async API for business logic

This enables:
- ✅ **Testable Dart code** — Mock the MethodChannel
- ✅ **Type-safe async/await** — No callback hell
- ✅ **Single source of truth** — One place to manage all accessibility calls
- ✅ **Easy AI integration** — Feed screen data directly to LLM

---

## Architecture: MethodChannel Bridge

```
┌─────────────────────────────────────────────────────────┐
│                   Dart/Flutter App                      │
│  (Business Logic, AI Integration, Task Execution)       │
└────────────────────┬────────────────────────────────────┘
                     │
        MethodChannel("com.privateagent/accessibility")
        (Async Method Calls with Serialization)
                     │
        ┌────────────▼────────────┐
        │   MainActivity.kt        │
        │  (MethodCallHandler)    │
        └────────────┬────────────┘
                     │
        ┌────────────▼──────────────────────┐
        │  AgentAccessibilityService        │
        │  (Native Accessibility Impl)      │
        └─────────────────────────────────┘
```

---

## Key Components in private-agent

### 1. Dart Service: `ScreenAutomationService.dart`

```dart
class ScreenAutomationService {
  static const _channel = MethodChannel('com.privateagent/accessibility');
  
  // Generic invoke with timeout
  static Future<T?> _invoke<T>(String method, [Map<String, Object?>? args]) {
    return _channel
        .invokeMethod<T>(method, args)
        .timeout(Duration(seconds: 3), onTimeout: () {
          throw TimeoutException('No reply from $method within 3s');
        });
  }
  
  // Example: Dump screen (Dart wrapper)
  Future<List<Map<String, dynamic>>> dumpScreen() async {
    try {
      final result = await _channel.invokeMethod<List>('dumpScreen');
      if (result == null) return [];
      return result.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    } catch (e) {
      return [];
    }
  }
  
  // Example: Click at coordinates (Dart wrapper)
  Future<bool> clickAt(double x, double y) async {
    try {
      return await _channel.invokeMethod<bool>('clickAt', {'x': x, 'y': y}) ?? false;
    } catch (e) {
      return false;
    }
  }
}
```

**Key Points**:
- Single MethodChannel instance (line 8)
- Generic `_invoke<T>()` reduces boilerplate
- 3-second timeout prevents UI freeze
- Returns structured data (List, bool, String)

---

### 2. Kotlin Handler: `MainActivity.kt` (MethodCallHandler)

```kotlin
override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    
    // Register accessibility channel
    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.privateagent/accessibility")
        .setMethodCallHandler { call, result ->
            when (call.method) {
                // dumpScreen: Delegate to native service
                "dumpScreen" -> {
                    val service = AgentAccessibilityService.instance
                    if (service == null) {
                        result.error("SERVICE_NOT_RUNNING", "...", null)
                    } else {
                        val screenDump = service.dumpScreen()
                        result.success(screenDump)  // Serializes List<Map> → Dart
                    }
                }
                
                // clickAt: Coordinate-based tap
                "clickAt" -> {
                    val x = call.argument<Double>("x")?.toFloat() ?: 0f
                    val y = call.argument<Double>("y")?.toFloat() ?: 0f
                    val service = AgentAccessibilityService.instance
                    if (service == null) {
                        result.error("SERVICE_NOT_RUNNING", "...", null)
                    } else {
                        result.success(service.clickAtCoordinates(x, y))
                    }
                }
                
                // ... more methods
                else -> result.notImplemented()
            }
        }
}
```

**Key Points**:
- One-to-one mapping between Dart methods and Kotlin handlers
- Automatic type marshalling (Double → Float, Bool → Boolean)
- Error handling via `result.error()` (becomes Dart exception)
- Success via `result.success(data)` (becomes Dart return value)

---

## Why Easy Launcher Should Adopt This Pattern

### Current Easy Launcher Approach (Kotlin-Only)
```kotlin
// Problem: Hard to reason about state from Kotlin
// No structured way to feed data to Gemini Nano

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    automateSendSequence(rootNode)  // Tightly coupled, manual state
}
```

### With Bridge Pattern (Kotlin + Dart)
```dart
// Dart: Clean, testable business logic
Future<void> sendMessageViaWhatsApp(String phone, String message) async {
  // Step 1: Get screen state
  final screenDump = await screenAutomation.dumpScreen();
  
  // Step 2: Send to Gemini Nano
  final action = await geminiBridge.analyzeScreen(screenDump, message);
  
  // Step 3: Execute action
  if (action.type == 'click') {
    await screenAutomation.clickAt(action.x, action.y);
  }
}
```

**Advantages**:
- ✅ Testable Dart code (mock MethodChannel)
- ✅ Clean separation of concerns
- ✅ Easy to log/debug (all calls go through one service)
- ✅ Natural fit with AI reasoning (Dart as orchestrator)

---

## Implementation for Easy Launcher

### Step 1: Refactor Kotlin Service

**File**: `app/src/main/java/com/example/accessibility/AccessibilityBridgeHandler.kt`

```kotlin
package com.example.accessibility

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

object AccessibilityBridgeHandler {
    
    fun registerWithFlutterEngine(engine: FlutterEngine, context: Context) {
        MethodChannel(
            engine.dartExecutor.binaryMessenger,
            "com.example.easylauncher/accessibility"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                // Screen dumping
                "dumpScreen" -> {
                    val service = WhatsAppAccessibilityFallbackService.instance
                    if (service == null) {
                        result.error("SERVICE_DOWN", "Accessibility not available", null)
                    } else {
                        val dump = ScreenDumpService.dumpScreen(
                            service.rootInActiveWindow ?: run {
                                result.success(emptyList<Map<String, Any?>>())
                                return@setMethodCallHandler
                            }
                        )
                        result.success(dump)
                    }
                }
                
                // Click at coordinates
                "clickAt" -> {
                    val x = call.argument<Double>("x")?.toFloat() ?: 0f
                    val y = call.argument<Double>("y")?.toFloat() ?: 0f
                    val service = WhatsAppAccessibilityFallbackService.instance
                    if (service == null) {
                        result.error("SERVICE_DOWN", "Accessibility not available", null)
                    } else {
                        result.success(
                            GestureController(service).clickAtCoordinates(x, y)
                        )
                    }
                }
                
                // Type text
                "typeText" -> {
                    val text = call.argument<String>("text") ?: ""
                    val hint = call.argument<String>("fieldHint")
                    val service = WhatsAppAccessibilityFallbackService.instance
                    if (service == null) {
                        result.error("SERVICE_DOWN", "Accessibility not available", null)
                    } else {
                        result.success(
                            GestureController(service).typeText(text, hint)
                        )
                    }
                }
                
                // Press Enter
                "pressEnter" -> {
                    val service = WhatsAppAccessibilityFallbackService.instance
                    if (service == null) {
                        result.error("SERVICE_DOWN", "Accessibility not available", null)
                    } else {
                        result.success(GestureController(service).pressEnter())
                    }
                }
                
                // Swipe
                "swipe" -> {
                    val startX = call.argument<Double>("startX")?.toFloat() ?: 0f
                    val startY = call.argument<Double>("startY")?.toFloat() ?: 0f
                    val endX = call.argument<Double>("endX")?.toFloat() ?: 0f
                    val endY = call.argument<Double>("endY")?.toFloat() ?: 0f
                    val service = WhatsAppAccessibilityFallbackService.instance
                    if (service == null) {
                        result.error("SERVICE_DOWN", "Accessibility not available", null)
                    } else {
                        result.success(
                            GestureController(service).swipe(startX, startY, endX, endY)
                        )
                    }
                }
                
                "getCurrentPackage" -> {
                    val service = WhatsAppAccessibilityFallbackService.instance
                    result.success(service?.getCurrentPackageName())
                }
                
                else -> result.notImplemented()
            }
        }
    }
}
```

Register in your Activity:

```kotlin
// In MainActivity.kt or your main activity
override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    AccessibilityBridgeHandler.registerWithFlutterEngine(flutterEngine, this)
}
```

---

### Step 2: Create Dart Service Layer

**File**: `lib/services/screen_automation_service.dart`

```dart
import 'package:flutter/services.dart';
import 'dart:async';

/// Dart wrapper around native accessibility service.
/// Clean API for business logic and AI integration.
class ScreenAutomationService {
  static const _channel = MethodChannel('com.example.easylauncher/accessibility');
  static const _timeout = Duration(seconds: 3);
  
  /// Generic invoke with timeout and error handling
  static Future<T?> _invoke<T>(String method, [Map<String, Object?>? args]) async {
    try {
      return await _channel
          .invokeMethod<T>(method, args)
          .timeout(_timeout, onTimeout: () => null);
    } on PlatformException catch (e) {
      print('Accessibility error: ${e.code} - ${e.message}');
      return null;
    } catch (e) {
      print('Accessibility error: $e');
      return null;
    }
  }
  
  /// Dump entire screen as structured list
  Future<List<Map<String, dynamic>>> dumpScreen() async {
    final result = await _invoke<List>('dumpScreen');
    if (result == null) return [];
    return result
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
  }
  
  /// Click at specific coordinates
  Future<bool> clickAt(double x, double y) async {
    return await _invoke<bool>('clickAt', {'x': x, 'y': y}) ?? false;
  }
  
  /// Type text into editable field
  Future<bool> typeText(String text, {String? fieldHint}) async {
    return await _invoke<bool>(
      'typeText',
      {'text': text, 'fieldHint': fieldHint}
    ) ?? false;
  }
  
  /// Press Enter/IME action
  Future<bool> pressEnter() async {
    return await _invoke<bool>('pressEnter') ?? false;
  }
  
  /// Swipe gesture
  Future<bool> swipe(
    double startX,
    double startY,
    double endX,
    double endY,
  ) async {
    return await _invoke<bool>('swipe', {
      'startX': startX,
      'startY': startY,
      'endX': endX,
      'endY': endY,
    }) ?? false;
  }
  
  /// Get current foreground app
  Future<String?> getCurrentPackage() async {
    return await _invoke<String>('getCurrentPackage');
  }
  
  /// Get formatted screen description for AI
  /// (Can be implemented entirely in Dart from dumpScreen() output)
  Future<String> getScreenDescriptionForAI(String task) async {
    final dump = await dumpScreen();
    return _formatForAI(dump, task);
  }
  
  /// Format screen dump as compact text for LLM
  static String _formatForAI(
    List<Map<String, dynamic>> elements,
    String task,
  ) {
    final buffer = StringBuffer();
    buffer.writeln('SCREEN_STATE:');
    
    // Extract keywords from task
    final keywords = task.toLowerCase().split(RegExp(r'[\s\W]+'));
    
    for (final elem in elements) {
      final idx = elem['index'] ?? -1;
      final text = elem['text'] ?? '';
      final desc = elem['contentDescription'] ?? '';
      final clickable = elem['isClickable'] == true;
      final editable = elem['isEditable'] == true;
      
      String label = text.isNotEmpty ? text : desc;
      if (label.isEmpty && !clickable && !editable) continue;
      
      if (label.length > 50) label = '${label.substring(0, 50)}...';
      
      final tags = <String>[];
      if (clickable) tags.add('tap');
      if (editable) tags.add('edit');
      
      // Highlight if matches task
      final isTarget = keywords.any((kw) => label.toLowerCase().contains(kw));
      final mark = isTarget ? '*' : ' ';
      
      buffer.writeln('[$idx]$mark "$label" ${tags.join(",")}');
    }
    
    return buffer.toString();
  }
}
```

---

### Step 3: Use in Business Logic (Gemini Integration)

**File**: `lib/services/gemini_automation_orchestrator.dart`

```dart
import 'screen_automation_service.dart';

/// Orchestrates screen understanding + Gemini Nano + UI automation
class GeminiAutomationOrchestrator {
  final screenAutomation = ScreenAutomationService();
  final geminiBridge = GeminiNanoManager();  // Your existing Gemini integration
  
  /// Execute a high-level task via screen understanding
  Future<bool> executeTask(String instruction) async {
    try {
      // Step 1: Read current screen state
      final screenDump = await screenAutomation.dumpScreen();
      if (screenDump.isEmpty) {
        print('ERROR: Could not read screen (accessibility not available?)');
        return false;
      }
      
      // Step 2: Format for AI
      final screenDescription = 
          ScreenAutomationService._formatForAI(screenDump, instruction);
      
      // Step 3: Get AI decision
      final decision = await geminiBridge.analyzeScreenAndDecide(
        screenDescription,
        instruction,
        screenDump,  // Pass structured data too
      );
      
      // Step 4: Execute action
      return await _executeAction(decision);
    } catch (e) {
      print('Task execution failed: $e');
      return false;
    }
  }
  
  /// Execute a single UI action decided by AI
  Future<bool> _executeAction(AIDecision decision) async {
    switch (decision.type) {
      case 'click':
        return await screenAutomation.clickAt(decision.x, decision.y);
      case 'type':
        return await screenAutomation.typeText(decision.text!);
      case 'enter':
        return await screenAutomation.pressEnter();
      case 'swipe':
        return await screenAutomation.swipe(
          decision.startX!,
          decision.startY!,
          decision.endX!,
          decision.endY!,
        );
      default:
        return false;
    }
  }
}

class AIDecision {
  final String type;  // 'click', 'type', 'enter', 'swipe'
  final double x, y;
  final double? startX, startY, endX, endY;
  final String? text;
  
  AIDecision({
    required this.type,
    this.x = 0,
    this.y = 0,
    this.startX,
    this.startY,
    this.endX,
    this.endY,
    this.text,
  });
}
```

---

## Comparison Table

| Aspect | Easy Launcher (Current) | Easy Launcher + Bridge Pattern |
|--------|------------------------|---------------------------------|
| **Language** | Kotlin only | Kotlin + Dart |
| **UI Automation** | Direct in `AccessibilityService` | Via `ScreenAutomationService` |
| **Testing** | Difficult (needs real AccessibilityService) | Easy (mock MethodChannel) |
| **AI Integration** | Manual node traversal → Gemini | Clean data flow: dumpScreen() → Gemini |
| **State Management** | Scattered in service lifecycle | Centralized in Dart orchestrator |
| **Error Handling** | Try/catch in Kotlin | Type-safe Dart futures |
| **Extensibility** | Add methods to service | Add to Dart API layer |

---

## Migration Path

1. **Week 1**: Create `AccessibilityBridgeHandler.kt`
   - Register MethodChannel in MainActivity
   - Implement basic methods (dumpScreen, clickAt)

2. **Week 2**: Create `ScreenAutomationService.dart`
   - Dart wrapper around MethodChannel
   - Test with mock implementation

3. **Week 3**: Create `GeminiAutomationOrchestrator.dart`
   - Feed screen data to Gemini Nano
   - Execute decisions via ScreenAutomationService

4. **Week 4**: Integration testing
   - Test on real devices
   - Verify Gemini reasoning with visual feedback

---

## Benefits for Easy Launcher

✅ **Testability**: Mock ScreenAutomationService in unit tests  
✅ **Clarity**: Dart code reads like pseudo-code  
✅ **Maintainability**: One service for all accessibility calls  
✅ **AI-Ready**: Screen data naturally serializes to JSON for Gemini  
✅ **Reusability**: Dart layer can be used by different UI frameworks (Compose, native Android, etc.)

This pattern is exactly what makes **private-agent** maintainable across 324 stars and 140 forks.

---

## See Also

- **Source**: `orailnoor/private-agent` → `lib/services/screen_automation_service.dart`
- **Handler**: `android/app/src/main/kotlin/.../MainActivity.kt` (MethodCallHandler)
- **Service**: `android/app/src/main/kotlin/.../AgentAccessibilityService.kt`
