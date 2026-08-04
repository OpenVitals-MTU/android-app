package tech.mmarca.openvitals.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketRetentionDays
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.repository.BodyEnergyBaselineCacheStore
import tech.mmarca.openvitals.data.repository.BodyEnergyChainSettlingDays
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.TestZone
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.grantedHealthRepository
import tech.mmarca.openvitals.data.repository.inMemoryBaselineStore
import tech.mmarca.openvitals.data.repository.inMemoryPreferences
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.insights.bodyEnergySeedScore
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/**
 * Records which days it was asked for, and persists each one so the service's
 * "already stored and fresh" skip is exercised for real.
 */
private class RecordingRepository(
    private val store: BodyEnergyTimelineStore,
    private val now: () -> Instant,
) : BodyEnergyRepository {
    val requested = mutableListOf<LocalDate>()
    var throwOnLoad = false

    override suspend fun loadTimeline(query: BodyEnergyTimelineQuery): BodyEnergyTimelineResult {
        if (throwOnLoad) error("health connect exploded")
        val date = query.period.start
        requested += date

        // Chain the stored predecessor, exactly as the real repository does, so
        // a test can assert the walk really produced a connected chain.
        val previous = store.storedDaysBetween(date.minusDays(1), date.minusDays(1))
        val seed = previous.firstOrNull()?.endScore
        val start = bodyEnergySeedScore(seed)
        val timeline = BodyEnergyTimeline(
            date = date,
            startScore = start,
            currentScore = (start - 7).coerceIn(0, 100),
            charged = 0,
            drained = 7,
            points = emptyList(),
            confidence = BodyEnergyConfidence.HIGH,
            confidenceReason = "test",
            inputSummary = BodyEnergyInputSummary(previousEndScore = seed),
            generatedAt = now(),
            signature = "v$BodyEnergyTimelineAlgorithmVersion|test|0",
        )
        store.save(timeline)
        return BodyEnergyTimelineResult(query = query, days = listOf(timeline))
    }
}

class BodyEnergyChainSyncServiceTest {

    // Mid-morning, so the cases that advance the clock by a couple of hours stay
    // on the same calendar day and the warm window does not shift under them.
    private var now = Instant.parse("2026-06-01T10:00:00Z")
    private val today: LocalDate = Instant.parse("2026-06-01T10:00:00Z").atZone(TestZone).toLocalDate()

    private lateinit var dao: FakeBodyEnergyTimelineDao
    private lateinit var store: BodyEnergyTimelineStore
    private lateinit var baselines: BodyEnergyBaselineCacheStore
    private lateinit var prefs: PreferencesRepository
    private lateinit var health: HealthRepository
    private lateinit var repository: RecordingRepository

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        now = Instant.parse("2026-06-01T10:00:00Z")
        dao = FakeBodyEnergyTimelineDao()
        store = BodyEnergyTimelineStore(dao)
        baselines = inMemoryBaselineStore()
        prefs = inMemoryPreferences()
        health = grantedHealthRepository(granted = setOf(ReadHeartRate))
        repository = RecordingRepository(store) { now }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun service(windowDays: Long = 5L) = BodyEnergyChainSyncService(
        repository = repository,
        store = store,
        baselineStore = baselines,
        healthRepository = health,
        preferencesRepository = prefs,
        clock = { now },
        zone = TestZone,
        windowDays = windowDays,
    )

    @Test
    fun `a cold window is walked oldest first, and today is left alone`() = runTest {
        service().syncAll()

        // Order is load-bearing: a day's seed must already be stored when its
        // successor is computed.
        assertEquals(
            listOf(
                today.minusDays(4),
                today.minusDays(3),
                today.minusDays(2),
                today.minusDays(1),
            ),
            repository.requested,
        )
    }

    @Test
    fun `the walked days form a connected chain`() = runTest {
        service().syncAll()

        val days = store.storedDaysBetween(today.minusDays(4), today.minusDays(1))
        assertEquals(4, days.size)
        for (i in 1 until days.size) {
            assertEquals(bodyEnergySeedScore(days[i - 1].endScore), days[i].startScore)
        }
    }

    @Test
    fun `a second pass inside the throttle window does no work`() = runTest {
        val service = service()
        service.syncAll()
        val firstPass = repository.requested.size

        now = now.plusSeconds(5 * 60)
        service.syncAll()

        assertEquals(
            "every screen open calls syncAll; it must not re-walk",
            firstPass,
            repository.requested.size,
        )
    }

    @Test
    fun `past the throttle, already-stored fresh days are still skipped`() = runTest {
        val service = service()
        service.syncAll()
        repository.requested.clear()

        now = now.plusSeconds(2 * 3600)
        service.syncAll()

        assertTrue(
            "stored days under 24h old are fresh and cost nothing",
            repository.requested.isEmpty(),
        )
    }

    @Test
    fun `a changed calibration purges the chain rather than ageing it out`() = runTest {
        val service = service()
        service.syncAll()
        assertEquals(4, dao.countDays())

        // Rows computed under retired ZONES are wrong, not merely stale: the
        // zones decide what every bucket meant.
        prefs.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                useManualZones = true,
                manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
            )
        )
        now = now.plusSeconds(2 * 3600)
        repository.requested.clear()
        service.syncAll()

        assertEquals("the purge must force a full rebuild", 4, repository.requested.size)
    }

    @Test
    fun `a gain the watch learner nudged does not purge the stored history`() = runTest {
        // The global signature gates a purgeAll() of every day AND every bucket.
        // With the learned gains folded into it, each observation the watch fit
        // absorbed wiped up to the whole retention window of history.
        service().syncAll()
        val storedBefore = store.storedDaysBetween(today.minusDays(BodyEnergyBucketRetentionDays), today)
        assertTrue(storedBefore.isNotEmpty())

        prefs.setBodyEnergyCalibration(prefs.bodyEnergyCalibration().copy(stressDrainGain = 1.04))
        now = now.plusSeconds(2 * 3600)
        service().syncAll()

        assertTrue(
            "a sub-percent gain nudge must not destroy the chain",
            store.storedDaysBetween(today.minusDays(BodyEnergyBucketRetentionDays), today).size >=
                storedBefore.size,
        )
    }

    @Test
    fun `without the heart-rate permission it does nothing`() = runTest {
        health = grantedHealthRepository(granted = emptySet())

        service().syncAll()

        assertTrue(repository.requested.isEmpty())
        assertEquals(0, dao.countDays())
    }

    @Test
    fun `concurrent calls share a single run`() = runTest {
        val service = service()

        coroutineScope {
            listOf(async { service.syncAll() }, async { service.syncAll() }).awaitAll()
        }

        assertEquals(
            "two callers must not walk the window twice",
            4,
            repository.requested.size,
        )
    }

    @Test
    fun `a throwing repository is swallowed, not surfaced`() = runTest {
        repository.throwOnLoad = true

        service().syncAll()
    }

    @Test
    fun `the legacy prefs timelines are purged on the first pass`() = runTest {
        val cachePrefs = FakeSharedPreferences()
        cachePrefs.edit().putString("2026-05-30|-12345", "a retired encoded timeline").commit()
        val context = mockk<Context> {
            every { getSharedPreferences(any(), any()) } returns (cachePrefs as SharedPreferences)
        }
        baselines = BodyEnergyBaselineCacheStore(context)

        service().syncAll()

        assertFalse(cachePrefs.contains("2026-05-30|-12345"))
    }

    @Test
    fun `retention drops old buckets but keeps their day summaries`() = runTest {
        val service = service()
        // Establish the global signature first: the very first pass has no
        // stored signature and so purges, which would take the fixture with it.
        service.syncAll()

        val ancient = today.minusDays(BodyEnergyBucketRetentionDays + 10)
        store.save(
            BodyEnergyTimeline(
                date = ancient,
                startScore = 50,
                currentScore = 40,
                charged = 0,
                drained = 10,
                points = listOf(
                    BodyEnergyTimelinePoint(
                        time = ancient.atStartOfDay(TestZone).toInstant(),
                        score = 50,
                        delta = 0.0,
                        state = BodyEnergyBucketState.REST,
                        confidence = BodyEnergyConfidence.HIGH,
                    )
                ),
                confidence = BodyEnergyConfidence.HIGH,
                confidenceReason = "test",
                generatedAt = now,
                signature = "v$BodyEnergyTimelineAlgorithmVersion|test|0",
            )
        )
        assertEquals(1, dao.countBucketsForDay(ancient.toEpochDay()))

        now = now.plusSeconds(2 * 3600)
        service.syncAll()

        assertEquals(0, dao.countBucketsForDay(ancient.toEpochDay()))
        assertNotNull(
            "the chain must stay walkable past the bucket window",
            dao.day(ancient.toEpochDay()),
        )
    }

    @Test
    fun `a later pass skips settled days and revisits only unsettled ones`() = runTest {
        // The window has to reach past the settling horizon for this to bite.
        val service = service(windowDays = 12L)
        service.syncAll()
        assertEquals(11, repository.requested.size)
        repository.requested.clear()

        // A day later every stored day is over 24h old, which before the
        // settling window meant the whole walk ran again — ~88 Health Connect
        // reads for days that cannot have gained anything.
        now = now.plusSeconds(25 * 3600)
        val shiftedToday = now.atZone(TestZone).toLocalDate()
        service.syncAll()

        for (back in 8..11) {
            assertTrue(
                "${shiftedToday.minusDays(back.toLong())} is settled and was already stored",
                shiftedToday.minusDays(back.toLong()) !in repository.requested,
            )
        }
        for (back in 1..BodyEnergyChainSettlingDays) {
            assertTrue(
                "days that can still gain late data must be revisited",
                shiftedToday.minusDays(back) in repository.requested,
            )
        }
    }

    @Test
    fun `a forced pass bypasses the throttle, so a watch sync is acted on at once`() = runTest {
        val service = service()
        service.syncAll()
        // The state a watch sync leaves behind: the days it back-filled dropped
        // from the chain, so there is real work for the next pass to find.
        store.invalidateForward(today.minusDays(2), today)
        repository.requested.clear()

        now = now.plusSeconds(2 * 60)
        service.syncAll(force = true)

        assertEquals(
            "oldest first, and only the days that went missing",
            listOf(today.minusDays(2), today.minusDays(1)),
            repository.requested,
        )
        assertEquals(4, dao.countDays())
    }

    @Test
    fun `an unforced call inside the throttle leaves the holes alone`() = runTest {
        val service = service()
        service.syncAll()
        store.invalidateForward(today.minusDays(2), today)
        repository.requested.clear()

        now = now.plusSeconds(2 * 60)
        service.syncAll()

        assertTrue(repository.requested.isEmpty())
        assertEquals(2, dao.countDays())
    }

    @Test
    fun `force does not override the freshness skip`() = runTest {
        // Force is about the throttle only. A day already stored and fresh is
        // still skipped, or every sync would re-read the whole window.
        val service = service()
        service.syncAll()
        repository.requested.clear()

        now = now.plusSeconds(2 * 60)
        service.syncAll(force = true)

        assertTrue(repository.requested.isEmpty())
    }

    private companion object {
        val ReadHeartRate: String = HealthPermission.getReadPermission(HeartRateRecord::class)
    }
}
