import '../../../domain/insights/daily_readiness.dart';
import '../../../domain/insights/intensity_minutes.dart';
import '../../../domain/insights/stress_tracking.dart';
import '../../../l10n/app_localizations.dart';

/// Renders the readiness domain's verdict through the catalog.
///
/// The domain keeps composing its English sentences — they are the canonical
/// wording for tests and non-UI callers, the same contract as
/// `SettingsSection`'s const titles — and everything the SCREEN shows comes
/// from here instead: the enums and numeric args the insight carries, mapped
/// key by key, so a Spanish device reads a Spanish verdict.
///
/// A factor whose template is [ReadinessFactorTemplate.legacy] (a fixture
/// built before templates existed) falls back to its English label/detail
/// rather than guessing.

String readinessStatusTitle(AppLocalizations l10n, ReadinessState state) =>
    switch (state) {
      ReadinessState.ready => l10n.readinessStatusReady,
      ReadinessState.moderate => l10n.readinessStatusModerate,
      ReadinessState.recover => l10n.readinessStatusRecover,
      ReadinessState.rest => l10n.readinessStatusRest,
      ReadinessState.unknown => l10n.readinessStatusUnknown,
    };

String readinessRecommendation(
  AppLocalizations l10n,
  ReadinessState state,
  ReadinessRecommendationType type,
) =>
    switch (type) {
      ReadinessRecommendationType.hardTraining =>
        l10n.readinessRecommendHardTraining,
      ReadinessRecommendationType.moderateTraining =>
        l10n.readinessRecommendModerateTraining,
      ReadinessRecommendationType.lightActivity =>
        l10n.readinessRecommendLightActivity,
      ReadinessRecommendationType.rest => l10n.readinessRecommendRest,
      ReadinessRecommendationType.checkSymptoms =>
        l10n.readinessRecommendCheckSymptoms,
      ReadinessRecommendationType.mobility => state == ReadinessState.unknown
          ? l10n.readinessRecommendConnectData
          : l10n.readinessRecommendMobility,
    };

String readinessAlternative(AppLocalizations l10n, ReadinessState state) =>
    switch (state) {
      ReadinessState.ready => l10n.readinessAlternativeReady,
      ReadinessState.moderate => l10n.readinessAlternativeModerate,
      ReadinessState.recover => l10n.readinessAlternativeRecover,
      ReadinessState.rest => l10n.readinessAlternativeRest,
      ReadinessState.unknown => l10n.readinessAlternativeUnknown,
    };

String readinessSuggestedWorkout(AppLocalizations l10n, ReadinessState state) =>
    switch (state) {
      ReadinessState.ready => l10n.readinessWorkoutReady,
      ReadinessState.moderate => l10n.readinessWorkoutModerate,
      ReadinessState.recover => l10n.readinessWorkoutRecover,
      ReadinessState.rest => l10n.readinessWorkoutRest,
      ReadinessState.unknown => l10n.readinessWorkoutUnknown,
    };

String readinessAvoid(AppLocalizations l10n, ReadinessState state) =>
    switch (state) {
      ReadinessState.ready => l10n.readinessAvoidReady,
      ReadinessState.moderate => l10n.readinessAvoidModerate,
      ReadinessState.recover => l10n.readinessAvoidRecover,
      ReadinessState.rest => l10n.readinessAvoidRest,
      ReadinessState.unknown => l10n.readinessAvoidUnknown,
    };

String readinessStrainValue(
  AppLocalizations l10n,
  DailyReadinessInsight insight,
) {
  final range = switch (insight.state) {
    ReadinessState.ready => '10-14',
    ReadinessState.moderate => '7-10',
    ReadinessState.recover => '3-6',
    ReadinessState.rest => '0-3',
    ReadinessState.unknown => '3-6',
  };
  final target = l10n.readinessStrainTarget(range);
  final current = insight.currentStrainValue;
  if (current == null) return target;
  return '$target · '
      '${l10n.readinessCurrentStrain(current.toStringAsFixed(1))}';
}

String readinessAdaptiveGoal(
  AppLocalizations l10n,
  DailyReadinessInsight insight,
) =>
    switch (insight.state) {
      ReadinessState.ready =>
        l10n.readinessGoalReady(insight.adaptiveStepsTarget ?? 0),
      ReadinessState.moderate => l10n.readinessGoalModerate(
          insight.adaptiveStepsTarget ?? 0,
          insight.adaptiveActiveMinutesTarget ?? 0,
        ),
      ReadinessState.recover =>
        l10n.readinessGoalRecover(insight.adaptiveStepsTarget ?? 0),
      ReadinessState.rest => l10n.readinessGoalRest,
      ReadinessState.unknown => l10n.readinessGoalUnknown,
    };

String readinessConfidenceText(
  AppLocalizations l10n,
  DailyReadinessInsight insight,
) {
  final label = switch (insight.confidence) {
    ReadinessConfidence.high => l10n.cardioLoadConfidenceHigh,
    ReadinessConfidence.medium => l10n.cardioLoadConfidenceMedium,
    ReadinessConfidence.low => l10n.cardioLoadConfidenceLow,
  };
  final reason = switch (insight.confidenceReason) {
    'complete_data' => l10n.readinessConfidenceReasonComplete,
    'missing_sleep_data' => l10n.readinessConfidenceReasonMissingSleep,
    'missing_hrv_data' => l10n.readinessConfidenceReasonMissingHrv,
    'new_user_not_enough_baseline' =>
      l10n.readinessConfidenceReasonBaseline,
    _ => l10n.readinessConfidenceReasonPartial,
  };
  return '$label · $reason';
}

String hrvStatusLabel(AppLocalizations l10n, HrvStatus status) =>
    switch (status) {
      HrvStatus.balanced => l10n.hrvStatusBalanced,
      HrvStatus.low => l10n.hrvStatusLow,
      HrvStatus.high => l10n.hrvStatusHigh,
      HrvStatus.unusuallyLow => l10n.hrvStatusUnusuallyLow,
      HrvStatus.unusuallyHigh => l10n.hrvStatusUnusuallyHigh,
      HrvStatus.needsMoreHrv => l10n.hrvStatusNeedsMore,
    };

String hrvStatusDetail(AppLocalizations l10n, HrvStatusInsight hrv) {
  final percent = hrv.percentFromBaseline;
  if (hrv.status == HrvStatus.needsMoreHrv || percent == null) {
    return l10n.hrvDetailNeedsMore;
  }
  final comparison = percent == 0
      ? l10n.hrvComparisonNear
      : percent > 0
          ? l10n.hrvComparisonAbove(percent)
          : l10n.hrvComparisonBelow(percent.abs());
  return switch (hrv.status) {
    HrvStatus.balanced => l10n.hrvDetailBalanced(comparison),
    HrvStatus.low => l10n.hrvDetailLow(comparison),
    HrvStatus.high => l10n.hrvDetailHigh(comparison),
    HrvStatus.unusuallyLow ||
    HrvStatus.unusuallyHigh =>
      l10n.hrvDetailUnusual(comparison),
    HrvStatus.needsMoreHrv => l10n.hrvDetailNeedsMore,
  };
}

String intensityLabel(AppLocalizations l10n, IntensityMinutesStatus status) =>
    switch (status) {
      IntensityMinutesStatus.goalMet => l10n.intensityLabelGoalMet,
      IntensityMinutesStatus.onTrack => l10n.intensityLabelOnTrack,
      IntensityMinutesStatus.behind => l10n.intensityLabelBehind,
      IntensityMinutesStatus.low => l10n.intensityLabelLow,
      IntensityMinutesStatus.needsMoreData => l10n.intensityLabelNeedsMore,
    };

String intensityDetail(
  AppLocalizations l10n,
  IntensityMinutesReadinessInsight im,
) {
  if (im.status == IntensityMinutesStatus.needsMoreData) {
    return im.moderateEquivalentMinutes == null
        ? l10n.intensityDetailNeedsSources
        : l10n.intensityDetailNeedsMore;
  }
  final minutes = im.moderateEquivalentMinutes ?? 0;
  final target = im.targetMinutes;
  final body = switch (im.status) {
    IntensityMinutesStatus.goalMet => l10n.intensityDetailGoalMet(
          minutes,
          target,
        ) +
        ((im.todayModerateEquivalentMinutes ?? 0) > 0
            ? ' ${l10n.intensityDetailTodayAdded(im.todayModerateEquivalentMinutes!)}'
            : ''),
    IntensityMinutesStatus.onTrack =>
      l10n.intensityDetailOnTrack(minutes, target, im.daysElapsed ?? 0),
    IntensityMinutesStatus.behind => l10n.intensityDetailBehind(
        minutes,
        target,
        im.expectedByNowMinutes ?? 0,
      ),
    IntensityMinutesStatus.low => l10n.intensityDetailLow(minutes, target),
    IntensityMinutesStatus.needsMoreData => l10n.intensityDetailNeedsMore,
  };
  final confidence = switch (im.confidence) {
    IntensityMinutesConfidence.high => l10n.intensityConfidenceHigh,
    IntensityMinutesConfidence.medium => l10n.intensityConfidenceMedium,
    IntensityMinutesConfidence.low => l10n.intensityConfidenceLow,
    IntensityMinutesConfidence.noData => l10n.intensityConfidenceNoData,
  };
  return '$body $confidence.';
}

String stressLevelLabel(AppLocalizations l10n, PhysiologicalStressLevel level) =>
    switch (level) {
      PhysiologicalStressLevel.resting => l10n.stressLabelResting,
      PhysiologicalStressLevel.low => l10n.stressLabelLow,
      PhysiologicalStressLevel.medium => l10n.stressLabelMedium,
      PhysiologicalStressLevel.high => l10n.stressLabelHigh,
      PhysiologicalStressLevel.needsMoreData => l10n.stressLabelNeedsMore,
    };

String stressLevelSummary(
  AppLocalizations l10n,
  PhysiologicalStressLevel level,
) =>
    switch (level) {
      PhysiologicalStressLevel.resting => l10n.stressSummaryResting,
      PhysiologicalStressLevel.low => l10n.stressSummaryLow,
      PhysiologicalStressLevel.medium => l10n.stressSummaryMedium,
      PhysiologicalStressLevel.high => l10n.stressSummaryHigh,
      PhysiologicalStressLevel.needsMoreData => l10n.stressSummaryNeedsMore,
    };

/// One factor row, localized: the label and detail the "Why" list shows.
({String label, String detail}) localizeReadinessFactor(
  AppLocalizations l10n,
  DailyReadinessInsight insight,
  DailyReadinessFactor factor,
) {
  double arg(int index) =>
      index < factor.args.length ? factor.args[index] : double.nan;
  int intArg(int index) {
    final value = arg(index);
    return value.isNaN ? 0 : value.round();
  }

  String sleepDetail() {
    final minutes = intArg(1);
    return minutes > 0
        ? l10n.readinessFactorSleepDetail(intArg(0), _formatDuration(minutes))
        : l10n.readinessFactorSleepDetailNoDuration(intArg(0));
  }

  switch (factor.template) {
    case ReadinessFactorTemplate.legacy:
      return (label: factor.label, detail: factor.detail);
    case ReadinessFactorTemplate.sleepHelpedRecovery:
      return (label: l10n.readinessFactorSleepHelpedLabel, detail: sleepDetail());
    case ReadinessFactorTemplate.sleepUsable:
      return (label: l10n.readinessFactorSleepUsableLabel, detail: sleepDetail());
    case ReadinessFactorTemplate.sleepMayLimit:
      return (
        label: l10n.readinessFactorSleepMayLimitLabel,
        detail: sleepDetail()
      );
    case ReadinessFactorTemplate.sleepLow:
      return (label: l10n.readinessFactorSleepLowLabel, detail: sleepDetail());
    case ReadinessFactorTemplate.sleepDataMissing:
      return (
        label: l10n.readinessFactorSleepMissingLabel,
        detail: l10n.readinessFactorSleepMissingDetail
      );
    case ReadinessFactorTemplate.hrvStatusFactor:
      return (
        label: l10n
            .readinessFactorHrvLabel(hrvStatusLabel(l10n, insight.hrvStatus.status)),
        detail: hrvStatusDetail(l10n, insight.hrvStatus)
      );
    case ReadinessFactorTemplate.hrvBaselineBuilding:
      return (
        label: l10n.readinessFactorHrvBaselineLabel,
        detail: l10n.readinessFactorHrvBaselineDetail
      );
    case ReadinessFactorTemplate.hrvDataMissing:
      return (
        label: l10n.readinessFactorHrvMissingLabel,
        detail: l10n.readinessFactorHrvMissingDetail
      );
    case ReadinessFactorTemplate.restingHrElevated:
    case ReadinessFactorTemplate.restingHrSlightlyElevated:
    case ReadinessFactorTemplate.restingHrNormal:
      final delta = intArg(0);
      final detail = delta > 0
          ? l10n.readinessFactorRestingHrDetailAbove(delta)
          : delta < 0
              ? l10n.readinessFactorRestingHrDetailBelow(delta.abs())
              : l10n.readinessFactorRestingHrDetailNear;
      final label = switch (factor.template) {
        ReadinessFactorTemplate.restingHrElevated =>
          l10n.readinessFactorRestingHrElevatedLabel,
        ReadinessFactorTemplate.restingHrSlightlyElevated =>
          l10n.readinessFactorRestingHrSlightlyElevatedLabel,
        _ => l10n.readinessFactorRestingHrNormalLabel,
      };
      return (label: label, detail: detail);
    case ReadinessFactorTemplate.restingHrBaselineBuilding:
      return (
        label: l10n.readinessFactorRestingHrBaselineLabel,
        detail: l10n.readinessFactorRestingHrBaselineDetail
      );
    case ReadinessFactorTemplate.trainingLoadHigh:
    case ReadinessFactorTemplate.trainingLoadStable:
    case ReadinessFactorTemplate.trainingLoadLight:
      final label = switch (factor.template) {
        ReadinessFactorTemplate.trainingLoadHigh =>
          l10n.readinessFactorLoadHighLabel,
        ReadinessFactorTemplate.trainingLoadStable =>
          l10n.readinessFactorLoadStableLabel,
        _ => l10n.readinessFactorLoadLightLabel,
      };
      return (
        label: label,
        detail: l10n.readinessFactorLoadDetail(intArg(0))
      );
    case ReadinessFactorTemplate.intensityFactor:
      final label = switch (insight.intensityMinutes.status) {
        IntensityMinutesStatus.goalMet =>
          l10n.readinessFactorIntensityGoalMetLabel,
        IntensityMinutesStatus.onTrack =>
          l10n.readinessFactorIntensityOnTrackLabel,
        IntensityMinutesStatus.behind =>
          l10n.readinessFactorIntensityBehindLabel,
        IntensityMinutesStatus.low => l10n.readinessFactorIntensityLowLabel,
        IntensityMinutesStatus.needsMoreData =>
          l10n.readinessFactorIntensityNeedsMoreLabel,
      };
      return (
        label: label,
        detail: intensityDetail(l10n, insight.intensityMinutes)
      );
    case ReadinessFactorTemplate.physiologicalStressFactor:
      return (
        label: l10n.readinessFactorStressLabel(
          stressLevelLabel(l10n, insight.physiologicalStress.level),
        ),
        detail:
            stressLevelSummary(l10n, insight.physiologicalStress.level)
      );
    case ReadinessFactorTemplate.physiologicalStressNeedsData:
      return (
        label: l10n.readinessFactorStressNeedsDataLabel,
        detail:
            stressLevelSummary(l10n, insight.physiologicalStress.level)
      );
    case ReadinessFactorTemplate.temperatureElevated:
      final body = arg(0);
      final skin = arg(1);
      final values = [
        if (!body.isNaN) l10n.readinessFactorTempBody(body.toStringAsFixed(1)),
        if (!skin.isNaN)
          l10n.readinessFactorTempSkin(
            (skin > 0 ? '+' : '') + skin.toStringAsFixed(1),
          ),
      ].join(', ');
      return (
        label: l10n.readinessFactorTempLabel,
        detail: l10n.readinessFactorTempDetail(values)
      );
    case ReadinessFactorTemplate.hydrationBehind:
      return (
        label: l10n.readinessFactorHydrationLabel,
        detail: l10n.readinessFactorHydrationDetail(intArg(0))
      );
    case ReadinessFactorTemplate.nutritionLogged:
      return (
        label: l10n.readinessFactorNutritionLabel,
        detail: l10n.readinessFactorNutritionDetail
      );
    case ReadinessFactorTemplate.mindfulnessMoment:
      return (
        label: l10n.readinessFactorMindfulnessLabel,
        detail: l10n.readinessFactorMindfulnessDetail(intArg(0))
      );
    case ReadinessFactorTemplate.bodyEnergyDrained:
    case ReadinessFactorTemplate.bodyEnergyLow:
    case ReadinessFactorTemplate.bodyEnergyCharged:
      final label = switch (factor.template) {
        ReadinessFactorTemplate.bodyEnergyDrained =>
          l10n.readinessFactorBodyEnergyDrainedLabel,
        ReadinessFactorTemplate.bodyEnergyLow =>
          l10n.readinessFactorBodyEnergyLowLabel,
        _ => l10n.readinessFactorBodyEnergyChargedLabel,
      };
      return (
        label: label,
        detail: l10n.readinessFactorBodyEnergyDetail(intArg(0), intArg(1))
      );
    case ReadinessFactorTemplate.bodySignalsElevated:
      return (
        label: l10n.readinessFactorSignalsElevatedLabel,
        detail: l10n.readinessFactorSignalsElevatedDetail
      );
  }
}

/// The "mainly because ..." sentence, from the top factors' localized details.
String readinessExplanation(
  AppLocalizations l10n,
  DailyReadinessInsight insight,
) {
  const excluded = {
    ReadinessFactorKind.missingSleepData,
    ReadinessFactorKind.missingHrvData,
    ReadinessFactorKind.missingIntensityMinutes,
    ReadinessFactorKind.missingStressData,
    ReadinessFactorKind.newUserNotEnoughBaseline,
  };
  final filtered =
      insight.factors.where((f) => !excluded.contains(f.kind)).toList();
  final indexed = [for (var i = 0; i < filtered.length; i++) (i, filtered[i])];
  indexed.sort((a, b) {
    final byPriority = b.$2.impact.priority.compareTo(a.$2.impact.priority);
    if (byPriority != 0) return byPriority;
    return a.$1.compareTo(b.$1);
  });
  final meaningful = indexed.map((e) => e.$2).take(3).toList();
  if (meaningful.isEmpty) {
    return insight.state == ReadinessState.unknown
        ? l10n.readinessExplanationNoData
        : l10n.readinessExplanationGeneric;
  }
  final clauses = [
    for (final factor in meaningful)
      _toClause(localizeReadinessFactor(l10n, insight, factor).detail),
  ];
  final String joined;
  switch (clauses.length) {
    case 1:
      joined = clauses.single;
    case 2:
      joined = '${clauses[0]} ${l10n.joinAnd} ${clauses[1]}';
    default:
      joined = '${clauses.sublist(0, clauses.length - 1).join(', ')}, '
          '${l10n.joinAnd} ${clauses.last}';
  }
  return l10n.readinessExplanationMainly(joined);
}

String _toClause(String value) {
  final lowered =
      value.isEmpty ? value : value[0].toLowerCase() + value.substring(1);
  final trimmed = lowered.replaceFirst(RegExp(r'\s+$'), '');
  return trimmed.replaceFirst(RegExp(r'\.+$'), '');
}

String _formatDuration(int totalMinutes) {
  final h = totalMinutes ~/ 60;
  final m = totalMinutes % 60;
  return '${h}h ${m.toString().padLeft(2, '0')}m';
}
