# 📋 Changelog — Easy Launcher

Tutte le modifiche rilevanti a questo progetto sono documentate in questo file.
Il formato è basato su [Keep a Changelog](https://keepachangelog.com/it/1.0.0/) e aderisce al [Semantic Versioning](https://semver.org/lang/it/).

---

## [Unreleased]

### In programma (Roadmap)
- Supporto avanzato a modelli On-Device Gemini Nano tramite ML Kit for Android e Gemini Nano e AICore per risposte NLU istantanee a zero latenza.
- Riconoscimento e lettura automatica di ricette mediche e referti tramite ML Kit Digital Ink Recognition & Text Recognition.
- Integrazione promemoria assunzione farmaci con allarmi vocali ad alto volume.

### 🧰 Customizations & Dev Skills (Antigravity Agents)
- Integrazione della suite ufficiale di 12 Android Skills di Google nel workspace (`.agents/skills/`):
  - `ml-kit-genai-prompt-api`: On-device Gemini Nano, Prefix Caching e Structured Output (@Generable).
  - `edge-to-edge`: Supporto schermo intero edge-to-edge per Android 15/16 e gestione insets IME/system bars.
  - `adaptive`: Layout reattivi per tablet, foldables e monitor esterni.
  - `android-cli`: Toolchain da riga di comando per testing, build ed emulazione Android.
  - `appfunctions`: Integrazione AppFunctions per esporre azioni ad assistenti AI on-device.
  - `navigation-event`: Gestione unificata eventi di navigazione e Predictive Back.
  - `navigation-3`: Architettura scene-based e Type-Safe per Jetpack Navigation 3.
  - `android-intent-security`: Hardening e audit di sicurezza per intent, PendingIntent e broadcast sensibili.
  - `camerax`: Pipeline fotocamera e analisi immagini con CameraX.
  - `styles`: Token semantici e design system styling accessibile per Jetpack Compose.
  - `testing-setup`: Infrastruttura di test screenshot (Roborazzi) e unit/UI test.
  - `r8-analyzer`: Analisi e ottimizzazione regole keep e configurazioni ProGuard/R8.

---

## [1.0.0] - 2026-09-08

### 🎉 Release Iniziale — Easy Launcher

Prima versione completa di **Easy Launcher**, il launcher Android accessibile progettato per anziani, persone allettate e utenti con disabilità motorie o visive, sviluppato da **GlitchLab Studio**.

### ✨ Added (Funzionalità Aggiunte)

#### 🖥️ Interfaccia Utente & Accessibilità (Jetpack Compose M3)
- **Schermata Home Tattile ad Alto Contrasto**:
  - Orologio digitale a caratteri giganti con data in italiano e ombreggiatura profonda anti-riflesso.
  - Barra di stato semplificata con indicatore batteria ad alta visibilità e segnale cellulare.
  - Banner riassuntivo notifiche e chiamate perse con badge colorati.
  - **6 Card Tattili Extra-Large**:
    - 📞 **Telefono**: Rubrica rapida con contatti di emergenza e chiamate dirette a un tocco.
    - 💬 **Messaggi / WhatsApp**: Accesso rapido alle conversazioni e alle notifiche in sospeso.
    - 🔦 **Torcia / Luce**: Controllo istantaneo flash con feedback visivo dinamico e animazione luminosa.
    - 📷 **Fotocamera / Foto**: Apertura diretta della fotocamera di sistema.
    - ⚙️ **Impostazioni**: Configurazione semplificata (accessibilità, sintesi vocale, contatti SOS).
    - 🚨 **SOS / Soccorsi**: Attivazione immediata della sequenza di emergenza salvavita.
- **Pulsante Assistente Vocale Fisso**:
  - Pillola inferiore con bordo animato ciano neon (`NeonCyanGlow`) e pulsante microfono da 56dp per un facile accesso motorio.
  - Overlay a tutto schermo con visualizzazione stato in tempo reale (Ascolto, Elaborazione, Esecuzione Tool, Risposta).
  - Feedback sonoro e indicatore di intensità vocale RMS.

#### 🧠 Intelligenza Artificiale & Orchestrazione NLU (Gemini API & Tool Calling)
- **`GeminiAssistantManager`**:
  - Integrazione nativa con **Gemini 2.5 Flash** via Google AI Generative Language API.
  - Motore di **Function Calling / Tool Calling** a turni multipli per tradurre il linguaggio naturale in azioni Android native.
  - **Dichiarazioni Tool Supportate**:
    - `send_sms`: Invio di SMS con destinatario e testo estratti dal comando vocale.
    - `send_whatsapp`: Invio di messaggi WhatsApp diretti.
    - `trigger_emergency_sos`: Attivazione allarme SOS con trasmissione GPS.
    - `toggle_torch`: Accensione e spegnimento del flash LED.
    - `make_phone_call`: Avvio di telefonate verso contatti specifici.
    - `read_notifications`: Lettura vocale delle notifiche recenti.
    - `open_app`: Avvio rapido di qualsiasi applicazione installata.
    - `get_device_status`: Lettura di orario, data, percentuale batteria e stato torcia.
- **Motore Euristico Locale Offline**:
  - Fallback intelligente a regole locali per garantire l'esecuzione dei comandi vitali (SOS, Chiamate, Torcia, SMS, WhatsApp) **anche in totale assenza di connessione internet** o in modalità aereo.

#### 🚨 Sistema di Emergenza SOS Multi-Livello
- **`SosManager` & Sequenza Salvavita**:
  - Conto alla rovescia di 5 secondi annullabile con segnale acustico d'allarme (`ToneGenerator`).
  - Modalità vocale immediata per situazioni di panico o caduta.
- **`SosEmergencyForegroundService`**:
  - Registrazione audio ambientale di emergenza di 10 secondi.
  - Trascrizione vocale contestuale dell'accaduto.
  - Geolocalizzazione ad alta precisione tramite Google Play Services `FusedLocationProviderClient` (`Priority.PRIORITY_HIGH_ACCURACY`).
  - Generazione link Google Maps con coordinate esatte.
  - Spedizione automatica multicanale (SMS e WhatsApp) del messaggio di soccorso con testo, coordinate e registrazione audio ai contatti designati.

#### 👁️ Servizi di Accessibilità & Automazione WhatsApp
- **`WhatsAppNotificationService` (`NotificationListenerService`)**:
  - Intercettazione in tempo reale di notifiche in arrivo (WhatsApp, WhatsApp Business, SMS, Telegram, Telefono).
  - Gestione risposte rapide senza aprire l'applicazione tramite `RemoteInput` inline bundle.
  - Rilevamento automatico e conteggio chiamate perse e messaggi non letti.
- **`WhatsAppAccessibilityFallbackService` (`AccessibilityService`)**:
  - Automazione sicura della UI per l'invio di messaggi WhatsApp quando le API standard o le notifiche dirette non sono disponibili.
  - Riconoscimento dei campi di input e pulsanti di invio tramite View ID, gerarchia nodi e OCR.
  - Supporto per azioni globali di sistema (Home, Indietro, Notifiche) a supporto di utenti con disabilità motorie gravi.

#### 🔊 Audio & Sintesi Vocale
- **`TtsManager`**:
  - Sintesi vocale italiana (Text-To-Speech) configurabile per conferme vocali chiare, rassicuranti e ad alto volume.
  - Lettura vocale automatica di messaggi e notifiche.
- **Speech Recognition Android**:
  - Riconoscimento vocale continuo e reattivo con gestione permessi dinamica in Jetpack Compose.

#### ⚙️ CI/CD & Build Automation
- **GitHub Actions Workflow (`.github/workflows/build-apk.yml`)**:
  - Compilazione automatica APK di debug su push sul branch `main` e trigger manuale `workflow_dispatch`.
  - Ambiente Ubuntu con Java 17 Temurin, Gradle 9.3.1 e Android Gradle Plugin 9.1.1.
  - Generazione automatica keystore di debug per packaging APK consistente.
  - Archiviazione artefatto `app-debug-apk`.
  - Pubblicazione automatica della release GitHub tramite `gh release create` con tag univoco per download immediato su smartphone.

---

## [0.1.0] - 2026-09-07

### 🏗️ Setup Iniziale e Prototipazione
- Creazione della struttura di base del progetto Android con Gradle Kotlin DSL (`build.gradle.kts`).
- Configurazione del Version Catalog (`gradle/libs.versions.toml`) con AGP 9.1.1, Kotlin 2.2.10 e Jetpack Compose BOM.
- Implementazione del tema accessibile ad alto contrasto (`Color.kt`, `Theme.kt`, `Type.kt`).
- Impostazione dei permessi di sistema in `AndroidManifest.xml` (`RECORD_AUDIO`, `CALL_PHONE`, `SEND_SMS`, `ACCESS_FINE_LOCATION`, `CAMERA`, `BIND_ACCESSIBILITY_SERVICE`, `BIND_NOTIFICATION_LISTENER_SERVICE`).
