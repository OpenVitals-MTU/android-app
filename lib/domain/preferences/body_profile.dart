import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'body_profile.freezed.dart';

@freezed
abstract class BodyProfile with _$BodyProfile {
  const BodyProfile._();

  const factory BodyProfile({
    int? birthYear,
    double? weightKg,
    int? restingHeartRateBpm,
    int? maxHeartRateBpm,
  }) = _BodyProfile;

  BodyProfile normalized({LocalDate? today}) {
    final resolvedToday = today ?? LocalDate.now();
    final currentYear = resolvedToday.year;
    final year = birthYear;
    final weight = weightKg;
    final resting = restingHeartRateBpm;
    final max = maxHeartRateBpm;
    return BodyProfile(
      birthYear:
          (year != null && year >= minBirthYear && year <= currentYear)
              ? year
              : null,
      weightKg: (weight != null && weight.isFinite)
          ? weight.clamp(minWeightKg, maxWeightKg).toDouble()
          : null,
      restingHeartRateBpm: resting
          ?.clamp(minRestingHeartRateBpm, maxRestingHeartRateBpm)
          .toInt(),
      maxHeartRateBpm:
          max?.clamp(minMaxHeartRateBpm, maxMaxHeartRateBpm).toInt(),
    );
  }

  int? ageYears({LocalDate? today}) {
    final resolvedToday = today ?? LocalDate.now();
    final year = birthYear;
    if (year == null) return null;
    final age = resolvedToday.year - year;
    return (age >= minAgeYears && age <= maxAgeYears) ? age : null;
  }

  String signature({LocalDate? today}) {
    final normalizedProfile = normalized(today: today);
    return [
      normalizedProfile.birthYear ?? 'auto',
      normalizedProfile.weightKg ?? 'auto',
      normalizedProfile.restingHeartRateBpm ?? 'auto',
      normalizedProfile.maxHeartRateBpm ?? 'auto',
    ].join('|');
  }

  static const int minBirthYear = 1900;
  static const int minAgeYears = 10;
  static const int maxAgeYears = 110;
  static const double minWeightKg = 30.0;
  static const double maxWeightKg = 250.0;
  static const int minRestingHeartRateBpm = 30;
  static const int maxRestingHeartRateBpm = 120;
  static const int minMaxHeartRateBpm = 80;
  static const int maxMaxHeartRateBpm = 240;
}
