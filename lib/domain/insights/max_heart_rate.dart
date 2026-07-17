import 'dart:math' as math;

/// The bar an *observed* maximum heart rate has to clear before it is believed.
///
/// The highest heart rate we have ever seen from someone is only their maximum if
/// they ever went hard enough to find it. For most people the highest sample on
/// record is a brisk walk up some stairs, and taking that as a maximum makes every
/// effort look near-maximal. Two conditions, both of which must hold: the reading is
/// at least [observedMaxHeartRateMinimumBpm], and it sits at least
/// [observedMaxHeartRateRestingDeltaBpm] above the person's resting rate — the second
/// is what makes it work for someone whose whole heart-rate range runs low.
///
/// The rule was written out twice, identically, in `cardio_load.dart` and
/// `intensity_minutes.dart`, and heart-rate recovery needs it too. There is now one
/// of it.
const int observedMaxHeartRateMinimumBpm = 150;
const int observedMaxHeartRateRestingDeltaBpm = 60;

/// Whether [observedMaxBpm] is high enough to be taken as a real maximum rather than
/// the ceiling of an easy week. See [observedMaxHeartRateMinimumBpm].
bool isObservedMaxHeartRateTrustworthy(
  int observedMaxBpm,
  int restingHeartRateBpm,
) =>
    observedMaxBpm >=
    math.max(
      observedMaxHeartRateMinimumBpm,
      restingHeartRateBpm + observedMaxHeartRateRestingDeltaBpm,
    );
