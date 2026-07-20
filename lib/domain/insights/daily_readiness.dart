import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/dashboard_data.dart';
import '../model/dashboard_query.dart';
import 'intensity_minutes.dart';
import 'sleep_score.dart';
import 'stress_tracking.dart';

part 'daily_readiness.freezed.dart';

// Daily readiness is a weighted blend of recovery signals; the science lives with
// each input rather than in a single formula here.
//
// Research:
//   sleep's contribution to next-day readiness  https://pubmed.ncbi.nlm.nih.gov/24470692/
//   (sub-score sources)                          see sleep_score.dart
// The activity (intensity minutes) and stress/HRV inputs carry their own
// citations in intensity_minutes.dart and stress_tracking.dart.

enum ReadinessState {
  ready,
  moderate,
  recover,
  rest,
  unknown,
}

enum ReadinessRecommendationType {
  hardTraining,
  moderateTraining,
  lightActivity,
  mobility,
  rest,
  checkSymptoms,
}

enum ReadinessConfidence {
  high,
  medium,
  low,
}

enum ReadinessFactorImpact {
  positive(1),
  neutral(0),
  negative(2),
  warning(3);

  const ReadinessFactorImpact(this.priority);

  final int priority;
}

enum ReadinessFactorKind {
  sleepBelowBaseline,
  sleepAboveBaseline,
  restingHrElevated,
  restingHrNormal,
  hrvBelowBaseline,
  hrvAboveBaseline,
  hrvNormal,
  stressHigh,
  stressLow,
  trainingLoadHigh,
  trainingLoadNormal,
  intensityMinutesOnTarget,
  intensityMinutesBehind,
  missingIntensityMinutes,
  physiologicalStressHigh,
  physiologicalStressLow,
  missingStressData,
  temperatureElevated,
  hydrationLow,
  nutritionLogged,
  missingSleepData,
  missingHrvData,
  newUserNotEnoughBaseline,
}

enum HrvStatus {
  balanced,
  low,
  high,
  unusuallyLow,
  unusuallyHigh,
  needsMoreHrv,
}

@freezed
abstract class HrvStatusInsight with _$HrvStatusInsight {
  const factory HrvStatusInsight({
    required HrvStatus status,
    required String label,
    required String detail,
    required double? currentRmssdMs,
    required double? baselineRmssdMs,
    required int? percentFromBaseline,
  }) = _HrvStatusInsight;
}

enum IntensityMinutesStatus {
  goalMet,
  onTrack,
  behind,
  low,
  needsMoreData,
}

@freezed
abstract class IntensityMinutesReadinessInsight
    with _$IntensityMinutesReadinessInsight {
  const factory IntensityMinutesReadinessInsight({
    required IntensityMinutesStatus status,
    required String label,
    required String detail,
    required int? moderateEquivalentMinutes,
    required int targetMinutes,
    required int? todayModerateEquivalentMinutes,
    required int progressPercent,
    required IntensityMinutesConfidence confidence,
  }) = _IntensityMinutesReadinessInsight;
}

@freezed
abstract class DailyReadinessGoalInputs with _$DailyReadinessGoalInputs {
  const factory DailyReadinessGoalInputs({
    @Default(8000.0) double stepsGoal,
    @Default(2.0) double hydrationLitersGoal,
    @Default(45.0) double activeMinutesGoal,
  }) = _DailyReadinessGoalInputs;
}

@freezed
abstract class DailyReadinessFactor with _$DailyReadinessFactor {
  const factory DailyReadinessFactor({
    required ReadinessFactorKind kind,
    required String label,
    required String detail,
    required ReadinessFactorImpact impact,
  }) = _DailyReadinessFactor;
}

@freezed
abstract class DailyReadinessInsight with _$DailyReadinessInsight {
  const factory DailyReadinessInsight({
    required ReadinessState state,
    required int score,
    required int bodyEnergyScore,
    required int trainingReadinessScore,
    required ReadinessRecommendationType recommendationType,
    required String statusTitle,
    required String recommendation,
    required String explanation,
    required String alternative,
    required String suggestedWorkout,
    required String avoid,
    required String strainTarget,
    required String? currentStrain,
    required String adaptiveGoal,
    required ReadinessConfidence confidence,
    required String confidenceReason,
    required HrvStatusInsight hrvStatus,
    required IntensityMinutesReadinessInsight intensityMinutes,
    required PhysiologicalStressEstimate physiologicalStress,
    required List<DailyReadinessFactor> factors,
    required bool recoveryModeSuggested,
  }) = _DailyReadinessInsight;
}

HrvStatusInsight calculateHrvStatus({
  required double? hrvRmssdMs,
  required double? baselineRmssdMs,
  required bool hasHrvData,
}) {
  if (!hasHrvData || hrvRmssdMs == null || hrvRmssdMs <= 0.0) {
    return HrvStatusInsight(
      status: HrvStatus.needsMoreHrv,
      label: 'Needs more HRV',
      detail: 'HRV was not available for this day.',
      currentRmssdMs: hrvRmssdMs,
      baselineRmssdMs: baselineRmssdMs,
      percentFromBaseline: null,
    );
  }
  if (baselineRmssdMs == null || baselineRmssdMs <= 0.0) {
    return HrvStatusInsight(
      status: HrvStatus.needsMoreHrv,
      label: 'Needs more HRV',
      detail:
          'HRV is recorded, but there is not enough history yet for a personal baseline.',
      currentRmssdMs: hrvRmssdMs,
      baselineRmssdMs: baselineRmssdMs,
      percentFromBaseline: null,
    );
  }

  final percent = ((hrvRmssdMs - baselineRmssdMs) / baselineRmssdMs * 100.0)
      .round();
  final HrvStatus status;
  if (percent <= -30) {
    status = HrvStatus.unusuallyLow;
  } else if (percent <= -15) {
    status = HrvStatus.low;
  } else if (percent >= 30) {
    status = HrvStatus.unusuallyHigh;
  } else if (percent >= 15) {
    status = HrvStatus.high;
  } else {
    status = HrvStatus.balanced;
  }
  final String label;
  switch (status) {
    case HrvStatus.balanced:
      label = 'Balanced';
    case HrvStatus.low:
      label = 'Low';
    case HrvStatus.high:
      label = 'High';
    case HrvStatus.unusuallyLow:
      label = 'Unusually low';
    case HrvStatus.unusuallyHigh:
      label = 'Unusually high';
    case HrvStatus.needsMoreHrv:
      label = 'Needs more HRV';
  }
  final String comparison;
  if (percent == 0) {
    comparison = 'near your usual baseline';
  } else if (percent > 0) {
    comparison = '$percent% above your usual baseline';
  } else {
    comparison = '${percent.abs()}% below your usual baseline';
  }
  final String detail;
  switch (status) {
    case HrvStatus.balanced:
      detail = 'HRV is $comparison.';
    case HrvStatus.low:
      detail = 'HRV is $comparison, which can point to incomplete recovery.';
    case HrvStatus.high:
      detail =
          'HRV is $comparison. Higher HRV can be positive when other signals agree.';
    case HrvStatus.unusuallyLow:
      detail = 'HRV is $comparison, outside your usual range.';
    case HrvStatus.unusuallyHigh:
      detail = 'HRV is $comparison, outside your usual range.';
    case HrvStatus.needsMoreHrv:
      detail = 'HRV status needs more data.';
  }
  return HrvStatusInsight(
    status: status,
    label: label,
    detail: detail,
    currentRmssdMs: hrvRmssdMs,
    baselineRmssdMs: baselineRmssdMs,
    percentFromBaseline: percent,
  );
}

IntensityMinutesReadinessInsight calculateIntensityMinutesReadiness({
  required DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes,
  required bool hasIntensityData,
}) {
  if (!hasIntensityData ||
      weeklyIntensityMinutes == null ||
      weeklyIntensityMinutes.confidence == IntensityMinutesConfidence.noData) {
    return const IntensityMinutesReadinessInsight(
      status: IntensityMinutesStatus.needsMoreData,
      label: 'Needs more data',
      detail:
          'Intensity minutes need workouts, heart rate, active calories, or activity load history.',
      moderateEquivalentMinutes: null,
      targetMinutes: defaultWeeklyIntensityMinutesTarget,
      todayModerateEquivalentMinutes: null,
      progressPercent: 0,
      confidence: IntensityMinutesConfidence.noData,
    );
  }

  final minutes = weeklyIntensityMinutes.moderateEquivalentMinutes;
  final target = weeklyIntensityMinutes.targetMinutes;
  final IntensityMinutesStatus status;
  if (target > 0 && minutes >= target) {
    status = IntensityMinutesStatus.goalMet;
  } else if (weeklyIntensityMinutes.isOnPace) {
    status = IntensityMinutesStatus.onTrack;
  } else if (target > 0 && minutes >= target * 0.5) {
    status = IntensityMinutesStatus.behind;
  } else {
    status = IntensityMinutesStatus.low;
  }
  final String label;
  switch (status) {
    case IntensityMinutesStatus.goalMet:
      label = 'Goal met';
    case IntensityMinutesStatus.onTrack:
      label = 'On track';
    case IntensityMinutesStatus.behind:
      label = 'Behind pace';
    case IntensityMinutesStatus.low:
      label = 'Low';
    case IntensityMinutesStatus.needsMoreData:
      label = 'Needs more data';
  }
  final String confidenceText;
  switch (weeklyIntensityMinutes.confidence) {
    case IntensityMinutesConfidence.high:
      confidenceText = 'high confidence';
    case IntensityMinutesConfidence.medium:
      confidenceText = 'medium confidence';
    case IntensityMinutesConfidence.low:
      confidenceText = 'low confidence estimate';
    case IntensityMinutesConfidence.noData:
      confidenceText = 'no data';
  }
  final todayText = weeklyIntensityMinutes.todayModerateEquivalentMinutes > 0
      ? ' Today added ${weeklyIntensityMinutes.todayModerateEquivalentMinutes}.'
      : '';
  final String detail;
  switch (status) {
    case IntensityMinutesStatus.goalMet:
      detail =
          '$minutes/$target moderate-equivalent min this week; vigorous minutes count double.$todayText';
    case IntensityMinutesStatus.onTrack:
      detail =
          '$minutes/$target moderate-equivalent min this week, on pace for day ${weeklyIntensityMinutes.daysElapsed}.';
    case IntensityMinutesStatus.behind:
      detail =
          '$minutes/$target moderate-equivalent min this week; expected about ${weeklyIntensityMinutes.expectedByNowMinutes} by now.';
    case IntensityMinutesStatus.low:
      detail =
          '$minutes/$target moderate-equivalent min this week; add easy aerobic work if recovery allows.';
    case IntensityMinutesStatus.needsMoreData:
      detail = 'Intensity minutes need more data.';
  }
  return IntensityMinutesReadinessInsight(
    status: status,
    label: label,
    detail: '$detail $confidenceText.',
    moderateEquivalentMinutes: minutes,
    targetMinutes: target,
    todayModerateEquivalentMinutes:
        weeklyIntensityMinutes.todayModerateEquivalentMinutes,
    progressPercent: weeklyIntensityMinutes.progressPercent,
    confidence: weeklyIntensityMinutes.confidence,
  );
}

DailyReadinessInsight calculateDailyReadiness(
  DashboardData data, {
  DailyReadinessGoalInputs goals = const DailyReadinessGoalInputs(),
}) {
  var score = 68;
  var bodyEnergyScore = 64;
  var trainingReadinessScore = 66;
  var availableSignals = 0;
  var baselineSignals = 0;
  var elevatedBodySignals = 0;
  var unusualVitals = false;
  final missingReasons = <String>[];
  final factors = <DailyReadinessFactor>[];

  void addFactor({
    required ReadinessFactorKind kind,
    required String label,
    required String detail,
    required ReadinessFactorImpact impact,
  }) {
    factors.add(
      DailyReadinessFactor(
        kind: kind,
        label: label,
        detail: detail,
        impact: impact,
      ),
    );
  }

  if (data.loadedMetrics.contains(DashboardMetric.sleep) &&
      data.sleepScore.confidence != SleepScoreConfidence.noData) {
    availableSignals += 1;
    final sleepScore = data.sleepScore.score;
    final sleepHours = data.sleepScore.sleepDurationMinutes / 60.0;
    final sleepDetail = sleepHours > 0.0
        ? 'Sleep scored $sleepScore/100 after ${_formatHours(sleepHours)}.'
        : 'Sleep scored $sleepScore/100.';
    if (sleepScore >= 82) {
      score += 12;
      bodyEnergyScore += 16;
      trainingReadinessScore += 8;
      addFactor(
        kind: ReadinessFactorKind.sleepAboveBaseline,
        label: 'Sleep helped recovery',
        detail: sleepDetail,
        impact: ReadinessFactorImpact.positive,
      );
    } else if (sleepScore >= 65) {
      score += 5;
      bodyEnergyScore += 7;
      trainingReadinessScore += 3;
      addFactor(
        kind: ReadinessFactorKind.sleepAboveBaseline,
        label: 'Sleep was usable',
        detail: sleepDetail,
        impact: ReadinessFactorImpact.neutral,
      );
    } else if (sleepScore >= 45) {
      score -= 8;
      bodyEnergyScore -= 12;
      trainingReadinessScore -= 6;
      addFactor(
        kind: ReadinessFactorKind.sleepBelowBaseline,
        label: 'Sleep may limit recovery',
        detail: sleepDetail,
        impact: ReadinessFactorImpact.negative,
      );
    } else {
      score -= 20;
      bodyEnergyScore -= 24;
      trainingReadinessScore -= 14;
      elevatedBodySignals += 1;
      addFactor(
        kind: ReadinessFactorKind.sleepBelowBaseline,
        label: 'Sleep was low',
        detail: sleepDetail,
        impact: ReadinessFactorImpact.warning,
      );
    }
  } else {
    score -= 6;
    missingReasons.add('missing_sleep_data');
    addFactor(
      kind: ReadinessFactorKind.missingSleepData,
      label: 'Sleep data missing',
      detail:
          "Sleep data was not available, so today's recommendation is a rough guide.",
      impact: ReadinessFactorImpact.neutral,
    );
  }

  final hrv = data.hrvRmssdMs;
  final hrvBaseline = data.hrvBaselineRmssdMs;
  final hrvStatus = calculateHrvStatus(
    hrvRmssdMs: hrv,
    baselineRmssdMs: hrvBaseline,
    hasHrvData: data.loadedMetrics.contains(DashboardMetric.hrv),
  );
  if (data.loadedMetrics.contains(DashboardMetric.hrv) &&
      hrv != null &&
      hrv > 0.0) {
    availableSignals += 1;
    if (hrvBaseline != null && hrvBaseline > 0.0) {
      baselineSignals += 1;
      switch (hrvStatus.status) {
        case HrvStatus.unusuallyLow:
          score -= 17;
          bodyEnergyScore -= 11;
          trainingReadinessScore -= 19;
          elevatedBodySignals += 1;
          addFactor(
            kind: ReadinessFactorKind.hrvBelowBaseline,
            label: 'HRV Status: ${hrvStatus.label}',
            detail: hrvStatus.detail,
            impact: ReadinessFactorImpact.warning,
          );
        case HrvStatus.low:
          score -= 8;
          bodyEnergyScore -= 5;
          trainingReadinessScore -= 10;
          addFactor(
            kind: ReadinessFactorKind.hrvBelowBaseline,
            label: 'HRV Status: ${hrvStatus.label}',
            detail: hrvStatus.detail,
            impact: ReadinessFactorImpact.negative,
          );
        case HrvStatus.unusuallyHigh:
          score -= 4;
          bodyEnergyScore -= 2;
          trainingReadinessScore -= 3;
          addFactor(
            kind: ReadinessFactorKind.hrvAboveBaseline,
            label: 'HRV Status: ${hrvStatus.label}',
            detail: hrvStatus.detail,
            impact: ReadinessFactorImpact.negative,
          );
        case HrvStatus.high:
          score += 3;
          bodyEnergyScore += 2;
          trainingReadinessScore += 3;
          addFactor(
            kind: ReadinessFactorKind.hrvAboveBaseline,
            label: 'HRV Status: ${hrvStatus.label}',
            detail: hrvStatus.detail,
            impact: ReadinessFactorImpact.positive,
          );
        case HrvStatus.balanced:
          score += 5;
          bodyEnergyScore += 3;
          trainingReadinessScore += 6;
          addFactor(
            kind: ReadinessFactorKind.hrvNormal,
            label: 'HRV Status: ${hrvStatus.label}',
            detail: hrvStatus.detail,
            impact: ReadinessFactorImpact.positive,
          );
        case HrvStatus.needsMoreHrv:
          break;
      }
    } else {
      missingReasons.add('new_user_not_enough_baseline');
      addFactor(
        kind: ReadinessFactorKind.newUserNotEnoughBaseline,
        label: 'HRV baseline building',
        detail:
            'HRV is recorded, but there is not enough history yet for a personal baseline.',
        impact: ReadinessFactorImpact.neutral,
      );
    }
  } else {
    missingReasons.add('missing_hrv_data');
    addFactor(
      kind: ReadinessFactorKind.missingHrvData,
      label: 'HRV data missing',
      detail: 'HRV was not available, which lowers recommendation confidence.',
      impact: ReadinessFactorImpact.neutral,
    );
  }

  if (data.loadedMetrics.contains(DashboardMetric.restingHeartRate) &&
      data.restingHeartRateBpm > 0) {
    availableSignals += 1;
    final baseline = data.restingHeartRateBaselineBpm;
    if (baseline != null && baseline > 0) {
      baselineSignals += 1;
      final delta = data.restingHeartRateBpm - baseline;
      final String detail;
      if (delta > 0) {
        detail =
            'Resting heart rate is +$delta bpm compared with your usual baseline.';
      } else if (delta < 0) {
        detail =
            'Resting heart rate is ${delta.abs()} bpm below your usual baseline.';
      } else {
        detail = 'Resting heart rate is near your usual baseline.';
      }
      if (delta >= 8) {
        score -= 16;
        bodyEnergyScore -= 12;
        trainingReadinessScore -= 12;
        elevatedBodySignals += 1;
        addFactor(
          kind: ReadinessFactorKind.restingHrElevated,
          label: 'Resting HR is elevated',
          detail: detail,
          impact: ReadinessFactorImpact.warning,
        );
      } else if (delta >= 4) {
        score -= 8;
        bodyEnergyScore -= 5;
        trainingReadinessScore -= 6;
        addFactor(
          kind: ReadinessFactorKind.restingHrElevated,
          label: 'Resting HR is slightly elevated',
          detail: detail,
          impact: ReadinessFactorImpact.negative,
        );
      } else {
        score += 4;
        bodyEnergyScore += 3;
        trainingReadinessScore += 3;
        addFactor(
          kind: ReadinessFactorKind.restingHrNormal,
          label: 'Resting HR looks normal',
          detail: detail,
          impact: ReadinessFactorImpact.positive,
        );
      }
    } else {
      missingReasons.add('new_user_not_enough_baseline');
      addFactor(
        kind: ReadinessFactorKind.newUserNotEnoughBaseline,
        label: 'Resting HR baseline building',
        detail:
            'Resting heart rate is available, but there is not enough history yet for a personal baseline.',
        impact: ReadinessFactorImpact.neutral,
      );
    }
  }

  final intensityMinutes = calculateIntensityMinutesReadiness(
    weeklyIntensityMinutes: data.weeklyIntensityMinutes,
    hasIntensityData:
        data.loadedMetrics.contains(DashboardMetric.intensityMinutes),
  );
  final physiologicalStress = calculatePhysiologicalStress(data);

  final load = data.weeklyCardioLoad;
  if (load != null) {
    availableSignals += 1;
    final ratio =
        load.targetScore > 0 ? load.currentScore / load.targetScore.toDouble() : null;
    if (ratio == null) {
      // No usable ratio.
    } else if (ratio > 1.35) {
      score -= 12;
      bodyEnergyScore -= 8;
      trainingReadinessScore -= 13;
      elevatedBodySignals += 1;
      addFactor(
        kind: ReadinessFactorKind.trainingLoadHigh,
        label: 'Training load is high',
        detail:
            'This week is ${(ratio * 100.0).round()}% of your current load target.',
        impact: ReadinessFactorImpact.warning,
      );
    } else if (ratio >= 0.75 && ratio <= 1.20) {
      score += 4;
      trainingReadinessScore += 5;
      addFactor(
        kind: ReadinessFactorKind.trainingLoadNormal,
        label: 'Training load is stable',
        detail:
            'This week is ${(ratio * 100.0).round()}% of your current load target.',
        impact: ReadinessFactorImpact.positive,
      );
    } else {
      addFactor(
        kind: ReadinessFactorKind.trainingLoadNormal,
        label: 'Training load is light',
        detail:
            'This week is ${(ratio * 100.0).round()}% of your current load target.',
        impact: ReadinessFactorImpact.neutral,
      );
    }
  }

  if (data.loadedMetrics.contains(DashboardMetric.intensityMinutes)) {
    if (data.weeklyIntensityMinutes != null &&
        data.weeklyIntensityMinutes!.confidence !=
            IntensityMinutesConfidence.noData) {
      availableSignals += 1;
      switch (intensityMinutes.status) {
        case IntensityMinutesStatus.goalMet:
          score += 3;
          trainingReadinessScore += 6;
          addFactor(
            kind: ReadinessFactorKind.intensityMinutesOnTarget,
            label: 'Intensity minutes goal met',
            detail: intensityMinutes.detail,
            impact: ReadinessFactorImpact.positive,
          );
        case IntensityMinutesStatus.onTrack:
          score += 2;
          trainingReadinessScore += 4;
          addFactor(
            kind: ReadinessFactorKind.intensityMinutesOnTarget,
            label: 'Intensity minutes on track',
            detail: intensityMinutes.detail,
            impact: ReadinessFactorImpact.positive,
          );
        case IntensityMinutesStatus.behind:
          addFactor(
            kind: ReadinessFactorKind.intensityMinutesBehind,
            label: 'Intensity minutes behind pace',
            detail: intensityMinutes.detail,
            impact: ReadinessFactorImpact.neutral,
          );
        case IntensityMinutesStatus.low:
          trainingReadinessScore -= 2;
          addFactor(
            kind: ReadinessFactorKind.intensityMinutesBehind,
            label: 'Intensity minutes are low',
            detail: intensityMinutes.detail,
            impact: ReadinessFactorImpact.neutral,
          );
        case IntensityMinutesStatus.needsMoreData:
          break;
      }
    } else {
      addFactor(
        kind: ReadinessFactorKind.missingIntensityMinutes,
        label: 'Intensity minutes need more data',
        detail: intensityMinutes.detail,
        impact: ReadinessFactorImpact.neutral,
      );
    }
  }

  switch (physiologicalStress.level) {
    case PhysiologicalStressLevel.high:
      addFactor(
        kind: ReadinessFactorKind.physiologicalStressHigh,
        label: 'Physiological stress: ${physiologicalStress.label}',
        detail: physiologicalStress.summary,
        impact: ReadinessFactorImpact.warning,
      );
    case PhysiologicalStressLevel.medium:
      addFactor(
        kind: ReadinessFactorKind.physiologicalStressHigh,
        label: 'Physiological stress: ${physiologicalStress.label}',
        detail: physiologicalStress.summary,
        impact: ReadinessFactorImpact.negative,
      );
    case PhysiologicalStressLevel.resting:
    case PhysiologicalStressLevel.low:
      addFactor(
        kind: ReadinessFactorKind.physiologicalStressLow,
        label: 'Physiological stress: ${physiologicalStress.label}',
        detail: physiologicalStress.summary,
        impact: ReadinessFactorImpact.positive,
      );
    case PhysiologicalStressLevel.needsMoreData:
      addFactor(
        kind: ReadinessFactorKind.missingStressData,
        label: 'Physiological stress needs more data',
        detail: physiologicalStress.summary,
        impact: ReadinessFactorImpact.neutral,
      );
  }

  final bodyTemperature = data.latestBodyTemperatureCelsius;
  final skinDelta = data.latestSkinTemperatureDeltaCelsius;
  if ((data.loadedMetrics.contains(DashboardMetric.bodyTemperature) &&
          bodyTemperature != null) ||
      (data.loadedMetrics.contains(DashboardMetric.skinTemperature) &&
          skinDelta != null)) {
    availableSignals += 1;
    final tempWarning = (bodyTemperature != null && bodyTemperature >= 37.7) ||
        (skinDelta != null && skinDelta >= 1.0);
    final tempElevated = tempWarning ||
        (bodyTemperature != null && bodyTemperature >= 37.2) ||
        (skinDelta != null && skinDelta >= 0.5);
    if (tempElevated) {
      final detailParts = <String>[
        if (bodyTemperature != null)
          'body temperature ${_formatOneDecimal(bodyTemperature)} C',
        if (skinDelta != null)
          'skin temperature ${_formatSignedOneDecimal(skinDelta)} C',
      ];
      final detail = detailParts.join(', ');
      score -= tempWarning ? 20 : 10;
      bodyEnergyScore -= tempWarning ? 18 : 8;
      trainingReadinessScore -= tempWarning ? 18 : 9;
      elevatedBodySignals += 1;
      unusualVitals = tempWarning;
      addFactor(
        kind: ReadinessFactorKind.temperatureElevated,
        label: 'Temperature signal elevated',
        detail:
            'Some temperature signals look elevated ($detail). If you feel unwell, consider resting.',
        impact: tempWarning
            ? ReadinessFactorImpact.warning
            : ReadinessFactorImpact.negative,
      );
    }
  }

  if (data.loadedMetrics.contains(DashboardMetric.hydration) &&
      goals.hydrationLitersGoal > 0.0) {
    availableSignals += 1;
    final hydrationRatio = data.hydrationLiters / goals.hydrationLitersGoal;
    if (hydrationRatio < 0.35) {
      score -= 4;
      bodyEnergyScore -= 4;
      addFactor(
        kind: ReadinessFactorKind.hydrationLow,
        label: 'Hydration is behind',
        detail:
            "Hydration is ${(hydrationRatio * 100.0).round()}% of today's goal.",
        impact: ReadinessFactorImpact.negative,
      );
    }
  }

  if (_hasLoggedNutrition(data)) {
    availableSignals += 1;
    bodyEnergyScore += 2;
    addFactor(
      kind: ReadinessFactorKind.nutritionLogged,
      label: 'Nutrition is logged',
      detail: "Meal data is available for today's energy context.",
      impact: ReadinessFactorImpact.positive,
    );
  }

  if ((data.mindfulnessMinutes ?? 0) >= 5) {
    availableSignals += 1;
    score += 2;
    bodyEnergyScore += 3;
    addFactor(
      kind: ReadinessFactorKind.stressLow,
      label: 'Recovery moment recorded',
      detail: '${data.mindfulnessMinutes} min of mindfulness is logged today.',
      impact: ReadinessFactorImpact.positive,
    );
  }

  if (elevatedBodySignals >= 2) {
    score -= 6;
    bodyEnergyScore -= 6;
    trainingReadinessScore -= 8;
    addFactor(
      kind: ReadinessFactorKind.stressHigh,
      label: 'Body signals look elevated',
      detail: 'Several recovery signals are outside your usual range.',
      impact: ReadinessFactorImpact.warning,
    );
  }

  final clampedScore = availableSignals == 0 ? 0 : score.clamp(0, 100);
  final ReadinessState state;
  if (availableSignals == 0) {
    state = ReadinessState.unknown;
  } else if (unusualVitals && clampedScore < 55) {
    state = ReadinessState.rest;
  } else if (clampedScore >= 80) {
    state = ReadinessState.ready;
  } else if (clampedScore >= 60) {
    state = ReadinessState.moderate;
  } else if (clampedScore >= 40) {
    state = ReadinessState.recover;
  } else {
    state = ReadinessState.rest;
  }
  final ReadinessRecommendationType recommendationType;
  if (unusualVitals && state == ReadinessState.rest) {
    recommendationType = ReadinessRecommendationType.checkSymptoms;
  } else if (state == ReadinessState.ready) {
    recommendationType = ReadinessRecommendationType.hardTraining;
  } else if (state == ReadinessState.moderate) {
    recommendationType = ReadinessRecommendationType.moderateTraining;
  } else if (state == ReadinessState.recover) {
    recommendationType = ReadinessRecommendationType.lightActivity;
  } else if (state == ReadinessState.rest) {
    recommendationType = ReadinessRecommendationType.rest;
  } else {
    recommendationType = ReadinessRecommendationType.mobility;
  }
  final ReadinessConfidence confidence;
  if (availableSignals >= 5 && baselineSignals >= 2 && missingReasons.isEmpty) {
    confidence = ReadinessConfidence.high;
  } else if (availableSignals >= 3 && baselineSignals >= 1) {
    confidence = ReadinessConfidence.medium;
  } else {
    confidence = ReadinessConfidence.low;
  }
  final String confidenceReason;
  if (confidence == ReadinessConfidence.high) {
    confidenceReason = 'complete_data';
  } else if (missingReasons.contains('missing_sleep_data')) {
    confidenceReason = 'missing_sleep_data';
  } else if (missingReasons.contains('missing_hrv_data')) {
    confidenceReason = 'missing_hrv_data';
  } else if (missingReasons.contains('new_user_not_enough_baseline')) {
    confidenceReason = 'new_user_not_enough_baseline';
  } else {
    confidenceReason = 'partial_data';
  }

  return DailyReadinessInsight(
    state: state,
    score: clampedScore,
    bodyEnergyScore: availableSignals == 0 ? 0 : bodyEnergyScore.clamp(0, 100),
    trainingReadinessScore:
        availableSignals == 0 ? 0 : trainingReadinessScore.clamp(0, 100),
    recommendationType: recommendationType,
    statusTitle: _statusTitleFor(state),
    recommendation: _recommendationFor(state, recommendationType),
    explanation: _explanationFor(state, factors),
    alternative: _alternativeFor(state),
    suggestedWorkout: _suggestedWorkoutFor(state),
    avoid: _avoidFor(state),
    strainTarget: _strainTargetFor(state),
    currentStrain: _currentStrainFor(data),
    adaptiveGoal: _adaptiveGoalFor(state, goals),
    confidence: confidence,
    confidenceReason: confidenceReason,
    hrvStatus: hrvStatus,
    intensityMinutes: intensityMinutes,
    physiologicalStress: physiologicalStress,
    factors: _sortedFactors(factors),
    recoveryModeSuggested: state == ReadinessState.rest || clampedScore < 35,
  );
}

List<DailyReadinessFactor> _sortedFactors(List<DailyReadinessFactor> factors) {
  final indexed = <MapEntry<int, DailyReadinessFactor>>[
    for (var i = 0; i < factors.length; i++) MapEntry(i, factors[i]),
  ];
  indexed.sort((a, b) {
    final byPriority = b.value.impact.priority.compareTo(a.value.impact.priority);
    if (byPriority != 0) return byPriority;
    final byLabel = a.value.label.compareTo(b.value.label);
    if (byLabel != 0) return byLabel;
    return a.key.compareTo(b.key);
  });
  return indexed.map((entry) => entry.value).toList();
}

bool _hasLoggedNutrition(DashboardData data) =>
    (data.caloriesInKcal != null && data.caloriesInKcal! > 0.0) ||
    (data.proteinGrams != null && data.proteinGrams! > 0.0) ||
    (data.carbsGrams != null && data.carbsGrams! > 0.0) ||
    (data.fatGrams != null && data.fatGrams! > 0.0);

String _statusTitleFor(ReadinessState state) {
  switch (state) {
    case ReadinessState.ready:
      return 'Ready to train';
    case ReadinessState.moderate:
      return 'Train, but keep it controlled';
    case ReadinessState.recover:
      return 'Recovery day';
    case ReadinessState.rest:
      return 'Take it easy';
    case ReadinessState.unknown:
      return 'Needs more data';
  }
}

String _recommendationFor(
  ReadinessState state,
  ReadinessRecommendationType type,
) {
  switch (type) {
    case ReadinessRecommendationType.hardTraining:
      return 'Good day for hard training if you feel normal.';
    case ReadinessRecommendationType.moderateTraining:
      return 'Do moderate training today, but avoid maximal effort.';
    case ReadinessRecommendationType.lightActivity:
      return 'Keep it light today with easy movement or mobility.';
    case ReadinessRecommendationType.rest:
      return 'Avoid intense training today and focus on recovery.';
    case ReadinessRecommendationType.checkSymptoms:
      return 'Some signals are outside your usual range. If you feel unwell, prioritize rest and hydration.';
    case ReadinessRecommendationType.mobility:
      if (state == ReadinessState.unknown) {
        return 'Connect sleep, HRV, resting heart rate, and workouts for a better guide.';
      }
      return 'Choose mobility or an easy walk until more data is available.';
  }
}

String _explanationFor(
  ReadinessState state,
  List<DailyReadinessFactor> factors,
) {
  final filtered = factors
      .where(
        (factor) =>
            factor.kind != ReadinessFactorKind.missingSleepData &&
            factor.kind != ReadinessFactorKind.missingHrvData &&
            factor.kind != ReadinessFactorKind.missingIntensityMinutes &&
            factor.kind != ReadinessFactorKind.missingStressData &&
            factor.kind != ReadinessFactorKind.newUserNotEnoughBaseline,
      )
      .toList();
  final indexed = <MapEntry<int, DailyReadinessFactor>>[
    for (var i = 0; i < filtered.length; i++) MapEntry(i, filtered[i]),
  ];
  indexed.sort((a, b) {
    final byPriority =
        b.value.impact.priority.compareTo(a.value.impact.priority);
    if (byPriority != 0) return byPriority;
    return a.key.compareTo(b.key);
  });
  final meaningful =
      indexed.map((entry) => entry.value).take(3).toList();
  if (meaningful.isEmpty) {
    return state == ReadinessState.unknown
        ? 'There is not enough local data yet to explain readiness with confidence.'
        : 'Your recommendation is based on the local signals available today.';
  }
  return 'Your signals suggest this mainly because '
      '${_joinToNaturalText(meaningful, (factor) => _toExplanationClause(factor.detail))}'
      '.';
}

String _toExplanationClause(String value) {
  final lowered = value.isEmpty
      ? value
      : value[0].toLowerCase() + value.substring(1);
  final trimmed = lowered.replaceFirst(RegExp(r'\s+$'), '');
  return trimmed.replaceFirst(RegExp(r'\.+$'), '');
}

String _alternativeFor(ReadinessState state) {
  switch (state) {
    case ReadinessState.ready:
      return 'If you feel sore, reduce the plan to 30-45 min easy cardio or mobility.';
    case ReadinessState.moderate:
      return 'If you feel tired, choose a 30 min easy walk or easy ride instead.';
    case ReadinessState.recover:
      return 'If you still want movement, keep it conversational and stop if it feels harder than expected.';
    case ReadinessState.rest:
      return 'If you feel unwell, rest or seek medical advice when needed.';
    case ReadinessState.unknown:
      return 'Start with a walk, mobility, or breathing session until more recovery data is available.';
  }
}

String _suggestedWorkoutFor(ReadinessState state) {
  switch (state) {
    case ReadinessState.ready:
      return 'Strength training, intervals, a long bike ride, or your normal planned workout.';
    case ReadinessState.moderate:
      return 'Zone 2 cardio, moderate strength, technique work, or an easy bike ride.';
    case ReadinessState.recover:
      return 'Walk, stretching, mobility, breathing, or an early night.';
    case ReadinessState.rest:
      return 'Rest, gentle movement only, and symptom monitoring if you feel unwell.';
    case ReadinessState.unknown:
      return 'Easy walk, mobility, or light stretching.';
  }
}

String _avoidFor(ReadinessState state) {
  switch (state) {
    case ReadinessState.ready:
      return 'Overreaching if you feel sore.';
    case ReadinessState.moderate:
      return 'Max effort, HIIT, and very long sessions.';
    case ReadinessState.recover:
      return 'Hard cardio, heavy lifting, and long intense sessions.';
    case ReadinessState.rest:
      return 'Intense training today.';
    case ReadinessState.unknown:
      return 'Making a hard training decision from incomplete data.';
  }
}

String _strainTargetFor(ReadinessState state) {
  switch (state) {
    case ReadinessState.ready:
      return "Today's strain target: 10-14";
    case ReadinessState.moderate:
      return "Today's strain target: 7-10";
    case ReadinessState.recover:
      return "Today's strain target: 3-6";
    case ReadinessState.rest:
      return "Today's strain target: 0-3";
    case ReadinessState.unknown:
      return "Today's strain target: 3-6";
  }
}

String? _currentStrainFor(DashboardData data) {
  final load = data.weeklyCardioLoad;
  if (load == null) return null;
  if (load.targetScore <= 0) return null;
  final dailyTarget = math.max(load.targetScore / 7.0, 1.0);
  final strain = (load.todayScore / dailyTarget * 7.0).clamp(0.0, 15.0);
  return 'Current strain: ${_formatOneDecimal(strain)}';
}

String _adaptiveGoalFor(ReadinessState state, DailyReadinessGoalInputs goals) {
  final normalSteps = _roundToNearestHundred(goals.stepsGoal);
  switch (state) {
    case ReadinessState.ready:
      return 'Adaptive goal: ${_roundToNearestHundred(goals.stepsGoal * 1.1)} steps + workout';
    case ReadinessState.moderate:
      return 'Adaptive goal: $normalSteps steps + ${goals.activeMinutesGoal.round()} active minutes';
    case ReadinessState.recover:
      return 'Adaptive goal: ${_roundToNearestHundred(goals.stepsGoal * 0.5)} steps + 15 min mobility';
    case ReadinessState.rest:
      return 'Adaptive goal: goals are reduced; focus on rest and hydration';
    case ReadinessState.unknown:
      return 'Adaptive goal: keep movement light until more data is available';
  }
}

int _roundToNearestHundred(double value) =>
    math.max(0, (value / 100.0).round() * 100);

String _formatHours(double hours) {
  final totalMinutes = math.max(0, (hours * 60.0).round());
  final h = totalMinutes ~/ 60;
  final m = totalMinutes % 60;
  return '${h}h ${m.toString().padLeft(2, '0')}m';
}

String _formatOneDecimal(double value) => value.toStringAsFixed(1);

String _formatSignedOneDecimal(double value) {
  final prefix = value > 0.0 ? '+' : '';
  return prefix + _formatOneDecimal(value);
}

String _joinToNaturalText(
  List<DailyReadinessFactor> items,
  String Function(DailyReadinessFactor) transform,
) {
  switch (items.length) {
    case 0:
      return '';
    case 1:
      return transform(items.first);
    case 2:
      return '${transform(items[0])} and ${transform(items[1])}';
    default:
      final head = items
          .sublist(0, items.length - 1)
          .map(transform)
          .join(', ');
      return '$head, and ${transform(items.last)}';
  }
}
