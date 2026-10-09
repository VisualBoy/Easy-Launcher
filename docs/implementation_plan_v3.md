# Implementation Plan v1.2.0 — Easy Launcher

Completamento dell'integrazione agentica: (1) collegamento di `TaskExecutor` al vero ciclo `GeminiAssistantManager` con system prompt e `parseAction`; (2) upgrade di `FloatingOverlayService` a Jetpack Compose UI; (3) wiring di `TaskExecutor` e `FloatingOverlayService` in `MainActivity`.




### 1. TaskExecutor ↔ GeminiAssistantManager — Real AI Decision Loop

#### [MODIFY] [`TaskExecutor.kt`](file:///c:/Users/glitch/antigravity/Easy-Launcher/app/src/main/java/com/example/ai/TaskExecutor.kt)

Lo **Step 4 (AI Decision)** attualmente usa un JSON stub. Sostituirlo con il vero ciclo Function Calling tramite `GeminiAssistantManager`:

- Iniettare `GeminiAssistantManager` come dipendenza (costruttore o `companion object`).

**System Prompt — Agente** (portato da [`ai_service.dart`](file:///C:/Users/glitch/.gemini/antigravity-ide/brain/d28cc846-4939-4ae4-9f61-2cacd51fd441/scratch/private-agent/lib/services/ai_service.dart#L62-L100), adattato per Easy Launcher + lingua italiana):

```kotlin
private const val AGENT_SYSTEM_PROMPT = """
You are EasyAgent, an AI assistant that controls an Android smartphone on behalf of elderly or motor-impaired users.
You can perform device actions AND hold normal conversations.
IMPORTANT: The user speaks Italian. Always write the "response" field and any conversational reply in Italian.

When the user wants to perform a device action, respond with ONLY a JSON object (no markdown, no code fences, no extra text) in this exact format:
{"action": "action_name", "params": {"key": "value"}, "response": "What you say to the user in Italian"}

Available actions and their params:

SIMPLE ACTIONS (single step only):
- open_app: {"app_name": "YouTube"} - ONLY use this when the user JUST wants to open an app and nothing else
- make_call: {"contact_name": "Mom"} OR {"phone_number": "1234567890"} - Makes a phone call
- send_sms: {"contact_name": "Marco", "message": "Ciao"} OR {"phone_number": "123", "message": "Hi"} - Sends an SMS
- send_whatsapp: {"contact_name": "Marco", "message": "Arrivo tra 5 minuti"} - Sends a WhatsApp message
- trigger_sos: {} - Triggers the SOS emergency sequence
- toggle_torch: {"on": true} - Turns the flashlight on or off
- read_notifications: {} - Reads recent notifications aloud
- get_device_status: {} - Returns time, date, battery level, torch state
- search_contact: {"query": "Marco"} - Searches contacts
- set_alarm: {"hour": 8, "minute": 0, "label": "Medicina"} - Sets an alarm
- read_screen: {} - Reads what is currently on screen
- press_back: {} - Presses the back button

MULTI-STEP TASK (for anything requiring more than one action):
- execute_task: {"goal": "full description of the objective"} - Automatically reads screen, taps, scrolls, types step by step

CRITICAL RULES:
1. If the user request contains "and" / "e" or involves MULTIPLE steps (open + search, open + send, open + find, etc.), you MUST use execute_task. NEVER use open_app for these.
2. execute_task handles everything: opening apps, finding elements, clicking, typing, scrolling.

Examples of when to use execute_task:
- "Crea una sveglia per le 8" → execute_task goal "Create a new alarm for 8 AM"
- "Vai su YouTube e cerca gatti" → execute_task
- "Apri WhatsApp e manda ciao a Marco" → execute_task
- "Apri Impostazioni e attiva il WiFi" → execute_task
- "Cerca ristoranti su Google Maps" → execute_task

Examples of when to use open_app:
- "Apri YouTube" → open_app (just opening, no further action)
- "Apri le Impostazioni" → open_app (just opening)

For normal conversation (questions, chat, information requests), reply in plain Italian text naturally.
"""

private const val CHAT_SYSTEM_PROMPT = """
You are EasyAgent, a friendly conversational AI assistant.
IMPORTANT: The user speaks Italian. Always respond in Italian.
Provide direct, natural, and reassuring text responses. You cannot perform device actions or run tools.
Answer questions, explain concepts, help write messages, and chat with the user in plain text.
"""
```

**Logica `sendTaskMessage` / `parseAction`** (da [`ai_service.dart`](file:///C:/Users/glitch/.gemini/antigravity-ide/brain/d28cc846-4939-4ae4-9f61-2cacd51fd441/scratch/private-agent/lib/services/ai_service.dart#L470-L612)):
- Nessuna conversation history per le chiamate task (stateless, a basso costo).
- Temperatura bassa (0.2), max 1024 token.
- Retry automatico con backoff lineare: fino a **4 tentativi**, delay `3 * tentativo` secondi.
- Strip blocchi `<think>…</think>` dalla risposta (modelli reasoning like DeepSeek/Nemotron).
- `parseAction`: strip code fence ` ``` ` se presente → parse JSON → fallback aggiunta `}` se risposta troncata.

```kotlin
data class AgentAction(
    val action: String,
    val params: Map<String, String> = emptyMap(),
    val response: String = ""
)

// In GeminiAssistantManager o nuovo AgentAiService:
suspend fun sendTaskMessage(systemPrompt: String, userPrompt: String): AgentAction
fun parseAction(raw: String): AgentAction?  // null = risposta testuale, non un'azione
```

- Inviare dump `ScreenDumpService.dumpScreen()` + ultima azione + goal come `userPrompt`.
- Propagare `totalTokens` (da `usage.total_tokens`) a `TaskHistoryLogger` dopo ogni risposta.

---

### 2. FloatingOverlayService — Jetpack Compose UI

#### [MODIFY] [`FloatingOverlayService.kt`](file:///c:/Users/glitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/FloatingOverlayService.kt)

Sostituire l'attuale widget `FrameLayout/ImageView` con un `ComposeView` ancorato al `WindowManager`:

```kotlin
val composeView = ComposeView(this).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    setContent {
        FloatingOverlayWidget(
            state = overlayState,
            onMicClick = { startVoiceCapture() },
            onSosClick = { triggerSos() },
            onDismiss = { collapseOverlay() }
        )
    }
}
windowManager.addView(composeView, layoutParams)
```

**Stati UI:**
- **Collapsed**: Pillola microfono neon-cyan 56dp draggabile. Bordo animato pulsante.
- **Expanded**: Card alto contrasto 320dp con:
  - Label stato (`Ascolto…`, `Elaborazione…`, `Esecuzione…`, `Risposta`)
  - 3 chip rapidi: `📞 Chiama`, `💬 WhatsApp`, `🚨 SOS`
  - Pulsante microfono centrale 72dp
  - Pulsante chiusura overlay

**Drag support**: `MotionEvent` su `Modifier.pointerInput` aggiorna `layoutParams.x/y` via `windowManager.updateViewLayout`.

> [!NOTE]
> **§3 già implementato** — [`SkillMemoryService.kt`](file:///c:/Users/glitch/antigravity/Easy-Launcher/app/src/main/java/com/example/ai/SkillMemoryService.kt) contiene già:
> - Regex accenti: `[^a-zàèéìòù0-9\s]` (linea 89)
> - Stop-words italiane complete: articoli, preposizioni semplici e articolate, congiunzioni, pronomi, verbi servili e filler vocale (linee 24-40)
> - `jaccardSimilarity()`, `findSkill()` (soglia 0.6), `saveSkill()` (update >0.8), `recordFailure()`, persistenza JSONL
> 
> **Nessuna modifica necessaria.**

---

## Verification Plan

### Build
```bash
./gradlew assembleDebug    # Build verde senza errori
```

### Manual E2E (on-device)
1. **Real AI Loop**: comando vocale `"Apri WhatsApp e scrivi a Luca: Ciao"` → `TaskExecutor.execute()` → `execute_task` selezionato → ciclo multi-step ScreenDump + GestureController.
2. **Azione semplice diretta**: `"Accendi la torcia"` → JSON `{"action":"toggle_torch","params":{"on":true}}` → torcia accesa senza passi intermedi.
3. **parseAction fence stripping**: risposta AI in ` ```json ` → deserializzata correttamente.
4. **Retry backoff**: API irraggiungibile → log `retrying 1/4 in 3s`, `retrying 2/4 in 6s`.
5. **TaskExecutor wired**: log `[TaskExecutor] execute() called` alla prima voce — **NON** `[GeminiAssistant] processIntent()`.
6. **Compose Overlay avviato**: alla prima apertura dell'app con permesso overlay attivo → pillola neon visibile sopra qualsiasi app → tap → card espanso con chip SOS/WhatsApp/Chiama.
7. **Overlay permission flow**: prima apertura senza permesso → reindirizzamento a `Impostazioni > Mostra sopra ad altre app`.
8. **0-Token Replay**: ripetere stesso comando → log `[SkillMemoryService] Replaying saved skill (0 tokens)`.
