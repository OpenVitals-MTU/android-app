package tech.mmarca.openvitals.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

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
    var elevationGainMeters = 0.0
    var altitudePairCount = 0

    sortedBy { it.time }.zipWithNext().forEach { (start, end) ->
        distanceMeters += start.distanceMetersTo(end)

        val startAltitude = start.altitudeMeters
        val endAltitude = end.altitudeMeters
        if (startAltitude != null && endAltitude != null) {
            altitudePairCount += 1
            val gain = endAltitude - startAltitude
            if (gain >= MinBackfillElevationDeltaMeters) {
                elevationGainMeters += gain
            }
        }
    }

    return RouteBackfillMetrics(
        distanceMeters = distanceMeters.takeIf { it.isFinite() } ?: 0.0,
        elevationGainMeters = elevationGainMeters.takeIf { it.isFinite() } ?: 0.0,
        altitudePairCount = altitudePairCount,
    )
}

private fun ExerciseRoutePoint.distanceMetersTo(other: ExerciseRoutePoint): Double {
    val deltaLat = Math.toRadians(other.latitude - latitude)
    val deltaLon = Math.toRadians(other.longitude - longitude)
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val a = sin(deltaLat / 2.0).pow(2.0) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
    return 2.0 * EarthRadiusMeters * asin(sqrt(a))
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

private const val EarthRadiusMeters = 6_371_000.0
private const val MinBackfillDistanceMeters = 1.0
private const val MinBackfillElevationMeters = 1.0
private const val MinBackfillElevationDeltaMeters = 1.0
