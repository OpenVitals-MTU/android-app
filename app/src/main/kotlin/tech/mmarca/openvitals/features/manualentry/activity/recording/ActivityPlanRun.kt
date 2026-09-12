package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRecordingSensor
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.manualentry.activity.toInputText
import tech.mmarca.openvitals.features.activity.exerciseSegmentLabel

enum class ActivityPlanGoalKind {
    REPS,
    SECONDS,
}

/**
 * One executable step of a plan run, rest folded in. [sensorTypeId] names
 * the recognizer that counts it; null means manual.
 */
@Immutable
data class ActivityPlanRunStep(
    val segmentType: Int,
    /** The plan's own name for the step ("Push-ups"); null means "the segment type's name". */
    val label: String?,
    val goalKind: ActivityPlanGoalKind,
    val goalValue: Long,
    /** Rest after this step, seconds; zero for none. */
    val restSeconds: Long,
    val blockIndex: Int,
    val round: Int,
    val rounds: Int,
    val sensorTypeId: String? = null,
    /** The plan's load for this step, kg; null when none is set. */
    val weightKg: Double? = null,
    /** Position within a run of identical steps ("Set 2 of 3"); 1 of 1 when alone. */
    val setIndex: Int = 1,
    val sets: Int = 1,
)

/** The step's name in the current language. */
fun ActivityPlanRunStep.displayLabel(context: Context): String = label ?: exerciseSegmentLabel(context, segmentType)

/** The step's name in the current language, for composables. */
@Composable
internal fun ActivityPlanRunStep.displayLabel(): String = label ?: exerciseSegmentLabel(segmentType)

/** "Push-ups, 10 reps" / "Plank, 45 seconds" / "Squat, 12 reps, 50 kilograms" — what the voice cue says. */
fun ActivityPlanRunStep.spokenGoal(context: Context): String = when (goalKind) {
    ActivityPlanGoalKind.REPS -> {
        val weight = weightKg
        if (weight != null) {
            context.getString(R.string.activity_recording_plan_spoken_reps_weight, displayLabel(context), goalValue, weight.toInputText(1))
        } else {
            context.getString(R.string.activity_recording_plan_spoken_reps, displayLabel(context), goalValue)
        }
    }
    ActivityPlanGoalKind.SECONDS -> {
        val minutes = wholeMinutesOrNull(goalValue)
        if (minutes != null) {
            context.resources.getQuantityString(R.plurals.activity_recording_plan_spoken_minutes, minutes.toInt(), displayLabel(context), minutes)
        } else {
            context.getString(R.string.activity_recording_plan_spoken_seconds, displayLabel(context), goalValue)
        }
    }
}

/** Whole minutes for a duration of a minute or more that divides evenly; null otherwise. */
fun wholeMinutesOrNull(seconds: Long): Long? = (seconds / 60).takeIf { seconds >= 60L && seconds % 60L == 0L }

/**
 * The entry type whose recognizer counts a step, if any. Matched on the
 * segment type and loosely on the label; [localizedTitle] adds the phone's
 * language. No match means counted by hand.
 */
internal fun planStepSensorTypeId(
    segmentType: Int,
    label: String?,
    localizedTitle: (type: ActivityEntryType) -> String? = { null },
): String? {
    val wanted = label?.let(::normalizedExerciseLabel)?.takeIf { it.isNotEmpty() }
    return DefaultActivityEntryTypes.firstOrNull { type ->
        type.segmentType == segmentType &&
            (type.recordingSensor == ActivityRecordingSensor.PROXIMITY ||
                type.recordingSensor == ActivityRecordingSensor.ACCELEROMETER) &&
            (
                type.defaultTitle == null ||
                    (
                        wanted != null &&
                            listOfNotNull(type.defaultTitle, localizedTitle(type))
                                .any { normalizedExerciseLabel(it) == wanted }
                        )
                )
    }?.id
}

/** Lower-case letters and digits only, accents stripped: the shape two spellings of one exercise share. */
private fun normalizedExerciseLabel(label: String): String =
    java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFD)
        .lowercase()
        .filter { it.isLetterOrDigit() }
