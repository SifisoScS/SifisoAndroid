package com.sifiso.ai.agents

import android.content.Context
import com.sifiso.ai.privacy.TransparencyLogger
import com.sifiso.ai.sensors.SensorSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Base AI Agent interface
 * All autonomous agents extend this interface
 */
abstract class Agent(
    protected val context: Context,
    protected val transparencyLogger: TransparencyLogger
) {
    /**
     * Agent metadata
     */
    abstract val name: String
    abstract val category: AgentCategory
    abstract val requiredPermissions: List<String>

    /**
     * Called when sensor data is updated
     * Agents can react to real-time sensor changes
     */
    abstract suspend fun onSensorData(snapshot: SensorSnapshot)

    /**
     * Main processing loop
     * Agents implement their core logic here
     */
    abstract suspend fun process()

    /**
     * Called when agent should stop processing
     */
    abstract fun stop()

    /**
     * Check if agent has necessary permissions
     */
    open fun hasRequiredPermissions(): Boolean {
        // Permission checking logic
        return true // Simplified for prototype
    }

    /**
     * Log data access for transparency
     */
    protected fun logDataAccess(dataType: String, purpose: String) {
        transparencyLogger.logDataAccess(
            agentName = name,
            dataType = dataType,
            purpose = purpose,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Log AI decision for explainability
     */
    protected fun logDecision(decision: String, reasoning: String, confidence: Float = 1.0f) {
        transparencyLogger.logAIDecision(
            agentName = name,
            decision = decision,
            reasoning = reasoning,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Agent categories for organization
 */
enum class AgentCategory {
    WELLNESS,
    CAREER,
    TRAVEL,
    FINANCE,
    HOME_AUTOMATION,
    COMMUNICATION,
    PRODUCTIVITY
}

/**
 * Agent task priorities
 */
enum class AgentPriority {
    CRITICAL,   // Immediate attention required (e.g., health emergency)
    HIGH,       // Important but not critical (e.g., meeting reminder)
    NORMAL,     // Regular operations (e.g., routine execution)
    LOW         // Background tasks (e.g., trend analysis)
}

/**
 * Agent task representation
 */
data class AgentTask(
    val taskType: String,
    val parameters: Map<String, Any>,
    val priority: AgentPriority,
    val timestamp: Long = System.currentTimeMillis(),
    var status: TaskStatus = TaskStatus.PENDING,
    var result: Any? = null
)

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

/**
 * Agent notification to user
 */
data class AgentNotification(
    val title: String,
    val message: String,
    val priority: AgentPriority,
    val category: AgentCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: List<NotificationAction> = emptyList(),
    val data: Map<String, Any> = emptyMap()
)

data class NotificationAction(
    val label: String,
    val actionId: String
)
