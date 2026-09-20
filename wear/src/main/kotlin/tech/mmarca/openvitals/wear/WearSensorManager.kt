package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

open class WearSensorManager(
    context: Context,
    private val onSensorDataUpdated: (SensorData) -> Unit
) : SensorEventListener {
    protected val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Dynamic tracking list of active sensors discovered on the watch
    protected val activeSensors = mutableListOf<Sensor>()

    open fun startListening() {
        // Fetch ALL available sensors on the current watch hardware
        val allAvailableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        // Log all hardware sensors so we can see precisely what the watch offers
        allAvailableSensors.forEach { sensor ->
            Log.d("WearSensorManager", "Discovered Hardware Sensor: [${sensor.name}] Type: ${sensor.stringType} ID: ${sensor.type}")
        }

        // Auto-detect standard health/fitness sensors from the hardware list
        allAvailableSensors.forEach { sensor ->
            when (sensor.type) {
                Sensor.TYPE_HEART_RATE,
                Sensor.TYPE_STEP_COUNTER,
                Sensor.TYPE_HEART_BEAT,
                Sensor.TYPE_PRESSURE -> { // Barometer for altitude tracking
                    activeSensors.add(sensor)
                }
            }
        }

        if (activeSensors.isEmpty()) {
            Log.e("WearSensorManager", "No standard health sensors detected on this device")
            return
        }

        activeSensors.forEach { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("WearSensorManager", "Registered listener for Standard Sensor: ${sensor.name}")
        }
    }

    open fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d("WearSensorManager", "Unregistered all sensor listeners")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        
        handleSensorEvent(event)
    }

    protected open fun handleSensorEvent(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val heartRate = event.values.getOrNull(0) ?: return
                onSensorDataUpdated(SensorData.HeartRate(heartRate))
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values.getOrNull(0) ?: return
                onSensorDataUpdated(SensorData.StepCount(totalSteps))
            }
            Sensor.TYPE_HEART_BEAT -> {
                val confidence = event.values.getOrNull(0) ?: return
                onSensorDataUpdated(SensorData.HeartBeat(confidence))
            }
            Sensor.TYPE_PRESSURE -> {
                val hpa = event.values.getOrNull(0) ?: return
                onSensorDataUpdated(SensorData.BarometerPressure(hpa))
            }
        }
    }

    protected fun notifyUpdate(data: SensorData) {
        onSensorDataUpdated(data)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be handled or monitored if signal accuracy degrades
    }
}

sealed interface SensorData {
    data class HeartRate(val bpm: Float) : SensorData
    data class StepCount(val totalSteps: Float) : SensorData
    data class HeartBeat(val confidence: Float) : SensorData
    data class BarometerPressure(val hpa: Float) : SensorData
}