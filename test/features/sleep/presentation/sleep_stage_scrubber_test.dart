import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart' show DateFormat;

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/presentation/sleep_stage_chart.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Scrubbing the hypnogram to read the clock time — and the stage — at any moment
/// of the night, and (as with every chart) still being able to scroll the page it
/// sits on.
void main() {
  final formatter = UnitFormatter(unitSystemProvider: () => UnitSystem.metric);

  // A night that straddles midnight: 470 minutes from 23:15 to 07:05.
  final bedtime = DateTime(2026, 6, 21, 23, 15);
  final wakeUp = DateTime(2026, 6, 22, 7, 5);

  // The tooltip formats time exactly as the chart does; build the expected string
  // the same way so the assertion matches its space characters (modern intl uses
  // a narrow no-break space before AM/PM, not a plain one).
  String timeAtFraction(double fraction) {
    final totalMs = wakeUp.difference(bedtime).inMilliseconds;
    final at = bedtime.add(Duration(milliseconds: (fraction * totalMs).round()));
    return DateFormat.jm('en').format(at.toLocal());
  }

  SleepStage stage(int type, int startMinute, int endMinute) => SleepStage(
        startTime: bedtime.add(Duration(minutes: startMinute)),
        endTime: bedtime.add(Duration(minutes: endMinute)),
        stageType: type,
      );

  final night = <SleepStage>[
    stage(SleepStage.stageAwake, 0, 10),
    stage(SleepStage.stageLight, 10, 145),
    stage(SleepStage.stageDeep, 145, 275),
    stage(SleepStage.stageRem, 275, 355),
    stage(SleepStage.stageLight, 355, 470),
  ];

  Widget harness({ScrollController? controller}) => MaterialApp(
        locale: const Locale('en'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: ListView(
            controller: controller,
            children: [
              const SizedBox(height: 400),
              SleepStagesLaneChart(
                stages: night,
                formatter: formatter,
                timelineStart: bedtime,
                timelineEnd: wakeUp,
              ),
              const SizedBox(height: 1200),
            ],
          ),
        ),
      );

  testWidgets('inert until touched — no crosshair, no tooltip', (tester) async {
    await tester.pumpWidget(harness());
    // "1:12 AM" is a quarter into the night; it is not one of the three axis
    // labels (11:15 PM / 3:10 AM / 7:05 AM), so it can only come from a scrub.
    expect(find.text(timeAtFraction(0.25)), findsNothing);
  });

  testWidgets('a horizontal drag reveals the clock time and stage at the finger',
      (tester) async {
    await tester.pumpWidget(harness());

    final scrubber = find.byType(SleepStageScrubber);
    final rect = tester.getRect(scrubber);
    // Start at the centre and slide to a quarter across: 0.25 of 470 min past
    // 23:15 is 01:12, and the stage there is Light (10–145 min).
    final gesture = await tester.startGesture(rect.center);
    await gesture.moveTo(Offset(rect.left + rect.width * 0.25, rect.center.dy));
    await tester.pump();

    expect(find.text(timeAtFraction(0.25)), findsOneWidget);
    // The lane label reads "Light - <total>", so a bare "Light" is the tooltip.
    expect(find.text('Light'), findsOneWidget);

    // And it lets go cleanly.
    await gesture.up();
    await tester.pumpAndSettle();
    expect(find.text(timeAtFraction(0.25)), findsNothing);
  });

  testWidgets('the time tracks the finger across the night', (tester) async {
    await tester.pumpWidget(harness());

    final scrubber = find.byType(SleepStageScrubber);
    final rect = tester.getRect(scrubber);
    final gesture = await tester.startGesture(rect.center);

    // Three-quarters across: 0.75 of 470 min past 23:15 is 05:07. This asserts the
    // mapping itself — the clock time tracks the finger — where the test above
    // covered the stage readout.
    await gesture.moveTo(Offset(rect.left + rect.width * 0.75, rect.center.dy));
    await tester.pump();
    expect(find.text(timeAtFraction(0.75)), findsOneWidget);

    await gesture.up();
    await tester.pumpAndSettle();
  });

  testWidgets('a VERTICAL drag starting on the chart still scrolls the page',
      (tester) async {
    final controller = ScrollController();
    addTearDown(controller.dispose);
    await tester.pumpWidget(harness(controller: controller));

    expect(controller.offset, 0);
    await tester.drag(find.byType(SleepStageScrubber), const Offset(0, -300));
    await tester.pumpAndSettle();

    expect(
      controller.offset,
      greaterThan(0),
      reason: 'a vertical drag must be left to the scrolling page',
    );
    // And it did not scrub while scrolling.
    expect(find.textContaining('AM'), findsWidgets); // axis labels remain
    expect(find.text(timeAtFraction(0.25)), findsNothing);
  });
}
