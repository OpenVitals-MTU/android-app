import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/metric_card.dart';

const _accent = Color(0xFF4CAF50);

/// The selector chip labels the widget renders (locale is en in tests).
String _rangeLabel(TimeRange range) => switch (range) {
      TimeRange.day => 'Day',
      TimeRange.week => 'Week',
      TimeRange.month => 'Month',
      TimeRange.year => 'Year',
    };

Future<void> _pump(WidgetTester tester, Widget child) async {
  await tester.pumpWidget(
    MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: child),
    ),
  );
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets('MetricCard shows title, value, unit, subtitle and source',
      (tester) async {
    await _pump(
      tester,
      const MetricCard(
        title: 'Steps',
        value: '8,432',
        unit: 'steps',
        icon: Icons.directions_walk,
        accentColor: _accent,
        subtitle: 'Goal 10,000',
        source: 'Fitbit',
      ),
    );
    expect(find.text('Steps'), findsOneWidget);
    expect(find.text('8,432'), findsOneWidget);
    expect(find.text('steps'), findsOneWidget);
    expect(find.text('Goal 10,000'), findsOneWidget);
    expect(find.text('Fitbit'), findsOneWidget);
  });

  testWidgets('MetricCard onTap fires', (tester) async {
    var tapped = false;
    await _pump(
      tester,
      MetricCard(
        title: 'Steps',
        value: '8,432',
        unit: 'steps',
        icon: Icons.directions_walk,
        accentColor: _accent,
        onTap: () => tapped = true,
      ),
    );
    await tester.tap(find.text('8,432'));
    await tester.pump();
    expect(tapped, isTrue);
  });

  testWidgets('MetricCardPlaceholder shows its message', (tester) async {
    await _pump(
      tester,
      const MetricCardPlaceholder(
        title: 'Sleep',
        icon: Icons.bedtime,
        accentColor: _accent,
        message: 'No data for this period',
      ),
    );
    expect(find.text('Sleep'), findsOneWidget);
    expect(find.text('No data for this period'), findsOneWidget);
  });

  testWidgets('TimeRangeSelector renders all ranges and reports selection',
      (tester) async {
    TimeRange? selected;
    await _pump(
      tester,
      TimeRangeSelector(
        selected: TimeRange.week,
        onSelect: (range) => selected = range,
      ),
    );
    for (final range in TimeRange.values) {
      expect(find.text(_rangeLabel(range)), findsOneWidget);
    }
    await tester.tap(find.text('Month'));
    await tester.pump();
    expect(selected, TimeRange.month);
  });

  testWidgets('SectionHeader renders its text', (tester) async {
    await _pump(tester, const SectionHeader('Trends'));
    expect(find.text('Trends'), findsOneWidget);
  });
}
