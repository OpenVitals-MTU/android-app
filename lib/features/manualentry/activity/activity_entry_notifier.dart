import 'package:flutter/foundation.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/contract/activity_repository.dart';
import '../../../data/repository/contract/heart_repository.dart';
import '../../../data/repository/contract/repository_exceptions.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../domain/preferences/unit_system.dart';
import 'activity_entry_clock.dart';
import 'activity_entry_edit_mapper.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'activity_entry_write_request_builder.dart';
import 'recording/activity_recording.dart';
import 'recording/activity_recording_draft_store.dart';
import 'routeimport/route_file_parser.dart';

/// A picked route file handed to [RouteFileImporter].
class ActivityRouteFileHandle {
  const ActivityRouteFileHandle({required this.bytes, this.fileName});

  final Uint8List bytes;
  final String? fileName;
}

/// Wraps route-file I/O so the notifier can be tested with a fake. Mirrors the
/// Kotlin `RouteFileImporter`.
abstract interface class RouteFileImporter {
  Future<RouteFileImport> import(ActivityRouteFileHandle handle);
}

/// Default importer: parses the bytes synchronously on a microtask.
class DefaultRouteFileImporter implements RouteFileImporter {
  const DefaultRouteFileImporter();

  @override
  Future<RouteFileImport> import(ActivityRouteFileHandle handle) async =>
      RouteFileParser.parseFile(handle.bytes, fileName: handle.fileName);
}

/// Port of the Kotlin `ActivityEntryViewModel`. A plain, directly-constructible
/// controller exposing an observable [ActivityEntryUiState]; wrapped by a
/// Riverpod provider for the screen (see `activity_entry_providers.dart`).
class ActivityEntryController {
  ActivityEntryController({
    required this.repository,
    this.heartRepository,
    this.routeFileImporter,
    this.activityRecorder,
    this.recordingDraftStore,
    this.preferencesRepository,
    this.markerRepository,
    ActivityEntryClock? clock,
    this.editActivityId,
    this.launchMode,
    this.launchPlanId,
    this.launchActivityTypeId,
  }) : clock = clock ?? ActivityEntryClock.system() {
    _state = ValueNotifier<ActivityEntryUiState>(
      _initialState(recordingDraftStore?.restore()),
    );
    _init();
  }

  final ActivityRepository repository;
  final HeartRepository? heartRepository;
  final RouteFileImporter? routeFileImporter;
  final ActivityRecordingController? activityRecorder;
  final ActivityRecordingDraftStore? recordingDraftStore;
  final PreferencesRepository? preferencesRepository;
  final ActivityMarkerRepository? markerRepository;
  final ActivityEntryClock clock;
  final String? editActivityId;
  final String? launchMode;
  final String? launchPlanId;
  final String? launchActivityTypeId;

  late final ValueNotifier<ActivityEntryUiState> _state;
  ValueListenable<ActivityEntryUiState> get uiState => _state;
  ActivityEntryUiState get value => _state.value;

  bool _editEntryLoaded = false;
  final Set<Future<void>> _pending = <Future<void>>{};
  VoidCallback? _recorderListener;
  bool _disposed = false;

  void _init() {
    refreshPermission();
    final recorder = activityRecorder;
    if (recorder != null) {
      void listener() {
        final recording = recorder.state.value;
        if (recording.isActive) _applyRecordingProgress(recording);
      }

      _recorderListener = listener;
      recorder.state.addListener(listener);
    }
    _applyLaunchIntent();
  }

  /// Awaits all in-flight async operations (test helper; analogue of
  /// `advanceUntilIdle()`).
  Future<void> idle() async {
    var guard = 0;
    while (_pending.isNotEmpty && guard < 1000) {
      guard++;
      await Future.wait(_pending.toList());
    }
    await Future<void>.delayed(Duration.zero);
    guard = 0;
    while (_pending.isNotEmpty && guard < 1000) {
      guard++;
      await Future.wait(_pending.toList());
    }
  }

  void dispose() {
    _disposed = true;
    final recorder = activityRecorder;
    final listener = _recorderListener;
    if (recorder != null && listener != null) {
      recorder.state.removeListener(listener);
    }
    recordingDraftStore?.store(_state.value);
    _state.dispose();
  }

  void _launch(Future<void> Function() body) {
    late final Future<void> future;
    future = body().whenComplete(() => _pending.remove(future));
    _pending.add(future);
  }

  void _set(ActivityEntryUiState state) {
    if (_disposed) return;
    _state.value = state;
  }

  // ── Init / launch intent ────────────────────────────────────────────────

  void _applyLaunchIntent() {
    if (editActivityId != null) return;
    if (_state.value.isRecordingDraft) return;

    final typeId = launchActivityTypeId;
    if (typeId != null) {
      final type = activityEntryTypeById(typeId);
      if (type != null) {
        _set(_state.value.copyWith(selectedActivityType: type));
      }
    }

    if (launchPlanId != null) {
      startWithPlan(launchPlanId!);
    } else if (launchMode == 'record') {
      prepareGpsRecording();
    } else if (launchMode == 'manual') {
      startManualEntry();
    } else if (launchMode == 'plan') {
      startFromExistingPlan();
    }
  }

  ActivityEntryUiState _initialState(ActivityEntryUiState? recordingDraft) {
    if (editActivityId == null && recordingDraft?.isRecordingDraft == true) {
      return recordingDraft!.copyWith(
        writePermissions: repository.activityWritePermissions(),
        canWrite: false,
        isCheckingPermission: true,
        isSavingEntry: false,
        isImportingRoute: false,
        entryError: null,
        detailError: null,
        validationErrors: const {},
        editRecordId: null,
        saveCompleted: false,
      );
    }
    return initialActivityEntryState(
      clock,
      repository.activityWritePermissions(),
      selectedActivityType: _preferredActivityType(),
    ).copyWith(
      mode: editActivityId == null
          ? ActivityEntryFormMode.chooseSource
          : ActivityEntryFormMode.manual,
      editRecordId: editActivityId,
    );
  }

  // ── Permissions ─────────────────────────────────────────────────────────

  void refreshPermission() {
    final permissions = _currentRequiredPermissions();
    _launch(() async {
      _set(_state.value.copyWith(
        isCheckingPermission: true,
        writePermissions: permissions,
        detailError: null,
      ));
      try {
        final canWrite = (await repository.hasActivityWritePermission()).orThrow();
        _set(_state.value.copyWith(
          isCheckingPermission: false,
          canWrite: canWrite,
          writePermissions: _currentRequiredPermissions(),
        ));
      } catch (error) {
        _set(_state.value.copyWith(
          isCheckingPermission: false,
          canWrite: false,
          entryError: ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
          writePermissions: _currentRequiredPermissions(),
        ));
      }
    });
  }

  // ── Source selection ────────────────────────────────────────────────────

  void selectActivityType(ActivityEntryType type) {
    final current = _state.value;
    final retainedRoute = current.importedRoute != null &&
            (current.importedRoute!.points.isEmpty || type.supportsGpsRoute)
        ? current.importedRoute
        : null;
    _set(current.copyWith(
      selectedActivityType: type,
      plannedWorkouts: const [],
      selectedPlannedWorkoutId: null,
      selectedPlannedWorkoutActivityTypeId: null,
      distanceText: type.supportsDistance ? current.distanceText : '',
      elevationText: type.supportsElevation ? current.elevationText : '',
      importedRoute: retainedRoute,
      recordedPauseIntervals:
          retainedRoute == null ? const [] : current.recordedPauseIntervals,
      recordedLaps: retainedRoute == null ? const [] : current.recordedLaps,
      recordedMarkers: retainedRoute == null ? const [] : current.recordedMarkers,
      mode: retainedRoute == null &&
              current.mode == ActivityEntryFormMode.routeImport
          ? ActivityEntryFormMode.manual
          : current.mode,
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
    refreshPlannedWorkouts();
  }

  void startManualEntry() {
    recordingDraftStore?.clear();
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.manual,
      selectedPlannedWorkoutId: null,
      selectedPlannedWorkoutActivityTypeId: null,
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      isRecordingDraft: false,
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
    refreshPlannedWorkouts();
  }

  void startFromExistingPlan() {
    recordingDraftStore?.clear();
    _launch(() async {
      _set(_state.value.copyWith(
        mode: ActivityEntryFormMode.planActivityPicker,
        plannedWorkouts: const [],
        selectedPlannedWorkoutId: null,
        selectedPlannedWorkoutActivityTypeId: null,
        isLoadingPlannedWorkouts: true,
        isRecordingDraft: false,
        entryError: null,
        detailError: null,
        validationErrors: const {},
      ));
      try {
        final plans = (await repository.loadExistingPlannedWorkouts(
                anchorDate: _todayLocalDate()))
            .orThrow();
        _set(_state.value.copyWith(
          plannedWorkouts: plans,
          isLoadingPlannedWorkouts: false,
        ));
        _autoAdvancePlanSelection(plans);
      } catch (error) {
        _set(_state.value.copyWith(
          plannedWorkouts: const [],
          isLoadingPlannedWorkouts: false,
          writePermissions: repository.plannedWorkoutWritePermissions(),
          canWrite: false,
          entryError: error is MissingHealthPermissionException
              ? ActivityEntryError.missingWritePermission
              : ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
        ));
      }
    });
  }

  void startWithPlan(String planId) {
    recordingDraftStore?.clear();
    _launch(() async {
      _set(_state.value.copyWith(
        mode: ActivityEntryFormMode.planActivityPicker,
        plannedWorkouts: const [],
        selectedPlannedWorkoutId: null,
        selectedPlannedWorkoutActivityTypeId: null,
        isLoadingPlannedWorkouts: true,
        isRecordingDraft: false,
        entryError: null,
        detailError: null,
        validationErrors: const {},
      ));
      try {
        final plans = (await repository.loadExistingPlannedWorkouts(
                anchorDate: _todayLocalDate()))
            .orThrow();
        _set(_state.value.copyWith(
          plannedWorkouts: plans,
          isLoadingPlannedWorkouts: false,
        ));
        if (plans.any((plan) => plan.id == planId)) {
          applyPlannedWorkout(planId);
        } else {
          _autoAdvancePlanSelection(plans);
        }
      } catch (error) {
        _set(_state.value.copyWith(
          plannedWorkouts: const [],
          isLoadingPlannedWorkouts: false,
          writePermissions: repository.plannedWorkoutWritePermissions(),
          canWrite: false,
          entryError: error is MissingHealthPermissionException
              ? ActivityEntryError.missingWritePermission
              : ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
        ));
      }
    });
  }

  void _autoAdvancePlanSelection(List<PlannedExerciseData> plans) {
    final selectable =
        plans.where((plan) => plan.completedExerciseSessionId == null).toList();
    if (selectable.isEmpty) return;
    if (selectable.length == 1) {
      applyPlannedWorkout(selectable.first.id);
      return;
    }
    final typeIds = <String>[];
    for (final plan in selectable) {
      final id = _planActivityType(plan)?.id;
      if (id != null && !typeIds.contains(id)) typeIds.add(id);
    }
    if (typeIds.length == 1) {
      selectPlannedWorkoutActivity(typeIds.first);
    }
  }

  void selectPlannedWorkoutActivity(String typeId) {
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.planPicker,
      selectedPlannedWorkoutActivityTypeId: typeId,
      selectedPlannedWorkoutId: null,
      entryError: null,
      detailError: null,
    ));
  }

  void choosePlannedWorkoutActivity() {
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.planActivityPicker,
      selectedPlannedWorkoutActivityTypeId: null,
      selectedPlannedWorkoutId: null,
      entryError: null,
      detailError: null,
    ));
  }

  void chooseSource() {
    if (_state.value.isEditMode) return;
    recordingDraftStore?.clear();
    activityRecorder?.stopBlePreview();
    activityRecorder?.clearPreparedRecording();
    _set(initialActivityEntryState(
      clock,
      repository.activityWritePermissions(),
      selectedActivityType: _preferredActivityType(),
    ).copyWith(
      canWrite: _state.value.canWrite,
      isCheckingPermission: _state.value.isCheckingPermission,
      editRecordId: editActivityId,
    ));
    refreshPermission();
  }

  // ── Field updates ───────────────────────────────────────────────────────

  void updateTitle(String text) => _updateState(
        clearFields: const {ActivityEntryField.title},
        update: (s) => s.copyWith(titleText: text, entryError: null, detailError: null),
      );

  void updateNotes(String text) => _updateState(
        update: (s) => s.copyWith(notesText: text, entryError: null, detailError: null),
      );

  void updateFeeling(ActivityEntryFeeling? feeling) => _updateState(
        update: (s) =>
            s.copyWith(selectedFeeling: feeling, entryError: null, detailError: null),
      );

  void updateStartDate(String text) {
    _updateState(
      clearFields: const {ActivityEntryField.startDate, ActivityEntryField.startTime},
      update: (s) => s.copyWith(
        startDateText: text,
        selectedPlannedWorkoutId: null,
        entryError: null,
        detailError: null,
      ),
    );
    refreshPlannedWorkouts();
  }

  void updateStartTime(String text) => _updateState(
        clearFields: const {ActivityEntryField.startTime},
        update: (s) =>
            s.copyWith(startTimeText: text, entryError: null, detailError: null),
      );

  void updateDurationMinutes(String text) => _updateState(
        clearFields: const {ActivityEntryField.duration},
        update: (s) =>
            s.copyWith(durationMinutesText: text, entryError: null, detailError: null),
      );

  void updateDistance(String text) => _updateState(
        clearFields: const {ActivityEntryField.distance},
        update: (s) =>
            s.copyWith(distanceText: text, entryError: null, detailError: null),
      );

  void updateElevation(String text) => _updateState(
        clearFields: const {ActivityEntryField.elevation},
        update: (s) =>
            s.copyWith(elevationText: text, entryError: null, detailError: null),
      );

  void updateActiveCalories(String text) => _updateState(
        clearFields: const {
          ActivityEntryField.activeCalories,
          ActivityEntryField.totalCalories
        },
        update: (s) =>
            s.copyWith(activeCaloriesText: text, entryError: null, detailError: null),
      );

  void updateTotalCalories(String text) => _updateState(
        clearFields: const {ActivityEntryField.totalCalories},
        update: (s) =>
            s.copyWith(totalCaloriesText: text, entryError: null, detailError: null),
      );

  void updateRepetitionMode(ActivityRepetitionEntryMode mode) => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) =>
            s.copyWith(repetitionMode: mode, entryError: null, detailError: null),
      );

  void updateRepetitionTotal(String text) => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) =>
            s.copyWith(repetitionTotalText: text, entryError: null, detailError: null),
      );

  void updateRepetitionSetRepetitions(int index, String text) => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) => s.copyWith(
          repetitionSets: [
            for (var i = 0; i < s.repetitionSets.length; i++)
              i == index
                  ? s.repetitionSets[i].copyWith(repetitionsText: text)
                  : s.repetitionSets[i],
          ],
          entryError: null,
          detailError: null,
        ),
      );

  void updateRepetitionSetRest(int index, String text) => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) => s.copyWith(
          repetitionSets: [
            for (var i = 0; i < s.repetitionSets.length; i++)
              i == index
                  ? s.repetitionSets[i].copyWith(restMinutesText: text)
                  : s.repetitionSets[i],
          ],
          entryError: null,
          detailError: null,
        ),
      );

  void addRepetitionSet() => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) => s.copyWith(
          repetitionMode: ActivityRepetitionEntryMode.sets,
          repetitionSets: [...s.repetitionSets, const ActivityRepetitionSetInput()],
          entryError: null,
          detailError: null,
        ),
      );

  void removeRepetitionSet(int index) => _updateState(
        clearFields: const {ActivityEntryField.repetitions},
        update: (s) {
          final next = [
            for (var i = 0; i < s.repetitionSets.length; i++)
              if (i != index) s.repetitionSets[i],
          ];
          return s.copyWith(
            repetitionSets:
                next.isEmpty ? const [ActivityRepetitionSetInput()] : next,
            entryError: null,
            detailError: null,
          );
        },
      );

  // ── Route import ────────────────────────────────────────────────────────

  void importRouteFile(ActivityRouteFileHandle handle, UnitSystem unitSystem) {
    final importer = routeFileImporter;
    if (importer == null) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.routeImportFailed,
        detailError: const ScreenErrorMessage('Activity file import is not available.'),
        validationErrors: const {},
      ));
      return;
    }
    recordingDraftStore?.clear();
    _launch(() async {
      _set(_state.value.copyWith(
        isImportingRoute: true,
        isRecordingDraft: false,
        entryError: null,
        detailError: null,
        validationErrors: const {},
      ));
      try {
        final routeImport = await importer.import(handle);
        _applyRouteImport(routeImport, unitSystem);
      } catch (error) {
        _set(_state.value.copyWith(
          isImportingRoute: false,
          entryError: ActivityEntryError.routeImportFailed,
          detailError: throwableToScreenError(error),
          validationErrors: const {},
        ));
      }
    });
  }

  void clearImportedRoute() {
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.manual,
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
  }

  // ── Planned workouts ────────────────────────────────────────────────────

  void refreshPlannedWorkouts() {
    final snapshot = _state.value;
    if (!snapshot.selectedActivityType.supportsSetRepetitions) {
      _set(snapshot.copyWith(
        plannedWorkouts: const [],
        selectedPlannedWorkoutId: null,
        isLoadingPlannedWorkouts: false,
      ));
      return;
    }
    final date = parseStartDate(snapshot.startDateText);
    if (date == null) return;
    _launch(() async {
      _set(_state.value.copyWith(isLoadingPlannedWorkouts: true));
      try {
        final plans = (await repository.loadPlannedWorkoutOptions(
          date,
          snapshot.selectedActivityType.exerciseType,
        ))
            .orThrow();
        final currentSelectedId = _state.value.selectedPlannedWorkoutId;
        final selectedId = currentSelectedId != null &&
                (plans.isEmpty || plans.any((p) => p.id == currentSelectedId))
            ? currentSelectedId
            : null;
        _set(_state.value.copyWith(
          plannedWorkouts: plans,
          selectedPlannedWorkoutId: selectedId,
          isLoadingPlannedWorkouts: false,
        ));
      } catch (error) {
        _set(_state.value.copyWith(
          plannedWorkouts: const [],
          selectedPlannedWorkoutId: null,
          isLoadingPlannedWorkouts: false,
          detailError: throwableToScreenError(error),
        ));
      }
    });
  }

  void applyPlannedWorkout(String planId) {
    final plan =
        _state.value.plannedWorkouts.firstWhereOrNull((p) => p.id == planId);
    if (plan == null) return;
    final sets = plannedWorkoutToRepetitionSetInputs(plan);
    if (sets.isEmpty) return;
    final activityType = _planActivityType(plan) ?? _state.value.selectedActivityType;
    final selectedAt = _truncateToMinute(clock.nowInZone());
    final startDateText = isoLocalDate(selectedAt);
    final startTimeText = timeFormatterText(selectedAt);
    final durationMinutesText = _planDurationMinutesText(plan);
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.manual,
      selectedActivityType: activityType,
      selectedPlannedWorkoutId: plan.id,
      selectedPlannedWorkoutBaseline: _planBaseline(
        plan,
        activityType,
        startDateText,
        startTimeText,
        durationMinutesText,
        sets,
      ),
      selectedPlannedWorkoutActivityTypeId: activityType.id,
      titleText: plan.title ?? '',
      notesText: plan.notes ?? '',
      startDateText: startDateText,
      startTimeText: startTimeText,
      durationMinutesText: durationMinutesText,
      repetitionMode: ActivityRepetitionEntryMode.sets,
      repetitionTotalText: '',
      repetitionSets: sets,
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
  }

  void createNewPlannedWorkout() {
    final current = _state.value;
    _set(current.copyWith(
      selectedPlannedWorkoutId: null,
      selectedPlannedWorkoutBaseline: null,
      selectedPlannedWorkoutActivityTypeId: current.selectedActivityType.id,
      titleText: '',
      notesText: '',
      durationMinutesText: '30',
      repetitionMode: ActivityRepetitionEntryMode.sets,
      repetitionTotalText: '',
      repetitionSets: const [ActivityRepetitionSetInput()],
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
  }

  void saveCurrentAsPlannedWorkout(UnitSystem unitSystem,
      {bool updateSelected = false}) {
    final current = _state.value;
    final validationErrors = validatePlannedExerciseWriteRequest(current, unitSystem);
    final request = buildPlannedExerciseWriteRequest(
      current,
      unitSystem,
      updateExistingId: updateSelected ? current.selectedPlannedWorkoutId : null,
    );
    if (validationErrors.isNotEmpty || request == null) {
      _set(current.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: validationErrors,
      ));
      return;
    }
    _launch(() async {
      _set(_state.value.copyWith(
        isSavingPlannedWorkout: true,
        entryError: null,
        detailError: null,
      ));
      try {
        final savedPlanId =
            (await repository.writePlannedWorkout(request)).orThrow();
        final savedState = _state.value;
        _set(savedState.copyWith(
          selectedPlannedWorkoutId: savedPlanId,
          selectedPlannedWorkoutBaseline:
              savedState.plannedWorkoutBaseline(savedPlanId),
          isSavingPlannedWorkout: false,
          detailError: null,
        ));
        refreshPlannedWorkouts();
      } catch (error) {
        _set(_state.value.copyWith(
          isSavingPlannedWorkout: false,
          writePermissions: repository.plannedWorkoutWritePermissions(),
          canWrite: false,
          entryError: error is MissingHealthPermissionException
              ? ActivityEntryError.missingWritePermission
              : ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
        ));
      }
    });
  }

  // ── Permission-driven error surfaces ────────────────────────────────────

  void reportLocationPermissionNeeded() => _set(_state.value.copyWith(
        entryError: ActivityEntryError.locationPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  void reportNotificationPermissionNeeded() => _set(_state.value.copyWith(
        entryError: ActivityEntryError.notificationPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  void reportActivityRecognitionPermissionNeeded() =>
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.activityRecognitionPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  // ── Recording ───────────────────────────────────────────────────────────

  void prepareGpsRecording() {
    final current = _state.value;
    recordingDraftStore?.clear();
    _set(current.copyWith(
      mode: ActivityEntryFormMode.recording,
      selectedActivityType: _preferredActivityType(requireLiveRecording: true),
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      isRecordingDraft: false,
      distanceText: '',
      elevationText: '',
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
    activityRecorder?.clearPreparedRecording();
    activityRecorder?.previewBleConnections();
  }

  void openRecordingDashboard({int repetitionRestSeconds = 0}) {
    final recorder = activityRecorder;
    if (recorder == null) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.recordingFailed,
        detailError: const ScreenErrorMessage('GPS recording is not available.'),
        validationErrors: const {},
      ));
      return;
    }
    final current = _state.value;
    if (!current.selectedActivityType.supportsLiveRecording) {
      _set(current.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: const {
          ActivityEntryValidationError.activityTypeDoesNotSupportRoute
        },
      ));
      return;
    }
    if (!current.selectedActivityType.supportsGpsRoute) {
      startGpsRecording(repetitionRestSeconds: repetitionRestSeconds);
      return;
    }
    recordingDraftStore?.clear();
    _set(current.copyWith(
      mode: ActivityEntryFormMode.recording,
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      isRecordingDraft: false,
      distanceText: '',
      elevationText: '',
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    recorder.prepareRecordingDashboard(current.selectedActivityType);
  }

  Future<void> startGpsRecording({
    ActivityRecordingInitialFix? initialFix,
    int repetitionRestSeconds = 0,
  }) async {
    final recorder = activityRecorder;
    if (recorder == null) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.recordingFailed,
        detailError: const ScreenErrorMessage('GPS recording is not available.'),
        validationErrors: const {},
      ));
      return;
    }
    final current = _state.value;
    if (!current.selectedActivityType.supportsLiveRecording) {
      _set(current.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: const {
          ActivityEntryValidationError.activityTypeDoesNotSupportRoute
        },
      ));
      return;
    }
    recordingDraftStore?.clear();
    final now = _truncateToMinute(clock.nowInZone());
    _set(current.copyWith(
      mode: ActivityEntryFormMode.recording,
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      isRecordingDraft: false,
      startDateText: isoLocalDate(now),
      startTimeText: timeFormatterText(now),
      durationMinutesText: '1',
      distanceText: '',
      elevationText: '',
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    final started = await recorder.startRecording(
      current.selectedActivityType,
      initialFix,
      repetitionRestSeconds: repetitionRestSeconds,
    );
    if (!started) {
      final message = recorder.state.value.errorMessage;
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.recordingFailed,
        detailError: message != null ? ScreenErrorMessage(message) : null,
        validationErrors: const {},
      ));
    }
  }

  void pauseGpsRecording() => activityRecorder?.pauseRecording();
  void resumeGpsRecording() => activityRecorder?.resumeRecording();
  void addRecordingLap() => activityRecorder?.addManualLap();
  void addRecordingMarker() => activityRecorder?.addMarker();
  void updateRecordingMarker(ActivityRecordingMarker marker) =>
      activityRecorder?.updateMarker(marker);
  void deleteRecordingMarker(String markerId) =>
      activityRecorder?.deleteMarker(markerId);
  void adjustRepetitionRecording(int delta) =>
      activityRecorder?.adjustRepetitionCount(delta);
  void endRepetitionSet() => activityRecorder?.endRepetitionSet();
  void startNextRepetitionSet() => activityRecorder?.startNextRepetitionSet();

  /// Kotlin `ActivityEntryViewModel.updateRecordingDashboardLayout`.
  void updateRecordingDashboardLayout(ActivityRecordingDashboardLayout layout) =>
      activityRecorder?.updateDashboardLayout(layout);

  void discardGpsRecording() {
    activityRecorder?.discardRecording();
    activityRecorder?.stopBlePreview();
    recordingDraftStore?.clear();
    chooseSource();
  }

  void discardRecordingDraft() {
    if (!_state.value.isRecordingDraft || _state.value.isEditMode) return;
    recordingDraftStore?.clear();
    chooseSource();
  }

  void finishGpsRecording(UnitSystem unitSystem) {
    final snapshot = activityRecorder?.finishRecording();
    if (snapshot == null) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.recordingFailed,
        detailError:
            const ScreenErrorMessage('No active activity recording was found.'),
        validationErrors: const {},
      ));
      return;
    }
    _rememberLastActivityType(snapshot.exerciseType);

    if (snapshot.recordingKind == ActivityRecordingKind.gpsRoute &&
        snapshot.points.length >= minRecordedRoutePoints) {
      _applyRouteImport(
        RouteFileImport(
          fileName: null,
          points: snapshot.points,
          distanceMeters: snapshot.distanceMeters,
          elevationGainedMeters: snapshot.elevationGainedMeters,
          startTime: snapshot.startTime,
          endTime: snapshot.endTime,
          originalPointCount: snapshot.points.length,
        ),
        unitSystem,
      );
      final activityType = activityEntryTypeById(snapshot.activityTypeId);
      _set(_state.value.copyWith(
        recordedPauseIntervals: snapshot.pauseIntervals,
        recordedLaps: snapshot.manualLaps.map(_lapToExerciseLap).toList(),
        recordedMarkers: snapshot.markers,
        repetitionMode: ActivityRepetitionEntryMode.total,
        repetitionTotalText:
            (activityType?.supportsStepCounting == true && snapshot.repetitionCount > 0)
                ? snapshot.repetitionCount.toString()
                : '',
        isRecordingDraft: true,
        recordedBleSamples: snapshot.bleSamples,
      ));
    } else {
      _applyRecordingWithoutRoute(snapshot);
    }
    if (recordingDraftStore != null) recordingDraftStore!.store(_state.value);
  }

  // ── Edit ────────────────────────────────────────────────────────────────

  void loadEditEntry(UnitSystem unitSystem) {
    final recordId = editActivityId;
    if (recordId == null || _editEntryLoaded) return;
    _editEntryLoaded = true;
    _launch(() async {
      try {
        final workout = (await repository.loadWorkout(recordId)).orThrow();
        if (workout == null || !workout.isOpenVitalsEntry) {
          _set(_state.value.copyWith(
            entryError: ActivityEntryError.writeFailed,
            detailError:
                const ScreenErrorMessage('Only OpenVitals entries can be edited.'),
            validationErrors: const {},
          ));
          return;
        }
        final heartRateSamples = heartRepository == null
            ? const <HeartRateSample>[]
            : (await heartRepository!.loadHeartRateSamplesInstant(
                    workout.startTime, workout.endTime))
                .orThrow();
        final current = _state.value;
        var editState = exerciseToEditState(
          workout,
          unitSystem: unitSystem,
          clock: clock,
          repository: repository,
          canWrite: current.canWrite,
          isCheckingPermission: current.isCheckingPermission,
        ).copyWith(sessionHeartRateSamples: heartRateSamples);
        final markers = markerRepository?.markersForActivity(recordId) ?? const [];
        final resolvedMarkers = markers.isNotEmpty
            ? markers
            : (workout.clientRecordId != null
                ? markerRepository?.markersForActivity(workout.clientRecordId!) ??
                    const []
                : const <ActivityRecordingMarker>[]);
        editState = editState.copyWith(recordedMarkers: resolvedMarkers);
        _set(editState);
        refreshPlannedWorkouts();
      } catch (error) {
        _set(_state.value.copyWith(
          entryError: ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
          validationErrors: const {},
        ));
      }
    });
  }

  // ── Save ────────────────────────────────────────────────────────────────

  void addEntry(UnitSystem unitSystem) {
    if (_state.value.mode == ActivityEntryFormMode.chooseSource) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: const {},
      ));
      return;
    }
    final validationErrors = validateActivityEntry(_state.value, unitSystem);
    if (validationErrors.isNotEmpty) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: validationErrors,
      ));
      return;
    }
    final request = buildWriteRequest(_state.value, unitSystem);
    if (request == null) {
      _set(_state.value.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: validationErrors,
      ));
      return;
    }
    final editRecordId = _state.value.editRecordId;
    final wasRecordingDraft = _state.value.isRecordingDraft;
    final markersToSave = _state.value.selectedActivityType.supportsGpsRoute
        ? _state.value.recordedMarkers
        : const <ActivityRecordingMarker>[];
    final requestPermissions =
        repository.activityWritePermissionsForRequest(request);

    _launch(() async {
      _set(_state.value.copyWith(
        isSavingEntry: true,
        entryError: null,
        detailError: null,
        validationErrors: const {},
        writePermissions: requestPermissions,
      ));
      final hasPermission =
          (await repository.hasActivityWritePermissionForRequest(request))
              .orThrow();
      if (!hasPermission) {
        _set(_state.value.copyWith(
          isSavingEntry: false,
          canWrite: false,
          entryError: ActivityEntryError.missingWritePermission,
          detailError: null,
          validationErrors: const {},
        ));
        return;
      }
      try {
        final String savedActivityId;
        if (editRecordId == null) {
          savedActivityId =
              (await repository.writeActivityEntry(request)).orThrow();
        } else {
          (await repository.updateActivityEntry(editRecordId, request))
              .orThrow();
          savedActivityId = editRecordId;
        }
        markerRepository?.setMarkersForActivity(savedActivityId, markersToSave);
        recordingDraftStore?.clear();
        if (wasRecordingDraft) {
          _rememberLastActivityType(request.exerciseType);
        }
        if (editRecordId == null) {
          _set(clearedAfterSaveState(clock, repository.activityWritePermissions(),
                  _preferredActivityType())
              .copyWith(saveCompleted: true));
          refreshPermission();
        } else {
          _set(_state.value.copyWith(
            isSavingEntry: false,
            saveCompleted: true,
            entryError: null,
            detailError: null,
            validationErrors: const {},
          ));
        }
      } catch (error) {
        _set(_state.value.copyWith(
          isSavingEntry: false,
          entryError: ActivityEntryError.writeFailed,
          detailError: throwableToScreenError(error),
          validationErrors: const {},
        ));
      }
    });
  }

  void onSaveCompletedHandled() =>
      _set(_state.value.copyWith(saveCompleted: false));

  // ── Internal transforms ─────────────────────────────────────────────────

  void _applyRouteImport(RouteFileImport routeImport, UnitSystem unitSystem) {
    _set(activityStateWithRouteImport(
      _state.value,
      routeImport,
      unitSystem,
      clock,
    ));
    refreshPermission();
  }

  void _applyRecordingProgress(ActivityRecordingState recording) {
    final start = recording.startTime;
    if (start == null) return;
    final startDateTime = clock.toZone(start);
    final durationMinutes =
        (_atLeast(recording.elapsedDuration(clock.nowUtc()).inSeconds, 1) / 60.0)
            .ceil()
            .clamp(1, maxActivityDurationMinutes);
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.recording,
      importedRoute: null,
      startDateText: isoLocalDate(startDateTime),
      startTimeText: timeFormatterText(startDateTime),
      durationMinutesText: durationMinutes.toString(),
      entryError:
          recording.errorMessage != null ? ActivityEntryError.recordingFailed : null,
      detailError: recording.errorMessage != null
          ? ScreenErrorMessage(recording.errorMessage!)
          : null,
      validationErrors: const {},
    ));
  }

  void _applyRecordingWithoutRoute(ActivityRecordingSnapshot snapshot) {
    final current = _state.value;
    final start = clock.toZone(snapshot.startTime);
    final durationMinutes =
        (_atLeast(snapshot.endTime.difference(snapshot.startTime).inSeconds, 1) /
                60.0)
            .ceil()
            .clamp(1, maxActivityDurationMinutes);
    final selectedActivityType = activityEntryTypeById(snapshot.activityTypeId) ??
        defaultActivityEntryTypes.firstWhereOrNull(
            (t) => t.exerciseType == snapshot.exerciseType && !t.isRepetitionLike) ??
        defaultActivityEntryTypes
            .firstWhereOrNull((t) => t.exerciseType == snapshot.exerciseType) ??
        current.selectedActivityType;
    final calorieEstimate = (current.activeCaloriesText.trim().isEmpty &&
            current.totalCaloriesText.trim().isEmpty)
        ? activityCalorieEstimate(
            activityType: selectedActivityType,
            distanceMeters: null,
            durationMinutesText: durationMinutes.toString(),
          )
        : null;
    final recordedSets = snapshot.repetitionSets
        .map((set) => ActivityRepetitionSetInput(
              repetitionsText: set.repetitions.toString(),
              restMinutesText: set.restSeconds > 0 ? set.restSeconds.toString() : '',
            ))
        .toList();
    _set(_state.value.copyWith(
      mode: ActivityEntryFormMode.manual,
      selectedActivityType: selectedActivityType,
      importedRoute: null,
      recordedPauseIntervals: snapshot.pauseIntervals,
      recordedLaps: snapshot.manualLaps.map(_lapToExerciseLap).toList(),
      recordedMarkers: snapshot.markers,
      isRecordingDraft: true,
      startDateText: isoLocalDate(start),
      startTimeText: timeFormatterText(start),
      durationMinutesText: durationMinutes.toString(),
      distanceText: '',
      elevationText: '',
      activeCaloriesText:
          calorieEstimate?.activeCaloriesText ?? current.activeCaloriesText,
      totalCaloriesText:
          calorieEstimate?.totalCaloriesText ?? current.totalCaloriesText,
      repetitionMode: recordedSets.isNotEmpty
          ? ActivityRepetitionEntryMode.sets
          : ActivityRepetitionEntryMode.total,
      repetitionTotalText: (selectedActivityType.isRepetitionLike &&
              snapshot.repetitionCount > 0 &&
              recordedSets.isEmpty)
          ? snapshot.repetitionCount.toString()
          : '',
      repetitionSets:
          recordedSets.isNotEmpty ? recordedSets : const [ActivityRepetitionSetInput()],
      recordedBleSamples: snapshot.bleSamples,
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
  }

  void _updateState({
    Set<ActivityEntryField> clearFields = const {},
    required ActivityEntryUiState Function(ActivityEntryUiState) update,
  }) {
    final previous = _state.value;
    final updated = update(previous);
    final permissions = _currentRequiredPermissions();
    _set(updated.copyWith(
      writePermissions: permissions,
      canWrite:
          updated.canWrite && setEquals(permissions, previous.writePermissions),
      validationErrors: updated.validationErrors
          .where((error) => !clearFields.contains(error.field))
          .toSet(),
    ));
  }

  Set<String> _currentRequiredPermissions() =>
      repository.activityWritePermissions();

  ActivityEntryType _preferredActivityType({
    bool requireGpsRoute = false,
    bool requireLiveRecording = false,
  }) =>
      preferredActivityEntryType(
        preferencesRepository,
        requireGpsRoute: requireGpsRoute,
        requireLiveRecording: requireLiveRecording,
      );

  void _rememberLastActivityType(int exerciseType) {
    preferencesRepository?.lastActivityExerciseType = exerciseType;
  }

  ActivityPlannedWorkoutBaseline _planBaseline(
    PlannedExerciseData plan,
    ActivityEntryType activityType,
    String startDateText,
    String startTimeText,
    String durationMinutesText,
    List<ActivityRepetitionSetInput> sets,
  ) =>
      ActivityPlannedWorkoutBaseline(
        planId: plan.id,
        activityTypeId: activityType.id,
        titleText: (plan.title ?? '').trim(),
        notesText: plan.notes ?? '',
        startDateText: startDateText,
        startTimeText: startTimeText,
        durationMinutesText: durationMinutesText,
        repetitionMode: ActivityRepetitionEntryMode.sets,
        repetitionTotalText: '',
        repetitionSets: [
          for (final set in sets)
            set.copyWith(
              repetitionsText: set.repetitionsText.trim(),
              restMinutesText: set.restMinutesText.trim(),
            ),
        ],
      );

  ActivityEntryType? _planActivityType(PlannedExerciseData plan) =>
      plannedWorkoutToActivityEntryType(plan);

  String _planDurationMinutesText(PlannedExerciseData plan) {
    final minutes = _atLeast(Duration(milliseconds: plan.durationMs).inMinutes, 1);
    return minutes.clamp(1, maxActivityDurationMinutes).toString();
  }

  LocalDate _todayLocalDate() => LocalDate.fromDateTime(clock.nowInZone());

  DateTime _truncateToMinute(DateTime value) =>
      DateTime(value.year, value.month, value.day, value.hour, value.minute);

  ExerciseLapData _lapToExerciseLap(ActivityRecordingLap lap) => ExerciseLapData(
        startTime: lap.startTime,
        endTime: lap.endTime,
        lengthMeters: lap.distanceMeters,
      );
}

// ── Pure route-import + preferred-type helpers (tested directly) ────────────

/// Port of the Kotlin `ActivityEntryUiState.withRouteImport` extension. Folds a
/// parsed [routeImport] into [current], returning the route-import form state.
/// Shared by the single-entry controller ([ActivityEntryController.importRouteFile]
/// via `_applyRouteImport`) and the settings bulk importer.
ActivityEntryUiState activityStateWithRouteImport(
  ActivityEntryUiState current,
  RouteFileImport routeImport,
  UnitSystem unitSystem,
  ActivityEntryClock clock,
) {
  final start = clock.toZone(routeImport.startTime);
  final selectedActivityType =
      inferActivityType(routeImport, current.selectedActivityType);
  final String routeDurationMinutes;
  if (routeImport.hasImportedTimeRange) {
    final routeDurationSeconds =
        _atLeast(routeImport.endTime.difference(routeImport.startTime).inSeconds, 1);
    final durationSecondsForDisplay =
        routeImport.points.isNotEmpty && routeImport.hasRecordedTimestamps
            ? routeDurationSeconds + 1
            : routeDurationSeconds;
    routeDurationMinutes = (durationSecondsForDisplay / 60.0)
        .ceil()
        .clamp(1, maxActivityDurationMinutes)
        .toString();
  } else if (routeImport.durationSeconds != null) {
    routeDurationMinutes = (_atLeast(routeImport.durationSeconds!, 1) / 60.0)
        .ceil()
        .clamp(1, maxActivityDurationMinutes)
        .toString();
  } else {
    routeDurationMinutes = current.durationMinutesText.trim().isEmpty
        ? '30'
        : current.durationMinutesText;
  }
  final calorieEstimate = (current.activeCaloriesText.trim().isEmpty &&
          current.totalCaloriesText.trim().isEmpty)
      ? activityCalorieEstimate(
          activityType: selectedActivityType,
          distanceMeters: routeImport.distanceMeters,
          durationMinutesText: routeDurationMinutes,
        )
      : null;
  final importedActiveCaloriesText = (routeImport.activeCaloriesKcal != null &&
          routeImport.activeCaloriesKcal! > 0.0)
      ? toInputText(routeImport.activeCaloriesKcal!, 1)
      : null;
  final importedTotalCaloriesText = (routeImport.totalCaloriesKcal != null &&
          routeImport.totalCaloriesKcal! > 0.0)
      ? toInputText(routeImport.totalCaloriesKcal!, 1)
      : null;

  return current.copyWith(
    mode: ActivityEntryFormMode.routeImport,
    selectedActivityType: selectedActivityType,
    titleText: current.titleText.trim().isNotEmpty
        ? current.titleText
        : (routeImport.name ?? substringBeforeLastDot(routeImport.fileName)),
    notesText: current.notesText.trim().isNotEmpty
        ? current.notesText
        : (routeImport.description ?? ''),
    distanceText: current.distanceText.trim().isNotEmpty
        ? current.distanceText
        : routeDistanceInputText(routeImport, unitSystem),
    elevationText: current.elevationText.trim().isNotEmpty
        ? current.elevationText
        : routeElevationInputText(routeImport, unitSystem),
    activeCaloriesText: current.activeCaloriesText.trim().isNotEmpty
        ? current.activeCaloriesText
        : (importedActiveCaloriesText ?? calorieEstimate?.activeCaloriesText ?? ''),
    totalCaloriesText: current.totalCaloriesText.trim().isNotEmpty
        ? current.totalCaloriesText
        : (importedTotalCaloriesText ?? calorieEstimate?.totalCaloriesText ?? ''),
    importedRoute: routeImport,
    recordedPauseIntervals: const [],
    recordedLaps: const [],
    recordedMarkers: const [],
    isRecordingDraft: false,
    startDateText: routeImport.hasImportedTimeRange
        ? isoLocalDate(start)
        : current.startDateText,
    startTimeText: routeImport.hasImportedTimeRange
        ? timeFormatterText(start)
        : current.startTimeText,
    durationMinutesText: routeDurationMinutes,
    isImportingRoute: false,
    entryError: null,
    detailError: null,
    validationErrors: const {},
  );
}

/// Port of the Kotlin `ActivityEntryViewModel.preferredActivityType`. Picks the
/// favourite/last-used activity type that satisfies the requested capabilities,
/// falling back to the first supported type.
ActivityEntryType preferredActivityEntryType(
  PreferencesRepository? preferencesRepository, {
  bool requireGpsRoute = false,
  bool requireLiveRecording = false,
}) {
  var activityTypes = defaultActivityEntryTypes
      .where((t) =>
          (!requireGpsRoute || t.supportsGpsRoute) &&
          (!requireLiveRecording || t.supportsLiveRecording))
      .toList();
  if (activityTypes.isEmpty) activityTypes = defaultActivityEntryTypes;
  final favorite = preferencesRepository?.favoriteActivityExerciseType;
  final last = preferencesRepository?.lastActivityExerciseType;
  int? preferred;
  if (favorite != null && activityTypes.any((t) => t.exerciseType == favorite)) {
    preferred = favorite;
  } else if (last != null && activityTypes.any((t) => t.exerciseType == last)) {
    preferred = last;
  }
  return activityTypes.firstWhereOrNull((t) => t.exerciseType == preferred) ??
      activityTypes.first;
}

/// Port of the Kotlin `fileName.substringBeforeLast('.', fileName)`.
String substringBeforeLastDot(String? fileName) {
  if (fileName == null) return '';
  final dot = fileName.lastIndexOf('.');
  return dot < 0 ? fileName : fileName.substring(0, dot);
}

// ── Pure planned-workout builders (tested directly) ─────────────────────────

PlannedExerciseWriteRequest? buildPlannedExerciseWriteRequest(
  ActivityEntryUiState state,
  UnitSystem unitSystem, {
  String? updateExistingId,
}) {
  if (!state.selectedActivityType.supportsSetRepetitions) return null;
  if (validatePlannedExerciseWriteRequest(state, unitSystem).isNotEmpty) return null;
  final activityRequest = buildWriteRequest(state, unitSystem);
  if (activityRequest == null) return null;
  final trimmedTitle = state.titleText.trim();
  if (trimmedTitle.isEmpty) return null;
  final segmentType =
      state.selectedActivityType.segmentType ?? ExerciseSegmentType.otherWorkout;
  final List<PlannedExerciseStepData> steps;
  switch (state.repetitionMode) {
    case ActivityRepetitionEntryMode.total:
      final repetitions = int.tryParse(state.repetitionTotalText.trim());
      if (repetitions == null || repetitions <= 0) return null;
      steps = [_repetitionPlanStep(segmentType, repetitions, 1)];
      break;
    case ActivityRepetitionEntryMode.sets:
      final built = <PlannedExerciseStepData>[];
      for (var index = 0; index < state.repetitionSets.length; index++) {
        final set = state.repetitionSets[index];
        final repetitions = int.tryParse(set.repetitionsText.trim());
        if (repetitions == null || repetitions <= 0) return null;
        built.add(_repetitionPlanStep(segmentType, repetitions, index + 1));
        final restSeconds = int.tryParse(set.restMinutesText.trim());
        if (restSeconds != null && restSeconds > 0) {
          built.add(_restPlanStep(restSeconds));
        }
      }
      steps = built;
      break;
  }
  if (steps.isEmpty) return null;
  return PlannedExerciseWriteRequest(
    id: updateExistingId,
    exerciseType: activityRequest.exerciseType,
    startTime: activityRequest.startTime,
    endTime: activityRequest.endTime,
    title: trimmedTitle,
    notes: activityRequest.notes,
    blocks: [
      PlannedExerciseBlockData(
        repetitions: 1,
        description: trimmedTitle,
        steps: steps,
      ),
    ],
  );
}

Set<ActivityEntryValidationError> validatePlannedExerciseWriteRequest(
  ActivityEntryUiState state,
  UnitSystem unitSystem,
) {
  final errors = {...validateActivityEntry(state, unitSystem)};
  if (state.titleText.trim().isEmpty) {
    errors.add(ActivityEntryValidationError.trainingPlanTitleRequired);
  }
  return errors;
}

PlannedExerciseStepData _repetitionPlanStep(
  int segmentType,
  int repetitions,
  int setNumber,
) =>
    PlannedExerciseStepData(
      exerciseType: segmentType,
      exercisePhase: PlannedExerciseStepPhase.active,
      description: 'Set $setNumber',
      completion: PlannedExerciseCompletionRepetitions(repetitions),
    );

PlannedExerciseStepData _restPlanStep(int seconds) => PlannedExerciseStepData(
      exerciseType: ExerciseSegmentType.rest,
      exercisePhase: PlannedExerciseStepPhase.rest,
      description: 'Rest',
      completion: PlannedExerciseCompletionDurationSeconds(seconds),
    );

List<ActivityRepetitionSetInput> plannedWorkoutToRepetitionSetInputs(
  PlannedExerciseData plan,
) {
  final sets = <ActivityRepetitionSetInput>[];
  for (final block in plan.blocks) {
    final repeats = block.repetitions < 1 ? 1 : block.repetitions;
    for (var i = 0; i < repeats; i++) {
      for (final step in block.steps) {
        final completion = step.completion;
        if (completion is PlannedExerciseCompletionRepetitions) {
          sets.add(ActivityRepetitionSetInput(
            repetitionsText: completion.repetitions.toString(),
          ));
        } else if (completion is PlannedExerciseCompletionDurationSeconds) {
          if (sets.isEmpty) continue;
          sets[sets.length - 1] = sets.last
              .copyWith(restMinutesText: completion.seconds.toString());
        }
      }
    }
  }
  return sets;
}

ActivityEntryType? plannedWorkoutToActivityEntryType(PlannedExerciseData plan) {
  int? activeSegmentType;
  outer:
  for (final block in plan.blocks) {
    for (final step in block.steps) {
      if (step.exercisePhase == PlannedExerciseStepPhase.active &&
          step.completion is PlannedExerciseCompletionRepetitions) {
        activeSegmentType = step.exerciseType;
        break outer;
      }
    }
  }
  return defaultActivityEntryTypes.firstWhereOrNull((t) =>
          t.exerciseType == plan.exerciseType &&
          t.segmentType != null &&
          t.segmentType == activeSegmentType) ??
      defaultActivityEntryTypes.firstWhereOrNull(
          (t) => t.exerciseType == plan.exerciseType && t.supportsSetRepetitions) ??
      defaultActivityEntryTypes
          .firstWhereOrNull((t) => t.exerciseType == plan.exerciseType);
}

int _atLeast(int value, int min) => value < min ? min : value;

extension _FirstWhereOrNull<E> on List<E> {
  E? firstWhereOrNull(bool Function(E) test) {
    for (final element in this) {
      if (test(element)) return element;
    }
    return null;
  }
}
