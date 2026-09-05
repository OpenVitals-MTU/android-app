package tech.mmarca.openvitals.domain.model

import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.insights.RouteElevation

internal fun ExerciseData.withRouteBackfilledMetrics(): ExerciseData {
    val metrics = route.takeIf { it.status == ExerciseRouteStatus.DATA }
        ?.points
        .orEmpty()
        .routeBackfillMetrics()

    return copy(
        totalDistanceMeters = totalDistanceMeters.backfilledBy(
            metrics.distanceMeters.takeIf { it >= MinBackfillDistanceMeters },
        ),
        elevationGainedMeters = elevationGainedMeters.backfilledBy(
            metrics.elevationGainMeters.takeIf { metrics.hasAltitudeData && it >= MinBackfillElevationMeters },
        ),
    )
}

internal fun ExerciseData.withSampleBackfilledMetrics(
    heartRateSamples: List<HeartRateSample>,
    speedSamples: List<SpeedSample>,
    cadenceSamples: List<ActivityCadenceSample>,
): ExerciseData =
    copy(
        // A watch that records speed but no DistanceRecord still implies a distance.
        totalDistanceMeters = totalDistanceMeters.backfilledBy(
            distanceFromSpeedSamples(speedSamples),
        ),
        averageHeartRateBpm = averageHeartRateBpm.backfilledBy(
            heartRateSamples
                .mapNotNull { it.beatsPerMinute.takeIf { bpm -> bpm > 0L } }
                .averageLongOrNull()
                ?.roundToLong(),
        ),
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond.backfilledBy(
            speedSamples
                .mapNotNull { it.metersPerSecond.takeIf { speed -> speed > 0.0 && speed.isFinite() } }
                .averageDoubleOrNull(),
        ),
        averageStepsCadenceRate = averageStepsCadenceRate.backfilledBy(
            cadenceSamples
                .filter { it.kind == ActivityCadenceKind.STEPS }
                .mapNotNull { it.rate.takeIf { rate -> rate > 0.0 && rate.isFinite() } }
                .averageDoubleOrNull(),
        ),
        averageCyclingCadenceRpm = averageCyclingCadenceRpm.backfilledBy(
            cadenceSamples
                .filter { it.kind == ActivityCadenceKind.CYCLING }
                .mapNotNull { it.rate.takeIf { rate -> rate > 0.0 && rate.isFinite() } }
                .averageDoubleOrNull(),
        ),
    )

private data class RouteBackfillMetrics(
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val altitudePairCount: Int = 0,
) {
    val hasAltitudeData: Boolean get() = altitudePairCount > 0
}

private fun List<ExerciseRoutePoint>.routeBackfillMetrics(): RouteBackfillMetrics {
    if (size < 2) return RouteBackfillMetrics()

    var distanceMeters = 0.0
    var altitudePairCount = 0

    val ordered = sortedBy { it.time }
    ordered.zipWithNext().forEach { (start, end) ->
        distanceMeters += start.distanceMetersTo(end)

        if (start.altitudeMeters != null && end.altitudeMeters != null) {
            altitudePairCount += 1
        }
    }

    // Through the smoothing filter, as a live recording is; raw rises bank GPS noise.
    val elevationGainMeters = RouteElevation.routeElevationGain(ordered)

    return RouteBackfillMetrics(
        distanceMeters = distanceMeters.takeIf { it.isFinite() } ?: 0.0,
        elevationGainMeters = elevationGainMeters.takeIf { it.isFinite() } ?: 0.0,
        altitudePairCount = altitudePairCount,
    )
}

private fun ExerciseRoutePoint.distanceMetersTo(other: ExerciseRoutePoint): Double =
    haversineMeters(latitude, longitude, other.latitude, other.longitude)

/** The distance a run of speed samples implies, by trapezoidal integration. Null when none. */
internal fun distanceFromSpeedSamples(samples: List<SpeedSample>): Double? {
    if (samples.size < 2) return null
    var cumulative = 0.0
    samples.sortedBy { it.time }.zipWithNext().forEach { (previous, current) ->
        val seconds = java.time.Duration.between(previous.time, current.time).toNanos() / 1_000_000_000.0
        val v0 = previous.metersPerSecond.coerceAtLeast(0.0)
        val v1 = current.metersPerSecond.coerceAtLeast(0.0)
        if (seconds > 0 && v0.isFinite() && v1.isFinite()) {
            cumulative += (v0 + v1) / 2.0 * seconds
        }
    }
    return cumulative.takeIf { it.isFinite() && it > 0.0 }
}

private fun Double?.backfilledBy(value: Double?): Double? =
    if (isMissingMetric() && value != null && value > 0.0 && value.isFinite()) value else this

private fun Long?.backfilledBy(value: Long?): Long? =
    if ((this == null || this <= 0L) && value != null && value > 0L) value else this

private fun Double?.isMissingMetric(): Boolean =
    this == null || this <= 0.0 || !isFinite()

private fun List<Long>.averageLongOrNull(): Double? =
    takeIf { it.isNotEmpty() }?.map { it.toDouble() }?.average()

private fun List<Double>.averageDoubleOrNull(): Double? =
    takeIf { it.isNotEmpty() }?.average()

private const val MinBackfillDistanceMeters = 1.0
private const val MinBackfillElevationMeters = 1.0
