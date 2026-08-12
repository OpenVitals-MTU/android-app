package tech.mmarca.openvitals.features.watches

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample

/**
 * Port of the aggregation rules under the Flutter build's
 * `watch_metrics_view_model.dart` (asserted there through
 * `watch_data_screen_test.dart`): the latest reading per metric, today's
 * series over the local day, and the weekly intensity-minutes total — a sum
 * of each day's FINAL running total, vigorous counted double.
 */
class WatchMetricsTest {

    private class FakeWellnessRepository : GarminWellnessRepository {
        val samples = mutableListOf<GarminWellnessSample>()

        override suspend fun upsert(samples: List<GarminWellnessSample>) {
            this.samples += samples
        }

        override suspend fun samplesBetween(
            metric: GarminWellnessMetric,
            from: Instant,
            to: Instant,
        ): List<GarminWellnessSample> =
            samples
                .filter { it.metric == metric && it.time >= from && it.time < to }
                .sortedBy { it.time }

        override suspend fun latest(metric: GarminWellnessMetric): GarminWellnessSample? =
            samples.filter { it.metric == metric }.maxByOrNull { it.time }

        override suspend fun countFor(metric: GarminWellnessMetric): Long =
            samples.count { it.metric == metric }.toLong()
    }

    private val zone = ZoneId.of("UTC")

    /** A fixed Wednesday noon, so "this week" started Monday the 9th. */
    private val now: Instant = Instant.parse("2026-06-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, zone)

    private val repo = FakeWellnessRepository()

    private fun seed(metric: GarminWellnessMetric, at: Instant, value: Long) {
        repo.samples += GarminWellnessSample(metric = metric, time = at, value = value)
    }

    @Test
    fun `resolves the latest reading of each stored metric`() = runTest {
        seed(GarminWellnessMetric.SLEEP_SCORE, now.minusSeconds(7200), 60)
        seed(GarminWellnessMetric.SLEEP_SCORE, now.minusSeconds(3600), 71)
        seed(GarminWellnessMetric.SLEEP_AWAKE_SECONDS, now.minusSeconds(3600), 1020)

        val metrics = loadWatchMetrics(repo, clock)

        assertEquals(71L, metrics.valueOf(GarminWellnessMetric.SLEEP_SCORE))
        assertEquals(1020L, metrics.valueOf(GarminWellnessMetric.SLEEP_AWAKE_SECONDS))
        assertNull(metrics.valueOf(GarminWellnessMetric.STRESS))
    }

    @Test
    fun `an empty table is empty, not a map of blanks`() = runTest {
        val metrics = loadWatchMetrics(repo, clock)

        assertTrue(metrics.isEmpty)
        assertNull(metrics.intensityMinutesWeek)
    }

    @Test
    fun `names what the watch never sent, in declaration order`() = runTest {
        seed(GarminWellnessMetric.STRESS, now.minusSeconds(60), 30)

        val metrics = loadWatchMetrics(repo, clock)
        val missing = metrics.missingFrom(
            listOf(
                GarminWellnessMetric.STRESS,
                GarminWellnessMetric.BODY_ENERGY,
                GarminWellnessMetric.SLEEP_SCORE,
            ),
        )

        assertEquals(
            listOf(GarminWellnessMetric.BODY_ENERGY, GarminWellnessMetric.SLEEP_SCORE),
            missing,
        )
    }

    @Test
    fun `today's series is windowed to the local day`() = runTest {
        seed(GarminWellnessMetric.STRESS, Instant.parse("2026-06-09T23:59:00Z"), 90)
        seed(GarminWellnessMetric.STRESS, Instant.parse("2026-06-10T08:00:00Z"), 20)
        seed(GarminWellnessMetric.STRESS, Instant.parse("2026-06-10T10:00:00Z"), 40)

        val metrics = loadWatchMetrics(repo, clock)

        assertEquals(listOf(20L, 40L), metrics.stressToday.map { it.value })
    }

    @Test
    fun `vigorous intensity minutes count double, as Garmin counts them`() = runTest {
        seed(GarminWellnessMetric.MODERATE_MINUTES, now.minusSeconds(600), 30)
        seed(GarminWellnessMetric.VIGOROUS_MINUTES, now.minusSeconds(600), 10)

        val metrics = loadWatchMetrics(repo, clock)

        assertEquals(30L + 2 * 10L, metrics.intensityMinutesWeek)
    }

    @Test
    fun `the weekly total sums each day's FINAL running total`() = runTest {
        // Monday: the running total climbed to 40 and reset overnight.
        seed(GarminWellnessMetric.MODERATE_MINUTES, Instant.parse("2026-06-08T10:00:00Z"), 25)
        seed(GarminWellnessMetric.MODERATE_MINUTES, Instant.parse("2026-06-08T20:00:00Z"), 40)
        // Today: the running total is at 20.
        seed(GarminWellnessMetric.MODERATE_MINUTES, Instant.parse("2026-06-10T08:00:00Z"), 10)
        seed(GarminWellnessMetric.MODERATE_MINUTES, Instant.parse("2026-06-10T11:00:00Z"), 20)
        // Last week must not leak in.
        seed(GarminWellnessMetric.MODERATE_MINUTES, Instant.parse("2026-06-05T20:00:00Z"), 99)

        val metrics = loadWatchMetrics(repo, clock)

        // 40 (Monday's final) + 20 (today's final) — never 25 + 10, and never
        // the bare latest reading alone.
        assertEquals(60L, metrics.intensityMinutesWeek)
    }

    @Test
    fun `no intensity samples at all means no weekly figure`() = runTest {
        seed(GarminWellnessMetric.SLEEP_SCORE, now.minusSeconds(3600), 71)

        val metrics = loadWatchMetrics(repo, clock)

        assertNull(metrics.intensityMinutesWeek)
    }
}
