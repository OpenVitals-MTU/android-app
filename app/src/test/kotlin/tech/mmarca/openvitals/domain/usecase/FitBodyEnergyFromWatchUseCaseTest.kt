package tech.mmarca.openvitals.domain.usecase

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.insights.WatchObservationBucket
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

/**
 * Teaching the Body Energy gains from the watch's own Body Battery.
 *
 * Without this the watch data a sync stores is only ever drawn on the watch
 * screen — the model never learns from it and the gains sit at their defaults
 * however much evidence accumulates.
 *
 * What is worth pinning is not the arithmetic of the fit (that is
 * `BodyEnergyCalibrationFitTest`'s) but the bookkeeping around it, because
 * every way of getting it wrong is silent: counting an hour twice makes the
 * learning rate depend on how often the user taps Sync, and advancing the
 * watermark over a day that was never examined destroys evidence permanently.
 */
class FitBodyEnergyFromWatchUseCaseTest {

    private val wellness = mockk<GarminWellnessRepository>()
    private val bodyEnergy = mockk<BodyEnergyRepository>()
    private val preferences = mockk<PreferencesRepository>(relaxed = true)

    private var calibration = BodyEnergyCalibration()
    private var watermark = 0L

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    init {
        every { preferences.bodyEnergyCalibration() } answers { calibration }
        every { preferences.setBodyEnergyCalibration(any()) } answers { calibration = firstArg() }
        every { preferences.bodyEnergyWatchFitWatermarkMillis } answers { watermark }
        every { preferences.bodyEnergyWatchFitWatermarkMillis = any() } answers { watermark = firstArg() }
    }

    @Test
    fun `an hour of watch samples counts once, however many samples it holds`() = runTest {
        // The watch emits about a sample a minute. Feeding every one in would
        // let a single day outvote months of evidence.
        val samples = (0 until 60).map { minute ->
            sample(NOON.plus(Duration.ofMinutes(minute.toLong())), score = 40)
        }
        givenSamples(samples)
        givenTimeline(TODAY, points = listOf(point(NOON, score = 70)))

        val fitted = useCase()(now = NOON.plus(Duration.ofHours(2)))

        assertThat(fitted).isEqualTo(1)
        assertThat(calibration.watchObservationCount).isEqualTo(1)
    }

    @Test
    fun `syncing the same hour twice does not teach the model twice`() = runTest {
        // The defect this watermark exists to prevent: ten syncs in an hour
        // must not teach ten times as fast as one, from identical watch data.
        givenSamples(listOf(sample(NOON, score = 40)))
        givenTimeline(TODAY, points = listOf(point(NOON, score = 70)))
        val at = NOON.plus(Duration.ofHours(2))

        val first = useCase()(now = at)
        val second = useCase()(now = at)

        assertThat(first).isEqualTo(1)
        assertThat(second).isEqualTo(0)
        assertThat(calibration.watchObservationCount).isEqualTo(1)
    }

    @Test
    fun `the watermark lands on the newest fitted bucket, not on now`() = runTest {
        // It has to be the bucket the evidence came from. Stamping "now" would
        // retire the hours between the last sample and the sync as if they had
        // been examined.
        val newest = NOON.plus(Duration.ofHours(1))
        givenSamples(listOf(sample(NOON, 40), sample(newest, 45)))
        givenTimeline(TODAY, points = listOf(point(NOON, 70), point(newest, 72)))

        useCase()(now = newest.plus(Duration.ofHours(5)))

        val expected = Math.floorDiv(newest.toEpochMilli(), WatchObservationBucket.toMillis()) *
            WatchObservationBucket.toMillis()
        assertThat(watermark).isEqualTo(expected)
    }

    @Test
    fun `a recent day the chain has not reached yet is waited for, not skipped`() = runTest {
        // This is the lossy-watermark bug. A day with no timeline is not
        // unpairable, merely early — advancing past it retires evidence that
        // was never examined, and the samples are then permanently invisible.
        givenSamples(listOf(sample(NOON, 40)))
        coEvery { bodyEnergy.loadTimeline(any()) } returns
            BodyEnergyTimelineResult(query = anyQuery(), days = emptyList())

        val fitted = useCase()(now = NOON.plus(Duration.ofHours(2)))

        assertThat(fitted).isEqualTo(0)
        assertThat(watermark).isEqualTo(0L)
    }

    @Test
    fun `a cold day older than the grace period is retired so the days behind it can move`() = runTest {
        // The counterweight: the watermark is one scalar, so waiting forever for
        // a day that will never have heart data holds every later day hostage.
        val old = NOON.minus(Duration.ofDays(5))
        givenSamples(listOf(sample(old, 40)))
        coEvery { bodyEnergy.loadTimeline(any()) } returns
            BodyEnergyTimelineResult(query = anyQuery(), days = emptyList())

        val fitted = useCase()(now = NOON)

        assertThat(fitted).isEqualTo(0)
        assertThat(watermark).isGreaterThan(0L)
    }

    @Test
    fun `days are retired oldest first, and a cold recent day stops the run`() = runTest {
        // The watermark can only say "everything before here is done", so a
        // warm day AFTER a cold one must not be fitted — that would claim the
        // cold day was handled too.
        val dayOne = NOON.minus(Duration.ofDays(1))
        givenSamples(listOf(sample(dayOne, 40), sample(NOON, 44)))
        coEvery { bodyEnergy.loadTimeline(any()) } answers {
            val query = firstArg<BodyEnergyTimelineQuery>()
            if (query.period.start == TODAY.minusDays(1)) {
                BodyEnergyTimelineResult(query = query, days = emptyList())
            } else {
                BodyEnergyTimelineResult(
                    query = query,
                    days = listOf(timeline(TODAY, listOf(point(NOON, 70)))),
                )
            }
        }

        val fitted = useCase()(now = NOON.plus(Duration.ofHours(2)))

        assertThat(fitted).isEqualTo(0)
        assertThat(watermark).isEqualTo(0L)
    }

    @Test
    fun `a first run looks back a week rather than at everything ever synced`() = runTest {
        // An install importing months of watch history must not try to fit all
        // of it in one pass.
        val from = slot<Instant>()
        coEvery { wellness.samplesBetween(any(), capture(from), any()) } returns emptyList()

        useCase()(now = NOON)

        assertThat(from.captured).isEqualTo(NOON.minus(Duration.ofDays(7)))
    }

    @Test
    fun `a resumed run starts at the bucket after the watermark`() = runTest {
        // Starting AT the watermark would re-read the hour already fitted.
        watermark = NOON.toEpochMilli()
        val from = slot<Instant>()
        coEvery { wellness.samplesBetween(any(), capture(from), any()) } returns emptyList()

        useCase()(now = NOON.plus(Duration.ofHours(3)))

        assertThat(from.captured)
            .isEqualTo(Instant.ofEpochMilli(NOON.toEpochMilli() + WatchObservationBucket.toMillis()))
    }

    @Test
    fun `only the watch's body battery series is read`() = runTest {
        coEvery { wellness.samplesBetween(any(), any(), any()) } returns emptyList()

        useCase()(now = NOON)

        coVerify { wellness.samplesBetween(GarminWellnessMetric.BODY_ENERGY, any(), any()) }
    }

    @Test
    fun `a failing sample read is swallowed and changes nothing`() = runTest {
        // Calibration is an enhancement; it must never fail the sync that
        // triggered it, and the evidence must survive to the next run.
        coEvery { wellness.samplesBetween(any(), any(), any()) } throws IllegalStateException("db closed")

        val fitted = useCase()(now = NOON)

        assertThat(fitted).isEqualTo(0)
        assertThat(watermark).isEqualTo(0L)
    }

    @Test
    fun `a throwing timeline read is treated as a day with no timeline`() = runTest {
        givenSamples(listOf(sample(NOON, 40)))
        coEvery { bodyEnergy.loadTimeline(any()) } throws SecurityException("permission revoked")

        val fitted = useCase()(now = NOON.plus(Duration.ofHours(2)))

        assertThat(fitted).isEqualTo(0)
        assertThat(watermark).isEqualTo(0L)
    }

    @Test
    fun `a day whose timeline pairs nothing is held, then retired with the rest`() = runTest {
        // A timeline that exists but has no point within the pairing gap is
        // treated like a day with no timeline at all: held while it is recent,
        // because a later chain pass can fill in the missing buckets, and
        // retired once it ages past the grace period so it cannot stall the
        // watermark forever.
        givenSamples(listOf(sample(NOON, 40)))
        givenTimeline(TODAY, points = listOf(point(NOON.plus(Duration.ofHours(6)), score = 70)))

        val heldFitted = useCase()(now = NOON.plus(Duration.ofHours(8)))
        assertThat(heldFitted).isEqualTo(0)
        assertThat(watermark).isEqualTo(0L)

        // Three days on, the same unpaired day is old enough to give up on.
        val laterFitted = useCase()(now = NOON.plus(Duration.ofDays(3)))
        assertThat(laterFitted).isEqualTo(0)
        assertThat(watermark).isGreaterThan(0L)
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private fun useCase() = FitBodyEnergyFromWatchUseCase(
        wellnessRepository = wellness,
        preferencesRepository = preferences,
        bodyEnergyRepository = bodyEnergy,
        zone = ZONE,
    )

    private fun givenSamples(samples: List<GarminWellnessSample>) {
        coEvery { wellness.samplesBetween(any(), any(), any()) } answers {
            val from = secondArg<Instant>()
            val to = thirdArg<Instant>()
            samples.filter { !it.time.isBefore(from) && it.time.isBefore(to) }
        }
    }

    private fun givenTimeline(date: LocalDate, points: List<BodyEnergyTimelinePoint>) {
        coEvery { bodyEnergy.loadTimeline(any()) } answers {
            BodyEnergyTimelineResult(
                query = firstArg(),
                days = listOf(timeline(date, points)),
            )
        }
    }

    private fun anyQuery() = BodyEnergyTimelineQuery(
        period = tech.mmarca.openvitals.core.period.DatePeriod(TODAY, TODAY),
        range = tech.mmarca.openvitals.core.period.TimeRange.DAY,
    )

    private fun sample(time: Instant, score: Int) = GarminWellnessSample(
        metric = GarminWellnessMetric.BODY_ENERGY,
        time = time,
        value = score.toLong(),
    )

    private fun point(time: Instant, score: Int) = BodyEnergyTimelinePoint(
        time = time,
        score = score,
        delta = 0.0,
        state = BodyEnergyBucketState.REST,
        confidence = BodyEnergyConfidence.HIGH,
        primaryInfluence = BodyEnergyPrimaryInfluence.QUIET_REST,
    )

    private fun timeline(date: LocalDate, points: List<BodyEnergyTimelinePoint>) = BodyEnergyTimeline(
        date = date,
        startScore = points.firstOrNull()?.score ?: 50,
        currentScore = points.lastOrNull()?.score ?: 50,
        charged = 0,
        drained = 0,
        points = points,
        confidence = BodyEnergyConfidence.HIGH,
        confidenceReason = "",
    )

    private companion object {
        val ZONE: ZoneId = ZoneOffset.UTC
        val TODAY: LocalDate = LocalDate.of(2026, 6, 23)
        val NOON: Instant = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC)
    }
}
