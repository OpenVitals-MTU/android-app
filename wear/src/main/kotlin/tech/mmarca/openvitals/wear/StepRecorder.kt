package tech.mmarca.openvitals.wear

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Writes the step counter's (`TYPE_STEP_COUNTER`) steps to the [MetricStore],
 * one row per clock minute. [StepMinuteCounter] does the arithmetic. Runs in
 * `WearAppService`, so it outlives the activity.
 *
 * The counter is the non-waking variant: it counts while the processor
 * sleeps. Every reading carries the running total, so a dropped batch loses
 * resolution, not steps. It reports with the heart rate's latency, so both
 * arrive in one wake-up. The last total is kept per boot, so a service
 * restart neither loses nor double-counts steps.
 */
class StepRecorder(
    context: Context,
    private val store: MetricStore,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val boot: Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    private var thread: HandlerThread? = null
    private var counter: StepMinuteCounter? = null
    private var lastPrunedAt = 0L

    val isRunning: Boolean
        get() = thread != null

    /** Starts recording. False when the watch has no step counter or the registration failed. */
    fun start(): Boolean {
        if (thread != null) return true
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            Log.w(TAG, "No step counter")
            return false
        }
        val worker = HandlerThread("StepRecorder").apply { start() }
        Handler(worker.looper).post {
            // The total is only comparable within one boot; after a reboot the counter starts again.
            val lastTotal = if (prefs.getInt(KEY_BOOT, NO_BOOT) == boot) prefs.getLong(KEY_TOTAL, 0L) else null
            counter = StepMinuteCounter(lastTotal, store.latest(WearMetrics.STEPS))
            Log.i(TAG, if (lastTotal == null) "New boot: the first reading sets the baseline" else "Continuing from total $lastTotal")
        }
        val registered = try {
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                MAX_REPORT_LATENCY_MICROS,
                Handler(worker.looper),
            )
        } catch (e: SecurityException) {
            // The grant was revoked between the check and the registration.
            Log.e(TAG, "Step counter refused", e)
            false
        }
        if (!registered) {
            worker.quitSafely()
            Log.w(TAG, "Step listener not registered")
            return false
        }
        thread = worker
        Log.i(TAG, "Recording steps from ${sensor.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        worker.quitSafely()
        thread = null
        Log.i(TAG, "Stopped recording steps")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val counter = counter ?: return
        val total = event.values.firstOrNull()?.toLong() ?: return
        val at = SensorTime.epochMillisOf(event.timestamp)
        val minute = counter.onTotal(at, total)
        prefs.edit().putInt(KEY_BOOT, boot).putLong(KEY_TOTAL, total).apply()
        if (minute == null) return
        store.put(WearMetrics.STEPS, minute)
        logFinishedMinute(minute)

        if (at - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = at
            store.prune(WearMetrics.STEPS, at)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private var lastRow: WearLinkProtocol.StepMinute? = null

    /**
     * Logs one line per minute once the next minute starts. The watch has no
     * steps screen yet; testers read `adb logcat -s StepRecorder`. A late
     * batch may still add to a logged minute.
     */
    private fun logFinishedMinute(row: WearLinkProtocol.StepMinute) {
        val previous = lastRow
        lastRow = row
        if (previous == null || previous.epochMillis == row.epochMillis) return
        Log.i(TAG, "${java.time.Instant.ofEpochMilli(previous.epochMillis)}: ${previous.steps} steps")
    }

    private companion object {
        const val TAG = "StepRecorder"
        const val PREFS_FILE = "step_counter"
        const val KEY_BOOT = "boot"
        const val KEY_TOTAL = "total"
        const val NO_BOOT = Int.MIN_VALUE

        /** The heart rate's latency, so both batches arrive in one wake-up; see `HeartRateRecorder`. */
        const val MAX_REPORT_LATENCY_MICROS = 40 * 1_000_000

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
    }
}
