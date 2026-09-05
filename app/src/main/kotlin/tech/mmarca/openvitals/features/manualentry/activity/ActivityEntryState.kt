package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.Icons
import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.runtime.Immutable
import java.time.Instant
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
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
    PLAN_NOT_FOUND,
}

enum class ActivityEntryMode {
    START_HUB,
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

/** A plan about to be run step by step: resolved type and the flattened steps. */
data class ActivityGuidedPlan(
    val plan: PlannedExerciseData,
    val activityType: ActivityEntryType,
    val steps: List<ActivityPlanRunStep>,
)

/** The plan a session is logged against; the title is null when only the id is known. */
data class ActivityLinkedPlan(
    val id: String,
    val title: String?,
)

/**
 * How the session felt, on a four-step scale. Material glyphs, not emoji:
 * emoji ignore the colour scheme and cannot carry selection state.
 */
enum class ActivityEntryFeeling(
    val icon: ImageVector,
    val labelRes: Int,
    val noteText: String,
) {
    GREAT(
        icon = Icons.Outlined.SentimentVerySatisfied,
        labelRes = R.string.activity_entry_feeling_great,
        noteText = "Felt great.",
    ),
    GOOD(
        icon = Icons.Outlined.SentimentSatisfied,
        labelRes = R.string.activity_entry_feeling_good,
        noteText = "Felt good.",
    ),
    HARD(
        icon = Icons.Outlined.SentimentDissatisfied,
        labelRes = R.string.activity_entry_feeling_hard,
        noteText = "Felt hard.",
    ),
    ROUGH(
        icon = Icons.Outlined.SentimentVeryDissatisfied,
        labelRes = R.string.activity_entry_feeling_rough,
        noteText = "Felt rough.",
    ),
}

/** One set. A plan can put a different exercise or a timed hold in the sequence. */
data class ActivityRepetitionSetInput(
    /** The rep count, or the seconds when [isDuration]. */
    val repetitionsText: String = "",
    val restMinutesText: String = "",
    /** Segment type for this set when it differs from the activity type's own; null means the activity's. */
    val segmentType: Int? = null,
    /** A stored step label ("Push-ups"); null falls back to the segment type's name. */
    val label: String? = null,
    val isDuration: Boolean = false,
)

@Immutable
data class ActivityEntryUiState(
    val mode: ActivityEntryMode = ActivityEntryMode.START_HUB,
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
    val linkedPlan: ActivityLinkedPlan? = null,
    /** Set while the recording setup is showing a plan to run; cleared with the rest of the form. */
    val guidedPlan: ActivityGuidedPlan? = null,
    /** Uncompleted plans for today and later, by start time; the start hub's list. */
    val hubPlans: List<PlannedExerciseData> = emptyList(),
    /** Recently completed plans, newest first, one per title: what "Repeat" offers. */
    val recentPlans: List<PlannedExerciseData> = emptyList(),
    val isLoadingHubPlans: Boolean = false,
    val hubPlansError: ScreenError? = null,
    /** False when this device's Health Connect has no planned-exercise feature at all. */
    val hubPlansAvailable: Boolean = true,
    val isSavingAsPlan: Boolean = false,
    /** One-shot request for the screen to open the builder on this plan. */
    val pendingBuilderPlanId: String? = null,
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
    val detailError: ScreenError? = null,
    val validationErrors: Set<ActivityEntryValidationError> = emptySet(),
    val editRecordId: String? = null,
    val isRecordingDraft: Boolean = false,
    val saveCompleted: Boolean = false,
    val recordedBleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    /** CoMaps guidance banked during the recording. App-local only. */
    val recordedCoMapsSamples: List<CoMapsNavigationSnapshot> = emptyList(),
    val sessionHeartRateSamples: List<HeartRateSample> = emptyList(),
    /** When the effort stopped in a recovery test, written as a trailing rest segment. */
    val recordedRecoveryStartTime: Instant? = null,
) {
    val routePoints: List<ExerciseRoutePoint>
        get() = importedRoute?.points.orEmpty()

    val isEditMode: Boolean
        get() = editRecordId != null
}
