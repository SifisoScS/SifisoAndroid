package com.sifiso.ai.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import kotlin.random.Random

/**
 * Sensor Fusion System
 * Combines data from multiple sensors for comprehensive context awareness
 *
 * In prototype: Simulates biosensor data since most phones don't have medical-grade sensors
 * In production: Would integrate with actual biosensor hardware
 */
class SensorFusion(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Real-time sensor data stream
    private val _sensorData = MutableStateFlow(generateInitialSnapshot())
    val sensorData: StateFlow<SensorSnapshot> = _sensorData.asStateFlow()

    // Sensor history for pattern analysis
    private val sensorHistory = mutableListOf<SensorSnapshot>()
    private val maxHistorySize = 1000  // Keep last ~1000 snapshots

    // Simulation parameters (for prototype)
    private var simulatedStressLevel = 0.3f
    private var simulatedActivity = ActivityType.SITTING
    private var baseHeartRate = 70

    init {
        registerSensors()
        startSimulation()
    }

    private fun registerSensors() {
        // Register real Android sensors where available
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Heart rate sensor (if available on device)
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Step counter
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentSnapshot = _sensorData.value

        val updatedSnapshot = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentSnapshot.copy(
                    motion = currentSnapshot.motion.copy(
                        accelerationX = event.values[0],
                        accelerationY = event.values[1],
                        accelerationZ = event.values[2],
                        motionIntensity = calculateMotionIntensity(event.values)
                    )
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                currentSnapshot.copy(
                    motion = currentSnapshot.motion.copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2]
                    )
                )
            }

            Sensor.TYPE_LIGHT -> {
                currentSnapshot.copy(
                    environmental = currentSnapshot.environmental.copy(
                        ambientLight = event.values[0].toInt()
                    )
                )
            }

            Sensor.TYPE_PRESSURE -> {
                currentSnapshot.copy(
                    environmental = currentSnapshot.environmental.copy(
                        pressure = event.values[0]
                    )
                )
            }

            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                currentSnapshot.copy(
                    environmental = currentSnapshot.environmental.copy(
                        temperature = event.values[0]
                    )
                )
            }

            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                currentSnapshot.copy(
                    environmental = currentSnapshot.environmental.copy(
                        humidity = event.values[0].toInt()
                    )
                )
            }

            Sensor.TYPE_HEART_RATE -> {
                // If device has heart rate sensor, use real data
                val heartRate = event.values[0].toInt()
                currentSnapshot.copy(
                    biosensors = currentSnapshot.biosensors.copy(
                        heartRate = heartRate
                    )
                )
            }

            else -> currentSnapshot
        }

        _sensorData.value = updatedSnapshot
        addToHistory(updatedSnapshot)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed
    }

    /**
     * Simulation system for biosensors (prototype only)
     * In production, this would be replaced with real biosensor hardware
     */
    private fun startSimulation() {
        // Update simulated data periodically
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                updateSimulatedData()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateSimulatedData() {
        val current = _sensorData.value

        // Simulate realistic variations
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = TimeOfDay.from(hour)

        // Stress varies throughout the day
        simulatedStressLevel = when (timeOfDay) {
            TimeOfDay.EARLY_MORNING -> 0.2f + Random.nextFloat() * 0.2f
            TimeOfDay.MORNING -> 0.4f + Random.nextFloat() * 0.3f
            TimeOfDay.AFTERNOON -> 0.5f + Random.nextFloat() * 0.3f
            TimeOfDay.EVENING -> 0.3f + Random.nextFloat() * 0.2f
            TimeOfDay.NIGHT -> 0.1f + Random.nextFloat() * 0.1f
        }

        // Heart rate correlates with stress
        val heartRate = baseHeartRate + (simulatedStressLevel * 30).toInt() + Random.nextInt(-5, 5)

        // HRV inversely correlates with stress
        val hrvRmssd = 50f - (simulatedStressLevel * 30f) + Random.nextFloat() * 10f

        val updatedBiosensors = BiosensorData(
            heartRate = heartRate.coerceIn(60, 120),
            hrvRmssd = hrvRmssd.coerceIn(15f, 80f),
            skinConductance = 5f + simulatedStressLevel * 10f + Random.nextFloat() * 3f,
            breathingRate = (16 + simulatedStressLevel * 8).toInt() + Random.nextInt(-2, 2),
            hydrationLevel = 0.7f - Random.nextFloat() * 0.1f,
            bloodOxygen = 98 - Random.nextInt(0, 2),
            bodyTemperature = 36.6f + Random.nextFloat() * 0.3f,
            stressScore = simulatedStressLevel
        )

        val updatedSnapshot = current.copy(
            timestamp = System.currentTimeMillis(),
            biosensors = updatedBiosensors,
            context = current.context.copy(
                timeOfDay = timeOfDay
            )
        )

        _sensorData.value = updatedSnapshot
        addToHistory(updatedSnapshot)
    }

    private fun calculateMotionIntensity(accelerationValues: FloatArray): Float {
        val magnitude = kotlin.math.sqrt(
            accelerationValues[0] * accelerationValues[0] +
            accelerationValues[1] * accelerationValues[1] +
            accelerationValues[2] * accelerationValues[2]
        )
        // Normalize to 0-1 range
        return (magnitude / 20f).coerceIn(0f, 1f)
    }

    private fun addToHistory(snapshot: SensorSnapshot) {
        sensorHistory.add(snapshot)
        if (sensorHistory.size > maxHistorySize) {
            sensorHistory.removeAt(0)
        }
    }

    fun getHistory(durationMillis: Long = 3600000): SensorHistory {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - durationMillis

        val filteredSnapshots = sensorHistory.filter {
            it.timestamp >= startTime && it.timestamp <= endTime
        }

        return SensorHistory(filteredSnapshots, startTime, endTime)
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Manual control for demonstration/testing
     */
    fun simulateStressEvent() {
        simulatedStressLevel = 0.8f
        baseHeartRate = 90
    }

    fun simulateCalm() {
        simulatedStressLevel = 0.2f
        baseHeartRate = 65
    }

    fun simulateActivity(activity: ActivityType) {
        simulatedActivity = activity
        baseHeartRate = when (activity) {
            ActivityType.STILL, ActivityType.SITTING -> 70
            ActivityType.STANDING, ActivityType.WALKING -> 85
            ActivityType.RUNNING -> 130
            ActivityType.CYCLING -> 120
            ActivityType.DRIVING -> 75
            ActivityType.UNKNOWN -> 70
        }
    }

    companion object {
        private fun generateInitialSnapshot(): SensorSnapshot {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return SensorSnapshot(
                timestamp = System.currentTimeMillis(),
                biosensors = BiosensorData(),
                environmental = EnvironmentalData(),
                motion = MotionData(),
                context = ContextData(timeOfDay = TimeOfDay.from(hour))
            )
        }
    }
}
