package com.sifiso.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.sifiso.ai.agents.AgentOrchestrator
import com.sifiso.ai.agents.WellnessAgent
import com.sifiso.ai.agents.CareerAgent
import com.sifiso.ai.sensors.SensorFusion
import com.sifiso.ai.privacy.TransparencyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * SIFISO AI Phone - Android Application
 * Main application class that initializes the AI agent system
 */
class SifisoApplication : Application() {

    // Application-wide coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Core AI components
    lateinit var agentOrchestrator: AgentOrchestrator
        private set

    lateinit var sensorFusion: SensorFusion
        private set

    lateinit var transparencyLogger: TransparencyLogger
        private set

    companion object {
        const val WELLNESS_CHANNEL_ID = "wellness_notifications"
        const val CAREER_CHANNEL_ID = "career_notifications"
        const val ROUTINE_CHANNEL_ID = "routine_notifications"
        const val SYSTEM_CHANNEL_ID = "system_notifications"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize privacy and transparency system
        transparencyLogger = TransparencyLogger(this)
        transparencyLogger.logSystemEvent("Application started", "System initialization")

        // Initialize sensor fusion system
        sensorFusion = SensorFusion(this)

        // Initialize AI agent orchestrator
        agentOrchestrator = AgentOrchestrator(
            context = this,
            sensorFusion = sensorFusion,
            transparencyLogger = transparencyLogger
        )

        // Register AI agents
        registerAgents()

        // Create notification channels
        createNotificationChannels()

        // Start background services
        startBackgroundServices()

        transparencyLogger.logSystemEvent(
            "AI system initialized",
            "All agents registered and ready"
        )
    }

    private fun registerAgents() {
        // Wellness Agent
        val wellnessAgent = WellnessAgent(
            context = this,
            sensorFusion = sensorFusion,
            transparencyLogger = transparencyLogger
        )
        agentOrchestrator.registerAgent("wellness", wellnessAgent)

        // Career Agent
        val careerAgent = CareerAgent(
            context = this,
            transparencyLogger = transparencyLogger
        )
        agentOrchestrator.registerAgent("career", careerAgent)

        // Additional agents can be registered here:
        // - TravelAgent
        // - FinanceAgent
        // - HomeAutomationAgent
        // - CommunicationAgent
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Wellness notifications
            val wellnessChannel = NotificationChannel(
                WELLNESS_CHANNEL_ID,
                "Wellness Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Stress alerts, break reminders, and wellness interventions"
                enableVibration(true)
            }

            // Career notifications
            val careerChannel = NotificationChannel(
                CAREER_CHANNEL_ID,
                "Career Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Job opportunities, skill recommendations, and career trends"
            }

            // Routine notifications
            val routineChannel = NotificationChannel(
                ROUTINE_CHANNEL_ID,
                "Routine Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning routines, travel updates, and automated actions"
            }

            // System notifications
            val systemChannel = NotificationChannel(
                SYSTEM_CHANNEL_ID,
                "System",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background AI processing and system status"
            }

            notificationManager.createNotificationChannels(listOf(
                wellnessChannel,
                careerChannel,
                routineChannel,
                systemChannel
            ))
        }
    }

    private fun startBackgroundServices() {
        // Background services will be started when user grants permissions
        // This is handled in MainActivity after permission checks
    }

    override fun onTerminate() {
        transparencyLogger.logSystemEvent("Application terminating", "Cleanup in progress")
        super.onTerminate()
    }
}
