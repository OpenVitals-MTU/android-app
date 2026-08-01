import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/daily_readiness.dart';
import '../../../l10n/app_localizations.dart';
import 'readiness_l10n.dart';

part 'daily_readiness_display.freezed.dart';

/// How many "Why" factors the panel lists (Kotlin `factors.take(5)`).
const int _maxPanelFactors = 5;

/// One "Why" row, already localized.
@freezed
abstract class ReadinessFactorDisplay with _$ReadinessFactorDisplay {
  const factory ReadinessFactorDisplay({
    required String label,
    required String detail,
    required ReadinessFactorImpact impact,
  }) = _ReadinessFactorDisplay;
}

/// The screen-ready derivation of one [DailyReadinessInsight]: every line the
/// readiness panel shows — the verdict texts, the confidence sentence, the
/// stress and strain values, the HRV/intensity summaries, and the capped
/// factor list — all rendered through the catalog, so the panel reads in the
/// app's language while the insight keeps its canonical English fields.
///
/// Built once per load by [buildDailyReadinessDisplay] and stored on the state —
/// the view-model precomputes, the widgets only render.
@freezed
abstract class DailyReadinessDisplay with _$DailyReadinessDisplay {
  const factory DailyReadinessDisplay({
    required String statusTitle,
    required String recommendation,
    required String explanation,
    required String alternative,
    required String suggestedWorkout,
    required String avoid,
    required String adaptiveGoal,
    required String confidenceText,
    required String hrvStatusValue,
    required String intensityMinutesValue,
    required String stressValue,
    required String strainValue,
    required List<ReadinessFactorDisplay> topFactors,
  }) = _DailyReadinessDisplay;
}

/// Pure derivation from the insight to its display model. No clock, no ref, no
/// I/O — unit-testable with a fixture insight and any [AppLocalizations].
DailyReadinessDisplay buildDailyReadinessDisplay(
  DailyReadinessInsight insight,
  AppLocalizations l10n,
) =>
    DailyReadinessDisplay(
      statusTitle: readinessStatusTitle(l10n, insight.state),
      recommendation: readinessRecommendation(
        l10n,
        insight.state,
        insight.recommendationType,
      ),
      explanation: readinessExplanation(l10n, insight),
      alternative: readinessAlternative(l10n, insight.state),
      suggestedWorkout: readinessSuggestedWorkout(l10n, insight.state),
      avoid: readinessAvoid(l10n, insight.state),
      adaptiveGoal: readinessAdaptiveGoal(l10n, insight),
      confidenceText: readinessConfidenceText(l10n, insight),
      hrvStatusValue: '${hrvStatusLabel(l10n, insight.hrvStatus.status)} · '
          '${hrvStatusDetail(l10n, insight.hrvStatus)}',
      intensityMinutesValue:
          '${intensityLabel(l10n, insight.intensityMinutes.status)} · '
          '${intensityDetail(l10n, insight.intensityMinutes)}',
      stressValue: _stressValue(l10n, insight),
      strainValue: readinessStrainValue(l10n, insight),
      topFactors: [
        for (final factor in insight.factors.take(_maxPanelFactors))
          () {
            final localized = localizeReadinessFactor(l10n, insight, factor);
            return ReadinessFactorDisplay(
              label: localized.label,
              detail: localized.detail,
              impact: factor.impact,
            );
          }(),
      ],
    );

String _stressValue(AppLocalizations l10n, DailyReadinessInsight insight) {
  final stress = insight.physiologicalStress;
  final score = stress.score != null ? ' · ${stress.score}/100' : '';
  return '${stressLevelLabel(l10n, stress.level)}$score · '
      '${stressLevelSummary(l10n, stress.level)}';
}
