Searched for "Core Services & API Integrations"
Listed directory docs
Viewed Core%20System%20Architecture%20&%20%20Edge-first%20System%20Design%20Strategy.md:1-69
Viewed CHANGELOG.md:1-103
Listed directory main
Listed directory java
Searched for "package "
Viewed build.gradle.kts:1-141
Viewed GeminiAssistantManager.kt:1-451
Viewed VoiceInputComponent.kt:1-172
Viewed WhatsAppAccessibilityFallbackService.kt:1-202
Viewed WhatsAppNotificationService.kt:1-148
Viewed SosEmergencyForegroundService.kt:1-512
Viewed NativeActionExecutor.kt:1-247
Viewed libs.versions.toml:1-110

Ho effettuato una revisione approfondita dell'implementazione attuale dell'applicazione rispetto alla sezione **`### Core Services & API Integrations`** del documento [Core System Architecture & Edge-first System Design Strategy.md](file:///c:/Users/mitch/antigravity/Easy-Launcher/docs/Core%20System%20Architecture%20&%20%20Edge-first%20System%20Design%20Strategy.md#L5-L35), incrociando i dettagli del [CHANGELOG.md](file:///c:/Users/mitch/antigravity/Easy-Launcher/CHANGELOG.md) e il codice sorgente in `app/`.

Di seguito trovi l'analisi dettagliata, lo stato di aderenza e il gap report.

---

## 📊 Sintesi dello Stato di Allineamento

| Pilastro Architetturale | Stato Attuale nel Codice | Aderenza al Documento |
| :--- | :--- | :---: |
| **1. On-Device NLU & Intent Engine** | Cloud `gemini-2.5-flash` via HTTP ([GeminiAssistantManager.kt](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/ai/GeminiAssistantManager.kt)) + Fallback euristico statico a regole regex. | 🟡 **30%** (Funziona, ma è Cloud/Heuristic anziché On-Device Prompt API/AICore) |
| **2. Local Perception & NLP (ML Kit)** | Android framework standard ([SpeechRecognizer](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/VoiceInputComponent.kt)). Assenti OCR, Entity Extraction, Language ID e Smart Reply. | 🔴 **15%** (Framework Android standard, librerie ML Kit non ancora collegate) |
| **3. UI Automation & System Access** | [WhatsAppAccessibilityFallbackService](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/WhatsAppAccessibilityFallbackService.kt), [WhatsAppNotificationService](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/notifications/WhatsAppNotificationService.kt), [SosEmergencyForegroundService](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/SosEmergencyForegroundService.kt), [TtsManager](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/accessibility/TtsManager.kt). | 🟢 **90%** (Quasi completamente aderente alle specifiche native) |

---

## 🔍 Analisi di Dettaglio per Componente

### 1. On-Device NLU & Intent Orchestration Engine

*Documento Architetturale:*
- **Google AI Edge SDK & AICore (Gemini Nano)** per esecuzione locale su hardware NPU/GPU senza trasmissione dati esterna.
- **Prompt API con Prefix Caching** per ridurre la latenza Time-To-First-Token (TTFT).
- **Structured Output API (`generateTypedContent`)** con classi Kotlin `@Generable`.
- **LiteRT-LM** come runtime di fallback per Gemma 270M.

*Stato Attuale della Codebase:*
1. **Chiamate Cloud anziché On-Device:** In [GeminiAssistantManager.kt:L20-L28](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/ai/GeminiAssistantManager.kt#L20-L28), l'app effettua chiamate REST via `OkHttpClient` all'endpoint cloud `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`.
2. **Nessun Prefix Caching o Prompt API:** Il payload JSON viene costruito manualmente con `buildToolsDeclaration()` ad ogni richiesta.
3. **Structured Output assente:** L'estrazione dell'intento avviene tramite parsing manuale di oggetti JSON (`org.json.JSONObject`) sia per il Function Calling di Gemini sia nelle regole euristiche.
4. **Fallback Locale:** In caso di assenza di connessione o errore API, l'app passa a `processLocalIntent()` ([GeminiAssistantManager.kt:L346-L434](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/ai/GeminiAssistantManager.kt#L346-L434)), un parser euristico basato su parole chiave (`contains("sos")`, `contains("torcia")`), efficace ma limitato rispetto a un modello SLM/LiteRT.

---

### 2. Local Perception & NLP Processing (ML Kit Suite)

*Documento Architetturale:*
- **ML Kit Speech Recognition API**: Trascrizione offline a bassa latenza.
- **ML Kit Text Recognition (OCR)**: Fallback visivo quando la gerarchia dei nodi accessibilità è opaca.
- **ML Kit Entity Extraction API**: Estrazione automatica di numeri, indirizzi e date dalle notifiche.
- **ML Kit Language Identification API**: Adattamento dinamico della lingua del `TextToSpeech`.
- **ML Kit Smart Reply API**: Generazione di chip di risposta rapida ad alto contrasto.

*Stato Attuale della Codebase:*
1. **Speech Recognition**: Sia in [VoiceInputComponent.kt:L61-L102](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/VoiceInputComponent.kt#L61-L102) che in [SosEmergencyForegroundService.kt:L230-L276](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/SosEmergencyForegroundService.kt#L230-L276) viene impiegato il componente nativo standard `android.speech.SpeechRecognizer`. Questo dipende dai servizi Google installati nel sistema operativo anziché dal bundle ML Kit on-device isolato.
2. **Dipendenze ML Kit assenti nel Catalog**: In [gradle/libs.versions.toml](file:///c:/Users/mitch/antigravity/Easy-Launcher/gradle/libs.versions.toml) e [app/build.gradle.kts](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/build.gradle.kts) non sono ancora configurate le librerie ML Kit (`com.google.mlkit:text-recognition`, `entity-extraction`, `language-id`, `smart-reply`, né `com.google.mlkit:genai-prompt`).
3. **Mancanza di OCR Fallback nell'Accessibilità**: [WhatsAppAccessibilityFallbackService.kt:L114-L151](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/WhatsAppAccessibilityFallbackService.kt#L114-L151) cerca i nodi solo tramite `findAccessibilityNodeInfosByViewId` e navigazione ricorsiva dei nodi; non cattura bitmap dello schermo (`takeScreenshot`) né invoca ML Kit OCR se la gerarchia fallisce.

---

### 3. System Access & UI Automation Infrastructure

*Documento Architetturale:*
- Custom `AccessibilityService` (`dispatchGesture`, azioni globali).
- `NotificationListenerService` & `RemoteInput` API per risposte inline dirette.
- Emergency `ForegroundService` (GPS con `FusedLocationProviderClient`, `SmsManager`, dialer).
- Native `TextToSpeech` (TTS) Engine.

*Stato Attuale della Codebase:*
1. **`WhatsAppNotificationService`**: Pienamente funzionante ([WhatsAppNotificationService.kt:L56-L75](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/notifications/WhatsAppNotificationService.kt#L56-L75)). Intercetta le notifiche e usa `RemoteInput.addResultsToIntent()` inviando la risposta inline senza aprire WhatsApp.
2. **`WhatsAppAccessibilityFallbackService`**: Implementato correttamente con `performGlobalAction(GLOBAL_ACTION_HOME)`, deep link e immissione testo `ACTION_SET_TEXT`. Manca solo il passaggio 5 del workflow (OCR bounding box + `dispatchGesture`).
3. **`SosEmergencyForegroundService`**: Molto solido e aderente ([SosEmergencyForegroundService.kt:L137-L190](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/SosEmergencyForegroundService.kt#L137-L190)): registrazione audio 10s, trascrizione, coordinate GPS ad alta precisione con `FusedLocationProviderClient`, invio multicanale SMS + WhatsApp audio/testo.
4. **`NativeActionExecutor` & `TtsManager`**: Integrati e pronti per essere pilotati da qualsiasi orchestratore di intenti.

---

## 📋 Coerenza con il `CHANGELOG.md`

Il [CHANGELOG.md](file:///c:/Users/mitch/antigravity/Easy-Launcher/CHANGELOG.md) riflette fedelmente lo stato di transizione del progetto da **Google AI Studio** a launcher Android avanzato:

1. **Release `[1.0.0]`**: Ha formalizzato le componenti UI (Jetpack Compose), l'automazione dei servizi di sistema (WhatsApp, NotificationListener, SOS Foreground Service) e l'integrazione con **Gemini 2.5 Flash Cloud via Tool Calling** + regole locali offline.
2. **Sezione `[Unreleased]`**: Identifica esattamente i punti emersi da questa review:
   - *"Supporto avanzato a modelli On-Device Gemini Nano tramite AICore per risposte NLU istantanee a zero latenza"*
   - *"Riconoscimento e lettura automatica di ricette mediche e referti tramite ML Kit Digital Ink Recognition & Text Recognition"*

---

## 🎯 Prossimi Passi Consigliati

Ora che abbiamo installato e configurato la skill [`ml-kit-genai-prompt-api`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/ml-kit-genai-prompt-api/SKILL.md), possiamo procedere per fasi:

1. **Integrazione ML Kit GenAI Prompt API:**
   - Aggiungere `com.google.mlkit:genai-prompt` a [libs.versions.toml](file:///c:/Users/mitch/antigravity/Easy-Launcher/gradle/libs.versions.toml) e [app/build.gradle.kts](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/build.gradle.kts).
   - Creare un `OnDevicePromptClient` che implementi la Prompt API su Gemini Nano locale.
   - Introdurre **Prefix Caching** per il system prompt dei comandi launcher (telefona, invia SMS, apri app, torcia, SOS) per raggiungere tempi di risposta inferiori a 100ms.
   - Sfruttare lo **Structured Output** (`@Generable` o schema JSON vincolato) per deserializzare direttamente in una classe `LauncherAction` tipizzata.
2. **Hybrid Orchestrator (Nano ➔ Cloud ➔ Heuristics):**
   - Eseguire prima su Gemini Nano locale tramite Prompt API.
   - Se il dispositivo non supporta AICore o la richiesta richiede conoscenze esterne, fare fallback su Cloud `gemini-2.5-flash`.
   - Se manca la rete, usare il fallback euristico `processLocalIntent` già collaudato.
3. **Aggiunta del supporto ML Kit Vision & NLP:**
   - Aggiungere ML Kit Text Recognition per completare l'OCR Fallback in [WhatsAppAccessibilityFallbackService.kt](file:///c:/Users/mitch/antigravity/Easy-Launcher/app/src/main/java/com/example/services/WhatsAppAccessibilityFallbackService.kt).

