<<<<<<< HEAD
The `MAIN_IMPLEMENTATION_PLAN_V1.md` bridges Easy Launcher's edge-first Kotlin foundation with **private-agent**'s Dart/Flutter bridge architecture, persistent skill memory, and system-level overlay capabilities.

---

## Unified System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                DART / FLUTTER LAYER                                    │
│                                                                                        │
│  ┌──────────────────────────────────┐        ┌──────────────────────────────────────┐  │
│  │    MAIN ISOLATE (lib/main.dart)  │        │   OVERLAY ISOLATE (overlay_main.dart)│  │
│  │ ├─ GeminiAutomationOrchestrator  │        │ ├─ Floating UI Voice Button          │  │
│  │ ├─ TaskExecutor (Loop Control)   │        │ ├─ Active Speech Ingestion           │  │
│  │ └─ ChatHistoryService            │        │ └─ TaskExecutor / Quick Actions      │  │
│  └─────────────────┬────────────────┘        └──────────────────┬───────────────────┘  │
│                    │                                            │                      │
│                    └──────────────────────┬─────────────────────┘                      │
│                                           │                                            │
│                      MethodChannel("com.example.easylauncher/accessibility")            │
│                      Handoff Layer: JSONL Task Logs & Shared Preferences              │
└───────────────────────────────────────────┼────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────┼────────────────────────────────────────────┐
│                                   KOTLIN NATIVE LAYER                                  │
│                                           │                                            │
│  ┌────────────────────────────────────────▼─────────────────────────────────────────┐  │
│  │                        AccessibilityBridgeHandler.kt                             │  │
│  └───────┬────────────────────────────────┬───────────────────────────────┬─────────┘  │
│          │                                │                               │            │
│          ▼                                ▼                               ▼            │
│ ┌──────────────────┐            ┌───────────────────┐           ┌────────────────────┐ │
│ │ScreenDumpService │            │GestureController  │           │OverlayService.java │ │
│ │(AccessibilityTree│            │(Coordinate Taps,  │           │(SYSTEM_ALERT_      │ │
│ │ & Spatial OCR)   │            │ Swipes, IME Enter)│           │ WINDOW Foreground) │ │
│ └──────────────────┘            └───────────────────┘           └────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘

```

---

## Module Mapping Matrix

| New Module Guide | Primary Native/Dart Components | Target Phase in `implementation_plan.md` | Core Architectural Responsibility |
| --- | --- | --- | --- |
| **`DART_FLUTTER_BRIDGE_PATTERN.md`** | `AccessibilityBridgeHandler.kt`<br>

<br>`ScreenAutomationService.dart` | **Phase 1 & Phase 4** | Async MethodChannel bridge separating UI automation logic from native Android accessibility APIs. |
| **`HISTORY_MEMORY_OPTIMIZATION.md`** | `TaskExecutor.dart`<br>

<br>`SkillMemoryService.dart`<br>

<br>`TaskHistoryLogger.dart` | **Phase 1 & Phase 2** | Resolves Gemini Nano token budgets (~1k tokens) via execution loops, JSONL logging, and skill replays. |
| **`INTEGRATION_PLAN_PRIVATE_AGENT.md`** | `ScreenDumpService.kt`<br>

<br>`GestureController.kt`<br>

<br>`ScreenAnalyzerForAI.kt` | **Phase 4** | Replaces hardcoded view IDs with universal node dumps, coordinate gesture injection, and base64 OCR fallbacks. |
| **`SYSTEM_LEVEL_FLOATING_WINDOW.md`** | `OverlayService.java`<br>

<br>`lib/overlay_main.dart` | **Phase 5** | Renders a persistent system-level floating assistant using dual Dart isolates and `SYSTEM_ALERT_WINDOW`. |

---

## Expanded Phase Integration Plan

### Phase 1: Toolchain, Bridge Architecture & Skill Caching

* **MethodChannel Registration**: Add `AccessibilityBridgeHandler.kt` to `MainActivity.kt` to establish channel `com.example.easylauncher/accessibility`.
* **Dart API Layer**: Implement `ScreenAutomationService.dart` to expose typed async methods (`dumpScreen`, `clickAt`, `typeText`, `swipe`, `pressEnter`).
* **Skill Caching Integration**: Wire `SkillMemoryService.dart` into `TaskExecutor.dart`. Before querying Gemini Nano or LiteRT, check local `skill_memory.json` for matching execution chains. If a matching reliable skill exists (>80% success rate), execute it immediately with 0 token consumption.
* **Structured Output**: Align `@Generable` models in Kotlin with Dart `AIDecision` models for typed action parsing.

### Phase 2: LiteRT-LM Fallback & Context-Aware Execution Engine

* **Context-Aware Prompts**: Integrate `TaskHistoryLogger.dart` (JSONL storage) into the `HybridOrchestrator` prompt builder. Pass the last 3-5 successful action patterns alongside compressed screen dumps to lower TTFT (<100ms).
* **Adaptive Loop Delays**: Implement dynamic waiting within `TaskExecutor` based on action types (e.g., 3000ms for app cold-starts, 2000ms for text entry, 1000ms for scrolls).
* **Recovery Engine**: Attach `RecoveryEngine.dart` to handle stuck loops. When 3 consecutive action failures occur, force navigation resets (`press_home` or `press_back`) based on historical success data.

### Phase 3: Speech Recognition & Chat Context Handoff

* **Offline Speech Ingestion**: Pipe offline ML Kit Speech Recognition transcripts directly into `ChatHistoryService.dart`.
* **Cross-Isolate Context**: Ensure conversation sessions saved by `ChatHistoryService` are accessible by both `main.dart` and `overlay_main.dart` via shared local JSON files.

### Phase 4: Universal Screen Parsing & Coordinate Gesture Engine

* **Node Tree Extraction**: Implement `ScreenDumpService.kt` to traverse active `AccessibilityNodeInfo` trees, stripping zero-size/invisible nodes and outputting structured bounds `[left, top, right, bottom, centerX, centerY]`.
* **Coordinate Gestures**: Replace hardcoded `ViewId` lookups in `WhatsAppAccessibilityFallbackService.kt` with `GestureController.kt`. Use `Path`-based `dispatchGesture()` for precise coordinate taps, swipes, and IME `ACTION_IME_ENTER` dispatching.
* **AI Analysis Serialization**: Transform screen dumps using `ScreenAnalyzerForAI.kt` into compact JSON streams. If accessibility node traversal returns empty, fallback to screenshot capture with ML Kit Spatial OCR.

### Phase 5: System-Level Floating Window & Notification System

* **Dual Isolate Setup**: Declare `@pragma("vm:entry-point") void overlayMain()` in `lib/overlay_main.dart` as a standalone Dart isolate.
* **Foreground Overlay Service**: Register `flutter.overlay.window.flutter_overlay_window.OverlayService` in `AndroidManifest.xml` under `SYSTEM_ALERT_WINDOW` permissions.
* **Notification Action Pipeline**: Combine `NotificationListenerService` with `OverlayApp`. Trigger `RemoteInput` inline replies or expand the floating voice widget upon receiving high-priority notifications.

### Phase 6 & 7: Emergency SOS Hardening & Adaptive UI Layouts

* **Bypass Execution**: Keep `SosEmergencyForegroundService.kt` strictly native to guarantee zero dependency on Flutter engine initialization during critical emergency calls.
* **Edge-to-Edge Padding**: Apply window insets to both the main Compose UI and the Flutter floating overlay view to prevent navigation bar overlap.

---

## Token & Performance Impact Metrics

| Metric | Legacy Plan | Integrated Architecture Plan | Improvement |
| --- | --- | --- | --- |
| **Token Budget / Task** | ~1,500 tokens (full prompt every step) | **0–500 tokens** (via `SkillMemoryService`) | **67% – 100% reduction** |
| **Screen Parsing Latency** | ~250ms (OCR-first or brittle ID search) | **<50ms** (`ScreenDumpService` tree traversal) | **5x faster execution** |
| **Gesture Success Rate** | ~65% (dependent on hardcoded view IDs) | **>95%** (`GestureController` spatial coordinates) | **30% higher reliability** |
=======
The `MAIN_IMPLEMENTATION_PLAN_V1.md` bridges Easy Launcher's edge-first Kotlin foundation with **private-agent**'s Dart/Flutter bridge architecture, persistent skill memory, and system-level overlay capabilities.

---

## Unified System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                DART / FLUTTER LAYER                                    │
│                                                                                        │
│  ┌──────────────────────────────────┐        ┌──────────────────────────────────────┐  │
│  │    MAIN ISOLATE (lib/main.dart)  │        │   OVERLAY ISOLATE (overlay_main.dart)│  │
│  │ ├─ GeminiAutomationOrchestrator  │        │ ├─ Floating UI Voice Button          │  │
│  │ ├─ TaskExecutor (Loop Control)   │        │ ├─ Active Speech Ingestion           │  │
│  │ └─ ChatHistoryService            │        │ └─ TaskExecutor / Quick Actions      │  │
│  └─────────────────┬────────────────┘        └──────────────────┬───────────────────┘  │
│                    │                                            │                      │
│                    └──────────────────────┬─────────────────────┘                      │
│                                           │                                            │
│                      MethodChannel("com.example.easylauncher/accessibility")            │
│                      Handoff Layer: JSONL Task Logs & Shared Preferences              │
└───────────────────────────────────────────┼────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────┼────────────────────────────────────────────┐
│                                   KOTLIN NATIVE LAYER                                  │
│                                           │                                            │
│  ┌────────────────────────────────────────▼─────────────────────────────────────────┐  │
│  │                        AccessibilityBridgeHandler.kt                             │  │
│  └───────┬────────────────────────────────┬───────────────────────────────┬─────────┘  │
│          │                                │                               │            │
│          ▼                                ▼                               ▼            │
│ ┌──────────────────┐            ┌───────────────────┐           ┌────────────────────┐ │
│ │ScreenDumpService │            │GestureController  │           │OverlayService.java │ │
│ │(AccessibilityTree│            │(Coordinate Taps,  │           │(SYSTEM_ALERT_      │ │
│ │ & Spatial OCR)   │            │ Swipes, IME Enter)│           │ WINDOW Foreground) │ │
│ └──────────────────┘            └───────────────────┘           └────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘

```

---

## Module Mapping Matrix

| New Module Guide | Primary Native/Dart Components | Target Phase in `implementation_plan.md` | Core Architectural Responsibility |
| --- | --- | --- | --- |
| **`DART_FLUTTER_BRIDGE_PATTERN.md`** | `AccessibilityBridgeHandler.kt`<br>

<br>`ScreenAutomationService.dart` | **Phase 1 & Phase 4** | Async MethodChannel bridge separating UI automation logic from native Android accessibility APIs. |
| **`HISTORY_MEMORY_OPTIMIZATION.md`** | `TaskExecutor.dart`<br>

<br>`SkillMemoryService.dart`<br>

<br>`TaskHistoryLogger.dart` | **Phase 1 & Phase 2** | Resolves Gemini Nano token budgets (~1k tokens) via execution loops, JSONL logging, and skill replays. |
| **`INTEGRATION_PLAN_PRIVATE_AGENT.md`** | `ScreenDumpService.kt`<br>

<br>`GestureController.kt`<br>

<br>`ScreenAnalyzerForAI.kt` | **Phase 4** | Replaces hardcoded view IDs with universal node dumps, coordinate gesture injection, and base64 OCR fallbacks. |
| **`SYSTEM_LEVEL_FLOATING_WINDOW.md`** | `OverlayService.java`<br>

<br>`lib/overlay_main.dart` | **Phase 5** | Renders a persistent system-level floating assistant using dual Dart isolates and `SYSTEM_ALERT_WINDOW`. |

---

## Expanded Phase Integration Plan

### Phase 1: Toolchain, Bridge Architecture & Skill Caching

* **MethodChannel Registration**: Add `AccessibilityBridgeHandler.kt` to `MainActivity.kt` to establish channel `com.example.easylauncher/accessibility`.
* **Dart API Layer**: Implement `ScreenAutomationService.dart` to expose typed async methods (`dumpScreen`, `clickAt`, `typeText`, `swipe`, `pressEnter`).
* **Skill Caching Integration**: Wire `SkillMemoryService.dart` into `TaskExecutor.dart`. Before querying Gemini Nano or LiteRT, check local `skill_memory.json` for matching execution chains. If a matching reliable skill exists (>80% success rate), execute it immediately with 0 token consumption.
* **Structured Output**: Align `@Generable` models in Kotlin with Dart `AIDecision` models for typed action parsing.

### Phase 2: LiteRT-LM Fallback & Context-Aware Execution Engine

* **Context-Aware Prompts**: Integrate `TaskHistoryLogger.dart` (JSONL storage) into the `HybridOrchestrator` prompt builder. Pass the last 3-5 successful action patterns alongside compressed screen dumps to lower TTFT (<100ms).
* **Adaptive Loop Delays**: Implement dynamic waiting within `TaskExecutor` based on action types (e.g., 3000ms for app cold-starts, 2000ms for text entry, 1000ms for scrolls).
* **Recovery Engine**: Attach `RecoveryEngine.dart` to handle stuck loops. When 3 consecutive action failures occur, force navigation resets (`press_home` or `press_back`) based on historical success data.

### Phase 3: Speech Recognition & Chat Context Handoff

* **Offline Speech Ingestion**: Pipe offline ML Kit Speech Recognition transcripts directly into `ChatHistoryService.dart`.
* **Cross-Isolate Context**: Ensure conversation sessions saved by `ChatHistoryService` are accessible by both `main.dart` and `overlay_main.dart` via shared local JSON files.

### Phase 4: Universal Screen Parsing & Coordinate Gesture Engine

* **Node Tree Extraction**: Implement `ScreenDumpService.kt` to traverse active `AccessibilityNodeInfo` trees, stripping zero-size/invisible nodes and outputting structured bounds `[left, top, right, bottom, centerX, centerY]`.
* **Coordinate Gestures**: Replace hardcoded `ViewId` lookups in `WhatsAppAccessibilityFallbackService.kt` with `GestureController.kt`. Use `Path`-based `dispatchGesture()` for precise coordinate taps, swipes, and IME `ACTION_IME_ENTER` dispatching.
* **AI Analysis Serialization**: Transform screen dumps using `ScreenAnalyzerForAI.kt` into compact JSON streams. If accessibility node traversal returns empty, fallback to screenshot capture with ML Kit Spatial OCR.

### Phase 5: System-Level Floating Window & Notification System

* **Dual Isolate Setup**: Declare `@pragma("vm:entry-point") void overlayMain()` in `lib/overlay_main.dart` as a standalone Dart isolate.
* **Foreground Overlay Service**: Register `flutter.overlay.window.flutter_overlay_window.OverlayService` in `AndroidManifest.xml` under `SYSTEM_ALERT_WINDOW` permissions.
* **Notification Action Pipeline**: Combine `NotificationListenerService` with `OverlayApp`. Trigger `RemoteInput` inline replies or expand the floating voice widget upon receiving high-priority notifications.

### Phase 6 & 7: Emergency SOS Hardening & Adaptive UI Layouts

* **Bypass Execution**: Keep `SosEmergencyForegroundService.kt` strictly native to guarantee zero dependency on Flutter engine initialization during critical emergency calls.
* **Edge-to-Edge Padding**: Apply window insets to both the main Compose UI and the Flutter floating overlay view to prevent navigation bar overlap.

---

## Token & Performance Impact Metrics

| Metric | Legacy Plan | Integrated Architecture Plan | Improvement |
| --- | --- | --- | --- |
| **Token Budget / Task** | ~1,500 tokens (full prompt every step) | **0–500 tokens** (via `SkillMemoryService`) | **67% – 100% reduction** |
| **Screen Parsing Latency** | ~250ms (OCR-first or brittle ID search) | **<50ms** (`ScreenDumpService` tree traversal) | **5x faster execution** |
| **Gesture Success Rate** | ~65% (dependent on hardcoded view IDs) | **>95%** (`GestureController` spatial coordinates) | **30% higher reliability** |
>>>>>>> 4a60f5643c5242720b38fe324c069f9aa81c7537
| **UI Availability** | Application-bounded (Main Activity) | **Always Available** (`overlayMain` floating isolate) | **System-wide persistence** |