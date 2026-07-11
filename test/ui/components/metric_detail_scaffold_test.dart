import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/ui/components/metric_detail_scaffold.dart';

Future<Widget> _bootstrap(Widget child) async {
  SharedPreferences.setMockInitialValues(<String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: child),
    ),
  );
}

void main() {
  testWidgets('renders header, range selector and content for the period',
      (tester) async {
    final selections = <PeriodSelection>[];
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          onSelectionChanged: selections.add,
          headerItems: const [Text('HEADER')],
          content: (period) => const [Text('CONTENT')],
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('HEADER'), findsOneWidget);
    expect(find.text('CONTENT'), findsOneWidget);
    // The Day/Week/Month/Year selector.
    for (final range in TimeRange.values) {
      expect(find.text(range.label), findsOneWidget);
    }
    // The default range for the heart key is Week → "This week" title.
    expect(find.text('This week'), findsOneWidget);
    // The scaffold notified the host of the initial selection.
    expect(selections, isNotEmpty);
    expect(selections.last.selectedRange, TimeRange.week);
  });

  testWidgets('selecting a range drives the navigator + content',
      (tester) async {
    final selections = <PeriodSelection>[];
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          onSelectionChanged: selections.add,
          content: (period) => const [Text('CONTENT')],
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Day'));
    await tester.pumpAndSettle();

    expect(find.text('Today'), findsOneWidget);
    expect(selections.last.selectedRange, TimeRange.day);
  });

  testWidgets('tapping next past today is a no-op (forward-capped)',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          content: (period) => const [Text('CONTENT')],
        ),
      ),
    );
    await tester.pumpAndSettle();

    // At the current period the next button is disabled.
    final nextButton = tester.widget<IconButton>(
      find.ancestor(
        of: find.byTooltip('Next period'),
        matching: find.byType(IconButton),
      ),
    );
    expect(nextButton.onPressed, isNull);

    // Tapping it changes nothing.
    await tester.tap(find.byTooltip('Next period'));
    await tester.pumpAndSettle();
    expect(find.text('This week'), findsOneWidget);
  });

  testWidgets('rolling dates retitle the week/month/year periods',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          weekPeriodMode: WeekPeriodMode.last7Days,
          content: (period) => const [Text('CONTENT')],
        ),
      ),
    );
    await tester.pumpAndSettle();

    // The default range for the heart key is Week.
    expect(find.text('Last 7 days'), findsOneWidget);

    await tester.tap(find.text('Month'));
    await tester.pumpAndSettle();
    expect(find.text('Last 30 days'), findsOneWidget);

    await tester.tap(find.text('Year'));
    await tester.pumpAndSettle();
    expect(find.text('Last 365 days'), findsOneWidget);

    // Stepping back off today falls back to the dated title.
    await tester.tap(find.byTooltip('Previous period'));
    await tester.pumpAndSettle();
    expect(find.text('Last 365 days'), findsNothing);
  });

  testWidgets('renders the error block from a ScreenError', (tester) async {
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          errorText: 'Could not load heart data',
          content: (period) => const [Text('CONTENT')],
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Could not load heart data'), findsOneWidget);
  });
}
