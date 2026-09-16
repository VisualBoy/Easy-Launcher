package com.example.ai

import android.content.Context
import com.example.model.ActionStep
import com.example.model.SavedSkill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Skill Memory Engine for Easy Launcher.
 * Learns and replays successful multi-step automation chains to bypass LLM inference (0 tokens consumed).
 * Features Italian-optimized NLU keyword extraction with full Italian accented vowel preservation.
 */
class SkillMemoryService(private val context: Context) {

    private val skills = mutableListOf<SavedSkill>()
    private var isLoaded = false

    private val storageFile: File
        get() = File(context.filesDir, "skills_memory.jsonl")

    private val italianStopWords = setOf(
        // Articoli determinativi e indeterminativi
        "il", "lo", "la", "i", "gli", "le", "un", "uno", "una",
        // Preposizioni semplici e articolate
        "di", "a", "da", "in", "con", "su", "per", "tra", "fra",
        "del", "dello", "della", "dei", "degli", "delle",
        "al", "allo", "alla", "ai", "agli", "alle",
        "dal", "dallo", "dalla", "dai", "dagli", "dalle",
        "nel", "nello", "nella", "nei", "negli", "nelle",
        "sul", "sullo", "sulla", "sui", "sugli", "sulle",
        // Congiunzioni, pronomi e particelle
        "e", "ed", "o", "ma", "se", "che", "chi", "cui", "non",
        "mi", "ti", "si", "ci", "vi", "ne", "me", "te",
        // Verbi servili, filler da assistente vocale e comandi d'azione generici
        "voglio", "puoi", "fai", "fare", "per favore",
        "apri", "avvia", "vai", "manda", "invia", "metti", "attiva", "imposta", "mostra"
    )

    private suspend fun loadSkills() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            val file = storageFile
            if (!file.exists()) {
                isLoaded = true
                return@withContext
            }

            val lines = file.readLines()
            skills.clear()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        val json = JSONObject(trimmed)
                        skills.add(SavedSkill.fromJson(json))
                    } catch (e: Exception) {
                        // Skip corrupted lines gracefully
                    }
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveAllSkills() = withContext(Dispatchers.IO) {
        try {
            val file = storageFile
            val content = StringBuilder()
            for (skill in skills) {
                content.append(skill.toJson().toString()).append("\n")
            }
            file.writeText(content.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Extracts keyword tokens from Italian text, preserving accented letters (à, è, é, ì, ò, ù)
     * and filtering out grammar filler and generic action verbs.
     */
    fun extractKeywords(text: String): List<String> {
        val normalized = text.lowercase()
            .replace(Regex("[^a-zàèéìòù0-9\\s]"), "")
            .split(Regex("\\s+"))

        return normalized.filter { word ->
            word.isNotEmpty() && !italianStopWords.contains(word)
        }
    }

    /**
     * Calculates Jaccard similarity index between two keyword sets:
     * Similarity = |A ∩ B| / |A ∪ B|
     */
    fun jaccardSimilarity(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val setA = a.toSet()
        val setB = b.toSet()
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return intersection.toDouble() / union.toDouble()
    }

    /**
     * Finds a matching cached skill if Jaccard similarity exceeds threshold (0.6).
     */
    suspend fun findSkill(taskGoal: String): SavedSkill? {
        loadSkills()
        if (skills.isEmpty()) return null

        val queryKeywords = extractKeywords(taskGoal)
        var bestMatch: SavedSkill? = null
        var highestSim = 0.0

        for (skill in skills) {
            val sim = jaccardSimilarity(queryKeywords, skill.taskKeywords)
            if (sim > highestSim) {
                highestSim = sim
                bestMatch = skill
            }
        }

        return if (highestSim > 0.6) bestMatch else null
    }

    /**
     * Caches a successful task execution chain. If an existing skill matches with >0.8 similarity,
     * updates it with the shortest path; otherwise creates a new skill.
     */
    suspend fun saveSkill(taskGoal: String, steps: List<ActionStep>) {
        loadSkills()
        val queryKeywords = extractKeywords(taskGoal)

        for (skill in skills) {
            if (jaccardSimilarity(queryKeywords, skill.taskKeywords) > 0.8) {
                skill.successCount++
                skill.lastUsed = System.currentTimeMillis()
                if (steps.isNotEmpty() && steps.size < skill.steps.size) {
                    skill.steps.clear()
                    skill.steps.addAll(steps)
                }
                saveAllSkills()
                return
            }
        }

        val newSkill = SavedSkill(
            id = System.currentTimeMillis().toString(),
            task = taskGoal,
            taskKeywords = queryKeywords,
            successCount = 1,
            failCount = 0,
            lastUsed = System.currentTimeMillis(),
            steps = steps.toMutableList()
        )
        skills.add(newSkill)
        saveAllSkills()
    }

    /**
     * Records a replay failure to decrement skill reliability.
     */
    suspend fun recordFailure(skillId: String) {
        loadSkills()
        val skill = skills.find { it.id == skillId }
        if (skill != null) {
            skill.failCount++
            saveAllSkills()
        }
    }
}
