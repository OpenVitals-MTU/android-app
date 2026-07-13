@Tags(['golden'])
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';
import 'package:openvitals/features/sleep/presentation/sleep_cards.dart';

import '../../support/golden_harness.dart';

/// [SleepStageShareCard] — the proportional stage-share bars.
///
/// These shipped as empty grey tracks: the coloured fill was a non-positioned
/// child of a [Stack], took loose constraints, and a childless box under loose
/// constraints is zero pixels tall. It was painted, at a height of nothing. Every
/// test passed, because every test asserted on the numbers beside the bars — and
/// the numbers were right the whole time.
///
/// `sleep_stage_share_card_test.dart` now pins the fill's SIZE. This pins its
/// picture, which is the assertion nobody thought to write.
void main() {
  final formatter = UnitFormatter(
    unitSystemProvider: () => UnitSystem.metric,
  );

  const minute = 60 * 1000;

  testWidgets('a night broken down by stage', (tester) async {
    // A 7h 50m night. The fractions are the bar widths, and they are given
    // separately from the percentages on purpose — the view-model clamps them to
    // the track, so a rounding error can never draw a bar past its own edge.
    await expectChartGoldenBothThemes(
      tester,
      () => SleepStageShareCard(
        shares: const [
          SleepStageShare(
            stageType: SleepStage.stageAwake,
            durationMs: 18 * minute,
            fraction: 0.038,
            percent: 4,
          ),
          SleepStageShare(
            stageType: SleepStage.stageRem,
            durationMs: 115 * minute,
            fraction: 0.245,
            percent: 24,
          ),
          SleepStageShare(
            stageType: SleepStage.stageLight,
            durationMs: 227 * minute,
            fraction: 0.483,
            percent: 48,
          ),
          SleepStageShare(
            stageType: SleepStage.stageDeep,
            durationMs: 110 * minute,
            fraction: 0.234,
            percent: 23,
          ),
        ],
        formatter: formatter,
      ),
      name: 'sleep_stage_share_card',
    );
  });

  testWidgets('a device that only says "asleep"', (tester) async {
    // One stage at nearly the full track, one at almost none of it — the two ends
    // of the bar's range in a single shot. A 2% bar that renders as nothing is
    // the same class of bug as a 100% bar that renders as nothing.
    await expectChartGoldenBothThemes(
      tester,
      () => SleepStageShareCard(
        shares: const [
          SleepStageShare(
            stageType: SleepStage.stageAwake,
            durationMs: 9 * minute,
            fraction: 0.019,
            percent: 2,
          ),
          SleepStageShare(
            stageType: SleepStage.stageSleeping,
            durationMs: 461 * minute,
            fraction: 0.981,
            percent: 98,
          ),
        ],
        formatter: formatter,
      ),
      name: 'sleep_stage_share_card_sleeping_only',
    );
  });
}
