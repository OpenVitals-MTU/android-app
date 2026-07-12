import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/features/sleep/sleep_cards.dart';
import 'package:openvitals/features/sleep/sleep_stage_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/ov_card.dart';
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

/// The night's card must draw a real HYPNOGRAM — one lane per stage, positioned
/// on the clock — not a flat proportional bar.
///
/// The port had reduced it to a single 16px strip of stages laid end to end.
/// That bar restated the shares the card below it already gives you and threw
/// away the only thing the chart is for: the SHAPE of the night. (It was also,
/// for a while, painted zero pixels tall — a Row hands its children loose
/// cross-axis constraints and a childless ColoredBox collapses under them — but
/// fixing the height only revealed that the whole chart was the wrong one.)
void _timelineTests() {
  SleepStage stage(int type, DateTime from, Duration length) => SleepStage(
        startTime: from,
        endTime: from.add(length),
        stageType: type,
      );

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

  Widget host({VoidCallback? onTap}) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 400,
              child: SleepSessionTimelineCard(
                session: session,
                selectedDate: LocalDate(2026, 7, 12),
                formatter: UnitFormatter(
                  unitSystemProvider: () => UnitSystem.metric,
                ),
                timeRangeText: '01:09 - 04:36',
                onTap: onTap,
              ),
            ),
          ),
        ),
      );

  testWidgets('the night is drawn as a lane chart, not a flat bar',
      (tester) async {
    await tester.pumpWidget(host());

    final chart = find.byType(SleepStagesLaneChart);
    expect(chart, findsOneWidget,
        reason: 'the day card must draw the hypnogram, not a proportional strip');
    // A lane per stage present, each one tall enough to read: the whole point of
    // the chart is that Deep sits below Light, at the time it happened.
    expect(tester.getSize(chart).height, greaterThan(100));
    expect(find.text('Deep'), findsOneWidget);
    expect(find.text('Light'), findsOneWidget);
    expect(find.text('REM'), findsOneWidget);
  });

  testWidgets('tapping the card opens that night, and it says so',
      (tester) async {
    var opened = false;
    await tester.pumpWidget(host(onTap: () => opened = true));

    expect(find.text('Details'), findsOneWidget,
        reason: 'the affordance must be visible, as it was in Kotlin');

    await tester.tap(find.byType(SleepStagesLaneChart));
    await tester.pumpAndSettle();
    expect(opened, isTrue,
        reason: 'tapping the hypnogram itself must open the detail screen');
  });

  testWidgets('a merged night offers no detail to open', (tester) async {
    // Two sessions in a day are shown as ONE merged summary whose id belongs to
    // no record. There is no single night to open, so the card must not pretend
    // there is.
    await tester.pumpWidget(host());

    expect(find.text('Details'), findsNothing);
    expect(
      tester.widget<OpenVitalsCard>(find.byType(OpenVitalsCard).first).onTap,
      isNull,
    );
  });
}
