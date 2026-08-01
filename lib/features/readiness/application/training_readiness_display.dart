import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/daily_readiness.dart';
import '../../../l10n/app_localizations.dart';
import 'readiness_l10n.dart';

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
/// I/O — unit-testable with a fixture insight and any [AppLocalizations].
TrainingReadinessDisplay buildTrainingReadinessDisplay(
  DailyReadinessInsight insight,
  AppLocalizations l10n,
) {
  final isUnknown = insight.state == ReadinessState.unknown;
  final factors = insight.factors
      .where((factor) => trainingReadinessFactorKinds.contains(factor.kind))
      .toList();
  final signals = factors.isEmpty
      ? [l10n.trainingReadinessDetailsNoSignals]
      : [
          for (final f in factors)
            () {
              final localized = localizeReadinessFactor(l10n, insight, f);
              return '${localized.label}: ${localized.detail}';
            }(),
        ];
  final guidance = <String>[
    '${l10n.dashboardReadinessRecommended}: '
        '${readinessSuggestedWorkout(l10n, insight.state)}',
    '${l10n.dashboardReadinessAvoid}: ${readinessAvoid(l10n, insight.state)}',
    '${l10n.dashboardReadinessStrain}: ${readinessStrainValue(l10n, insight)}',
  ];
  return TrainingReadinessDisplay(
    score: insight.trainingReadinessScore,
    verdict: _scoreBandLabel(l10n, insight.trainingReadinessScore, isUnknown),
    confidence: readinessConfidenceText(l10n, insight),
    signals: signals,
    guidance: guidance,
  );
}

/// Verdict band for a readiness score (Kotlin `scoreBandLabel`).
String _scoreBandLabel(AppLocalizations l10n, int score, bool isUnknown) {
  if (isUnknown) return l10n.readinessDetailsScoreNeedsMoreData;
  if (score >= 80) return l10n.readinessDetailsScoreStrong;
  if (score >= 60) return l10n.readinessDetailsScoreSteady;
  if (score >= 40) return l10n.readinessDetailsScoreLimited;
  return l10n.readinessDetailsScoreLow;
}
