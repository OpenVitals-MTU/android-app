package tech.mmarca.openvitals.data.sync

import tech.mmarca.openvitals.healthconnect.StrictHealthConnectReads
import kotlinx.coroutines.currentCoroutineContext
import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketRetentionDays
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.repository.BodyEnergyChainSettlingDays
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.TestZone
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.grantedHealthRepository
import tech.mmarca.openvitals.data.repository.inMemoryPreferences
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.insights.bodyEnergySeedScore
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/** Records which days it was asked for, and persists each one so the "already stored" skip is real. */
private class RecordingRepository(
    private val store: BodyEnergyTimelineStore,
    private val now: () -> Instant,
) : BodyEnergyRepository {
    val requested = mutableListOf<LocalDate>()
    var throwOnLoad = false

    /** Whether each load ran with strict Health Connect reads, where a failed read throws. */
    val loadedStrictly = mutableListOf<Boolean>()

    /** A day the foreground stores during the walk, on the first requested day. */
    var foregroundDayDuringWalk: LocalDate? = null

    override suspend fun loadTimeline(query: BodyEnergyTimelineQuery): BodyEnergyTimelineResult {
        if (throwOnLoad) error("health connect exploded")
        val date = query.period.start
        requested += date
        loadedStrictly += currentCoroutineContext()[StrictHealthConnectReads] != null
        foregroundDayDuringWalk?.let { foreground ->
            foregroundDayDuringWalk = null
            store.save(timelineFor(foreground))
        }

        val timeline = timelineFor(date)
        store.save(timeline)
        return BodyEnergyTimelineResult(query = query, days = listOf(timeline))
    }

    private suspend fun timelineFor(date: LocalDate): BodyEnergyTimeline {
        // Chain the stored predecessor, as the real repository does.
        val previous = store.storedDaysBetween(date.minusDays(1), date.minusDays(1))
        val seed = previous.firstOrNull()?.endScore
        val start = bodyEnergySeedScore(seed)
        return BodyEnergyTimeline(
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
    }
}

class BodyEnergyChainSyncServiceTest {

    // Mid-morning, so advancing the clock by hours stays on the same day.
    private var now = Instant.parse("2026-06-01T10:00:00Z")
    private val today: LocalDate = Instant.parse("2026-06-01T10:00:00Z").atZone(TestZone).toLocalDate()

    private lateinit var dao: FakeBodyEnergyTimelineDao
    private lateinit var store: BodyEnergyTimelineStore
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
        prefs = inMemoryPreferences()
        health = grantedHealthRepository(granted = setOf(ReadHeartRate))
        repository = RecordingRepository(store) { now }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun service(
        windowDays: Long = 5L,
        readsOtherAppsData: Boolean = true,
        zoneSource: () -> ZoneId = { TestZone },
    ) = BodyEnergyChainSyncService(
        readsOtherAppsData = { readsOtherAppsData },
        repository = repository,
        store = store,
        healthRepository = health,
        preferencesRepository = prefs,
        clock = { now },
        zoneSource = zoneSource,
        windowDays = windowDays,
    )

    @Test
    fun `a cold window is walked oldest first, and today is left alone`() = runTest {
        service().syncAll()

        // Order is load-bearing: a day's seed must be stored before its successor is computed.
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
    fun `a time zone change after start-up moves today`() = runTest {
        var zone = TestZone
        val service = service(zoneSource = { zone })
        service.syncAll()
        assertFalse(today in repository.requested)

        // The user flew east. It is 2 June there, so 1 June has closed.
        zone = ZoneId.of("Pacific/Kiritimati")
        service.syncAll(force = true)

        assertTrue(today in repository.requested)
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
        val rebuilds = mutableListOf<Unit>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            service.chainRebuilt.collect { rebuilds += it }
        }

        // Rows computed under retired zones are wrong, not stale.
        prefs.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                useManualZones = true,
                manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
            )
        )
        // Inside the throttle: a purge must not wait it out with an empty chain.
        now = now.plusSeconds(2 * 60)
        repository.requested.clear()
        // The dashboard computes today while the walk runs, seeded from a stale chain.
        repository.foregroundDayDuringWalk = today
        service.syncAll()
        runCurrent()

        assertEquals("the purge must force a full rebuild", 4, repository.requested.size)
        assertNull(
            "a today seeded mid-rebuild must be dropped for the foreground to re-chain",
            dao.day(today.toEpochDay()),
        )
        assertEquals("screens showing today are told once", 1, rebuilds.size)
        collector.cancel()
    }

    @Test
    fun `a body profile edit purges the chain too`() = runTest {
        val service = service()
        service.syncAll()
        assertEquals(4, dao.countDays())

        // The automatic zone ladder derives from age: the profile is a chain input like the zones.
        prefs.setBodyProfile(BodyProfile(birthYear = 1980))
        now = now.plusSeconds(2 * 60)
        repository.requested.clear()
        service.syncAll()

        assertEquals(4, repository.requested.size)
    }

    @Test
    fun `a new write grant leaves the stored chain alone, a new read grant purges it`() = runTest {
        service().syncAll()
        assertEquals(4, dao.countDays())
        now = now.plusSeconds(2 * 60)
        repository.requested.clear()

        // Switching on water logging changes no input of the model.
        health = grantedHealthRepository(granted = setOf(ReadHeartRate, "android.permission.health.WRITE_HYDRATION"))
        service().syncAll()
        assertEquals(emptyList<LocalDate>(), repository.requested)

        // Sleep is an input. Days computed without it are wrong.
        health = grantedHealthRepository(granted = setOf(ReadHeartRate, "android.permission.health.READ_SLEEP"))
        service().syncAll()
        assertEquals(4, repository.requested.size)
    }

    @Test
    fun `an unchanged signature does not announce a rebuild`() = runTest {
        val service = service()
        service.syncAll()
        val rebuilds = mutableListOf<Unit>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            service.chainRebuilt.collect { rebuilds += it }
        }

        now = now.plusSeconds(2 * 3600)
        service.syncAll()
        runCurrent()

        assertTrue("an ordinary pass must not make every screen reload", rebuilds.isEmpty())
        collector.cancel()
    }

    @Test
    fun `a gain the watch learner nudged does not purge the stored history`() = runTest {
        // The global signature gates a purgeAll(). With the gains folded in, each fit wiped the retention window.
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
    fun `the walk reads strictly, so a failed read ends the pass instead of storing an empty day`() = runTest {
        service().syncAll()

        assertEquals(4, repository.loadedStrictly.size)
        assertTrue(repository.loadedStrictly.all { it })
    }

    @Test
    fun `in the background without the background-read grant the chain is left alone`() = runTest {
        // A scheduled watch sync forces a pass. Reads would see only our own records.
        service().syncAll()
        assertEquals(4, dao.countDays())
        repository.requested.clear()
        prefs.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                useManualZones = true,
                manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
            )
        )

        service(readsOtherAppsData = false).syncAll(force = true)

        // Not even the purge a changed calibration calls for: nothing could rebuild it.
        assertEquals(emptyList<LocalDate>(), repository.requested)
        assertEquals(4, dao.countDays())
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
    fun `retention drops old buckets but keeps their day summaries`() = runTest {
        val service = service()
        // Establish the global signature first: the first pass has none and purges.
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

        // A day later every stored day is over 24h old; before the settling window the whole walk ran again.
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
        // The state a watch sync leaves: back-filled days dropped from the chain.
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
        // Force is about the throttle only. A stored, fresh day is still skipped.
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
