package tech.mmarca.openvitals.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.util.TimeZone

/**
 * Records the sleep pipeline's input, one row per clock minute, into the
 * [SleepMinuteStore]. The math is [MinuteAggregator]'s; this class is the
 * Android around it: the batched accelerometer, the off-body sensor, the
 * charger and screen broadcasts, and the heart rate the other recorder
 * hands over. Runs inside `WearAppService`, so it outlives the activity.
 *
 * The accelerometer needs no permission. It is registered batched at five
 * readings a second with the same report latency as the heart rate, so the
 * two share their wake-ups. On a watch whose hub lets a still arm sleep for
 * minutes, that is not enough: while bedtime mode is on, the watch is worn
 * and off the charger, a wake lock keeps the processor up so every minute
 * is recorded whole ([BedtimeHold]). Everything runs on one handler thread;
 * the heart rate recorder posts to it.
 */
class SleepMinuteRecorder(
    private val context: Context,
    private val store: SleepMinuteStore,
    /** Whether heart rate is being recorded: only then is a minute without one a sign of no wrist. */
    isHeartRateRecording: () -> Boolean,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val aggregator = MinuteAggregator(
        offsetSecondsAt = { TimeZone.getDefault().getOffset(it) / 1000 },
        isHeartRateRecording = isHeartRateRecording,
    )
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastPrunedAt = 0L

    // Read and written on the handler thread only.
    private var worn = true
    private var charging = false
    private var bedtime: BedtimeMonitor? = null
    private var hold = BedtimeHold.OFF
    private var holdRenewedAt = 0L
    private val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        .apply { setReferenceCounted(false) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = System.currentTimeMillis()
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> aggregator.onScreenOn(now)
                Intent.ACTION_POWER_CONNECTED -> setCharging(now, true)
                Intent.ACTION_POWER_DISCONNECTED -> setCharging(now, false)
            }
        }
    }

    val isRunning: Boolean
        get() = thread != null

    /** Starts recording. False when the watch has no accelerometer or the registration failed. */
    fun start(): Boolean {
        if (thread != null) return true
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer")
            return false
        }
        val worker = HandlerThread("SleepMinuteRecorder").apply { start() }
        val workerHandler = Handler(worker.looper)
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SAMPLING_PERIOD_MICROS,
            MAX_REPORT_LATENCY_MICROS,
            workerHandler,
        )
        if (!registered) {
            worker.quitSafely()
            Log.w(TAG, "Accelerometer listener not registered")
            return false
        }
        // Optional: not every watch exposes it, and without it the heart rate tells.
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, true)?.let { offBody ->
            sensorManager.registerListener(this, offBody, SensorManager.SENSOR_DELAY_NORMAL, workerHandler)
        }
        // The sticky battery broadcast seeds the charging state; the two power actions follow it.
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        workerHandler.post { setCharging(System.currentTimeMillis(), plugged != 0) }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter, null, workerHandler)
        thread = worker
        handler = workerHandler
        workerHandler.post {
            bedtime = BedtimeMonitor(context, workerHandler) { applyHold() }.also { it.start() }
            applyHold()
        }
        Log.i(TAG, "Recording sleep minutes from ${accelerometer.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        runCatching { context.unregisterReceiver(receiver) }
        handler?.post {
            bedtime?.stop()
            bedtime = null
            if (wakeLock.isHeld) wakeLock.release()
            hold = BedtimeHold.OFF
            WearLinkState.update { it.copy(bedtimeHold = BedtimeHold.OFF) }
        }
        worker.quitSafely()
        thread = null
        handler = null
        Log.i(TAG, "Stopped recording sleep minutes")
    }

    /** A heart rate sample the other recorder stored. Any thread. */
    fun noteHeartRate(epochMillis: Long, bpm: Int) {
        handler?.post { aggregator.onHeartRate(epochMillis, bpm) }
    }

    /** The heart rate sensor lost contact with the wrist. Any thread. */
    fun noteHeartRateContact(epochMillis: Long, contact: Boolean) {
        handler?.post { aggregator.onHeartRateContact(epochMillis, contact) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val at = SensorTime.epochMillisOf(event.timestamp)
        if (SensorTime.isStale(at)) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size < 3) return
                aggregator.onAcceleration(
                    at,
                    event.values[0] / GRAVITY,
                    event.values[1] / GRAVITY,
                    event.values[2] / GRAVITY,
                )
            }
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> {
                // 1 on the body, 0 off it.
                worn = (event.values.firstOrNull() ?: 1f) >= 0.5f
                aggregator.onWorn(at, worn)
                applyHold()
            }
            else -> return
        }
        closeMinutes(System.currentTimeMillis())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun setCharging(now: Long, plugged: Boolean) {
        charging = plugged
        aggregator.onCharging(now, plugged)
        applyHold()
    }

    /**
     * Takes or drops the wake lock for the current state. Taken with a
     * short timeout and renewed at most once a minute while holding, so a
     * stuck thread or a missed broadcast costs at most [HOLD_TIMEOUT_MILLIS].
     */
    private fun applyHold() {
        val next = BedtimeHold.decide(bedtime?.isBedtime == true, worn, charging)
        if (next == BedtimeHold.HOLDING) {
            val now = SystemClock.elapsedRealtime()
            if (!wakeLock.isHeld || now - holdRenewedAt >= HOLD_RENEW_EVERY_MILLIS) {
                wakeLock.acquire(HOLD_TIMEOUT_MILLIS)
                holdRenewedAt = now
            }
        } else if (wakeLock.isHeld) {
            wakeLock.release()
        }
        if (next != hold) {
            hold = next
            Log.i(TAG, "Bedtime hold: $next")
            WearLinkState.update { it.copy(bedtimeHold = next) }
        }
    }

    private fun closeMinutes(now: Long) {
        if (hold == BedtimeHold.HOLDING) applyHold()
        val closed = aggregator.close(now)
        if (closed.isEmpty()) return
        for (minute in closed) store.upsert(minute)
        Log.d(TAG, "Stored ${closed.size} minute(s) up to ${closed.last().epochMillis}")
        if (now - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = now
            store.prune(now)
        }
    }

    private companion object {
        const val TAG = "SleepMinuteRecorder"

        /** Standard gravity: the sensor reports m/s², the specification works in g. */
        const val GRAVITY = 9.80665

        /** Five readings a second: enough to count a turn in bed, a fraction of the hub's budget. */
        const val SAMPLING_PERIOD_MICROS = 200_000

        /** The same latency as the heart rate, so both batches arrive in one wake-up, inside the buffer's 48 seconds. */
        const val MAX_REPORT_LATENCY_MICROS = 40 * 1_000_000

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000

        const val WAKE_LOCK_TAG = "OpenVitals:bedtime"

        /** Several batches long, so one late batch does not drop the lock. */
        const val HOLD_TIMEOUT_MILLIS = 10L * 60 * 1000

        /** Readings arrive several times a second; the lock is renewed once a minute. */
        const val HOLD_RENEW_EVERY_MILLIS = 60L * 1000
    }
}
