package tech.mmarca.openvitals.wear

import android.os.SystemClock

/** Sensor events are stamped in elapsed-realtime nanoseconds; a batched one is older than now. */
object SensorTime {
    fun epochMillisOf(elapsedRealtimeNanos: Long): Long {
        val elapsedNowMillis = SystemClock.elapsedRealtime()
        val eventElapsedMillis = elapsedRealtimeNanos / 1_000_000L
        return System.currentTimeMillis() - (elapsedNowMillis - eventElapsedMillis)
    }

    /**
     * True for an event too old to be part of a batch: a freshly registered
     * listener is handed the sensor's last cached reading, stamped whenever
     * the sensor last ran, which can be hours earlier. A batch is at most a
     * minute late; anything beyond [MAX_EVENT_AGE_MILLIS] is that stale reading.
     */
    fun isStale(eventEpochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        nowEpochMillis - eventEpochMillis > MAX_EVENT_AGE_MILLIS

    const val MAX_EVENT_AGE_MILLIS: Long = 5 * 60 * 1000
}
