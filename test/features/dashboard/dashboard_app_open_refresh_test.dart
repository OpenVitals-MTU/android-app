import 'package:clock/clock.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/bootstrap/data_refresh_bootstrap.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/sync/history_sync_scheduler.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/dashboard/presentation/dashboard_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/period_navigator.dart';
import 'package:openvitals/ui/components/summary_ring_card.dart';

/// A data source whose availability can be flipped mid-test, standing in for
/// Health Connect finishing an update, or the user granting a permission in the
/// Health Connect app, while OpenVitals sits in the background.
class _MutableHealthDataSource extends HealthDataSource {
  _MutableHealthDataSource(this._availability, this.granted) {
    cachedAvailability = _availability;
  }

  HealthConnectAvailability _availability;
  Set<String> granted;

  set current(HealthConnectAvailability value) {
    _availability = value;
    cachedAvailability = value;
  }

  @override
  Future<HealthConnectAvailability> availability() async => _availability;

  @override
  Future<Set<String>> grantedPermissions() async => granted;
}

/// Counts loads, so a test can prove a refresh actually re-read rather than
/// re-rendering what was already there.
class _CountingUseCase extends LoadDashboardDayUseCase {
  _CountingUseCase() : super(DashboardDataLoader(HealthDataSource()));

  int calls = 0;

  @override
  Future<Result<DashboardData>> call(DashboardQuery query) async {
    calls++;
    return Ok(DashboardData(
      date: query.date,
      steps: 8000,
      distanceMeters: 5200,
      caloriesKcal: 540,
      loadedMetrics: query.visibleMetrics,
      supportedMetrics: DashboardMetric.values.toSet(),
    ));
  }
}

/// Counts drains without owning any of the three history sync services.
class _RecordingScheduler implements HistorySyncScheduler {
  int drains = 0;

  @override
  Future<void> drainIncremental() async => drains++;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

final Set<String> _minimumPermissions =
    HealthDataSource().permissionService.minimumOnboardingPermissions;

void main() {
  late _MutableHealthDataSource source;
  late _CountingUseCase useCase;
  late _RecordingScheduler scheduler;
  late DateTime now;

  /// Mounts the dashboard under [DataRefreshBootstrap], exactly as `main.dart`
  /// nests them.
  Future<void> pump(
    WidgetTester tester, {
    required HealthConnectAvailability availability,
    Set<String>? granted,
  }) async {
    tester.view.physicalSize = const Size(800, 1400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    source = _MutableHealthDataSource(
      availability,
      granted ?? _minimumPermissions,
    );
    useCase = _CountingUseCase();
    scheduler = _RecordingScheduler();
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();

    await tester.pumpWidget(ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        healthDataSourceProvider.overrideWithValue(source),
        // Deliberately NOT a fixed value: the point of the fix is that these
        // are re-resolved on resume, so they must read the mutable source.
        healthConnectAvailabilityProvider
            .overrideWith((ref) async => source.availability()),
        grantedHealthPermissionsProvider
            .overrideWith((ref) async => source.granted),
        loadDashboardDayUseCaseProvider.overrideWithValue(useCase),
        historySyncSchedulerProvider.overrideWithValue(scheduler),
      ],
      child: const DataRefreshBootstrap(
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(body: DashboardScreen()),
        ),
      ),
    ));
    await tester.pumpAndSettle();
  }

  /// The legal background/foreground walk — AppLifecycleListener asserts on
  /// skipped transitions.
  Future<void> resume(WidgetTester tester) async {
    final binding = tester.binding;
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
  }

  Future<void> atClock(Future<void> Function() body) =>
      withClock(Clock(() => now), body);

  setUp(() => now = DateTime(2025, 6, 25, 14, 30));

  testWidgets('the app-open refresh recovers a dashboard stuck behind the '
      'Health Connect access gate', (tester) async {
    await atClock(() async {
      // A cold start that resolved notSupported — Health Connect mid-update.
      // The gate replaces the dashboard, so the resume wiring that used to live
      // inside it was never constructed and the app could not recover without a
      // force-stop.
      await pump(tester, availability: HealthConnectAvailability.notSupported);
      expect(find.byType(DayNavigator), findsNothing,
          reason: 'the gate should be showing its access screen');

      source.current = HealthConnectAvailability.available;
      now = now.add(const Duration(minutes: 5));
      await resume(tester);

      expect(find.byType(DayNavigator), findsOneWidget);
      expect(find.byType(SummaryRingCard), findsNWidgets(2));
      expect(useCase.calls, greaterThan(0),
          reason: 'the recovered dashboard must have actually read its day');
    });
  });

  testWidgets('a permission granted outside the app is picked up on the next '
      'app open', (tester) async {
    await atClock(() async {
      await pump(
        tester,
        availability: HealthConnectAvailability.available,
        granted: const <String>{},
      );
      expect(find.text('Set up your health data'), findsOneWidget);

      // Granted in the Health Connect app while OpenVitals was backgrounded.
      source.granted = _minimumPermissions;
      now = now.add(const Duration(minutes: 5));
      await resume(tester);

      // The promo card is the dashboard's rendering of "the minimum permissions
      // were never granted". Its disappearance is the granted set having been
      // re-resolved, which nothing used to do outside an explicit tap.
      expect(find.text('Set up your health data'), findsNothing);
    });
  });

  testWidgets('returning to the foreground re-reads the day', (tester) async {
    await atClock(() async {
      await pump(tester, availability: HealthConnectAvailability.available);
      final before = useCase.calls;
      expect(before, greaterThan(0));

      now = now.add(const Duration(minutes: 5));
      await resume(tester);

      expect(useCase.calls, greaterThan(before),
          reason: 'opening the app is the first of the three refresh triggers');
    });
  });

  testWidgets('the history caches drain after the app-open read settles, not '
      'alongside it', (tester) async {
    await atClock(() async {
      await pump(tester, availability: HealthConnectAvailability.available);
      // Cold start is not an app open: the drains stay owned by their screens
      // until something says the data may have moved.
      expect(scheduler.drains, 0);

      now = now.add(const Duration(minutes: 5));
      await resume(tester);

      // Health Connect serializes concurrent reads, so the drain waits for the
      // dashboard's own load to finish rather than racing it.
      expect(scheduler.drains, 1);
      expect(useCase.calls, greaterThan(1),
          reason: 'the foreground read must have run first');
    });
  });

  testWidgets('a resume inside the guard interval does not re-read',
      (tester) async {
    await atClock(() async {
      await pump(tester, availability: HealthConnectAvailability.available);
      now = now.add(const Duration(minutes: 5));
      await resume(tester);
      final after = useCase.calls;

      now = now.add(const Duration(seconds: 2));
      await resume(tester);

      expect(useCase.calls, after,
          reason: 'a two-second app switch must not re-run the whole load');
    });
  });
}
