package tech.mmarca.openvitals.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry

/** The cache tiers and the input fan-out, one day at a time. */
class BodyEnergyRepositoryTest {

    private var clock = Instant.parse("2026-06-01T08:00:00Z")
    private val today: LocalDate = Instant.parse("2026-06-01T08:00:00Z").atZone(TestZone).toLocalDate()

    private lateinit var heart: FakeHeartRepository
    private lateinit var timelines: BodyEnergyTimelineStore
    private lateinit var activity: ActivityRepository
    private lateinit var body: BodyRepository
    private lateinit var vitals: VitalsRepository

    @Before
    fun setUp() {
        clock = Instant.parse("2026-06-01T08:00:00Z")
        heart = FakeHeartRepository()
        timelines = BodyEnergyTimelineStore(FakeBodyEnergyTimelineDao())
        activity = emptyActivityRepository()
        body = emptyBodyRepository()
        vitals = emptyVitalsRepository()
    }

    private fun repo() = BodyEnergyRepositoryImpl(
        heartRepository = heart.repository,
        sleepRepository = emptySleepRepository(),
        activityRepository = activity,
        vitalsRepository = vitals,
        bodyRepository = body,
        healthRepository = grantedHealthRepository(),
        preferencesRepository = inMemoryPreferences(),
        baselineCacheStore = inMemoryBaselineStore(),
        timelineStore = timelines,
        now = { clock },
        zone = TestZone,
        dispatchers = TestDispatcherProvider,
    )

    private val query = BodyEnergyTimelineQuery(
        period = DatePeriod(today, today),
        range = TimeRange.DAY,
    )

    @Test
    fun `the day timeline uses raw full heart rate samples, never the summarised read`() = runTest {
        repo().loadTimeline(query)

        coVerify(exactly = 1) { heart.repository.loadRawHeartRateSamplesForDayGraph(today) }
        // The only baseline-window instant read is the observed-max scan over the 28 days before today.
        coVerify(exactly = 1) {
            heart.repository.loadHeartRateSamples(any<Instant>(), any<Instant>())
        }
    }

    @Test
    fun `the energy-balance inputs are read for the day`() = runTest {
        repo().loadTimeline(query)

        coVerify(exactly = 1) { activity.loadActivityProgress(today) }
        coVerify(exactly = 1) { body.loadLatestBMR() }
    }

    @Test
    fun `the respiratory baseline is the median daily rate over the window`() = runTest {
        // It used to be copied from a cache entry that never held one, so it was always null.
        coEvery { vitals.loadDailyVitals(VitalsPeriodMetric.RESPIRATORY_RATE, any(), any()) } returns listOf(
            DailyVitalPoint(today.minusDays(3), 13.0, 4),
            DailyVitalPoint(today.minusDays(2), 15.0, 4),
            DailyVitalPoint(today.minusDays(1), 14.0, 4),
        )

        val day = repo().loadTimeline(query).days.single()

        assertTrue(day.inputSummary.hasRespiratoryBaseline)
        coVerify(exactly = 1) {
            vitals.loadDailyVitals(VitalsPeriodMetric.RESPIRATORY_RATE, today.minusDays(28), today.minusDays(1))
        }
    }

    @Test
    fun `the day's respiratory records are read without a baseline`() = runTest {
        // A manual entry showed as "0 records": the read was gated on a baseline that never existed.
        coEvery { vitals.loadRespiratoryRate(today, today) } returns listOf(
            RespiratoryRateEntry(
                time = today.atStartOfDay(TestZone).plusHours(5).toInstant(),
                breathsPerMinute = 14.0,
                source = "test",
            )
        )

        val day = repo().loadTimeline(query).days.single()

        assertEquals(1, day.inputSummary.respiratorySampleCount)
        assertFalse(day.inputSummary.hasRespiratoryBaseline)
    }

    @Test
    fun `a fresh cached timeline is served without recomputing`() = runTest {
        val r = repo()
        r.loadTimeline(query)
        assertEquals(1, heart.dayGraphCalls)

        // Same instant → within the 15-minute freshness window → cache hit.
        r.loadTimeline(query)

        assertEquals("timeline should be served cached", 1, heart.dayGraphCalls)
    }

    @Test
    fun `a stale timeline recomputes but reuses the fresh baseline`() = runTest {
        val r = repo()
        r.loadTimeline(query)
        assertEquals(1, heart.dayGraphCalls)
        assertEquals(1, heart.dailyRestingCalls)

        // 20 minutes later: today's timeline is stale, but the baseline is still fresh and must be reused.
        clock = clock.plusSeconds(20 * 60)
        r.loadTimeline(query)

        assertEquals("stale timeline recomputes", 2, heart.dayGraphCalls)
        assertEquals("baseline reused, not recomputed", 1, heart.dailyRestingCalls)
    }

    @Test
    fun `a forced refresh recomputes even within the freshness window`() = runTest {
        val r = repo()
        r.loadTimeline(query)
        assertEquals(1, heart.dayGraphCalls)

        r.loadTimeline(query.copy(refreshMode = RefreshMode.FORCE))

        assertEquals("force bypasses the cache", 2, heart.dayGraphCalls)
    }
}
