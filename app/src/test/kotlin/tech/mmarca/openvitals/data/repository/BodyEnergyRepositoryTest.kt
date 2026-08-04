package tech.mmarca.openvitals.data.repository

import io.mockk.coVerify
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.domain.model.RefreshMode

/** The cache tiers and the input fan-out, one day at a time. */
class BodyEnergyRepositoryTest {

    private var clock = Instant.parse("2026-06-01T08:00:00Z")
    private val today: LocalDate = Instant.parse("2026-06-01T08:00:00Z").atZone(TestZone).toLocalDate()

    private lateinit var heart: FakeHeartRepository
    private lateinit var timelines: BodyEnergyTimelineStore
    private lateinit var activity: ActivityRepository
    private lateinit var body: BodyRepository

    @Before
    fun setUp() {
        clock = Instant.parse("2026-06-01T08:00:00Z")
        heart = FakeHeartRepository()
        timelines = BodyEnergyTimelineStore(FakeBodyEnergyTimelineDao())
        activity = emptyActivityRepository()
        body = emptyBodyRepository()
    }

    private fun repo() = BodyEnergyRepositoryImpl(
        heartRepository = heart.repository,
        sleepRepository = emptySleepRepository(),
        activityRepository = activity,
        vitalsRepository = emptyVitalsRepository(),
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
        // The only baseline-window instant read is the observed-max scan, which
        // covers the 28 days BEFORE today.
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

        // 20 minutes later: today's timeline is stale (>=15 min) so it
        // recomputes, but the baseline is still fresh (<24 h) and must be
        // reused.
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
