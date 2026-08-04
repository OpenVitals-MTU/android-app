package tech.mmarca.openvitals.features.bodyenergy

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint

class BodyEnergyPresentationMapperTest {

    private val date = LocalDate.of(2026, 6, 1)
    private val zone = ZoneId.systemDefault()
    private val dayStart = date.atStartOfDay(zone).toInstant()

    @Test
    fun `no timeline at all is an empty display`() {
        val display = (null as BodyEnergyTimeline?).toBodyEnergyDisplayState()

        assertTrue(display.isEmpty)
        assertNull(display.timeline)
        assertTrue(display.chartPoints.isEmpty())
        assertTrue(display.inputRows.isEmpty())
        assertEquals(1.0, display.maxInfluenceMagnitude, 0.0001)
    }

    @Test
    fun `influence bars preserve bucket x fractions`() {
        val timeline = timeline(
            points = listOf(
                point(minutes = 0, score = 50, charge = 0.4),
                point(minutes = 12 * 60, score = 48, intensityDrain = 1.5),
            )
        )

        val display = timeline.toBodyEnergyDisplayState()

        assertEquals(2, display.chartPoints.size)
        assertEquals(0f, display.chartPoints[0].xFraction, 0.0001f)
        assertEquals(0.5f, display.chartPoints[1].xFraction, 0.0001f)
        assertEquals(0.4, display.influenceBars[0].charge, 0.0001)
        assertEquals(1.5, display.influenceBars[1].drain, 0.0001)
    }

    @Test
    fun `influence bar drain is every v11 component including basal`() {
        val timeline = timeline(
            points = listOf(
                point(
                    minutes = 0,
                    score = 47,
                    intensityDrain = 1.5,
                    activityEnergyDrain = 0.4,
                    basalDrain = 0.11,
                    stressDrain = 0.2,
                    recoveryDebtDrain = 0.07,
                ),
            )
        )

        val bar = timeline.toBodyEnergyDisplayState().influenceBars.single()

        // The applied activity drain is the STRONGER of the two estimates, not
        // their sum: they describe the same movement from different sensors.
        assertEquals(1.5 + 0.11 + 0.2 + 0.07, bar.drain, 0.0001)
    }

    @Test
    fun `max influence magnitude floors at one so an empty day divides by something`() {
        val flat = timeline(points = listOf(point(minutes = 0, score = 50)))

        assertEquals(1.0, flat.toBodyEnergyDisplayState().maxInfluenceMagnitude, 0.0001)

        val busy = timeline(
            points = listOf(
                point(minutes = 0, score = 52, charge = 0.9),
                point(minutes = 5, score = 49, intensityDrain = 2.5),
            )
        )

        assertEquals(2.5, busy.toBodyEnergyDisplayState().maxInfluenceMagnitude, 0.0001)
    }

    @Test
    fun `top reasons summarize largest charge and drain contributors`() {
        val timeline = timeline(
            points = listOf(
                point(minutes = 0, score = 52, charge = 1.2, influence = BodyEnergyPrimaryInfluence.SLEEP_RECOVERY),
                point(minutes = 5, score = 53, charge = 0.8, influence = BodyEnergyPrimaryInfluence.QUIET_REST),
                point(minutes = 10, score = 50, intensityDrain = 2.0),
                point(
                    minutes = 15,
                    score = 49,
                    stressDrain = 1.0,
                    influence = BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE,
                ),
                point(
                    minutes = 20,
                    score = 48,
                    recoveryDebtDrain = 0.6,
                    influence = BodyEnergyPrimaryInfluence.RECOVERY_DEBT,
                ),
            )
        )

        val reasons = timeline.toBodyEnergyDisplayState().topReasons

        // Charge and drain compete in ONE ranking, biggest amount first; the
        // smaller contributors (quiet rest 0.8, recovery debt 0.6) fall off the
        // three-slot list entirely.
        assertEquals(
            listOf(
                BodyEnergyPrimaryInfluence.EXERTION,
                BodyEnergyPrimaryInfluence.SLEEP_RECOVERY,
                BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE,
            ),
            reasons.map { it.influence },
        )
        assertEquals(BodyEnergyReasonDirection.DRAIN, reasons[0].direction)
        assertEquals(2.0, reasons[0].amount, 0.0001)
        assertEquals(2, reasons[0].roundedAmount)
        assertEquals(BodyEnergyReasonDirection.CHARGE, reasons[1].direction)
        assertEquals(1.2, reasons[1].amount, 0.0001)
        assertEquals(BodyEnergyReasonDirection.DRAIN, reasons[2].direction)
        assertTrue(reasons.none { it.influence == BodyEnergyPrimaryInfluence.RECOVERY_DEBT })
    }

    @Test
    fun `the legend lists only the influences that actually moved the score`() {
        val timeline = timeline(
            points = listOf(
                point(minutes = 3 * 60, score = 52, charge = 2.0, influence = BodyEnergyPrimaryInfluence.QUIET_REST),
                // No charge, no drain, not NO_DATA: nothing to put in the legend.
                point(minutes = 4 * 60, score = 52, influence = BodyEnergyPrimaryInfluence.STEADY),
                point(minutes = 10 * 60, score = 47, intensityDrain = 5.0, influence = BodyEnergyPrimaryInfluence.EXERTION),
            )
        )

        assertEquals(
            listOf(
                BodyEnergyPrimaryInfluence.QUIET_REST,
                BodyEnergyPrimaryInfluence.EXERTION,
            ),
            timeline.toBodyEnergyDisplayState().legendInfluences,
        )
    }

    @Test
    fun `activity drain reads as everyday movement when calories carried it`() {
        val timeline = timeline(
            points = listOf(
                // Calories over heart rate: a 20k-step day the wrist never
                // noticed.
                point(minutes = 0, score = 49, intensityDrain = 0.2, activityEnergyDrain = 1.4),
                point(minutes = 5, score = 48, intensityDrain = 0.2, activityEnergyDrain = 1.1),
            )
        )

        val reason = timeline.toBodyEnergyDisplayState().topReasons.single()

        assertEquals(BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY, reason.influence)
        assertEquals(2.5, reason.amount, 0.0001)
    }

    @Test
    fun `activity drain reads as exertion when heart rate carried it`() {
        val timeline = timeline(
            points = listOf(
                point(minutes = 0, score = 47, intensityDrain = 2.2, activityEnergyDrain = 0.5),
            )
        )

        val reason = timeline.toBodyEnergyDisplayState().topReasons.single()

        assertEquals(BodyEnergyPrimaryInfluence.EXERTION, reason.influence)
        assertEquals(2.2, reason.amount, 0.0001)
    }

    @Test
    fun `basal drain is reported as a steady reason across the day`() {
        // Basal never wins a single bucket — it is deliberately excluded from
        // that competition — but summed over a quiet day it is often the only
        // thing that happened, and a card that could not say so would have
        // nothing to explain the decline with.
        val timeline = timeline(
            points = (0 until 24).map { index ->
                point(minutes = index * 5L, score = 50 - index / 4, basalDrain = 0.11)
            }
        )

        val reason = timeline.toBodyEnergyDisplayState().topReasons.single()

        assertEquals(BodyEnergyPrimaryInfluence.STEADY, reason.influence)
        assertEquals(BodyEnergyReasonDirection.DRAIN, reason.direction)
        assertEquals(24 * 0.11, reason.amount, 0.0001)
    }

    @Test
    fun `reasons below the minimum amount are dropped`() {
        val timeline = timeline(
            points = listOf(point(minutes = 0, score = 50, charge = 0.2, basalDrain = 0.1)),
        )

        assertTrue(timeline.toBodyEnergyDisplayState().topReasons.isEmpty())
    }

    @Test
    fun `missing input rows expose sparse body energy inputs`() {
        val timeline = timeline(
            inputSummary = BodyEnergyInputSummary(
                heartRateSampleCount = 0,
                sleepSessionCount = 0,
                workoutCount = 0,
                calibrationMode = BodyEnergyCalibrationMode.AUTOMATIC,
            ),
            points = emptyList(),
        )

        val rows = timeline.toBodyEnergyDisplayState().inputRows

        assertEquals(BodyEnergyInputStatus.MISSING, rows.first { it.kind == BodyEnergyInputKind.HEART_RATE }.status)
        assertEquals(BodyEnergyInputStatus.OPTIONAL, rows.first { it.kind == BodyEnergyInputKind.SLEEP }.status)
        assertEquals(BodyEnergyInputStatus.OPTIONAL, rows.first { it.kind == BodyEnergyInputKind.WORKOUTS }.status)
        assertEquals(BodyEnergyInputStatus.AVAILABLE, rows.first { it.kind == BodyEnergyInputKind.CALIBRATION }.status)
        assertEquals(BodyEnergyCalibrationMode.AUTOMATIC.name, rows.first { it.kind == BodyEnergyInputKind.CALIBRATION }.value)
    }

    @Test
    fun `a cold start leaves the previous score row optional`() {
        val row = previousScoreRow(
            BodyEnergyInputSummary(seedSource = BodyEnergySeedSource.NEUTRAL, previousEndScore = null)
        )

        assertEquals(BodyEnergyInputStatus.OPTIONAL, row.status)
        assertNull(row.value)
    }

    @Test
    fun `a chain gap reads as missing rather than as a fresh start`() {
        // The predecessor exists but is too far back to trust, which is not the
        // same as never having had one — the row has to say so or the reset
        // looks like lost data.
        val row = previousScoreRow(
            BodyEnergyInputSummary(seedSource = BodyEnergySeedSource.CHAIN_GAP, previousEndScore = 41)
        )

        assertEquals(BodyEnergyInputStatus.MISSING, row.status)
        assertNull(row.value)
    }

    @Test
    fun `a carried score is shown as itself`() {
        val row = previousScoreRow(
            BodyEnergyInputSummary(seedSource = BodyEnergySeedSource.CARRIED_OVER, previousEndScore = 41)
        )

        assertEquals(BodyEnergyInputStatus.AVAILABLE, row.status)
        assertEquals("41", row.value)
    }

    @Test
    fun `a floored carry-over shows both numbers`() {
        val row = previousScoreRow(
            BodyEnergyInputSummary(
                seedSource = BodyEnergySeedSource.CARRIED_OVER,
                previousEndScore = 3,
                carryOverFloorApplied = true,
            )
        )

        assertEquals(BodyEnergyInputStatus.AVAILABLE, row.status)
        assertEquals("3 -> 10", row.value)
    }

    private fun previousScoreRow(summary: BodyEnergyInputSummary): BodyEnergyInputRow =
        timeline(inputSummary = summary, points = emptyList())
            .toBodyEnergyDisplayState()
            .inputRows
            .first { it.kind == BodyEnergyInputKind.PREVIOUS_SCORE }

    private fun timeline(
        inputSummary: BodyEnergyInputSummary = BodyEnergyInputSummary(
            heartRateSampleCount = 2,
            hasRestingHeartRate = true,
            hasBaselineRestingHeartRate = true,
        ),
        points: List<BodyEnergyTimelinePoint>,
    ): BodyEnergyTimeline =
        BodyEnergyTimeline(
            date = date,
            startScore = 50,
            currentScore = points.lastOrNull()?.score ?: 50,
            charged = points.sumOf { it.charge }.toInt(),
            drained = points.sumOf {
                it.basalDrain + it.appliedActivityDrain + it.stressDrain + it.recoveryDebtDrain
            }.toInt(),
            points = points,
            confidence = BodyEnergyConfidence.HIGH,
            confidenceReason = "test",
            inputSummary = inputSummary,
        )

    private fun point(
        minutes: Long,
        score: Int,
        charge: Double = 0.0,
        intensityDrain: Double = 0.0,
        activityEnergyDrain: Double = 0.0,
        basalDrain: Double = 0.0,
        stressDrain: Double = 0.0,
        recoveryDebtDrain: Double = 0.0,
        influence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.EXERTION,
    ): BodyEnergyTimelinePoint {
        val drain = maxOf(intensityDrain, activityEnergyDrain) +
            basalDrain + stressDrain + recoveryDebtDrain
        val state = when {
            charge > 0.0 -> BodyEnergyBucketState.REST
            stressDrain > 0.0 -> BodyEnergyBucketState.STRESS
            drain > 0.0 -> BodyEnergyBucketState.ACTIVITY
            else -> BodyEnergyBucketState.REST
        }
        return BodyEnergyTimelinePoint(
            time = dayStart.plusSeconds(minutes * 60),
            score = score,
            delta = charge - drain,
            state = state,
            confidence = BodyEnergyConfidence.HIGH,
            charge = charge,
            intensityDrain = intensityDrain,
            activityEnergyDrain = activityEnergyDrain,
            basalDrain = basalDrain,
            stressDrain = stressDrain,
            recoveryDebtDrain = recoveryDebtDrain,
            primaryInfluence = influence,
        )
    }
}
