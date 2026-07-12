import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/daily_readiness.dart';

part 'training_readiness_display.freezed.dart';

/// The training-side factor kinds shown on the training-readiness detail (Kotlin
/// `TrainingReadinessFactorKinds`).
const Set<ReadinessFactorKind> trainingReadinessFactorKinds =
    <ReadinessFactorKind>{
  ReadinessFactorKind.sleepBelowBaseline,
  ReadinessFactorKind.sleepAboveBaseline,
  ReadinessFactorKind.restingHrElevated,
  ReadinessFactorKind.restingHrNormal,
  ReadinessFactorKind.hrvBelowBaseline,
  ReadinessFactorKind.hrvAboveBaseline,
  ReadinessFactorKind.hrvNormal,
  ReadinessFactorKind.trainingLoadHigh,
  ReadinessFactorKind.trainingLoadNormal,
  ReadinessFactorKind.intensityMinutesOnTarget,
  ReadinessFactorKind.intensityMinutesBehind,
  ReadinessFactorKind.physiologicalStressHigh,
  ReadinessFactorKind.physiologicalStressLow,
  ReadinessFactorKind.stressHigh,
  ReadinessFactorKind.temperatureElevated,
  ReadinessFactorKind.missingSleepData,
  ReadinessFactorKind.missingHrvData,
  ReadinessFactorKind.missingIntensityMinutes,
  ReadinessFactorKind.missingStressData,
  ReadinessFactorKind.newUserNotEnoughBaseline,
};

/// The screen-ready derivation of one [DailyReadinessInsight] for the
/// training-readiness detail: the score and its verdict band, the confidence
/// line, the training-side signals that were actually used, and the guidance
/// bullets.
///
/// Built once per load by [buildTrainingReadinessDisplay] and stored on the
/// state — the view-model precomputes, the widgets only render.
@freezed
abstract class TrainingReadinessDisplay with _$TrainingReadinessDisplay {
  const factory TrainingReadinessDisplay({
    required int score,
    required String verdict,
    required String confidence,
    required List<String> signals,
    required List<String> guidance,
  }) = _TrainingReadinessDisplay;
}

/// Pure derivation from the insight to its display model. No clock, no ref, no
/// I/O — unit-testable with a fixture insight.
TrainingReadinessDisplay buildTrainingReadinessDisplay(
  DailyReadinessInsight insight,
) {
  final isUnknown = insight.state == ReadinessState.unknown;
  final factors = insight.factors
      .where((factor) => trainingReadinessFactorKinds.contains(factor.kind))
      .toList();
  final signals = factors.isEmpty
      ? const ['No usable training-side signals were available.']
      : [for (final f in factors) '${f.label}: ${f.detail}'];
  final strain = [
    insight.strainTarget,
    if (insight.currentStrain != null) insight.currentStrain!,
  ].join(' · ');
  final guidance = <String>[
    'Recommended: ${insight.suggestedWorkout}',
    'Avoid: ${insight.avoid}',
    if (strain.trim().isNotEmpty) 'Strain target: $strain',
  ];
  return TrainingReadinessDisplay(
    score: insight.trainingReadinessScore,
    verdict: _scoreBandLabel(insight.trainingReadinessScore, isUnknown),
    confidence: _confidenceText(insight.confidence, insight.confidenceReason),
    signals: signals,
    guidance: guidance,
  );
}

/// Verdict band for a readiness score (Kotlin `scoreBandLabel`).
String _scoreBandLabel(int score, bool isUnknown) {
  if (isUnknown) return 'Needs more data';
  if (score >= 80) return 'Strong';
  if (score >= 60) return 'Steady';
  if (score >= 40) return 'Limited';
  return 'Low';
}

/// Confidence line (Kotlin `readinessConfidenceText`).
String _confidenceText(ReadinessConfidence confidence, String reason) {
  final label = switch (confidence) {
    ReadinessConfidence.high => 'High confidence',
    ReadinessConfidence.medium => 'Medium confidence',
    ReadinessConfidence.low => 'Low confidence',
  };
  final reasonLabel = switch (reason) {
    'complete_data' => 'complete local data',
    'missing_sleep_data' => 'sleep data missing',
    'missing_hrv_data' => 'HRV data missing',
    'new_user_not_enough_baseline' => 'baseline still building',
    _ => 'partial local data',
  };
  return '$label · $reasonLabel';
}
