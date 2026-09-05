package tech.mmarca.openvitals.features.bodyenergy

import java.time.Duration
import java.time.ZoneId
import kotlin.math.roundToInt
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.domain.insights.BodyEnergyInputSummary
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergySeedSource
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.insights.bodyEnergySeedScore

data class BodyEnergyDisplayState(
    val timeline: BodyEnergyTimeline? = null,
    val chartPoints: List<BodyEnergyChartPoint> = emptyList(),
    val influenceBars: List<BodyEnergyInfluenceBar> = emptyList(),
    val legendInfluences: List<BodyEnergyPrimaryInfluence> = emptyList(),
    val topReasons: List<BodyEnergyReason> = emptyList(),
    val inputRows: List<BodyEnergyInputRow> = emptyList(),
    val inputSummary: BodyEnergyInputSummary? = null,
    /** The tallest influence bar, floored at 1.0. Precomputed for the painter. */
    val maxInfluenceMagnitude: Double = 1.0,
) {
    val isEmpty: Boolean
        get() = timeline == null || chartPoints.isEmpty()
}

data class BodyEnergyChartPoint(
    val xFraction: Float,
    val score: Double,
)

data class BodyEnergyInfluenceBar(
    val xFraction: Float,
    val widthFraction: Float,
    val charge: Double,
    val drain: Double,
    val influence: BodyEnergyPrimaryInfluence,
)

data class BodyEnergyReason(
    val influence: BodyEnergyPrimaryInfluence,
    val direction: BodyEnergyReasonDirection,
    val amount: Double,
    val bucketCount: Int,
) {
    val roundedAmount: Int
        get() = amount.roundToInt()
}

enum class BodyEnergyReasonDirection {
    CHARGE,
    DRAIN,
}

data class BodyEnergyInputRow(
    val kind: BodyEnergyInputKind,
    val status: BodyEnergyInputStatus,
    val count: Int? = null,
    val value: String? = null,
)

enum class BodyEnergyInputKind {
    HEART_RATE,
    SLEEP,
    WORKOUTS,
    RESTING_HEART_RATE,
    HEART_RATE_BASELINE,
    HRV,
    RESPIRATORY_RATE,
    PREVIOUS_SCORE,
    CALIBRATION,
}

enum class BodyEnergyInputStatus {
    AVAILABLE,
    MISSING,
    OPTIONAL,
}

fun BodyEnergyTimelineResult.toBodyEnergyDisplayState(): BodyEnergyDisplayState =
    latestDay.toBodyEnergyDisplayState()

fun BodyEnergyTimeline?.toBodyEnergyDisplayState(): BodyEnergyDisplayState {
    val timeline = this ?: return BodyEnergyDisplayState()
    if (timeline.points.isEmpty()) {
        return BodyEnergyDisplayState(
            timeline = timeline,
            inputRows = timeline.inputSummary.inputRows(),
            inputSummary = timeline.inputSummary,
        )
    }

    val start = timeline.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val totalSeconds = Duration.ofDays(1).seconds.toFloat()
    val widthFraction = (timeline.inputSummary.bucketMinutes / MinutesPerDay).toFloat()
    val chartPoints = timeline.points.map { point ->
        BodyEnergyChartPoint(
            xFraction = (Duration.between(start, point.time).seconds / totalSeconds).coerceIn(0f, 1f),
            score = point.score.toDouble(),
        )
    }
    val influenceBars = timeline.points.map { point ->
        // The applied activity drain, not the two estimates: the bar must match the score.
        val drain = point.basalDrain +
            point.appliedActivityDrain +
            point.stressDrain +
            point.recoveryDebtDrain
        BodyEnergyInfluenceBar(
            xFraction = (Duration.between(start, point.time).seconds / totalSeconds).coerceIn(0f, 1f),
            widthFraction = widthFraction,
            charge = point.charge,
            drain = drain,
            influence = point.primaryInfluence,
        )
    }
    val legendInfluences = influenceBars
        .filter { it.charge > 0.0 || it.drain > 0.0 || it.influence == BodyEnergyPrimaryInfluence.NO_DATA }
        .map { it.influence }
        .distinct()
        .ifEmpty { listOf(BodyEnergyPrimaryInfluence.STEADY) }

    return BodyEnergyDisplayState(
        timeline = timeline,
        chartPoints = chartPoints,
        influenceBars = influenceBars,
        legendInfluences = legendInfluences,
        topReasons = timeline.topReasons(),
        inputRows = timeline.inputSummary.inputRows(),
        inputSummary = timeline.inputSummary,
        maxInfluenceMagnitude = influenceBars.maxInfluenceMagnitude(),
    )
}

/** The tallest bar, floored at 1.0 so an empty day divides by something. */
private fun List<BodyEnergyInfluenceBar>.maxInfluenceMagnitude(): Double =
    maxOfOrNull { maxOf(it.charge, it.drain) }?.takeIf { it > 0.0 } ?: 1.0

/**
 * The day's charge and drain by cause, biggest first. Charge per bucket,
 * drain per component, so the sums add back up to the headline.
 */
private fun BodyEnergyTimeline.topReasons(): List<BodyEnergyReason> {
    val chargeReasons = points
        .filter { it.charge > 0.0 }
        .groupBy {
            if (it.primaryInfluence == BodyEnergyPrimaryInfluence.SLEEP_RECOVERY) {
                BodyEnergyPrimaryInfluence.SLEEP_RECOVERY
            } else {
                BodyEnergyPrimaryInfluence.QUIET_REST
            }
        }
        .map { (influence, points) ->
            BodyEnergyReason(
                influence = influence,
                direction = BodyEnergyReasonDirection.CHARGE,
                amount = points.sumOf { it.charge },
                bucketCount = points.size,
            )
        }

    val drainReasons = points
        .flatMap { point -> point.drainComponents() }
        .filter { it.second > 0.0 }
        .groupBy({ it.first }, { it.second })
        .map { (influence, values) ->
            BodyEnergyReason(
                influence = influence,
                direction = BodyEnergyReasonDirection.DRAIN,
                amount = values.sum(),
                bucketCount = values.size,
            )
        }

    return (chargeReasons + drainReasons)
        .filter { it.amount >= MinimumReasonAmount }
        .sortedByDescending { it.amount }
        .take(MaxTopReasons)
}

private fun BodyEnergyTimelinePoint.drainComponents(): List<Pair<BodyEnergyPrimaryInfluence, Double>> {
    val activityInfluence = if (activityEnergyDrain > intensityDrain) {
        BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY
    } else {
        BodyEnergyPrimaryInfluence.EXERTION
    }
    return listOf(
        activityInfluence to appliedActivityDrain,
        BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE to stressDrain,
        BodyEnergyPrimaryInfluence.RECOVERY_DEBT to recoveryDebtDrain,
        // Basal is STEADY: never a bucket's primary influence, but often the day's largest.
        BodyEnergyPrimaryInfluence.STEADY to basalDrain,
    )
}

private fun BodyEnergyInputSummary.inputRows(): List<BodyEnergyInputRow> =
    listOf(
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.HEART_RATE,
            status = if (heartRateSampleCount > 0) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.MISSING
            },
            count = heartRateSampleCount,
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.SLEEP,
            status = if (sleepSessionCount > 0) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.OPTIONAL
            },
            count = sleepSessionCount,
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.WORKOUTS,
            status = if (workoutCount > 0) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.OPTIONAL
            },
            count = workoutCount,
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.RESTING_HEART_RATE,
            status = if (hasRestingHeartRate) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.MISSING
            },
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.HEART_RATE_BASELINE,
            status = if (hasBaselineRestingHeartRate || hasObservedMaxHeartRate) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.MISSING
            },
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.HRV,
            status = if (hrvSampleCount > 0 || hasHrvBaseline) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.OPTIONAL
            },
            count = hrvSampleCount,
        ),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.RESPIRATORY_RATE,
            status = if (respiratorySampleCount > 0 || hasRespiratoryBaseline) {
                BodyEnergyInputStatus.AVAILABLE
            } else {
                BodyEnergyInputStatus.OPTIONAL
            },
            count = respiratorySampleCount,
        ),
        previousScoreRow(),
        BodyEnergyInputRow(
            kind = BodyEnergyInputKind.CALIBRATION,
            status = BodyEnergyInputStatus.AVAILABLE,
            value = calibrationMode.name,
        ),
    )

/**
 * The score carried over from the previous day. A chain gap or a floored
 * carry-over shows both numbers rather than presenting the raised one as measured.
 */
private fun BodyEnergyInputSummary.previousScoreRow(): BodyEnergyInputRow {
    if (seedSource == BodyEnergySeedSource.CHAIN_GAP) {
        return BodyEnergyInputRow(
            kind = BodyEnergyInputKind.PREVIOUS_SCORE,
            status = BodyEnergyInputStatus.MISSING,
        )
    }
    val carried = previousEndScore
        ?: return BodyEnergyInputRow(
            kind = BodyEnergyInputKind.PREVIOUS_SCORE,
            status = BodyEnergyInputStatus.OPTIONAL,
        )
    return BodyEnergyInputRow(
        kind = BodyEnergyInputKind.PREVIOUS_SCORE,
        status = BodyEnergyInputStatus.AVAILABLE,
        value = if (carryOverFloorApplied) {
            "$carried -> ${bodyEnergySeedScore(carried)}"
        } else {
            carried.toString()
        },
    )
}

private const val MinutesPerDay = 24.0 * 60.0
private const val MinimumReasonAmount = 0.5
private const val MaxTopReasons = 3
