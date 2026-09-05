package tech.mmarca.openvitals.testing

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyReasonCode
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint

/** A fixed past day, so nothing here moves with the wall clock. */
val BodyEnergyFixtureDate: LocalDate = LocalDate.of(2026, 6, 23)

/**
 * A whole, plausible Body Energy day, so a test can name the one thing it cares about.
 * The defaults describe an unremarkable, well-measured day.
 */
fun bodyEnergyTimeline(
    date: LocalDate = BodyEnergyFixtureDate,
    startScore: Int = 50,
    currentScore: Int = 62,
    charged: Int = 14,
    drained: Int = 2,
    confidence: BodyEnergyConfidence = BodyEnergyConfidence.HIGH,
    confidenceReasonCode: BodyEnergyReasonCode = BodyEnergyReasonCode.STRONG_CALIBRATION,
    confidenceReason: String = "Heart-rate intensity has strong calibration.",
    points: List<BodyEnergyTimelinePoint> = listOf(
        bodyEnergyPoint(date, hour = 3, score = 54, charge = 1.4),
        bodyEnergyPoint(
            date,
            hour = 9,
            score = 60,
            basalDrain = 0.6,
            influence = BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
        ),
        bodyEnergyPoint(
            date,
            hour = 17,
            score = 62,
            basalDrain = 0.4,
            influence = BodyEnergyPrimaryInfluence.STEADY,
        ),
    ),
    inputSummary: BodyEnergyInputSummary = bodyEnergyInputSummary(),
): BodyEnergyTimeline = BodyEnergyTimeline(
    date = date,
    startScore = startScore,
    currentScore = currentScore,
    charged = charged,
    drained = drained,
    points = points,
    confidence = confidence,
    confidenceReason = confidenceReason,
    confidenceReasonCode = confidenceReasonCode,
    inputSummary = inputSummary,
    generatedAt = date.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant(),
)

fun bodyEnergyPoint(
    date: LocalDate = BodyEnergyFixtureDate,
    hour: Int,
    score: Int,
    charge: Double = 0.0,
    basalDrain: Double = 0.0,
    state: BodyEnergyBucketState = BodyEnergyBucketState.REST,
    influence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.SLEEP_RECOVERY,
    confidence: BodyEnergyConfidence = BodyEnergyConfidence.HIGH,
): BodyEnergyTimelinePoint = BodyEnergyTimelinePoint(
    time = timeAt(date, hour),
    score = score,
    delta = charge - basalDrain,
    state = state,
    confidence = confidence,
    charge = charge,
    basalDrain = basalDrain,
    primaryInfluence = influence,
)

/** What the day was computed from. Defaults to a fully measured day. */
fun bodyEnergyInputSummary(
    heartRateSampleCount: Int = 288,
    sleepSessionCount: Int = 1,
    workoutCount: Int = 0,
    hrvSampleCount: Int = 6,
    hasRestingHeartRate: Boolean = true,
    hasBaselineRestingHeartRate: Boolean = true,
    hasObservedMaxHeartRate: Boolean = true,
): BodyEnergyInputSummary = BodyEnergyInputSummary(
    heartRateSampleCount = heartRateSampleCount,
    hrvSampleCount = hrvSampleCount,
    sleepSessionCount = sleepSessionCount,
    workoutCount = workoutCount,
    hasRestingHeartRate = hasRestingHeartRate,
    hasBaselineRestingHeartRate = hasBaselineRestingHeartRate,
    hasObservedMaxHeartRate = hasObservedMaxHeartRate,
)

private fun timeAt(date: LocalDate, hour: Int): Instant =
    date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant()
