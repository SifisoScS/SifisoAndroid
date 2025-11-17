package com.sifiso.ai.sensors

import java.util.Date

/**
 * Data classes for sensor readings
 */

/**
 * Complete sensor snapshot at a moment in time
 */
data class SensorSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val biosensors: BiosensorData,
    val environmental: EnvironmentalData,
    val motion: MotionData,
    val context: ContextData
)

/**
 * Biosensor readings (heart rate, stress, etc.)
 */
data class BiosensorData(
    val heartRate: Int = 70,              // beats per minute
    val hrvRmssd: Float = 40f,            // HRV metric (ms)
    val skinConductance: Float = 5f,      // microsiemens
    val breathingRate: Int = 16,          // breaths per minute
    val hydrationLevel: Float = 0.7f,     // 0.0 - 1.0
    val bloodOxygen: Int = 98,            // SpO2 percentage
    val bodyTemperature: Float = 36.6f,   // Celsius
    val stressScore: Float = 0.3f         // 0.0 - 1.0 (calculated)
)

/**
 * Environmental sensor readings
 */
data class EnvironmentalData(
    val ambientLight: Int = 500,          // lux
    val colorTemperature: Int = 5000,     // Kelvin
    val temperature: Float = 22f,         // Celsius
    val humidity: Int = 45,               // percentage
    val pressure: Float = 1013.25f,       // hPa
    val noiseLevel: Int = 40,             // dB
    val airQuality: AirQuality = AirQuality()
)

data class AirQuality(
    val pm25: Float = 10f,                // μg/m³
    val pm10: Float = 15f,                // μg/m³
    val voc: Float = 100f,                // ppb
    val co2: Int = 400,                   // ppm
    val aqi: Int = 50                     // Air Quality Index (0-500)
)

/**
 * Motion and activity data
 */
data class MotionData(
    val accelerationX: Float = 0f,        // m/s²
    val accelerationY: Float = 0f,
    val accelerationZ: Float = 9.81f,     // gravity
    val gyroX: Float = 0f,                // rad/s
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val motionIntensity: Float = 0.1f,    // 0.0 - 1.0
    val stepsLastHour: Int = 50,
    val currentActivity: ActivityType = ActivityType.SITTING,
    val posture: PostureType = PostureType.SITTING
)

enum class ActivityType {
    STILL,
    SITTING,
    STANDING,
    WALKING,
    RUNNING,
    CYCLING,
    DRIVING,
    UNKNOWN
}

enum class PostureType {
    SITTING,
    STANDING,
    LYING_DOWN,
    WALKING,
    UNKNOWN
}

/**
 * Contextual information
 */
data class ContextData(
    val timeOfDay: TimeOfDay,
    val location: LocationContext,
    val batteryLevel: Int = 80,           // percentage
    val isCharging: Boolean = false,
    val screenOn: Boolean = true,
    val inCall: Boolean = false,
    val headphonesConnected: Boolean = false,
    val wifiConnected: Boolean = true,
    val currentWeather: WeatherCondition = WeatherCondition()
)

enum class TimeOfDay {
    EARLY_MORNING,  // 5-8 AM
    MORNING,        // 8-12 PM
    AFTERNOON,      // 12-5 PM
    EVENING,        // 5-9 PM
    NIGHT;          // 9 PM - 5 AM

    companion object {
        fun from(hour: Int): TimeOfDay = when (hour) {
            in 5..7 -> EARLY_MORNING
            in 8..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

data class LocationContext(
    val type: LocationType = LocationType.HOME,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f              // meters
)

enum class LocationType {
    HOME,
    WORK,
    GYM,
    TRANSIT,
    OUTDOOR,
    UNKNOWN
}

data class WeatherCondition(
    val temperature: Float = 20f,         // Celsius
    val condition: String = "clear",
    val humidity: Int = 50,               // percentage
    val windSpeed: Float = 5f             // m/s
)

/**
 * Historical sensor data for pattern analysis
 */
data class SensorHistory(
    val snapshots: List<SensorSnapshot>,
    val startTime: Long,
    val endTime: Long
) {
    fun averageHeartRate(): Float {
        return snapshots.map { it.biosensors.heartRate }.average().toFloat()
    }

    fun averageStressScore(): Float {
        return snapshots.map { it.biosensors.stressScore }.average().toFloat()
    }

    fun totalSteps(): Int {
        return snapshots.sumOf { it.motion.stepsLastHour }
    }

    fun sittingDuration(): Long {
        // Calculate continuous sitting time in minutes
        var sittingMinutes = 0L
        var continuousSitting = 0L

        for (snapshot in snapshots) {
            if (snapshot.motion.currentActivity == ActivityType.SITTING ||
                snapshot.motion.posture == PostureType.SITTING) {
                continuousSitting++
            } else {
                continuousSitting = 0
            }
            sittingMinutes = maxOf(sittingMinutes, continuousSitting)
        }

        return sittingMinutes
    }
}
