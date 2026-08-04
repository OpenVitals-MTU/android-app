package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketRetentionDays
import tech.mmarca.openvitals.data.local.bodyenergy.FakeBodyEnergyTimelineDao
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyReasonCode
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint

class BodyEnergyTimelineStoreTest {

    private val date = LocalDate.of(2026, 6, 1)
    private val signature = "v11|1234|0|5678"

    private fun store(dao: FakeBodyEnergyTimelineDao = FakeBodyEnergyTimelineDao()) =
        dao to BodyEnergyTimelineStore(dao)

    private fun timeline(
        date: LocalDate = this.date,
        signature: String = this.signature,
        endScore: Int = 42,
        points: List<BodyEnergyTimelinePoint> = listOf(samplePoint(date)),
        confidence: BodyEnergyConfidence = BodyEnergyConfidence.HIGH,
        confidenceReason: String = "Heart-rate intensity has strong calibration.",
        generatedAt: Instant = Instant.parse("2026-06-01T20:00:00Z"),
    ) = BodyEnergyTimeline(
        date = date,
        startScore = 50,
        currentScore = endScore,
        charged = 3,
        drained = 11,
        points = points,
        confidence = confidence,
        confidenceReason = confidenceReason,
        confidenceReasonCode = BodyEnergyReasonCode.STRONG_CALIBRATION,
        inputSummary = BodyEnergyInputSummary(
            heartRateSampleCount = 180,
            hrvSampleCount = 4,
            sleepSessionCount = 1,
            workoutCount = 2,
            respiratorySampleCount = 3,
            hasRestingHeartRate = true,
            hasBaselineRestingHeartRate = true,
            hasObservedMaxHeartRate = true,
            hasHrvBaseline = true,
            hasRespiratoryBaseline = false,
            previousEndScore = 4,
            carryOverFloorApplied = true,
            seedSource = BodyEnergySeedSource.CARRIED_OVER,
            calibrationMode = BodyEnergyCalibrationMode.MANUAL_ZONES,
        ),
        generatedAt = generatedAt,
        signature = signature,
    )

    private fun samplePoint(date: LocalDate) = BodyEnergyTimelinePoint(
        time = date.atStartOfDay(TestZone).plusHours(9).toInstant(),
        score = 47,
        delta = -0.75,
        state = BodyEnergyBucketState.ACTIVITY,
        confidence = BodyEnergyConfidence.HIGH,
        charge = 0.1,
        intensityDrain = 0.4,
        activityEnergyDrain = 0.6,
        basalDrain = 0.11,
        stressDrain = 0.02,
        recoveryDebtDrain = 0.03,
        primaryInfluence = BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
    )

    @Test
    fun `a saved timeline round-trips every field the summary carries`() = runTest {
        val (_, store) = store()
        val saved = timeline()

        store.save(saved)
        val loaded = store.load(date, signature)

        assertNotNull(loaded)
        assertEquals(saved.startScore, loaded!!.startScore)
        assertEquals(saved.currentScore, loaded.currentScore)
        assertEquals(saved.charged, loaded.charged)
        assertEquals(saved.drained, loaded.drained)
        assertEquals(saved.confidence, loaded.confidence)
        assertEquals(saved.confidenceReason, loaded.confidenceReason)
        assertEquals(saved.generatedAt, loaded.generatedAt)
        assertEquals(saved.signature, loaded.signature)
        assertEquals(saved.inputSummary, loaded.inputSummary)
        assertEquals(BodyEnergySeedSource.CARRIED_OVER, loaded.inputSummary.seedSource)
        assertTrue(loaded.inputSummary.carryOverFloorApplied)
    }

    @Test
    fun `a saved bucket round-trips the new drain components`() = runTest {
        val (_, store) = store()
        store.save(timeline())

        val point = store.load(date, signature)!!.points.single()

        assertEquals(samplePoint(date), point)
        assertEquals(0.6, point.activityEnergyDrain, 0.0)
        assertEquals(0.11, point.basalDrain, 0.0)
        // The stronger of the two estimates, never their sum.
        assertEquals(0.6, point.appliedActivityDrain, 0.0)
    }

    @Test
    fun `the reason code is back-filled from the stored English sentence`() = runTest {
        val (_, store) = store()
        store.save(
            timeline(confidenceReason = "Some timeline buckets have sparse Health Connect data.")
        )

        val loaded = store.load(date, signature)

        assertEquals(BodyEnergyReasonCode.SPARSE_BUCKETS, loaded!!.confidenceReasonCode)
    }

    @Test
    fun `an unrecognised reason stays legacy and renders as stored`() = runTest {
        val (_, store) = store()
        store.save(timeline(confidenceReason = "Something a future version wrote."))

        val loaded = store.load(date, signature)

        assertEquals(BodyEnergyReasonCode.LEGACY, loaded!!.confidenceReasonCode)
        assertEquals("Something a future version wrote.", loaded.confidenceReason)
    }

    @Test
    fun `a signature mismatch is a miss, not a stale hit`() = runTest {
        val (_, store) = store()
        store.save(timeline())

        assertNull(store.load(date, "v11|other-calibration|0|1"))
        assertNotNull(store.load(date, signature))
    }

    @Test
    fun `an unsigned timeline is refused, because no read could ever validate it`() = runTest {
        val (dao, store) = store()

        store.save(timeline(signature = "   "))

        assertEquals(0, dao.countDays())
    }

    @Test
    fun `storedDaysBetween returns the window oldest first without decoding buckets`() = runTest {
        val (dao, store) = store()
        for (back in 0..3) {
            store.save(timeline(date = date.minusDays(back.toLong()), endScore = 40 + back))
        }

        val window = store.storedDaysBetween(date.minusDays(2), date)

        assertEquals(
            listOf(date.minusDays(2), date.minusDays(1), date),
            window.map { it.date },
        )
        assertEquals(listOf(42, 41, 40), window.map { it.endScore })
        assertEquals(signature, window.first().signature)
        assertEquals(4, dao.countDays())
    }

    @Test
    fun `hasStoredPoints ignores the signature entirely`() = runTest {
        val (_, store) = store()
        store.save(timeline())

        assertTrue(store.hasStoredPoints(date))
        assertFalse(store.hasStoredPoints(date.minusDays(1)))
    }

    @Test
    fun `invalidateForward drops the range from both tables`() = runTest {
        val (dao, store) = store()
        for (back in 0..3) {
            store.save(timeline(date = date.minusDays(back.toLong())))
        }

        store.invalidateForward(date.minusDays(2), date)

        assertEquals(listOf(date.minusDays(3)), store.storedDaysBetween(date.minusDays(9), date).map { it.date })
        assertEquals(0, dao.countBucketsForDay(date.toEpochDay()))
        assertEquals(1, dao.countBucketsForDay(date.minusDays(3).toEpochDay()))
    }

    @Test
    fun `invalidateForward is a no-op when the range is empty`() = runTest {
        // The forward ripple passes (date + 1, today); recomputing today makes
        // end < start, and that must not wipe the chain.
        val (dao, store) = store()
        store.save(timeline(date = date))

        store.invalidateForward(date.plusDays(1), date)

        assertEquals(1, dao.countDays())
        assertEquals(1, dao.countBucketsForDay(date.toEpochDay()))
    }

    @Test
    fun `retention drops old buckets but keeps their day summaries walkable`() = runTest {
        val (dao, store) = store()
        val ancient = date.minusDays(BodyEnergyBucketRetentionDays + 10)
        store.save(timeline(date = ancient, endScore = 33))
        store.save(timeline(date = date))

        store.applyRetention(today = date)

        assertEquals(0, dao.countBucketsForDay(ancient.toEpochDay()))
        assertEquals(1, dao.countBucketsForDay(date.toEpochDay()))
        val stored = store.storedDaysBetween(ancient, date)
        assertEquals(2, stored.size)
        assertEquals(33, stored.first().endScore)
    }

    @Test
    fun `purgeAll clears the chain and its cursor`() = runTest {
        val (dao, store) = store()
        store.save(timeline())
        store.writeGlobalSignature("v11|zones|perms")

        store.purgeAll()

        assertEquals(0, dao.countDays())
        assertEquals(0, dao.countBucketsForDay(date.toEpochDay()))
        assertNull(store.storedGlobalSignature())
    }

    @Test
    fun `recording a pass leaves the global signature intact`() = runTest {
        val (_, store) = store()
        store.writeGlobalSignature("v11|zones|perms")

        store.writeLastPassAt(Instant.parse("2026-06-01T10:00:00Z"))

        assertEquals("v11|zones|perms", store.storedGlobalSignature())
        assertEquals(Instant.parse("2026-06-01T10:00:00Z"), store.lastPassAt())
    }

    @Test
    fun `saving a day replaces its buckets rather than accumulating them`() = runTest {
        val (dao, store) = store()
        store.save(timeline())

        store.save(timeline(points = emptyList()))

        assertEquals(0, dao.countBucketsForDay(date.toEpochDay()))
        assertEquals(1, dao.countDays())
    }
}
