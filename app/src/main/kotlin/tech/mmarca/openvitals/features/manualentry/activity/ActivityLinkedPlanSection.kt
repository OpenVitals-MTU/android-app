package tech.mmarca.openvitals.features.manualentry.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.WorkoutColor

private val LinkedPlanIconSize = 22.dp

/** The plan this session is logged against; editing it is the builder's job. */
@Composable
internal fun ActivityLinkedPlanCard(
    plan: ActivityLinkedPlan,
    enabled: Boolean,
    onChangePlan: () -> Unit,
    onEditPlan: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = WorkoutColor,
                    modifier = Modifier.size(LinkedPlanIconSize),
                )
                Text(
                    text = plan.title?.let { stringResource(R.string.activity_entry_linked_plan, it) }
                        ?: stringResource(R.string.activity_entry_linked_plan_untitled),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                OpenVitalsTextButton(onClick = onChangePlan, enabled = enabled) {
                    Text(stringResource(R.string.activity_entry_linked_plan_change))
                }
                OpenVitalsTextButton(onClick = onEditPlan, enabled = enabled) {
                    Text(stringResource(R.string.action_edit))
                }
                OpenVitalsTextButton(onClick = onClear, enabled = enabled) {
                    Text(stringResource(R.string.action_remove))
                }
            }
        }
    }
}

/** Offered when the steps are the user's own: hand them to the builder as a plan. */
@Composable
internal fun ActivitySaveAsPlanButton(
    state: ActivityEntryUiState,
    enabled: Boolean,
    onSaveAsPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.selectedActivityType.supportsSetRepetitions || state.linkedPlan != null || state.isEditMode) return
    OpenVitalsOutlinedButton(
        onClick = onSaveAsPlan,
        enabled = enabled && !state.isSavingAsPlan,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
        Text(
            text = stringResource(R.string.activity_entry_save_as_plan),
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
