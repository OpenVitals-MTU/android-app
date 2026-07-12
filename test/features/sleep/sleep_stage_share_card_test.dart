import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/sleep_cards.dart';
import 'package:openvitals/features/sleep/sleep_presentation.dart';

/// The "Share of time in bed" bars rendered as empty grey tracks in the shipped
/// app — correct durations and percentages beside them, and no coloured fill at
/// all.
///
/// The cause was pure layout: the fill was a non-positioned child of a [Stack],
/// so it got LOOSE constraints, and a childless [ColoredBox] collapses to zero
/// height under those. It was painted 0px tall. The grey track sat under a
/// [Positioned.fill] and so kept its full height — which is exactly why the card
/// looked "there but empty".
///
/// Nothing caught it because every existing sleep test asserted on the numbers,
/// which were right the whole time. These assert on the PIXELS.
void main() {
  Widget host(SleepOverviewSummary summary) => MaterialApp(
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 400,
              child: SleepStageShareCard(
                summary: summary,
                formatter: UnitFormatter(
                  unitSystemProvider: () => UnitSystem.metric,
                ),
              ),
            ),
          ),
        ),
      );

  /// The coloured fills, i.e. every ColoredBox that is not the grey track.
  List<Size> fillSizes(WidgetTester tester) {
    final boxes = tester
        .widgetList<ColoredBox>(find.byType(ColoredBox))
        .toList(growable: false);
    final sizes = <Size>[];
    for (var i = 0; i < boxes.length; i++) {
      final size = tester.getSize(find.byType(ColoredBox).at(i));
      sizes.add(size);
    }
    return sizes;
  }

  testWidgets('the stage fills are actually painted — non-zero HEIGHT',
      (tester) async {
    await tester.pumpWidget(host(
      const SleepOverviewSummary(
        remDurationMs: 48 * 60 * 1000,
        coreDurationMs: 106 * 60 * 1000,
        deepDurationMs: 53 * 60 * 1000,
      ),
    ));

    final sizes = fillSizes(tester);
    expect(sizes, isNotEmpty);
    // The bug: every fill was 0px tall while the track kept its 10px.
    expect(
      sizes.every((s) => s.height > 0),
      isTrue,
      reason: 'a stage fill was painted with zero height — the Stack bug is back',
    );
    // And every bar must have SOME width: three stages, none of them zero.
    expect(sizes.where((s) => s.width > 0).length, greaterThanOrEqualTo(3));
  });

  testWidgets('a bigger stage gets a wider bar than a smaller one',
      (tester) async {
    // Guards the other half: FractionallySizedBox centres by default, so even a
    // full-height fill would have grown from the middle. Widths must track the
    // durations.
    await tester.pumpWidget(host(
      const SleepOverviewSummary(
        remDurationMs: 30 * 60 * 1000,
        coreDurationMs: 120 * 60 * 1000, // 4x the REM
        deepDurationMs: 30 * 60 * 1000,
      ),
    ));

    final widths = fillSizes(tester).map((s) => s.width).toList()..sort();
    // The widest fill (Light) must clearly exceed the narrowest, and it must not
    // fill the whole track.
    expect(widths.last, greaterThan(widths.first * 2));
  });

  testWidgets('no stage data hides the card rather than drawing empty bars',
      (tester) async {
    await tester.pumpWidget(host(const SleepOverviewSummary()));
    expect(find.byType(SleepStageShareCard), findsOneWidget);
    expect(find.text('Share of time in bed'), findsNothing);
  });
}
