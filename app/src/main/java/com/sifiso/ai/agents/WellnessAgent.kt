package com.sifiso.ai.agents

import android.Manifest
import android.content.Context
import com.sifiso.ai.sensors.SensorFusion
import com.sifiso.ai.sensors.SensorSnapshot
import com.sifiso.ai.privacy.TransparencyLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.math.min
import kotlin.math.max

/**
 * Wellness Agent
 * Monitors user wellbeing through biosensors and provides proactive interventions
 *
 * Features:
 * - Continuous stress monitoring
 * - Break reminders based on sitting time
 * - Hydration tracking
 * - Sleep quality analysis
 * - Mood-based recommendations
 */
class WellnessAgent(
    context: Context,
    private val sensorFusion: SensorFusion,
    transparencyLogger: TransparencyLogger
) : Agent(context, transparencyLogger) {

    override val name = "Wellness Agent"
    override val category = AgentCategory.WELLNESS
    override val requiredPermissions = listOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false

    // Wellness state tracking
    private val stressHistory = mutableListOf<StressReading>()
    private var lastBreakReminder: Long = 0
    private var lastHydrationReminder: Long = 0
    private var sittingStartTime: Long = 0
    private var isSitting = false

    // Configuration
    private val stressThreshold = 0.7f
    private val sittingReminderMinutes = 90
    private val hydrationReminderHours = 2

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        isRunning = true
        scope.launch {
            sensorFusion.sensorData.collect { snapshot ->
                onSensorData(snapshot)
            }
        }
    }

    override suspend fun onSensorData(snapshot: SensorSnapshot) {
        if (!isRunning) return

        // Log data access
        logDataAccess("biosensors", "Continuous wellbeing monitoring")

        // Analyze wellbeing
        val analysis = analyzeWellbeing(snapshot)

        // Take action if intervention needed
        if (analysis.requiresIntervention) {
            handleIntervention(analysis, snapshot)
        }

        // Update wellness state
        updateWellnessState(snapshot, analysis)
    }

    override suspend fun process() {
        // Main processing loop for background analysis
        while (isRunning) {
            delay(60000) // Process every minute

            // Analyze patterns
            analyzeStressPatterns()

            // Check for long-term wellness trends
            val history = sensorFusion.getHistory(durationMillis = 86400000) // Last 24 hours
            analyzeLongTermTrends(history)
        }
    }

    override fun stop() {
        isRunning = false
        scope.cancel()
    }

    /**
     * Comprehensive wellbeing analysis
     */
    private fun analyzeWellbeing(snapshot: SensorSnapshot): WellbeingAnalysis {
        val biosensors = snapshot.biosensors
        val motion = snapshot.motion
        val currentTime = System.currentTimeMillis()

        // Stress analysis
        val stressScore = calculateStressScore(
            heartRate = biosensors.heartRate,
            hrv = biosensors.hrvRmssd,
            skinConductance = biosensors.skinConductance,
            breathingRate = biosensors.breathingRate
        )

        // Activity level
        val activityLevel = min(1.0f, (motion.motionIntensity * 0.5f) + (motion.stepsLastHour / 1000f * 0.5f))

        // Sitting duration
        val sittingDuration = if (motion.posture.name.contains("SITTING")) {
            if (!isSitting) {
                sittingStartTime = currentTime
                isSitting = true
            }
            (currentTime - sittingStartTime) / 60000 // minutes
        } else {
            if (isSitting) {
                isSitting = false
            }
            0L
        }

        // Hydration check
        val needsHydration = biosensors.hydrationLevel < 0.4f &&
                (currentTime - lastHydrationReminder) > (hydrationReminderHours * 3600000)

        // Determine if intervention needed
        val requiresIntervention = stressScore > stressThreshold ||
                sittingDuration > sittingReminderMinutes ||
                needsHydration

        // Calculate overall wellbeing score
        val wellbeingScore = calculateWellbeingScore(
            stressScore = stressScore,
            activityLevel = activityLevel,
            hydration = biosensors.hydrationLevel,
            sleepQuality = 0.7f // Would come from sleep tracking
        )

        return WellbeingAnalysis(
            timestamp = currentTime,
            stressScore = stressScore,
            stressLevel = categorizeStress(stressScore),
            activityLevel = activityLevel,
            sittingDurationMinutes = sittingDuration,
            hydrationLevel = biosensors.hydrationLevel,
            wellbeingScore = wellbeingScore,
            requiresIntervention = requiresIntervention,
            recommendations = generateRecommendations(stressScore, sittingDuration, needsHydration)
        )
    }

    /**
     * Multi-signal stress calculation
     */
    private fun calculateStressScore(
        heartRate: Int,
        hrv: Float,
        skinConductance: Float,
        breathingRate: Int
    ): Float {
        // Heart rate contribution (elevated HR = higher stress)
        val hrStress = min(1.0f, max(0f, (heartRate - 60f) / 40f))

        // HRV contribution (lower HRV = higher stress)
        val hrvStress = min(1.0f, max(0f, (50f - hrv) / 40f))

        // Skin conductance contribution
        val scStress = min(1.0f, max(0f, (skinConductance - 5f) / 10f))

        // Breathing rate contribution
        val brStress = min(1.0f, max(0f, (breathingRate - 12f) / 10f))

        // Weighted combination
        return 0.3f * hrStress + 0.35f * hrvStress + 0.2f * scStress + 0.15f * brStress
    }

    private fun categorizeStress(stressScore: Float): StressLevel {
        return when {
            stressScore > 0.7f -> StressLevel.HIGH
            stressScore > 0.4f -> StressLevel.MODERATE
            else -> StressLevel.LOW
        }
    }

    private fun calculateWellbeingScore(
        stressScore: Float,
        activityLevel: Float,
        hydration: Float,
        sleepQuality: Float
    ): Float {
        val stressComponent = 1.0f - stressScore
        val activityComponent = min(1.0f, activityLevel * 2f)
        val hydrationComponent = hydration
        val sleepComponent = sleepQuality

        return 0.3f * stressComponent +
               0.2f * activityComponent +
               0.2f * hydrationComponent +
               0.3f * sleepComponent
    }

    /**
     * Handle wellbeing interventions
     */
    private suspend fun handleIntervention(analysis: WellbeingAnalysis, snapshot: SensorSnapshot) {
        val currentTime = System.currentTimeMillis()

        // Stress intervention
        if (analysis.stressLevel == StressLevel.HIGH) {
            logDecision(
                decision = "Stress intervention triggered",
                reasoning = "Stress score ${analysis.stressScore} exceeds threshold $stressThreshold",
                confidence = analysis.stressScore
            )

            sendNotification(
                AgentNotification(
                    title = "Take a Breathing Break",
                    message = "Your stress levels are elevated. A 5-minute breathing exercise can help.",
                    priority = AgentPriority.HIGH,
                    category = AgentCategory.WELLNESS,
                    actions = listOf(
                        NotificationAction("Start Exercise", "breathing_exercise"),
                        NotificationAction("Dismiss", "dismiss")
                    )
                )
            )
        }

        // Sitting time intervention
        if (analysis.sittingDurationMinutes > sittingReminderMinutes &&
            (currentTime - lastBreakReminder) > 1800000) { // 30 min cooldown

            logDecision(
                decision = "Movement break suggested",
                reasoning = "User has been sitting for ${analysis.sittingDurationMinutes} minutes",
                confidence = 0.9f
            )

            sendNotification(
                AgentNotification(
                    title = "Time to Move!",
                    message = "You've been sitting for ${analysis.sittingDurationMinutes} minutes. A short walk will boost your energy.",
                    priority = AgentPriority.NORMAL,
                    category = AgentCategory.WELLNESS,
                    actions = listOf(
                        NotificationAction("Stretch Routine", "stretch"),
                        NotificationAction("Later", "snooze")
                    )
                )
            )

            lastBreakReminder = currentTime
        }

        // Hydration reminder
        if (analysis.hydrationLevel < 0.4f &&
            (currentTime - lastHydrationReminder) > (hydrationReminderHours * 3600000)) {

            sendNotification(
                AgentNotification(
                    title = "Hydration Reminder",
                    message = "Your hydration levels are low. Time for some water! 💧",
                    priority = AgentPriority.NORMAL,
                    category = AgentCategory.WELLNESS
                )
            )

            lastHydrationReminder = currentTime
        }
    }

    private fun generateRecommendations(
        stressScore: Float,
        sittingDuration: Long,
        needsHydration: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            stressScore > 0.7f -> recommendations.add("Take a 5-minute breathing break")
            stressScore > 0.4f -> recommendations.add("Consider a short walk or stretch")
        }

        if (sittingDuration > 90) {
            recommendations.add("Stand up and move around")
        }

        if (needsHydration) {
            recommendations.add("Drink water to stay hydrated")
        }

        return recommendations
    }

    private fun updateWellnessState(snapshot: SensorSnapshot, analysis: WellbeingAnalysis) {
        stressHistory.add(
            StressReading(
                timestamp = analysis.timestamp,
                stressScore = analysis.stressScore,
                heartRate = snapshot.biosensors.heartRate,
                hrv = snapshot.biosensors.hrvRmssd
            )
        )

        // Keep only last 24 hours of data
        val dayAgo = System.currentTimeMillis() - 86400000
        stressHistory.removeAll { it.timestamp < dayAgo }
    }

    private fun analyzeStressPatterns() {
        if (stressHistory.size < 10) return

        val avgStress = stressHistory.map { it.stressScore }.average().toFloat()
        val recentStress = stressHistory.takeLast(5).map { it.stressScore }.average().toFloat()

        if (recentStress > avgStress * 1.5f) {
            logDecision(
                decision = "Elevated stress pattern detected",
                reasoning = "Recent stress ($recentStress) significantly higher than average ($avgStress)",
                confidence = 0.8f
            )
        }
    }

    private suspend fun analyzeLongTermTrends(history: com.sifiso.ai.sensors.SensorHistory) {
        // Analyze 24-hour trends
        val avgStress = history.averageStressScore()
        val avgHR = history.averageHeartRate()

        // Could generate weekly wellness reports here
    }

    private suspend fun sendNotification(notification: AgentNotification) {
        // Notification sending handled by orchestrator
        // In full implementation, this would use Android's NotificationManager
    }

    // Data classes for wellness tracking
    private data class StressReading(
        val timestamp: Long,
        val stressScore: Float,
        val heartRate: Int,
        val hrv: Float
    )

    private enum class StressLevel {
        LOW, MODERATE, HIGH
    }

    private data class WellbeingAnalysis(
        val timestamp: Long,
        val stressScore: Float,
        val stressLevel: StressLevel,
        val activityLevel: Float,
        val sittingDurationMinutes: Long,
        val hydrationLevel: Float,
        val wellbeingScore: Float,
        val requiresIntervention: Boolean,
        val recommendations: List<String>
    )
}
