package com.example.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TaskRecord(
    val goal: String,
    val status: String,
    val totalTokens: Int,
    val stepsTaken: Int,
    val trace: List<String>,
    val timestamp: String
)

data class TaskAnalytics(
    val totalTasks: Int,
    val successRate: Double,
    val successCount: Int,
    val failedCount: Int,
    val totalTokensUsed: Int,
    val avgStepsPerTask: Double
)

/**
 * Persists task execution logs and traces in JSONL format to local storage.
 * Enables post-task analytics, performance auditing, and context retrieval.
 */
class TaskHistoryLogger(private val context: Context) {

    private val storageFile: File
        get() = File(context.filesDir, "task_history.jsonl")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    /**
     * Appends an execution trace to task_history.jsonl.
     */
    suspend fun logTask(
        goal: String,
        status: String,
        totalTokens: Int,
        steps: Int,
        trace: List<String>
    ) = withContext(Dispatchers.IO) {
        try {
            val file = storageFile
            val record = JSONObject().apply {
                put("goal", goal.trim())
                put("status", status)
                put("total_tokens", totalTokens)
                put("steps_taken", steps)
                val traceArray = JSONArray()
                trace.forEach { traceArray.put(it) }
                put("trace", traceArray)
                put("timestamp", dateFormat.format(Date()))
            }
            file.appendText("${record}\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads all task execution records (newest first).
     */
    suspend fun readHistory(): List<TaskRecord> = withContext(Dispatchers.IO) {
        try {
            val file = storageFile
            if (!file.exists()) return@withContext emptyList()

            val lines = file.readLines()
            val records = mutableListOf<TaskRecord>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                try {
                    val json = JSONObject(trimmed)
                    val traceArray = json.optJSONArray("trace") ?: JSONArray()
                    val traceList = mutableListOf<String>()
                    for (i in 0 until traceArray.length()) {
                        traceList.add(traceArray.getString(i))
                    }

                    records.add(
                        TaskRecord(
                            goal = json.optString("goal", ""),
                            status = json.optString("status", "Unknown"),
                            totalTokens = json.optInt("total_tokens", 0),
                            stepsTaken = json.optInt("steps_taken", 0),
                            trace = traceList,
                            timestamp = json.optString("timestamp", "")
                        )
                    )
                } catch (e: Exception) {
                    // Ignore corrupted lines
                }
            }

            records.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculates system performance analytics from history.
     */
    suspend fun getAnalytics(): TaskAnalytics {
        val history = readHistory()
        if (history.isEmpty()) {
            return TaskAnalytics(0, 0.0, 0, 0, 0, 0.0)
        }

        var successCount = 0
        var failedCount = 0
        var totalTokens = 0
        var totalSteps = 0

        for (task in history) {
            totalTokens += task.totalTokens
            totalSteps += task.stepsTaken
            if (task.status.startsWith("Success", ignoreCase = true)) {
                successCount++
            } else {
                failedCount++
            }
        }

        return TaskAnalytics(
            totalTasks = history.size,
            successRate = successCount.toDouble() / history.size,
            successCount = successCount,
            failedCount = failedCount,
            totalTokensUsed = totalTokens,
            avgStepsPerTask = totalSteps.toDouble() / history.size
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            val file = storageFile
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
