package com.sifiso.ai.privacy

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transparency Logger
 * Records all AI decisions and data access for user visibility and trust
 *
 * Every data access is logged with:
 * - What data was accessed
 * - Why it was needed
 * - Who requested it (which agent)
 * - When it occurred
 */
class TransparencyLogger(private val context: Context) {

    private val logs = mutableListOf<LogEntry>()
    private val maxLogs = 10000  // Keep last 10,000 entries

    // Real-time log stream for UI
    private val _recentLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val recentLogs: StateFlow<List<LogEntry>> = _recentLogs.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Log data access by an agent
     */
    fun logDataAccess(
        agentName: String,
        dataType: String,
        purpose: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = LogEntry(
            timestamp = timestamp,
            type = LogType.DATA_ACCESS,
            agentName = agentName,
            message = "Data access: $dataType",
            details = mapOf(
                "data_type" to dataType,
                "purpose" to purpose,
                "justification" to "Required for $purpose"
            )
        )

        addLog(entry)
    }

    /**
     * Log AI decision with reasoning
     */
    fun logAIDecision(
        agentName: String,
        decision: String,
        reasoning: String,
        confidence: Float,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = LogEntry(
            timestamp = timestamp,
            type = LogType.AI_DECISION,
            agentName = agentName,
            message = decision,
            details = mapOf(
                "decision" to decision,
                "reasoning" to reasoning,
                "confidence" to confidence,
                "explainability" to "Decision made because: $reasoning"
            )
        )

        addLog(entry)
    }

    /**
     * Log system events
     */
    fun logSystemEvent(
        event: String,
        details: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = LogEntry(
            timestamp = timestamp,
            type = LogType.SYSTEM_EVENT,
            agentName = "System",
            message = event,
            details = mapOf("details" to details)
        )

        addLog(entry)
    }

    /**
     * Log user interaction
     */
    fun logUserInteraction(
        interaction: String,
        context: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = LogEntry(
            timestamp = timestamp,
            type = LogType.USER_INTERACTION,
            agentName = "User",
            message = interaction,
            details = mapOf("context" to context)
        )

        addLog(entry)
    }

    /**
     * Log privacy-related event
     */
    fun logPrivacyEvent(
        event: String,
        dataTypes: List<String>,
        action: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = LogEntry(
            timestamp = timestamp,
            type = LogType.PRIVACY_EVENT,
            agentName = "Privacy System",
            message = event,
            details = mapOf(
                "event" to event,
                "data_types" to dataTypes.joinToString(", "),
                "action" to action
            )
        )

        addLog(entry)
    }

    private fun addLog(entry: LogEntry) {
        synchronized(logs) {
            logs.add(entry)

            // Keep only recent logs in memory
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }

            // Update real-time stream (last 100 entries)
            _recentLogs.value = logs.takeLast(100).reversed()
        }

        // In production: Persist to database
        // In production: Encrypt sensitive log data
    }

    /**
     * Get logs filtered by type
     */
    fun getLogsByType(type: LogType, limit: Int = 100): List<LogEntry> {
        return logs.filter { it.type == type }.takeLast(limit).reversed()
    }

    /**
     * Get logs filtered by agent
     */
    fun getLogsByAgent(agentName: String, limit: Int = 100): List<LogEntry> {
        return logs.filter { it.agentName == agentName }.takeLast(limit).reversed()
    }

    /**
     * Get logs within time range
     */
    fun getLogsByTimeRange(startTime: Long, endTime: Long): List<LogEntry> {
        return logs.filter { it.timestamp in startTime..endTime }.reversed()
    }

    /**
     * Get all logs (for privacy dashboard)
     */
    fun getAllLogs(limit: Int = 1000): List<LogEntry> {
        return logs.takeLast(limit).reversed()
    }

    /**
     * Get summary statistics
     */
    fun getStatistics(durationMillis: Long = 86400000): LogStatistics {
        val cutoff = System.currentTimeMillis() - durationMillis
        val recentLogs = logs.filter { it.timestamp >= cutoff }

        return LogStatistics(
            totalLogs = recentLogs.size,
            dataAccessCount = recentLogs.count { it.type == LogType.DATA_ACCESS },
            aiDecisionCount = recentLogs.count { it.type == LogType.AI_DECISION },
            privacyEventCount = recentLogs.count { it.type == LogType.PRIVACY_EVENT },
            agentBreakdown = recentLogs.groupingBy { it.agentName }.eachCount(),
            startTime = cutoff,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Export logs (for user download)
     */
    fun exportLogs(format: ExportFormat = ExportFormat.JSON): String {
        return when (format) {
            ExportFormat.JSON -> exportAsJSON()
            ExportFormat.CSV -> exportAsCSV()
            ExportFormat.TEXT -> exportAsText()
        }
    }

    private fun exportAsJSON(): String {
        // In production: Use proper JSON serialization
        return logs.joinToString(",\n", "[\n", "\n]") { entry ->
            """  {
    "timestamp": "${dateFormat.format(Date(entry.timestamp))}",
    "type": "${entry.type}",
    "agent": "${entry.agentName}",
    "message": "${entry.message}",
    "details": ${entry.details}
  }"""
        }
    }

    private fun exportAsCSV(): String {
        val header = "Timestamp,Type,Agent,Message,Details\n"
        val rows = logs.joinToString("\n") { entry ->
            "${dateFormat.format(Date(entry.timestamp))},${entry.type},${entry.agentName},${entry.message},${entry.details}"
        }
        return header + rows
    }

    private fun exportAsText(): String {
        return logs.joinToString("\n\n") { entry ->
            """
            [${dateFormat.format(Date(entry.timestamp))}] ${entry.type}
            Agent: ${entry.agentName}
            Message: ${entry.message}
            Details: ${entry.details}
            """.trimIndent()
        }
    }

    /**
     * Clear old logs (privacy compliance)
     */
    fun clearLogsOlderThan(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 86400000L)
        synchronized(logs) {
            logs.removeAll { it.timestamp < cutoffTime }
        }
    }

    /**
     * Data classes
     */
    data class LogEntry(
        val timestamp: Long,
        val type: LogType,
        val agentName: String,
        val message: String,
        val details: Map<String, Any>
    )

    enum class LogType {
        DATA_ACCESS,        // Agent accessed user data
        AI_DECISION,        // Agent made an AI-powered decision
        SYSTEM_EVENT,       // System-level event
        USER_INTERACTION,   // User interacted with agent
        PRIVACY_EVENT       // Privacy-related event
    }

    data class LogStatistics(
        val totalLogs: Int,
        val dataAccessCount: Int,
        val aiDecisionCount: Int,
        val privacyEventCount: Int,
        val agentBreakdown: Map<String, Int>,
        val startTime: Long,
        val endTime: Long
    )

    enum class ExportFormat {
        JSON,
        CSV,
        TEXT
    }
}
