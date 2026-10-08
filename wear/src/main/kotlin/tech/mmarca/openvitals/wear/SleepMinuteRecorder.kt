package tech.mmarca.openvitals.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.TimeZone

/**
 * Records the sleep estimator's input, one row per minute, into the
 * [SleepMinuteStore]: movement from the batched accelerometer, the minute's
 * mean heart rate from the [HeartRateStore], whether the watch was worn, and
 * whether the screen woke. Runs inside `WearAppService`, next to the heart
 * rate recorder, so it outlives the activity.
 *
 * A minute is `U` (unmeasurable) when the off-body sensor saw the watch off
 * the wrist, or when heart rate is being recorded and the minute got none:
 * off the wrist the sensor reports no contact, which the heart rate recorder
 * skips. It is `A` (awake) when the screen became interactive during it. The
 * rest are `R` (raw), left for the phone to classify.
 *
 * The accelerometer needs no permission. It is registered batched at five
 * readings a second with the same minute of report latency as the heart
 * rate, so the two share their wake-ups.
 */
class SleepMinuteRecorder(
    private val context: Context,
    private val store: SleepMinuteStore,
    private val heartRates: HeartRateStore,
    /** Whether heart rate is being recorded: only then is a minute without one a sign of no wrist. */
    private val isHeartRateRecording: () -> Boolean,
    private val aggregator: MinuteAggregator = MinuteAggregator(),
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastPrunedAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) aggregator.onScreenOn(System.currentTimeMillis())
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
        context.registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), null, workerHandler)
        thread = worker
        handler = workerHandler
        Log.i(TAG, "Recording sleep minutes from ${accelerometer.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        runCatching { context.unregisterReceiver(screenReceiver) }
        worker.quitSafely()
        thread = null
        handler = null
        Log.i(TAG, "Stopped recording sleep minutes")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val at = SensorTime.epochMillisOf(event.timestamp)
        if (SensorTime.isStale(at)) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size < 3) return
                aggregator.onAcceleration(at, event.values[0], event.values[1], event.values[2])
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
        val zone = TimeZone.getDefault()
        val heartRateRecording = isHeartRateRecording()
        for (minute in closed) {
            val bpm = heartRates.averageBetween(minute.startEpochMillis, minute.startEpochMillis + MinuteAggregator.MINUTE_MILLIS)
            val kind = when {
                minute.offBody || (heartRateRecording && bpm == null) -> WearLinkProtocol.MinuteKind.UNMEASURABLE
                minute.screenOn -> WearLinkProtocol.MinuteKind.AWAKE
                else -> WearLinkProtocol.MinuteKind.RAW
            }
            store.upsert(
                WearLinkProtocol.SleepMinute(
                    epochMillis = minute.startEpochMillis,
                    kind = kind,
                    movement = minute.movement,
                    bpm = bpm,
                    offsetSeconds = zone.getOffset(minute.startEpochMillis) / 1000,
                ),
            )
        }
        Log.d(TAG, "Stored ${closed.size} minute(s) up to ${closed.last().startEpochMillis}")
        if (now - lastPrunedAt > PRUNE_EVERY_MILLIS) {
            lastPrunedAt = now
            store.prune(now)
        }
    }

    private companion object {
        const val TAG = "SleepMinuteRecorder"

        /** Five readings a second: enough to count a turn in bed, a fraction of the hub's budget. */
        const val SAMPLING_PERIOD_MICROS = 200_000

        /** The same latency as the heart rate, so both batches arrive in one wake-up. */
        const val MAX_REPORT_LATENCY_MICROS = 60 * 1_000_000

        const val PRUNE_EVERY_MILLIS = 6L * 60 * 60 * 1000
    }
}
