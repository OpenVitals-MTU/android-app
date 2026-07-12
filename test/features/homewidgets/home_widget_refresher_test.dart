import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/domain/insights/daily_readiness.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/homewidgets/home_widget_refresher.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';
import 'package:openvitals/l10n/app_localizations.dart';

import 'fake_home_widget_client.dart';

const String _metricReceiver =
    'tech.mmarca.openvitals.features.homewidgets.HomeMetricWidgetReceiver';

class _StubDashboardDataLoader implements DashboardDataLoader {
  _StubDashboardDataLoader(this.data);

  final DashboardData data;
  final List<DashboardQuery> queries = [];

  @override
  Future<Result<DashboardData>> loadDashboard(DashboardQuery query) async {
    queries.add(query);
    return Ok(data);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _ThrowingDashboardDataLoader implements DashboardDataLoader {
  @override
  Future<Result<DashboardData>> loadDashboard(DashboardQuery query) async =>
      Ok(throw StateError('Health Connect is unavailable'));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  HomeWidgetRefresher refresher(
    HomeWidgetClient client, {
    DashboardDataLoader? loader,
    DashboardData? data,
    FakeHealthRepository? health,
  }) =>
      HomeWidgetRefresher(
        service: HomeWidgetService(client: client),
        health: health ?? FakeHealthRepository(),
        loadDashboardDay: LoadDashboardDayUseCase(
          loader ??
              _StubDashboardDataLoader(
                data ?? DashboardData(date: LocalDate.now()),
              ),
        ),
        unitFormatter: UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
        localizations: l10n,
        goals: const DailyReadinessGoalInputs(),
      );

  // Regression: shipped to a device rendering "Grant permission in OpenVitals"
  // on every widget even though the permissions were granted. HealthDataSource
  // starts at `notSupported` and the repositories report NO granted permissions
  // while it does, so a load that skips this resolve marks every metric as
  // permission-missing. The app gets the resolve for free from
  // HealthConnectGate; the alarm isolate and the modal configure launch (which
  // never mounts the gate) do not.
  test('refresh resolves Health Connect access before loading', () async {
    final client = FakeHomeWidgetClient();
    final health = FakeHealthRepository();

    await refresher(client, health: health).refresh();

    expect(health.refreshCalls, 1);
  });

  DashboardData today() => DashboardData(
        date: LocalDate.now(),
        steps: 8432,
        restingHeartRateBpm: 52,
        restingHeartRateBaselineBpm: 54,
        loadedMetrics: const {DashboardMetric.restingHeartRate},
      );

  test('pushes all three shared widgets from one load', () async {
    final client = FakeHomeWidgetClient();

    await refresher(client, data: today()).refresh();

    // Each shared widget writes under its own namespace, and each is redrawn.
    expect(client.saved['daily_readiness.title'], l10n.screenDailyReadiness);
    expect(client.saved['body_energy.title'], l10n.screenBodyEnergy);
    expect(client.saved['today_vitals.title'], l10n.homeWidgetTodayTitle);
    expect(
      client.updated,
      containsAll([
        'tech.mmarca.openvitals.features.homewidgets.HomeDailyReadinessWidgetReceiver',
        'tech.mmarca.openvitals.features.homewidgets.HomeBodyEnergyWidgetReceiver',
        'tech.mmarca.openvitals.features.homewidgets.HomeTodayVitalsWidgetReceiver',
      ]),
    );
  });

  test('pushes one snapshot per placed metric instance, keyed by its id',
      () async {
    final client = FakeHomeWidgetClient(
      installed: const [
        HomeWidgetInstance(appWidgetId: 11, className: _metricReceiver),
        HomeWidgetInstance(appWidgetId: 12, className: _metricReceiver),
      ],
      // What the native configuration activity persisted for each instance.
      stored: const {
        'metric.11.selection_id': 'STEPS',
        'metric.12.selection_id': 'RESTING_HEART_RATE',
      },
    );

    await refresher(client, data: today()).refresh();

    expect(client.saved['metric.11.title'], l10n.metricSteps);
    expect(client.saved['metric.11.value'], '8,432');
    expect(client.saved['metric.11.route'], 'metric/STEPS');
    expect(client.saved['metric.11.selection_id'], 'STEPS');

    expect(client.saved['metric.12.title'], l10n.metricRestingHeartRate);
    expect(client.saved['metric.12.value'], '52');
    expect(client.saved['metric.12.unit'], 'bpm');
    expect(client.saved['metric.12.selection_id'], 'RESTING_HEART_RATE');
  });

  test('leaves an unconfigured or unknown instance alone', () async {
    final client = FakeHomeWidgetClient(
      installed: const [
        HomeWidgetInstance(appWidgetId: 20, className: _metricReceiver),
        HomeWidgetInstance(appWidgetId: 21, className: _metricReceiver),
      ],
      stored: const {'metric.21.selection_id': 'NOT_A_METRIC'},
    );

    await refresher(client, data: today()).refresh();

    // Pushing over these would claim they are configured; they keep their
    // native "Select a metric" state instead.
    expect(client.saved.containsKey('metric.20.title'), isFalse);
    expect(client.saved.containsKey('metric.21.title'), isFalse);
  });

  test('a beverage instance is not touched (a later phase)', () async {
    final client = FakeHomeWidgetClient(
      installed: const [
        HomeWidgetInstance(
          appWidgetId: 30,
          className:
              'tech.mmarca.openvitals.features.homewidgets.HomeQuickBeverageWidgetReceiver',
        ),
      ],
      stored: const {'beverage.30.selection_id': 'coffee'},
    );

    await refresher(client, data: today()).refresh();

    expect(client.saved.containsKey('beverage.30.title'), isFalse);
  });

  test('loads today, forcing a fresh read', () async {
    final loader = _StubDashboardDataLoader(today());

    await refresher(FakeHomeWidgetClient(), loader: loader).refresh();

    expect(loader.queries.single.date, LocalDate.now());
    expect(loader.queries.single.refreshMode.name, 'force');
  });

  test('a failed load never throws', () async {
    final client = FakeHomeWidgetClient();

    await expectLater(
      refresher(client, loader: _ThrowingDashboardDataLoader()).refresh(),
      completes,
    );
    expect(client.saved, isEmpty);
  });

  test('a failing client cannot stop the other widgets updating', () async {
    final client = _FlakyClient(failingKeyPrefix: 'daily_readiness.');

    await refresher(client, data: today()).refresh();

    expect(client.saved.containsKey('daily_readiness.title'), isFalse);
    expect(client.saved['body_energy.title'], l10n.screenBodyEnergy);
    expect(client.saved['today_vitals.title'], l10n.homeWidgetTodayTitle);
  });

  test('push reuses the caller\'s data without loading', () async {
    final client = FakeHomeWidgetClient();
    final loader = _StubDashboardDataLoader(today());

    await refresher(client, loader: loader).push(today());

    expect(loader.queries, isEmpty);
    expect(client.saved['today_vitals.title'], l10n.homeWidgetTodayTitle);
  });
}

/// A client whose writes blow up for one widget's namespace.
class _FlakyClient extends FakeHomeWidgetClient {
  _FlakyClient({required this.failingKeyPrefix});

  final String failingKeyPrefix;

  @override
  Future<void> saveWidgetData(String key, Object? value) {
    if (key.startsWith(failingKeyPrefix)) {
      throw StateError('widget storage is full');
    }
    return super.saveWidgetData(key, value);
  }
}
