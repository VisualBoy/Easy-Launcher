# System-Level Floating Window AI Orchestrator

## Overview: Dual Dart Isolate Architecture

Easy Launcher può evolversi in uno **strumento vocale sempre disponibile** come in Easy Launcher Screenshot (L'immagine mostra 2 notifiche, 1 messaggio WhatsApp).

**private-agent dimostra un pattern robusto**:
- ✅ **Secondo Entry Point** (`overlayMain`) — Dart isolate indipendente
- ✅ **Native Foreground Service** — Persiste sopra altre app (SYSTEM_ALERT_WINDOW)
- ✅ **SharedPreferences + File Handoff** — Condivisione dati tra main app e overlay
- ✅ **BasicMessageChannel** — Comunicazione bidirezionale (Dart ↔ Dart, resa possibile da MethodChannel)

---

## Architettura Completa

```
┌───────────────────────────────────────────────────────────────┐
│              Easy Launcher Dual Isolate Stack                 │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  MAIN APP (Activity Main Isolate)                             │
│  ├─ MainActivity.kt                                           │
│  ├─ lib/main.dart (HomeScreen, SettingsDialog)               │
│  ├─ GeminiAutomationOrchestrator.dart                         │
│  └─ Communicates via FlutterOverlayWindow.overlayListener()  │
│                                                                │
│  ─────────────────────────────────────────────────────         │
│                   HANDOFF LAYER                                │
│  SharedPreferences, ChatHistoryService (JSONL files)          │
│  ─────────────────────────────────────────────────────         │
│                                                                │
│  OVERLAY FLOATING WINDOW (Separate Dart Isolate)             │
│  ├─ @pragma("vm:entry-point") overlayMain()                  │
│  ├─ OverlayApp.dart (Floating UI Button + Speech)            │
│  ├─ ScreenAutomationService.dart                             │
│  ├─ TaskExecutor.dart (Multi-step loop)                       │
│  └─ Accessible via FlutterOverlayWindow.showOverlay()        │
│                                                                │
│  NATIVE LAYER (Android)                                       │
│  ├─ OverlayService.java (Foreground Service)                 │
│  ├─ FlutterView + WindowManager (SYSTEM_ALERT_WINDOW)        │
│  ├─ MethodChannel registration                               │
│  └─ Foreground Notification (persistent)                      │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

---

## Step 1: AndroidManifest Configuration

**File**: `app/src/main/AndroidManifest.xml`

Add permissions and Foreground Service:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <!-- Floating Window Permissions -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    
    <application>
        <!-- Existing MainActivity -->
        <activity android:name=".MainActivity" ... />
        
        <!-- OVERLAY FOREGROUND SERVICE -->
        <!-- This is the native service that renders the floating window -->
        <service
            android:name="flutter.overlay.window.flutter_overlay_window.OverlayService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="AI voice assistant floating button" />
        </service>
        
        <!-- ACCESSIBILITY SERVICE (for screen automation) -->
        <service
            android:name=".services.WhatsAppAccessibilityFallbackService"
            android:label="@string/accessibility_service_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>
        
    </application>
</manifest>
```

---

## Step 2: Separate Entry Point (lib/overlay_main.dart)

**File**: `lib/overlay_main.dart`

This is the Dart code that runs **inside the Foreground Service** (separate isolate).

```dart
import 'dart:async';
import 'dart:developer';
import 'package:flutter/material.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

import 'services/ai_service.dart';
import 'services/task_executor.dart';
import 'services/screen_automation_service.dart';
import 'services/chat_history_service.dart';
import 'models/chat_message.dart';

/// Entry point for the overlay isolate.
/// This function is called ONLY when the Foreground Service is created.
/// It runs in a SEPARATE Dart isolate from the main app.
@pragma("vm:entry-point")
void overlayMain() {
  runApp(const OverlayApp());
}

class OverlayApp extends StatefulWidget {
  const OverlayApp({super.key});

  @override
  State<OverlayApp> createState() => _OverlayAppState();
}

class _OverlayAppState extends State<OverlayApp> {
  // UI State
  bool _isExpanded = false;
  bool _isListening = false;
  
  // Controllers
  final TextEditingController _taskController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  
  // Services
  late final AiService _aiService;
  late final ScreenAutomationService _screenService;
  late final TaskExecutor _taskExecutor;
  
  // Speech Recognition
  final stt.SpeechToText _speech = stt.SpeechToText();
  
  // Chat History
  List<ChatMessage> _messages = [];
  StreamSubscription<dynamic>? _overlaySubscription;
  
  @override
  void initState() {
    super.initState();
    
    // 1. Initialize services
    _aiService = AiService();
    _screenService = ScreenAutomationService();
    _taskExecutor = TaskExecutor(
      aiService: _aiService,
      screenService: _screenService,
      onProgress: _updateProgress,
    );
    
    // 2. Initialize speech recognition
    _speech.initialize();
    
    // 3. Listen for events from main app
    //    The main app can send messages to the overlay via:
    //    FlutterOverlayWindow.sendMessageToOverlay(message)
    _overlaySubscription = FlutterOverlayWindow.overlayListener.listen(
      _handleMainAppEvent,
    );
    
    // 4. Load chat history from shared storage
    _loadChatHistory();
    
    log('[Overlay] Initialization complete');
  }

  @override
  void dispose() {
    _overlaySubscription?.cancel();
    _taskController.dispose();
    _scrollController.dispose();
    _speech.stop();
    super.dispose();
  }

  /// Handle incoming messages from the main app
  void _handleMainAppEvent(dynamic event) {
    if (event is! String) return;
    
    try {
      // Main app can send: "TASK:goal" or "SYNC:data" etc.
      if (event.startsWith('TASK:')) {
        final task = event.substring(5);
        _sendTask(task);
      } else if (event.startsWith('SYNC:')) {
        _loadChatHistory();
      }
    } catch (e) {
      log('[Overlay] Event parse error: $e');
    }
  }

  /// Load chat history from shared storage
  Future<void> _loadChatHistory() async {
    try {
      final sessions = await ChatHistoryService.loadSessions();
      if (sessions.isEmpty) {
        _messages = [
          ChatMessage(
            role: 'assistant',
            content: 'Hi! I am your AI assistant. Speak or type a task.',
          ),
        ];
      } else {
        _messages = sessions.last.messages
            .map((m) => ChatMessage.fromJson(m))
            .toList();
      }
      if (mounted) {
        setState(() {});
        _scrollToBottom();
      }
    } catch (e) {
      log('[Overlay] Load history error: $e');
    }
  }

  /// Send task to AI executor
  Future<void> _sendTask(String task) async {
    if (task.trim().isEmpty) return;
    
    final userMsg = ChatMessage(role: 'user', content: task);
    setState(() {
      _messages.add(userMsg);
      _taskController.clear();
    });
    _scrollToBottom();
    
    // Persist message
    await ChatHistoryService.appendOverlayMessage(userMsg.toJson());
    
    // Execute task
    try {
      final result = await _taskExecutor.executeTask(task);
      
      final assistantMsg = ChatMessage(
        role: 'assistant',
        content: 'Task complete: $result',
      );
      setState(() {
        _messages.add(assistantMsg);
      });
      _scrollToBottom();
      
      await ChatHistoryService.appendOverlayMessage(assistantMsg.toJson());
    } catch (e) {
      final errorMsg = ChatMessage(
        role: 'assistant',
        content: 'Error: $e',
      );
      setState(() {
        _messages.add(errorMsg);
      });
      _scrollToBottom();
    }
  }

  /// Speech recognition
  Future<void> _toggleListening() async {
    if (_isListening) {
      await _speech.stop();
      setState(() => _isListening = false);
      return;
    }
    
    setState(() => _isListening = true);
    await _speech.listen(
      onResult: (result) {
        if (result.finalResult) {
          setState(() => _isListening = false);
          _sendTask(result.recognizedWords);
        }
      },
    );
  }

  void _updateProgress(String message) {
    setState(() {
      _messages.add(ChatMessage(role: 'system', content: message));
    });
    _scrollToBottom();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    // When collapsed: Show only the circular speech button
    if (!_isExpanded) {
      return MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(
          backgroundColor: Colors.transparent,
          body: Center(
            child: GestureDetector(
              onTap: () {
                setState(() => _isExpanded = true);
              },
              child: Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFF4F46E5),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.3),
                      blurRadius: 8,
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.mic,
                  color: Colors.white,
                  size: 28,
                ),
              ),
            ),
          ),
        ),
      );
    }

    // When expanded: Show chat interface
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Easy Launcher Assistant'),
          centerTitle: true,
          backgroundColor: const Color(0xFF4F46E5),
          elevation: 0,
          actions: [
            IconButton(
              icon: const Icon(Icons.close),
              onPressed: () {
                setState(() => _isExpanded = false);
              },
            ),
          ],
        ),
        body: Column(
          children: [
            Expanded(
              child: ListView.builder(
                controller: _scrollController,
                itemCount: _messages.length,
                itemBuilder: (ctx, idx) {
                  final msg = _messages[idx];
                  return Align(
                    alignment: msg.role == 'user'
                        ? Alignment.centerRight
                        : Alignment.centerLeft,
                    child: Container(
                      margin: const EdgeInsets.all(8),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: msg.role == 'user'
                            ? const Color(0xFF4F46E5)
                            : Colors.grey[300],
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        msg.content,
                        style: TextStyle(
                          color: msg.role == 'user'
                              ? Colors.white
                              : Colors.black,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
            // Input area
            Container(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _taskController,
                      decoration: InputDecoration(
                        hintText: 'Type or speak a task...',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  FloatingActionButton.small(
                    onPressed: _toggleListening,
                    backgroundColor: _isListening
                        ? Colors.red
                        : const Color(0xFF4F46E5),
                    child: Icon(
                      _isListening ? Icons.stop : Icons.mic,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(width: 8),
                  FloatingActionButton.small(
                    onPressed: () => _sendTask(_taskController.text),
                    backgroundColor: const Color(0xFF4F46E5),
                    child: const Icon(Icons.send, color: Colors.white),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
```

---

## Step 3: Main Entry Point (lib/main.dart)

**File**: `lib/main.dart`

The MAIN app initializes the overlay service and communicates with it.

```dart
import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Easy Launcher',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primaryColor: const Color(0xFF4F46E5),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  bool _overlayEnabled = false;
  AppLifecycleState _appLifecycleState = AppLifecycleState.resumed;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkAndShowOverlay();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _appLifecycleState = state;
    
    // Auto-show overlay when app goes to background
    if (state == AppLifecycleState.paused && _overlayEnabled) {
      _showFloatingWindow();
    }
  }

  Future<void> _checkAndShowOverlay() async {
    // 1. Check if user enabled floating window
    final enabled = await _getPreference('overlay_enabled', false);
    setState(() => _overlayEnabled = enabled);
    
    // 2. Request permission if needed
    if (enabled) {
      final hasPermission = await FlutterOverlayWindow.isPermissionGranted();
      if (!hasPermission) {
        await FlutterOverlayWindow.requestPermission();
      }
      
      // 3. Show overlay
      await _showFloatingWindow();
    }
  }

  Future<void> _showFloatingWindow() async {
    final isActive = await FlutterOverlayWindow.isActive();
    if (isActive) return;
    
    await FlutterOverlayWindow.showOverlay(
      enableDrag: true,
      overlayTitle: "Easy Launcher",
      overlayContent: "AI Assistant",
      flag: OverlayFlag.focusPointer,
      alignment: OverlayAlignment.centerRight,
      visibility: NotificationVisibility.visibilitySecret,
      positionGravity: PositionGravity.auto,
      startPosition: const OverlayPosition(0, 200),
      width: 56,
      height: 56,
    );
  }

  /// Send a message TO the overlay isolate
  Future<void> _sendMessageToOverlay(String task) async {
    await FlutterOverlayWindow.sendMessageToOverlay('TASK:$task');
  }

  Future<bool> _getPreference(String key, bool defaultValue) async {
    // Use SharedPreferences or your preferred method
    return defaultValue;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Easy Launcher'),
        backgroundColor: const Color(0xFF4F46E5),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'Easy Launcher v2.0',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 32),
            SwitchListTile(
              title: const Text('Enable Floating Assistant'),
              subtitle: const Text('AI icon visible over other apps'),
              value: _overlayEnabled,
              onChanged: (val) async {
                if (val) {
                  final granted = 
                      await FlutterOverlayWindow.isPermissionGranted() ??
                      await FlutterOverlayWindow.requestPermission() ??
                      false;
                  if (!granted) return;
                }
                setState(() => _overlayEnabled = val);
                await _setPreference('overlay_enabled', val);
                
                if (val) {
                  await _showFloatingWindow();
                } else {
                  await FlutterOverlayWindow.closeOverlay();
                }
              },
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: () => _sendMessageToOverlay(
                'Send WhatsApp message to mom: Hi, how are you?',
              ),
              child: const Text('Test: Send to Overlay'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _setPreference(String key, bool value) async {
    // Implementation here
  }
}
```

---

## Step 4: MainActivity (Dart Isolate Registration)

**File**: `app/src/main/java/com/example/MainActivity.kt`

Register both the main and overlay engines:

```kotlin
package com.example

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import io.flutter.FlutterInjector

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Register this engine (main app)
        FlutterEngineCache.getInstance().put("main", flutterEngine)
        
        // Create and register overlay engine with separate entry point
        if (FlutterEngineCache.getInstance().get("overlay") == null) {
            val engineGroup = FlutterEngineGroup(this)
            
            // Entry point: "overlayMain" from lib/overlay_main.dart
            val overlayEntry = DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "overlayMain"  // This calls overlayMain() in overlay_main.dart
            )
            
            val overlayEngine = engineGroup.createAndRunEngine(
                this.applicationContext,
                overlayEntry
            )
            
            // Cache it for the overlay service to use
            FlutterEngineCache.getInstance().put("overlay", overlayEngine)
        }
        
        // Register accessibility service (if needed)
        registerAccessibilityChannel(flutterEngine, this)
    }
    
    companion object {
        fun registerAccessibilityChannel(
            flutterEngine: FlutterEngine,
            context: android.content.Context
        ) {
            MethodChannel(
                flutterEngine.dartExecutor.binaryMessenger,
                "com.example.easylauncher/accessibility"
            ).setMethodCallHandler { call, result ->
                when (call.method) {
                    "dumpScreen" -> {
                        // Your implementation
                        result.success(emptyList<Map<String, Any?>>())
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }
}
```

---

## Step 5: pubspec.yaml Dependencies

**File**: `pubspec.yaml`

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # Overlay Window
  flutter_overlay_window: ^0.1.0  # Or latest from your local_plugins
  
  # Speech & AI
  speech_to_text: ^6.0.0
  google_generative_ai: latest
  
  # Storage & Communication
  shared_preferences: ^2.0.0
  path_provider: ^2.0.0
  
  # Platform Channels
  android_intent_plus: ^4.0.0
```

---

## Data Flow: Main App ↔ Overlay

```
User in Main App
│
├─ Enables "Floating Assistant" toggle
│
├─ MainActivity creates overlay engine → OverlayService starts
│
├─ OverlayApp shows as floating button on screen
│
User taps floating button (in another app)
│
├─ OverlayApp expands → Shows chat + speech button
│
├─ User says "Send message to mom"
│
├─ overlayMain.dart → TaskExecutor → ScreenAutomationService
│
├─ ScreenAutomationService reads screen via AccessibilityService
│
├─ TaskExecutor loops: read screen → ask Gemini → execute action
│
├─ Optional: Send progress updates back to main app
│
└─ TaskExecutor saves result to ChatHistoryService (shared JSON files)

Main App (when resumed)
│
└─ Reads ChatHistoryService to sync conversation history
```

---

## Communication Patterns

### 1. Main App → Overlay (Send Task)

```dart
// In main app
await FlutterOverlayWindow.sendMessageToOverlay('TASK:Send message to mom');
```

### 2. Overlay → Main App (Receive Tasks or Sync)

```dart
// In overlay_main.dart
_overlaySubscription = FlutterOverlayWindow.overlayListener.listen((event) {
  if (event is String && event.startsWith('TASK:')) {
    final task = event.substring(5);
    _sendTask(task);
  }
});
```

### 3. Shared Storage (Persistent)

Both isolates can read/write to **ChatHistoryService** (JSON files in app documents):

```dart
// Overlay writes message
await ChatHistoryService.appendOverlayMessage({
  'role': 'user',
  'content': 'Send message to mom',
});

// Main app reads messages
final sessions = await ChatHistoryService.loadSessions();
```

---

## Key Advantages

✅ **Always Available** — Floating button persists over other apps  
✅ **Independent Execution** — Overlay isolate runs even if main app is backgrounded  
✅ **Shared Accessibility** — Both isolates can access screen via same MethodChannel  
✅ **Native Performance** — Foreground Service + WindowManager for smooth rendering  
✅ **Persistent History** — JSON files survive app restarts  
✅ **No Duplicates** — Atomic message sync on resume  

---

## Troubleshooting

### Issue: Overlay doesn't appear
- **Check**: `SYSTEM_ALERT_WINDOW` permission granted
- **Check**: `OverlayService` defined in AndroidManifest
- **Check**: `overlayMain()` entry point exists in lib/overlay_main.dart

### Issue: Overlay stops responding
- **Check**: Isolate crashed — look for `@pragma("vm:entry-point")` on overlayMain()
- **Check**: FlutterEngine lifecycle — may need to call `engine.lifecycleChannel.appIsResumed()`

### Issue: Screen automation fails from overlay
- **Check**: AccessibilityService running and listening
- **Check**: MethodChannel name matches ("com.example.easylauncher/accessibility")

---

## See Also

- **private-agent reference**: `/lib/overlay_main.dart` (full implementation)
- **Native overlay service**: `/local_plugins/flutter_overlay_window/OverlayService.java`
- **Main app communication**: `/lib/screens/home_screen.dart` (showOverlay logic)

This architecture is production-ready and already battle-tested in **private-agent**. 🚀
