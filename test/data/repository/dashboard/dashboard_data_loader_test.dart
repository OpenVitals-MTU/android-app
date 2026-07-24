import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/data/repository/contract/caffeine_repository.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/insights/caffeine_insight_calculator.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';

/// A device-free [HealthDataSource] that returns canned values for a few metrics
/// and reports a fixed granted-permission set. All other reads fall through to
/// the base class's empty/null defaults.
class _FakeSource extends HealthDataSource {
  _FakeSource(this._granted) {
    cachedAvailability = HealthConnectAvailability.available;
  }

  final Set<String> _granted;

  @override
  Future<Set<String>> grantedPermissions() async => _granted;

  @override
  Future<int> readSteps(LocalDate date) async => 4321;

  @override
  Future<double> readDistanceMeters(LocalDate date) async => 5000.0;

  @override
  Future<double?> readHydrationLiters(LocalDate date) async => 2.5;
}

/// A [BodyEnergyRepository] that returns a single canned day, and records
/// whether it was asked to load at all.
class _FakeBodyEnergyRepository implements BodyEnergyRepository {
  _FakeBodyEnergyRepository(this._day);

  final BodyEnergyTimeline _day;
  bool loaded = false;

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async {
    loaded = true;
    return Ok(BodyEnergyTimelineResult(query: query, days: [_day]));
  }
}

BodyEnergyTimeline _timeline(LocalDate date) => BodyEnergyTimeline(
      date: date,
      startScore: 60,
      currentScore: 74,
      charged: 30,
      drained: 16,
      points: const [],
      confidence: BodyEnergyConfidence.medium,
      confidenceReason: '',
    );

Future<PreferencesRepository> _prefsWithCalibration(
    {required bool setupCompleted}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = PreferencesRepository(await SharedPreferences.getInstance());
  prefs.setBodyEnergyCalibration(
    BodyEnergyCalibration(setupCompleted: setupCompleted),
  );
  return prefs;
}

/// A [CaffeineRepository] returning canned entries; records whether it was
/// asked to load at all.
class _FakeCaffeineRepository
    with CaffeineRepositoryDefaults
    implements CaffeineRepository {
  _FakeCaffeineRepository(this._entries);

  final List<CaffeineEntry> _entries;
  bool loaded = false;

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    loaded = true;
    return Ok(CaffeinePeriodData(entries: _entries));
  }
}

class _ThrowingCaffeineRepository
    with CaffeineRepositoryDefaults
    implements CaffeineRepository {
  @override
  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      throw StateError('caffeine read blew up');
}

CaffeineEntry _caffeineEntry(DateTime start, double mg) => CaffeineEntry(
      id: 'drink-${start.millisecondsSinceEpoch}',
      startTime: start,
      endTime: start.add(const Duration(minutes: 10)),
      caffeineMg: mg,
      name: 'Coffee',
      source: 'test.source',
      mealType: 0,
    );

/// A source whose reads all park on one gate, so a test can observe how many
/// run concurrently. Used to assert the loader bounds its metric-read fan-out.
class _GatedSource extends HealthDataSource {
  _GatedSource(this._granted) {
    cachedAvailability = HealthConnectAvailability.available;
  }

  final Set<String> _granted;
  final Completer<void> _gate = Completer<void>();
  int inFlight = 0;
  int maxInFlight = 0;
  int completed = 0;

  Future<T> _gated<T>(T value) async {
    inFlight++;
    if (inFlight > maxInFlight) maxInFlight = inFlight;
    await _gate.future;
    inFlight--;
    completed++;
    return value;
  }

  void release() => _gate.complete();

  @override
  Future<Set<String>> grantedPermissions() async => _granted;

  @override
  Future<int> readSteps(LocalDate date) => _gated(1);
  @override
  Future<double> readDistanceMeters(LocalDate date) => _gated(1.0);
  @override
  Future<int> readFloorsClimbed(LocalDate date) => _gated(1);
  @override
  Future<double?> readElevationGained(LocalDate date) => _gated(1.0);
  @override
  Future<int?> readWheelchairPushes(LocalDate date) => _gated(1);
  @override
  Future<double?> readCaloriesInKcal(LocalDate date) => _gated(1.0);
  @override
  Future<double?> readHydrationLiters(LocalDate date) => _gated(1.0);
  @override
  Future<int?> readAvgHeartRate(LocalDate date) => _gated(1);
  @override
  Future<int?> readRestingHeartRate(LocalDate date) => _gated(1);
  @override
  Future<int> readMindfulnessMinutes(LocalDate date) => _gated(1);
}

void main() {
  test('assembles granted metrics and reports missing permissions', () async {
    final source = _FakeSource({
      HcPermissions.readSteps,
      HcPermissions.readDistance,
      // hydration read permission intentionally NOT granted
    });
    final loader = DashboardDataLoader(source);

    final data = (await loader.loadDashboard(
      DashboardQuery(
        date: LocalDate(2026, 1, 2),
        visibleMetrics: {
          DashboardMetric.steps,
          DashboardMetric.distance,
          DashboardMetric.hydration,
        },
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    ))
        .orThrow();

    expect(data.steps, 4321);
    expect(data.distanceMeters, 5000.0);
    // Not granted → the metric read is skipped and the value stays at 0.
    expect(data.hydrationLiters, 0.0);
    expect(data.missingPermissions, contains(HcPermissions.readHydration));
    expect(data.missingPermissions, isNot(contains(HcPermissions.readSteps)));
    expect(
      data.loadedMetrics,
      {DashboardMetric.steps, DashboardMetric.distance, DashboardMetric.hydration},
    );
  });

  group('active caffeine (point-in-time decaying quantity)', () {
    DashboardQuery query(LocalDate date) => DashboardQuery(
          date: date,
          visibleMetrics: {DashboardMetric.caffeine},
          includeHistoricalBaselines: false,
          includeWeeklyTrainingSignals: false,
        );

    test('morning carryover from last night is reported for today', () async {
      final repo = _FakeCaffeineRepository([
        _caffeineEntry(DateTime.now().subtract(const Duration(hours: 8)), 150),
      ]);
      final loader = DashboardDataLoader(
        _FakeSource({HcPermissions.readNutrition}),
        caffeineRepository: repo,
      );

      final data =
          (await loader.loadDashboard(query(LocalDate.now()))).orThrow();

      expect(repo.loaded, isTrue);
      expect(data.activeCaffeineMg, isNotNull);
      expect(data.activeCaffeineMg, greaterThan(0.0));
    });

    test('matches the caffeine screen\'s currentMg for the same inputs',
        () async {
      final entries = [
        _caffeineEntry(DateTime.now().subtract(const Duration(hours: 8)), 150),
        _caffeineEntry(DateTime.now().subtract(const Duration(hours: 2)), 80),
      ];
      final loader = DashboardDataLoader(
        _FakeSource({HcPermissions.readNutrition}),
        caffeineRepository: _FakeCaffeineRepository(entries),
      );

      final data =
          (await loader.loadDashboard(query(LocalDate.now()))).orThrow();
      final today = LocalDate.now();
      final screenCurrentMg = CaffeineInsightCalculator.build(
        entries: entries,
        period: DatePeriod(today, today),
        preferences: const CaffeinePreferences(),
        now: DateTime.now().toUtc(),
      ).currentMg;

      // Active caffeine decays by well under 0.1 mg between the two "now"
      // samples, so the tile and the detail screen agree.
      expect(data.activeCaffeineMg, closeTo(screenCurrentMg, 0.1));
    });

    test('a past day keeps intake semantics: no PK read at all', () async {
      final repo = _FakeCaffeineRepository([
        _caffeineEntry(DateTime.now().subtract(const Duration(hours: 8)), 150),
      ]);
      final loader = DashboardDataLoader(
        _FakeSource({HcPermissions.readNutrition}),
        caffeineRepository: repo,
      );

      final data = (await loader
              .loadDashboard(query(LocalDate.now().minusDays(1))))
          .orThrow();

      expect(repo.loaded, isFalse);
      expect(data.activeCaffeineMg, isNull);
    });

    test('hidden metric or missing permission skips the read', () async {
      final repo = _FakeCaffeineRepository([
        _caffeineEntry(DateTime.now().subtract(const Duration(hours: 8)), 150),
      ]);

      final hidden = DashboardDataLoader(
        _FakeSource({HcPermissions.readNutrition}),
        caffeineRepository: repo,
      );
      await hidden.loadDashboard(DashboardQuery(
        date: LocalDate.now(),
        visibleMetrics: {DashboardMetric.steps},
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ));
      expect(repo.loaded, isFalse);

      final ungranted = DashboardDataLoader(
        _FakeSource(const {}),
        caffeineRepository: repo,
      );
      final data =
          (await ungranted.loadDashboard(query(LocalDate.now()))).orThrow();
      expect(repo.loaded, isFalse);
      expect(data.activeCaffeineMg, isNull);
    });

    test('a throwing caffeine read nulls the field, not the dashboard',
        () async {
      final loader = DashboardDataLoader(
        _FakeSource({HcPermissions.readNutrition, HcPermissions.readSteps}),
        caffeineRepository: _ThrowingCaffeineRepository(),
      );

      final data = (await loader.loadDashboard(DashboardQuery(
        date: LocalDate.now(),
        visibleMetrics: {DashboardMetric.caffeine, DashboardMetric.steps},
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      )))
          .orThrow();

      expect(data.activeCaffeineMg, isNull);
      expect(data.steps, 4321);
    });

    test('mergeLoaded carries activeCaffeineMg across the two-pass load', () {
      final date = LocalDate(2026, 7, 24);
      final first =
          DashboardData(date: date, loadedMetrics: const {DashboardMetric.steps});
      final second = DashboardData(
        date: date,
        activeCaffeineMg: 21.0,
        loadedMetrics: const {DashboardMetric.caffeine},
      );
      expect(first.mergeLoaded(second).activeCaffeineMg, 21.0);
    });
  });

  test('omits permissions the installed provider cannot grant', () async {
    // The provider does not define WHEELCHAIR_PUSHES (the app's connect-client
    // is newer than it), and the mindfulness feature is unavailable — the
    // default feature flags. Neither permission can ever be granted, and
    // `grantedPermissions()` cannot even report them, so requiring them would
    // strand the dashboard's permission callout on an ungrantable set.
    final source = _FakeSource({HcPermissions.readSteps})
      ..unsupportedPermissions = {HcPermissions.readWheelchairPushes};
    final loader = DashboardDataLoader(source);

    final data = (await loader.loadDashboard(
      DashboardQuery(
        date: LocalDate(2026, 1, 2),
        visibleMetrics: {
          DashboardMetric.steps,
          DashboardMetric.wheelchairPushes,
          DashboardMetric.mindfulness,
        },
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    ))
        .orThrow();

    expect(data.missingPermissions, isEmpty);
  });

  test('supportedMetrics drops metrics the provider cannot serve', () async {
    final source = _FakeSource(const <String>{})
      ..unsupportedPermissions = {
        HcPermissions.readWheelchairPushes,
        HcPermissions.readBloodGlucose,
      };
    final loader = DashboardDataLoader(source);

    final data = (await loader.loadDashboard(
      DashboardQuery(
        date: LocalDate(2026, 1, 2),
        visibleMetrics: {DashboardMetric.steps},
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    ))
        .orThrow();

    // Reported for every metric, not just the queried ones — the dashboard uses
    // it to decide which tiles exist at all.
    expect(data.supportedMetrics, contains(DashboardMetric.steps));
    expect(data.supportedMetrics, contains(DashboardMetric.distance));
    expect(
      data.supportedMetrics,
      isNot(contains(DashboardMetric.wheelchairPushes)),
    );
    expect(data.supportedMetrics, isNot(contains(DashboardMetric.bloodGlucose)));
    // Feature-flagged permissions are unsupported by default.
    expect(
      data.supportedMetrics,
      isNot(contains(DashboardMetric.skinTemperature)),
    );
    expect(data.supportedMetrics, isNot(contains(DashboardMetric.mindfulness)));
  });

  test('a multi-permission metric needs all of its permissions supported',
      () async {
    // BMI reads weight + height; dropping height alone must unsupport it.
    final source = _FakeSource(const <String>{})
      ..unsupportedPermissions = {HcPermissions.readHeight};
    final loader = DashboardDataLoader(source);

    final data = (await loader.loadDashboard(
      DashboardQuery(
        date: LocalDate(2026, 1, 2),
        visibleMetrics: {DashboardMetric.steps},
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    ))
        .orThrow();

    expect(data.supportedMetrics, contains(DashboardMetric.weight));
    expect(data.supportedMetrics, isNot(contains(DashboardMetric.height)));
    expect(data.supportedMetrics, isNot(contains(DashboardMetric.bmi)));
    expect(data.supportedMetrics, isNot(contains(DashboardMetric.ffmi)));
  });

  test('returns empty granted set when Health Connect is unavailable', () async {
    final source = _FakeSource({HcPermissions.readSteps})
      ..cachedAvailability = HealthConnectAvailability.notSupported;
    final loader = DashboardDataLoader(source);

    final data = (await loader.loadDashboard(
      DashboardQuery(
        date: LocalDate(2026, 1, 2),
        visibleMetrics: {DashboardMetric.steps},
        includeHistoricalBaselines: false,
        includeWeeklyTrainingSignals: false,
      ),
    ))
        .orThrow();

    // Availability gate short-circuits granted permissions, so nothing is read.
    expect(data.steps, 0);
    expect(data.missingPermissions, contains(HcPermissions.readSteps));
  });

  group('body energy timeline', () {
    final date = LocalDate(2026, 1, 2);

    Future<Result<DashboardData>> load(DashboardDataLoader loader) =>
        loader.loadDashboard(
          DashboardQuery(
            date: date,
            visibleMetrics: {DashboardMetric.bodyEnergy},
            includeHistoricalBaselines: false,
            includeWeeklyTrainingSignals: false,
          ),
        );

    test('populates the timeline when set up and heart-rate is granted',
        () async {
      final source = _FakeSource({HcPermissions.readHeartRate});
      final repo = _FakeBodyEnergyRepository(_timeline(date));
      final loader = DashboardDataLoader(
        source,
        preferencesRepository: await _prefsWithCalibration(setupCompleted: true),
        bodyEnergyRepository: repo,
      );

      final data = (await load(loader)).orThrow();

      expect(repo.loaded, isTrue);
      expect(data.bodyEnergyTimeline, isNotNull);
      expect(data.bodyEnergyTimeline!.currentScore, 74);
      expect(data.bodyEnergyTimeline!.startScore, 60);
    });

    test('skips the load when calibration is not set up', () async {
      final source = _FakeSource({HcPermissions.readHeartRate});
      final repo = _FakeBodyEnergyRepository(_timeline(date));
      final loader = DashboardDataLoader(
        source,
        preferencesRepository:
            await _prefsWithCalibration(setupCompleted: false),
        bodyEnergyRepository: repo,
      );

      final data = (await load(loader)).orThrow();

      expect(repo.loaded, isFalse);
      expect(data.bodyEnergyTimeline, isNull);
    });

    test('skips the load when heart-rate read is not granted', () async {
      final source = _FakeSource(const <String>{});
      final repo = _FakeBodyEnergyRepository(_timeline(date));
      final loader = DashboardDataLoader(
        source,
        preferencesRepository: await _prefsWithCalibration(setupCompleted: true),
        bodyEnergyRepository: repo,
      );

      final data = (await load(loader)).orThrow();

      expect(repo.loaded, isFalse);
      expect(data.bodyEnergyTimeline, isNull);
    });
  });

  test('bounds how many metric reads run at once', () async {
    // Ten independent reads are enabled; the loader must run them as a bounded
    // wave, not all at once (unbounded fan-out swamps the binder) and not one at
    // a time (the ~35 serial round-trips this refactor removed).
    final source = _GatedSource({
      HcPermissions.readSteps,
      HcPermissions.readDistance,
      HcPermissions.readFloors,
      HcPermissions.readElevation,
      HcPermissions.readWheelchairPushes,
      HcPermissions.readNutrition,
      HcPermissions.readHydration,
      HcPermissions.readHeartRate,
      HcPermissions.readRestingHeartRate,
      HcPermissions.readMindfulness,
    });
    final loader = DashboardDataLoader(source);

    final future = loader.loadDashboard(DashboardQuery(
      date: LocalDate(2026, 1, 2),
      visibleMetrics: {
        DashboardMetric.steps,
        DashboardMetric.distance,
        DashboardMetric.floors,
        DashboardMetric.elevation,
        DashboardMetric.wheelchairPushes,
        DashboardMetric.caloriesIn,
        DashboardMetric.hydration,
        DashboardMetric.avgHeartRate,
        DashboardMetric.restingHeartRate,
        DashboardMetric.mindfulness,
      },
      includeHistoricalBaselines: false,
      includeWeeklyTrainingSignals: false,
    ));

    // Let the pool admit as many reads as it will; they park at the gate.
    await pumpEventQueue();
    // Exactly _maxConcurrentDashboardReads (8) are in flight — the other two
    // queue. A serial loader would show 1; an unbounded one would show 10.
    expect(source.maxInFlight, 8);

    source.release();
    await future;
    // Every enabled read still ran.
    expect(source.completed, 10);
  });
}
