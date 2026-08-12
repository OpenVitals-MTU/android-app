package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyNeutralStartScore
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.insights.BodyEnergyWatchFitEpoch
import tech.mmarca.openvitals.domain.insights.bodyEnergySeedScore
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

/**
 * The chain: what a day opens on, and what invalidates that claim.
 *
 * "Now" is late on 1 June, so 1 June is today and everything before it is a
 * completed past day.
 */
class BodyEnergyChainTest {

    private val now = Instant.parse("2026-06-01T22:00:00Z")
    private val today: LocalDate = now.atZone(TestZone).toLocalDate()

    private lateinit var dao: FakeBodyEnergyTimelineDao
    private lateinit var timelines: BodyEnergyTimelineStore
    private lateinit var baselines: BodyEnergyBaselineCacheStore
    private lateinit var prefs: PreferencesRepository
    private lateinit var heart: FakeHeartRepository

    @Before
    fun setUp() {
        dao = FakeBodyEnergyTimelineDao()
        timelines = BodyEnergyTimelineStore(dao)
        baselines = inMemoryBaselineStore()
        prefs = inMemoryPreferences()
        heart = FakeHeartRepository()
    }

    private fun repo(
        heartRepository: FakeHeartRepository = heart,
        withStore: Boolean = true,
        healthRepository: HealthRepository = grantedHealthRepository(),
    ) = BodyEnergyRepositoryImpl(
        heartRepository = heartRepository.repository,
        sleepRepository = emptySleepRepository(),
        activityRepository = emptyActivityRepository(),
        vitalsRepository = emptyVitalsRepository(),
        bodyRepository = emptyBodyRepository(),
        healthRepository = healthRepository,
        preferencesRepository = prefs,
        baselineCacheStore = baselines,
        timelineStore = if (withStore) timelines else null,
        now = { now },
        zone = TestZone,
        dispatchers = TestDispatcherProvider,
    )

    private suspend fun load(
        repository: BodyEnergyRepositoryImpl,
        date: LocalDate,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): BodyEnergyTimeline = repository.loadTimeline(
        BodyEnergyTimelineQuery(
            period = DatePeriod(date, date),
            range = TimeRange.DAY,
            refreshMode = refreshMode,
        )
    ).days.single()

    /** Stores a day as the warm service or an earlier open would have. */
    private suspend fun seedStoredDay(
        repository: BodyEnergyRepositoryImpl,
        date: LocalDate,
    ): Int = load(repository, date).currentScore

    // region continuity across midnight

    @Test
    fun `a day opens where the stored previous day closed`() = runTest {
        val r = repo()
        val yesterdayEnd = seedStoredDay(r, today.minusDays(1))
        assertTrue("a waking day with no sleep should drain", yesterdayEnd < 50)

        val day = load(r, today)

        assertEquals(bodyEnergySeedScore(yesterdayEnd), day.startScore)
        assertNotEquals(BodyEnergyNeutralStartScore, day.startScore)
        assertEquals(yesterdayEnd, day.inputSummary.previousEndScore)
        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
    }

    @Test
    fun `the warm path costs no Health Connect read beyond today`() = runTest {
        val r = repo()
        seedStoredDay(r, today.minusDays(1))
        val callsAfterSeed = heart.dayGraphCalls

        load(r, today)

        assertEquals(
            "only today is read; the seed comes from the store",
            callsAfterSeed + 1,
            heart.dayGraphCalls,
        )
    }

    @Test
    fun `a cold chain starts neutral, because there is nothing to carry`() = runTest {
        val day = load(repo(), today)

        assertEquals(BodyEnergyNeutralStartScore, day.startScore)
        assertEquals(BodyEnergySeedSource.NEUTRAL, day.inputSummary.seedSource)
        assertEquals(null, day.inputSummary.previousEndScore)
    }

    @Test
    fun `a multi-day query threads each day into the next`() = runTest {
        val result = repo().loadTimeline(
            BodyEnergyTimelineQuery(
                period = DatePeriod(today.minusDays(3), today),
                range = TimeRange.DAY,
            )
        )

        for (i in 1 until result.days.size) {
            assertEquals(
                "day $i must open where day ${i - 1} closed",
                bodyEnergySeedScore(result.days[i - 1].currentScore),
                result.days[i].startScore,
            )
            assertEquals(result.days[i - 1].currentScore, result.days[i].inputSummary.previousEndScore)
        }
    }

    // endregion

    // region the gap fill

    @Test
    fun `a one-day gap is closed and the filled day is persisted`() = runTest {
        val r = repo()
        seedStoredDay(r, today.minusDays(2))

        val day = load(r, today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertTrue(
            "the missing day must be computed to close the chain",
            today.minusDays(1) in heart.daysRead,
        )
        val stored = timelines.storedDaysBetween(today.minusDays(1), today.minusDays(1))
        assertEquals("a filled gap day must be stored so the next open is warm", 1, stored.size)
        assertEquals(stored.single().endScore, day.inputSummary.previousEndScore)
        assertEquals(bodyEnergySeedScore(stored.single().endScore), day.startScore)
    }

    @Test
    fun `a gap wider than the foreground bound is reported, not walked`() = runTest {
        val r = repo()
        seedStoredDay(r, today.minusDays(5))
        val callsAfterSeed = heart.dayGraphCalls

        val day = load(r, today)

        assertEquals(BodyEnergySeedSource.CHAIN_GAP, day.inputSummary.seedSource)
        assertEquals(BodyEnergyNeutralStartScore, day.startScore)
        assertEquals(
            "a 4-day gap must not trigger four background-sized reads",
            callsAfterSeed + 1,
            heart.dayGraphCalls,
        )
    }

    // endregion

    // region the per-day signature

    @Test
    fun `a stored day under a foreign signature is not used as an anchor`() = runTest {
        val r = repo()
        val yesterday = today.minusDays(1)
        seedStoredDay(r, yesterday)

        // Rewrite yesterday's row with a signature from a different world. The
        // regression this guards: validating a predecessor against the REQUESTED
        // day's signature instead of its own, which the body profile's
        // date-dependent signature made silently wrong across a birthday.
        val stored = timelines.storedDaysBetween(yesterday, yesterday).single()
        val cached = timelines.load(yesterday, stored.signature)!!
        timelines.save(cached.copy(signature = "v11|not-this-calibration|0|0"))

        val day = load(r, today)

        assertEquals(
            "a row from another calibration must not seed the chain",
            BodyEnergySeedSource.NEUTRAL,
            day.inputSummary.seedSource,
        )
    }

    @Test
    fun `a gain the watch learner nudged still seeds the next day`() = runTest {
        // The learner moves the gains by a fraction of a percent per
        // observation, and they used to be part of the day signature. So one
        // watch reading invalidated all fourteen stored days at once, the next
        // load found no valid predecessor, and the day opened on the neutral 50
        // with yesterday sitting at 0.
        val r = repo()
        val yesterdayEnd = seedStoredDay(r, today.minusDays(1))

        prefs.setBodyEnergyCalibration(
            prefs.bodyEnergyCalibration().copy(stressDrainGain = 1.04)
        )

        val day = load(repo(), today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertEquals(bodyEnergySeedScore(yesterdayEnd), day.startScore)
    }

    @Test
    fun `but editing the heart zones does break the chain`() = runTest {
        // The other half of the split. Zones change what a bucket MEANS, and
        // only ever because someone edited a setting, so a reset there is
        // honest.
        val r = repo()
        seedStoredDay(r, today.minusDays(1))

        prefs.setBodyEnergyCalibration(
            prefs.bodyEnergyCalibration().copy(
                useManualZones = true,
                manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
            )
        )

        val day = load(repo(), today)

        assertEquals(BodyEnergySeedSource.NEUTRAL, day.inputSummary.seedSource)
    }

    // endregion

    // region the forward ripple

    @Test
    fun `recomputing a past day drops the days that followed it`() = runTest {
        val r = repo()
        for (back in 3 downTo 1) {
            seedStoredDay(r, today.minusDays(back.toLong()))
        }
        assertEquals(3, timelines.storedDaysBetween(today.minusDays(3), today).size)

        val target = today.minusDays(3)
        val before = timelines.storedDaysBetween(target, target).single().endScore
        // Swap in a harder day: a much higher heart rate drains further, so the
        // end score moves. Deliberately NOT a data-less day — that is the case
        // the empty-recompute guard protects.
        val recomputed = load(
            repo(heartRepository = FakeHeartRepository(wakingBpm = 115L)),
            target,
            refreshMode = RefreshMode.FORCE,
        )
        assertNotEquals(
            "the test needs the end score to actually move",
            before,
            recomputed.currentScore,
        )

        assertTrue(
            "days built on the old seed must be invalidated",
            timelines.storedDaysBetween(today.minusDays(2), today).isEmpty(),
        )
    }

    @Test
    fun `a recompute landing on the same score keeps the chain intact`() = runTest {
        val r = repo()
        for (back in 3 downTo 1) {
            seedStoredDay(r, today.minusDays(back.toLong()))
        }

        // Same inputs, so the same end score — nothing downstream changed.
        load(r, today.minusDays(3), refreshMode = RefreshMode.FORCE)

        assertEquals(
            "a no-op recompute must not wipe the stored chain",
            2,
            timelines.storedDaysBetween(today.minusDays(2), today).size,
        )
    }

    // endregion

    // region a day with no data

    @Test
    fun `a day with no data passes the seed through instead of resetting to 50`() = runTest {
        val yesterdayEnd = seedStoredDay(repo(), today.minusDays(1))

        val day = load(repo(heartRepository = FakeHeartRepository(wakingBpm = null)), today)

        assertEquals(BodyEnergyConfidence.NO_DATA, day.confidence)
        assertEquals(yesterdayEnd, day.startScore)
        assertEquals(
            "a day we know nothing about must not reset the chain",
            yesterdayEnd,
            day.currentScore,
        )
    }

    // endregion

    // region a transiently unreadable permission set

    @Test
    fun `a failed permission read does not orphan the stored chain`() = runTest {
        // The regression behind the widget's intermittent "Start: 50": the
        // permission hash used to collapse to a constant when the read failed,
        // which made every stored day look like it came from another permission
        // world — no anchor validated, and the day silently reopened neutral.
        val yesterdayEnd = seedStoredDay(repo(), today.minusDays(1))

        val day = load(repo(healthRepository = failingPermissionsHealthRepository()), today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertEquals(bodyEnergySeedScore(yesterdayEnd), day.startScore)
    }

    @Test
    fun `a permission read that never succeeded still starts neutral`() = runTest {
        // Nothing cached to fall back on — a fresh install whose very first
        // read fails. The documented neutral default is the whole truth then.
        val day = load(repo(healthRepository = failingPermissionsHealthRepository()), today)

        assertEquals(BodyEnergyNeutralStartScore, day.startScore)
        assertEquals(BodyEnergySeedSource.NEUTRAL, day.inputSummary.seedSource)
    }

    // endregion

    // region the seed mirror

    @Test
    fun `without a store the chain falls back to the prefs seed mirror`() = runTest {
        seedStoredDay(repo(), today.minusDays(1))
        val mirrored = prefs.bodyEnergyChainSeedMirror
        assertNotNull("computing a past day must mirror its end score", mirrored)

        val day = load(repo(withStore = false), today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertEquals(
            "${today.minusDays(1).toEpochDay()}|${day.inputSummary.previousEndScore}",
            mirrored?.split("|")?.take(2)?.joinToString("|"),
        )
    }

    @Test
    fun `a mirror that is not for the immediately previous day is ignored`() = runTest {
        prefs.bodyEnergyChainSeedMirror = "${today.minusDays(3).toEpochDay()}|40"

        val day = load(repo(withStore = false), today)

        assertEquals(BodyEnergySeedSource.NEUTRAL, day.inputSummary.seedSource)
        assertEquals(BodyEnergyNeutralStartScore, day.startScore)
    }

    @Test
    fun `the mirror only moves forward, so a backfill cannot rewind it`() = runTest {
        val r = repo()
        seedStoredDay(r, today.minusDays(1))
        val afterYesterday = prefs.bodyEnergyChainSeedMirror

        seedStoredDay(r, today.minusDays(4))

        assertEquals(
            "an older backfilled day must not overwrite the mirror",
            afterYesterday,
            prefs.bodyEnergyChainSeedMirror,
        )
    }

    @Test
    fun `an empty store falls back to the mirror, not the neutral 50`() = runTest {
        // The store exists but holds nothing — a cleared database under prefs
        // that survived. That is the store-less situation wearing a store, and
        // it degrades the same way: the mirrored score, not a reset.
        prefs.bodyEnergyChainSeedMirror = "${today.minusDays(1).toEpochDay()}|43"

        val day = load(repo(), today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertEquals(43, day.startScore)
    }

    @Test
    fun `a chain gap still carries yesterday's mirrored score`() = runTest {
        // The anchor is too far back to fill, but the mirror knows what
        // yesterday closed on — its row was lost, not the day itself.
        val r = repo()
        seedStoredDay(r, today.minusDays(5))
        prefs.bodyEnergyChainSeedMirror = "${today.minusDays(1).toEpochDay()}|37"

        val day = load(r, today)

        assertEquals(BodyEnergySeedSource.CARRIED_OVER, day.inputSummary.seedSource)
        assertEquals(37, day.startScore)
    }

    @Test
    fun `computing today keeps the mirror fresh`() = runTest {
        // Mirroring only completed days froze the mirror on whatever past day
        // was last recomputed; on a device where only the widgets run (they ask
        // for today alone) it was reliably stale by the time it was needed.
        val day = load(repo(), today)

        assertEquals(
            "${today.toEpochDay()}|${day.currentScore}|${day.startScore}|0",
            prefs.bodyEnergyChainSeedMirror,
        )
    }

    @Test
    fun `a rescued day reopens on the same chained score across recomputes`() = runTest {
        // The first rescue moves the mirror onto today; the day's own opening
        // score keeps later refreshes from oscillating back to 50.
        prefs.bodyEnergyChainSeedMirror = "${today.minusDays(1).toEpochDay()}|43"
        val r = repo()

        val first = load(r, today, refreshMode = RefreshMode.FORCE)
        val second = load(r, today, refreshMode = RefreshMode.FORCE)

        assertEquals(43, first.startScore)
        assertEquals(43, second.startScore)
        assertEquals(BodyEnergySeedSource.CARRIED_OVER, second.inputSummary.seedSource)
    }

    @Test
    fun `a neutrally opened day does not relabel itself as chained on recompute`() = runTest {
        val r = repo()
        load(r, today, refreshMode = RefreshMode.FORCE)

        val second = load(r, today, refreshMode = RefreshMode.FORCE)

        assertEquals(BodyEnergySeedSource.NEUTRAL, second.inputSummary.seedSource)
        assertEquals(BodyEnergyNeutralStartScore, second.startScore)
    }

    // endregion

    // region settled days are served, not recomputed

    /** Ages the stored copy of [date] by rewriting its generatedAt. */
    private suspend fun ageStoredDay(date: LocalDate, byMillis: Long) {
        val stored = timelines.storedDaysBetween(date, date).single()
        val cached = timelines.load(date, stored.signature)!!
        timelines.save(cached.copy(generatedAt = now.minusMillis(byMillis)))
    }

    @Test
    fun `a settled day is served from storage with no Health Connect read`() = runTest {
        val r = repo()
        val settled = today.minusDays(BodyEnergyChainSettlingDays + 3)
        seedStoredDay(r, settled)
        ageStoredDay(settled, byMillis = 30L * 24 * 3_600_000)
        val callsBefore = heart.dayGraphCalls

        val day = load(r, settled)

        assertEquals(
            "a settled day must cost no Health Connect read at all",
            callsBefore,
            heart.dayGraphCalls,
        )
        assertTrue(day.points.isNotEmpty())
    }

    @Test
    fun `a day still inside the settling window recomputes once it ages`() = runTest {
        val r = repo()
        val recent = today.minusDays(2)
        seedStoredDay(r, recent)
        ageStoredDay(recent, byMillis = 25L * 3_600_000)
        val callsBefore = heart.dayGraphCalls

        load(r, recent)

        assertEquals(
            "late watch data can still land on a recent day",
            callsBefore + 1,
            heart.dayGraphCalls,
        )
    }

    @Test
    fun `today still follows the 15-minute rule`() = runTest {
        val r = repo()
        load(r, today)
        val callsBefore = heart.dayGraphCalls

        load(r, today)

        assertEquals(
            "within the window today is served cached",
            callsBefore,
            heart.dayGraphCalls,
        )
    }

    @Test
    fun `a forced refresh still recomputes a settled day`() = runTest {
        val r = repo()
        val settled = today.minusDays(BodyEnergyChainSettlingDays + 3)
        seedStoredDay(r, settled)
        ageStoredDay(settled, byMillis = 30L * 24 * 3_600_000)
        val callsBefore = heart.dayGraphCalls

        load(r, settled, refreshMode = RefreshMode.FORCE)

        assertEquals(
            "pull-to-refresh must always reach Health Connect",
            callsBefore + 1,
            heart.dayGraphCalls,
        )
    }

    @Test
    fun `a signature change still rebuilds a settled day`() = runTest {
        // "Never stale" must not be read as "never updated": a calibration edit
        // has to reach even a day the settling window would otherwise freeze.
        val r = repo()
        val settled = today.minusDays(BodyEnergyChainSettlingDays + 3)
        seedStoredDay(r, settled)
        val stored = timelines.storedDaysBetween(settled, settled).single()
        val cached = timelines.load(settled, stored.signature)!!
        timelines.save(cached.copy(signature = "v11|other-calibration|0|0"))
        val callsBefore = heart.dayGraphCalls

        load(r, settled)

        assertEquals(callsBefore + 1, heart.dayGraphCalls)
    }

    @Test
    fun `a day whose buckets retention purged is recomputed, not served blank`() = runTest {
        val r = repo()
        val ancient = today.minusDays(BodyEnergyChainSettlingDays + 5)
        seedStoredDay(r, ancient)
        // Retention keeps the summary and drops the buckets; serving that would
        // put a real headline score above an empty chart.
        dao.purgeBucketsBefore(ancient.toEpochDay() + 1)
        val callsBefore = heart.dayGraphCalls

        val day = load(r, ancient)

        assertEquals(callsBefore + 1, heart.dayGraphCalls)
        assertTrue(day.points.isNotEmpty())
    }

    // endregion

    // region an empty recompute never destroys a stored day

    @Test
    fun `an empty recompute returns the stored timeline it could not replace`() = runTest {
        val target = today.minusDays(2)
        seedStoredDay(repo(), target)
        val storedBefore = timelines.storedDaysBetween(target, target).single()
        val bucketsBefore = dao.countBucketsForDay(target.toEpochDay())
        assertTrue(bucketsBefore > 0)

        val day = load(
            repo(heartRepository = FakeHeartRepository(wakingBpm = null)),
            target,
            refreshMode = RefreshMode.FORCE,
        )

        assertTrue(
            "the caller must get the day we still have, not the blank",
            day.points.isNotEmpty(),
        )
        assertEquals(storedBefore.endScore, day.currentScore)
        assertEquals(bucketsBefore, dao.countBucketsForDay(target.toEpochDay()))
    }

    @Test
    fun `an empty recompute does not ripple the days after it away`() = runTest {
        val r = repo()
        for (back in 3 downTo 1) {
            seedStoredDay(r, today.minusDays(back.toLong()))
        }

        load(
            repo(heartRepository = FakeHeartRepository(wakingBpm = null)),
            today.minusDays(3),
            refreshMode = RefreshMode.FORCE,
        )

        assertEquals(
            "nothing changed upstream, so nothing downstream is invalid",
            2,
            timelines.storedDaysBetween(today.minusDays(2), today.minusDays(1)).size,
        )
    }

    @Test
    fun `a genuinely data-less day with nothing stored is still recorded`() = runTest {
        // The guard protects existing buckets; it must not stop a first, honest
        // "we know nothing about this day" from being written.
        val target = today.minusDays(2)

        load(repo(heartRepository = FakeHeartRepository(wakingBpm = null)), target)

        val stored = timelines.storedDaysBetween(target, target)
        assertEquals(1, stored.size)
        assertEquals(BodyEnergyNeutralStartScore, stored.single().endScore)
    }

    // endregion

    // region the algorithm-change gain reset

    @Test
    fun `the gain reset runs on any load, not only when the chain sync fires`() = runTest {
        // It used to live in the chain sync service, which is only kicked by the
        // Body Energy screen — so the dashboard, the widgets and the diagnostics
        // all reached the model without it and the reset silently never ran.
        prefs.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                sleepChargeGain = 0.8,
                activityDrainGain = 1.1,
                basalDrainGain = 0.72,
                watchObservationCount = 39,
            )
        )

        load(repo(), today)

        val after = prefs.bodyEnergyCalibration()
        assertEquals(1.0, after.sleepChargeGain, 0.0)
        assertEquals(1.0, after.activityDrainGain, 0.0)
        assertEquals(1.0, after.basalDrainGain, 0.0)
        assertEquals(0, after.watchObservationCount)
        assertEquals(BodyEnergyTimelineAlgorithmVersion, prefs.bodyEnergyGainsAlgorithmVersion)
    }

    @Test
    fun `the reset rewinds the watch fit watermark so the gains can relearn`() = runTest {
        // The reset without this is a trap: it tells the model to relearn from
        // 1.0 while the watermark still says every stored watch sample has been
        // consumed.
        prefs.bodyEnergyWatchFitWatermarkMillis = now.toEpochMilli()

        load(repo(), today)

        assertEquals(0L, prefs.bodyEnergyWatchFitWatermarkMillis)
    }

    @Test
    fun `the watermark rewinds on an install already at this algorithm version`() = runTest {
        // The rewind used to hang off the algorithm-version reset, which returns
        // early when the version already matches — so on every install that had
        // seen the current version, which is all of them, it was dead code.
        prefs.bodyEnergyGainsAlgorithmVersion = BodyEnergyTimelineAlgorithmVersion
        prefs.bodyEnergyWatchFitEpoch = 0
        prefs.bodyEnergyWatchFitWatermarkMillis = now.toEpochMilli()

        load(repo(), today)

        assertEquals(0L, prefs.bodyEnergyWatchFitWatermarkMillis)
        assertEquals(BodyEnergyWatchFitEpoch, prefs.bodyEnergyWatchFitEpoch)
    }

    @Test
    fun `the watermark is not rewound again once that epoch is recorded`() = runTest {
        // Otherwise every load re-reads a week of watch samples and refits the
        // gains from them, compounding the same evidence without end.
        prefs.bodyEnergyGainsAlgorithmVersion = BodyEnergyTimelineAlgorithmVersion
        prefs.bodyEnergyWatchFitEpoch = BodyEnergyWatchFitEpoch
        val watermark = now.toEpochMilli()
        prefs.bodyEnergyWatchFitWatermarkMillis = watermark

        load(repo(), today)

        assertEquals(watermark, prefs.bodyEnergyWatchFitWatermarkMillis)
    }

    @Test
    fun `the watermark rewinds even when there were no personal gains to reset`() = runTest {
        // A model still sitting at 1.0 is the one with the most to relearn, and
        // the early return for "nothing to reset" used to skip it.
        prefs.bodyEnergyWatchFitWatermarkMillis = now.toEpochMilli()
        prefs.setBodyEnergyCalibration(BodyEnergyCalibration())

        load(repo(), today)

        assertEquals(0L, prefs.bodyEnergyWatchFitWatermarkMillis)
    }

    @Test
    fun `the reset leaves the manual heart zones alone`() = runTest {
        prefs.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                sleepChargeGain = 0.8,
                useManualZones = true,
                manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
            )
        )

        load(repo(), today)

        val after = prefs.bodyEnergyCalibration()
        assertEquals(1.0, after.sleepChargeGain, 0.0)
        assertTrue(after.useManualZones)
        assertEquals(155, after.manualZoneThresholdsBpm?.zone4LowerBpm)
    }

    @Test
    fun `the reset does not undo a gain learned after it ran`() = runTest {
        val r = repo()
        load(r, today)

        prefs.setBodyEnergyCalibration(BodyEnergyCalibration(sleepChargeGain = 1.4))
        load(r, today.minusDays(1))

        assertEquals(1.4, prefs.bodyEnergyCalibration().sleepChargeGain, 0.0)
    }

    // endregion
}
