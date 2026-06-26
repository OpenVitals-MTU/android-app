package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.R

enum class ActivityEntryError {
    INVALID_VALUE,
    MISSING_WRITE_PERMISSION,
    ROUTE_IMPORT_FAILED,
    LOCATION_PERMISSION_NEEDED,
    NOTIFICATION_PERMISSION_NEEDED,
    ACTIVITY_RECOGNITION_PERMISSION_NEEDED,
    RECORDING_FAILED,
    WRITE_FAILED,
}

enum class ActivityEntryMode {
    CHOOSE_SOURCE,
    PLAN_ACTIVITY_PICKER,
    PLAN_PICKER,
    MANUAL,
    ROUTE_IMPORT,
    RECORDING,
}

enum class ActivityEntryField {
    ACTIVITY_TYPE,
    TITLE,
    START_DATE,
    START_TIME,
    DURATION,
    REPETITIONS,
    DISTANCE,
    ELEVATION,
    ACTIVE_CALORIES,
    TOTAL_CALORIES,
}

enum class ActivityEntryValidationError(
    val field: ActivityEntryField,
) {
    ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE(ActivityEntryField.ACTIVITY_TYPE),
    TRAINING_PLAN_TITLE_REQUIRED(ActivityEntryField.TITLE),
    START_DATE_INVALID(ActivityEntryField.START_DATE),
    START_TIME_INVALID(ActivityEntryField.START_TIME),
    START_TIME_AFTER_ROUTE_START(ActivityEntryField.START_TIME),
    DURATION_INVALID(ActivityEntryField.DURATION),
    REPETITIONS_INVALID(ActivityEntryField.REPETITIONS),
    DISTANCE_INVALID(ActivityEntryField.DISTANCE),
    DISTANCE_UNSUPPORTED(ActivityEntryField.DISTANCE),
    ELEVATION_INVALID(ActivityEntryField.ELEVATION),
    ELEVATION_UNSUPPORTED(ActivityEntryField.ELEVATION),
    ACTIVE_CALORIES_INVALID(ActivityEntryField.ACTIVE_CALORIES),
    TOTAL_CALORIES_INVALID(ActivityEntryField.TOTAL_CALORIES),
    TOTAL_CALORIES_BELOW_ACTIVE(ActivityEntryField.TOTAL_CALORIES),
}

enum class ActivityRepetitionEntryMode {
    TOTAL,
    SETS,
}

data class ActivityPlannedWorkoutBaseline(
    val planId: String,
    val activityTypeId: String,
    val titleText: String,
    val notesText: String,
    val startDateText: String,
    val startTimeText: String,
    val durationMinutesText: String,
    val repetitionMode: ActivityRepetitionEntryMode,
    val repetitionTotalText: String,
    val repetitionSets: List<ActivityRepetitionSetInput>,
)

enum class ActivityEntryFeeling(
    val emoji: String,
    val labelRes: Int,
    val noteText: String,
) {
    GREAT(
        emoji = "😀",
        labelRes = R.string.activity_entry_feeling_great,
        noteText = "Felt great.",
    ),
    GOOD(
        emoji = "🙂",
        labelRes = R.string.activity_entry_feeling_good,
        noteText = "Felt good.",
    ),
    HARD(
        emoji = "😓",
        labelRes = R.string.activity_entry_feeling_hard,
        noteText = "Felt hard.",
    ),
    ROUGH(
        emoji = "😖",
        labelRes = R.string.activity_entry_feeling_rough,
        noteText = "Felt rough.",
    ),
}

data class ActivityRepetitionSetInput(
    val repetitionsText: String = "",
    val restMinutesText: String = "",
)

data class ActivityEntryUiState(
    val mode: ActivityEntryMode = ActivityEntryMode.CHOOSE_SOURCE,
    val activityTypes: List<ActivityEntryType> = DefaultActivityEntryTypes,
    val selectedActivityType: ActivityEntryType = DefaultActivityEntryTypes.first(),
    val titleText: String = "",
    val selectedFeeling: ActivityEntryFeeling? = null,
    val notesText: String = "",
    val startDateText: String = "",
    val startTimeText: String = "",
    val durationMinutesText: String = "30",
    val distanceText: String = "",
    val elevationText: String = "",
    val activeCaloriesText: String = "",
    val totalCaloriesText: String = "",
    val repetitionMode: ActivityRepetitionEntryMode = ActivityRepetitionEntryMode.TOTAL,
    val repetitionTotalText: String = "",
    val repetitionSets: List<ActivityRepetitionSetInput> = listOf(ActivityRepetitionSetInput()),
    val plannedWorkouts: List<PlannedExerciseData> = emptyList(),
    val selectedPlannedWorkoutId: String? = null,
    val selectedPlannedWorkoutBaseline: ActivityPlannedWorkoutBaseline? = null,
    val selectedPlannedWorkoutActivityTypeId: String? = null,
    val isLoadingPlannedWorkouts: Boolean = false,
    val isSavingPlannedWorkout: Boolean = false,
    val importedRoute: RouteFileImport? = null,
    val recordedPauseIntervals: List<ActivityPauseInterval> = emptyList(),
    val recordedLaps: List<ExerciseLapData> = emptyList(),
    val recordedMarkers: List<ActivityRecordingMarker> = emptyList(),
    val writePermissions: Set<String> = emptySet(),
    val canWrite: Boolean = false,
    val isCheckingPermission: Boolean = true,
    val isImportingRoute: Boolean = false,
    val isSavingEntry: Boolean = false,
    val entryError: ActivityEntryError? = null,
    val detailMessage: String? = null,
    val validationErrors: Set<ActivityEntryValidationError> = emptySet(),
    val editRecordId: String? = null,
    val isRecordingDraft: Boolean = false,
    val saveCompleted: Boolean = false,
    val recordedBleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    val sessionHeartRateSamples: List<HeartRateSample> = emptyList(),
) {
    val routePoints: List<ExerciseRoutePoint>
        get() = importedRoute?.points.orEmpty()

    val isEditMode: Boolean
        get() = editRecordId != null

    val hasSelectedPlannedWorkoutChanges: Boolean
        get() {
            val planId = selectedPlannedWorkoutId ?: return false
            val baseline = selectedPlannedWorkoutBaseline?.takeIf { it.planId == planId } ?: return false
            return plannedWorkoutBaseline(planId) != baseline
        }
}

internal fun ActivityEntryUiState.plannedWorkoutBaseline(planId: String): ActivityPlannedWorkoutBaseline =
    ActivityPlannedWorkoutBaseline(
        planId = planId,
        activityTypeId = selectedActivityType.id,
        titleText = titleText.trim(),
        notesText = activitySaveNotes().orEmpty(),
        startDateText = startDateText.trim(),
        startTimeText = startTimeText.trim(),
        durationMinutesText = durationMinutesText.trim(),
        repetitionMode = repetitionMode,
        repetitionTotalText = repetitionTotalText.trim(),
        repetitionSets = repetitionSets.map { set ->
            set.copy(
                repetitionsText = set.repetitionsText.trim(),
                restMinutesText = set.restMinutesText.trim(),
            )
        },
    )
