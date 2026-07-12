import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/sleep_cards.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
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
  _timelineTests();

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

/// The night's hypnogram — the "Sleep" card's stage timeline — had the SAME bug
/// as the share bars one card below it, and it read as "the daily graph is
/// missing": the card showed 3h 27m and 01:09–04:36 and then a blank 16px strip.
///
/// A Row hands its children LOOSE cross-axis constraints under the default centre
/// alignment. Expanded makes the WIDTH tight and says nothing about the height, so
/// every childless ColoredBox band collapsed to zero height.
void _timelineTests() {
  SleepStage stage(int type, DateTime from, Duration length) => SleepStage(
        startTime: from,
        endTime: from.add(length),
        stageType: type,
      );

  testWidgets('the night timeline paints its stage bands with real height',
      (tester) async {
    final start = DateTime.utc(2026, 7, 12, 1, 9);
    final session = SleepData(
      id: 's1',
      startTime: start,
      endTime: start.add(const Duration(hours: 3, minutes: 27)),
      durationMs: const Duration(hours: 3, minutes: 27).inMilliseconds,
      source: 'test',
      stages: [
        stage(SleepStage.stageLight, start, const Duration(minutes: 106)),
        stage(SleepStage.stageDeep, start.add(const Duration(minutes: 106)),
            const Duration(minutes: 53)),
        stage(SleepStage.stageRem, start.add(const Duration(minutes: 159)),
            const Duration(minutes: 48)),
      ],
    );

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: 400,
            child: SleepSessionTimelineCard(
              session: session,
              formatter: UnitFormatter(
                unitSystemProvider: () => UnitSystem.metric,
              ),
              timeRangeText: '01:09 - 04:36',
            ),
          ),
        ),
      ),
    ));

    // Scoped to the rounded strip: the card's own surface is a ColoredBox too.
    final bands = find.descendant(
      of: find.byType(ClipRRect),
      matching: find.byType(ColoredBox),
    );
    expect(bands, findsNWidgets(3), reason: 'three stages -> three bands');
    for (var i = 0; i < 3; i++) {
      final size = tester.getSize(bands.at(i));
      expect(size.height, greaterThan(0),
          reason: 'a stage band was painted zero pixels tall');
      expect(size.width, greaterThan(0));
    }
    // Light (106 min) must be visibly wider than REM (48 min).
    final widths = [for (var i = 0; i < 3; i++) tester.getSize(bands.at(i)).width]
      ..sort();
    expect(widths.last, greaterThan(widths.first));
  });
}
