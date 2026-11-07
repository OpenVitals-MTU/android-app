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

/**
 * Whether the watch is on the wrist: the one owner of the off-body sensor.
 * Hands each reading to [onWorn] (the sleep minute recorder) and tells
 * [onHeartRateWanted] when the heart rate sensor should run or stop; the
 * decision is [WornGate]'s. Both callbacks run on this monitor's thread.
 *
 * The off-body sensor is optional. Without it nothing is reported, and the
 * heart rate always runs.
 */
class WornMonitor(
    private val context: Context,
    private val onWorn: (epochMillis: Long, worn: Boolean) -> Unit,
    private val onHeartRateWanted: (Boolean) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gate = WornGate()
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    // Read and written on the handler thread only.
    private var wanted = true

    private val check = Runnable { apply(System.currentTimeMillis()) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_POWER_DISCONNECTED -> {
                    val now = System.currentTimeMillis()
                    gate.onWakeUp(now)
                    apply(now)
                }
            }
        }
    }

    fun start() {
        if (thread != null) return
        val offBody = sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, true)
        if (offBody == null) {
            Log.i(TAG, "No off-body sensor; heart rate stays on")
            return
        }
        val worker = HandlerThread("WornMonitor").apply { start() }
        val workerHandler = Handler(worker.looper)
        if (!sensorManager.registerListener(this, offBody, SensorManager.SENSOR_DELAY_NORMAL, workerHandler)) {
            worker.quitSafely()
            Log.w(TAG, "Off-body listener not registered; heart rate stays on")
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter, null, workerHandler)
        thread = worker
        handler = workerHandler
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        runCatching { context.unregisterReceiver(receiver) }
        handler?.removeCallbacks(check)
        worker.quitSafely()
        thread = null
        handler = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) return
        val at = SensorTime.epochMillisOf(event.timestamp)
        // 1 on the body, 0 off it.
        val worn = (event.values.firstOrNull() ?: 1f) >= 0.5f
        onWorn(at, worn)
        gate.onWorn(System.currentTimeMillis(), worn)
        apply(System.currentTimeMillis())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Reports a change of [WornGate.heartRateWanted] and schedules the next check. */
    private fun apply(now: Long) {
        val next = gate.heartRateWanted(now)
        if (next != wanted) {
            wanted = next
            Log.i(TAG, if (next) "Heart rate on: worn or woken" else "Heart rate off: not worn")
            onHeartRateWanted(next)
        }
        val workerHandler = handler ?: return
        workerHandler.removeCallbacks(check)
        gate.nextChangeAt()?.takeIf { it > now }?.let { workerHandler.postDelayed(check, it - now) }
    }

    private companion object {
        const val TAG = "WornMonitor"
    }
}
