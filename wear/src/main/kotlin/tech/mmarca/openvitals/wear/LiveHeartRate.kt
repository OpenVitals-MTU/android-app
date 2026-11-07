package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * The heart rate as it happens, for a phone that is showing it: one
 * `HR_LIVE` window at a time per connection. Each window registers its own
 * listener without batching and drops it at the end, so the recorder's
 * batched listener and its wake-ups stay as they are. Nothing is stored.
 *
 * Only while [isRecording]: the recorder runs only when the heart rate is
 * granted and the watch is worn ([WornMonitor]), and the phone's view must
 * not light the sensor of a watch lying on a table.
 */
class LiveHeartRate(
    context: Context,
    private val isRecording: () -> Boolean,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Whether the watch has a heart rate sensor at all; the link announces `hl` then. */
    val hasSensor: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null

    /** Blocks for up to [seconds], handing each usable reading to [emit] on the caller's thread; returns how many. */
    fun window(seconds: Int, emit: (WearLinkProtocol.HeartRateSample) -> Unit): Int {
        if (!isRecording()) return 0
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return 0
        val readings = LinkedBlockingQueue<WearLinkProtocol.HeartRateSample>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val bpm = event.values.firstOrNull()?.toInt() ?: return
                val at = SensorTime.epochMillisOf(event.timestamp)
                if (SensorTime.isStale(at) || !isUsableHeartRate(event.accuracy, bpm)) return
                readings.put(WearLinkProtocol.HeartRateSample(at, bpm))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val worker = HandlerThread("LiveHeartRate").apply { start() }
        val registered = try {
            sensorManager.registerListener(listener, sensor, SAMPLING_PERIOD_MICROS, 0, Handler(worker.looper))
        } catch (e: SecurityException) {
            Log.e(TAG, "Heart rate sensor refused", e)
            false
        }
        if (!registered) {
            worker.quitSafely()
            return 0
        }
        var count = 0
        try {
            val deadline = System.currentTimeMillis() + seconds.coerceIn(1, WearLinkProtocol.MAX_LIVE_SECONDS) * 1000L
            while (true) {
                val left = deadline - System.currentTimeMillis()
                if (left <= 0) break
                val reading = readings.poll(left, TimeUnit.MILLISECONDS) ?: break
                emit(reading)
                count++
            }
        } finally {
            sensorManager.unregisterListener(listener)
            worker.quitSafely()
        }
        return count
    }

    private companion object {
        const val TAG = "LiveHeartRate"

        /** One reading a second, as the sensor delivers anyway. */
        const val SAMPLING_PERIOD_MICROS = 1_000_000
    }
}
