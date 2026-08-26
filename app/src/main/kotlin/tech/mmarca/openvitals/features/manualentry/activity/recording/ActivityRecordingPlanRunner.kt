package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.theme.Spacing

private val PlanControlHeight = 56.dp

/** The last seconds of a rest read as "Get ready" and are announced. */
internal const val GetReadySeconds = 5L

/**
 * What to do now, and how far along the plan is. Leads with the step in words
 * and the live number underneath — reps done over target, or the seconds left
 * — because a phone on the floor beside a plank is read from a metre away.
 */
@Composable
internal fun ActivityPlanStepBanner(
    state: ActivityRecordingState,
    now: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val step = state.currentPlanStep
    val stepNumber = (state.planStepIndex + 1).coerceAtMost(state.planSteps.size)
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = listOfNotNull(
                    stringResource(R.string.activity_recording_plan_step_progress, stepNumber, state.planSteps.size),
                    step?.takeIf { it.rounds > 1 }?.let {
                        stringResource(R.string.activity_recording_plan_round_progress, it.round, it.rounds)
                    },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            when {
                state.isPlanComplete -> {
                    Text(
                        text = stringResource(R.string.activity_recording_plan_complete),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                state.status == ActivityRecordingStatus.RESTING -> {
                    val remaining = state.restRemainingDuration(now)
                    val gettingReady = remaining.seconds <= GetReadySeconds
                    Text(
                        text = stringResource(
                            if (gettingReady) R.string.activity_recording_plan_get_ready else R.string.activity_recording_plan_rest,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text(
                        text = hrrCountdownText(remaining),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        // Read out only through the final seconds, not every tick.
                        modifier = if (gettingReady) Modifier.semantics { liveRegion = LiveRegionMode.Assertive } else Modifier,
                    )
                    step?.let {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.activity_recording_plan_next, planStepGoalText(it, unitFormatter)),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                step != null -> {
                    Text(
                        text = step.displayLabel(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text(
                        text = when (step.goalKind) {
                            ActivityPlanGoalKind.REPS -> stringResource(
                                R.string.activity_recording_plan_reps_progress,
                                unitFormatter.count(state.currentSetRepetitionCount),
                                unitFormatter.count(step.goalValue),
                            )
                            ActivityPlanGoalKind.SECONDS -> hrrCountdownText(
                                state.planStepRemaining(now) ?: java.time.Duration.ofSeconds(step.goalValue),
                            )
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.nextPlanStep?.let { next ->
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.activity_recording_plan_next, planStepGoalText(next, unitFormatter)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** "Push-ups · 10 reps" / "Plank · 45 s". */
@Composable
internal fun planStepGoalText(step: ActivityPlanRunStep, unitFormatter: UnitFormatter): String =
    step.displayLabel() + " · " + when (step.goalKind) {
        ActivityPlanGoalKind.REPS -> stringResource(R.string.activity_entry_plan_preview_reps, step.goalValue.toInt())
        ActivityPlanGoalKind.SECONDS -> stringResource(R.string.workout_plan_preview_seconds, step.goalValue)
    }

@Composable
internal fun PlanRunControls(
    state: ActivityRecordingState,
    onCompleteStep: () -> Unit,
    onSkipStep: () -> Unit,
    onStartNextStep: () -> Unit,
    onFinishRecording: () -> Unit,
    modifier: Modifier = Modifier,
    onUndoStep: () -> Unit = {},
) {
    val canUndo = (state.planStepIndex > 0 || state.isPlanComplete) && state.status != ActivityRecordingStatus.PAUSED
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (canUndo) {
                OpenVitalsOutlinedButton(
                    onClick = onUndoStep,
                    modifier = Modifier.height(PlanControlHeight),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.activity_recording_plan_back),
                    )
                }
            }
            when {
                state.isPlanComplete -> Unit
                state.status == ActivityRecordingStatus.RESTING -> {
                    OpenVitalsButton(
                        onClick = onStartNextStep,
                        modifier = Modifier
                            .weight(1f)
                            .height(PlanControlHeight),
                    ) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                        Text(
                            text = stringResource(R.string.activity_entry_recording_start_next_set),
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
                else -> {
                    OpenVitalsButton(
                        onClick = onCompleteStep,
                        enabled = state.status == ActivityRecordingStatus.RECORDING,
                        modifier = Modifier
                            .weight(1f)
                            .height(PlanControlHeight),
                    ) {
                        Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                        Text(
                            text = stringResource(R.string.activity_recording_plan_done),
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                    OpenVitalsOutlinedButton(
                        onClick = onSkipStep,
                        enabled = state.status == ActivityRecordingStatus.RECORDING,
                        modifier = Modifier.height(PlanControlHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = stringResource(R.string.activity_recording_plan_skip),
                        )
                    }
                }
            }
            OpenVitalsOutlinedButton(
                onClick = onFinishRecording,
                modifier = Modifier
                    .weight(1f)
                    .height(PlanControlHeight),
            ) {
                Text(stringResource(R.string.activity_entry_recording_end_session))
            }
        }
    }
}
