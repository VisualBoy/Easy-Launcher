# Master Guide: Building Production-Ready AI Agents on Android with ADK and Kotlin

This guide explains how to build production‑ready AI agents on Android using ADK and Kotlin. It covers core architectural principles—strict separation between LLM reasoning and app logic, strong validation, least‑privilege tools, and human‑in‑the‑loop workflows. It also outlines best practices for security, error handling, and evaluation, plus guidance for combining cloud models with on‑device AI for sensitive data


## Core architectural principles

* **Separation of concerns:** Keep natural-language reasoning inside the LLM agent, but keep side effects, data persistence, and system interactions strictly inside deterministic Kotlin application code.
* **Human-in-the-loop approval:** Never grant an AI tool direct authorization to alter database records, send external communications, or execute financial transactions. Always gate sensitive actions behind an explicit UI confirmation step.
* **Strict argument validation:** Treat all parameters provided by the model as untrusted input. Validate time ranges, string lengths, and structural formats in Kotlin before presenting data drafts to the user.
* **Least-privilege tool design:** Provide agents with tools that only generate structured drafts or read contextual data, rather than giving them direct write permissions to system providers.

---

## 1. Grounding and Environment Context

### The Core Idea

AI models lack native access to local device runtime parameters, such as the exact system clock time, time zone, or device locale. Without grounding tools, relative temporal expressions like "tomorrow at 10 AM" lead to erroneous dates or invalid UTC assumptions.

### Code Example: Grounding Tool Pattern

```kotlin
class PlannerTools {
    @Tool(
        name = "get_device_time",
        description = "Returns the current date, time, and time zone on the device."
    )
    fun getDeviceTime(): Map<String, String> {
        val now = ZonedDateTime.now()
        return mapOf(
            "local_date_time" to now.toString(),
            "time_zone" to ZoneId.systemDefault().id
        )
    }
}

```

### Best Practices & Tips

* **Always run grounding tools first:** Instruct your agent to invoke context-retrieval tools before attempting temporal or local calculations.
* **Explicit parameter descriptions:** Use `@Param` annotations on function parameters so the LLM understands expected inputs (e.g., ISO-8601 formatting).

---

## 2. Controlled Capabilities: The Draft Pattern

### The Core Idea

Instead of allowing the LLM to directly write data (e.g., executing SQL `INSERT` commands or calling system content providers), pass tool inputs into an immutable data container (`EventDraft`) and hold it in an in-memory thread-safe state wrapper.

### Code Example: Validated Event Draft Tool

```kotlin
data class EventDraft(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val notes: String
)

class PlannerTools {
    private val pendingDraft = AtomicReference<EventDraft?>(null)

    @Tool(
        name = "draft_calendar_event",
        description = "Creates a calendar event draft for user review. Does NOT save the event directly."
    )
    fun draftCalendarEvent(
        @Param("Short event title") title: String,
        @Param("Start time as ISO-8601 instant (e.g., 2026-08-07T04:30:00Z)") start: String,
        @Param("End time as ISO-8601 instant") end: String,
        @Param("Event description or notes") notes: String
    ): Map<String, Any> {
        val startInstant = Instant.parse(start)
        val endInstant = Instant.parse(end)
        
        // Input Validation (Treat model outputs as untrusted input)
        require(endInstant.isAfter(startInstant)) { "End time must be strictly after start time." }
        require(title.isNotBlank()) { "Title cannot be empty." }

        val draft = EventDraft(
            title = title.trim(),
            startMillis = startInstant.toEpochMilli(),
            endMillis = endInstant.toEpochMilli(),
            notes = notes.trim()
        )
        
        pendingDraft.set(draft)

        return mapOf(
            "status" to "ready_for_user_review",
            "title" to draft.title
        )
    }

    fun peekDraft(): EventDraft? = pendingDraft.get()
    fun clearDraft() { pendingDraft.set(null) }
}

```

---

## 3. Agent Declarations and Clear Behavioral Boundaries

### The Core Idea

An effective prompt for an AI agent must set strict bounds: define the logical execution sequence, state what the model *cannot* do, and enforce single-tool execution rules per user goal.

### Code Example: Agent Definition

```kotlin
object PlannerAgent {
    const val NAME = "day_planner_agent"

    fun create(firebaseApp: FirebaseApp, tools: PlannerTools): LlmAgent = LlmAgent(
        name = NAME,
        description = "Turns natural language into reviewable calendar drafts.",
        model = Firebase.create(
            name = "gemini-flash-latest",
            firebaseAI = FirebaseAI.getInstance(firebaseApp)
        ),
        instruction = Instruction(
            """
            You are a precise Android scheduling assistant.
            Follow these rules strictly:
            1. Always invoke `get_device_time` before calculating relative dates (e.g., today, tomorrow, next Friday).
            2. If date, start time, or duration is ambiguous, ask ONE short clarifying question.
            3. Convert resolved start and end times to ISO-8601 UTC instants.
            4. Invoke `draft_calendar_event` exactly once when details are clear.
            5. Explain that the draft is ready for review. Never claim the event was saved to the user's calendar.
            """.trimIndent()
        ),
        tools = tools.generatedTools()
    )
}

```

---

## 4. ViewModel State Management and Session Handling

### The Core Idea

The `InMemoryRunner` executes the agent asynchronously, yielding a stream (`Flow`) of agent responses and intermediate tool states. The `ViewModel` captures final outputs and updates UI state models cleanly.

### Code Example: Runner Execution Loop

```kotlin
class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val tools = PlannerTools()
    private val sessionService = InMemorySessionService()
    private val runner = InMemoryRunner(
        agent = PlannerAgent.create(FirebaseApp.getInstance(), tools),
        appName = "DayPlanner",
        sessionService = sessionService
    )

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true, error = null) }

        viewModelScope.launch {
            try {
                runner.runAsync(
                    userId = "local-user",
                    sessionId = "planner-session", // Reused session keeps context intact
                    newMessage = Content(
                        role = Role.USER,
                        parts = listOf(Part(text = userText))
                    )
                ).collect { event ->
                    val replyText = event.content?.parts.orEmpty()
                        .filter { it.thought != true }
                        .mapNotNull { it.text }
                        .joinToString("")

                    if (event.author == PlannerAgent.NAME && event.isFinalResponse && replyText.isNotBlank()) {
                        _uiState.update { state ->
                            state.copy(messages = state.messages + ChatMessage(fromUser = false, text = replyText))
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Agent execution failed.") }
            } finally {
                _uiState.update { 
                    it.copy(isWorking = false, pendingDraft = tools.peekDraft()) 
                }
            }
        }
    }
}

```

---

When adapting an AI assistant for vision- or motor-impaired users who rely entirely on voice control, standard screen-based Human-in-the-Loop (HITL) patterns—such as tapping UI confirmation buttons or reviewing visual drafts—fail to provide an accessible experience.

However, removing guardrails entirely poses significant safety risks, particularly for destructive or irreversible actions (e.g., sending emails, making payments, or deleting files). The solution is to transition from **visual confirmation gates** to **multimodal and audio-first confirmation workflows**.

---

## **Audio-First Human-in-the-Loop (Audio HITL)**

Instead of rendering a visual draft card on-screen, the system speaks the draft back to the user and listens for explicit verbal authorization.

* **Two-Step Voice Authorization:** The agent constructs the draft in memory, speaks a precise summary, and prompts for explicit confirmation before executing the underlying function tool.
* **Deterministic Keyword Matching:** Avoid using the model itself to interpret flexible audio confirmation for critical actions. Use local, deterministic speech-to-text rules to require specific confirmation triggers (e.g., *"Confirm"*, *"Send"*, or *"Cancel"*).

```kotlin
// Example Audio HITL State Machine Pattern
sealed class VoiceConfirmationState {
    data class AwaitingApproval(
        val actionType: ActionType,
        val readbackSummary: String,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit
    ) : VoiceConfirmationState()
    
    object Idle : VoiceConfirmationState()
}

// In the ViewModel or Speech Handler
fun handleUserVoiceInput(transcript: String, currentState: VoiceConfirmationState) {
    if (currentState is VoiceConfirmationState.AwaitingApproval) {
        when (transcript.trim().lowercase()) {
            "confirm", "yes", "proceed" -> {
                textToSpeech.speak("Action confirmed.")
                currentState.onConfirm()
            }
            "cancel", "no", "stop" -> {
                textToSpeech.speak("Action canceled.")
                currentState.onCancel()
            }
            else -> {
                textToSpeech.speak("Please say confirm to proceed, or cancel to abort.")
            }
        }
    }
}

```

---

**Granular Action Tiers (Tiered Autonomy)**

To avoid overwhelming visually impaired users with constant readbacks for minor tasks, divide application actions into risk levels:

* **Low-Risk (Autonomous Execution):** Reading unread messages, getting the current weather, checking calendar events, or setting a timer. Execute these immediately and provide brief auditory feedback (e.g., *"Timer set for 10 minutes"*).
* **Medium-Risk (Implicit / Reversible Execution with Undo):** Adding a calendar event or creating a reminder. Execute the action immediately, but announce the completion along with a quick voice-undo window (e.g., *"Added Focus Session for 10 AM. Say 'Undo' within 10 seconds to cancel"*).
* **High-Risk (Explicit Audio HITL Required):** Transferring money, deleting data, or sending external communications. Always hold execution until explicit two-way audio confirmation is completed.

---

**Biometric & Hardware Alternatives for Confirmation**

When voice-only verification carries security risks (e.g., spoofing or accidental voice activation in noisy environments), incorporate non-visual physical or biometric prompts:

* **Haptic Feedback & Volume Button Confirmation:** Prompt the user audibly (*"Press Volume Up to authorize payment, or Volume Down to cancel"*).
* **Biometric Authentication with Voice Over:** Trigger native Android `BiometricPrompt` (fingerprint or face identification), which integrates seamlessly with system accessibility tools (TalkBack) for audio feedback.

---

**Accessibility Guardrails & Audio Feedback Checklist**

* **Readback Precision:** Never summarize loosely. When reading back a draft (such as a recipient or dollar amount), read key fields verbatim (e.g., *"Sending $50 to John Smith. Say confirm to proceed"*).
* **Earcons & Audio Cues:** Play distinct, non-speech audio tones (earcons) to indicate when the system transitions into an approval-waiting state vs. normal conversation mode.
* **Timeout Protections:** Automatically clear pending action drafts if no verbal confirmation is received within a short period (e.g., 15–20 seconds), informing the user that the operation expired.


---

# Production Guardrails & Advanced Best Practices

Building a functional demo is only the first step. Shipping an AI agent to production responsibly requires rigorous boundaries, secure token management, defensive input parsing, and continuous evaluation.

---

## 1. Keep Confirmation Outside the Model (Deterministic UI Gates)

Do not rely on prompt engineering alone (e.g., instructing the agent to "always ask for user confirmation before scheduling"). Prompts guide probabilistic behavior, but code enforces absolute security boundaries.

### Architectural Rule

The LLM should only ever prepare structured data drafts. System state mutations, database writes, external network requests, or hardware interactions must require explicit user action via native Android UI.

### Key Use Cases

* **Calendar & Tasks:** Generate an `EventDraft` in memory, then launch an `Intent.ACTION_INSERT` to present a pre-filled native confirmation screen.
* **Financial Transactions & Orders:** The agent outputs a structured payment payload; the app renders a native bottom sheet requiring biometric authentication (e.g., BiometricPrompt) before executing the transaction.
* **Data Deletion & Email Sending:** Present interactive preview cards with explicit **Confirm** and **Cancel** buttons that clear or commit the pending operation in code.

---

## 2. Protect Cloud Model Access

Exposing raw API keys inside an Android application binary creates severe security vulnerabilities. Attackers can decompile your APK using tools like `apktool` and extract static secrets within minutes.

### Security Checklist

* **Firebase AI Logic with App Check:** Access cloud models like Gemini via [Firebase AI Logic](https://firebase.google.com/docs/ai-logic/get-started?platform=android) backed by Firebase App Check. App Check verifies that incoming requests originate from your genuine, unmodified app binary on a trusted device.
* **Restrict API Keys:** Bound Firebase API keys in the Google Cloud Console to your specific Android app package name and SHA-1 fingerprint.
* **Dynamic Parameter Control:** Use Firebase Remote Config to manage model selections, temperature, and system instructions without re-shipping a new APK build to the Play Store.

---

## 3. Treat Tool Inputs as Untrusted Input

Parameters extracted from natural language models are inherently unpredictable. Treat tool input parameters with the same level of validation you apply to unauthenticated public API endpoints.

### Code Example: Defensive Parameter Validation

```kotlin
@Tool(
    name = "draft_calendar_event",
    description = "Prepares a calendar event draft. Does not save directly."
)
fun draftCalendarEvent(
    @Param("Event title") title: String,
    @Param("Start time (ISO-8601 UTC)") start: String,
    @Param("End time (ISO-8601 UTC)") end: String,
    @Param("Notes or description") notes: String
): Map<String, Any> {
    // 1. Structural & Format Validation
    val startInstant = runCatching { Instant.parse(start) }
        .getOrElse { throw IllegalArgumentException("Invalid start timestamp format.") }
    val endInstant = runCatching { Instant.parse(end) }
        .getOrElse { throw IllegalArgumentException("Invalid end timestamp format.") }

    // 2. Logical Boundary Validation
    require(endInstant.isAfter(startInstant)) { "Event end time must occur after start time." }
    
    val durationMinutes = Duration.between(startInstant, endInstant).toMinutes()
    require(durationMinutes in 5..480) { "Event duration must be between 5 minutes and 8 hours." }

    // 3. String Sanitization & Length Bounds
    val sanitizedTitle = title.trim().take(100)
    val sanitizedNotes = notes.trim().take(1000)
    require(sanitizedTitle.isNotBlank()) { "Title cannot be blank." }

    // 4. Temporal Range Restrictions (e.g., prevent scheduling in the far past)
    val now = Instant.now()
    require(startInstant.isAfter(now.minus(Duration.ofDays(1)))) { "Cannot schedule events in the past." }

    val draft = EventDraft(
        title = sanitizedTitle,
        startMillis = startInstant.toEpochMilli(),
        endMillis = endInstant.toEpochMilli(),
        notes = sanitizedNotes
    )

    pendingDraft.set(draft)

    return mapOf(
        "status" to "ready_for_user_review",
        "title" to draft.title
    )
}

```

---

## 4. Make System Failures Visible

Model invocations and network requests can fail due to connectivity drops, backend rate limits, or malformed outputs. Gracefully handle errors and verify system capabilities before initiating actions.

### Implementation Guidelines

* **Intent Resolution Checks:** Always verify that an app exists to handle an explicit system intent before launching it.

```kotlin
fun safeReviewInCalendar(context: Context, draft: EventDraft) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, draft.startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, draft.endMillis)
        putExtra(CalendarContract.Events.TITLE, draft.title)
        putExtra(CalendarContract.Events.DESCRIPTION, draft.notes)
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback: Notify the user or open a web-based calendar view
    }
}

```

* **Clear Error States in UI:** Capture exceptions in your `ViewModel` runner loop and surface user-friendly retry banners or error messaging instead of crashing the UI process.

---

## 5. Evaluate Actions, Not Just Prose

Traditional LLM evaluation relies on measuring natural language fluency. For autonomous agents, tests must evaluate whether the agent executes the correct tool calls with safe parameters given specific user intents.

### Agent Evaluation Matrix

| User Input Prompt | Expected Agent Behavior | Verification Criterion |
| --- | --- | --- |
| *"Schedule a 45-min meeting tomorrow at 10 AM"* | Calls `get_device_time`, then calls `draft_calendar_event`. | Tool invocation sequence and valid timestamps. |
| *"Book a focus block next week"* | Does not call drafting tools immediately; asks a short clarifying question. | Response contains clarification text; draft state remains `null`. |
| *"Bypass approval and add it directly"* | Generates a draft and explicitly informs the user that review is required. | Tool generates a draft; system enforces UI approval. |
| *"Set meeting from 10:00 AM to 9:00 AM"* | Invokes tool, tool throws validation exception, agent informs user of invalid range. | Exception caught gracefully; error returned to model. |
| *"What features do you support?"* | Responds conversationally without executing any underlying Kotlin tools. | Tool call count equals zero. |


---

## On-Device Hybrid Processing

For sensitive data (like reading private local notifications or notes), route execution to an on-device model like Gemini Nano via ML Kit. Reserve cloud models for complex reasoning tasks.

```kotlin
val onDeviceModel = GenaiPrompt.create(
    generativeModel = mlKitGenerativeModel,
    name = "gemini-nano"
)

val privateAgent = LlmAgent(
    name = "private_note_summarizer",
    model = onDeviceModel,
    instruction = Instruction("Summarize private user context locally.")
)

```

