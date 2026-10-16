package tech.mmarca.openvitals.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
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
 * arrive in one wake-up. Without the heart rate nothing of ours wakes the
 * processor, and a reading could come hours late, past the hour
 * [StepMinuteCounter] spreads over; so while the heart rate is not running
 * ([setHeartRateRunning]) the counter's waking variant is used, where the
 * watch has one.
 *
 * A batch is written once, a second after its last event, in one
 * transaction with the last reading. So a restart neither loses nor
 * double-counts steps: what was not written is in the next difference.
 */
class StepRecorder(
    context: Context,
    private val store: MetricStore,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val boot: Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    private var thread: HandlerThread? = null
    /** Volatile: written on the caller's thread, read by the listener on the worker. */
    @Volatile
    private var handler: Handler? = null
    private var flushPending = false
    private var counter: StepMinuteCounter? = null
    private var lastPrunedAt = 0L

    val isRunning: Boolean
        get() = thread != null

    /** False on a watch without a step counter: then nothing will ever start. */
    val hasSensor: Boolean = hasStepCounter(context)

    /** True while the heart rate's wake-ups bring the readings; false asks for the waking counter. */
    private var heartRateRunning = true

    /** Switches between the two variants of the counter. A running recorder registers again. */
    fun setHeartRateRunning(running: Boolean) {
        if (running == heartRateRunning) return
        heartRateRunning = running
        val workerHandler = handler ?: return
        sensorManager.unregisterListener(this)
        if (!register(workerHandler)) Log.w(TAG, "Step listener not registered again")
    }

    /** The waking counter while nothing else wakes the processor and the watch has one, else the non-waking one. */
    private fun sensor(): Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)?.takeIf { !heartRateRunning }
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private fun register(workerHandler: Handler): Boolean {
        val sensor = sensor() ?: return false
        val registered = try {
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                MAX_REPORT_LATENCY_MICROS,
                workerHandler,
            )
        } catch (e: SecurityException) {
            // The grant was revoked between the check and the registration.
            Log.e(TAG, "Step counter refused", e)
            false
        }
        if (registered) Log.i(TAG, "Recording steps from ${sensor.name}${if (sensor.isWakeUpSensor) " (waking)" else ""}")
        return registered
    }

    /** Starts recording. False when the watch has no step counter or the registration failed. */
    fun start(): Boolean {
        if (thread != null) return true
        if (sensor() == null) {
            Log.w(TAG, "No step counter")
            return false
        }
        val worker = HandlerThread("StepRecorder").apply { start() }
        val workerHandler = Handler(worker.looper)
        // Set before registering: the first event may arrive at once, and it schedules the flush.
        thread = worker
        handler = workerHandler
        workerHandler.post {
            counter = StepMinuteCounter(lastReading(), store.latest(WearMetrics.STEPS))
        }
        if (!register(workerHandler)) {
            thread = null
            handler = null
            worker.quitSafely()
            Log.w(TAG, "Step listener not registered")
            return false
        }
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        // Runs before the quit: quitSafely still delivers what is due now.
        handler?.post(::flush)
        worker.quitSafely()
        thread = null
        handler = null
        Log.i(TAG, "Stopped recording steps")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val counter = counter ?: return
        val total = event.values.firstOrNull()?.toLong() ?: return
        counter.onTotal(SensorTime.epochMillisOf(event.timestamp), total)
        if (!flushPending) {
            flushPending = true
            handler?.postDelayed(::flush, FLUSH_DELAY_MILLIS)
        }
    }

    /** Writes the changed minutes and the last reading in one transaction. */
    private fun flush() {
        flushPending = false
        val counter = counter ?: return
        val last = counter.last ?: return
        val rows = counter.drain()
        store.putAll(WearMetrics.STEPS, rows, STATE_KEY, "$boot,${last.total},${last.epochMillis}")
        rows.forEach(::logFinishedMinute)
        if (last.epochMillis - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = last.epochMillis
            store.prune(WearMetrics.STEPS, last.epochMillis)
        }
    }

    /**
     * Where to count from. On the same boot, the stored reading. On a new
     * boot the counter restarted at 0, so its first total is every step since
     * the boot. With no stored reading at all, the app is new on this watch:
     * the first reading is the baseline, so steps from before the install are
     * not counted.
     */
    private fun lastReading(): StepMinuteCounter.Reading? {
        val fields = store.state(STATE_KEY)?.split(',')?.mapNotNull { it.toLongOrNull() }
        if (fields == null || fields.size != 3) {
            Log.i(TAG, "No stored reading: the first reading sets the baseline")
            return null
        }
        val (storedBoot, total, at) = fields
        if (storedBoot == boot.toLong()) {
            Log.i(TAG, "Continuing from total $total")
            return StepMinuteCounter.Reading(at, total)
        }
        val bootedAt = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        Log.i(TAG, "New boot: counting from 0 at ${java.time.Instant.ofEpochMilli(bootedAt)}")
        return StepMinuteCounter.Reading(bootedAt, 0L)
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

    companion object {
        private const val TAG = "StepRecorder"
        private const val STATE_KEY = "steps"

        /** A batch arrives within a second; one write after its last event covers it. */
        private const val FLUSH_DELAY_MILLIS = 1_000L

        fun hasStepCounter(context: Context): Boolean =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

        /** The heart rate's latency, so both batches arrive in one wake-up; see `HeartRateRecorder`. */
        private const val MAX_REPORT_LATENCY_MICROS = 40 * 1_000_000

        private const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
    }
}
