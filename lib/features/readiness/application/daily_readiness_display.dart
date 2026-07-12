import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/insights/daily_readiness.dart';

part 'daily_readiness_display.freezed.dart';

/// How many "Why" factors the panel lists (Kotlin `factors.take(5)`).
const int _maxPanelFactors = 5;

/// The screen-ready derivation of one [DailyReadinessInsight]: every line the
/// readiness panel used to compose in its `build` — the confidence sentence, the
/// stress and strain values, the HRV/intensity summaries, and the capped factor
/// list.
///
/// Built once per load by [buildDailyReadinessDisplay] and stored on the state —
/// the view-model precomputes, the widgets only render.
@freezed
abstract class DailyReadinessDisplay with _$DailyReadinessDisplay {
  const factory DailyReadinessDisplay({
    required String confidenceText,
    required String hrvStatusValue,
    required String intensityMinutesValue,
    required String stressValue,
    required String strainValue,
    required List<DailyReadinessFactor> topFactors,
  }) = _DailyReadinessDisplay;
}

/// Pure derivation from the insight to its display model. No clock, no ref, no
/// I/O — unit-testable with a fixture insight.
DailyReadinessDisplay buildDailyReadinessDisplay(
  DailyReadinessInsight insight,
) =>
    DailyReadinessDisplay(
      confidenceText: _confidenceText(insight),
      hrvStatusValue:
          '${insight.hrvStatus.label} · ${insight.hrvStatus.detail}',
      intensityMinutesValue: '${insight.intensityMinutes.label} · '
          '${insight.intensityMinutes.detail}',
      stressValue: _stressValue(insight),
      strainValue: [
        insight.strainTarget,
        if (insight.currentStrain != null) insight.currentStrain!,
      ].join(' · '),
      topFactors: insight.factors.take(_maxPanelFactors).toList(),
    );

String _stressValue(DailyReadinessInsight insight) {
  final stress = insight.physiologicalStress;
  final score = stress.score != null ? ' · ${stress.score}/100' : '';
  return '${stress.label}$score · ${stress.summary}';
}

String _confidenceText(DailyReadinessInsight insight) {
  final String label;
  switch (insight.confidence) {
    case ReadinessConfidence.high:
      label = 'High confidence';
    case ReadinessConfidence.medium:
      label = 'Medium confidence';
    case ReadinessConfidence.low:
      label = 'Low confidence';
  }
  final String reason;
  switch (insight.confidenceReason) {
    case 'complete_data':
      reason = 'complete local data';
    case 'missing_sleep_data':
      reason = 'sleep data missing';
    case 'missing_hrv_data':
      reason = 'HRV data missing';
    case 'new_user_not_enough_baseline':
      reason = 'baseline still building';
    default:
      reason = 'partial local data';
  }
  return '$label · $reason';
}
