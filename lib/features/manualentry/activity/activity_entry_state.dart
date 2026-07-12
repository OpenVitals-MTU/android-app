import 'package:collection/collection.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/ble_sensor_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'routeimport/route_file_parser.dart';

part 'activity_entry_state.freezed.dart';

/// Port of the Kotlin `ActivityEntryState.kt` — the manual-entry / recording
/// screen UI state plus its supporting enums and value types.

enum ActivityEntryError {
  invalidValue,
  missingWritePermission,
  routeImportFailed,
  locationPermissionNeeded,
  notificationPermissionNeeded,
  activityRecognitionPermissionNeeded,
  recordingFailed,
  writeFailed,
}

/// The screen's high-level form mode (Kotlin `ActivityEntryMode`; renamed to
/// avoid clashing with the navigation-intent `ActivityEntryMode`).
enum ActivityEntryFormMode {
  chooseSource,
  planActivityPicker,
  planPicker,
  manual,
  routeImport,
  recording,
}

enum ActivityEntryField {
  activityType,
  title,
  startDate,
  startTime,
  duration,
  repetitions,
  distance,
  elevation,
  activeCalories,
  totalCalories,
}

enum ActivityEntryValidationError {
  activityTypeDoesNotSupportRoute(ActivityEntryField.activityType),
  trainingPlanTitleRequired(ActivityEntryField.title),
  startDateInvalid(ActivityEntryField.startDate),
  startTimeInvalid(ActivityEntryField.startTime),
  startTimeAfterRouteStart(ActivityEntryField.startTime),
  durationInvalid(ActivityEntryField.duration),
  repetitionsInvalid(ActivityEntryField.repetitions),
  distanceInvalid(ActivityEntryField.distance),
  distanceUnsupported(ActivityEntryField.distance),
  elevationInvalid(ActivityEntryField.elevation),
  elevationUnsupported(ActivityEntryField.elevation),
  activeCaloriesInvalid(ActivityEntryField.activeCalories),
  totalCaloriesInvalid(ActivityEntryField.totalCalories),
  totalCaloriesBelowActive(ActivityEntryField.totalCalories);

  const ActivityEntryValidationError(this.field);

  final ActivityEntryField field;
}

enum ActivityRepetitionEntryMode { total, sets }

enum ActivityEntryFeeling {
  great('😀', 'Great', 'Felt great.'),
  good('🙂', 'Good', 'Felt good.'),
  hard('😓', 'Hard', 'Felt hard.'),
  rough('😖', 'Rough', 'Felt rough.');

  const ActivityEntryFeeling(this.emoji, this.label, this.noteText);

  final String emoji;
  final String label;
  final String noteText;
}

/// Port of the Kotlin `ActivityRepetitionSetInput`.
class ActivityRepetitionSetInput {
  const ActivityRepetitionSetInput({
    this.repetitionsText = '',
    this.restMinutesText = '',
  });

  final String repetitionsText;
  final String restMinutesText;

  ActivityRepetitionSetInput copyWith({
    String? repetitionsText,
    String? restMinutesText,
  }) =>
      ActivityRepetitionSetInput(
        repetitionsText: repetitionsText ?? this.repetitionsText,
        restMinutesText: restMinutesText ?? this.restMinutesText,
      );

  @override
  bool operator ==(Object other) =>
      other is ActivityRepetitionSetInput &&
      other.repetitionsText == repetitionsText &&
      other.restMinutesText == restMinutesText;

  @override
  int get hashCode => Object.hash(repetitionsText, restMinutesText);
}

/// Port of the Kotlin `ActivityPlannedWorkoutBaseline`.
class ActivityPlannedWorkoutBaseline {
  const ActivityPlannedWorkoutBaseline({
    required this.planId,
    required this.activityTypeId,
    required this.titleText,
    required this.notesText,
    required this.startDateText,
    required this.startTimeText,
    required this.durationMinutesText,
    required this.repetitionMode,
    required this.repetitionTotalText,
    required this.repetitionSets,
  });

  final String planId;
  final String activityTypeId;
  final String titleText;
  final String notesText;
  final String startDateText;
  final String startTimeText;
  final String durationMinutesText;
  final ActivityRepetitionEntryMode repetitionMode;
  final String repetitionTotalText;
  final List<ActivityRepetitionSetInput> repetitionSets;

  @override
  bool operator ==(Object other) =>
      other is ActivityPlannedWorkoutBaseline &&
      other.planId == planId &&
      other.activityTypeId == activityTypeId &&
      other.titleText == titleText &&
      other.notesText == notesText &&
      other.startDateText == startDateText &&
      other.startTimeText == startTimeText &&
      other.durationMinutesText == durationMinutesText &&
      other.repetitionMode == repetitionMode &&
      other.repetitionTotalText == repetitionTotalText &&
      const ListEquality<ActivityRepetitionSetInput>()
          .equals(other.repetitionSets, repetitionSets);

  @override
  int get hashCode => Object.hash(
        planId,
        activityTypeId,
        titleText,
        notesText,
        startDateText,
        startTimeText,
        durationMinutesText,
        repetitionMode,
        repetitionTotalText,
        Object.hashAll(repetitionSets),
      );
}

@freezed
abstract class ActivityEntryUiState with _$ActivityEntryUiState {
  const ActivityEntryUiState._();

  const factory ActivityEntryUiState({
    @Default(ActivityEntryFormMode.chooseSource) ActivityEntryFormMode mode,
    required ActivityEntryType selectedActivityType,
    @Default('') String titleText,
    ActivityEntryFeeling? selectedFeeling,
    @Default('') String notesText,
    @Default('') String startDateText,
    @Default('') String startTimeText,
    @Default('30') String durationMinutesText,
    @Default('') String distanceText,
    @Default('') String elevationText,
    @Default('') String activeCaloriesText,
    @Default('') String totalCaloriesText,
    @Default(ActivityRepetitionEntryMode.total)
    ActivityRepetitionEntryMode repetitionMode,
    @Default('') String repetitionTotalText,
    @Default([ActivityRepetitionSetInput()])
    List<ActivityRepetitionSetInput> repetitionSets,
    @Default(<PlannedExerciseData>[]) List<PlannedExerciseData> plannedWorkouts,
    String? selectedPlannedWorkoutId,
    ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline,
    String? selectedPlannedWorkoutActivityTypeId,
    @Default(false) bool isLoadingPlannedWorkouts,
    @Default(false) bool isSavingPlannedWorkout,
    RouteFileImport? importedRoute,
    @Default(<ActivityPauseInterval>[])
    List<ActivityPauseInterval> recordedPauseIntervals,
    @Default(<ExerciseLapData>[]) List<ExerciseLapData> recordedLaps,
    @Default(<ActivityRecordingMarker>[])
    List<ActivityRecordingMarker> recordedMarkers,
    @Default(<String>{}) Set<String> writePermissions,
    @Default(false) bool canWrite,
    @Default(true) bool isCheckingPermission,

    /// The route/FIT file import: its own command, because it is a second
    /// failable action on this form and it fails for its own reasons.
    @Default(CommandState<void>.idle()) CommandState<void> routeImport,

    /// Writing (or updating) the activity. [CommandSuccess] is consumed exactly
    /// once by the screen, which then leaves the route.
    @Default(CommandState<void>.idle()) CommandState<void> save,
    ActivityEntryError? entryError,

    /// The detail behind a non-command [entryError] — a permission probe, a
    /// planned-workout read, a recording that would not start. A save or an
    /// import that failed carries its own [ScreenError] in its command; read
    /// them together through [blockingError].
    ScreenError? detailError,
    @Default(<ActivityEntryValidationError>{})
    Set<ActivityEntryValidationError> validationErrors,
    String? editRecordId,
    @Default(false) bool isRecordingDraft,
    @Default(BleRecordingSampleBuffer()) BleRecordingSampleBuffer recordedBleSamples,
    @Default(<HeartRateSample>[]) List<HeartRateSample> sessionHeartRateSamples,
  }) = _ActivityEntryUiState;

  List<ActivityEntryType> get activityTypes => defaultActivityEntryTypes;

  bool get isSavingEntry => save is CommandRunning<void>;

  bool get isImportingRoute => routeImport is CommandRunning<void>;

  /// The error line the form renders under [entryError]: whichever of the two
  /// commands failed, else the detail of the last non-command failure.
  ScreenError? get blockingError => switch (save) {
        CommandFailure<void>(:final error) => error,
        _ => switch (routeImport) {
            CommandFailure<void>(:final error) => error,
            _ => detailError,
          },
      };

  List<ExerciseRoutePoint> get routePoints =>
      importedRoute?.points ?? const [];

  bool get isEditMode => editRecordId != null;

  bool get hasSelectedPlannedWorkoutChanges {
    final planId = selectedPlannedWorkoutId;
    if (planId == null) return false;
    final baseline = selectedPlannedWorkoutBaseline;
    if (baseline == null || baseline.planId != planId) return false;
    return plannedWorkoutBaseline(planId) != baseline;
  }

  /// Port of the Kotlin `ActivityEntryUiState.activitySaveNotes`.
  String? activitySaveNotes() {
    final feelingText = selectedFeeling?.noteText;
    final trimmedNote = notesText.trim();
    final noteText = trimmedNote.isEmpty ? null : trimmedNote;
    final parts = <String>[
      ?feelingText,
      ?noteText,
    ];
    final joined = parts.join('\n\n');
    return joined.isEmpty ? null : joined;
  }

  /// Port of the Kotlin `ActivityEntryUiState.plannedWorkoutBaseline`.
  ActivityPlannedWorkoutBaseline plannedWorkoutBaseline(String planId) =>
      ActivityPlannedWorkoutBaseline(
        planId: planId,
        activityTypeId: selectedActivityType.id,
        titleText: titleText.trim(),
        notesText: activitySaveNotes() ?? '',
        startDateText: startDateText.trim(),
        startTimeText: startTimeText.trim(),
        durationMinutesText: durationMinutesText.trim(),
        repetitionMode: repetitionMode,
        repetitionTotalText: repetitionTotalText.trim(),
        repetitionSets: [
          for (final set in repetitionSets)
            set.copyWith(
              repetitionsText: set.repetitionsText.trim(),
              restMinutesText: set.restMinutesText.trim(),
            ),
        ],
      );
}
