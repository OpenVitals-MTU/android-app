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
 * readings a second with the same minute of report latency as the heart
 * rate, so the two share their wake-ups. Everything runs on one handler
 * thread; the heart rate recorder posts to it.
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = System.currentTimeMillis()
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> aggregator.onScreenOn(now)
                Intent.ACTION_POWER_CONNECTED -> aggregator.onCharging(now, true)
                Intent.ACTION_POWER_DISCONNECTED -> aggregator.onCharging(now, false)
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
        workerHandler.post { aggregator.onCharging(System.currentTimeMillis(), plugged != 0) }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter, null, workerHandler)
        thread = worker
        handler = workerHandler
        Log.i(TAG, "Recording sleep minutes from ${accelerometer.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        runCatching { context.unregisterReceiver(receiver) }
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
                aggregator.onWorn(at, (event.values.firstOrNull() ?: 1f) >= 0.5f)
            }
            else -> return
        }
        closeMinutes(System.currentTimeMillis())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun closeMinutes(now: Long) {
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

        /** The same latency as the heart rate, so both batches arrive in one wake-up. */
        const val MAX_REPORT_LATENCY_MICROS = 60 * 1_000_000

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
    }
}
