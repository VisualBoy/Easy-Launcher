package com.example.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Single executable action step in an automation sequence.
 */
data class ActionStep(
    val action: String,
    val params: Map<String, Any?> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("action", action)
        put("params", JSONObject(params))
    }

    companion object {
        fun fromJson(json: JSONObject): ActionStep {
            val action = json.optString("action", "done")
            val paramsObj = json.optJSONObject("params") ?: JSONObject()
            val paramsMap = mutableMapOf<String, Any?>()
            paramsObj.keys().forEach { key ->
                paramsMap[key] = paramsObj.opt(key)
            }
            return ActionStep(action, paramsMap)
        }
    }
}

/**
 * Saved reusable skill cached in local memory to enable 0-token replays.
 */
data class SavedSkill(
    val id: String,
    val task: String,
    val taskKeywords: List<String>,
    var successCount: Int,
    var failCount: Int,
    var lastUsed: Long,
    val steps: MutableList<ActionStep>
) {
    val isReliable: Boolean
        get() {
            val total = successCount + failCount
            return total >= 2 && (successCount.toDouble() / total) >= 0.75
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("task", task)
        put("taskKeywords", JSONArray(taskKeywords))
        put("successCount", successCount)
        put("failCount", failCount)
        put("lastUsed", lastUsed)
        val stepsArray = JSONArray()
        steps.forEach { stepsArray.put(it.toJson()) }
        put("steps", stepsArray)
    }

    companion object {
        fun fromJson(json: JSONObject): SavedSkill {
            val keywordsArray = json.optJSONArray("taskKeywords") ?: JSONArray()
            val keywords = mutableListOf<String>()
            for (i in 0 until keywordsArray.length()) {
                keywords.add(keywordsArray.getString(i))
            }

            val stepsArray = json.optJSONArray("steps") ?: JSONArray()
            val stepsList = mutableListOf<ActionStep>()
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                stepsList.add(ActionStep.fromJson(stepObj))
            }

            return SavedSkill(
                id = json.optString("id", System.currentTimeMillis().toString()),
                task = json.optString("task", ""),
                taskKeywords = keywords,
                successCount = json.optInt("successCount", 1),
                failCount = json.optInt("failCount", 0),
                lastUsed = json.optLong("lastUsed", System.currentTimeMillis()),
                steps = stepsList
            )
        }
    }
}
