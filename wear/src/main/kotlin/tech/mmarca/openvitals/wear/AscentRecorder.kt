package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Writes the height the barometer (`TYPE_PRESSURE`) climbed into the
 * [MetricStore], one row per clock minute. [AscentMinuteCounter] does the
 * arithmetic. Runs in `WearAppService`, so it outlives the activity. Needs
 * no grant.
 *
 * The barometer is read once a second, batched with the heart rate's
 * latency, so both arrive in one wake-up.
 */
class AscentRecorder(
    context: Context,
    private val store: MetricStore,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var thread: HandlerThread? = null
    private var counter: AscentMinuteCounter? = null
    private var lastPrunedAt = 0L

    /** Starts recording. False when the watch has no barometer or the registration failed. */
    fun start(): Boolean {
        if (thread != null) return true
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (sensor == null) {
            Log.w(TAG, "No barometer")
            return false
        }
        val worker = HandlerThread("AscentRecorder").apply { start() }
        Handler(worker.looper).post { counter = AscentMinuteCounter(store.latest(WearMetrics.ASCENT)) }
        val registered = sensorManager.registerListener(
            this,
            sensor,
            SAMPLING_PERIOD_MICROS,
            MAX_REPORT_LATENCY_MICROS,
            Handler(worker.looper),
        )
        if (!registered) {
            worker.quitSafely()
            Log.w(TAG, "Barometer listener not registered")
            return false
        }
        thread = worker
        Log.i(TAG, "Recording ascent from ${sensor.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        worker.quitSafely()
        thread = null
        Log.i(TAG, "Stopped recording ascent")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return
        val counter = counter ?: return
        val hectopascals = event.values.firstOrNull() ?: return
        val at = SensorTime.epochMillisOf(event.timestamp)
        // A fresh listener gets the last cached reading, possibly hours old.
        if (SensorTime.isStale(at)) return
        val minute = counter.onPressure(at, hectopascals) ?: return
        store.put(WearMetrics.ASCENT, minute)
        // The watch has no ascent screen yet; testers read `adb logcat -s AscentRecorder`.
        Log.i(TAG, "${java.time.Instant.ofEpochMilli(minute.epochMillis)}: ${minute.decimetres / 10.0} m climbed so far")

        if (at - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = at
            store.prune(WearMetrics.ASCENT, at)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "AscentRecorder"
        const val SAMPLING_PERIOD_MICROS = 1_000_000

        /** The heart rate's latency, so both batches arrive in one wake-up; see `HeartRateRecorder`. */
        const val MAX_REPORT_LATENCY_MICROS = 40 * 1_000_000

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
    }
}
