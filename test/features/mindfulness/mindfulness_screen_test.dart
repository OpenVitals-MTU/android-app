import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/mindfulness_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/activity_week_mode.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/query/mindfulness_period_data.dart';
import 'package:openvitals/features/mindfulness/presentation/mindfulness_screen.dart';
import 'package:openvitals/data/source/health/health_permissions.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_card.dart';

class _FakeMindfulnessRepository implements MindfulnessRepository {
  _FakeMindfulnessRepository({this.sessions = const <MindfulnessSession>[]});

  final List<MindfulnessSession> sessions;

  @override
  Future<MindfulnessPeriodData> loadMindfulnessPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      MindfulnessPeriodData(sessions: sessions);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

MindfulnessSession _session(DateTime start, int minutes, {String? title}) =>
    MindfulnessSession(
      id: 'session-${start.millisecondsSinceEpoch}',
      title: title ?? 'Morning meditation',
      startTime: start,
      endTime: start.add(Duration(minutes: minutes)),
      durationMs: minutes * 60000,
      source: 'com.openvitals',
    );

Future<Widget> _bootstrap({
  required _FakeMindfulnessRepository repository,
  required Set<String> granted,
  ActivityWeekMode weekMode = ActivityWeekMode.mondayToSunday,
  TimeRange range = TimeRange.week,
}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  // Seed the preferences the screen reads before the scope builds its own
  // repository over the same SharedPreferences instance.
  PreferencesRepository(prefs)
    ..activityWeekMode = weekMode
    ..setTimeRangeFor(PeriodRangePreferenceKey.mindfulness, range);
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      mindfulnessRepositoryProvider.overrideWithValue(repository),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider.overrideWith((ref) async => granted),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: MindfulnessScreen(),
    ),
  );
}

void main() {
  testWidgets('renders total card + session list once loaded', (tester) async {
    final now = DateTime.now();
    final repo = _FakeMindfulnessRepository(sessions: [
      _session(now, 10),
      _session(now.subtract(const Duration(days: 1)), 15,
          title: 'Evening breathing'),
    ]);
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readMindfulness}),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(MetricCard), findsWidgets);
    expect(find.text('Total mindfulness'), findsOneWidget);
    expect(find.text('Sessions'), findsWidgets);

    // The session list renders below the fold; scroll it into view.
    await tester.scrollUntilVisible(
      find.text('Morning meditation'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Morning meditation'), findsOneWidget);
  });

  testWidgets('shows the access gate when the permission is missing',
      (tester) async {
    final repo = _FakeMindfulnessRepository(sessions: [_session(DateTime.now(), 10)]);
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: const <String>{}),
    );
    await tester.pumpAndSettle();

    expect(find.text('Permissions needed'), findsOneWidget);
    expect(find.byType(MetricCard), findsNothing);
  });

  testWidgets('shows the empty placeholder with no sessions', (tester) async {
    final repo = _FakeMindfulnessRepository();
    await tester.pumpWidget(
      await _bootstrap(repository: repo, granted: {HcPermissions.readMindfulness}),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
  });

  // Every period title on the screen — navigator, card subtitles, chart summary
  // — must name the window the same way. With the rolling week mode on a month
  // that ends today, that name is "Last 30 days"; "This month" anywhere means a
  // title defaulted to the calendar mode instead of reading the preference.
  testWidgets('rolling week mode names every period title "Last 30 days"',
      (tester) async {
    final repo = _FakeMindfulnessRepository(
      sessions: [_session(DateTime.now(), 10)],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: {HcPermissions.readMindfulness},
        weekMode: ActivityWeekMode.last7Days,
        range: TimeRange.month,
      ),
    );
    await tester.pumpAndSettle();

    // The navigator title, the "Total mindfulness" card subtitle and the bar
    // chart's summary line.
    expect(find.textContaining('Last 30 days'), findsNWidgets(3));
    expect(find.textContaining('This month'), findsNothing);
  });

  testWidgets('calendar week mode keeps the "This month" titles',
      (tester) async {
    final repo = _FakeMindfulnessRepository(
      sessions: [_session(DateTime.now(), 10)],
    );
    await tester.pumpWidget(
      await _bootstrap(
        repository: repo,
        granted: {HcPermissions.readMindfulness},
        range: TimeRange.month,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('This month'), findsNWidgets(3));
    expect(find.textContaining('Last 30 days'), findsNothing);
  });
}
