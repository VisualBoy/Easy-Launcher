package com.example.ai

data class RecoveryAction(
    val action: String,
    val params: Map<String, Any?> = emptyMap(),
    val description: String
)

/**
 * Diagnostic failure recovery engine ported from private-agent.
 * Analyzes screen content and failed action context to suggest intelligent unsticking actions.
 */
class RecoveryEngine {

    /**
     * Diagnoses the failure reason and suggests an automated recovery action.
     */
    fun diagnose(lastFailedAction: String, screenContent: String): RecoveryAction {
        val lowerScreen = screenContent.lowercase()

        // 1. Loading/spinner detected -> Wait for UI stabilization
        if (lowerScreen.contains("loading") || lowerScreen.contains("caricamento") ||
            lowerScreen.contains("progress") || lowerScreen.contains("attendere") ||
            lowerScreen.contains("spinner") || lowerScreen.contains("wait")
        ) {
            return RecoveryAction(
                action = "wait",
                params = mapOf("durationMs" to 1500),
                description = "L'app sembra in caricamento, attendo completamento..."
            )
        }

        // 2. Soft keyboard is covering elements -> Press Back to dismiss
        if (lowerScreen.contains("gboard") || lowerScreen.contains("keyboard") || lowerScreen.contains("tastiera")) {
            return RecoveryAction(
                action = "press_back",
                params = emptyMap(),
                description = "La tastiera sembra coprire lo schermo, premo Indietro per chiuderla."
            )
        }

        // 3. Last action was a click/tap that failed
        if (lastFailedAction == "click_text" || lastFailedAction == "click_at") {
            return if (lowerScreen.contains("scroll")) {
                RecoveryAction(
                    action = "scroll",
                    params = mapOf("direction" to "down"),
                    description = "Tocco non riuscito: scorro verso il basso per trovare il target."
                )
            } else {
                RecoveryAction(
                    action = "press_back",
                    params = emptyMap(),
                    description = "Tocco non riuscito e schermo non scorrevole: torno indietro per ritentare."
                )
            }
        }

        // 4. Failed to open target application
        if (lastFailedAction == "open_app") {
            return RecoveryAction(
                action = "press_home",
                params = emptyMap(),
                description = "Apertura app fallita: ritorno alla schermata Home per riprovare."
            )
        }

        // Generic fallback: dismiss current dialog or overlay
        return RecoveryAction(
            action = "press_back",
            params = emptyMap(),
            description = "Stato bloccato sconosciuto: premo Indietro per ripristinare il contesto."
        )
    }
}
