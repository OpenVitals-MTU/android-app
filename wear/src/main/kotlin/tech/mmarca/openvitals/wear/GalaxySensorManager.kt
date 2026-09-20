package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.util.Log

class GalaxySensorManager(
    context: Context,
    onSensorDataUpdated: (SensorData) -> Unit,
) : WearSensorManager(context, onSensorDataUpdated) {

    // Samsung specialized vendor sensors can be added here if needed,
    // or standard sensors can be overrode if Samsung requires custom batching thresholds.
    
    override fun startListening() {
        super.startListening()
        
        // Query the complete discovered hardware list specifically for Samsung vendor custom strings
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        allSensors.forEach { sensor ->
            val typeString = sensor.stringType.lowercase()
            if (typeString.contains("samsung") || typeString.contains("bio") || typeString.contains("spo2") || typeString.contains("bia")) {
                if (!activeSensors.contains(sensor)) {
                    activeSensors.add(sensor)
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                    Log.d("GalaxySensorManager", "Registered Samsung Vendor Extension Sensor: ${sensor.name} (${sensor.stringType})")
                }
            }
        }
    }

    override fun handleSensorEvent(event: SensorEvent) {
        // Intercept standard events or handle custom Samsung string sensor types
        super.handleSensorEvent(event)
    }
}
