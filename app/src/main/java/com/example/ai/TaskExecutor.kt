package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.accessibility.ScreenDumpService
import com.example.model.ActionStep
import com.example.model.SavedSkill
import com.example.services.AccessibilityFallbackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Multi-step UI Automation Coroutine Loop Executor ported from private-agent.
 * Orchestrates screen understanding, skill memory caching (0-token replays),
 * adaptive wait delays, gesture execution, and diagnostic recovery.
 */
class TaskExecutor(
    private val context: Context,
    private val onProgress: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "TaskExecutor"
        private const val MAX_STEPS = 10
    }

    val skillMemory = SkillMemoryService(context)
    val historyLogger = TaskHistoryLogger(context)
    val recoveryEngine = RecoveryEngine()

    @Volatile
    private var isCancelled = false

    fun cancel() {
        isCancelled = true
        report("Automazione annullata dall'utente.")
    }

    private fun report(message: String) {
        Log.d(TAG, message)
        onProgress?.invoke(message)
    }

    /**
     * Executes an end-to-end task goal with fast skill memory replay and AI fallback.
     */
    suspend fun executeTask(userGoal: String): String = withContext(Dispatchers.Default) {
        isCancelled = false
        val trace = mutableListOf<String>()
        val executedSteps = mutableListOf<ActionStep>()
        var totalTokens = 0

        trace.add("Inizio task: $userGoal")
        report("Inizio task: $userGoal")

        val service = AccessibilityFallbackService.instance
        if (service == null) {
            val errorMsg = "Servizio di accessibilità non attivo. Abilitalo nelle Impostazioni."
            report(errorMsg)
            return@withContext errorMsg
        }

        // ─── STEP 1: Skill Memory Fast Path (0 Tokens) ───
        val savedSkill = skillMemory.findSkill(userGoal)
        if (savedSkill != null && savedSkill.isReliable) {
            report("Trovata skill memorizzata! Eseguo ${savedSkill.steps.size} azioni a 0 token...")
            val replaySuccess = replaySkill(service, savedSkill, trace)
            if (replaySuccess) {
                trace.add("Task completato tramite Skill Memory.")
                historyLogger.logTask(
                    userGoal,
                    "Success (via memory)",
                    0,
                    savedSkill.steps.size,
                    trace
                )
                return@withContext "Operazione completata con successo tramite memoria rapida."
            } else {
                report("Riesecuzione skill fallita, procedo con l'orchestrazione AI...")
                skillMemory.recordFailure(savedSkill.id)
            }
        }

        // ─── STEP 2: Multi-step Automation Loop ───
        var consecutiveFailures = 0
        var lastAction = ""
        var sameActionCount = 0

        for (stepIndex in 0 until MAX_STEPS) {
            if (isCancelled) break

            // Adaptive delay to allow apps to stabilize
            val delayMs = getAdaptiveDelay(lastAction)
            delay(delayMs)

            // Read active screen
            val screenElements = ScreenDumpService.dumpScreen(service)
            val screenDescription = ScreenDumpService.getCompressedScreenDescription(screenElements, userGoal)
            trace.add("Step ${stepIndex + 1}: Letto schermo con ${screenElements.size} elementi.")

            // Build action decision (simulated or LLM parsed)
            val decision = decideNextAction(userGoal, screenDescription, stepIndex, screenElements)
            val actionName = decision.action
            val params = decision.params

            report("Step ${stepIndex + 1}: Azione -> $actionName")

            // Stuck loop detector
            sameActionCount = if (actionName == lastAction) sameActionCount + 1 else 1
            if (sameActionCount > 2 && actionName != "scroll" && actionName != "wait") {
                trace.add("Rilevato loop ripetuto per $actionName. Avvio recovery...")
                consecutiveFailures = 3
                lastAction = actionName
            }
            lastAction = actionName

            // Execute action
            val actionSuccess = executeActionStep(service, decision)
            if (actionSuccess) {
                consecutiveFailures = 0
                executedSteps.add(decision)
                trace.add("Eseguito con successo: $actionName")
            } else {
                consecutiveFailures++
                trace.add("Fallita esecuzione: $actionName")
            }

            if (actionName == "done") {
                trace.add("Task completato con successo.")
                break
            }

            // Recovery check
            if (consecutiveFailures >= 3) {
                val recovery = recoveryEngine.diagnose(actionName, screenDescription)
                report("Recovery attiva: ${recovery.description}")
                trace.add("Recovery: ${recovery.action}")
                executeActionStep(service, ActionStep(recovery.action, recovery.params))
                consecutiveFailures = 0
            }
        }

        val finalStatus = if (isCancelled) "Cancelled" else "Success"
        historyLogger.logTask(
            userGoal,
            finalStatus,
            totalTokens,
            executedSteps.size,
            trace
        )

        if (finalStatus == "Success" && executedSteps.isNotEmpty()) {
            skillMemory.saveSkill(userGoal, executedSteps)
        }

        return@withContext trace.joinToString("\n")
    }

    private suspend fun replaySkill(
        service: AccessibilityFallbackService,
        skill: SavedSkill,
        trace: MutableList<String>
    ): Boolean {
        for ((idx, step) in skill.steps.withIndex()) {
            if (isCancelled) return false
            delay(getAdaptiveDelay(step.action))
            val success = executeActionStep(service, step)
            trace.add("Replay step ${idx + 1} (${step.action}): ${if (success) "OK" else "FAIL"}")
            if (!success && step.action != "wait") {
                return false
            }
        }
        return true
    }

    private fun executeActionStep(
        service: AccessibilityFallbackService,
        step: ActionStep
    ): Boolean {
        val gc = service.gestureController
        return when (step.action) {
            "click_at" -> {
                val x = (step.params["x"] as? Number)?.toFloat() ?: 0f
                val y = (step.params["y"] as? Number)?.toFloat() ?: 0f
                gc.clickAtCoordinates(x, y)
            }
            "click_text" -> {
                val text = step.params["text"] as? String ?: ""
                gc.clickByText(text)
            }
            "type_text" -> {
                val text = step.params["text"] as? String ?: ""
                val hint = step.params["field_hint"] as? String
                gc.typeText(text, hint)
            }
            "press_enter" -> gc.pressEnter()
            "scroll" -> {
                val direction = step.params["direction"] as? String ?: "down"
                val targetText = step.params["targetText"] as? String
                gc.scroll(direction, targetText)
            }
            "swipe" -> {
                val startX = (step.params["startX"] as? Number)?.toFloat() ?: 0f
                val startY = (step.params["startY"] as? Number)?.toFloat() ?: 0f
                val endX = (step.params["endX"] as? Number)?.toFloat() ?: 0f
                val endY = (step.params["endY"] as? Number)?.toFloat() ?: 0f
                gc.swipe(startX, startY, endX, endY)
            }
            "press_back" -> gc.pressBack()
            "press_home" -> gc.pressHome()
            "open_app" -> {
                val appName = step.params["app_name"] as? String ?: ""
                openAppByName(appName)
            }
            "wait" -> true
            "done" -> true
            else -> false
        }
    }

    private fun openAppByName(appName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(appName)
                ?: pm.getInstalledApplications(0).firstOrNull {
                    pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
                }?.let { pm.getLaunchIntentForPackage(it.packageName) }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getAdaptiveDelay(action: String): Long {
        return when (action) {
            "open_app" -> 2500L
            "type_text" -> 1200L
            "click_at", "click_text" -> 1000L
            "scroll" -> 800L
            "wait" -> 1500L
            else -> 600L
        }
    }

    /**
     * Determines the next UI action. Can connect to Gemini Nano or heuristic solver.
     */
    private fun decideNextAction(
        userGoal: String,
        screenDescription: String,
        stepIndex: Int,
        elements: List<com.example.accessibility.UIElement>
    ): ActionStep {
        val lowerGoal = userGoal.lowercase()

        // 1. WhatsApp direct messaging pattern
        if (lowerGoal.contains("whatsapp") || lowerGoal.contains("messaggio")) {
            val sendLabels = listOf("invia", "send")
            val sendNode = elements.firstOrNull { elem ->
                sendLabels.any { elem.text.contains(it, ignoreCase = true) || elem.contentDescription.contains(it, ignoreCase = true) }
            }

            if (sendNode != null) {
                return ActionStep("click_at", mapOf("x" to sendNode.bounds.centerX, "y" to sendNode.bounds.centerY))
            }

            val editNode = elements.firstOrNull { it.isEditable }
            if (editNode != null && stepIndex == 0) {
                val extractedText = userGoal.substringAfter(":", "").ifEmpty {
                    userGoal.substringAfter(" a ", "").substringAfter(" ", "")
                }.trim()
                return ActionStep("type_text", mapOf("text" to extractedText))
            }
        }

        // Finalize if no pending interactive steps
        return ActionStep("done")
    }
}
