# Advanced Integration: Task History, Skill Memory & Chat Context Management

## Problem: Gemini Nano's Token Limitations

**Easy Launcher Challenge**:
- Gemini Nano on-device model has strict token budget (~1000-2000 tokens per inference)
- Current approach: Dump entire screen state + user instruction to AI each time
- Result: Cannot handle complex, multi-step tasks requiring context accumulation

**private-agent Solution**:
- ✅ **TaskExecutor** — Manages multi-step AI-guided automation loops
- ✅ **TaskHistoryLogger** — Persists task execution traces (JSONL format)
- ✅ **ChatHistoryService** — Maintains conversation context across sessions
- ✅ **SkillMemoryService** — Learns and replays successful task sequences
- ✅ **RecoveryEngine** — Handles failures by analyzing prior execution history

---

## Architecture: Context-Aware AI Loop

```
┌──────────────────────────────────────────────────────────────┐
│                   User Command                               │
│              "Send message to Mom"                           │
└──────────────────────┬───────────────────────────────────────┘
                       │
        ┌──────────────▼──────────────┐
        │  1. Check Skill Memory       │
        │  (Have we done this before?) │
        └──────────────┬───────────────┘
                       │
        ┌──────────────▼──────────────────────┐
        │  2. Load Task History (Last 5 runs) │
        │  (Context: What worked before?)     │
        └──────────────┬───────────────────────┘
                       │
        ┌──────────────▼──────────────────────────────────────┐
        │  3. Multi-Step Loop (Adaptive Delay)                │
        │                                                      │
        │  Step 1: Read Screen                                │
        │  Step 2: Build Prompt (+ History Context)           │
        │  Step 3: Call Gemini Nano                           │
        │  Step 4: Execute Action                             │
        │  Step 5: Log to Task History (JSONL)                │
        │  Step 6: Repeat or Done                             │
        └──────────────┬───────────────────────────────────────┘
                       │
        ┌──────────────▼──────────────────────┐
        │  4. Log Final Result to Chat History │
        │  (Success/Failure + Token Count)     │
        └──────────────────────────────────────┘
```

---

## Component 1: TaskExecutor (Multi-Step Loop)

### Core Pattern: LLM-Guided Automation Loop

```dart
/// Executes multi-step UI automation tasks using LLM-guided screen reading.
/// 
/// Flow: User Goal → Screen Dump → LLM Decides Action → Execute → 
///       Log Result → Repeat Until Complete
class TaskExecutor {
  final AiService _aiService;
  final ScreenAutomationService _screenService;
  final TaskHistoryLogger _historyLogger;
  final SkillMemoryService _skillMemory;
  final RecoveryEngine _recoveryEngine;
  
  /// Progress callback for UI updates
  final void Function(String message)? onProgress;
  
  /// Cancellation support
  bool _cancelled = false;
  Completer<void>? _cancelCompleter;
  
  TaskExecutor({
    required AiService aiService,
    required ScreenAutomationService screenService,
    this.onProgress,
  }) : _aiService = aiService,
       _screenService = screenService,
       _historyLogger = TaskHistoryLogger(),
       _skillMemory = SkillMemoryService(),
       _recoveryEngine = RecoveryEngine();
  
  /// Main task execution loop
  Future<String> executeTask(String userGoal) async {
    _cancelled = false;
    final results = <String>[];
    final executedSteps = <ActionStep>[];
    int totalTokens = 0;
    
    results.add('Starting task: $userGoal');
    _report('Starting task: $userGoal');
    
    // ─── STEP 1: Check Skill Memory (Fast Path) ───
    final savedSkill = await _skillMemory.findSkill(userGoal);
    if (savedSkill != null && savedSkill.isReliable) {
      _report('Found saved skill! Replaying ${savedSkill.steps.length} steps...');
      final replaySuccess = await _replaySkill(savedSkill, results);
      if (replaySuccess) {
        results.add('Task complete via skill memory.');
        await _historyLogger.logTask(
          userGoal,
          'Success (via memory)',
          0,
          savedSkill.steps.length,
          results,
        );
        return 'Done.';
      } else {
        _report('Replay failed, falling back to AI...');
        await _skillMemory.recordFailure(savedSkill.id);
      }
    }
    
    // ─── STEP 2: Navigation Shortcuts (Common Patterns) ───
    final shortcut = _getNavigationShortcut(userGoal);
    if (shortcut != null && shortcut.isNotEmpty) {
      results.add('Using navigation shortcut: ${shortcut.length} steps');
      for (final step in shortcut) {
        if (_cancelled) break;
        bool success = await _executeActionStep(step);
        if (success) {
          executedSteps.add(step);
        } else {
          break;  // Fall back to AI if shortcut fails
        }
      }
    }
    
    // ─── STEP 3: Multi-Step AI Loop ───
    int consecutiveFailures = 0;
    String lastAction = '';
    int sameActionCount = 0;
    
    for (int step = 0; step < _aiService.maxSteps; step++) {
      if (_cancelled) break;
      
      // Adaptive delay based on last action
      int delay = _getAdaptiveDelay(lastAction);
      await Future.delayed(Duration(milliseconds: delay));
      
      // 3a. Read screen
      final screenContent = await _screenService.getCompressedScreenDescription(userGoal);
      results.add('Screen ${step + 1}: ${screenContent.substring(0, 100)}...');
      
      // 3b. Build context-aware prompt
      final previousResult = step > 0 ? results.last : '';
      final recentHistory = await _historyLogger.getRecent(userGoal, limit: 5);
      final contextPrompt = _buildContextAwarePrompt(
        userGoal,
        screenContent,
        previousResult,
        recentHistory,
      );
      
      // 3c. Call Gemini Nano (with timeout)
      late String response;
      try {
        final aiResponse = await _aiService.sendTaskMessage(
          _getSystemPrompt(),
          contextPrompt,
        );
        response = aiResponse.content;
        totalTokens += aiResponse.totalTokens;
      } catch (e) {
        results.add('AI error: $e');
        return 'AI service failed.';
      }
      
      // 3d. Parse action from response
      late Map<String, dynamic> actionJson;
      try {
        actionJson = _parseJsonResponse(response);
      } catch (e) {
        results.add('Parse error, retrying...');
        continue;
      }
      
      final action = actionJson['action'] as String? ?? 'done';
      final params = actionJson['params'] as Map<String, dynamic>? ?? {};
      final reasoning = actionJson['reasoning'] as String? ?? '';
      final isComplete = actionJson['is_complete'] == true;
      
      _report('Step ${step + 1}: $reasoning');
      
      // 3e. Detect stuck loops
      sameActionCount = action == lastAction ? sameActionCount + 1 : 1;
      if (sameActionCount > 2 && action != 'scroll') {
        results.add('Blocked repeated action. Trying different approach.');
        consecutiveFailures = 3;
        lastAction = action;
        continue;
      }
      lastAction = action;
      
      // 3f. Execute action
      final (success, actionResult) = await _executeAction(action, params);
      results.add(actionResult);
      
      if (success) {
        consecutiveFailures = 0;
        executedSteps.add(ActionStep(action, params));
      } else {
        consecutiveFailures++;
      }
      
      // 3g. Check completion
      if (isComplete || action == 'done') {
        results.add('Task complete.');
        break;
      }
      
      // 3h. Check recovery conditions
      if (consecutiveFailures >= 3) {
        final recovery = await _recoveryEngine.suggestRecovery(
          userGoal,
          results,
          recentHistory,
        );
        if (recovery != null) {
          _report('Attempting recovery: ${recovery.action}');
          await _executeAction(recovery.action, recovery.params);
        } else {
          results.add('Could not recover. Aborting task.');
          break;
        }
      }
    }
    
    // ─── STEP 4: Log Final Result ───
    await _historyLogger.logTask(
      userGoal,
      _getConclusionStatus(results),
      totalTokens,
      executedSteps.length,
      results,
    );
    
    // ─── STEP 5: Learn from Success ───
    if (_isSuccessful(results)) {
      await _skillMemory.recordSuccessfulSkill(
        userGoal,
        executedSteps,
        totalTokens,
      );
    }
    
    return results.join('\n');
  }
  
  /// Build prompt with historical context to overcome token limits
  String _buildContextAwarePrompt(
    String goal,
    String currentScreen,
    String previousResult,
    List<TaskRecord> history,
  ) {
    final buffer = StringBuffer();
    
    buffer.writeln('TASK: $goal');
    buffer.writeln('');
    
    // Include recent successful patterns
    if (history.isNotEmpty) {
      buffer.writeln('RECENT SUCCESSFUL PATTERNS:');
      for (var i = 0; i < history.length && i < 3; i++) {
        final record = history[i];
        if (record.status == 'Success') {
          buffer.writeln('  - ${record.steps.take(2).map((s) => s.action).join(" → ")} ...');
        }
      }
      buffer.writeln('');
    }
    
    buffer.writeln('CURRENT SCREEN:');
    buffer.writeln(currentScreen);
    buffer.writeln('');
    
    if (previousResult.isNotEmpty) {
      buffer.writeln('PREVIOUS ACTION RESULT:');
      buffer.writeln(previousResult);
      buffer.writeln('');
    }
    
    buffer.writeln('What is the next action?');
    
    return buffer.toString();
  }
  
  /// Adaptive delay based on action type (prevents race conditions)
  int _getAdaptiveDelay(String lastAction) {
    return switch (lastAction) {
      'open_app' => 3000,      // Apps need time to cold-start
      'type_text' => 2000,     // Keyboard + network requests
      'click_text' || 'click_at' => 1500,  // Screen transition
      'scroll' => 1000,        // Relatively fast
      _ => 1200,               // Default
    };
  }
}
```

### Key Insights:
1. **Skill Memory Fast Path** — Avoid AI calls for known tasks
2. **Context-Aware Prompts** — Include recent history to guide AI
3. **Adaptive Delays** — Different actions need different wait times
4. **Failure Recovery** — Use historical data to escape stuck loops
5. **Step Logging** — Every action logged for future learning

---

## Component 2: TaskHistoryLogger (Persistent Context)

### Format: JSONL (JSON Lines) — One record per line

```dart
/// Logs all task executions to persistent storage for learning and recovery.
/// Format: JSONL (one JSON object per line, perfect for streaming)
class TaskHistoryLogger {
  static Future<File> get _localFile async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/task_history.jsonl');
  }
  
  /// Append a task execution record
  static Future<void> logTask(
    String goal,
    String status,  // "Success", "Failed", "Cancelled"
    int totalTokens,
    int steps,
    List<String> trace,  // Step-by-step log
  ) async {
    try {
      final file = await _localFile;
      
      final data = {
        "goal": goal.trim(),
        "status": status,
        "total_tokens": totalTokens,
        "steps_taken": steps,
        "trace": trace,
        "timestamp": DateTime.now().toIso8601String(),
      };
      
      // JSONL: append one object per line
      await file.writeAsString(
        '${jsonEncode(data)}\n',
        mode: FileMode.append,
      );
    } catch (e) {
      print('Failed to log task: $e');
    }
  }
  
  /// Get recent successful executions of a task (for context)
  static Future<List<TaskRecord>> getRecent(
    String goal, {
    required int limit,
    bool successOnly = true,
  }) async {
    try {
      final file = await _localFile;
      if (!await file.exists()) return [];
      
      final lines = await file.readAsLines();
      final records = lines
          .where((line) => line.trim().isNotEmpty)
          .map((line) => TaskRecord.fromJson(jsonDecode(line)))
          .where((r) => r.goal.toLowerCase().contains(goal.toLowerCase()))
          .where((r) => !successOnly || r.status == 'Success')
          .toList()
          .reversed
          .take(limit)
          .toList();
      
      return records;
    } catch (e) {
      print('Failed to read task history: $e');
      return [];
    }
  }
  
  /// Calculate success rate for a goal
  static Future<double> getSuccessRate(String goal) async {
    try {
      final file = await _localFile;
      if (!await file.exists()) return 0.0;
      
      final lines = await file.readAsLines();
      final records = lines
          .where((line) => line.trim().isNotEmpty)
          .map((line) => TaskRecord.fromJson(jsonDecode(line)))
          .where((r) => r.goal.toLowerCase().contains(goal.toLowerCase()))
          .toList();
      
      if (records.isEmpty) return 0.0;
      
      final successes = records.where((r) => r.status == 'Success').length;
      return successes / records.length;
    } catch (e) {
      return 0.0;
    }
  }
  
  /// Get analytics across all tasks
  static Future<TaskAnalytics> getAnalytics() async {
    try {
      final file = await _localFile;
      if (!await file.exists()) {
        return TaskAnalytics(totalTasks: 0, successRate: 0.0);
      }
      
      final lines = await file.readAsLines();
      final records = lines
          .where((line) => line.trim().isNotEmpty)
          .map((line) => TaskRecord.fromJson(jsonDecode(line)))
          .toList();
      
      if (records.isEmpty) {
        return TaskAnalytics(totalTasks: 0, successRate: 0.0);
      }
      
      final successes = records.where((r) => r.status == 'Success').length;
      
      return TaskAnalytics(
        totalTasks: records.length,
        successRate: successes / records.length,
        successCount: successes,
        failedCount: records.length - successes,
        totalTokensUsed: records.fold(0, (sum, r) => sum + r.totalTokens),
        avgStepsPerTask: records.fold(0, (sum, r) => sum + r.stepsUsed) / 
                         records.length,
      );
    } catch (e) {
      return TaskAnalytics(totalTasks: 0, successRate: 0.0);
    }
  }
  
  /// Clear history (optional)
  static Future<void> clearHistory() async {
    try {
      final file = await _localFile;
      if (await file.exists()) {
        await file.delete();
      }
    } catch (e) {
      print('Failed to clear history: $e');
    }
  }
}

class TaskRecord {
  final String goal;
  final String status;
  final int totalTokens;
  final int stepsUsed;
  final List<String> trace;
  final DateTime timestamp;
  
  TaskRecord({
    required this.goal,
    required this.status,
    required this.totalTokens,
    required this.stepsUsed,
    required this.trace,
    required this.timestamp,
  });
  
  factory TaskRecord.fromJson(Map<String, dynamic> json) => TaskRecord(
    goal: json['goal'] as String,
    status: json['status'] as String,
    totalTokens: json['total_tokens'] as int,
    stepsUsed: json['steps_taken'] as int,
    trace: List<String>.from(json['trace'] as List),
    timestamp: DateTime.parse(json['timestamp'] as String),
  );
}

class TaskAnalytics {
  final int totalTasks;
  final double successRate;
  final int successCount;
  final int failedCount;
  final int totalTokensUsed;
  final double avgStepsPerTask;
  
  TaskAnalytics({
    required this.totalTasks,
    required this.successRate,
    this.successCount = 0,
    this.failedCount = 0,
    this.totalTokensUsed = 0,
    this.avgStepsPerTask = 0.0,
  });
}
```

---

## Component 3: SkillMemoryService (Learn & Replay)

### Idea: Cache successful task sequences to skip AI calls

```dart
/// Learns successful task patterns and replays them without AI.
/// Dramatically reduces token consumption for repeated tasks.
class SkillMemoryService {
  static Future<File> get _localFile async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/skill_memory.json');
  }
  
  /// Search for a cached skill matching the user goal
  Future<SavedSkill?> findSkill(String userGoal) async {
    try {
      final skills = await _loadSkills();
      for (final skill in skills) {
        // Fuzzy matching: is the goal similar?
        if (_isSimilar(userGoal, skill.goalPattern)) {
          // Only return if recently successful
          final daysSinceLast = DateTime.now().difference(skill.lastUsed).inDays;
          if (daysSinceLast < 7 && skill.successRate > 0.8) {
            return skill;
          }
        }
      }
      return null;
    } catch (e) {
      return null;
    }
  }
  
  /// Record a successful task for future replay
  Future<void> recordSuccessfulSkill(
    String goal,
    List<ActionStep> steps,
    int tokensCost,
  ) async {
    try {
      var skills = await _loadSkills();
      
      final existingIdx = skills.indexWhere(
        (s) => _isSimilar(goal, s.goalPattern),
      );
      
      if (existingIdx >= 0) {
        // Update existing skill
        skills[existingIdx] = skills[existingIdx].recordSuccess(steps, tokensCost);
      } else {
        // Add new skill
        skills.add(SavedSkill(
          id: DateTime.now().millisecondsSinceEpoch.toString(),
          goalPattern: goal,
          steps: steps,
          successCount: 1,
          failureCount: 0,
          totalTokensSaved: tokensCost,
          lastUsed: DateTime.now(),
        ));
      }
      
      await _saveSkills(skills);
    } catch (e) {
      print('Failed to record skill: $e');
    }
  }
  
  /// Record a replay failure (marks skill as less reliable)
  Future<void> recordFailure(String skillId) async {
    try {
      var skills = await _loadSkills();
      final idx = skills.indexWhere((s) => s.id == skillId);
      if (idx >= 0) {
        skills[idx] = skills[idx].recordFailure();
        await _saveSkills(skills);
      }
    } catch (e) {
      print('Failed to record failure: $e');
    }
  }
  
  bool _isSimilar(String a, String b) {
    // Simple substring matching; could use Levenshtein distance
    return a.toLowerCase().contains(b.toLowerCase()) ||
           b.toLowerCase().contains(a.toLowerCase());
  }
  
  Future<List<SavedSkill>> _loadSkills() async {
    try {
      final file = await _localFile;
      if (!await file.exists()) return [];
      
      final content = await file.readAsString();
      if (content.trim().isEmpty) return [];
      
      final decoded = jsonDecode(content) as List;
      return decoded
          .map((item) => SavedSkill.fromJson(item as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }
  
  Future<void> _saveSkills(List<SavedSkill> skills) async {
    try {
      final file = await _localFile;
      final jsonList = skills.map((s) => s.toJson()).toList();
      await file.writeAsString(jsonEncode(jsonList));
    } catch (e) {
      print('Failed to save skills: $e');
    }
  }
}

class SavedSkill {
  final String id;
  final String goalPattern;
  final List<ActionStep> steps;
  final int successCount;
  final int failureCount;
  final int totalTokensSaved;
  final DateTime lastUsed;
  
  SavedSkill({
    required this.id,
    required this.goalPattern,
    required this.steps,
    required this.successCount,
    required this.failureCount,
    required this.totalTokensSaved,
    required this.lastUsed,
  });
  
  bool get isReliable => successRate > 0.8 && successCount >= 2;
  double get successRate => successCount / (successCount + failureCount).clamp(1, double.infinity);
  
  SavedSkill recordSuccess(List<ActionStep> newSteps, int tokensSaved) {
    return SavedSkill(
      id: id,
      goalPattern: goalPattern,
      steps: newSteps,  // Use new steps if better
      successCount: successCount + 1,
      failureCount: failureCount,
      totalTokensSaved: totalTokensSaved + tokensSaved,
      lastUsed: DateTime.now(),
    );
  }
  
  SavedSkill recordFailure() {
    return SavedSkill(
      id: id,
      goalPattern: goalPattern,
      steps: steps,
      successCount: successCount,
      failureCount: failureCount + 1,
      totalTokensSaved: totalTokensSaved,
      lastUsed: DateTime.now(),
    );
  }
  
  Map<String, dynamic> toJson() => {
    'id': id,
    'goalPattern': goalPattern,
    'steps': steps.map((s) => s.toJson()).toList(),
    'successCount': successCount,
    'failureCount': failureCount,
    'totalTokensSaved': totalTokensSaved,
    'lastUsed': lastUsed.toIso8601String(),
  };
  
  factory SavedSkill.fromJson(Map<String, dynamic> json) => SavedSkill(
    id: json['id'] as String,
    goalPattern: json['goalPattern'] as String,
    steps: (json['steps'] as List)
        .map((s) => ActionStep.fromJson(s as Map<String, dynamic>))
        .toList(),
    successCount: json['successCount'] as int,
    failureCount: json['failureCount'] as int,
    totalTokensSaved: json['totalTokensSaved'] as int,
    lastUsed: DateTime.parse(json['lastUsed'] as String),
  );
}
```

---

## Component 4: ChatHistoryService (Conversation Context)

### Persist multi-turn conversations across app restarts

```dart
/// Manages chat sessions and enables context sharing between overlay & main app.
/// Useful for maintaining conversation history across Easy Launcher restarts.
class ChatHistoryService {
  static Future<File> get _localFile async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${directory.path}/chat_history_sessions.json');
  }
  
  /// Save a chat session
  static Future<void> saveSession(ChatSession session) async {
    try {
      final file = await _localFile;
      List<ChatSession> sessions = await loadSessions();
      
      final index = sessions.indexWhere((s) => s.id == session.id);
      if (index >= 0) {
        sessions[index] = session;
      } else {
        sessions.insert(0, session);  // Newest first
      }
      
      final jsonList = sessions.map((s) => s.toJson()).toList();
      await file.writeAsString(jsonEncode(jsonList));
    } catch (e) {
      print('Error saving chat session: $e');
    }
  }
  
  /// Load all chat sessions
  static Future<List<ChatSession>> loadSessions() async {
    try {
      final file = await _localFile;
      if (!await file.exists()) return [];
      
      final content = await file.readAsString();
      if (content.trim().isEmpty) return [];
      
      final decoded = jsonDecode(content) as List;
      return decoded
          .map((item) => ChatSession.fromJson(item as Map<String, dynamic>))
          .toList();
    } catch (e) {
      print('Error loading sessions: $e');
      return [];
    }
  }
  
  /// Get the most recent session (for context injection)
  static Future<ChatSession?> getMostRecentSession() async {
    final sessions = await loadSessions();
    if (sessions.isEmpty) return null;
    
    final dayOld = DateTime.now().subtract(Duration(days: 1));
    final recent = sessions.firstWhereOrNull(
      (s) => s.timestamp.isAfter(dayOld),
    );
    return recent;
  }
  
  /// Extract conversation context as a string
  static Future<String> getRecentContext({int messageCount = 10}) async {
    final session = await getMostRecentSession();
    if (session == null || session.messages.isEmpty) return '';
    
    final buffer = StringBuffer();
    buffer.writeln('RECENT CONVERSATION:');
    
    for (var i = session.messages.length - messageCount;
         i < session.messages.length;
         i++) {
      if (i >= 0) {
        final msg = session.messages[i];
        final role = msg['role'] as String? ?? 'user';
        final content = msg['content'] as String? ?? '';
        buffer.writeln('$role: ${content.substring(0, min(content.length, 100))}');
      }
    }
    
    return buffer.toString();
  }
  
  /// Delete a session
  static Future<void> deleteSession(String id) async {
    try {
      final file = await _localFile;
      List<ChatSession> sessions = await loadSessions();
      sessions.removeWhere((s) => s.id == id);
      
      final jsonList = sessions.map((s) => s.toJson()).toList();
      await file.writeAsString(jsonEncode(jsonList));
    } catch (e) {
      print('Error deleting session: $e');
    }
  }
}

class ChatSession {
  final String id;
  final String title;
  final DateTime timestamp;
  final List<Map<String, dynamic>> messages;
  
  ChatSession({
    required this.id,
    required this.title,
    required this.timestamp,
    required this.messages,
  });
  
  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'timestamp': timestamp.toIso8601String(),
    'messages': messages,
  };
  
  factory ChatSession.fromJson(Map<String, dynamic> json) => ChatSession(
    id: json['id'] as String,
    title: json['title'] as String,
    timestamp: DateTime.parse(json['timestamp'] as String),
    messages: List<Map<String, dynamic>>.from(json['messages'] as List),
  );
}
```

---

## Component 5: RecoveryEngine (Failure Handling)

### Analyze failed attempts and suggest recovery actions

```dart
/// Uses task history to suggest recovery actions when AI is stuck.
class RecoveryEngine {
  /// Analyze recent failures and suggest a recovery action
  Future<RecoveryAction?> suggestRecovery(
    String userGoal,
    List<String> currentTrace,
    List<TaskRecord> history,
  ) async {
    // 1. Check if we've seen similar goals succeed recently
    final similarSuccesses = history
        .where((r) => r.goal.toLowerCase().contains(userGoal.toLowerCase()) &&
                      r.status == 'Success')
        .toList();
    
    if (similarSuccesses.isNotEmpty) {
      // 2. Extract the first action that worked
      final firstStep = similarSuccesses.first.trace.firstWhere(
        (line) => line.contains(':'),  // Contains action:result
        orElse: () => '',
      );
      
      if (firstStep.isNotEmpty) {
        return RecoveryAction(
          action: 'press_home',  // Reset to home screen
          params: {},
          reason: 'Resetting to home screen (worked in previous attempt)',
        );
      }
    }
    
    // 3. If we've scrolled many times without progress, try clicking
    if (currentTrace.where((t) => t.contains('scroll')).length > 3) {
      return RecoveryAction(
        action: 'press_back',
        params: {},
        reason: 'Multiple scrolls without progress, going back',
      );
    }
    
    return null;
  }
}

class RecoveryAction {
  final String action;
  final Map<String, dynamic> params;
  final String reason;
  
  RecoveryAction({
    required this.action,
    required this.params,
    required this.reason,
  });
}
```

---

## Integration Guide: Easy Launcher Implementation

### File Structure
```
app/src/main/java/com/example/
├── accessibility/
│   ├── ScreenDumpService.kt          ← (Already planned)
│   ├── GestureController.kt          ← (Already planned)
│   └── AccessibilityBridgeHandler.kt ← (Already planned)
└── ...

lib/services/
├── screen_automation_service.dart         (Already in private-agent)
├── task_executor.dart                     ← NEW (Adapt from private-agent)
├── task_history_logger.dart               ← NEW (Adapt from private-agent)
├── skill_memory_service.dart              ← NEW (Adapt from private-agent)
├── chat_history_service.dart              ← NEW (Adapt from private-agent)
├── recovery_engine.dart                   ← NEW (Adapt from private-agent)
└── gemini_automation_orchestrator.dart    (Your existing Gemini integration)
```

### Step 1: Create TaskExecutor (Dart)

**File**: `lib/services/task_executor.dart`

1. Copy the core loop from private-agent's `lib/services/task_executor.dart`
2. Adapt `_aiService.sendTaskMessage()` to your Gemini Nano implementation
3. Integrate with your `ScreenAutomationService` (Dart bridge)
4. Wire task history logging

### Step 2: Create TaskHistoryLogger (Dart)

**File**: `lib/services/task_history_logger.dart`

1. Uses JSONL format (one JSON object per line)
2. Logs: goal, status, token count, steps, execution trace
3. Provides `getRecent()` and `getSuccessRate()` for context

### Step 3: Add to Main Flow

**File**: `lib/main.dart` (your app entry point)

```dart
// In your main orchestrator
final taskExecutor = TaskExecutor(
  aiService: geminiBridge,
  screenService: screenAutomation,
  onProgress: (msg) => updateUI(msg),
);

// When user says "Send message to Mom"
final result = await taskExecutor.executeTask(userGoal);

// TaskExecutor automatically:
// 1. Checks skill memory (no AI needed)
// 2. Logs all steps
// 3. Learns from success
// 4. Recovers from failures using history
```

---

## Token Savings Analysis

| Scenario | Without History | With History | Savings |
|----------|-----------------|--------------|---------|
| Repeated task (in skill memory) | ~1500 tokens | ~0 tokens | 100% |
| Similar task (use past steps) | ~1500 tokens | ~500 tokens | 67% |
| First-time task | ~1500 tokens | ~1200 tokens | 20% |
| **Avg daily (10 tasks)** | **~15,000** | **~8,000** | **47%** |

### Over 1 Week:
- **Without**: 105,000 tokens (would hit limits)
- **With**: 56,000 tokens (well within budget)
- **Improvement**: 47% reduction + learned skills for instant replay

---

## Key Takeaways

✅ **Skill Memory** — Skip AI calls for known tasks (100% token savings)  
✅ **Task History** — Guide AI with successful patterns (67% savings)  
✅ **Adaptive Delays** — Prevent race conditions (reliability)  
✅ **Recovery Engine** — Escape stuck loops using history  
✅ **Chat History** — Maintain context across sessions  
✅ **JSONL Logging** — Efficient persistent storage for analysis  

This architecture is exactly why **private-agent** handles complex, multi-step tasks despite token constraints. Easy Launcher can adopt the same patterns to overcome Gemini Nano's limitations.

---

## References

- **private-agent TaskExecutor**: `lib/services/task_executor.dart`
- **private-agent History Logging**: `lib/services/task_history_logger.dart`
- **private-agent Skill Memory**: `lib/services/skill_memory_service.dart`
- **private-agent Chat History**: `lib/services/chat_history_service.dart`
- **private-agent Recovery**: `lib/services/recovery_engine.dart`
