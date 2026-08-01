import '../../../core/time/local_date.dart';
import '../../../domain/insights/stress_tracking.dart';
import '../../../l10n/app_localizations.dart';

/// Renders the stress estimate's lists and detail through the catalog — the
/// counterpart of `readiness_l10n.dart` for the stress screen. The estimate's
/// English lists stay canonical; the screen renders the structured
/// [StressListItem]s, falling back to the English text when a fixture predates
/// them.

String stressLevelDetail(AppLocalizations l10n, PhysiologicalStressLevel level) =>
    switch (level) {
      PhysiologicalStressLevel.resting => l10n.stressDetailResting,
      PhysiologicalStressLevel.low => l10n.stressDetailLow,
      PhysiologicalStressLevel.medium => l10n.stressDetailMedium,
      PhysiologicalStressLevel.high => l10n.stressDetailHigh,
      PhysiologicalStressLevel.needsMoreData => l10n.stressDetailNeedsMore,
    };

/// The estimate's contributing factors, localized — or the English list when
/// the structured items are absent (a pre-template fixture).
List<String> stressFactorLines(
  AppLocalizations l10n,
  PhysiologicalStressEstimate stress,
) =>
    stress.factorItems.isEmpty && stress.contributingFactors.isNotEmpty
        ? stress.contributingFactors
        : [for (final item in stress.factorItems) _line(l10n, item)];

List<String> stressCoverageLines(
  AppLocalizations l10n,
  PhysiologicalStressEstimate stress,
) =>
    stress.coverageItems.isEmpty && stress.dataCoverage.isNotEmpty
        ? stress.dataCoverage
        : [for (final item in stress.coverageItems) _line(l10n, item)];

List<String> stressCaveatLines(
  AppLocalizations l10n,
  PhysiologicalStressEstimate stress,
) =>
    stress.caveatItems.isEmpty && stress.caveats.isNotEmpty
        ? stress.caveats
        : [for (final item in stress.caveatItems) _line(l10n, item)];

String _line(AppLocalizations l10n, StressListItem item) {
  double arg(int index) =>
      index < item.args.length ? item.args[index] : double.nan;
  int intArg(int index) {
    final value = arg(index);
    return value.isNaN ? 0 : value.round();
  }

  String tempValues() {
    final body = arg(0);
    final skin = arg(1);
    return [
      if (!body.isNaN) l10n.readinessFactorTempBody(body.toStringAsFixed(1)),
      if (!skin.isNaN)
        l10n.readinessFactorTempSkin(
          (skin > 0 ? '+' : '') + skin.toStringAsFixed(1),
        ),
    ].join(', ');
  }

  String window() {
    final start = arg(1);
    final end = arg(2);
    if (start.isNaN || end.isNaN) return l10n.stressWindowDay;
    final startText = _clock(start.round());
    final endText = _clock(end.round());
    return start == end
        ? l10n.stressWindowAt(startText)
        : l10n.stressWindowRange(startText, endText);
  }

  return switch (item.template) {
    StressItemTemplate.hrvBelowBaseline => l10n.stressFactorHrvBelow(intArg(0)),
    StressItemTemplate.hrvAboveBaseline => l10n.stressFactorHrvAbove(intArg(0)),
    StressItemTemplate.hrvNearBaseline => l10n.stressFactorHrvNear,
    StressItemTemplate.restingHrAbove =>
      l10n.stressFactorRestingHrAbove(intArg(0)),
    StressItemTemplate.restingHrBelow =>
      l10n.stressFactorRestingHrBelow(intArg(0)),
    StressItemTemplate.restingHrNear => l10n.stressFactorRestingHrNear,
    StressItemTemplate.avgHrAboveResting => l10n.stressFactorAvgHr(intArg(0)),
    StressItemTemplate.activityInfluence => l10n.stressFactorActivity,
    StressItemTemplate.sleepRaisesStrain =>
      l10n.stressFactorSleepRaises(intArg(0)),
    StressItemTemplate.sleepMixed => l10n.stressFactorSleepMixed(intArg(0)),
    StressItemTemplate.sleepSupportsLower =>
      l10n.stressFactorSleepSupports(intArg(0)),
    StressItemTemplate.sleepPlain => l10n.stressFactorSleepPlain(intArg(0)),
    StressItemTemplate.noHydrationLogged => l10n.stressFactorNoHydration,
    StressItemTemplate.hydrationSoFar =>
      l10n.stressFactorHydration(arg(0).toStringAsFixed(1)),
    StressItemTemplate.nutritionLarge => l10n.stressFactorNutritionLarge,
    StressItemTemplate.nutritionPlain => l10n.stressFactorNutritionPlain,
    StressItemTemplate.temperatureElevated =>
      l10n.stressFactorTempElevated(tempValues()),
    StressItemTemplate.temperatureSlightlyElevated =>
      l10n.stressFactorTempSlight(tempValues()),
    StressItemTemplate.temperatureNotElevated =>
      l10n.stressFactorTempNot(tempValues()),
    StressItemTemplate.loadHighPercent => l10n.stressFactorLoadHigh(intArg(0)),
    StressItemTemplate.loadNearTarget => l10n.stressFactorLoadNear,
    StressItemTemplate.mindfulnessLogged =>
      l10n.readinessFactorMindfulnessDetail(intArg(0)),
    StressItemTemplate.coverageHrSamples =>
      l10n.stressCoverageHrSamples(intArg(0), window()),
    StressItemTemplate.coverageHrAverageOnly => l10n.stressCoverageHrAvgOnly,
    StressItemTemplate.coverageHrNone => l10n.stressCoverageHrNone,
    StressItemTemplate.coverageHrvPoints =>
      l10n.stressCoverageHrvPoints(intArg(0), window()),
    StressItemTemplate.coverageHrvSinglePoint =>
      l10n.stressCoverageHrvSingle(window()),
    StressItemTemplate.coverageHrvAverageOnly => l10n.stressCoverageHrvAvgOnly,
    StressItemTemplate.coverageHrvNone => l10n.stressCoverageHrvNone,
    StressItemTemplate.caveatNotMentalStress => l10n.stressCaveatNotMental,
    StressItemTemplate.caveatNoHealthConnectScore => l10n.stressCaveatNoHcScore,
    StressItemTemplate.caveatConfounders => l10n.stressCaveatConfounders,
    StressItemTemplate.caveatAllDayModel => l10n.stressCaveatAllDayModel,
    StressItemTemplate.caveatWorkoutInfluence => l10n.stressCaveatWorkout,
    StressItemTemplate.caveatLowConfidence => l10n.stressCaveatLowConfidence,
    StressItemTemplate.caveatSparseHrv => l10n.stressCaveatSparseHrv,
  };
}

String _clock(int epochMs) {
  final local =
      instantToLocalTime(DateTime.fromMillisecondsSinceEpoch(epochMs));
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '$hour:$minute';
}
