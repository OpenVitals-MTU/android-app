// The dashboard's display is derived ONCE, in the view-model, from the loaded
// day plus the saved layout. These pin the derivation the screen's `build` used
// to do on every frame: the summary mapping, the layout order, the hidden set,
// the edit-mode expansion of unsupported metrics, the add-tray, and today's
// activities.

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/dashboard/application/dashboard_display.dart';
import 'package:openvitals/l10n/app_localizations.dart';

final UnitFormatter _formatter =
    UnitFormatter(unitSystemProvider: () => UnitSystem.metric);

DashboardData _data({
  int steps = 8000,
  Set<DashboardMetric>? supported,
  List<ExerciseData> workouts = const <ExerciseData>[],
  ExerciseData? workout,
}) =>
    DashboardData(
      date: LocalDate(2026, 1, 2),
      steps: steps,
      workout: workout,
      workouts: workouts,
      supportedMetrics: supported ?? DashboardMetric.values.toSet(),
    );

ExerciseData _exercise(String id) => ExerciseData(
      id: id,
      title: null,
      exerciseType: 1,
      startTime: DateTime.utc(2026, 1, 2, 7),
      endTime: DateTime.utc(2026, 1, 2, 8),
      durationMs: 3600000,
      source: 'test',
    );

List<String> _titles(List<StatTileData> tiles) =>
    [for (final t in tiles) t.title];

void main() {
  late AppLocalizations l10n;

  setUpAll(() async {
    l10n = await AppLocalizations.delegate.load(const Locale('en'));
  });

  DashboardDisplay build(
    DashboardData data, {
    bool editing = false,
    List<String> tileOrder = const <String>[],
    List<String> ringOrder = const <String>[],
    Set<String> hiddenTiles = const <String>{},
  }) =>
      buildDashboardDisplay(
        data,
        _formatter,
        l10n,
        goals: kDefaultDashboardGoals,
        editing: editing,
        tileOrder: tileOrder,
        ringOrder: ringOrder,
        hiddenTiles: hiddenTiles,
      );

  List<String> trayTitles(DashboardDisplay d) =>
      [for (final e in d.trayEntries) e.title];

  test('a fully-supported day maps both rings and every tile', () {
    final display = build(_data());

    expect([for (final r in display.orderedRings) r.title],
        <String>['Steps', 'Weekly cardio']);
    expect(display.visibleRings, hasLength(2));
    expect(_titles(display.visibleTiles), contains('Distance'));
    expect(display.hiddenIds, isEmpty);
    expect(display.trayEntries, isEmpty);
    expect(display.unsupportedIds, isEmpty);
  });

  test('empty data still renders the rings and the empty tiles', () {
    // No readings, no workouts, nothing supported: the device serves nothing, so
    // there are no tiles at all — but the hero rings are always there (the Kotlin
    // summary keeps them, empty).
    final display = build(
      _data(steps: 0, supported: const <DashboardMetric>{}),
    );

    expect(display.orderedTiles, isEmpty);
    expect(display.visibleTiles, isEmpty);
    expect(display.visibleRings, hasLength(2));
    expect(display.orderedRings.first.value, '0');
    expect(display.orderedRings.last.value, '—');
    expect(display.activities, isEmpty);
    expect(display.trayEntries, isEmpty);
  });

  test('the saved order and hidden set are already applied', () {
    final display = build(
      _data(),
      tileOrder: const ['Beverages', 'Distance'],
      ringOrder: const ['Weekly cardio', 'Steps'],
      hiddenTiles: const {'Distance'},
    );

    // Ordered keeps the hidden tile (the edit grid needs it); visible drops it.
    expect(_titles(display.orderedTiles).take(2), ['Beverages', 'Distance']);
    expect(_titles(display.visibleTiles), isNot(contains('Distance')));
    expect([for (final r in display.orderedRings) r.title],
        <String>['Weekly cardio', 'Steps']);
    // A removed tile is offered back in the tray.
    expect(trayTitles(display), contains('Distance'));
    // The saved layout came in as legacy TITLES and is handed back as ids, so
    // the caller can write the translated form once instead of forever.
    // 'Beverages' is the DISPLAY of the hydration metric — the exact reason a
    // title cannot be an identity.
    expect(display.migratedTileOrder, ['hydration', 'distance']);
    expect(display.migratedHiddenTiles, {'distance'});
  });

  test('a hidden hero ring leaves the row and joins the tray', () {
    final display = build(_data(), hiddenTiles: const {'Steps'});

    expect([for (final r in display.visibleRings) r.title],
        <String>['Weekly cardio']);
    expect(trayTitles(display).first, 'Steps');
    expect(display.migratedHiddenTiles, {'steps'});
  });

  test('edit mode materialises an unsupported metric into the tray, not the '
      'carousel', () {
    final supported = DashboardMetric.values.toSet()
      ..remove(DashboardMetric.spo2);

    final normal = build(_data(supported: supported));
    expect(_titles(normal.orderedTiles), isNot(contains('Blood oxygen')));
    expect(normal.unsupportedIds, isEmpty);

    final editing = build(_data(supported: supported), editing: true);
    // Materialised, but treated as hidden until the user deliberately places it.
    expect(_titles(editing.orderedTiles), contains('Blood oxygen'));
    expect(_titles(editing.visibleTiles), isNot(contains('Blood oxygen')));
    expect(editing.unsupportedIds, contains(DashboardMetric.spo2.name));
    expect(editing.hiddenIds, contains(DashboardMetric.spo2.name));
    expect(trayTitles(editing), contains('Blood oxygen'));

    // Recording it in the tile order is what marks it as placed.
    final placed = build(
      _data(supported: supported),
      editing: true,
      tileOrder: const ['Blood oxygen'],
    );
    expect(_titles(placed.visibleTiles), contains('Blood oxygen'));
    expect(trayTitles(placed), isNot(contains('Blood oxygen')));
  });

  test('the goals reach the ring, not the defaults', () {
    final display = buildDashboardDisplay(
      _data(steps: 3000),
      _formatter,
      l10n,
      goals: const DashboardGoals(
        steps: 6000,
        distanceMeters: 5000,
        caloriesOutKcal: 2000,
        activeCaloriesKcal: 400,
        floors: 10,
        elevationMeters: 100,
        wheelchairPushes: 1000,
        sleepHours: 8,
        hydrationLiters: 2,
        caloriesInKcal: 2000,
        proteinGrams: 50,
        carbsGrams: 275,
        fatGrams: 70,
        mindfulnessMinutes: 10,
      ),
    );

    final steps = display.orderedRings.firstWhere((r) => r.title == 'Steps');
    expect(steps.subtitle, 'steps of 6,000');
    expect(steps.progress, closeTo(3000 / 6000, 1e-9));
  });

  group('activities', () {
    test('the workout list wins when it has entries', () {
      final display = build(
        _data(workouts: [_exercise('a'), _exercise('b')], workout: _exercise('c')),
      );
      expect([for (final w in display.activities) w.id], ['a', 'b']);
    });

    test('a lone workout is the fallback', () {
      final display = build(_data(workout: _exercise('c')));
      expect([for (final w in display.activities) w.id], ['c']);
    });
  });
}
