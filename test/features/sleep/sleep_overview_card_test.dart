import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/sleep/application/sleep_display.dart';
import 'package:openvitals/features/sleep/presentation/sleep_cards.dart';

/// The overview now highlights time ASLEEP and keeps time in bed as a demoted
/// tile, with awake beside it. Guards that the two are distinct figures and both
/// shown — the whole point of "focus on sleep, not time in bed".
void main() {
  final formatter = UnitFormatter(unitSystemProvider: () => UnitSystem.metric);

  const summary = SleepOverviewSummary(
    sleepScore: 82,
    sleepDurationMs: 6 * 3600000 + 30 * 60000, // 6h30m asleep
    timeInBedMs: 7 * 3600000 + 45 * 60000, // 7h45m in bed (full span)
    awakeDurationMs: 45 * 60000, // 45m awake
    remDurationMs: 60 * 60000,
    coreDurationMs: 200 * 60000,
    deepDurationMs: 60 * 60000,
    sleepEfficiencyPercent: 84,
  );

  testWidgets('highlights Sleep (asleep) and demotes Time in bed', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: SleepOverviewCard(
          summary: summary,
          formatter: formatter,
          periodTitle: 'This week',
        ),
      ),
    ));

    // Both tiles present — sleep is highlighted, time in bed is kept but demoted.
    expect(find.text('Sleep'), findsOneWidget);
    expect(find.text('Time in bed'), findsOneWidget);
    expect(find.text('Awake'), findsOneWidget);
    expect(find.text('Sleep efficiency'), findsOneWidget);

    // Asleep, in-bed and awake are three DISTINCT durations, not the same number.
    expect(find.text(formatter.duration(summary.sleepDurationMs)), findsOneWidget);
    expect(find.text(formatter.duration(summary.timeInBedMs)), findsOneWidget);
    expect(find.text(formatter.duration(summary.awakeDurationMs)), findsOneWidget);
    expect(summary.sleepDurationMs, lessThan(summary.timeInBedMs));
  });
}
