import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/repository_exceptions.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_clock.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_notifier.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_state.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_write_request_builder.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_draft_store.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/route_file_parser.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Port of the Kotlin `ActivityEntryViewModelTest`.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const activityWritePermissions = {
    'write_activity',
    'write_route',
    'write_distance',
    'write_elevation',
    'write_active_calories',
    'write_total_calories',
  };
  const plannedWorkoutWritePermissions = {'read_planned', 'write_planned'};

  ActivityEntryType typeById(String id) =>
      defaultActivityEntryTypes.firstWhere((t) => t.id == id);
  ActivityEntryType typeByExercise(int exerciseType) =>
      defaultActivityEntryTypes.firstWhere((t) => t.exerciseType == exerciseType);

  ExerciseRoutePoint routePoint(
    DateTime time, {
    double latitude = 59.0,
    double longitude = 24.0,
  }) =>
      ExerciseRoutePoint(
        time: time,
        latitude: latitude,
        longitude: longitude,
        altitudeMeters: 10.0,
        horizontalAccuracyMeters: null,
        verticalAccuracyMeters: null,
      );

  ActivityEntryClock clockAt(String iso) =>
      ActivityEntryClock.fixedUtc(DateTime.parse(iso));

  Future<PreferencesRepository> makePrefs({
    int? favorite,
    int? last,
  }) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (favorite != null) prefs.favoriteActivityExerciseType = favorite;
    if (last != null) prefs.lastActivityExerciseType = last;
    return prefs;
  }

  _FakeActivityRepository repo({
    bool canWrite = true,
    List<PlannedExerciseData> plannedWorkouts = const [],
    ExerciseData? workout,
    bool canReadPlans = true,
    bool canWritePlan = true,
  }) =>
      _FakeActivityRepository(
        canWriteValue: canWrite,
        plannedWorkouts: plannedWorkouts,
        workout: workout,
        canReadPlans: canReadPlans,
        canWritePlan: canWritePlan,
        activityPermissions: activityWritePermissions,
        plannedPermissions: plannedWorkoutWritePermissions,
      );

  PlannedExerciseData plannedPushUpPlan() => PlannedExerciseData(
        id: 'planned-push-id',
        title: 'Push-up pyramid',
        exerciseType: ExerciseSessionType.calisthenics,
        startTime: DateTime.utc(2026, 5, 26, 9, 30),
        endTime: DateTime.utc(2026, 5, 26, 9, 35),
        hasExplicitTime: true,
        completedExerciseSessionId: null,
        notes: 'Slow tempo',
        blockCount: 1,
        source: 'tech.mmarca.openvitals',
        blocks: [
          PlannedExerciseBlockData(
            repetitions: 1,
            description: 'Main set',
            steps: [
              PlannedExerciseStepData(
                exerciseType: ExerciseSegmentType.otherWorkout,
                exercisePhase: PlannedExerciseStepPhase.active,
                description: 'Set 1',
                completion: const PlannedExerciseCompletionRepetitions(12),
              ),
            ],
          ),
        ],
      );

  PlannedExerciseData plannedPullUpPlan() => PlannedExerciseData(
        id: 'planned-id',
        title: 'Pull-up ladder',
        exerciseType: ExerciseSessionType.calisthenics,
        startTime: DateTime.utc(2026, 5, 26, 8, 30),
        endTime: DateTime.utc(2026, 5, 26, 8, 35),
        hasExplicitTime: true,
        completedExerciseSessionId: null,
        notes: 'Strict reps',
        blockCount: 1,
        source: 'tech.mmarca.openvitals',
        blocks: [
          PlannedExerciseBlockData(
            repetitions: 1,
            description: 'Main set',
            steps: [
              PlannedExerciseStepData(
                exerciseType: ExerciseSegmentType.pullUp,
                exercisePhase: PlannedExerciseStepPhase.active,
                description: 'Set 1',
                completion: const PlannedExerciseCompletionRepetitions(8),
              ),
              PlannedExerciseStepData(
                exerciseType: ExerciseSegmentType.rest,
                exercisePhase: PlannedExerciseStepPhase.rest,
                description: 'Rest',
                completion: const PlannedExerciseCompletionDurationSeconds(60),
              ),
              PlannedExerciseStepData(
                exerciseType: ExerciseSegmentType.pullUp,
                exercisePhase: PlannedExerciseStepPhase.active,
                description: 'Set 2',
                completion: const PlannedExerciseCompletionRepetitions(6),
              ),
            ],
          ),
        ],
      );

  // ── Pure buildWriteRequest / validate ────────────────────────────────────

  test('buildWriteRequest converts metric distance and trims text', () {
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      titleText: '  Morning run  ',
      notesText: '  Easy effort  ',
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '45',
      distanceText: '10.5',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;

    expect(request.exerciseType, ExerciseSessionType.running);
    expect(request.title, 'Morning run');
    expect(request.notes, 'Easy effort');
    expect(request.distanceMeters ?? 0.0, closeTo(10500.0, 0.001));
    expect(request.startTime.isBefore(request.endTime), isTrue);
  });

  test('buildWriteRequest combines selected feeling and notes', () {
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      selectedFeeling: ActivityEntryFeeling.good,
      notesText: '  Kept the last mile steady.  ',
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '45',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.notes, 'Felt good.\n\nKept the last mile steady.');
  });

  test('buildWriteRequest ignores hidden unsupported metric values', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('push_ups'),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '30',
      distanceText: '10.5',
      elevationText: '120',
      repetitionTotalText: '25',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.distanceMeters, isNull);
    expect(request.elevationGainedMeters, isNull);
  });

  test('buildWriteRequest rejects total calories below active calories', () {
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '45',
      activeCaloriesText: '500',
      totalCaloriesText: '300',
    );
    expect(buildWriteRequest(state, UnitSystem.metric), isNull);
  });

  test('validateActivityEntry returns field specific errors', () {
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      startDateText: '',
      startTimeText: '25:99',
      durationMinutesText: '0',
      distanceText: '-1',
      activeCaloriesText: 'abc',
      totalCaloriesText: '0',
    );

    final errors = validateActivityEntry(state, UnitSystem.metric);
    expect(errors, contains(ActivityEntryValidationError.startDateInvalid));
    expect(errors, contains(ActivityEntryValidationError.startTimeInvalid));
    expect(errors, contains(ActivityEntryValidationError.durationInvalid));
    expect(errors, contains(ActivityEntryValidationError.distanceInvalid));
    expect(errors, contains(ActivityEntryValidationError.activeCaloriesInvalid));
    expect(errors, contains(ActivityEntryValidationError.totalCaloriesInvalid));
  });

  test('buildWriteRequest uses imported route distance and adjusts end after last point',
      () {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final last = DateTime.utc(2026, 5, 26, 9, 0);
    final route = RouteFileImport(
      fileName: 'run.gpx',
      points: [routePoint(start), routePoint(last, latitude: 59.01)],
      distanceMeters: 1200.0,
      elevationGainedMeters: 12.0,
      startTime: start,
      endTime: last,
    );
    final localStart = start.toLocal();
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      startDateText:
          '${localStart.year}-${_p2(localStart.month)}-${_p2(localStart.day)}',
      startTimeText: '${localStart.hour}:${_p2(localStart.minute)}',
      durationMinutesText: '30',
      importedRoute: route,
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.routePoints.length, 2);
    expect(request.distanceMeters ?? 0.0, closeTo(1200.0, 0.001));
    expect(request.elevationGainedMeters ?? 0.0, closeTo(12.0, 0.001));
    expect(last.isBefore(request.endTime), isTrue);
  });

  test('buildWriteRequest retimes imported route without recorded timestamps', () {
    final originalStart = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
    final originalLast = originalStart.add(const Duration(seconds: 20));
    final route = RouteFileImport(
      fileName: 'route.kml',
      points: [routePoint(originalStart), routePoint(originalLast, latitude: 59.01)],
      distanceMeters: 1200.0,
      elevationGainedMeters: 12.0,
      startTime: originalStart,
      endTime: originalLast,
      hasRecordedTimestamps: false,
      hasImportedTimeRange: false,
    );
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '30',
      importedRoute: route,
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    final expectedStart = DateTime(2026, 5, 26, 8, 30);
    expect(request.routePoints.first.time, expectedStart);
    expect(request.routePoints.last.time.isBefore(request.endTime), isTrue);
    expect(request.routePoints.first.time != originalStart, isTrue);
  });

  test('buildWriteRequest includes recorded pause intervals inside session', () {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final pauseStart = start.add(const Duration(seconds: 600));
    final pauseEnd = start.add(const Duration(seconds: 900));
    final local = start.toLocal();
    final state = ActivityEntryUiState(
      selectedActivityType: defaultActivityEntryTypes.first,
      startDateText: '${local.year}-${_p2(local.month)}-${_p2(local.day)}',
      startTimeText: '${local.hour}:${_p2(local.minute)}',
      durationMinutesText: '45',
      recordedPauseIntervals: [
        ActivityPauseInterval(startTime: pauseStart, endTime: pauseEnd),
      ],
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.pauseIntervals.length, 1);
    expect(request.pauseIntervals.first.startTime, pauseStart);
    expect(request.pauseIntervals.first.endTime, pauseEnd);
  });

  test('buildWriteRequest ignores recorded GPS metadata for non GPS activity', () {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final pauseStart = start.add(const Duration(seconds: 600));
    final pauseEnd = start.add(const Duration(seconds: 900));
    final local = start.toLocal();
    final state = ActivityEntryUiState(
      selectedActivityType: typeByExercise(ExerciseSessionType.otherWorkout),
      startDateText: '${local.year}-${_p2(local.month)}-${_p2(local.day)}',
      startTimeText: '${local.hour}:${_p2(local.minute)}',
      durationMinutesText: '45',
      recordedPauseIntervals: [
        ActivityPauseInterval(startTime: pauseStart, endTime: pauseEnd),
      ],
      recordedLaps: [
        ExerciseLapData(
            startTime: pauseStart, endTime: pauseEnd, lengthMeters: 100.0),
      ],
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.routePoints, isEmpty);
    expect(request.pauseIntervals, isEmpty);
    expect(request.laps, isEmpty);
  });

  test('buildWriteRequest keeps BLE heart rate samples for strength training', () {
    final strengthTraining = typeByExercise(ExerciseSessionType.strengthTraining);
    final sampleTime = DateTime.utc(2026, 5, 26, 8, 35);
    final state = ActivityEntryUiState(
      selectedActivityType: strengthTraining,
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '30',
      recordedBleSamples: const BleRecordingSampleBuffer()
          .withHeartRateSample(sampleTime, 132)
          .withHeartRateSample(sampleTime.add(const Duration(seconds: 30)), 140),
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseType, ExerciseSessionType.strengthTraining);
    expect(strengthTraining.supportsLiveRecording, isTrue);
    expect(strengthTraining.supportsGpsRoute, isFalse);
    expect(strengthTraining.isRepetitionLike, isFalse);
    expect(request.routePoints, isEmpty);
    expect(request.exerciseSegments, isEmpty);
    expect(request.bleSamples.heartRateSamples.length, 2);
    expect(request.bleSamples.heartRateSamples.first.beatsPerMinute, 132);
  });

  test('buildWriteRequest writes total push-ups as one set segment', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('push_ups'),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '10',
      repetitionTotalText: '25',
      recordedPauseIntervals: [
        ActivityPauseInterval(
          startTime: DateTime.utc(2026, 5, 26, 8, 35),
          endTime: DateTime.utc(2026, 5, 26, 8, 36),
        ),
      ],
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseType, ExerciseSessionType.calisthenics);
    expect(request.title, 'Push-ups');
    expect(request.exerciseSegments.length, 1);
    expect(request.exerciseSegments.first.segmentType,
        ExerciseSegmentType.otherWorkout);
    expect(request.exerciseSegments.first.repetitions, 25);
    expect(request.exerciseSegments.first.setIndex, 0);
    expect(request.pauseIntervals, isEmpty);
    expect(request.laps, isEmpty);
  });

  test('buildWriteRequest writes repetition sets and rest segments', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('pull_ups'),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '5',
      repetitionMode: ActivityRepetitionEntryMode.sets,
      repetitionSets: const [
        ActivityRepetitionSetInput(repetitionsText: '8', restMinutesText: '1'),
        ActivityRepetitionSetInput(repetitionsText: '6'),
      ],
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseSegments.length, 3);
    expect(request.exerciseSegments[0].repetitions, 8);
    expect(request.exerciseSegments[0].setIndex, 0);
    expect(request.exerciseSegments[1].segmentType, ExerciseSegmentType.rest);
    expect(request.exerciseSegments[2].repetitions, 6);
    expect(request.exerciseSegments[2].setIndex, 1);
  });

  test('buildWriteRequest links selected planned workout', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('pull_ups'),
      selectedPlannedWorkoutId: 'planned-id',
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '5',
      repetitionMode: ActivityRepetitionEntryMode.sets,
      repetitionSets: const [ActivityRepetitionSetInput(repetitionsText: '8')],
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.plannedExerciseSessionId, 'planned-id');
  });

  test('buildPlannedExerciseWriteRequest maps sets and rest steps', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('pull_ups'),
      titleText: 'Pull day',
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '5',
      repetitionMode: ActivityRepetitionEntryMode.sets,
      repetitionSets: const [
        ActivityRepetitionSetInput(repetitionsText: '8', restMinutesText: '60'),
        ActivityRepetitionSetInput(repetitionsText: '6'),
      ],
    );

    final request = buildPlannedExerciseWriteRequest(state, UnitSystem.metric)!;
    expect(request.title, 'Pull day');
    expect(request.blocks.length, 1);
    expect(request.blocks.first.steps.length, 3);
    expect(request.blocks.first.steps[0].completion,
        const PlannedExerciseCompletionRepetitions(8));
    expect(request.blocks.first.steps[1].completion,
        const PlannedExerciseCompletionDurationSeconds(60));
    expect(request.blocks.first.steps[2].completion,
        const PlannedExerciseCompletionRepetitions(6));
  });

  test('buildWriteRequest writes treadmill steps as steps count', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeById('treadmill'),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '20',
      repetitionTotalText: '2400',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseType, ExerciseSessionType.runningTreadmill);
    expect(request.stepsCount, 2400);
  });

  test('buildWriteRequest writes walking steps as steps count', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeByExercise(ExerciseSessionType.walking),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '20',
      distanceText: '1.6',
      repetitionTotalText: '2100',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseType, ExerciseSessionType.walking);
    expect(request.stepsCount, 2100);
    expect(request.distanceMeters ?? 0.0, closeTo(1600.0, 0.001));
  });

  test('buildWriteRequest allows walking without steps', () {
    final state = ActivityEntryUiState(
      selectedActivityType: typeByExercise(ExerciseSessionType.walking),
      startDateText: '2026-05-26',
      startTimeText: '8:30',
      durationMinutesText: '20',
      distanceText: '1.6',
    );

    final request = buildWriteRequest(state, UnitSystem.metric)!;
    expect(request.exerciseType, ExerciseSessionType.walking);
    expect(request.stepsCount, isNull);
  });

  // ── Controller behaviour ─────────────────────────────────────────────────

  test('activity entry exposes field errors and skips write for invalid values',
      () async {
    final repository = repo(canWrite: true);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    vm.updateDurationMinutes('0');
    vm.updateDistance('-1');
    vm.addEntry(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.entryError, ActivityEntryError.invalidValue);
    expect(vm.value.validationErrors,
        contains(ActivityEntryValidationError.durationInvalid));
    expect(vm.value.validationErrors,
        contains(ActivityEntryValidationError.distanceInvalid));
    expect(repository.writeActivityEntryCalls, isEmpty);
    vm.dispose();
  });

  test('selecting activity clears metric fields that activity does not use',
      () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    vm.updateDistance('10.5');
    vm.updateElevation('120');
    vm.selectActivityType(typeById('push_ups'));
    await vm.idle();

    expect(vm.value.distanceText, '');
    expect(vm.value.elevationText, '');
    vm.dispose();
  });

  test('missing activity write permission prevents write', () async {
    final repository = repo(canWrite: false);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    await vm.idle();
    vm.addEntry(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.entryError, ActivityEntryError.missingWritePermission);
    expect(repository.writeActivityEntryCalls, isEmpty);
    vm.dispose();
  });

  test('activity entry writes request when permission is granted', () async {
    final repository = repo(canWrite: true);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    await vm.idle();
    vm.updateDistance('5');
    vm.refreshPermission();
    await vm.idle();
    vm.addEntry(UnitSystem.metric);
    await vm.idle();

    expect(repository.writeActivityEntryCalls.length, 1);
    expect(
      (repository.writeActivityEntryCalls.first.distanceMeters ?? 0.0) - 5000.0,
      closeTo(0.0, 0.001),
    );
    expect(vm.value.isSavingEntry, isFalse);
    expect(vm.value.saveCompleted, isTrue);
    vm.dispose();
  });

  test('selecting planned workout prefills editable set structure', () async {
    final plan = plannedPullUpPlan();
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, plannedWorkouts: [plan]),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    await vm.idle();
    vm.applyPlannedWorkout('planned-id');

    expect(vm.value.selectedPlannedWorkoutId, 'planned-id');
    expect(vm.value.hasSelectedPlannedWorkoutChanges, isFalse);
    expect(vm.value.titleText, 'Pull-up ladder');
    expect(vm.value.repetitionMode, ActivityRepetitionEntryMode.sets);
    expect(vm.value.repetitionSets, const [
      ActivityRepetitionSetInput(repetitionsText: '8', restMinutesText: '60'),
      ActivityRepetitionSetInput(repetitionsText: '6'),
    ]);

    vm.updateTitle('Pull-up ladder plus');
    expect(vm.value.hasSelectedPlannedWorkoutChanges, isTrue);
    vm.dispose();
  });

  test('start from existing plan auto-applies the only available plan', () async {
    final plan = plannedPullUpPlan();
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, plannedWorkouts: [plan]),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startFromExistingPlan();
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.manual);
    expect(vm.value.plannedWorkouts, [plan]);
    expect(vm.value.selectedPlannedWorkoutId, 'planned-id');
    expect(vm.value.isLoadingPlannedWorkouts, isFalse);
    vm.dispose();
  });

  test('start from existing plan keeps picker when multiple activity types exist',
      () async {
    final pullUps = plannedPullUpPlan();
    final pushUps = plannedPushUpPlan();
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, plannedWorkouts: [pullUps, pushUps]),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startFromExistingPlan();
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.planActivityPicker);
    expect(vm.value.plannedWorkouts, [pullUps, pushUps]);
    expect(vm.value.isLoadingPlannedWorkouts, isFalse);
    vm.dispose();
  });

  test('startWithPlan opens the requested plan directly in manual entry', () async {
    final vm = ActivityEntryController(
      repository: repo(
        canWrite: true,
        plannedWorkouts: [plannedPullUpPlan(), plannedPushUpPlan()],
      ),
      clock: clockAt('2026-05-27T09:45:00Z'),
    );
    await vm.idle();

    vm.startWithPlan('planned-push-id');
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.manual);
    expect(vm.value.selectedPlannedWorkoutId, 'planned-push-id');
    expect(vm.value.selectedActivityType.id, 'push_ups');
    vm.dispose();
  });

  test('selecting activity then plan opens editable manual entry', () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, plannedWorkouts: [plannedPullUpPlan()]),
      clock: clockAt('2026-05-27T09:45:00Z'),
    );
    await vm.idle();

    vm.startFromExistingPlan();
    await vm.idle();
    vm.selectPlannedWorkoutActivity('pull_ups');
    vm.applyPlannedWorkout('planned-id');

    expect(vm.value.mode, ActivityEntryFormMode.manual);
    expect(vm.value.selectedActivityType.id, 'pull_ups');
    expect(vm.value.selectedPlannedWorkoutId, 'planned-id');
    expect(vm.value.startDateText, '2026-05-27');
    expect(vm.value.startTimeText, '9:45');
    vm.dispose();
  });

  test('edit entry loads matching planned workouts without selecting a plan',
      () async {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final plan = plannedPullUpPlan();
    final workout = ExerciseData(
      id: 'activity-id',
      title: 'Pull-up ladder',
      exerciseType: ExerciseSessionType.calisthenics,
      startTime: start,
      endTime: start.add(const Duration(minutes: 5)),
      durationMs: 5 * 60 * 1000,
      source: 'tech.mmarca.openvitals',
      plannedExerciseSessionId: 'planned-id',
      segments: [
        ExerciseSegmentData(
          startTime: start,
          endTime: start.add(const Duration(seconds: 60)),
          segmentType: ExerciseSegmentType.pullUp,
          repetitions: 8,
        ),
      ],
      isOpenVitalsEntry: true,
    );
    final repository =
        repo(canWrite: true, plannedWorkouts: [plan], workout: workout);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
      editActivityId: 'activity-id',
    );
    await vm.idle();

    vm.loadEditEntry(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.selectedActivityType.id, 'pull_ups');
    expect(vm.value.plannedWorkouts, [plan]);
    expect(vm.value.selectedPlannedWorkoutId, isNull);
    expect(
      repository.loadPlannedWorkoutOptionsExerciseTypes,
      contains(ExerciseSessionType.calisthenics),
    );
    vm.dispose();
  });

  test('missing planned read permission is surfaced when loading existing plans',
      () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, canReadPlans: false),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startFromExistingPlan();
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.planActivityPicker);
    expect(vm.value.entryError, ActivityEntryError.missingWritePermission);
    expect(vm.value.writePermissions, plannedWorkoutWritePermissions);
    vm.dispose();
  });

  test('activity entry writes selected planned workout id', () async {
    final repository = repo(canWrite: true, plannedWorkouts: [plannedPullUpPlan()]);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    await vm.idle();
    vm.applyPlannedWorkout('planned-id');
    vm.addEntry(UnitSystem.metric);
    await vm.idle();

    expect(repository.writeActivityEntryCalls.length, 1);
    expect(repository.writeActivityEntryCalls.first.plannedExerciseSessionId,
        'planned-id');
    vm.dispose();
  });

  test('saving current structure writes planned workout', () async {
    final repository = repo(canWrite: true);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    vm.updateTitle('Pull-up ladder');
    vm.updateRepetitionMode(ActivityRepetitionEntryMode.sets);
    vm.updateRepetitionSetRepetitions(0, '8');
    vm.updateRepetitionSetRest(0, '60');
    vm.addRepetitionSet();
    vm.updateRepetitionSetRepetitions(1, '6');
    await vm.idle();
    vm.saveCurrentAsPlannedWorkout(UnitSystem.metric);
    await vm.idle();

    expect(repository.writePlannedWorkoutCalls.length, 1);
    final saved = repository.writePlannedWorkoutCalls.first;
    expect(saved.id, isNull);
    expect(saved.blocks.first.steps.map((s) => s.completion).toList(), const [
      PlannedExerciseCompletionRepetitions(8),
      PlannedExerciseCompletionDurationSeconds(60),
      PlannedExerciseCompletionRepetitions(6),
    ]);
    expect(vm.value.selectedPlannedWorkoutId, 'saved-plan-id');
    vm.dispose();
  });

  test('updating selected plan clears changed highlight baseline', () async {
    final repository = repo(canWrite: true, plannedWorkouts: [plannedPullUpPlan()]);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    await vm.idle();
    vm.applyPlannedWorkout('planned-id');
    vm.updateTitle('Pull-up ladder plus');

    expect(vm.value.hasSelectedPlannedWorkoutChanges, isTrue);

    vm.saveCurrentAsPlannedWorkout(UnitSystem.metric, updateSelected: true);
    await vm.idle();

    expect(repository.writePlannedWorkoutCalls.length, 1);
    expect(repository.writePlannedWorkoutCalls.first.id, 'planned-id');
    expect(repository.writePlannedWorkoutCalls.first.title, 'Pull-up ladder plus');
    expect(vm.value.hasSelectedPlannedWorkoutChanges, isFalse);
    vm.dispose();
  });

  test('saving current structure requires a training plan title', () async {
    final repository = repo(canWrite: true);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    vm.updateRepetitionMode(ActivityRepetitionEntryMode.sets);
    vm.updateRepetitionSetRepetitions(0, '8');
    await vm.idle();
    vm.saveCurrentAsPlannedWorkout(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.entryError, ActivityEntryError.invalidValue);
    expect(vm.value.validationErrors,
        contains(ActivityEntryValidationError.trainingPlanTitleRequired));
    expect(repository.writePlannedWorkoutCalls, isEmpty);
    vm.dispose();
  });

  test('new plan option clears selected plan and saves a new planned workout',
      () async {
    final repository = repo(canWrite: true, plannedWorkouts: [plannedPullUpPlan()]);
    final vm = ActivityEntryController(
      repository: repository,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    await vm.idle();
    vm.applyPlannedWorkout('planned-id');
    vm.createNewPlannedWorkout();

    expect(vm.value.selectedPlannedWorkoutId, isNull);
    expect(vm.value.titleText, '');
    expect(vm.value.notesText, '');
    expect(vm.value.durationMinutesText, '30');
    expect(vm.value.repetitionMode, ActivityRepetitionEntryMode.sets);
    expect(vm.value.repetitionSets, const [ActivityRepetitionSetInput()]);

    vm.updateTitle('New pull-up plan');
    vm.updateRepetitionSetRepetitions(0, '5');
    vm.saveCurrentAsPlannedWorkout(UnitSystem.metric);
    await vm.idle();

    expect(repository.writePlannedWorkoutCalls.length, 1);
    expect(repository.writePlannedWorkoutCalls.first.id, isNull);
    expect(repository.writePlannedWorkoutCalls.first.title, 'New pull-up plan');
    vm.dispose();
  });

  test('missing planned workout permission is surfaced before saving plan',
      () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true, canWritePlan: false),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.selectActivityType(typeById('pull_ups'));
    vm.startManualEntry();
    vm.updateTitle('Pull-up ladder');
    vm.updateRepetitionMode(ActivityRepetitionEntryMode.sets);
    vm.updateRepetitionSetRepetitions(0, '8');
    await vm.idle();
    vm.saveCurrentAsPlannedWorkout(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.entryError, ActivityEntryError.missingWritePermission);
    expect(vm.value.writePermissions, plannedWorkoutWritePermissions);
    vm.dispose();
  });

  test('activity entry defaults to latest recorded activity when no favorite is set',
      () async {
    final prefs = await makePrefs(last: ExerciseSessionType.biking);
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      preferencesRepository: prefs,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    expect(vm.value.selectedActivityType.exerciseType, ExerciseSessionType.biking);
    vm.dispose();
  });

  test('favorite activity overrides latest recorded activity', () async {
    final prefs = await makePrefs(
      favorite: ExerciseSessionType.walking,
      last: ExerciseSessionType.biking,
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      preferencesRepository: prefs,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    expect(vm.value.selectedActivityType.exerciseType, ExerciseSessionType.walking);
    vm.dispose();
  });

  test('manual activity entry does not estimate calories', () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    await vm.idle();

    expect(vm.value.activeCaloriesText, '');
    expect(vm.value.totalCaloriesText, '');
    vm.dispose();
  });

  test('recorded activity without enough route points estimates calories',
      () async {
    final prefs = await makePrefs();
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final recorder = _FakeRecordingController(
      snapshot: ActivityRecordingSnapshot(
        exerciseType: ExerciseSessionType.running,
        startTime: start,
        endTime: start.add(const Duration(minutes: 30)),
        points: const [],
        pauseIntervals: const [],
        distanceMeters: 0.0,
        elevationGainedMeters: 0.0,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      activityRecorder: recorder,
      preferencesRepository: prefs,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.finishGpsRecording(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.manual);
    expect(vm.value.activeCaloriesText, '308');
    expect(vm.value.totalCaloriesText, '343');
    expect(prefs.lastActivityExerciseType, ExerciseSessionType.running);
    vm.dispose();
  });

  test('finished recording draft is restored by a new activity entry view model',
      () async {
    final draftStore = ActivityRecordingDraftStore();
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final recorder = _FakeRecordingController(
      snapshot: ActivityRecordingSnapshot(
        exerciseType: ExerciseSessionType.biking,
        startTime: start,
        endTime: start.add(const Duration(minutes: 45)),
        points: [
          routePoint(start),
          routePoint(start.add(const Duration(minutes: 45)), latitude: 59.01),
        ],
        pauseIntervals: const [],
        distanceMeters: 1200.0,
        elevationGainedMeters: 12.0,
      ),
    );
    final firstVm = ActivityEntryController(
      repository: repo(canWrite: true),
      activityRecorder: recorder,
      recordingDraftStore: draftStore,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await firstVm.idle();
    firstVm.selectActivityType(typeByExercise(ExerciseSessionType.biking));
    await firstVm.idle();

    firstVm.finishGpsRecording(UnitSystem.metric);
    await firstVm.idle();

    final restoredVm = ActivityEntryController(
      repository: repo(canWrite: true),
      recordingDraftStore: draftStore,
      clock: clockAt('2026-05-26T08:31:00Z'),
    );
    await restoredVm.idle();

    expect(restoredVm.value.mode, ActivityEntryFormMode.routeImport);
    expect(restoredVm.value.selectedActivityType.exerciseType,
        ExerciseSessionType.biking);
    expect(restoredVm.value.distanceText, '1.2');
    expect(restoredVm.value.elevationText, '12');
    expect(restoredVm.value.isRecordingDraft, isTrue);
    firstVm.dispose();
    restoredVm.dispose();
  });

  test('finished walking route recording keeps recorded steps', () async {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final recorder = _FakeRecordingController(
      snapshot: ActivityRecordingSnapshot(
        exerciseType: ExerciseSessionType.walking,
        activityTypeId: typeByExercise(ExerciseSessionType.walking).id,
        startTime: start,
        endTime: start.add(const Duration(minutes: 30)),
        points: [
          routePoint(start),
          routePoint(start.add(const Duration(minutes: 30)), latitude: 59.01),
        ],
        pauseIntervals: const [],
        distanceMeters: 1200.0,
        elevationGainedMeters: 12.0,
        repetitionCount: 1800,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      activityRecorder: recorder,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();
    vm.selectActivityType(typeByExercise(ExerciseSessionType.walking));
    await vm.idle();

    vm.finishGpsRecording(UnitSystem.metric);
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.routeImport);
    expect(vm.value.selectedActivityType.exerciseType, ExerciseSessionType.walking);
    expect(vm.value.repetitionTotalText, '1800');
    expect(vm.value.distanceText, '1.2');
    vm.dispose();
  });

  test('saving a restored recording draft clears it', () async {
    final draftStore = ActivityRecordingDraftStore();
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final recorder = _FakeRecordingController(
      snapshot: ActivityRecordingSnapshot(
        exerciseType: ExerciseSessionType.running,
        startTime: start,
        endTime: start.add(const Duration(minutes: 30)),
        points: const [],
        pauseIntervals: const [],
        distanceMeters: 0.0,
        elevationGainedMeters: 0.0,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      activityRecorder: recorder,
      recordingDraftStore: draftStore,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();
    vm.finishGpsRecording(UnitSystem.metric);
    await vm.idle();

    final restoredVm = ActivityEntryController(
      repository: repo(canWrite: true),
      recordingDraftStore: draftStore,
      clock: clockAt('2026-05-26T08:31:00Z'),
    );
    await restoredVm.idle();
    restoredVm.addEntry(UnitSystem.metric);
    await restoredVm.idle();

    expect(draftStore.restore(), isNull);
    vm.dispose();
    restoredVm.dispose();
  });

  test('discarding a finished recording draft clears it and returns to source choice',
      () async {
    final draftStore = ActivityRecordingDraftStore();
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final recorder = _FakeRecordingController(
      snapshot: ActivityRecordingSnapshot(
        exerciseType: ExerciseSessionType.biking,
        startTime: start,
        endTime: start.add(const Duration(minutes: 45)),
        points: [
          routePoint(start),
          routePoint(start.add(const Duration(minutes: 45)), latitude: 59.01),
        ],
        pauseIntervals: const [],
        distanceMeters: 1200.0,
        elevationGainedMeters: 12.0,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      activityRecorder: recorder,
      recordingDraftStore: draftStore,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();
    vm.finishGpsRecording(UnitSystem.metric);
    await vm.idle();

    vm.discardRecordingDraft();
    await vm.idle();

    expect(draftStore.restore(), isNull);
    expect(vm.value.mode, ActivityEntryFormMode.chooseSource);
    expect(vm.value.isRecordingDraft, isFalse);
    expect(vm.value.importedRoute, isNull);
    vm.dispose();
  });

  test('activity entry keeps full write permissions when optional fields change',
      () async {
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.startManualEntry();
    await vm.idle();
    vm.updateDistance('5');
    vm.updateElevation('20');
    vm.updateActiveCalories('300');
    vm.updateTotalCalories('350');

    expect(vm.value.writePermissions, activityWritePermissions);
    expect(vm.value.canWrite, isTrue);
    vm.dispose();
  });

  test('route import fills distance and elevation fields in current unit system',
      () async {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final last = DateTime.utc(2026, 5, 26, 8, 40);
    final importer = _FakeRouteFileImporter(
      RouteFileImport(
        fileName: 'run.kmz',
        points: [routePoint(start), routePoint(last, latitude: 59.01)],
        distanceMeters: 0.4 * 1609.344,
        elevationGainedMeters: 12.0 * 0.3048,
        startTime: start,
        endTime: last,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      routeFileImporter: importer,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.importRouteFile(_handle(), UnitSystem.imperial);
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.routeImport);
    expect(vm.value.distanceText, '0.4');
    expect(vm.value.elevationText, '12');
    expect(vm.value.durationMinutesText, '11');
    expect(vm.value.activeCaloriesText, '113');
    expect(vm.value.totalCaloriesText, '126');
    vm.dispose();
  });

  test('FIT import without route fills manual activity fields', () async {
    final start = DateTime.utc(2026, 5, 26, 8, 30);
    final end = DateTime.utc(2026, 5, 26, 9, 15);
    final importer = _FakeRouteFileImporter(
      RouteFileImport(
        fileName: 'Functional Strength Training.fit',
        points: const [],
        distanceMeters: 0.0,
        elevationGainedMeters: 0.0,
        activeCaloriesKcal: 220.0,
        startTime: start,
        endTime: end,
        type: 'training',
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      routeFileImporter: importer,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.importRouteFile(_handle(), UnitSystem.metric);
    await vm.idle();

    expect(vm.value.mode, ActivityEntryFormMode.routeImport);
    expect(vm.value.selectedActivityType.exerciseType,
        ExerciseSessionType.strengthTraining);
    expect(vm.value.selectedActivityType.supportsGpsRoute, isFalse);
    expect(vm.value.importedRoute?.points ?? const [], isEmpty);
    expect(vm.value.titleText, 'Functional Strength Training');
    expect(vm.value.durationMinutesText, '45');
    expect(vm.value.activeCaloriesText, '220');
    vm.dispose();
  });

  test('FIT workout import uses workout duration without changing selected time',
      () async {
    final importer = _FakeRouteFileImporter(
      RouteFileImport(
        fileName: 'Tempo Run.fit',
        points: const [],
        distanceMeters: 0.0,
        elevationGainedMeters: 0.0,
        startTime: DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
        endTime: DateTime.fromMillisecondsSinceEpoch(0, isUtc: true)
            .add(const Duration(minutes: 15)),
        durationSeconds: 15 * 60,
        name: 'Tempo Run',
        type: 'running',
        hasRecordedTimestamps: false,
        hasImportedTimeRange: false,
      ),
    );
    final vm = ActivityEntryController(
      repository: repo(canWrite: true),
      routeFileImporter: importer,
      clock: clockAt('2026-05-26T08:30:00Z'),
    );
    await vm.idle();

    vm.importRouteFile(_handle(), UnitSystem.metric);
    await vm.idle();

    expect(vm.value.titleText, 'Tempo Run');
    expect(vm.value.startDateText, '2026-05-26');
    expect(vm.value.startTimeText, '8:30');
    expect(vm.value.durationMinutesText, '15');
    expect(vm.value.selectedActivityType.exerciseType, ExerciseSessionType.running);
    vm.dispose();
  });
}

String _p2(int value) => value.toString().padLeft(2, '0');

ActivityRouteFileHandle _handle() =>
    ActivityRouteFileHandle(bytes: Uint8List(0), fileName: 'route');

class _FakeRouteFileImporter implements RouteFileImporter {
  _FakeRouteFileImporter(this._result);
  final RouteFileImport _result;

  @override
  Future<RouteFileImport> import(ActivityRouteFileHandle handle) async => _result;
}

class _FakeRecordingController implements ActivityRecordingController {
  _FakeRecordingController({required this.snapshot});

  final ActivityRecordingSnapshot snapshot;

  @override
  final ValueNotifier<ActivityRecordingState> state =
      ValueNotifier(const ActivityRecordingState());

  @override
  ActivityRecordingSnapshot? finishRecording() => snapshot;

  @override
  void stopBlePreview() {}
  @override
  void clearPreparedRecording() {}
  @override
  void previewBleConnections() {}
  @override
  void prepareRecordingDashboard(ActivityEntryType activityType) {}
  @override
  void updateDashboardLayout(dynamic layout) {}
  @override
  Future<bool> startRecording(
    ActivityEntryType activityType,
    ActivityRecordingInitialFix? initialFix, {
    int repetitionRestSeconds = 0,
  }) async =>
      true;
  @override
  void pauseRecording() {}
  @override
  void resumeRecording() {}
  @override
  void addManualLap() {}
  @override
  void addMarker() {}
  @override
  void updateMarker(ActivityRecordingMarker marker) {}
  @override
  void deleteMarker(String markerId) {}
  @override
  void discardRecording() {}
  @override
  void adjustRepetitionCount(int delta) {}
  @override
  void endRepetitionSet() {}
  @override
  void startNextRepetitionSet() {}
}

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({
    required this.canWriteValue,
    required this.plannedWorkouts,
    required this.workout,
    required this.canReadPlans,
    required this.canWritePlan,
    required this.activityPermissions,
    required this.plannedPermissions,
  });

  final bool canWriteValue;
  final List<PlannedExerciseData> plannedWorkouts;
  final ExerciseData? workout;
  final bool canReadPlans;
  final bool canWritePlan;
  final Set<String> activityPermissions;
  final Set<String> plannedPermissions;

  final List<ActivityWriteRequest> writeActivityEntryCalls = [];
  final List<PlannedExerciseWriteRequest> writePlannedWorkoutCalls = [];
  final List<int> loadPlannedWorkoutOptionsExerciseTypes = [];

  @override
  Set<String> activityWritePermissions() => activityPermissions;

  @override
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest request) =>
      activityPermissions;

  @override
  Set<String> plannedWorkoutWritePermissions() => plannedPermissions;

  @override
  Future<Result<bool>> hasActivityWritePermission() async => Ok(canWriteValue);

  @override
  Future<Result<bool>> hasActivityWritePermissionForRequest(
          ActivityWriteRequest request) async =>
      Ok(canWriteValue);

  @override
  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request) async {
    writeActivityEntryCalls.add(request);
    return const Ok('activity-id');
  }

  @override
  Future<Result<void>> updateActivityEntry(
          String id, ActivityWriteRequest request) async =>
      const Ok(null);

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => Ok(workout);

  @override
  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  ) async {
    loadPlannedWorkoutOptionsExerciseTypes.add(exerciseType);
    return Ok(plannedWorkouts);
  }

  @override
  Future<Result<List<PlannedExerciseData>>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  }) async {
    if (!canReadPlans) {
      throw const MissingHealthPermissionException('Missing planned read.');
    }
    return Ok(plannedWorkouts);
  }

  @override
  Future<Result<String>> writePlannedWorkout(
      PlannedExerciseWriteRequest request) async {
    if (!canWritePlan) {
      throw const MissingHealthPermissionException('Missing planned write.');
    }
    writePlannedWorkoutCalls.add(request);
    return const Ok('saved-plan-id');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}
