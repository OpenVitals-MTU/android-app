import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/model/caffeine_models.dart';

part 'caffeine_display.freezed.dart';

/// The screen-ready derivation of one caffeine load: the home (today) card's
/// sleep-impact verdict and curve geometry, and the analytics window's ranked
/// distribution bars.
///
/// Built once per load by [buildCaffeineDisplay] and stored on the state — the
/// view-model precomputes, the screen only renders. The cards used to take the
/// top six slices, scan them for their tallest bar and re-derive the sleep
/// verdict on every rebuild, and the curve painter re-scanned its points for the
/// axis maximum on every repaint.
@freezed
abstract class CaffeineDisplay with _$CaffeineDisplay {
  const factory CaffeineDisplay({
    @Default(CaffeineHomeDisplay()) CaffeineHomeDisplay home,
    @Default(CaffeineAnalyticsDisplay()) CaffeineAnalyticsDisplay analytics,
  }) = _CaffeineDisplay;
}

/// How the day's active caffeine stands against the sleep threshold (Kotlin
/// `sleepImpactStatus`). The banner's colour, icon and copy are the view's.
enum CaffeineSleepImpactStatus { unlikely, elevatedNow, mayAffectSleep }

/// The today card: the verdict, and the curve the painter draws.
@freezed
abstract class CaffeineHomeDisplay with _$CaffeineHomeDisplay {
  const factory CaffeineHomeDisplay({
    @Default(CaffeineInsights()) CaffeineInsights insights,
    @Default(CaffeineSleepImpactStatus.unlikely)
    CaffeineSleepImpactStatus sleepImpactStatus,

    /// Kotlin's bedtime card colours on this: is the projection under the line?
    @Default(true) bool bedtimeIsSafe,

    /// When each logged drink lands on the curve, for the entry markers.
    @Default(<DateTime>[]) List<DateTime> curveEntryTimes,

    /// The curve's y-axis maximum: the tallest of the threshold and the points,
    /// floored at 1.0 so an empty day divides by something.
    @Default(1.0) double curveMaxMg,
  }) = _CaffeineHomeDisplay;
}

/// The analytics window: the ranked distribution bars, already cut to the six
/// the cards have room for and scaled against their own tallest.
@freezed
abstract class CaffeineAnalyticsDisplay with _$CaffeineAnalyticsDisplay {
  const factory CaffeineAnalyticsDisplay({
    @Default(CaffeineInsights()) CaffeineInsights insights,
    @Default(<CaffeineBar>[]) List<CaffeineBar> sourceBars,
    @Default(<CaffeineBar>[]) List<CaffeineBar> itemBars,
    @Default(<CaffeineBar>[]) List<CaffeineBar> categoryBars,
    @Default(<CaffeineTimeBucketBar>[])
    List<CaffeineTimeBucketBar> timeBucketBars,

    /// The biggest source over the window, or null when nothing was logged.
    String? topSourceLabel,
  }) = _CaffeineAnalyticsDisplay;
}

/// One distribution row: its label, its total, and its share of the tallest bar
/// in the same card.
@freezed
abstract class CaffeineBar with _$CaffeineBar {
  const factory CaffeineBar({
    required String label,
    required double valueMg,
    required double fraction,
  }) = _CaffeineBar;
}

/// A time-of-day row. The bucket keeps its enum — the label is l10n's problem.
@freezed
abstract class CaffeineTimeBucketBar with _$CaffeineTimeBucketBar {
  const factory CaffeineTimeBucketBar({
    required CaffeineTimeOfDayBucket bucket,
    required double valueMg,
    required double fraction,
  }) = _CaffeineTimeBucketBar;
}

/// Pure derivation from the two computed insight windows to the display model.
/// No clock, no I/O — unit-testable with fixture insights.
CaffeineDisplay buildCaffeineDisplay({
  required CaffeineInsights home,
  required CaffeineInsights analytics,
}) =>
    CaffeineDisplay(
      home: _homeDisplay(home),
      analytics: _analyticsDisplay(analytics),
    );

CaffeineHomeDisplay _homeDisplay(CaffeineInsights insights) {
  final threshold = insights.sleepThresholdMg.toDouble();
  final status = insights.bedtimeMg > threshold
      ? CaffeineSleepImpactStatus.mayAffectSleep
      : insights.currentMg > threshold
          ? CaffeineSleepImpactStatus.elevatedNow
          : CaffeineSleepImpactStatus.unlikely;

  // The painter's own scale: the threshold line must always fit, and so must
  // every point on the curve.
  var maxValue = threshold < 1.0 ? 1.0 : threshold;
  for (final point in insights.curvePoints) {
    if (point.valueMg > maxValue) maxValue = point.valueMg;
  }

  return CaffeineHomeDisplay(
    insights: insights,
    sleepImpactStatus: status,
    bedtimeIsSafe: insights.bedtimeMg <= insights.sleepThresholdMg,
    curveEntryTimes: [
      for (final insight in insights.entryInsights) insight.entry.startTime,
    ],
    curveMaxMg: maxValue,
  );
}

CaffeineAnalyticsDisplay _analyticsDisplay(CaffeineInsights insights) {
  var bucketMax = 1.0;
  for (final bucket in insights.timeBuckets) {
    if (bucket.valueMg > bucketMax) bucketMax = bucket.valueMg;
  }

  return CaffeineAnalyticsDisplay(
    insights: insights,
    sourceBars: _bars(insights.sourceTotals),
    itemBars: _bars(insights.itemTotals),
    categoryBars: _bars(insights.categoryTotals),
    timeBucketBars: [
      for (final bucket in insights.timeBuckets)
        CaffeineTimeBucketBar(
          bucket: bucket.bucket,
          valueMg: bucket.valueMg,
          fraction: bucket.valueMg / bucketMax,
        ),
    ],
    topSourceLabel: insights.sourceTotals.isNotEmpty
        ? insights.sourceTotals.first.label
        : null,
  );
}

/// The six biggest slices a distribution card shows, each as a share of the
/// tallest bar among them (the scale starts at 1 mg, as the card's did).
List<CaffeineBar> _bars(List<CaffeineDistributionSlice> slices) {
  final visible = slices.take(6).toList();
  var max = 1.0;
  for (final slice in visible) {
    if (slice.valueMg > max) max = slice.valueMg;
  }
  return [
    for (final slice in visible)
      CaffeineBar(
        label: slice.label,
        valueMg: slice.valueMg,
        fraction: slice.valueMg / max,
      ),
  ];
}
