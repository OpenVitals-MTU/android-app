package tech.mmarca.openvitals.wear

import android.content.ContentValues
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/** The store against real SQLite, on a device; an in-memory database per test. */
@RunWith(AndroidJUnit4::class)
class MetricStoreTest {

    private lateinit var store: MetricStore

    @Before
    fun open() {
        store = MetricStore(ApplicationProvider.getApplicationContext(), name = null)
    }

    @After
    fun close() = store.close()

    @Test
    fun pagesAreOldestFirstNewerThanTheCursorAndLimited() {
        store.putAll(WearMetrics.HEART_RATE, listOf(hr(3_000, 70), hr(1_000, 60), hr(2_000, 65), hr(4_000, 75)))

        assertEquals(listOf(hr(2_000, 65), hr(3_000, 70)), store.since(WearMetrics.HEART_RATE, 1_000, limit = 2))
        assertEquals(hr(4_000, 75), store.latest(WearMetrics.HEART_RATE))
        assertEquals(4L, store.count(WearMetrics.HEART_RATE))
    }

    @Test
    fun aSampleAtTheSameTimeIsKeptAMinuteIsReplaced() {
        store.put(WearMetrics.HEART_RATE, hr(1_000, 60))
        store.put(WearMetrics.HEART_RATE, hr(1_000, 99))
        assertEquals(listOf(hr(1_000, 60)), store.since(WearMetrics.HEART_RATE, 0, 10))

        store.put(WearMetrics.SLEEP_MINUTES, minute(60_000, samples = 30))
        store.put(WearMetrics.SLEEP_MINUTES, minute(60_000, samples = 374))
        assertEquals(listOf(374), store.since(WearMetrics.SLEEP_MINUTES, 0, 10).map { it.sampleCount })
    }

    @Test
    fun metricsDoNotSeeEachOther() {
        store.put(WearMetrics.HEART_RATE, hr(60_000, 60))
        store.put(WearMetrics.SLEEP_MINUTES, minute(60_000, samples = 10))

        assertEquals(1L, store.count(WearMetrics.HEART_RATE))
        assertEquals(1L, store.count(WearMetrics.SLEEP_MINUTES))
        store.prune(WearMetrics.HEART_RATE, nowEpochMillis = 60_001 + WearMetric.WEEK_MILLIS)
        assertEquals(0L, store.count(WearMetrics.HEART_RATE))
        assertEquals(1L, store.count(WearMetrics.SLEEP_MINUTES))
    }

    @Test
    fun pruningKeepsTheRetentionWindow() {
        val now = 10 * WearMetric.WEEK_MILLIS
        store.putAll(WearMetrics.HEART_RATE, listOf(hr(now - WearMetric.WEEK_MILLIS - 1, 60), hr(now - WearMetric.WEEK_MILLIS, 61)))
        store.prune(WearMetrics.HEART_RATE, now)
        assertEquals(listOf(hr(now - WearMetric.WEEK_MILLIS, 61)), store.since(WearMetrics.HEART_RATE, 0, 10))
    }

    @Test
    fun aRowThisBuildCannotReadIsSkippedNotFatal() {
        store.put(WearMetrics.HEART_RATE, hr(1_000, 60))
        store.writableDatabase.insert(
            MetricStore.TABLE,
            null,
            ContentValues().apply {
                put("metric", WearMetrics.HEART_RATE.key)
                put("time_ms", 2_000L)
                put("line", "HR from a future build")
            },
        )
        assertEquals(listOf(hr(1_000, 60)), store.since(WearMetrics.HEART_RATE, 0, 10))
        assertNull(store.latest(WearMetrics.HEART_RATE))
    }

    private fun hr(at: Long, bpm: Int) = WearLinkProtocol.HeartRateSample(at, bpm)

    private fun minute(at: Long, samples: Int) = WearLinkProtocol.SleepMinute(
        epochMillis = at,
        kind = WearLinkProtocol.MinuteKind.RAW,
        offsetSeconds = 0,
        flags = 0,
        sampleCount = samples,
        movement10 = 0,
        bpm = null,
        heartRateSd10 = null,
        heartRateSamples = 0,
        meanMilliG = intArrayOf(0, 0, 1000),
        sdMilliG = intArrayOf(0, 0, 0),
        zAngleMin = null,
        zAngleMax = null,
        zAngleDelta10 = null,
    )
}
