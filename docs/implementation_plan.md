# 🚀 Comprehensive Implementation Plan: Edge-First System Architecture

> **Riferimento Architetturale Ufficiale:** [Core System Architecture & Edge-first System Design Strategy.md](file:///c:/Users/mitch/antigravity/Easy-Launcher/docs/Core%20System%20Architecture%20&%20%20Edge-first%20System%20Design%20Strategy.md)  
> **Obiettivo:** Realizzare un'architettura **completamente edge-first e offline-primary**, eliminando le dipendenze da rete per tutte le operazioni vitali (NLU, sintesi vocale, trascrizione, automazione UI, emergenza SOS) e garantendo esecuzione deterministica con latenza sub-100ms.

---

## 🏛️ Matrice di Copertura Architetturale

Questo piano implementa fedelmente **tutte le direttive, le API e i 4 workflow end-to-end** specificati nel documento architetturale:

| Componente del Documento Architetturale | Soluzione Tecnica nel Piano | Stato |
| :--- | :--- | :---: |
| **1. Google AI Edge SDK & AICore (Gemini Nano)** | `OnDevicePromptClient` con ML Kit GenAI Prompt API | ⏳ Fase 1 |
| **2. Prompt API con Prefix Caching** | Cache locale dello stato statico di sistema (schemi tool launcher) | ⏳ Fase 1 |
| **3. Structured Output (`generateTypedContent`)** | Modelli Kotlin `@Generable` senza parsing JSON manuale | ⏳ Fase 1 |
| **4. LiteRT-LM Execution Engine (Gemma 270M)** | Runtime on-device CPU/GPU per dispositivi privi di AICore nativo | ⏳ Fase 2 |
| **5. ML Kit Speech Recognition API (Offline)** | Trascrizione audio on-device isolata dal framework OS | ⏳ Fase 3 |
| **6. ML Kit Text Recognition API (Spatial OCR Fallback)** | `MlKitAccessibilityBridge` + coordinate bounding box + `dispatchGesture()` | ⏳ Fase 4 |
| **7. ML Kit Entity Extraction API** | Parsing locale di date, indirizzi e numeri di telefono dalle notifiche | ⏳ Fase 5 |
| **8. ML Kit Language Identification API** | Rilevamento lingua notifiche per aggiornamento dinamico del `Locale` TTS | ⏳ Fase 5 |
| **9. ML Kit Smart Reply API** | Generazione chip di risposta rapida ad alto contrasto nella UI | ⏳ Fase 5 |
| **10. UI Automation & Accessibility Gesture Injection** | `AccessibilityService` con navigazione gerarchica + coordinate touch | ⏳ Fase 4 |
| **11. NotificationListenerService & RemoteInput** | Pipeline intercettazione notifiche ed invio inline senza aprire le app | ⏳ Fase 5 |
| **12. High-Priority Emergency ForegroundService** | SOS con bypass rete, `FusedLocationProviderClient`, SMS ed `ACTION_CALL` su viva voce | ⏳ Fase 6 |
| **13. Native TextToSpeech (TTS) & AudioManager** | Feedback sonoro multilingua e controllo hardware del volume | ⏳ Fase 6 |

---

## ⚠️ Note Tecniche & Strategie di Raffinamento Architetturale

### 1. Strategia di Graceful Degradation a 4 Livelli (Hardware Compatibility)
Poiché la fascia target include dispositivi non-flagship o hardware datato utilizzato da utenti anziani, **AICore / Gemini Nano** potrebbe non essere presente. L'architettura adotta fin dalla progettazione il pattern `HybridOrchestrator` con una catena di fallback deterministica:

$$\text{Gemini Nano (AICore)} \longrightarrow \text{LiteRT (Gemma 270M)} \longrightarrow \text{Gemini Cloud 2.5 Flash} \longrightarrow \text{Regex Local Heuristic}$$

- **Livello 1 (AICore / Gemini Nano):** Inferenza locale su NPU hardware, latenza sub-100ms, zero banda consumata.
- **Livello 2 (LiteRT Gemma 270M On-Device):** Fallback CPU/GPU quantizzato on-device quando AICore è assente sul sistema operativo.
- **Livello 3 (Gemini 2.5 Flash Cloud):** Fallback cloud abilitato se la connettività è presente e per richieste non di sopravvivenza.
- **Livello 4 (Regex Local Heuristic):** Paracadute deterministico offline a zero risorse (`processLocalIntent`), collaudato in v1.0.0, attivo al 100% per azioni critiche (SOS, Chiamate, Torcia, SMS) anche a batteria critica o in modalità aereo.

### 2. Gestione del Footprint di Memoria e Lazy Loading On-Demand
L'allocazione simultanea di tutti i modelli ML Kit (Speech, OCR, Entity Extraction, Language ID, Smart Reply) causerebbe picchi di RAM insostenibili su smartphone con 2GB - 3GB di memoria:
- **Inizializzazione On-Demand (Lazy):** 
  - `TextRecognizer` (OCR) viene instanziato ed allocato **esclusivamente** se e quando la ricerca gerarchica nell'`AccessibilityNodeInfo` fallisce, e rilasciato immediatamente con `close()` al termine del tocco simulato.
  - `LanguageIdentifier` ed `EntityExtractor` vengono allocati solo durante il ciclo di vita del parsing di una nuova notifica in arrivo.
  - `SmartReply` viene liberato dalla memoria dopo la generazione dei chip.
- **Lifecycle Scoping:** Tutti i client ML Kit implementano `AutoCloseable` o vengono rilasciati tramite scoping coroutine `Dispatchers.Default` con pulizia esplicita nel ciclo di vita.

### 3. Matrice di Compatibilità Toolchain (AGP 9.1.1, Kotlin 2.2.10, KSP 2.3.6)
- Il Version Catalog include già **AGP 9.1.1** e **Kotlin 2.2.10**.
- La versione KSP configurata (`2.3.5` / `2.3.6`) e il plugin KSP devono essere allineati con precisione alla versione di Kotlin runtime (`2.2.10-X.X`).
- La libreria `mlkit-genai-prompt:1.0.0-beta4` e il processore di annotazioni `genai-schema-compiler` devono essere convalidati preventivamente in Fase 1 per scongiurare discrepanze di byte-code con il compilatore Kotlin Compose di AGP 9.

---

## 📅 Piano Operativo Dettagliato per Fasi

---

### 📦 FASE 1: Setup Toolchain, KSP & On-Device NLU (Gemini Nano)
*Direttiva Architetturale:* Sezione 1 — *Google AI Edge SDK, Prompt API con Prefix Caching, Structured Output*  
*Skills correlate:* [`ml-kit-genai-prompt-api`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/ml-kit-genai-prompt-api/SKILL.md), [`r8-analyzer`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/r8-analyzer/SKILL.md)

1. **Setup Dipendenze Gradle & Validazione KSP:**
   - Verificare l'allineamento tra KSP (`2.3.6`) e Kotlin (`2.2.10`).
   - In `gradle/libs.versions.toml`:
     ```toml
     [versions]
     mlkitGenaiPrompt = "1.0.0-beta4"
     googleDevtoolsKsp = "2.3.6"

     [libraries]
     mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenaiPrompt" }
     mlkit-genai-schema-compiler = { group = "com.google.mlkit", name = "genai-schema-compiler", version.ref = "mlkitGenaiPrompt" }
     ```
   - In `app/build.gradle.kts`:
     - Impostare `minSdk = 26` (requisito essenziale per AICore / ML Kit GenAI).
     - Aggiungere `implementation(libs.mlkit.genai.prompt)` e `"ksp"(libs.mlkit.genai.schema.compiler)`.

2. **Definizione Azioni Tipizzate `@Generable` (`LauncherAction.kt`):**
   - Eliminare l'uso di stringhe JSON. Creare `app/src/main/java/com/example/ai/model/LauncherAction.kt`:
     ```kotlin
     @Generable
     data class LauncherActionCommand(
         val actionType: ActionType,
         val recipient: String? = null,
         val phoneNumber: String? = null,
         val messageText: String? = null,
         val appName: String? = null,
         val volumeLevel: Int? = null, // Supporto AudioManager
         val torchState: Boolean? = null,
         val spokenFeedback: String
     )

     enum class ActionType {
         SEND_SMS,
         SEND_WHATSAPP,
         MAKE_PHONE_CALL,
         TRIGGER_EMERGENCY_SOS,
         TOGGLE_TORCH,
         ADJUST_VOLUME,
         READ_NOTIFICATIONS,
         OPEN_APP,
         GET_DEVICE_STATUS,
         CONVERSATIONAL_RESPONSE
     }
     ```

3. **Client Prompt On-Device con Prefix Caching (`OnDevicePromptClient.kt`):**
   - Inizializzare la sessione con system instruction statica pre-compilata.
   - Sfruttare il **Prefix Caching** di intermediate states per abbattere il Time-To-First-Token (TTFT < 100ms) durante l'elaborazione dei comandi vocali.
   - Utilizzare `generateTypedContent<LauncherActionCommand>()`.

---

### 🧠 FASE 2: LiteRT-LM Fallback Engine & HybridOrchestrator a 4 Livelli
*Direttiva Architetturale:* Sezione 1 — *LiteRT-LM Execution Engine*

1. **Integrazione Runtime LiteRT-LM:**
   - Per dispositivi Android che non dispongono dell'AICore di sistema o di Gemini Nano integrato, configurare il runtime **LiteRT (precedentemente TensorFlow Lite / Google AI Edge LiteRT)**.
   - Caricare un modello compatto quantizzato (es. **Gemma 270M** fine-tuned per intent mapping).
2. **Streaming Token Output via Kotlin Flow:**
   - Implementare `LiteRtIntentEngine.kt` che elabora il prompt ed emette tokens e comandi tramite `Flow<LauncherActionCommand>`, garantendo continuità offline totale.
3. **Formalizzazione `HybridOrchestrator`:**
   - Implementare la pipeline completa a 4 stadi:
     1. Prova **Gemini Nano** (`OnDevicePromptClient.isAvailable()`).
     2. Fallback su **LiteRT Gemma 270M** se AICore non supportato.
     3. Fallback su **Gemini 2.5 Flash Cloud** se presente connessione internet.
     4. Fallback su **Heuristic Regex** (`processLocalIntent`) come garanzia deterministica per SOS, torcia e chiamate.

---

### 🎙️ FASE 3: ML Kit Speech Recognition Offline
*Direttiva Architetturale:* Sezione 2 — *ML Kit Speech Recognition API*  
*Workflow di Riferimento:* Workflow 1 — *Audio Ingestion*

1. **Migrazione da standard `SpeechRecognizer` ad Audio Ingestion Offline:**
   - Rimpiazzare la dipendenza dai Google Play Services online in `VoiceInputComponent.kt` e `SosEmergencyForegroundService.kt`.
   - Utilizzare il modello di trascrizione vocale on-device di **ML Kit Speech Recognition / LoRA local adapter**.
   - Garantire trascrizione vocale immediata in italiano anche con telefono in modalità aereo o con SIM assente.

---

### 👁️ FASE 4: Accessibilità con Spatial OCR Fallback On-Demand (MlKitAccessibilityBridge)
*Direttiva Architetturale:* Sezione 2 & 3 — *ML Kit Text Recognition API (OCR) & Custom AccessibilityService*  
*Workflow di Riferimento:* Workflow 3 — *Accessibility Automation & Visual OCR Fallback*  
*Skill correlata:* [`camerax`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/camerax/SKILL.md)

1. **Aggiunta ML Kit Text Recognition:**
   - Aggiungere `com.google.android.gms:play-services-mlkit-text-recognition`.
2. **Creazione `MlKitAccessibilityBridge.kt` con Lazy Allocation:**
   - Collegare `WhatsAppAccessibilityFallbackService.kt`:
     - **Passo 1 (Node Tree):** Ricerca nodi per `ViewId` (`com.whatsapp:id/entry`, `send`).
     - **Passo 2 (Standard Action):** Esecuzione `ACTION_SET_TEXT` ed `ACTION_CLICK`. Se ha successo, nessun modello OCR viene caricato.
     - **Passo 3 (OCR Fallback):** Solo se i nodi non vengono trovati, invocare `takeScreenshot()`.
     - **Passo 4 (Spatial OCR On-Demand):** Inizializzare `TextRecognizer` sul momento, analizzare il bitmap, individuare i bounding box di "Invia" / "Send", calcolare il centroide $(X, Y)$ e chiamare `dispatchGesture()`.
     - **Passo 5 (Memory Release):** Invocare `recognizer.close()` per liberare istantaneamente la RAM.

---

### 💬 FASE 5: NLP Locale Notifiche con Lazy Loading (Entity Extraction, Language ID, Smart Reply)
*Direttiva Architetturale:* Sezione 2 & 3 — *Entity Extraction, Language ID, Smart Reply, NotificationListenerService*  
*Workflow di Riferimento:* Workflow 2 — *Notification Interception & Inline Reply*

1. **Aggiornamento `WhatsAppNotificationService.kt` con Pipeline ML Kit Scoped:**
   - **ML Kit Language Identification (Lazy):**
     - Identificare la lingua del messaggio ricevuto (`identifyLanguage(messageText)`).
     - Aggiornare istantaneamente il `Locale` del `TtsManager` per consentire una pronuncia corretta in italiano, inglese, spagnolo, ecc. Rilasciare il client.
   - **ML Kit Entity Extraction (Lazy):**
     - Estrarre entità locali: numeri telefonici, indirizzi civici, date/orari solo su payload testuali validi.
     - Esporre card di azione immediata ("Chiama numero", "Apri mappa") nella UI del launcher. Rilasciare il modello.
   - **ML Kit Smart Reply (On-Demand):**
     - Generare 3 chip di risposta rapida offline con palette ad alto contrasto prima che l'utente attivi la voce.
   - **Inline Reply via RemoteInput:**
     - Inviare la risposta dettata o scelta via chip tramite `RemoteInput.addResultsToIntent()` senza aprire l'applicazione target.

---

### 🚨 FASE 6: Hardening Emergenza SOS, Viva Voce & AudioManager
*Direttiva Architetturale:* Sezione 3 — *Emergency ForegroundService, Native TTS & Audio Routing*  
*Workflow di Riferimento:* Workflow 1 & Workflow 4 — *Emergency SOS & Voice Execution*  
*Skill correlata:* [`android-intent-security`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/android-intent-security/SKILL.md)

1. **Potenziamento `SosEmergencyForegroundService.kt`:**
   - **Bypass Assoluto:** Ignorare controlli di connettività di rete.
   - **Acquisizione Posizione GPS:** `FusedLocationProviderClient` (`PRIORITY_HIGH_ACCURACY`).
   - **SMS Multiplo Automatico:** Invio SMS programmatico con coordinate Maps a tutti i contatti configurati.
   - **Telefonia con Speakerphone Diretto:**
     - Avviare `Intent.ACTION_CALL` verso il caregiver principale.
     - Impostare automaticamente l'audio routing su **viva voce** tramite `AudioManager.mode = MODE_IN_CALL` e `isSpeakerphoneOn = true`.
2. **Supporto Hardware Volume in `NativeActionExecutor.kt`:**
   - Integrare la gestione del volume dispositivo (`ActionType.ADJUST_VOLUME`) tramite `AudioManager.setStreamVolume()`.

---

### 🎨 FASE 7: Edge-to-Edge & Adattabilità Layout
*Skills correlate:* [`edge-to-edge`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/edge-to-edge/SKILL.md), [`adaptive`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/adaptive/SKILL.md), [`styles`](file:///c:/Users/mitch/antigravity/Easy-Launcher/.agents/skills/styles/SKILL.md)

1. **Edge-to-Edge su Android 15/16:**
   - Applicare `enableEdgeToEdge()` in `MainActivity.kt`.
   - Gestire `WindowInsets` ed insets tastiera IME per non coprire le card tattili e l'overlay vocale.
2. **Adaptive Layouts:**
   - Adattare le 6 card tattili e i dialoghi per schermi grandi, tablet e dispositivi pieghevoli per garantire target motori da almeno 48dp-96dp.

---

## 🔄 Verifica dei 4 Workflow End-to-End

Al termine dell'implementazione, verranno collaudati esattamente i 4 workflow del documento architetturale:

1. **Workflow 1 (Offline Voice Command):**
   - Utente parla -> ML Kit Speech locale -> `HybridOrchestrator` (Nano / LiteRT Gemma) con Prefix Caching -> `LauncherActionCommand` tipizzato -> Esecuzione nativa (`NativeActionExecutor`) -> Audio feedback TTS.
2. **Workflow 2 (Notification & Inline Reply):**
   - Notifica intercettata -> Language ID regola TTS -> Entity Extraction estrae numeri/date -> Smart Reply crea chip -> TTS legge il testo -> Risposta via `RemoteInput`.
3. **Workflow 3 (Accessibility & Spatial OCR Fallback):**
   - Apertura chat WhatsApp -> Ricerca albero nodi -> Se fallisce: Screenshot -> `MlKitAccessibilityBridge` (caricato on-demand) trova coordinate "Invia" -> `dispatchGesture()` tocca il punto -> rilascio memoria OCR -> ritorno a Home.
4. **Workflow 4 (Emergency SOS Routine):**
   - Trigger SOS -> Bypass connettività -> Foreground Service attivo -> GPS Fix -> Invio SMS di soccorso -> Chiamata automatica con viva voce attivo.
