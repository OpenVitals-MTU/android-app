@Tags(['golden'])
library;

import 'dart:math' as math;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/caffeine/application/caffeine_display.dart';
import 'package:openvitals/features/caffeine/presentation/caffeine_screen.dart';

import '../../support/golden_harness.dart';

/// [CaffeineCurveCard] — the app's third and worst line rendering.
///
/// Raw `lineTo` between points, no axis, no gridlines, a dashed threshold stepped
/// by hand in a `for` loop, and entry markers dropped along the baseline. It is
/// about to be consolidated with the other two, so this is the picture that says
/// what it looked like BEFORE — which is the only way to tell "the new renderer
/// draws it properly" apart from "the new renderer draws it differently".
///
/// Three things have to survive that: the SAWTOOTH (a dose lands instantly and
/// then decays, so the curve jumps and slides, it does not swell), the DASHED
/// threshold line, and the markers on the baseline. The fixture is built to make
/// all three unmissable.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  // Caffeine's half-life, which is what makes the curve a decay rather than a
  // line. The view-model's own model: a dose is absorbed at once and then halves
  // every five hours.
  const halfLifeHours = 5.0;

  // A day's drinking, on the golden clock. Four doses, spaced so their decays
  // overlap — a curve of one dose is just an exponential, and would photograph
  // none of the interesting behaviour.
  final doses = <(DateTime, double)>[
    (DateTime(2026, 6, 22, 7, 30), 95), // the morning coffee
    (DateTime(2026, 6, 22, 10, 15), 65), // the second one
    (DateTime(2026, 6, 22, 14, 0), 80), // the after-lunch espresso
    (DateTime(2026, 6, 22, 17, 30), 30), // a tea, late enough to matter
  ];

  /// Active mg at [time]: every dose already taken, each halved for every five
  /// hours since it landed.
  double activeMg(DateTime time) {
    var total = 0.0;
    for (final (at, mg) in doses) {
      if (time.isBefore(at)) continue;
      final hours = time.difference(at).inMinutes / 60.0;
      total += mg * math.pow(0.5, hours / halfLifeHours);
    }
    return total;
  }

  // Sampled every fifteen minutes across the whole day. The sawtooth only exists
  // because the samples are dense enough to land either side of a dose.
  final curvePoints = <CaffeinePoint>[
    for (var minute = 0; minute <= 24 * 60; minute += 15)
      () {
        final time = kGoldenDay.add(Duration(minutes: minute));
        return CaffeinePoint(time: time, valueMg: activeMg(time));
      }(),
  ];

  // The sleep threshold: 50 mg is the default, and the day deliberately crosses
  // it — a threshold line that nothing ever touches proves nothing about where
  // it was drawn.
  const thresholdMg = 50;

  final insights = CaffeineInsights(
    currentMg: activeMg(kGoldenNow),
    todayTotalMg: 270,
    sleepThresholdMg: thresholdMg,
    bedtimeMg: activeMg(DateTime(2026, 6, 22, 23, 0)),
    curvePoints: curvePoints,
  );

  // Built exactly as `_homeDisplay` builds it: the y-axis maximum is the tallest
  // of the threshold and every point, so the dashed line can never fall off the
  // top of a quiet day's chart.
  final home = CaffeineHomeDisplay(
    insights: insights,
    sleepImpactStatus: CaffeineSleepImpactStatus.mayAffectSleep,
    bedtimeIsSafe: false,
    curveEntryTimes: [for (final (at, _) in doses) at],
    curveMaxMg: curvePoints.fold<double>(
      thresholdMg.toDouble(),
      (max, point) => point.valueMg > max ? point.valueMg : max,
    ),
  );

  testWidgets('a day of drinking — sawtooth, threshold, markers',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineCurveCard(home: home, formatter: formatter),
      name: 'caffeine_curve_day',
    );
  });

  testWidgets('a single dose — one rise, one long decay', (tester) async {
    // The shape a light day actually has, and the one where the curve never
    // reaches the threshold: the dashed line sits ABOVE the whole trace, because
    // the axis maximum is the threshold rather than the peak. That branch of
    // `curveMaxMg` has no other picture.
    final singleDose = <CaffeinePoint>[
      for (var minute = 0; minute <= 24 * 60; minute += 15)
        () {
          final time = kGoldenDay.add(Duration(minutes: minute));
          final at = DateTime(2026, 6, 22, 8, 0);
          final mg = time.isBefore(at)
              ? 0.0
              : 40 *
                  math.pow(
                    0.5,
                    (time.difference(at).inMinutes / 60.0) / halfLifeHours,
                  );
          return CaffeinePoint(time: time, valueMg: mg.toDouble());
        }(),
    ];
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineCurveCard(
        home: CaffeineHomeDisplay(
          insights: CaffeineInsights(
            currentMg: 18,
            todayTotalMg: 40,
            sleepThresholdMg: thresholdMg,
            curvePoints: singleDose,
          ),
          curveEntryTimes: [DateTime(2026, 6, 22, 8, 0)],
          curveMaxMg: thresholdMg.toDouble(),
        ),
        formatter: formatter,
      ),
      name: 'caffeine_curve_single_dose',
    );
  });

  testWidgets('a day with nothing in it', (tester) async {
    // Fewer than two points and the card refuses to draw a line at all — one
    // point is not a trend, and the old painter would have divided by a zero-wide
    // time span to place it. The empty label is what it shows instead.
    await expectChartGoldenBothThemes(
      tester,
      () => CaffeineCurveCard(
        home: const CaffeineHomeDisplay(
          insights: CaffeineInsights(sleepThresholdMg: thresholdMg),
        ),
        formatter: formatter,
      ),
      name: 'caffeine_curve_empty',
    );
  });
}
