import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/period_selection.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/ui/charts/metric_day_opener.dart';
import 'package:openvitals/ui/components/metric_detail_scaffold.dart';

Future<Widget> _bootstrap(Widget child, {double bottomInset = 0}) async {
  SharedPreferences.setMockInitialValues(<String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    child: MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Builder(
        builder: (context) => MediaQuery(
          // The system navigation bar, as the platform reports it: a three-button
          // bar is tall, gesture navigation is a sliver.
          data: MediaQuery.of(context).copyWith(
            padding: EdgeInsets.only(bottom: bottomInset),
          ),
          child: Scaffold(body: child),
        ),
      ),
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
    // The Day/Week/Month/Year selector (locale is en in tests).
    const labels = {
      TimeRange.day: 'Day',
      TimeRange.week: 'Week',
      TimeRange.month: 'Month',
      TimeRange.year: 'Year',
    };
    for (final range in TimeRange.values) {
      expect(find.text(labels[range]!), findsOneWidget);
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

  testWidgets('provides a day opener that drills into that day\'s Day view',
      (tester) async {
    final selections = <PeriodSelection>[];
    ValueChanged<LocalDate>? opener;
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          onSelectionChanged: selections.add,
          content: (period) => [
            Builder(
              builder: (context) {
                opener = MetricDetailDayOpener.maybeOf(context);
                return const Text('CONTENT');
              },
            ),
          ],
        ),
      ),
    );
    await tester.pumpAndSettle();

    // The scaffold hands its subtree a day opener (what a month heatmap cell
    // calls on tap), and starts on the heart key's default Week range.
    expect(opener, isNotNull);
    expect(selections.last.selectedRange, TimeRange.week);

    final target = LocalDate.now().minusDays(40);
    opener!(target);
    await tester.pumpAndSettle();

    // Opening a day switches to the Day range anchored on that date.
    expect(selections.last.selectedRange, TimeRange.day);
    expect(selections.last.selectedDate, target);
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

  testWidgets('changing the week mode reloads the selection and retitles',
      (tester) async {
    final selections = <PeriodSelection>[];
    SharedPreferences.setMockInitialValues(<String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    Widget build(WeekPeriodMode mode) => ProviderScope(
          overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            home: Scaffold(
              body: MetricDetailScaffold(
                rangePreferenceKey: PeriodRangePreferenceKey.heart,
                onRefresh: () async {},
                onSelectionChanged: selections.add,
                weekPeriodMode: mode,
                content: (period) => const [Text('CONTENT')],
              ),
            ),
          ),
        );

    await tester.pumpWidget(build(WeekPeriodMode.mondayToSunday));
    await tester.pumpAndSettle();
    expect(find.text('This week'), findsOneWidget);
    final loadsBefore = selections.length;

    await tester.pumpWidget(build(WeekPeriodMode.last7Days));
    await tester.pumpAndSettle();

    // The unchanged anchor now derives a differently-shaped period, so the
    // host must be asked to reload — without it the old data stayed on screen
    // under the re-derived period (a rolling window scattered over a
    // calendar-year grid) — and the title follows the new mode.
    expect(selections.length, loadsBefore + 1);
    expect(find.text('Last 7 days'), findsOneWidget);
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

  testWidgets('reserves the system navigation bar below the last item',
      (tester) async {
    // A three-button navigation bar. The app draws edge to edge, so the bar is
    // painted over the body; without room reserved for it, the foot of the list
    // sits underneath the buttons and cannot be scrolled into view.
    await tester.pumpWidget(
      await _bootstrap(
        MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: () async {},
          content: (period) => const [Text('CONTENT')],
        ),
        bottomInset: 48,
      ),
    );
    await tester.pumpAndSettle();

    final padding = tester.widget<ListView>(find.byType(ListView)).padding;
    expect(padding, const EdgeInsets.only(top: 8, bottom: 8 + 48));
  });

  testWidgets('reserves nothing extra when the bar is a gesture sliver',
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

    final padding = tester.widget<ListView>(find.byType(ListView)).padding;
    expect(padding, const EdgeInsets.only(top: 8, bottom: 8));
  });
}
