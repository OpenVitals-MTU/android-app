package tech.mmarca.openvitals.domain.insights

import kotlin.math.max

/**
 * The bar an observed maximum must clear: at least this, and at least
 * [observedMaxHeartRateRestingDeltaBpm] above resting. Otherwise the highest
 * sample on record is a brisk walk up the stairs.
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
