package tech.mmarca.openvitals.wear

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The watch's minute features against the shared vector
 * `tool/sleep_accel_fixture/feature_vector.json`, whose expected lines come
 * from the Python mirror (`features.py --make-vector`). Either side drifting
 * from `docs/engineering/sleep-minute-features.md` fails here or in
 * `features.py --self-check`.
 */
class SleepMinuteFeatureVectorTest {

    private val vector = JSONObject(File("../tool/sleep_accel_fixture/feature_vector.json").readText())

    @Test
    fun `the aggregator reproduces every expected line`() {
        val aggregator = MinuteAggregator(
            offsetSecondsAt = { vector.getInt("offsetSeconds") },
            isHeartRateRecording = { vector.getBoolean("hrRecording") },
        )
        val events = vector.getJSONArray("events").let { array -> List(array.length()) { array.getJSONObject(it) } }
            .sortedBy { it.getLong("t") }
        val heart = vector.getJSONArray("heartRate").let { array -> List(array.length()) { array.getJSONArray(it) } }
            .sortedBy { it.getLong(0) }
        val samples = vector.getJSONArray("samples")
        var eventIndex = 0
        var heartIndex = 0
        var lastStart = Long.MIN_VALUE
        val lines = ArrayList<String>()
        for (index in 0 until samples.length()) {
            val sample = samples.getJSONArray(index)
            val t = sample.getLong(0)
            while (eventIndex < events.size && events[eventIndex].getLong("t") <= t) {
                val event = events[eventIndex++]
                when (event.getString("type")) {
                    "worn" -> aggregator.onWorn(event.getLong("t"), event.getBoolean("value"))
                    "charging" -> aggregator.onCharging(event.getLong("t"), event.getBoolean("value"))
                    "screenOn" -> aggregator.onScreenOn(event.getLong("t"))
                    "hrContact" -> aggregator.onHeartRateContact(event.getLong("t"), event.getBoolean("value"))
                }
            }
            while (heartIndex < heart.size && heart[heartIndex].getLong(0) <= t) {
                val beat = heart[heartIndex++]
                aggregator.onHeartRate(beat.getLong(0), beat.getInt(1))
            }
            // Python closes a minute when a later minute's sample arrives; the lag does the same here.
            val start = t - t % MinuteAggregator.MINUTE_MILLIS
            if (start > lastStart) {
                lines += aggregator.close(start + 90_000L).map(WearLinkProtocol::formatSleepMinute)
                lastStart = start
            }
            aggregator.onAcceleration(t, sample.getDouble(1), sample.getDouble(2), sample.getDouble(3))
        }
        lines += aggregator.close(Long.MAX_VALUE).map(WearLinkProtocol::formatSleepMinute)

        val expected = vector.getJSONArray("expected").let { array -> List(array.length()) { array.getString(it) } }
        assertEquals(expected.joinToString("\n"), lines.joinToString("\n"))
    }
}
