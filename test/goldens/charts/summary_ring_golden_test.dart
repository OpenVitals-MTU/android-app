@Tags(['golden'])
library;

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/ui/components/summary_ring_card.dart';
import 'package:openvitals/ui/theme/app_colors.dart';

import '../../support/golden_harness.dart';

/// [SummaryRingCard] — the two hero gauges at the top of the dashboard.
///
/// An open-bottom arc: 280° starting at 130°, round caps, an accent fill over an
/// outline track. The two ends of its range are where it can lie. At zero the
/// fill must not be drawn at all — a round cap on a zero-length arc still paints
/// a dot, which reads as "you have started" when you have not. Over goal the
/// progress is clamped, so a 135% day and a 100% day are the same picture, and
/// the NUMBER in the middle is the only thing that can tell them apart. Both are
/// worth having on film.
void main() {
  // Half of a phone's content width, less the gap: what an [Expanded] gives each
  // ring when two share a row. The gauge scales its stroke off its own side, so
  // the width is part of the fixture, not a detail of the harness.
  const ringWidth = 174.0;

  testWidgets('nothing yet — the track, and no fill', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => const SummaryRingCard(
        title: 'Steps',
        value: '0',
        subtitle: 'of 10,000',
        accentColor: AppColors.steps,
      ),
      name: 'summary_ring_zero',
      width: ringWidth,
    );
  });

  testWidgets('part way round', (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => const SummaryRingCard(
        title: 'Steps',
        value: '4,512',
        subtitle: 'of 10,000',
        accentColor: AppColors.steps,
        progress: 0.45,
      ),
      name: 'summary_ring_partial',
      width: ringWidth,
    );
  });

  testWidgets('past the goal — the arc closes, the number keeps going',
      (tester) async {
    await expectChartGoldenBothThemes(
      tester,
      () => const SummaryRingCard(
        title: 'Weekly cardio',
        value: '203',
        subtitle: 'of 150 min',
        accentColor: AppColors.workout,
        // Clamped to 1.0 inside the card. The gauge cannot overrun its own
        // track, so 135% and 100% draw the same arc.
        progress: 1.35,
      ),
      name: 'summary_ring_over',
      width: ringWidth,
    );
  });

  testWidgets('a value long enough to fight the ring for room', (tester) async {
    // The centre text is a [FittedBox]: it shrinks rather than wraps or clips. A
    // six-figure step count on a 174px ring is the case that exercises it, and
    // the one where a regression would show as an ellipsis instead of a number.
    await expectChartGoldenBothThemes(
      tester,
      () => const SummaryRingCard(
        title: 'Steps',
        value: '128,540',
        subtitle: 'of 10,000 · goal smashed',
        accentColor: AppColors.steps,
        progress: 1.0,
      ),
      name: 'summary_ring_long_value',
      width: ringWidth,
    );
  });
}
