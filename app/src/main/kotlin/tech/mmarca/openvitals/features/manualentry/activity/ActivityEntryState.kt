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
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

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
    MANUAL,
    ROUTE_IMPORT,
    RECORDING,
}

enum class ActivityEntryField {
    ACTIVITY_TYPE,
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

data class ActivityRepetitionSetInput(
    val repetitionsText: String = "",
    val restMinutesText: String = "",
)

data class ActivityEntryUiState(
    val mode: ActivityEntryMode = ActivityEntryMode.CHOOSE_SOURCE,
    val activityTypes: List<ActivityEntryType> = DefaultActivityEntryTypes,
    val selectedActivityType: ActivityEntryType = DefaultActivityEntryTypes.first(),
    val titleText: String = "",
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
    val importedRoute: RouteFileImport? = null,
    val recordedPauseIntervals: List<ActivityPauseInterval> = emptyList(),
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
) {
    val routePoints: List<ExerciseRoutePoint>
        get() = importedRoute?.points.orEmpty()

    val isEditMode: Boolean
        get() = editRecordId != null
}
