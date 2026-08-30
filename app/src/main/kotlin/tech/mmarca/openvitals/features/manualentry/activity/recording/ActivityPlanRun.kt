package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRecordingSensor
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.features.activity.exerciseSegmentLabel

enum class ActivityPlanGoalKind {
    REPS,
    SECONDS,
}

/**
 * One executable step of a plan run: the plan's blocks unrolled by round, with
 * each rest folded into the active step before it. [sensorTypeId] names the
 * entry type whose repetition recognizer counts this step; null means the
 * count is manual (a plank, a lunge — anything the phone cannot sense).
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
)

/** The step's name in the current language. */
fun ActivityPlanRunStep.displayLabel(context: Context): String = label ?: exerciseSegmentLabel(context, segmentType)

/** The step's name in the current language, for composables. */
@Composable
internal fun ActivityPlanRunStep.displayLabel(): String = label ?: exerciseSegmentLabel(segmentType)

/** "Push-ups, 10 reps" / "Plank, 45 seconds" — what the voice cue says. */
fun ActivityPlanRunStep.spokenGoal(context: Context): String = when (goalKind) {
    ActivityPlanGoalKind.REPS -> context.getString(R.string.activity_recording_plan_spoken_reps, displayLabel(context), goalValue)
    ActivityPlanGoalKind.SECONDS -> context.getString(R.string.activity_recording_plan_spoken_seconds, displayLabel(context), goalValue)
}

/**
 * The entry type whose recognizer can count a step, if any. Matched on the
 * segment type, and on the label for the segment types several exercises
 * share (push-ups and trampoline jumping are both "other workout").
 *
 * The label is whatever the step's author typed, so the match is loose: case,
 * spaces, hyphens and accents are ignored ("Pushups", "push ups" and
 * "Push-Ups" all count), and [localizedTitle] lets the caller offer the
 * exercise's name in the phone's language ("Liegestütze") next to the English
 * preset. A step nothing matches gets no sensor and is counted by hand.
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
