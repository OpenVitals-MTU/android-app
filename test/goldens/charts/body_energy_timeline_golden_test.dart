@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/features/bodyenergy/application/body_energy_display.dart';
import 'package:openvitals/features/bodyenergy/presentation/body_energy_timeline_chart.dart';

import '../../support/golden_harness.dart';

/// [BodyEnergyTimelineChart] — the 0-100 score line, and the charge/drain strip
/// under it.
///
/// Two painters that have to agree with each other and with the hour row beneath
/// them: the line's x, the bar's x and the axis's 12:00 all come from the same
/// fraction of the same day, and if any one of them drifts the card says the
/// workout happened at a time it did not. Neither painter draws a y-axis column,
/// which is why the hour row is at `inset: 0` — the one card in the app where
/// that is the RIGHT answer.
///
/// Everything here arrives precomputed from the view-model, so the fixture is
/// what `buildBodyEnergyDisplay` produces: xFractions across the WHOLE day
/// (86400s), one bar per five-minute bucket.
void main() {
  // The bucket the algorithm actually uses, expressed the way the display does.
  const bucketMinutes = 5;
  const bucketsPerDay = 24 * 60 ~/ bucketMinutes;
  const widthFraction = bucketMinutes / (24 * 60);

  /// What each bucket of a plausible day is doing. A night of sleep recovering
  /// the score, a quiet breakfast, an hour the watch was off the wrist, a hard
  /// hour on the bike, then a slow afternoon drain.
  (double charge, double drain, BodyEnergyPrimaryInfluence influence) bucket(
    int index,
  ) {
    if (index < 84) {
      return (0.4, 0.0, BodyEnergyPrimaryInfluence.sleepRecovery); // to 07:00
    }
    if (index < 96) {
      return (0.08, 0.0, BodyEnergyPrimaryInfluence.quietRest); // to 08:00
    }
    if (index < 108) {
      // Watch off the wrist. NO_DATA with nothing to draw is not a blank gap —
      // it is a low-emphasis tick spanning the strip, which is the only way the
      // card can say "I do not know" instead of "nothing happened".
      return (0.0, 0.0, BodyEnergyPrimaryInfluence.noData); // to 09:00
    }
    if (index < 120) {
      return (0.0, 1.6, BodyEnergyPrimaryInfluence.exertion); // to 10:00
    }
    if (index < 150) {
      return (0.0, 0.12, BodyEnergyPrimaryInfluence.steady); // to 12:30
    }
    return (0.0, 0.3, BodyEnergyPrimaryInfluence.elevatedHeartRate);
  }

  /// The day up to [buckets], accumulating the score the way the timeline does.
  (List<BodyEnergyChartPoint>, List<BodyEnergyInfluenceBar>) day(int buckets) {
    final points = <BodyEnergyChartPoint>[];
    final bars = <BodyEnergyInfluenceBar>[];
    var score = 62.0;
    for (var index = 0; index < buckets; index++) {
      final (charge, drain, influence) = bucket(index);
      score = (score + charge - drain).clamp(0.0, 100.0);
      final xFraction = index / bucketsPerDay;
      points.add(BodyEnergyChartPoint(xFraction, score.roundToDouble()));
      bars.add(
        BodyEnergyInfluenceBar(
          xFraction: xFraction,
          widthFraction: widthFraction,
          charge: charge,
          drain: drain,
          influence: influence,
        ),
      );
    }
    return (points, bars);
  }

  testWidgets('a day up to the golden clock', (tester) async {
    // 14:30, so the line stops just past halfway and the rest of the day is empty
    // — the same honesty the day charts owe: a line held out to the right edge
    // would be a claim about hours that have not happened.
    final (points, bars) = day(174);
    await expectChartGoldenBothThemes(
      tester,
      () => BodyEnergyTimelineChart(
        points: points,
        influenceBars: bars,
        // The tallest bar the strip must fit, folded by the view-model rather
        // than rescanned on every repaint. It is the workout hour.
        maxMagnitude: 1.6,
      ),
      name: 'body_energy_timeline_day',
    );
  });

  testWidgets('early morning — few enough points that the line grows dots',
      (tester) async {
    // 36 buckets (three hours in). Past forty the dots are dropped, so this is
    // the only shape in which the point markers are ever drawn at all.
    final (points, bars) = day(36);
    await expectChartGoldenBothThemes(
      tester,
      () => BodyEnergyTimelineChart(
        points: points,
        influenceBars: bars,
        // Floored at 1.0 by the view-model, so an all-quiet night still divides
        // by something.
        maxMagnitude: 1.0,
      ),
      name: 'body_energy_timeline_morning',
    );
  });
}
