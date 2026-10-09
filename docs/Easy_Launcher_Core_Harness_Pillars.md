
## Easy Launcher Core AI Harness Pillars

**Harness engineering** is the discipline of designing the supporting technical scaffolding—context delivery, tool interfaces, planning artifacts, verification loops, memory systems, and sandboxes—that surrounds an AI model to enable reliable execution on real-world tasks. Because AI models cannot accomplish complex, multi-step objectives alone, engineering the system surrounding the model is the primary lever for improving agent reliability.

When applied to mobile platforms like **Easy Launcher**—which operates on-device via **Gemini Nano**, **ML Kit OCR**, and **coordinate gesture automation** to navigate standard as well as unoptimized Android applications—the supporting environment must be optimized across four core harness pillars:

---

### 1. Context Delivery & Compaction

#### Best Practices
* **Treat Context as a Finite, Curated Resource:** Rather than injecting raw UI state dumps or full logs, system prompts and tool outputs must be managed as finite context budgets.
* **Quarantine Critical Rules from Compaction:** Compaction algorithms can silently drop policy rules and constraints over long sessions (a phenomenon known as constraint rot). Essential system directives and project rules should be stored in static instruction files (such as `AGENTS.md` or `CLAUDE.md`) in the system prompt layer so they survive compaction rounds.
* **Navigate via Pointers Instead of Raw Dumps:** For code bases or screen dumps, navigation by AST symbols or structured pointers drastically reduces active token consumption compared to reading raw monolithic files.

#### Useful Tools & Primitives
* **LLMLingua / LLMLingua-2:** Prompt compression toolkits capable of compressing context up to 20x prior to inference.
* **Prompt Caching:** Placing `cache_control` breakpoints across system prompts and tool definitions cuts inference latency and cost across multi-turn sessions.
* **headroom & context-mode:** MCP servers and proxies that intercept and compress bulky tool outputs (logs, snapshots, file contents) before they enter the model's context window.
* **Token Savior & codebase-memory-mcp:** Symbol-indexing MCP servers that replace whole-file reads with sub-millisecond pointer lookups, cutting active tokens by over 75%.

---

### 2. Typed Tool Interfaces

#### Best Practices
* **Tool Design is Agent UX:** Tool names, descriptions, parameter schemas, and return formats directly determine model execution success. Schemas should be strict and unambiguous.
* **Risk Annotations for Permissions:** Use explicit risk hints (`readOnlyHint`, `destructiveHint`, `idempotentHint`, `openWorldHint`) on tool schemas so the harness can make automated security and approval decisions.
* **Type Safety & CFG Decoding:** Constrain sampling at the decoding layer using JSON Schema or Pydantic models to eliminate JSON parsing errors.
* **Code Execution Primitives:** Wrap repetitive multi-step tool calls into code execution blocks or parallel tool calls to reduce model round trips and context inflation.

#### Useful Tools & Frameworks
* **Model Context Protocol (MCP):** The open standard for connecting AI agents to external tools, system utilities, and data sources.
* **agent-device:** An MCP-native control layer specifically built for **Android and iOS mobile devices**, providing semantic targeting, UI snapshots, typed client access, and replayable mobile workflows.
* **Instructor & Outlines:** Libraries that map Pydantic data models directly to structured LLM outputs with built-in validation retry loops or regex/CFG decoding constraints.
* **A2A Protocol & Agentic Resource Discovery:** Protocols for structured inter-agent communication and runtime capability discovery.
* **Composio & Nango:** Production authentication and API integration gateways for wrapping external actions into agent-ready tools.

---

### 3. Planning Artifacts & Skill Memory

#### Best Practices
* **Plan-and-Execute Architecture:** Decouple high-level task decomposition from low-level step execution by assigning separate planner and executor sub-agent roles.
* **Externalize Plans as Machine-Checkable Artifacts:** Maintain persistent markdown planning documents (`PLAN.md`, `IMPLEMENT.md`, `tasks.md`, `spec.md`) that serve as persistent state across context windows and session restarts.
* **Versioned Skill Bundles (`SKILL.md`):** Package proven procedural workflows into versioned skill files with explicit manifests and negative examples to guide routing accuracy.
* **Multi-Tier Memory Architecture:** Separate working memory from long-term memory using core, recall, and archival storage tiers or structured graph-vector stores.

#### Useful Tools & Frameworks
* **GitHub Spec Kit:** Open-source toolkit for spec-driven planning (`spec.md` → `plan.md` → `tasks.md`) that converts intent into machine-checkable steps.
* **Letta (MemGPT) & mem0:** Three-tier agent memory platforms (core, recall, archival) for managing state across long-running tasks.
* **Microsoft Skills Framework & SkillsBench:** Infrastructure for defining, versioning, evaluating, and sharing reusable agent skills.
* **cognee & Zep:** Hybrid graph-vector-relational memory platforms that enable structured recall and automatic conversation summarization.

---

### 4. Self-Healing Verification Loops & Failure Recovery

#### Best Practices
* **Feedforward Guides & Feedback Sensors:** Treat the harness as a combination of feedforward instructions (prompts, skills) and feedback sensors (linters, test runners, verifiers) that self-correct errors before output reaches the user.
* **Actionable Verifier Error Messages:** Verifier feedback must explain *why* an action failed and provide specific error context so the agent can self-correct in a single iteration.
* **Multi-Layer Fault Tolerance:** Implement layered recovery logic—starting with immediate retries with backoff, stepping down to model fallback chains, running error classification, and finally restoring from checkpoint state.
* **Loop Detection Middleware:** Monitor execution traces for repetitive identical actions or non-responsive UI states to trigger circuit breakers before token budgets are exhausted.

#### Useful Tools & Frameworks
* **promptfoo & DeepEval:** Automated testing and evaluation frameworks offering assertion DSLs, LLM-as-judge metrics, and native CI integration.
* **Terminal-Bench / Harbor:** Sandboxed execution benchmarks for validating shell and system agent actions against test assertions.
* **AgentOps, Arize Phoenix, & Langfuse:** Observability platforms providing step-by-step trace visualization, call graph capture, session replays, and failure detection.
* **AgentRx & TraceCoder:** Automated root-cause analysis and trace-driven debugging frameworks that perform causal fault isolation and execute validated recovery fixes.
* **shadcn-ui/lint:** An example of agent-first linter feedback where error messages contain exact contextual fix instructions to achieve single-round self-correction.

---

### Applying the Scaffolding to Easy Launcher

In Easy Launcher, combining on-device **Gemini Nano** and **ML Kit OCR** with coordinate gesture automation allows the system to interact with unoptimized Android interfaces. Integrating these harness engineering primitives creates a robust environment:

1. **Mobile Tool Interface:** Utilizing an MCP mobile abstraction like `agent-device` standardizes coordinate taps, text entry, and UI snapshots into typed tool schemas.
2. **Compact Screen Delivery:** Flattening accessibility trees or filtering OCR text blocks using tools like `headroom` prevents context overflow on lightweight on-device models.
3. **Skill Memory Macros:** Caching successful interaction traces as parameterized `SKILL.md` bundles allows recurring tasks (e.g., sending a WhatsApp message or making an emergency SOS call) to replay reliably without continuous model inference.
4. **Self-Healing Recovery:** Implementing multi-layer fault tolerance (detecting stuck loops, dismissing popups, recalibrating coordinates relative to OCR anchors) ensures that automation on unoptimized UIs recovers gracefully from unexpected layout shifts.

---
---


Here is a complete, production-ready **`[link sospetto rimosso]` manifest template** tailored specifically for the **Easy Launcher** Android app harness, followed by concrete prompt templates and few-shot examples for on-device execution.

---

### 📄 `[link sospetto rimosso]` Manifest Template for Easy Launcher

```markdown
---
name: easy-launcher-unoptimized-app-navigation
description: >
  Executes spatial coordinate gesture sequences on unoptimized or non-accessible Android 
  applications (e.g., custom Canvas views, WebViews, or missing view IDs). 
  Triggers when user voice intent (Italian or English) requests actions like tapping unlabelled 
  buttons, navigating custom messaging apps, or interacting with canvas-rendered screens.
  DO NOT USE for native system actions accessible directly via Android Intents or standard 
  Accessibility API node IDs (e.g., emergency SOS, toggling torch, or native settings toggles).
version: 1.1.0
author: GlitchLab Studio / Easy Launcher Harness Team
tools:
  - ScreenDumpService
  - GestureController
  - MLKitOCR
  - SkillMemoryService
  - RecoveryEngine
---

# Skill: Spatial Coordinate Navigation on Unoptimized UIs

## 🎯 Purpose & Applicability
This skill enables Easy Launcher to execute multi-step touch automations on Android applications that lack accessible UI tree nodes or view IDs. By pairing local **ML Kit OCR text anchors** with **relative screen coordinates**, this macro allows hands-free voice control even on third-party or legacy user interfaces.

---

## 📋 Preconditions & Required Context
Before executing this skill, the harness must verify:
1. `AccessibilityService` is active and granted touch injection permissions (`dispatchGesture()`).
2. `OverlayPermission` is active to display state notifications to visually or cognitively impaired users.
3. Current foreground package matches the expected target application or can be launched via standard Intent.

---

## 🔄 Step-by-Step Execution Workflow

### Phase 1: Intent Normalization & Macro Cache Lookup
* **Input:** Raw spoken user intent (e.g., *"Invia un messaggio WhatsApp a Marco"*).
* **Process:** Normalize intent via `gemini-nano`. Check `SkillMemoryService` for cached `goalPattern` matching `"send_whatsapp {contact_name}"`.
* **Fast-Path Check:** If a matching macro exists with `successRate > 0.8` and `successCount >= 2`, proceed directly to Phase 3 (0-token execution). Otherwise, continue to Phase 2.

### Phase 2: Screen Analysis & Anchor Localization
1. Capture an invisible screen state via `takeScreenshot()`.
2. Run local **ML Kit OCR** to map text bounding boxes `(left, top, right, bottom)`.
3. Extract target visual anchors (e.g., target contact name, input box, or "Invia" button) and calculate exact center coordinates `(x, y)`.

### Phase 3: Spatial Gesture Dispatch
Execute actions sequentially via `GestureController`:
```json
[
  { "action": "type_text", "target_anchor": "Cerca...", "text": "{{contact_name}}" },
  { "action": "click_anchor", "target_anchor": "{{contact_name}}" },
  { "action": "type_text", "target_anchor": "Scrivi un messaggio", "text": "{{message_body}}" },
  { "action": "click_anchor", "target_anchor": "Invia" }
]
```

### Phase 4: Verification & Self-Healing Loop
1. Capture post-action screenshot and verify screen transition.
2. **Success Gate:** Target screen element (e.g., sent timestamp) is detected via OCR. Record execution success in `task_history.jsonl`.
3. **Failure / Stuck Gate:** If target anchor is not found after 3,000ms:
   * Trigger `RecoveryEngine.recalibrate_coordinates()`.
   * Re-scan fresh OCR dump to locate offset text anchors.
   * If recovery fails, fall back to global `press_back` or inform the user via TTS.

---

## 🚫 Negative Examples & Routing Counter-Examples
*Adding explicit negative examples improves routing accuracy up to 85%:*

* ❌ **User says:** *"Attiva la torcia"* / *"Chiama i soccorsi"*
  * **Incorrect Routing:** Do NOT invoke coordinate macro navigation.
  * **Correct Action:** Invoke native system tool (`toggle_torch` / `trigger_sos_call`) directly via Android system APIs.
* ❌ **User says:** *"Apri le impostazioni del telefono"*
  * **Incorrect Routing:** Do NOT simulate visual taps on the launcher drawer.
  * **Correct Action:** Dispatch native `Settings` Intent.
```

---

### 💬 Prompt Examples for Easy Launcher Agent Harness

Below are three specialized prompt templates engineered for Easy Launcher's on-device AI pipeline (`gemini-nano` and system controllers).

#### 1. Intent Normalization & Entity Extraction Prompt (Gemini Nano)
*Used by `SkillMemoryService` to parse raw spoken Italian input into standardized intent patterns.*

```yaml
---
template_id: easy_launcher_intent_normalizer_v1
model: gemini-nano
type: system_prompt
---
Tu sei l'assistente vocale di Easy Launcher per Android, un launcher accessibile per anziani e persone con disabilità.
Il tuo compito è trasformare il comando vocale grezzo dell'utente in un intento strutturato pulito, rimuovendo parole di riempimento ed estraendo le entità.

Istruzioni:
1. Rimuovi forme di cortesia (es. "per favore", "potresti", "vorrei").
2. Standardizza l'azione principale in inglese (es. send_message, make_call, open_app).
3. Estrai le entità dinamiche tra parentesi graffe: {contact_name}, {message_body}, {app_name}.

--- FEW-SHOT EXAMPLES ---

Input: "Puoi per favore mandare un messaggio WhatsApp a Marco dicendo che sto arrivando?"
Output:
{
  "normalizedGoal": "send_whatsapp {contact_name}",
  "intentCategory": "MESSAGING",
  "extractedEntities": {
    "contact_name": "Marco",
    "message_body": "sto arrivando"
  }
}

Input: "Vorrei chiamare mia figlia Lucia"
Output:
{
  "normalizedGoal": "make_call {contact_name}",
  "intentCategory": "TELEPHONY",
  "extractedEntities": {
    "contact_name": "Lucia"
  }
}

Input: "Apri le foto di ieri"
Output:
{
  "normalizedGoal": "open_app {app_name}",
  "intentCategory": "SYSTEM",
  "extractedEntities": {
    "app_name": "Galleria"
  }
}
```

---

#### 2. Visual OCR Target Resolution Prompt
*Used when standard Accessibility API fails, converting user intent + ML Kit OCR text dumps into spatial click actions.*

```yaml
---
template_id: ocr_spatial_target_resolver_v1
model: gemini-nano
type: task_prompt
---
Sei il motore di risoluzione spaziale per Easy Launcher. 
Ricevi l'obiettivo dell'utente e un dump JSON del testo rilevato da ML Kit OCR sullo schermo corrente con le relative coordinate di bounding box.

Obiettivo Utente: Premere il pulsante per inviare il messaggio a "Giuseppe".

Dati OCR Schermo (JSON):
[
  {"text": "Chat con Giuseppe", "bounds":},
  {"text": "Scrivi un messaggio...", "bounds":},
  {"text": "Invia", "bounds": }
]

Compito:
Seleziona il blocco OCR corrispondente all'azione richiesta e calcola le coordinate centrali (centerX, centerY) per il gesto di tap.

Risposta richiesta (JSON):
{
  "targetText": "Invia",
  "action": "clickAtCoordinates",
  "coordinates": {
    "x": 900,
    "y": 1840
  },
  "confidence": 0.98,
  "reasoning": "Trovato pulsante visivo 'Invia' calcolato al centro del bounding box ."
}
```

---

#### 3. Verification & Self-Healing Recovery Prompt
*Invoked by the `RecoveryEngine` when an action tap misses its target or gets stuck in a loop.*

```yaml
---
template_id: recovery_engine_stuck_loop_v1
model: gemini-nano
type: recovery_prompt
---
Sei il modulo di ripristino per Easy Launcher. Un'azione automatizzata si è bloccata.

Contesto Esecuzione:
- Ultima azione tentata: clickAtCoordinates(x=900, y=1840) sul pulsante "Invia".
- Stato: Lo schermo non è cambiato dopo 3 tentativi consecutivi.
- Testo OCR Schermo Corrente: ["Attenzione: Permesso richiesto", "Annulla", "Consenti"]

Seleziona la strategia di ripristino appropriata:
1. dismiss_popup: Tocca Annulla/Consenti o premi Back per chiudere un overlay inaspettato.
2. recalibrate_coordinates: Rimappa le coordinate se il layout si è spostato.
3. press_home: Torna alla schermata iniziale per riavviare la procedura in modo sicuro.

Risposta (JSON):
{
  "recoveryStrategy": "dismiss_popup",
  "action": "clickAtCoordinates",
  "coordinates": { "x": 500, "y": 1200 },
  "explanation": "Trovato popup di overlay non previsto. Premuto 'Consenti' per ripristinare il flusso primario."
}
```

---

