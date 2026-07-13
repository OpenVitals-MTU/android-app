import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Everything a chart needs to know about how it should look — in one place, so
/// that changing how the app's charts look is one edit and not seventeen.
///
/// There was no such place. `AppColors` holds thirty-three colours and not one of
/// them is a chart colour: no grid, no axis, no track. So every painter decided
/// for itself, and they disagreed — a grid line is the accent at 12% in one chart
/// and `outlineVariant` at 50% in another; a bar's track is
/// `surfaceContainerHighest` here and whatever `LinearProgressIndicator` felt like
/// there. The heights are worse: 232, 180, 172, 72, all typed straight into the
/// widget that used them.
///
/// A [ThemeExtension] rather than a file of constants, because the colours have to
/// derive from the LIVE [ColorScheme]: the app supports dynamic colour and an
/// AMOLED variant, so a hard-coded grey is wrong on two of the three themes it can
/// be looked at in.
@immutable
class ChartTokens extends ThemeExtension<ChartTokens> {
  const ChartTokens({
    required this.axis,
    required this.track,
    required this.emptyTrack,
    required this.crosshair,
    required this.tooltipSurface,
    required this.onTooltipSurface,
  });

  /// Derived from the scheme the app is actually being shown in.
  factory ChartTokens.of(ColorScheme scheme) => ChartTokens(
        axis: scheme.outlineVariant.withValues(alpha: 0.8),
        track: scheme.surfaceContainerHighest,
        emptyTrack: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        crosshair: scheme.onSurfaceVariant.withValues(alpha: 0.4),
        tooltipSurface: scheme.inverseSurface,
        onTooltipSurface: scheme.onInverseSurface,
      );

  static ChartTokens read(BuildContext context) =>
      Theme.of(context).extension<ChartTokens>() ??
      ChartTokens.of(Theme.of(context).colorScheme);

  /// The axis line itself.
  final Color axis;

  /// The unfilled part of any bar. One answer, for all nine bars that each had
  /// their own.
  final Color track;

  /// A heatmap cell for a day with no reading — which is not the same as a day
  /// with a reading of zero, and must not look like one.
  final Color emptyTrack;

  final Color crosshair;
  final Color tooltipSurface;
  final Color onTooltipSurface;

  /// A grid line is the SERIES colour, faint.
  ///
  /// Not a grey. Two of the three charts that draw a grid had already worked this
  /// out independently — a neutral grid under a coloured line reads as a separate
  /// object sitting behind the chart, where a tinted one reads as part of it.
  Color grid(Color accent) => accent.withValues(alpha: 0.12);

  /// The wash under a line. (Phase B replaces the flat fill with a gradient; it
  /// stays a function of the accent, so nothing else has to change.)
  Color areaFill(Color accent) => accent.withValues(alpha: 0.12);

  /// The line a chart sits ON — heavier than a grid line, lighter than the trace.
  Color baseline(Color accent) => accent.withValues(alpha: 0.22);

  @override
  ChartTokens copyWith({
    Color? axis,
    Color? track,
    Color? emptyTrack,
    Color? crosshair,
    Color? tooltipSurface,
    Color? onTooltipSurface,
  }) =>
      ChartTokens(
        axis: axis ?? this.axis,
        track: track ?? this.track,
        emptyTrack: emptyTrack ?? this.emptyTrack,
        crosshair: crosshair ?? this.crosshair,
        tooltipSurface: tooltipSurface ?? this.tooltipSurface,
        onTooltipSurface: onTooltipSurface ?? this.onTooltipSurface,
      );

  @override
  ChartTokens lerp(ChartTokens? other, double t) {
    if (other == null) return this;
    return ChartTokens(
      axis: Color.lerp(axis, other.axis, t)!,
      track: Color.lerp(track, other.track, t)!,
      emptyTrack: Color.lerp(emptyTrack, other.emptyTrack, t)!,
      crosshair: Color.lerp(crosshair, other.crosshair, t)!,
      tooltipSurface: Color.lerp(tooltipSurface, other.tooltipSurface, t)!,
      onTooltipSurface:
          Color.lerp(onTooltipSurface, other.onTooltipSurface, t)!,
    );
  }
}

// ── Layout ──────────────────────────────────────────────────────────────────
//
// The numbers themselves are unchanged: this is where they LIVE, not what they
// are. Retuning any of them is a deliberate act with a golden diff behind it, and
// that is exactly the point of naming them.

/// The gutter the y-axis labels are written in.
const double kChartYAxisWidth = 56;

/// Between that gutter and the plot.
const double kChartAxisGap = 8;

/// How far a plot with a y axis starts from the card's edge. An x-axis row that
/// ignores this describes a chart that is not there.
const double kChartPlotInset = kChartYAxisWidth + kChartAxisGap;

/// The sleep schedule chart writes its hour scale down the RIGHT-hand side, hard
/// against the bars, instead of down the left. That is deliberate and it is the
/// one thing that chart does better than the others — a bar per night with the
/// clock beside the bars, not beside a strip of whitespace. Naming the gutter is
/// how it stops being an accident.
const double kChartRightAxisWidth = 46;

const double kChartHeightDay = 180;
const double kChartHeightSession = 180;
const double kChartHeightPeriodBar = 120;
const double kChartHeightLine = 150;
const double kChartHeightSchedule = 232;
const double kChartHeightBodyEnergy = 172;
const double kChartHeightInfluenceStrip = 44;

/// One hypnogram lane: its label, and the track the stage segments ride in.
const double kSleepLaneHeight = 72;
const double kSleepLaneLabelHeight = 28;
const double kSleepLaneTrackHeight = 26;

const double kChartLineStroke = 3;
const double kChartTraceStroke = 2;
const double kChartPointRadius = 3.5;

/// A bar row's default thickness (the nine copies ran from 3 to 18).
const double kChartBarRowHeight = 6;

/// A bar is a pill until it gets fat, then it is a rounded rectangle — beyond
/// this a "fully rounded" bar just looks like a lozenge.
const double kChartBarRadiusMax = 8;

/// The corner a bar of [barWidth] gets. One rule, so a bar cannot be rounder in
/// one chart than another.
double chartBarRadius(double barWidth) =>
    math.min(barWidth / 2, kChartBarRadiusMax);
