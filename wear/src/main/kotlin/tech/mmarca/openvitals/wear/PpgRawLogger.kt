package tech.mmarca.openvitals.wear

import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A research spike, debuggable builds only: dumps the Samsung raw PPG
 * sensor (`com.samsung.sensor.hr_raw`, 25 Hz, sixteen undocumented floats,
 * gated by the heart rate permission the app already holds) to a CSV under
 * the app's files, so the channel layout and any beat markers can be read
 * off the watch with `tool/ppg_raw/inspect.py`. Nothing in the app depends
 * on it. Started and stopped by hand from the status screen; stops itself
 * after a night or when the file reaches its cap.
 *
 * The sensor is found by its string type, never a numeric constant, so a
 * watch without it simply reports the spike unavailable.
 */
class PpgRawLogger(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var thread: HandlerThread? = null
    private var writer: BufferedWriter? = null
    private var file: File? = null
    private var bytes = 0L
    private var events = 0L
    private var startedAt = 0L

    /** Only a debuggable build may run it, and only a watch that has the sensor. */
    val isAvailable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && sensor() != null

    val isRunning: Boolean
        get() = thread != null

    /** The file being written, or null. */
    val currentFile: File?
        get() = file

    private fun sensor(): Sensor? = sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull { it.stringType == SENSOR_TYPE }

    /** Starts a new log; false when unavailable or refused. */
    fun start(): Boolean {
        if (thread != null) return true
        if (!isAvailable) return false
        val sensor = sensor() ?: return false
        val dir = File(context.filesDir, "ppg_raw").apply { mkdirs() }
        val target = File(dir, "ppg_raw_${SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())}.csv")
        val out = try {
            BufferedWriter(FileWriter(target), 1 shl 16)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open ${target.name}", e)
            return false
        }
        val worker = HandlerThread("PpgRawLogger").apply { start() }
        val registered = try {
            sensorManager.registerListener(this, sensor, sensor.minDelay.coerceAtLeast(1), MAX_REPORT_LATENCY_MICROS, Handler(worker.looper))
        } catch (e: SecurityException) {
            Log.e(TAG, "Raw PPG refused", e)
            false
        }
        if (!registered) {
            worker.quitSafely()
            runCatching { out.close() }
            target.delete()
            return false
        }
        out.write("elapsedNanos,epochMillis,accuracy")
        for (index in 0 until MAX_VALUES) out.write(",v$index")
        out.newLine()
        writer = out
        file = target
        bytes = 0
        events = 0
        startedAt = System.currentTimeMillis()
        thread = worker
        Log.i(TAG, "Logging ${sensor.name} (${sensor.stringType}, ${sensor.minDelay} µs, fifo ${sensor.fifoMaxEventCount}) to ${target.name}")
        return true
    }

    fun stop() {
        val worker = thread ?: return
        sensorManager.unregisterListener(this)
        Handler(worker.looper).post {
            runCatching { writer?.flush(); writer?.close() }
            writer = null
            worker.quitSafely()
        }
        thread = null
        Log.i(TAG, "Stopped after $events events, $bytes bytes")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val out = writer ?: return
        val line = StringBuilder(160)
        line.append(event.timestamp).append(',').append(SensorTime.epochMillisOf(event.timestamp)).append(',').append(event.accuracy)
        for (index in 0 until MAX_VALUES) {
            line.append(',')
            if (index < event.values.size) line.append(event.values[index])
        }
        try {
            out.write(line.toString())
            out.newLine()
        } catch (e: Exception) {
            Log.e(TAG, "Write failed", e)
            stop()
            return
        }
        bytes += line.length + 1
        events++
        if (bytes >= MAX_BYTES || System.currentTimeMillis() - startedAt >= MAX_DURATION_MILLIS) {
            Log.i(TAG, "Cap reached")
            stop()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "PpgRawLogger"
        const val SENSOR_TYPE = "com.samsung.sensor.hr_raw"
        private const val MAX_VALUES = 16
        private const val MAX_BYTES = 200L * 1024 * 1024
        private const val MAX_DURATION_MILLIS = 8L * 60 * 60 * 1000
        private const val MAX_REPORT_LATENCY_MICROS = 20 * 1_000_000
    }
}
