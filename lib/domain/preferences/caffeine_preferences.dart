import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import 'body_profile.dart';

part 'caffeine_preferences.freezed.dart';

enum CaffeineSleepSensitivity {
  low(0.9),
  normal(1.0),
  high(1.2),
  insomnia(1.35);

  const CaffeineSleepSensitivity(this.halfLifeMultiplier);
  final double halfLifeMultiplier;
}

enum CaffeineAlcoholUse {
  none(1.0),
  occasional(1.05),
  regular(1.15);

  const CaffeineAlcoholUse(this.halfLifeMultiplier);
  final double halfLifeMultiplier;
}

enum CaffeineHabituation {
  low(1.1),
  moderate(1.0),
  high(0.95);

  const CaffeineHabituation(this.halfLifeMultiplier);
  final double halfLifeMultiplier;
}

enum CaffeineGenotype {
  unknown(1.0),
  fast(0.85),
  normal(1.0),
  slow(1.25);

  const CaffeineGenotype(this.halfLifeMultiplier);
  final double halfLifeMultiplier;
}

enum CaffeineHormonalStatus {
  none(1.0),
  oralContraceptive(1.4),
  pregnant(1.7);

  const CaffeineHormonalStatus(this.halfLifeMultiplier);
  final double halfLifeMultiplier;
}

@freezed
abstract class CaffeinePreferences with _$CaffeinePreferences {
  const CaffeinePreferences._();

  const factory CaffeinePreferences({
    @Default(false) bool profileCompleted,
    @Default(CaffeinePreferences.defaultHalfLifeMinutes) int halfLifeMinutes,
    @Default(CaffeinePreferences.defaultAbsorptionMinutes)
    int absorptionMinutes,
    @Default(CaffeinePreferences.defaultSleepThresholdMg) int sleepThresholdMg,
    @Default(LocalTime(22, 30)) LocalTime bedtime,
    @Default(CaffeineSleepSensitivity.normal)
    CaffeineSleepSensitivity sleepSensitivity,
    @Default(false) bool smoker,
    @Default(CaffeineAlcoholUse.none) CaffeineAlcoholUse alcoholUse,
    @Default(CaffeineHabituation.moderate)
    CaffeineHabituation caffeineHabituation,
    @Default(false) bool liverImpairment,
    @Default(false) bool medicationInteraction,
    @Default(CaffeineGenotype.unknown) CaffeineGenotype cyp1a2Genotype,
    @Default(CaffeineGenotype.unknown) CaffeineGenotype ahrGenotype,
    @Default(CaffeineHormonalStatus.none) CaffeineHormonalStatus hormonalStatus,
  }) = _CaffeinePreferences;

  CaffeinePreferences normalized() => copyWith(
        halfLifeMinutes: halfLifeMinutes
            .clamp(minHalfLifeMinutes, maxHalfLifeMinutes)
            .toInt(),
        absorptionMinutes: absorptionMinutes
            .clamp(minAbsorptionMinutes, maxAbsorptionMinutes)
            .toInt(),
        sleepThresholdMg: sleepThresholdMg
            .clamp(minSleepThresholdMg, maxSleepThresholdMg)
            .toInt(),
      );

  int effectiveHalfLifeMinutes(BodyProfile bodyProfile) {
    final base = halfLifeMinutes.toDouble();
    final factors = <double>[
      sleepSensitivity.halfLifeMultiplier,
      alcoholUse.halfLifeMultiplier,
      caffeineHabituation.halfLifeMultiplier,
      cyp1a2Genotype.halfLifeMultiplier,
      ahrGenotype.halfLifeMultiplier,
      hormonalStatus.halfLifeMultiplier,
      smoker ? 0.7 : 1.0,
      liverImpairment ? 1.8 : 1.0,
      medicationInteraction ? 1.4 : 1.0,
      _ageMultiplier(bodyProfile),
      _weightMultiplier(bodyProfile),
    ];
    final multiplier = factors.fold<double>(1.0, (product, f) => product * f);
    return (base * multiplier)
        .round()
        .clamp(minHalfLifeMinutes, maxEffectiveHalfLifeMinutes)
        .toInt();
  }

  double _ageMultiplier(BodyProfile bodyProfile) {
    final age = bodyProfile.ageYears();
    if (age == null) return 1.0;
    if (age <= 17) return 1.1;
    if (age <= 44) return 1.0;
    if (age <= 64) return 1.1;
    return 1.2;
  }

  double _weightMultiplier(BodyProfile bodyProfile) {
    final weight = bodyProfile.weightKg;
    if (weight == null) return 1.0;
    if (weight < 55.0) return 1.1;
    if (weight > 95.0) return 0.92;
    return 1.0;
  }

  static const int defaultHalfLifeMinutes = 300;
  static const int defaultAbsorptionMinutes = 45;
  static const int defaultSleepThresholdMg = 60;
  static const LocalTime defaultBedtime = LocalTime(22, 30);
  static const int defaultConsumptionDurationMinutes = 10;
  static const int minHalfLifeMinutes = 90;
  static const int maxHalfLifeMinutes = 720;
  static const int maxEffectiveHalfLifeMinutes = 1080;
  static const int minAbsorptionMinutes = 10;
  static const int maxAbsorptionMinutes = 180;
  static const int minSleepThresholdMg = 5;
  static const int maxSleepThresholdMg = 300;
}
