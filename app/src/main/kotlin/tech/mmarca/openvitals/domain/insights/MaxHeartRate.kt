package tech.mmarca.openvitals.domain.insights

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The age-predicted maximum heart rate, 208 − 0.7 × age: Tanaka, Monahan and
 * Seals, "Age-predicted maximal heart rate revisited", J Am Coll Cardiol
 * 2001;37(1):153–156, doi:10.1016/S0735-1097(00)01054-8. A meta-analysis of
 * 351 studies (18,712 healthy adults), cross-validated in the laboratory; it
 * found no difference by sex or activity. A group mean: one person's maximum
 * can sit well away from it, so a trustworthy observed maximum wins wherever
 * one is known. Every age-derived maximum in the app comes from here.
 */
fun tanakaMaxHeartRate(ageYears: Int): Long =
    (208.0 - 0.7 * ageYears).roundToInt().toLong().coerceAtLeast(1L)

/**
 * The bar an observed maximum must clear: at least this, and at least
 * [observedMaxHeartRateRestingDeltaBpm] above resting. Otherwise the highest
 * sample on record is a brisk walk up the stairs. Both thresholds: app
 * heuristic, no published source.
 */
const val observedMaxHeartRateMinimumBpm = 150
const val observedMaxHeartRateRestingDeltaBpm = 60

/** Whether [observedMaxBpm] is a real maximum rather than the ceiling of an easy week. */
fun isObservedMaxHeartRateTrustworthy(
    observedMaxBpm: Int,
    restingHeartRateBpm: Int,
): Boolean =
    observedMaxBpm >=
        max(
            observedMaxHeartRateMinimumBpm,
            restingHeartRateBpm + observedMaxHeartRateRestingDeltaBpm,
        )
