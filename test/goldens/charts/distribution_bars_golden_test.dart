@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/caffeine/application/caffeine_display.dart';
import 'package:openvitals/features/caffeine/presentation/caffeine_screen.dart';
import 'package:openvitals/features/hydration/application/hydration_display.dart';
import 'package:openvitals/features/hydration/presentation/hydration_screen.dart';

import '../../support/golden_harness.dart';

/// The "labelled proportional bar" rows — three of the nine copies of them.
///
/// Every one is the same idea: a label, a value on the right, and a
/// [LinearProgressIndicator] under it whose fraction is the row's share of the
/// biggest row in the same card. They were written nine times and they do not
/// quite agree — the caffeine rows scale against the tallest BAR, hydration
/// against a `maxDrinkLiters` folded by its view-model, and the two round their
/// corners differently. They are about to become one widget.
///
/// So the fixtures all lean on the same two places a bar can lie: a row at
/// essentially the FULL width (where the rounded end cap either meets the track's
/// end or overshoots it) and a row at almost nothing (where a bar can round down
/// to a bare stub, or to nothing at all, and take the row's meaning with it).
/// Those two ends are what a consolidation is most likely to quietly change.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  testWidgets('caffeine by source', (tester) async {
    // `_bars` cuts to the top six and scales each against the tallest of them, so
    // the first row is always exactly 1.0 — the full-width case is not an edge
    // case here, it is EVERY card's first row.
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineDistributionCard(
        title: 'By source',
        bars: const [
          CaffeineBar(label: 'Home espresso', valueMg: 420, fraction: 1.0),
          CaffeineBar(label: 'Office filter', valueMg: 265, fraction: 0.631),
          CaffeineBar(label: 'Corner café', valueMg: 148, fraction: 0.352),
          CaffeineBar(label: 'Green tea', valueMg: 62, fraction: 0.148),
          CaffeineBar(label: 'Cola', valueMg: 34, fraction: 0.081),
          // 2% of the tallest bar. A row that renders as an empty track here is
          // the bug; the number beside it would still be right, which is exactly
          // how the sleep stage bars shipped broken for months.
          CaffeineBar(label: 'Dark chocolate', valueMg: 9, fraction: 0.021),
        ],
        formatter: formatter,
      ),
      name: 'distribution_caffeine_sources',
    );
  });

  testWidgets('caffeine by category — long labels against the value',
      (tester) async {
    // The label is [Expanded] and the value is not, so a long name has to yield
    // rather than push the milligrams off the card.
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineDistributionCard(
        title: 'By category',
        bars: const [
          CaffeineBar(label: 'Coffee', valueMg: 685, fraction: 1.0),
          CaffeineBar(label: 'Tea', valueMg: 148, fraction: 0.216),
          CaffeineBar(
            label: 'Energy drink (imported, 500 ml can)',
            valueMg: 80,
            fraction: 0.117,
          ),
          CaffeineBar(label: 'Soda', valueMg: 34, fraction: 0.05),
        ],
        formatter: formatter,
      ),
      name: 'distribution_caffeine_categories',
    );
  });

  testWidgets('caffeine with nothing logged', (tester) async {
    // The only one of the three cards with a real empty state. The other two
    // simply render a title and a void, which is worth having on film too.
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineDistributionCard(
        title: 'By source',
        bars: const [],
        formatter: formatter,
      ),
      name: 'distribution_caffeine_empty',
    );
  });

  testWidgets('caffeine by time of day', (tester) async {
    // Four fixed buckets, always all four, so an evening of nothing is a bucket
    // at zero rather than a missing row — the shape of the day is the point.
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineTimeBucketsCard(
        analytics: const CaffeineAnalyticsDisplay(
          timeBucketBars: [
            CaffeineTimeBucketBar(
              bucket: CaffeineTimeOfDayBucket.morning,
              valueMg: 512,
              fraction: 1.0,
            ),
            CaffeineTimeBucketBar(
              bucket: CaffeineTimeOfDayBucket.afternoon,
              valueMg: 340,
              fraction: 0.664,
            ),
            CaffeineTimeBucketBar(
              bucket: CaffeineTimeOfDayBucket.evening,
              valueMg: 48,
              fraction: 0.094,
            ),
            // Nothing at all after midnight. A zero-fraction bar and a missing
            // bar are different claims, and this card makes the first one.
            CaffeineTimeBucketBar(
              bucket: CaffeineTimeOfDayBucket.night,
              valueMg: 0,
              fraction: 0.0,
            ),
          ],
        ),
        formatter: formatter,
      ),
      name: 'distribution_caffeine_buckets',
    );
  });

  testWidgets('hydration drink breakdown', (tester) async {
    // The other family: scaled against `maxDrinkLiters` rather than against the
    // top row's own fraction, so the widest bar is only full-width when the
    // biggest drink IS the maximum — which, since the max is folded from these
    // same slices, it is. Same picture, different arithmetic; that divergence is
    // half the reason these are being merged.
    await expectChartGoldenBothThemes(
      tester,
      () => HydrationDrinkBreakdownCard(
        display: const HydrationDisplay(
          hasData: true,
          topDrinkSlices: [
            HydrationDrinkSlice(label: 'Water', liters: 12.4),
            HydrationDrinkSlice(label: 'Sparkling water', liters: 4.1),
            HydrationDrinkSlice(label: 'Coffee', liters: 1.8),
            // No name of its own: another app's plain log. The card calls it
            // "Beverage" rather than naming the package it came from.
            HydrationDrinkSlice(label: null, liters: 0.9),
            HydrationDrinkSlice(label: 'Herbal tea', liters: 0.25),
          ],
          maxDrinkLiters: 12.4,
        ),
        formatter: formatter,
      ),
      name: 'distribution_hydration_drinks',
    );
  });

  testWidgets('hydration where one drink is nearly everything', (tester) async {
    // A water-only person with one stray coffee: 98% against 2%. Both end caps of
    // the bar's range in one shot.
    await expectChartGoldenBothThemes(
      tester,
      () => HydrationDrinkBreakdownCard(
        display: const HydrationDisplay(
          hasData: true,
          topDrinkSlices: [
            HydrationDrinkSlice(label: 'Water', liters: 15.0),
            HydrationDrinkSlice(label: 'Coffee', liters: 0.3),
          ],
          maxDrinkLiters: 15.0,
        ),
        formatter: formatter,
      ),
      name: 'distribution_hydration_lopsided',
    );
  });
}
