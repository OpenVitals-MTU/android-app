package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.ZoneId

internal fun LazyListScope.dashboardActivitiesToday(
    workouts: List<ExerciseData>,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onRequestDeleteActivity: (ExerciseData) -> Unit,
) {
    item {
        DashboardActivitiesSectionHeader(onClick = onOpenActivities)
    }
    if (workouts.isNotEmpty()) {
        items(
            count = workouts.size,
            key = { index -> workouts[index].id.ifBlank { "workout_$index" } },
        ) { index ->
            val workout = workouts[index]
            val editable = workout.isOpenVitalsEntry && workout.id.isNotBlank()
            val openAction = workout.id.takeIf { it.isNotBlank() }?.let { activityId ->
                { onOpenActivity(activityId) }
            }
            val editAction = if (editable) {
                { onEditActivity(workout.id) }
            } else {
                null
            }
            val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = cardModifier,
                    onClick = openAction,
                    onEdit = editAction,
                )
            }
            if (editable) {
                DashboardSwipeToDeleteActivityCard(
                    onDelete = { onRequestDeleteActivity(workout) },
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    ),
                ) {
                    cardContent(Modifier)
                }
            } else {
                cardContent(
                    Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    )
                )
            }
        }
    } else {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.section_activities),
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                message = stringResource(R.string.message_no_workouts_day),
                modifier = Modifier.padding(
                    horizontal = DashboardScreenPadding,
                    vertical = 6.dp,
                ),
                showHeader = false,
                onClick = onOpenActivities,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardSwipeToDeleteActivityCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    val shape = MaterialTheme.shapes.medium

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
            }
            scope.launch {
                dismissState.reset()
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.errorContainer, shape)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_entry),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier.clip(shape),
        content = { content() },
    )
}

@Composable
internal fun DeleteActivityConfirmationDialog(
    workout: ExerciseData,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_delete_activity_title)) },
        text = {
            Text(
                stringResource(
                    R.string.dashboard_delete_activity_message,
                    exerciseTypeLabel(workout.exerciseType),
                )
            )
        },
        confirmButton = {
            OpenVitalsTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DashboardActivitiesSectionHeader(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = DashboardScreenPadding,
                vertical = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_activities_today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
