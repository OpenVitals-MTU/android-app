import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_form.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_state.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_types.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/activity_route_section.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Covers the two behaviours that only appear once a route is attached: the
/// activity-type selector narrows to GPS-capable types, and the route section
/// renders its summary and average metrics.
void main() {
  final start = DateTime.utc(2026, 7, 9, 8);

  ExerciseRoutePoint point(int minute) => ExerciseRoutePoint(
        time: start.add(Duration(minutes: minute)),
        latitude: 59.0 + minute * 0.001,
        longitude: 24.0,
        altitudeMeters: 10,
        horizontalAccuracyMeters: null,
        verticalAccuracyMeters: null,
      );

  RouteFileImport route({
    Duration duration = const Duration(minutes: 30),
    double distanceMeters = 5000,
  }) =>
      RouteFileImport(
        fileName: 'morning.gpx',
        name: 'Morning run',
        points: [point(0), point(10), point(20)],
        distanceMeters: distanceMeters,
        elevationGainedMeters: 42,
        startTime: start,
        endTime: start.add(duration),
      );

  UnitFormatter formatter([UnitSystem system = UnitSystem.metric]) =>
      UnitFormatter(unitSystemProvider: () => system);

  group('routeMovingDurationMs', () {
    test('is the full span when nothing was paused', () {
      expect(
        routeMovingDurationMs(route(), const []),
        const Duration(minutes: 30).inMilliseconds,
      );
    });

    test('subtracts every pause', () {
      final pauses = [
        ActivityPauseInterval(
          startTime: start.add(const Duration(minutes: 5)),
          endTime: start.add(const Duration(minutes: 9)),
        ),
        ActivityPauseInterval(
          startTime: start.add(const Duration(minutes: 20)),
          endTime: start.add(const Duration(minutes: 21)),
        ),
      ];
      expect(
        routeMovingDurationMs(route(), pauses),
        const Duration(minutes: 25).inMilliseconds,
      );
    });

    test('never goes negative when the pauses exceed the span', () {
      final pauses = [
        ActivityPauseInterval(
          startTime: start,
          endTime: start.add(const Duration(hours: 5)),
        ),
      ];
      expect(routeMovingDurationMs(route(), pauses), 0);
    });
  });

  group('routeAverageMetrics', () {
    test('is null when the route has no moving time left', () {
      final metrics = routeAverageMetrics(
        route: route(duration: Duration.zero),
        pauseIntervals: const [],
        unitFormatter: formatter(),
      );
      expect(metrics, isNull);
    });

    test('reports pace and speed over the moving time only', () {
      // 5 km in 30 min = 6:00 /km. Pausing 10 min leaves 20 min → 4:00 /km.
      final moving = routeAverageMetrics(
        route: route(),
        pauseIntervals: [
          ActivityPauseInterval(
            startTime: start.add(const Duration(minutes: 5)),
            endTime: start.add(const Duration(minutes: 15)),
          ),
        ],
        unitFormatter: formatter(),
      );
      expect(moving!.averagePace, contains('4:00'));

      final unpaused = routeAverageMetrics(
        route: route(),
        pauseIntervals: const [],
        unitFormatter: formatter(),
      );
      expect(unpaused!.averagePace, contains('6:00'));
    });
  });

  group('ActivityEntryCard with an imported route', () {
    late ActivityEntryTextControllers controllers;

    setUp(() => controllers = ActivityEntryTextControllers());
    tearDown(() => controllers.dispose());

    ActivityEntryUiState stateWithRoute() => ActivityEntryUiState(
          mode: ActivityEntryFormMode.routeImport,
          selectedActivityType: defaultActivityEntryTypes
              .firstWhere((type) => type.supportsGpsRoute),
          canWrite: true,
          isCheckingPermission: false,
          importedRoute: route(),
        );

    Future<void> pumpCard(WidgetTester tester, ActivityEntryUiState state) async {
      tester.view.physicalSize = const Size(1000, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        // ProviderScope: the route preview map reads the offline map library
        // (which resolves to "no active pack" in tests).
        ProviderScope(
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            home: Scaffold(
              body: SingleChildScrollView(
                child: ActivityEntryCard(
                  state: state,
                  unitFormatter: formatter(),
                  controllers: controllers,
                  callbacks: _noopCallbacks(),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump();
    }

    testWidgets('narrows the type selector to GPS-capable types',
        (tester) async {
      // Guards the fixture: the assertion below is vacuous if every type
      // supported a GPS route.
      final routeless = defaultActivityEntryTypes
          .firstWhere((type) => !type.supportsGpsRoute);

      await pumpCard(tester, stateWithRoute());
      await tester.tap(find.byType(DropdownButtonFormField<ActivityEntryType>));
      await tester.pumpAndSettle();

      expect(find.text('Running'), findsWidgets);
      expect(find.text(routeless.label), findsNothing);
    });

    testWidgets('offers every type when no route is attached', (tester) async {
      final routeless = defaultActivityEntryTypes
          .firstWhere((type) => !type.supportsGpsRoute);

      await pumpCard(
        tester,
        ActivityEntryUiState(
          mode: ActivityEntryFormMode.manual,
          selectedActivityType: defaultActivityEntryTypes.first,
          canWrite: true,
          isCheckingPermission: false,
        ),
      );
      await tester.tap(find.byType(DropdownButtonFormField<ActivityEntryType>));
      await tester.pumpAndSettle();

      expect(find.text(routeless.label), findsWidgets);
    });

    testWidgets('renders the route summary and its average metrics',
        (tester) async {
      await pumpCard(tester, stateWithRoute());

      expect(find.textContaining('Morning run'), findsOneWidget);
      expect(find.textContaining('3 points'), findsOneWidget);
      expect(find.textContaining('Avg pace'), findsOneWidget);
    });

    testWidgets('renders no route section when no route is attached',
        (tester) async {
      await pumpCard(
        tester,
        ActivityEntryUiState(
          mode: ActivityEntryFormMode.manual,
          selectedActivityType: defaultActivityEntryTypes.first,
          canWrite: true,
          isCheckingPermission: false,
        ),
      );

      expect(find.text('Imported route'), findsNothing);
      expect(find.textContaining('Avg pace'), findsNothing);
    });
  });
}

ActivityEntryCardCallbacks _noopCallbacks() => ActivityEntryCardCallbacks(
      onSelectActivityType: (_) {},
      onTitleChanged: (_) {},
      onFeelingChanged: (_) {},
      onNotesChanged: (_) {},
      onStartDateChanged: (_) {},
      onStartTimeChanged: (_) {},
      onDurationChanged: (_) {},
      onRepetitionModeChanged: (_) {},
      onRepetitionTotalChanged: (_) {},
      onRepetitionSetRepetitionsChanged: (_, _) {},
      onRepetitionSetRestChanged: (_, _) {},
      onAddRepetitionSet: () {},
      onRemoveRepetitionSet: (_) {},
      onCreateNewPlannedWorkout: () {},
      onApplyPlannedWorkout: (_) {},
      onSavePlannedWorkout: () {},
      onUpdatePlannedWorkout: () {},
      onDistanceChanged: (_) {},
      onElevationChanged: (_) {},
      onActiveCaloriesChanged: (_) {},
      onTotalCaloriesChanged: (_) {},
      onClearRoute: () {},
      onChooseSource: () {},
      onRequestWritePermission: () {},
      onAddEntry: () {},
      onDiscardRecordingDraft: () {},
    );
