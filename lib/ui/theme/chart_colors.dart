import 'package:flutter/material.dart';

import '../../domain/insights/body_energy_timeline.dart';
import '../../domain/model/sleep_models.dart';
import 'app_colors.dart';

/// The colours a chart draws its DATA in — as opposed to [ChartTokens], which is
/// the colours it draws its furniture in (grid, axis, track).
///
/// These two palettes were living in feature files: the sleep stage colours
/// beside the hypnogram, the body-energy influence colours beside its painter.
/// That is where a colour goes to be forgotten. `sleep_cards.dart`,
/// `sleep_schedule_chart.dart` and `sleep_detail_screen.dart` all had to reach
/// into a chart file to find out what "deep sleep" looks like, and nothing in the
/// theme knew these colours existed at all — so the app's palette could be
/// restyled without them, and they would simply have stayed 2019.
///
/// A colour that names a piece of DATA belongs with the other colours that name
/// data. That is here.

/// The sleep-stage accents, ported from the Kotlin `stageColor(...)`.
///
/// Fixed hues rather than scheme-derived ones, deliberately: a hypnogram is read
/// by comparing its bands to each other, and five stages need five colours that
/// stay distinguishable from one another in both themes. A scheme's tonal palette
/// cannot promise that.
Color sleepStageColor(int stageType) {
  switch (stageType) {
    case SleepStage.stageAwake:
      return const Color(0xFFF48FB1);
    case SleepStage.stageLight:
      return const Color(0xFF8AB4F8);
    case SleepStage.stageDeep:
      return const Color(0xFF8E63CE);
    case SleepStage.stageRem:
      return const Color(0xFFB3E5FC);
    case SleepStage.stageAwakeInBed:
      return const Color(0xFFF8A6C6);
    case SleepStage.stageSleeping:
      return const Color(0xFF7EA7F5);
    case SleepStage.stageOutOfBed:
      return const Color(0xFFEF9A9A);
    default:
      return const Color(0xFF90A4AE);
  }
}

/// What moved the body-energy score, in colour.
///
/// Two of these are scheme-derived, and correctly so: "no data" and "steady" are
/// the ABSENCE of an influence, and an absence should read as furniture, not as
/// data.
Color influenceColor(
  BodyEnergyPrimaryInfluence influence,
  ColorScheme scheme,
) {
  switch (influence) {
    case BodyEnergyPrimaryInfluence.sleepRecovery:
      return AppColors.steps;
    case BodyEnergyPrimaryInfluence.quietRest:
      return AppColors.workout;
    case BodyEnergyPrimaryInfluence.everydayActivity:
      return AppColors.distance;
    case BodyEnergyPrimaryInfluence.exertion:
      return AppColors.calories;
    case BodyEnergyPrimaryInfluence.elevatedHeartRate:
      return AppColors.floors;
    case BodyEnergyPrimaryInfluence.recoveryDebt:
      return AppColors.heart;
    case BodyEnergyPrimaryInfluence.noData:
      return scheme.outline;
    case BodyEnergyPrimaryInfluence.steady:
      return scheme.onSurfaceVariant;
  }
}
