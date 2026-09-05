package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanRow
import tech.mmarca.openvitals.features.workoutplans.isGuidedRunnable
import tech.mmarca.openvitals.features.workoutplans.WorkoutPlanRowDividerInset
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId

/** Where a new activity starts: today's plans, then the two ways to start from nothing. */
@Composable
internal fun ActivityStartHub(
    state: ActivityEntryUiState,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onLogFromPlan: (String) -> Unit,
    onStartPlan: (String) -> Unit,
    onRepeatPlan: (String) -> Unit,
    onRecord: () -> Unit,
    onLogManually: () -> Unit,
    onManagePlans: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ActivityEntryHeader(state = state, onRequestWritePermission = onRequestWritePermission)

            if (state.hubPlansAvailable) {
                HubPlansSection(
                    state = state,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onLogFromPlan = onLogFromPlan,
                    onStartPlan = onStartPlan,
                )
                if (state.recentPlans.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.activity_entry_hub_repeat_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    state.recentPlans.forEachIndexed { index, plan ->
                        WorkoutPlanRow(
                            plan = plan,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onRepeatPlan(plan.id) },
                        ) {
                            OpenVitalsTextButton(onClick = { onRepeatPlan(plan.id) }) {
                                Text(stringResource(R.string.workout_plan_action_repeat))
                            }
                        }
                        if (index < state.recentPlans.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = WorkoutPlanRowDividerInset),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill),
                            )
                        }
                    }
                }
            }

            OpenVitalsButton(
                onClick = onRecord,
                enabled = !state.isCheckingPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.MyLocation, contentDescription = null)
                Text(
                    text = stringResource(R.string.activity_entry_record_gps),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            OpenVitalsOutlinedButton(
                onClick = onLogManually,
                enabled = !state.isCheckingPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.activity_entry_hub_log_manually),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            if (state.hubPlansAvailable) {
                OpenVitalsTextButton(onClick = onManagePlans, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_entry_hub_manage_plans))
                }
            }

            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HubPlansSection(
    state: ActivityEntryUiState,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onLogFromPlan: (String) -> Unit,
    onStartPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.section_planned_workouts),
            style = MaterialTheme.typography.titleSmall,
        )
        state.hubPlansError?.let { error ->
            ScreenErrorContent(screenError = error)
            return@Column
        }
        if (state.isLoadingHubPlans && state.hubPlans.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        if (state.hubPlans.isEmpty()) {
            Text(
                text = stringResource(R.string.activity_entry_hub_plans_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val (todayPlans, upcoming) = state.hubPlans.partition {
            !it.startTime.atZone(it.startZoneOffset ?: zone).toLocalDate().isAfter(today)
        }
        listOf(
            R.string.workout_plans_group_today to todayPlans,
            R.string.workout_plans_group_upcoming to upcoming,
        ).forEach { (titleRes, plans) ->
            if (plans.isEmpty()) return@forEach
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            plans.forEachIndexed { index, plan ->
                val runnable = plan.isGuidedRunnable()
                WorkoutPlanRow(
                    plan = plan,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onClick = { if (runnable) onStartPlan(plan.id) else onLogFromPlan(plan.id) },
                ) {
                    OpenVitalsTextButton(onClick = { onLogFromPlan(plan.id) }) {
                        Text(stringResource(R.string.activity_entry_hub_log_from_plan))
                    }
                    if (runnable) {
                        OpenVitalsButton(
                            onClick = { onStartPlan(plan.id) },
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                        ) {
                            Text(stringResource(R.string.activity_entry_hub_start_plan))
                        }
                    }
                }
                if (index < plans.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = WorkoutPlanRowDividerInset),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill),
                    )
                }
            }
        }
    }
}
