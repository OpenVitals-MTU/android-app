import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import '../model/dashboard_data.dart';
import '../model/dashboard_query.dart';
import 'sleep_score.dart';

part 'stress_tracking.freezed.dart';

enum PhysiologicalStressLevel {
  resting,
  low,
  medium,
  high,
  needsMoreData,
}

enum PhysiologicalStressConfidence {
  high,
  medium,
  low,
  noData,
}

@freezed
abstract class PhysiologicalStressEstimate with _$PhysiologicalStressEstimate {
  const factory PhysiologicalStressEstimate({
    required PhysiologicalStressLevel level,
    required String label,
    required int? score,
    required String summary,
    required String detail,
    required PhysiologicalStressConfidence confidence,
    required String confidenceReason,
    required int? hrvPercentFromBaseline,
    required int? restingHeartRateDeltaBpm,
    required int? averageHeartRateDeltaFromRestingBpm,
    required bool hasWorkoutInfluence,
    required List<String> contributingFactors,
    required List<String> dataCoverage,
    required List<String> caveats,
  }) = _PhysiologicalStressEstimate;

  static const PhysiologicalStressEstimate needsMoreData =
      PhysiologicalStressEstimate(
    level: PhysiologicalStressLevel.needsMoreData,
    label: 'Needs more data',
    score: null,
    summary: 'Stress estimate needs HRV or heart-rate baseline data.',
    detail:
        'OpenVitals needs HRV, resting heart rate, or average heart rate context to estimate physiological stress.',
    confidence: PhysiologicalStressConfidence.noData,
    confidenceReason: 'no_stress_signals',
    hrvPercentFromBaseline: null,
    restingHeartRateDeltaBpm: null,
    averageHeartRateDeltaFromRestingBpm: null,
    hasWorkoutInfluence: false,
    contributingFactors: <String>[],
    dataCoverage: <String>[],
    caveats: _defaultStressCaveats,
  );
}

PhysiologicalStressEstimate calculatePhysiologicalStress(DashboardData data) {
  var rawScore = 32;
  var signalCount = 0;
  var contextSignalCount = 0;
  final factors = <String>[];
  final dataCoverage = _stressDataCoverage(data);

  final hrvPercent = _hrvPercentFromBaseline(data);
  if (hrvPercent != null) {
    signalCount += 1;
    if (hrvPercent <= -30) {
      rawScore += 34;
    } else if (hrvPercent <= -15) {
      rawScore += 22;
    } else if (hrvPercent <= -7) {
      rawScore += 10;
    } else if (hrvPercent >= 20) {
      rawScore += -8;
    } else if (hrvPercent >= 8) {
      rawScore += -5;
    } else {
      rawScore += -3;
    }
    if (hrvPercent < 0) {
      factors.add('HRV is ${hrvPercent.abs()}% below your usual baseline.');
    } else if (hrvPercent > 0) {
      factors.add('HRV is $hrvPercent% above your usual baseline.');
    } else {
      factors.add('HRV is near your usual baseline.');
    }
  }

  final restingHeartRateDelta = _restingHeartRateDelta(data);
  if (restingHeartRateDelta != null) {
    signalCount += 1;
    if (restingHeartRateDelta >= 10) {
      rawScore += 26;
    } else if (restingHeartRateDelta >= 6) {
      rawScore += 17;
    } else if (restingHeartRateDelta >= 3) {
      rawScore += 8;
    } else if (restingHeartRateDelta <= -4) {
      rawScore += -6;
    } else {
      rawScore += 0;
    }
    if (restingHeartRateDelta > 0) {
      factors.add(
        'Resting heart rate is +$restingHeartRateDelta bpm versus baseline.',
      );
    } else if (restingHeartRateDelta < 0) {
      factors.add(
        'Resting heart rate is ${restingHeartRateDelta.abs()} bpm below baseline.',
      );
    } else {
      factors.add('Resting heart rate is near baseline.');
    }
  }

  final averageHeartRateDelta = _averageHeartRateDeltaFromResting(data);
  if (averageHeartRateDelta != null) {
    signalCount += 1;
    if (averageHeartRateDelta >= 40) {
      rawScore += 22;
    } else if (averageHeartRateDelta >= 28) {
      rawScore += 15;
    } else if (averageHeartRateDelta >= 18) {
      rawScore += 8;
    } else if (averageHeartRateDelta <= 8) {
      rawScore += -4;
    } else {
      rawScore += 0;
    }
    factors.add(
      'Average heart rate today is $averageHeartRateDelta bpm above resting heart rate.',
    );
  }

  final hasWorkoutInfluence = data.workouts.isNotEmpty ||
      ((data.weeklyIntensityMinutes?.todayModerateEquivalentMinutes ?? 0) >= 20);
  if (hasWorkoutInfluence) {
    factors.add('Recorded activity today may raise this physiological estimate.');
    final todayIntensity =
        data.weeklyIntensityMinutes?.todayModerateEquivalentMinutes ?? 0;
    if (todayIntensity >= 60) {
      rawScore += 8;
    } else if (todayIntensity >= 30) {
      rawScore += 5;
    } else {
      rawScore += 3;
    }
    contextSignalCount += 1;
  }

  if (signalCount == 0) {
    return PhysiologicalStressEstimate.needsMoreData
        .copyWith(dataCoverage: dataCoverage);
  }

  if (data.loadedMetrics.contains(DashboardMetric.sleep) &&
      data.sleepScore.confidence != SleepScoreConfidence.noData) {
    contextSignalCount += 1;
    final sleepScore = data.sleepScore.score;
    if (sleepScore < 45) {
      rawScore += 12;
    } else if (sleepScore < 65) {
      rawScore += 6;
    } else if (sleepScore >= 82) {
      rawScore += -5;
    } else {
      rawScore += 0;
    }
    if (sleepScore < 45) {
      factors.add(
        'Sleep score is $sleepScore/100, which can raise physiological strain today.',
      );
    } else if (sleepScore < 65) {
      factors.add(
        'Sleep score is $sleepScore/100, so recovery context is mixed.',
      );
    } else if (sleepScore >= 82) {
      factors.add(
        'Sleep score is $sleepScore/100, which supports lower strain.',
      );
    } else {
      factors.add('Sleep score is $sleepScore/100.');
    }
  }

  if (data.loadedMetrics.contains(DashboardMetric.hydration)) {
    contextSignalCount += 1;
    if (data.hydrationLiters <= 0.0) {
      rawScore += 4;
    } else if (data.hydrationLiters < 1.0) {
      rawScore += 3;
    } else {
      rawScore += 0;
    }
    if (data.hydrationLiters <= 0.0) {
      factors.add(
        'No hydration is logged for today; dehydration can raise heart-rate strain.',
      );
    } else if (data.hydrationLiters < 1.0) {
      factors.add(
        'Hydration is ${_formatOneDecimal(data.hydrationLiters)} L so far today.',
      );
    } else {
      factors.add(
        'Hydration is ${_formatOneDecimal(data.hydrationLiters)} L so far today.',
      );
    }
  }

  final nutrition = _nutritionContext(data);
  if (nutrition != null) {
    contextSignalCount += 1;
    rawScore += nutrition.scoreDelta;
    factors.add(nutrition.text);
  }

  final temperature = _temperatureContext(data);
  if (temperature != null) {
    contextSignalCount += 1;
    rawScore += temperature.scoreDelta;
    factors.add(temperature.text);
  }

  final load = data.weeklyCardioLoad;
  if (load != null) {
    if (load.targetScore > 0) {
      final ratio = load.currentScore / load.targetScore.toDouble();
      if (ratio > 1.35) {
        contextSignalCount += 1;
        rawScore += 7;
        factors.add(
          'Weekly training load is ${(ratio * 100.0).round()}% of target.',
        );
      } else if (ratio >= 0.75 && ratio <= 1.20) {
        contextSignalCount += 1;
        factors.add('Weekly training load is near target.');
      }
    }
  }

  if ((data.mindfulnessMinutes ?? 0) >= 5) {
    contextSignalCount += 1;
    rawScore -= 4;
    factors.add('${data.mindfulnessMinutes} min of mindfulness is logged today.');
  }

  final score = rawScore.clamp(0, 100);
  final level = _stressLevelForScore(score);
  final label = _stressLabel(level);
  final sparseHrv = data.hrvRmssdMs != null && data.hrvSampleCount == 1;
  final confidence = _stressConfidence(
    signalCount: signalCount,
    contextSignalCount: contextSignalCount,
    hasWorkoutInfluence: hasWorkoutInfluence,
    sparseHrv: sparseHrv,
  );
  final String summary;
  switch (level) {
    case PhysiologicalStressLevel.resting:
      summary = 'Signals look restful right now.';
    case PhysiologicalStressLevel.low:
      summary = 'Signals suggest low physiological stress.';
    case PhysiologicalStressLevel.medium:
      summary = 'Signals suggest elevated physiological strain.';
    case PhysiologicalStressLevel.high:
      summary = 'Signals suggest high physiological strain.';
    case PhysiologicalStressLevel.needsMoreData:
      summary = 'Stress estimate needs more data.';
  }
  final String detail;
  switch (level) {
    case PhysiologicalStressLevel.resting:
      detail =
          'The estimate is in the resting range because HRV and heart-rate signals look calm.';
    case PhysiologicalStressLevel.low:
      detail =
          'The estimate is in the low range; this usually means no strong strain signal is visible.';
    case PhysiologicalStressLevel.medium:
      detail =
          'The estimate is elevated. Low HRV, higher resting heart rate, activity, illness, caffeine, food, heat, or alcohol can all contribute.';
    case PhysiologicalStressLevel.high:
      detail =
          'The estimate is high. Treat this as physiological strain, especially if it follows exercise, poor sleep, illness, caffeine, alcohol, heat, or dehydration.';
    case PhysiologicalStressLevel.needsMoreData:
      detail =
          'OpenVitals needs more local signals to estimate physiological stress.';
  }
  final caveats = <String>[
    ..._defaultStressCaveats,
    if (hasWorkoutInfluence)
      'Recorded workouts or active minutes are present today, so this should not be read as a quiet resting stress score.',
    if (confidence == PhysiologicalStressConfidence.low)
      'Confidence is low because only part of the HRV/resting-heart-rate context is available.',
    if (sparseHrv)
      'Only one HRV point is available for this day, so HRV context is thin.',
  ];

  return PhysiologicalStressEstimate(
    level: level,
    label: label,
    score: score,
    summary: summary,
    detail: detail,
    confidence: confidence,
    confidenceReason:
        _stressConfidenceReason(confidence, signalCount, hasWorkoutInfluence),
    hrvPercentFromBaseline: hrvPercent,
    restingHeartRateDeltaBpm: restingHeartRateDelta,
    averageHeartRateDeltaFromRestingBpm: averageHeartRateDelta,
    hasWorkoutInfluence: hasWorkoutInfluence,
    contributingFactors: factors,
    dataCoverage: dataCoverage,
    caveats: caveats,
  );
}

int? _hrvPercentFromBaseline(DashboardData data) {
  final hrv = data.hrvRmssdMs;
  if (hrv == null) return null;
  final baseline = data.hrvBaselineRmssdMs;
  if (baseline == null) return null;
  if (!data.loadedMetrics.contains(DashboardMetric.hrv) ||
      hrv <= 0.0 ||
      baseline <= 0.0) {
    return null;
  }
  return ((hrv - baseline) / baseline * 100.0).round();
}

int? _restingHeartRateDelta(DashboardData data) {
  final baseline = data.restingHeartRateBaselineBpm;
  if (baseline == null) return null;
  if (!data.loadedMetrics.contains(DashboardMetric.restingHeartRate) ||
      data.restingHeartRateBpm <= 0 ||
      baseline <= 0) {
    return null;
  }
  return data.restingHeartRateBpm - baseline;
}

int? _averageHeartRateDeltaFromResting(DashboardData data) {
  if (!data.loadedMetrics.contains(DashboardMetric.avgHeartRate) ||
      data.avgHeartRateBpm <= 0 ||
      data.restingHeartRateBpm <= 0) {
    return null;
  }
  final delta = data.avgHeartRateBpm - data.restingHeartRateBpm;
  return delta >= 0 ? delta : null;
}

PhysiologicalStressLevel _stressLevelForScore(int score) {
  if (score >= 0 && score <= 25) return PhysiologicalStressLevel.resting;
  if (score >= 26 && score <= 50) return PhysiologicalStressLevel.low;
  if (score >= 51 && score <= 75) return PhysiologicalStressLevel.medium;
  return PhysiologicalStressLevel.high;
}

String _stressLabel(PhysiologicalStressLevel level) {
  switch (level) {
    case PhysiologicalStressLevel.resting:
      return 'Resting';
    case PhysiologicalStressLevel.low:
      return 'Low';
    case PhysiologicalStressLevel.medium:
      return 'Medium';
    case PhysiologicalStressLevel.high:
      return 'High';
    case PhysiologicalStressLevel.needsMoreData:
      return 'Needs more data';
  }
}

PhysiologicalStressConfidence _stressConfidence({
  required int signalCount,
  required int contextSignalCount,
  required bool hasWorkoutInfluence,
  required bool sparseHrv,
}) {
  if (signalCount >= 3 && !hasWorkoutInfluence && !sparseHrv) {
    return PhysiologicalStressConfidence.high;
  }
  if (signalCount >= 2 || contextSignalCount >= 2) {
    return PhysiologicalStressConfidence.medium;
  }
  return PhysiologicalStressConfidence.low;
}

String _stressConfidenceReason(
  PhysiologicalStressConfidence confidence,
  int signalCount,
  bool hasWorkoutInfluence,
) {
  if (confidence == PhysiologicalStressConfidence.noData) {
    return 'no_stress_signals';
  }
  if (hasWorkoutInfluence) return 'activity_may_influence';
  if (signalCount >= 3) return 'hrv_resting_hr_average_hr';
  if (signalCount == 2) return 'partial_hrv_or_heart_rate_context';
  return 'single_signal';
}

List<String> _stressDataCoverage(DashboardData data) {
  final coverage = <String>[];
  if (data.loadedMetrics.contains(DashboardMetric.avgHeartRate)) {
    if (data.heartRateSampleCount > 0) {
      coverage.add(
        'Heart rate used ${data.heartRateSampleCount} samples '
        '${_sampleWindowText(data.heartRateSampleStartTime, data.heartRateSampleEndTime)}'
        '.',
      );
    } else if (data.avgHeartRateBpm > 0) {
      coverage.add(
        'Average heart rate is available, but raw same-day sample coverage was not available.',
      );
    } else {
      coverage.add('No heart-rate samples were available for this day.');
    }
  }
  if (data.loadedMetrics.contains(DashboardMetric.hrv)) {
    if (data.hrvSampleCount > 1) {
      coverage.add(
        'HRV used ${data.hrvSampleCount} RMSSD points '
        '${_sampleWindowText(data.hrvSampleStartTime, data.hrvSampleEndTime)}'
        '.',
      );
    } else if (data.hrvSampleCount == 1) {
      coverage.add(
        'HRV used 1 RMSSD point '
        '${_sampleWindowText(data.hrvSampleStartTime, data.hrvSampleEndTime)}'
        '; confidence is lower until more points arrive.',
      );
    } else if (data.hrvRmssdMs != null) {
      coverage.add(
        'HRV average is available, but raw same-day sample coverage was not available.',
      );
    } else {
      coverage.add('No HRV points were available for this day.');
    }
  }
  return coverage;
}

class _StressContextFactor {
  const _StressContextFactor({required this.scoreDelta, required this.text});

  final int scoreDelta;
  final String text;
}

_StressContextFactor? _nutritionContext(DashboardData data) {
  final hasNutrition =
      (data.caloriesInKcal != null && data.caloriesInKcal! > 0.0) ||
          (data.proteinGrams != null && data.proteinGrams! > 0.0) ||
          (data.carbsGrams != null && data.carbsGrams! > 0.0) ||
          (data.fatGrams != null && data.fatGrams! > 0.0);
  if (!hasNutrition) return null;
  final calories = data.caloriesInKcal;
  if (calories != null && calories >= 1000.0) {
    return const _StressContextFactor(
      scoreDelta: 3,
      text:
          'Nutrition is logged; larger meals and digestion can raise heart-rate strain.',
    );
  } else {
    return const _StressContextFactor(
      scoreDelta: 0,
      text: "Nutrition is logged for today's context.",
    );
  }
}

_StressContextFactor? _temperatureContext(DashboardData data) {
  final bodyTemperature = data.latestBodyTemperatureCelsius;
  final skinDelta = data.latestSkinTemperatureDeltaCelsius;
  final bodyLoaded =
      data.loadedMetrics.contains(DashboardMetric.bodyTemperature) &&
          bodyTemperature != null;
  final skinLoaded =
      data.loadedMetrics.contains(DashboardMetric.skinTemperature) &&
          skinDelta != null;
  if (!bodyLoaded && !skinLoaded) return null;

  final warning = (bodyTemperature != null && bodyTemperature >= 37.7) ||
      (skinDelta != null && skinDelta >= 1.0);
  final elevated = warning ||
      (bodyTemperature != null && bodyTemperature >= 37.2) ||
      (skinDelta != null && skinDelta >= 0.5);
  final detailParts = <String>[
    if (bodyTemperature != null)
      'body temperature ${_formatOneDecimal(bodyTemperature)} C',
    if (skinDelta != null)
      'skin temperature ${_formatSignedOneDecimal(skinDelta)} C',
  ];
  final detail = detailParts.join(', ');
  if (warning) {
    return _StressContextFactor(
      scoreDelta: 18,
      text: 'Temperature context is elevated ($detail).',
    );
  } else if (elevated) {
    return _StressContextFactor(
      scoreDelta: 9,
      text: 'Temperature context is slightly elevated ($detail).',
    );
  } else {
    return _StressContextFactor(
      scoreDelta: 0,
      text: 'Temperature context is available and not elevated ($detail).',
    );
  }
}

String _sampleWindowText(DateTime? start, DateTime? end) {
  if (start == null || end == null) return 'for this day';
  final startText = _formatClock(start);
  final endText = _formatClock(end);
  if (start.isAtSameMomentAs(end)) {
    return 'at $startText';
  } else {
    return 'from $startText to $endText';
  }
}

String _formatClock(DateTime instant) {
  final local = instantToLocalTime(instant);
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '$hour:$minute';
}

String _formatOneDecimal(double value) => value.toStringAsFixed(1);

String _formatSignedOneDecimal(double value) {
  final prefix = value > 0.0 ? '+' : '';
  return prefix + _formatOneDecimal(value);
}

const List<String> _defaultStressCaveats = <String>[
  'This estimate does not diagnose mental stress.',
  'Health Connect does not provide a stress score, so OpenVitals estimates physiological strain locally.',
  'Food, caffeine, alcohol, illness, heat, dehydration, exercise, and emotional excitement can all move the estimate.',
  'A true all-day stress model should use inactive-period HRV samples; this screen uses the local signals currently available.',
];
