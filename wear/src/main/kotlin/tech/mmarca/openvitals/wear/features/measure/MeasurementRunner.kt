package tech.mmarca.openvitals.wear.features.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs one [Measurement] against the real sensors. Each flow emits
 * [MeasureEvent]s, ends with a [MeasureEvent.Result] and fails with a
 * [MeasureException] when there is no result to give. Collecting stops the
 * sensor, so leaving the screen cancels the measurement.
 */
class MeasurementRunner(private val context: Context) {

    fun run(measurement: Measurement): Flow<MeasureEvent> = when (measurement) {
        Measurement.HEART_RATE -> heartRate()
        Measurement.HRV -> hrv()
        Measurement.AIR_PRESSURE -> airPressure()
    }

    private fun heartRate(): Flow<MeasureEvent> = flow {
        val readings = mutableListOf<Double>()
        var offBody = false
        var settled: Double? = null
        withTimeoutOrNull(Measurement.HEART_RATE.durationSeconds * 1000L) {
            healthServicesHeartRate()
                .transformWhile { sample ->
                    when (sample) {
                        is HeartRateSample.Reading -> {
                            offBody = false
                            if (sample.bpm > 0) {
                                readings += sample.bpm
                                emit(MeasureEvent.Live(sample.bpm))
                            }
                            settled = settledHeartRate(readings)
                        }
                        is HeartRateSample.Waiting -> {
                            offBody = sample.offBody
                            emit(MeasureEvent.Waiting(sample.offBody))
                        }
                    }
                    settled == null
                }
                .collect { emit(it) }
        }
        val value = settled
            ?: throw MeasureException(if (offBody) MeasureError.OFF_BODY else MeasureError.NO_SIGNAL)
        emit(MeasureEvent.Result(value))
    }

    private fun hrv(): Flow<MeasureEvent> = flow {
        val beats = mutableListOf<Long>()
        withTimeoutOrNull(Measurement.HRV.durationSeconds * 1000L) {
            sensorEvents(Sensor.TYPE_HEART_BEAT).collect { event ->
                beats += event.timestamp
                rrIntervalsMillis(beats.takeLast(2)).lastOrNull()?.let { rr ->
                    if (rr in 300.0..2000.0) emit(MeasureEvent.Live(60_000.0 / rr))
                }
            }
        }
        val value = rmssd(rrIntervalsMillis(beats)) ?: throw MeasureException(MeasureError.NO_SIGNAL)
        emit(MeasureEvent.Result(value))
    }

    private fun airPressure(): Flow<MeasureEvent> = flow {
        val readings = mutableListOf<Double>()
        withTimeoutOrNull(Measurement.AIR_PRESSURE.durationSeconds * 1000L) {
            sensorEvents(Sensor.TYPE_PRESSURE)
                .transformWhile { event ->
                    val hpa = event.values[0].toDouble()
                    readings += hpa
                    emit(MeasureEvent.Live(hpa))
                    readings.size < PressureReadingsNeeded
                }
                .collect { emit(it) }
        }
        if (readings.size < PressureReadingsNeeded) throw MeasureException(MeasureError.NO_SIGNAL)
        val hpa = readings.average()
        val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hpa.toFloat())
        emit(MeasureEvent.Result(value = hpa, secondary = altitude.toDouble()))
    }

    private fun healthServicesHeartRate(): Flow<HeartRateSample> = callbackFlow {
        val client = try {
            HealthServices.getClient(context).measureClient
        } catch (e: Exception) {
            throw MeasureException(MeasureError.NOT_SUPPORTED, e)
        }
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                if (availability !is DataTypeAvailability) return
                when (availability) {
                    DataTypeAvailability.AVAILABLE -> Unit
                    DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> trySend(HeartRateSample.Waiting(offBody = true))
                    else -> trySend(HeartRateSample.Waiting(offBody = false))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                data.getData(DataType.HEART_RATE_BPM).forEach { trySend(HeartRateSample.Reading(it.value)) }
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                val error = if (throwable is SecurityException) MeasureError.PERMISSION_DENIED else MeasureError.FAILED
                close(MeasureException(error, throwable))
            }
        }
        client.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        awaitClose { client.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback) }
    }

    private fun sensorEvents(type: Int): Flow<SensorEvent> = callbackFlow {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(type) ?: throw MeasureException(MeasureError.NOT_SUPPORTED)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = try {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        } catch (e: SecurityException) {
            throw MeasureException(MeasureError.PERMISSION_DENIED, e)
        }
        if (!registered) throw MeasureException(MeasureError.FAILED)
        awaitClose { manager.unregisterListener(listener) }
    }

    private sealed interface HeartRateSample {
        data class Reading(val bpm: Double) : HeartRateSample
        data class Waiting(val offBody: Boolean) : HeartRateSample
    }

    private companion object {
        const val PressureReadingsNeeded = 5
    }
}
