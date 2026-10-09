package tech.mmarca.openvitals.wear.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.getCapabilities
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.features.measure.Measurement
import tech.mmarca.openvitals.wear.features.recording.ActivityType
import tech.mmarca.openvitals.wear.features.recording.WorkoutField

private const val TAG = "CapabilityProbe"
private const val HealthServicesTimeoutMillis = 5_000L

/**
 * Asks Health Services and the sensor list what this watch can do. Health
 * Services is the source of truth where it answers; the raw sensor list fills
 * in what it does not cover (single beats, the barometer, vendor SpO2) and
 * stands in for it on a watch without it.
 */
class CapabilityProbe(private val context: Context) {

    suspend fun probe(): WearCapabilities {
        val sensors = context.getSystemService(SensorManager::class.java)
        val hasSensor = { type: Int -> sensors?.getDefaultSensor(type) != null }
        val hasBeatSensor = hasSensor(Sensor.TYPE_HEART_BEAT)
        val hasBarometer = hasSensor(Sensor.TYPE_PRESSURE)
        val hasVendorSpo2 = sensors?.getSensorList(Sensor.TYPE_ALL).orEmpty()
            .any { it.stringType.contains("spo2", ignoreCase = true) }

        val client = runCatching { HealthServices.getClient(context) }.getOrNull()
        val passive = client?.let { healthServicesCall { it.passiveMonitoringClient.getCapabilities() } }
        val measure = client?.let { healthServicesCall { it.measureClient.getCapabilities() } }
        val exercise = client?.let { healthServicesCall { it.exerciseClient.getCapabilities() } }

        val sensorMetrics = buildSet {
            passive?.supportedDataTypesPassiveMonitoring?.forEach { type ->
                PassiveMetrics[type]?.let(::add)
            }
            if (passive == null) {
                // No Health Services answer: fall back to what the raw sensors can count.
                if (hasSensor(Sensor.TYPE_STEP_COUNTER)) add(WearMetric.STEPS)
                if (hasSensor(Sensor.TYPE_HEART_RATE)) add(WearMetric.HEART_RATE)
            }
            if (hasBarometer && passive == null) {
                add(WearMetric.FLOORS)
                add(WearMetric.ELEVATION)
            }
            if (hasVendorSpo2) add(WearMetric.BLOOD_OXYGEN)
            if (exercise?.typeToCapabilities?.values?.any { DataType.VO2_MAX in it.supportedDataTypes } == true) {
                add(WearMetric.VO2_MAX)
            }
        }

        val measurements = buildSet {
            if (measure?.supportedDataTypesMeasure?.contains(DataType.HEART_RATE_BPM) == true) {
                add(Measurement.HEART_RATE)
            }
            if (hasBeatSensor) add(Measurement.HRV)
            if (hasBarometer) add(Measurement.AIR_PRESSURE)
        }

        val workouts = buildMap {
            ActivityType.entries.forEach { activity ->
                val typeCapabilities = exercise?.typeToCapabilities?.get(activity.exerciseType) ?: return@forEach
                put(
                    activity,
                    typeCapabilities.supportedDataTypes.mapNotNullTo(mutableSetOf()) { WorkoutFields[it] },
                )
            }
        }

        return WearCapabilities(
            probed = true,
            sensorMetrics = sensorMetrics,
            measurements = measurements,
            workouts = workouts,
        )
    }

    /** A Health Services call that fails or hangs counts as "not supported". */
    private suspend fun <T> healthServicesCall(call: suspend () -> T): T? =
        try {
            withTimeoutOrNull(HealthServicesTimeoutMillis) { call() }
        } catch (e: Exception) {
            Log.w(TAG, "Health Services capability query failed", e)
            null
        }

    private companion object {
        val PassiveMetrics = mapOf(
            DataType.STEPS_DAILY to WearMetric.STEPS,
            DataType.HEART_RATE_BPM to WearMetric.HEART_RATE,
            DataType.DISTANCE_DAILY to WearMetric.DISTANCE,
            DataType.CALORIES_DAILY to WearMetric.ACTIVE_CALORIES,
            DataType.FLOORS_DAILY to WearMetric.FLOORS,
            DataType.ELEVATION_GAIN_DAILY to WearMetric.ELEVATION,
        )

        val WorkoutFields = mapOf(
            DataType.HEART_RATE_BPM to WorkoutField.HEART_RATE,
            DataType.DISTANCE_TOTAL to WorkoutField.DISTANCE,
            DataType.CALORIES_TOTAL to WorkoutField.CALORIES,
            DataType.PACE to WorkoutField.PACE,
            DataType.SPEED to WorkoutField.SPEED,
            DataType.ELEVATION_GAIN_TOTAL to WorkoutField.ELEVATION,
            DataType.STEPS_TOTAL to WorkoutField.STEPS,
        )
    }
}

/** The Health Services exercise a workout is recorded as. */
val ActivityType.exerciseType: ExerciseType
    get() = when (this) {
        ActivityType.WALK -> ExerciseType.WALKING
        ActivityType.RUN -> ExerciseType.RUNNING
        ActivityType.CYCLE -> ExerciseType.BIKING
        ActivityType.HIKE -> ExerciseType.HIKING
        ActivityType.WORKOUT -> ExerciseType.WORKOUT
    }
