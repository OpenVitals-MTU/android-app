package tech.mmarca.openvitals.features.activity.maps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * Which way the phone is pointing, degrees clockwise from north, or null when
 * disabled or the device cannot say.
 *
 * State only moves on a change the eye can see (a few degrees), so the sensor
 * ticking at UI rate does not recompose the map at UI rate. [enabled] exists
 * because this powers the live-recording arrow only — a detail screen showing
 * an old route must not hold the compass awake.
 */
@Composable
internal fun rememberDeviceHeadingDegrees(enabled: Boolean): State<Float?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            heading.value = null
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensorManager == null || sensor == null) {
            heading.value = null
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val degrees =
                    ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                val current = heading.value
                if (current == null || abs(angleDeltaDegrees(current, degrees)) >= 3f) {
                    heading.value = degrees
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

private fun angleDeltaDegrees(from: Float, to: Float): Float =
    ((to - from + 540f) % 360f) - 180f
