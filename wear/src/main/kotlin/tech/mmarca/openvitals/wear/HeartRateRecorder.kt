package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log

/**
 * Keeps the heart rate sensor on and writes what it reports to the
 * [HeartRateStore], one sample every [MIN_SAMPLE_GAP_MILLIS] at most. Runs
 * inside `WearAppService`, so it outlives the activity.
 *
 * The sensor is registered batched: the sensor hub buffers readings for up
 * to [MAX_REPORT_LATENCY_MICROS] and wakes the CPU once per batch instead of
 * once per beat. The listener runs on its own thread, where the SQLite write
 * is allowed.
 */
class HeartRateRecorder(
    context: Context,
    private val store: HeartRateStore,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var thread: HandlerThread? = null
    private var lastStoredAt = 0L
    private var lastPrunedAt = 0L

    val isRunning: Boolean
        get() = thread != null

    /** Starts recording. False when the watch has no heart rate sensor or the registration failed. */
    fun start(): Boolean {
        if (thread != null) return true
        // The wake-up variant empties its batch even while the watch sleeps; the
        // other one hands over only what survived until the next wake.
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (sensor == null) {
            Log.w(TAG, "No heart rate sensor")
            return false
        }
        val worker = HandlerThread("HeartRateRecorder").apply { start() }
        val registered = try {
            sensorManager.registerListener(
                this,
                sensor,
                SAMPLING_PERIOD_MICROS,
                MAX_REPORT_LATENCY_MICROS,
                Handler(worker.looper),
            )
        } catch (e: SecurityException) {
            // The grant was revoked between the check and the registration.
            Log.e(TAG, "Heart rate sensor refused", e)
            false
        }
        if (!registered) {
            worker.quitSafely()
            Log.w(TAG, "Heart rate listener not registered")
            return false
        }
        thread = worker
        Log.i(TAG, "Recording heart rate from ${sensor.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        worker.quitSafely()
        thread = null
        Log.i(TAG, "Stopped recording heart rate")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HEART_RATE) return
        // No contact and unreliable readings carry a rate of zero or a flag; both are skipped.
        val bpm = event.values.firstOrNull()?.toInt() ?: return
        if (event.accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT ||
            event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            bpm !in WearLinkProtocol.MIN_BPM..WearLinkProtocol.MAX_BPM
        ) {
            noteSkipped(event.accuracy, bpm)
            return
        }

        val at = SensorTime.epochMillisOf(event.timestamp)
        // The cached reading handed to a new listener carries the time the sensor last ran.
        if (SensorTime.isStale(at)) return
        if (at - lastStoredAt < MIN_SAMPLE_GAP_MILLIS) return
        lastStoredAt = at
        store.insert(at, bpm)
        Log.d(TAG, "Stored $bpm bpm at $at")

        if (at - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = at
            store.prune(at)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private var lastSkipLogAt = 0L

    /** Off the wrist the sensor keeps reporting; one line a minute says so without flooding. */
    private fun noteSkipped(accuracy: Int, bpm: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSkipLogAt < SKIP_LOG_EVERY_MILLIS) return
        lastSkipLogAt = now
        Log.d(TAG, "Skipping readings: accuracy=$accuracy bpm=$bpm")
    }

    private companion object {
        const val TAG = "HeartRateRecorder"

        /** One reading a second from the sensor; the store keeps one in ten. */
        const val SAMPLING_PERIOD_MICROS = 1_000_000

        /** How long the sensor hub may hold readings before waking the CPU. */
        const val MAX_REPORT_LATENCY_MICROS = 60 * 1_000_000

        /** Stored sample spacing. Enough for resting and daily curves; a tenth of the bytes. */
        const val MIN_SAMPLE_GAP_MILLIS = 10_000L

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
        const val SKIP_LOG_EVERY_MILLIS = 60_000L
    }
}
