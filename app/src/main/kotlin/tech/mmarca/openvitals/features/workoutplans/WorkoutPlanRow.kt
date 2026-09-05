package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.features.activity.exerciseTypeIcon
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.WorkoutColor

private val PlanIconSize = 22.dp
private val PlanIconSurfaceSize = 40.dp

/** The plan divider inset that lines up with the text, past the icon surface. */
internal val WorkoutPlanRowDividerInset = 72.dp

/** One plan as a list row. The list screen and the start hub differ only in [trailing]. */
@Composable
internal fun WorkoutPlanRow(
    plan: PlannedExerciseData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val isCompleted = plan.completedExerciseSessionId != null
    val start = plan.startTime.atZone(plan.startZoneOffset ?: ZoneId.systemDefault())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(PlanIconSurfaceSize)
                .background(WorkoutColor.copy(alpha = Emphasis.wash), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Outlined.CheckCircle else exerciseTypeIcon(plan.exerciseType),
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(PlanIconSize),
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.title ?: exerciseTypeLabel(plan.exerciseType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = "${dateTimeFormatterProvider.mediumDate().format(start)} · ${dateTimeFormatterProvider.shortTime().format(start)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = listOfNotNull(
                    stringResource(
                        R.string.workout_plan_summary,
                        plan.blocks.size,
                        plan.stepCount,
                        Duration.ofMillis(plan.durationMs).toMinutes().coerceAtLeast(1L),
                    ),
                    stringResource(R.string.planned_workout_completed).takeIf { isCompleted },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        trailing()
    }
}
