package tech.mmarca.openvitals.features.bodyenergy

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyCalibrationMode
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint

class BodyEnergyPresentationMapperTest {

    private val date = LocalDate.of(2026, 6, 1)
    private val zone = ZoneId.systemDefault()
    private val dayStart = date.atStartOfDay(zone).toInstant()

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

        assertEquals(3, reasons.size)
        assertEquals(BodyEnergyPrimaryInfluence.EXERTION, reasons[0].influence)
        assertEquals(BodyEnergyReasonDirection.DRAIN, reasons[0].direction)
        assertEquals(2.0, reasons[0].amount, 0.0001)
        assertTrue(reasons.any { it.influence == BodyEnergyPrimaryInfluence.SLEEP_RECOVERY })
        assertTrue(reasons.any { it.influence == BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE })
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
            drained = points.sumOf { it.intensityDrain + it.stressDrain + it.recoveryDebtDrain }.toInt(),
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
        stressDrain: Double = 0.0,
        recoveryDebtDrain: Double = 0.0,
        influence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.EXERTION,
    ): BodyEnergyTimelinePoint {
        val drain = intensityDrain + stressDrain + recoveryDebtDrain
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
            stressDrain = stressDrain,
            recoveryDebtDrain = recoveryDebtDrain,
            primaryInfluence = influence,
        )
    }
}
