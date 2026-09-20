package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.SensorEvent

class PixelSensorManager(
    context: Context,
    onSensorDataUpdated: (SensorData) -> Unit,
) : WearSensorManager(context, onSensorDataUpdated) {

    // Pixel Watch specialized vendor sensors can be added here if needed,
    // or standard sensors can be overrode if Google requires custom configurations.

    override fun startListening() {
        super.startListening()
        // Pixel Watch specific setup can go here
    }

    override fun handleSensorEvent(event: SensorEvent) {
        // Intercept standard events or handle custom Pixel sensor types here
        super.handleSensorEvent(event)
    }
}
