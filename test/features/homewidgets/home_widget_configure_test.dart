import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/homewidgets/home_widget_configure.dart';
import 'package:openvitals/features/homewidgets/home_widget_refresher.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:openvitals/features/homewidgets/home_widget_snapshots.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import 'fake_home_widget_client.dart';

/// The appWidgetId Android would hand the configuration launch.
const int _appWidgetId = 42;

class _StubDashboardDataLoader implements DashboardDataLoader {
  _StubDashboardDataLoader(this.data);

  final DashboardData data;

  @override
  Future<DashboardData> loadDashboard(DashboardQuery query) async => data;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// A loader that cannot reach Health Connect — the pick must still be recorded.
class _ThrowingDashboardDataLoader implements DashboardDataLoader {
  @override
  Future<DashboardData> loadDashboard(DashboardQuery query) async =>
      throw StateError('Health Connect is unavailable');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeConfigureChannel implements HomeWidgetConfigureChannel {
  int finished = 0;

  @override
  Future<int?> pendingAppWidgetId() async => _appWidgetId;

  @override
  Future<void> finish() async => finished++;
}

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  DashboardData today() => DashboardData(
        date: LocalDate.now(),
        steps: 8432,
        loadedMetrics: const {DashboardMetric.steps},
      );

  HomeWidgetRefresher refresher(
    HomeWidgetClient client, {
    DashboardDataLoader? loader,
  }) =>
      HomeWidgetRefresher(
        service: HomeWidgetService(client: client),
        health: FakeHealthRepository(),
        loadDashboardDay:
            LoadDashboardDayUseCase(loader ?? _StubDashboardDataLoader(today())),
        unitFormatter: UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
        localizations: l10n,
        goals: const DailyReadinessGoalInputs(),
      );

  /// The picker under a MaterialApp with the l10n delegates it reads from.
  Future<void> pumpPicker(
    WidgetTester tester, {
    required HomeWidgetRefresher widgetRefresher,
    required HomeWidgetConfigureChannel channel,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          homeWidgetRefresherProvider.overrideWithValue(widgetRefresher),
          homeWidgetConfigureChannelProvider.overrideWithValue(channel),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: const HomeMetricWidgetConfigureScreen(appWidgetId: _appWidgetId),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('renders the prompt and the metric catalog', (tester) async {
    await pumpPicker(
      tester,
      widgetRefresher: refresher(FakeHomeWidgetClient()),
      channel: _FakeConfigureChannel(),
    );

    expect(find.text(l10n.homeMetricWidgetConfigTitle), findsOneWidget);
    expect(find.text(l10n.homeMetricWidgetConfigPrompt), findsOneWidget);
    // The list is long; the first entries are enough to prove it is the catalog.
    expect(find.text(l10n.metricSteps), findsOneWidget);
    expect(find.text(l10n.metricDistance), findsOneWidget);
    expect(
      tester.widgetList<ListTile>(find.byType(ListTile)).length,
      lessThanOrEqualTo(homeMetricWidgetCatalog().length),
    );
  });

  testWidgets('excludes what the catalog excludes', (tester) async {
    await pumpPicker(
      tester,
      widgetRefresher: refresher(FakeHomeWidgetClient()),
      channel: _FakeConfigureChannel(),
    );

    // Kotlin's catalog drops caffeine; the Flutter one additionally drops
    // intensity minutes (no Kotlin `DashboardWidgetId` for it).
    expect(homeMetricWidgetCatalog(), isNot(contains(DashboardMetric.caffeine)));
    expect(
      homeMetricWidgetCatalog(),
      isNot(contains(DashboardMetric.intensityMinutes)),
    );
    await tester.scrollUntilVisible(find.text(l10n.metricCycle), 200);
    expect(find.text(l10n.metricCaffeine), findsNothing);
  });

  testWidgets('picking a metric persists its selection_id and pushes the tile',
      (tester) async {
    final client = FakeHomeWidgetClient();
    final channel = _FakeConfigureChannel();

    await pumpPicker(
      tester,
      widgetRefresher: refresher(client),
      channel: channel,
    );
    await tester.tap(find.text(l10n.metricSteps));
    await tester.pump();
    await tester.pump();

    // The handshake the refresher reads back, under this instance's prefix.
    expect(client.saved['metric.$_appWidgetId.selection_id'], 'STEPS');
    // …and the first snapshot, so the tile is not blank until the next refresh.
    expect(client.saved['metric.$_appWidgetId.title'], l10n.metricSteps);
    expect(client.saved['metric.$_appWidgetId.value'], '8,432');
    expect(client.saved['metric.$_appWidgetId.route'], 'metric/STEPS');
    expect(
      client.updated,
      contains(
        'tech.mmarca.openvitals.features.homewidgets.HomeMetricWidgetReceiver',
      ),
    );
    // Only now may the widget be kept (the plugin already set RESULT_CANCELED).
    expect(channel.finished, 1);
  });

  testWidgets('backing out without picking never finishes the configuration',
      (tester) async {
    final client = FakeHomeWidgetClient();
    final channel = _FakeConfigureChannel();

    await pumpPicker(
      tester,
      widgetRefresher: refresher(client),
      channel: channel,
    );

    // RESULT_CANCELED stands, so Android drops the half-placed widget.
    expect(channel.finished, 0);
    expect(client.saved, isEmpty);
  });

  testWidgets('a failed load still records the pick and finishes',
      (tester) async {
    final client = FakeHomeWidgetClient();
    final channel = _FakeConfigureChannel();

    await pumpPicker(
      tester,
      widgetRefresher:
          refresher(client, loader: _ThrowingDashboardDataLoader()),
      channel: channel,
    );
    await tester.tap(find.text(l10n.metricSteps));
    await tester.pump();
    await tester.pump();

    // No snapshot to push, but the selection survives: the next refresh brings
    // the tile up to date rather than leaving it stuck on "Select a metric".
    expect(client.saved['metric.$_appWidgetId.selection_id'], 'STEPS');
    expect(client.saved.containsKey('metric.$_appWidgetId.title'), isFalse);
    expect(channel.finished, 1);
  });
}
