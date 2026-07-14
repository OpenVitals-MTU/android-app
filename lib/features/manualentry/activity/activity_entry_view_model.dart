import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../data/repository/contract/activity_repository.dart';
import '../../../data/repository/contract/heart_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/preferences/activity_recording_dashboard_layout.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../state/app_providers.dart';
import 'activity_entry_clock.dart';
import 'activity_entry_edit_mapper.dart';
import 'activity_entry_providers.dart';
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

/// Wraps route-file I/O so the view-model can be tested with a fake. Mirrors the
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

/// Port of the Kotlin `ActivityEntryViewModel`. Owns the manual-entry /
/// route-import / recording form: every dependency comes from a provider, and
/// the screen holds nothing but the [NotifierProvider] built with its route
/// arguments (see `activity_entry_screen.dart`).
///
/// The two failable actions each carry their own [CommandState]:
/// [ActivityEntryUiState.save] and [ActivityEntryUiState.routeImport]. The
/// recording controller is NOT owned here — this view-model drives the same
/// `ActivityRecordingController` the recording screens do, and listens to it.
class ActivityEntryViewModel extends Notifier<ActivityEntryUiState> {
  ActivityEntryViewModel({
    this.editActivityId,
    this.launchMode,
    this.launchPlanId,
    this.launchActivityTypeId,
  });

  /// The record being edited (the edit route), or null for a new entry.
  final String? editActivityId;

  /// The new-entry route's launch intent: `record` / `manual` / `plan`, an
  /// optional plan to start from, and an optional preselected activity type.
  final String? launchMode;
  final String? launchPlanId;
  final String? launchActivityTypeId;

  ActivityRepository get _repository => ref.read(activityRepositoryProvider);
  HeartRepository get _heartRepository => ref.read(heartRepositoryProvider);
  RouteFileImporter get _routeFileImporter => ref.read(routeFileImporterProvider);
  ActivityRecordingController get _activityRecorder =>
      ref.read(activityRecordingControllerProvider);
  ActivityRecordingDraftStore get _recordingDraftStore =>
      ref.read(activityRecordingDraftStoreProvider);
  PreferencesRepository get _preferencesRepository =>
      ref.read(preferencesRepositoryProvider);
  ActivityMarkerRepository get _markerRepository =>
      ref.read(activityMarkerRepositoryProvider);
  ActivityEntryClock get _clock => ref.read(activityEntryClockProvider);

  bool _editEntryLoaded = false;
  final Set<Future<void>> _pending = <Future<void>>{};

  /// The last state published, so the draft store can be written on dispose
  /// (where `state` is no longer readable).
  ActivityEntryUiState? _latest;

  @override
  ActivityEntryUiState build() {
    final draftStore = _recordingDraftStore;
    final recorder = _activityRecorder;

    void listener() {
      final recording = recorder.state.value;
      if (recording.isActive) _applyRecordingProgress(recording);
    }

    recorder.state.addListener(listener);
    ref.onDispose(() {
      recorder.state.removeListener(listener);
      final last = _latest;
      if (last != null) draftStore.store(last);
    });

    final initial = _initialState(draftStore.restore());
    _latest = initial;
    // Riverpod forbids writing `state` while `build` runs, so the init the
    // Kotlin constructor did inline is deferred by one microtask.
    Future.microtask(() {
      if (!ref.mounted) return;
      refreshPermission();
      _applyLaunchIntent();
      if (editActivityId != null) loadEditEntry(ref.read(unitSystemProvider));
    });
    return initial;
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

  void _launch(Future<void> Function() body) {
    late final Future<void> future;
    future = body().whenComplete(() => _pending.remove(future));
    _pending.add(future);
  }

  void _set(ActivityEntryUiState next) {
    if (!ref.mounted) return;
    _latest = next;
    state = next;
  }

  // ── Init / launch intent ────────────────────────────────────────────────

  void _applyLaunchIntent() {
    if (editActivityId != null) return;
    if (state.isRecordingDraft) return;

    final typeId = launchActivityTypeId;
    if (typeId != null) {
      final type = activityEntryTypeById(typeId);
      if (type != null) {
        _set(state.copyWith(selectedActivityType: type));
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
        writePermissions: _repository.activityWritePermissions(),
        canWrite: false,
        isCheckingPermission: true,
        save: const CommandState.idle(),
        routeImport: const CommandState.idle(),
        entryError: null,
        detailError: null,
        validationErrors: const {},
        editRecordId: null,
      );
    }
    return initialActivityEntryState(
      _clock,
      _repository.activityWritePermissions(),
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
      _set(state.copyWith(
        isCheckingPermission: true,
        writePermissions: permissions,
        detailError: null,
      ));
      switch (await _repository.hasActivityWritePermission()) {
        case Ok(:final value):
          _set(state.copyWith(
            isCheckingPermission: false,
            canWrite: value,
            writePermissions: _currentRequiredPermissions(),
          ));
        case Err(:final failure):
          _set(state.copyWith(
            isCheckingPermission: false,
            canWrite: false,
            entryError: ActivityEntryError.writeFailed,
            detailError: failure.toScreenError(),
            writePermissions: _currentRequiredPermissions(),
          ));
      }
    });
  }

  // ── Source selection ────────────────────────────────────────────────────

  void selectActivityType(ActivityEntryType type) {
    final current = state;
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
    _recordingDraftStore.clear();
    _set(state.copyWith(
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
    _recordingDraftStore.clear();
    _launch(() async {
      _set(state.copyWith(
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
      switch (await _repository.loadExistingPlannedWorkouts(
        anchorDate: _todayLocalDate(),
      )) {
        case Ok(:final value):
          _set(state.copyWith(
            plannedWorkouts: value,
            isLoadingPlannedWorkouts: false,
          ));
          _autoAdvancePlanSelection(value);
        case Err(:final failure):
          _setPlannedWorkoutFailure(failure);
      }
    });
  }

  void startWithPlan(String planId) {
    _recordingDraftStore.clear();
    _launch(() async {
      _set(state.copyWith(
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
      switch (await _repository.loadExistingPlannedWorkouts(
        anchorDate: _todayLocalDate(),
      )) {
        case Ok(:final value):
          _set(state.copyWith(
            plannedWorkouts: value,
            isLoadingPlannedWorkouts: false,
          ));
          if (value.any((plan) => plan.id == planId)) {
            applyPlannedWorkout(planId);
          } else {
            _autoAdvancePlanSelection(value);
          }
        case Err(:final failure):
          _setPlannedWorkoutFailure(failure);
      }
    });
  }

  /// A planned-workout read or write that came back [Err]: the form falls back
  /// to the planned-workout permission set, exactly as the Kotlin does.
  void _setPlannedWorkoutFailure(AppFailure failure) {
    _set(state.copyWith(
      plannedWorkouts: const [],
      isLoadingPlannedWorkouts: false,
      isSavingPlannedWorkout: false,
      writePermissions: _repository.plannedWorkoutWritePermissions(),
      canWrite: false,
      entryError: failure is PermissionFailure
          ? ActivityEntryError.missingWritePermission
          : ActivityEntryError.writeFailed,
      detailError: failure.toScreenError(),
    ));
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
    _set(state.copyWith(
      mode: ActivityEntryFormMode.planPicker,
      selectedPlannedWorkoutActivityTypeId: typeId,
      selectedPlannedWorkoutId: null,
      entryError: null,
      detailError: null,
    ));
  }

  void choosePlannedWorkoutActivity() {
    _set(state.copyWith(
      mode: ActivityEntryFormMode.planActivityPicker,
      selectedPlannedWorkoutActivityTypeId: null,
      selectedPlannedWorkoutId: null,
      entryError: null,
      detailError: null,
    ));
  }

  void chooseSource() {
    if (state.isEditMode) return;
    _recordingDraftStore.clear();
    _activityRecorder.stopBlePreview();
    _activityRecorder.clearPreparedRecording();
    _set(initialActivityEntryState(
      _clock,
      _repository.activityWritePermissions(),
      selectedActivityType: _preferredActivityType(),
    ).copyWith(
      canWrite: state.canWrite,
      isCheckingPermission: state.isCheckingPermission,
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

  /// The route-import command: the file picked in Settings → Data import is
  /// handed here (see `pendingRouteImportProvider`), and so is a file picked on
  /// this screen.
  void importRouteFile(ActivityRouteFileHandle handle, UnitSystem unitSystem) {
    final importer = _routeFileImporter;
    _recordingDraftStore.clear();
    _launch(() async {
      _set(state.copyWith(
        routeImport: const CommandState.running(),
        isRecordingDraft: false,
        entryError: null,
        detailError: null,
        validationErrors: const {},
      ));
      // The parsers throw on a malformed file rather than returning `Result` —
      // they are not repositories, and the throw IS the parse verdict.
      try {
        final routeImport = await importer.import(handle);
        _applyRouteImport(routeImport, unitSystem);
      } catch (error) {
        _set(state.copyWith(
          routeImport: CommandState.failure(throwableToScreenError(error)),
          entryError: ActivityEntryError.routeImportFailed,
          detailError: null,
          validationErrors: const {},
        ));
      }
    });
  }

  void clearImportedRoute() {
    _set(state.copyWith(
      mode: ActivityEntryFormMode.manual,
      importedRoute: null,
      recordedPauseIntervals: const [],
      recordedLaps: const [],
      recordedMarkers: const [],
      routeImport: const CommandState.idle(),
      entryError: null,
      detailError: null,
      validationErrors: const {},
    ));
    refreshPermission();
  }

  // ── Planned workouts ────────────────────────────────────────────────────

  void refreshPlannedWorkouts() {
    final snapshot = state;
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
      _set(state.copyWith(isLoadingPlannedWorkouts: true));
      switch (await _repository.loadPlannedWorkoutOptions(
        date,
        snapshot.selectedActivityType.exerciseType,
      )) {
        case Ok(:final value):
          final currentSelectedId = state.selectedPlannedWorkoutId;
          final selectedId = currentSelectedId != null &&
                  (value.isEmpty ||
                      value.any((p) => p.id == currentSelectedId))
              ? currentSelectedId
              : null;
          _set(state.copyWith(
            plannedWorkouts: value,
            selectedPlannedWorkoutId: selectedId,
            isLoadingPlannedWorkouts: false,
          ));
        case Err(:final failure):
          _set(state.copyWith(
            plannedWorkouts: const [],
            selectedPlannedWorkoutId: null,
            isLoadingPlannedWorkouts: false,
            detailError: failure.toScreenError(),
          ));
      }
    });
  }

  void applyPlannedWorkout(String planId) {
    final plan = state.plannedWorkouts.firstWhereOrNull((p) => p.id == planId);
    if (plan == null) return;
    final sets = plannedWorkoutToRepetitionSetInputs(plan);
    if (sets.isEmpty) return;
    final activityType = _planActivityType(plan) ?? state.selectedActivityType;
    final selectedAt = _truncateToMinute(_clock.nowInZone());
    final startDateText = isoLocalDate(selectedAt);
    final startTimeText = timeFormatterText(selectedAt);
    final durationMinutesText = _planDurationMinutesText(plan);
    _set(state.copyWith(
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
    final current = state;
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
    final current = state;
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
      _set(state.copyWith(
        isSavingPlannedWorkout: true,
        entryError: null,
        detailError: null,
      ));
      switch (await _repository.writePlannedWorkout(request)) {
        case Ok(:final value):
          final savedState = state;
          _set(savedState.copyWith(
            selectedPlannedWorkoutId: value,
            selectedPlannedWorkoutBaseline:
                savedState.plannedWorkoutBaseline(value),
            isSavingPlannedWorkout: false,
            detailError: null,
          ));
          refreshPlannedWorkouts();
        case Err(:final failure):
          _setPlannedWorkoutFailure(failure);
      }
    });
  }

  // ── Permission-driven error surfaces ────────────────────────────────────

  void reportLocationPermissionNeeded() => _set(state.copyWith(
        entryError: ActivityEntryError.locationPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  void reportNotificationPermissionNeeded() => _set(state.copyWith(
        entryError: ActivityEntryError.notificationPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  void reportActivityRecognitionPermissionNeeded() => _set(state.copyWith(
        entryError: ActivityEntryError.activityRecognitionPermissionNeeded,
        detailError: null,
        validationErrors: const {},
      ));

  // ── Recording ───────────────────────────────────────────────────────────
  //
  // The recorder itself is a LATER phase: these methods keep talking to the
  // `ActivityRecordingController` exactly as the Kotlin ViewModel did.

  void prepareGpsRecording() {
    final current = state;
    _recordingDraftStore.clear();
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
    _activityRecorder.clearPreparedRecording();
    _activityRecorder.previewBleConnections();
  }

  void openRecordingDashboard({int repetitionRestSeconds = 0}) {
    final current = state;
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
    _recordingDraftStore.clear();
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
    _activityRecorder.prepareRecordingDashboard(current.selectedActivityType);
  }

  Future<void> startGpsRecording({
    ActivityRecordingInitialFix? initialFix,
    int repetitionRestSeconds = 0,
  }) async {
    final recorder = _activityRecorder;
    final current = state;
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
    _recordingDraftStore.clear();
    final now = _truncateToMinute(_clock.nowInZone());
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
      _set(state.copyWith(
        entryError: ActivityEntryError.recordingFailed,
        detailError: message != null ? ScreenErrorMessage(message) : null,
        validationErrors: const {},
      ));
    }
  }

  void pauseGpsRecording() => _activityRecorder.pauseRecording();
  void resumeGpsRecording() => _activityRecorder.resumeRecording();
  void addRecordingLap() => _activityRecorder.addManualLap();
  void addRecordingMarker() => _activityRecorder.addMarker();
  void updateRecordingMarker(ActivityRecordingMarker marker) =>
      _activityRecorder.updateMarker(marker);
  void deleteRecordingMarker(String markerId) =>
      _activityRecorder.deleteMarker(markerId);
  void adjustRepetitionRecording(int delta) =>
      _activityRecorder.adjustRepetitionCount(delta);
  void endRepetitionSet() => _activityRecorder.endRepetitionSet();
  void startNextRepetitionSet() => _activityRecorder.startNextRepetitionSet();

  /// Kotlin `ActivityEntryViewModel.updateRecordingDashboardLayout`.
  void updateRecordingDashboardLayout(ActivityRecordingDashboardLayout layout) =>
      _activityRecorder.updateDashboardLayout(layout);

  void discardGpsRecording() {
    _activityRecorder.discardRecording();
    _activityRecorder.stopBlePreview();
    _recordingDraftStore.clear();
    chooseSource();
  }

  void discardRecordingDraft() {
    if (!state.isRecordingDraft || state.isEditMode) return;
    _recordingDraftStore.clear();
    chooseSource();
  }

  void finishGpsRecording(UnitSystem unitSystem) {
    final snapshot = _activityRecorder.finishRecording();
    if (snapshot == null) {
      _set(state.copyWith(
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
      _set(state.copyWith(
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
    _recordingDraftStore.store(state);
  }

  // ── Edit ────────────────────────────────────────────────────────────────

  void loadEditEntry(UnitSystem unitSystem) {
    final recordId = editActivityId;
    if (recordId == null || _editEntryLoaded) return;
    _editEntryLoaded = true;
    _launch(() async {
      final ExerciseData? workout;
      switch (await _repository.loadWorkout(recordId)) {
        case Ok(:final value):
          workout = value;
        case Err(:final failure):
          _setEditLoadFailure(failure.toScreenError());
          return;
      }
      if (workout == null || !workout.isOpenVitalsEntry) {
        _setEditLoadFailure(
          const ScreenErrorMessage('Only OpenVitals entries can be edited.'),
        );
        return;
      }
      final List<HeartRateSample> heartRateSamples;
      switch (await _heartRepository.loadHeartRateSamplesInstant(
        workout.startTime,
        workout.endTime,
      )) {
        case Ok(:final value):
          heartRateSamples = value;
        case Err(:final failure):
          _setEditLoadFailure(failure.toScreenError());
          return;
      }
      final current = state;
      var editState = exerciseToEditState(
        workout,
        unitSystem: unitSystem,
        clock: _clock,
        repository: _repository,
        canWrite: current.canWrite,
        isCheckingPermission: current.isCheckingPermission,
      ).copyWith(sessionHeartRateSamples: heartRateSamples);
      final markers = _markerRepository.markersForActivity(recordId);
      final resolvedMarkers = markers.isNotEmpty
          ? markers
          : (workout.clientRecordId != null
              ? _markerRepository.markersForActivity(workout.clientRecordId!)
              : const <ActivityRecordingMarker>[]);
      editState = editState.copyWith(recordedMarkers: resolvedMarkers);
      _set(editState);
      refreshPlannedWorkouts();
    });
  }

  /// The edit prefill could not be read. It is not a failed *write*, but the
  /// form still cannot be trusted, so it is reported on the same error line.
  void _setEditLoadFailure(ScreenError error) {
    _set(state.copyWith(
      entryError: ActivityEntryError.writeFailed,
      detailError: error,
      validationErrors: const {},
    ));
  }

  // ── Save ────────────────────────────────────────────────────────────────

  void addEntry(UnitSystem unitSystem) {
    if (state.mode == ActivityEntryFormMode.chooseSource) {
      _set(state.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: const {},
      ));
      return;
    }
    final validationErrors = validateActivityEntry(state, unitSystem);
    if (validationErrors.isNotEmpty) {
      _set(state.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: validationErrors,
      ));
      return;
    }
    final request = buildWriteRequest(state, unitSystem);
    if (request == null) {
      _set(state.copyWith(
        entryError: ActivityEntryError.invalidValue,
        detailError: null,
        validationErrors: validationErrors,
      ));
      return;
    }
    final editRecordId = state.editRecordId;
    final wasRecordingDraft = state.isRecordingDraft;
    final markersToSave = state.selectedActivityType.supportsGpsRoute
        ? state.recordedMarkers
        : const <ActivityRecordingMarker>[];
    final requestPermissions =
        _repository.activityWritePermissionsForRequest(request);

    _launch(() async {
      _set(state.copyWith(
        save: const CommandState.running(),
        entryError: null,
        detailError: null,
        validationErrors: const {},
        writePermissions: requestPermissions,
      ));
      final bool hasPermission;
      switch (await _repository.hasActivityWritePermissionForRequest(request)) {
        case Ok(:final value):
          hasPermission = value;
        case Err(:final failure):
          _setSaveFailure(failure);
          return;
      }
      if (!hasPermission) {
        // A refusal, not a failure: the command goes back to rest and the form
        // asks for the permission.
        _set(state.copyWith(
          save: const CommandState.idle(),
          canWrite: false,
          entryError: ActivityEntryError.missingWritePermission,
          detailError: null,
          validationErrors: const {},
        ));
        return;
      }
      final String savedActivityId;
      if (editRecordId == null) {
        switch (await _repository.writeActivityEntry(request)) {
          case Ok(:final value):
            savedActivityId = value;
          case Err(:final failure):
            _setSaveFailure(failure);
            return;
        }
      } else {
        switch (await _repository.updateActivityEntry(editRecordId, request)) {
          case Ok():
            savedActivityId = editRecordId;
          case Err(:final failure):
            _setSaveFailure(failure);
            return;
        }
      }
      _markerRepository.setMarkersForActivity(savedActivityId, markersToSave);
      _recordingDraftStore.clear();
      if (wasRecordingDraft) {
        _rememberLastActivityType(request.exerciseType);
      }
      if (editRecordId == null) {
        _set(clearedAfterSaveState(
          _clock,
          _repository.activityWritePermissions(),
          _preferredActivityType(),
        ).copyWith(save: const CommandState.success(null)));
        refreshPermission();
      } else {
        _set(state.copyWith(
          save: const CommandState.success(null),
          entryError: null,
          detailError: null,
          validationErrors: const {},
        ));
      }
    });
  }

  void _setSaveFailure(AppFailure failure) {
    _set(state.copyWith(
      save: CommandState.failure(failure.toScreenError()),
      entryError: ActivityEntryError.writeFailed,
      detailError: null,
      validationErrors: const {},
    ));
  }

  /// The screen consumed the success (it left the route), so the command returns
  /// to rest — otherwise re-entering the route would fire it again.
  void onSaveCompletedHandled() =>
      _set(state.copyWith(save: const CommandState.idle()));

  // ── Internal transforms ─────────────────────────────────────────────────

  void _applyRouteImport(RouteFileImport routeImport, UnitSystem unitSystem) {
    _set(activityStateWithRouteImport(
      state,
      routeImport,
      unitSystem,
      _clock,
    ));
    refreshPermission();
  }

  void _applyRecordingProgress(ActivityRecordingState recording) {
    final start = recording.startTime;
    if (start == null) return;
    final startDateTime = _clock.toZone(start);
    final durationMinutes =
        (_atLeast(recording.elapsedDuration(_clock.nowUtc()).inSeconds, 1) / 60.0)
            .ceil()
            .clamp(1, maxActivityDurationMinutes);
    _set(state.copyWith(
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
    final current = state;
    final start = _clock.toZone(snapshot.startTime);
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
    _set(state.copyWith(
      mode: ActivityEntryFormMode.manual,
      selectedActivityType: selectedActivityType,
      importedRoute: null,
      recordedPauseIntervals: snapshot.pauseIntervals,
      recordedLaps: snapshot.manualLaps.map(_lapToExerciseLap).toList(),
      recordedMarkers: snapshot.markers,
      recordedRecoveryStartTime: snapshot.hrrEffortEndedAt == null
          ? null
          : _clock.toZone(snapshot.hrrEffortEndedAt!),
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
    final previous = state;
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
      _repository.activityWritePermissions();

  ActivityEntryType _preferredActivityType({
    bool requireGpsRoute = false,
    bool requireLiveRecording = false,
  }) =>
      preferredActivityEntryType(
        _preferencesRepository,
        requireGpsRoute: requireGpsRoute,
        requireLiveRecording: requireLiveRecording,
      );

  void _rememberLastActivityType(int exerciseType) {
    _preferencesRepository.lastActivityExerciseType = exerciseType;
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

  LocalDate _todayLocalDate() => LocalDate.fromDateTime(_clock.nowInZone());

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
/// Shared by the single-entry view-model ([ActivityEntryViewModel.importRouteFile]
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
  final importedActiveCaloriesText = (routeImport.activeCaloriesKcal != null &&
          routeImport.activeCaloriesKcal! > 0.0)
      ? toInputText(routeImport.activeCaloriesKcal!, 1)
      : null;
  final importedTotalCaloriesText = (routeImport.totalCaloriesKcal != null &&
          routeImport.totalCaloriesKcal! > 0.0)
      ? toInputText(routeImport.totalCaloriesKcal!, 1)
      : null;
  // The estimate fills in for a file that measured NO calories at all. It must
  // never stand beside a number the file did measure.
  //
  // A FIT session records `total_calories` and has no active-calorie field, so
  // active came back null — and the estimate then filled it, from METs and
  // distance, with a number that had nothing to do with the file's total. An
  // indoor run arrived as 226 active against its own measured 208 total, and the
  // write was refused ("total cannot be lower than active"): a real activity, a
  // real total, and an invented active that contradicted it. Every FIT file with
  // no GPS failed this way, which is every treadmill run and every trainer ride.
  //
  // So: estimate both, or estimate neither. A measurement does not get a
  // guess for a neighbour.
  final fileMeasuredCalories =
      importedActiveCaloriesText != null || importedTotalCaloriesText != null;
  final calorieEstimate = (current.activeCaloriesText.trim().isEmpty &&
          current.totalCaloriesText.trim().isEmpty &&
          !fileMeasuredCalories)
      ? activityCalorieEstimate(
          activityType: selectedActivityType,
          distanceMeters: routeImport.distanceMeters,
          durationMinutesText: routeDurationMinutes,
        )
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
    routeImport: const CommandState.idle(),
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
