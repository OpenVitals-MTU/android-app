package tech.mmarca.openvitals.wear.features.metric

import tech.mmarca.openvitals.wear.R

/** Lower bound of each zone as a share of the maximum heart rate, zone 1 first. */
private val ZoneLowerBounds = listOf(0.5, 0.6, 0.7, 0.8, 0.9)

/** The five zones' names, light to maximum. */
val HeartZoneLabels = listOf(
    R.string.zone_very_light,
    R.string.zone_light,
    R.string.zone_moderate,
    R.string.zone_hard,
    R.string.zone_maximum,
)

/** The zone index, 0 to 4, for [bpm]; null below zone 1. */
fun heartRateZone(bpm: Double, maxHeartRate: Int): Int? {
    val share = bpm / maxHeartRate
    return ZoneLowerBounds.indexOfLast { share >= it }.takeIf { it >= 0 }
}

/** Where [bpm] sits across all five zones, 0 at the start of zone 1 and 1 at the maximum. */
fun heartRateZoneProgress(bpm: Double, maxHeartRate: Int): Float =
    ((bpm / maxHeartRate - ZoneLowerBounds.first()) / (1 - ZoneLowerBounds.first())).toFloat().coerceIn(0f, 1f)

/** The share of [samples] in each of the five zones. Samples below zone 1 count towards none. */
fun heartRateZoneShares(samples: List<Double>, maxHeartRate: Int): List<Float> {
    if (samples.isEmpty()) return List(ZoneLowerBounds.size) { 0f }
    val counts = IntArray(ZoneLowerBounds.size)
    samples.forEach { bpm -> heartRateZone(bpm, maxHeartRate)?.let { counts[it]++ } }
    return counts.map { it.toFloat() / samples.size }
}
