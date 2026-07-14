import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/caffeine_models.dart';
import '../preferences/body_profile.dart';
import '../preferences/caffeine_preferences.dart';
import 'caffeine_insight_calculator.dart';

part 'caffeine_drink_profile.freezed.dart';

/// One drink, on its own: what it did, what it is still doing, and when it will be done.
///
/// Nothing here is new arithmetic. [CaffeineInsightCalculator.contributionMg] already
/// answers "how much of THIS drink is still in you at that moment" — it has to, because
/// the whole-day curve is the sum of exactly these. All this does is ask it about one
/// drink, repeatedly, which is the difference between a number in a total and a thing you
/// can understand.
///
/// The question a drinker actually has is "will this coffee still be in me at bedtime",
/// so that is the question this answers: the peak and when it fell, what is left right
/// now, when half of it will be gone, and when it will effectively be gone.

/// Below this, a drink is finished. Not zero: the model decays exponentially and never
/// reaches zero, so a "gone" that waited for zero would never come.
const double caffeineNegligibleMg = 5.0;

/// How far past a drink the profile looks before giving up on it fading.
///
/// Thirty-six hours, not twenty-four. A large dose really does take longer than a day to
/// fall below [caffeineNegligibleMg] -- a 200mg energy drink is still carrying more than
/// that a full day later -- and a 24-hour horizon simply reported "we do not know when
/// this goes away" for exactly the drinks whose staying power is most worth knowing.
const Duration caffeineProfileHorizon = Duration(hours: 36);

/// How finely the drink's curve is sampled.
const Duration caffeineProfileStep = Duration(minutes: 10);

@freezed
abstract class CaffeineDrinkProfile with _$CaffeineDrinkProfile {
  const CaffeineDrinkProfile._();

  const factory CaffeineDrinkProfile({
    required CaffeineEntry entry,

    /// This drink's own rise and fall — not the day's.
    required List<CaffeinePoint> curve,

    /// The most of this drink that was ever in the body at once, and when.
    ///
    /// Lower than the dose, always: absorption takes time, and elimination has begun
    /// before absorption has finished. A 95mg coffee never puts 95mg in you at once.
    required double peakMg,
    required DateTime peakTime,

    /// What is left of it now. Zero before it was drunk.
    required double currentMg,

    /// When half of the peak has gone, and when what remains stops mattering.
    /// Null when the drink has not faded within [caffeineProfileHorizon].
    required DateTime? halfGoneTime,
    required DateTime? goneTime,
  }) = _CaffeineDrinkProfile;

  /// Whether this drink is still doing anything worth speaking of.
  bool get isActive => currentMg >= caffeineNegligibleMg;
}

/// Works [entry] out on its own, against the same model the whole-day curve uses — so the
/// number a drink shows here and the bump it makes in the day's curve are the same number,
/// and can never disagree.
CaffeineDrinkProfile caffeineDrinkProfile({
  required CaffeineEntry entry,
  required DateTime now,
  required CaffeinePreferences preferences,
  BodyProfile bodyProfile = const BodyProfile(),
}) {
  final start = entry.startTime;
  final end = start.add(caffeineProfileHorizon);

  final curve = <CaffeinePoint>[];
  var peakMg = 0.0;
  var peakTime = start;

  for (var time = start;
      !time.isAfter(end);
      time = time.add(caffeineProfileStep)) {
    final value = CaffeineInsightCalculator.contributionMg(
      entry: entry,
      at: time,
      preferences: preferences,
      bodyProfile: bodyProfile,
    );
    curve.add(CaffeinePoint(time: time, valueMg: value));
    if (value > peakMg) {
      peakMg = value;
      peakTime = time;
    }
  }

  final currentMg = CaffeineInsightCalculator.contributionMg(
    entry: entry,
    at: now,
    preferences: preferences,
    bodyProfile: bodyProfile,
  );

  return CaffeineDrinkProfile(
    entry: entry,
    curve: curve,
    peakMg: peakMg,
    peakTime: peakTime,
    currentMg: currentMg,
    // Both are read off the curve AFTER the peak. Before it the drink is still being
    // absorbed and is on its way up — a threshold crossed on the way up is not the drink
    // fading, it is the drink arriving.
    halfGoneTime: _fallsBelow(curve, peakTime, peakMg / 2.0),
    goneTime: _fallsBelow(curve, peakTime, caffeineNegligibleMg),
  );
}

/// The first moment after [afterTime] that the curve drops below [threshold], or null if
/// it never does within the horizon.
DateTime? _fallsBelow(
  List<CaffeinePoint> curve,
  DateTime afterTime,
  double threshold,
) {
  if (threshold <= 0) return null;
  for (final point in curve) {
    if (point.time.isBefore(afterTime)) continue;
    if (point.valueMg < threshold) return point.time;
  }
  return null;
}

/// The highest point of any of [profiles] — what a set of per-drink charts should share as
/// their y axis, so a small drink next to a large one LOOKS small.
double caffeineProfilePeak(Iterable<CaffeineDrinkProfile> profiles) =>
    profiles.fold<double>(0.0, (peak, profile) => math.max(peak, profile.peakMg));
