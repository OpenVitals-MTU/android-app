package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
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
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.WorkoutColor

private val PlanIconSize = 22.dp

/** The plan a saved session was logged against, and a way to it. */
@Composable
internal fun ActivityLinkedPlanDetailCard(
    planTitle: String?,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = WorkoutColor,
                modifier = Modifier.size(PlanIconSize),
            )
            Column(
                modifier = Modifier
                    .padding(start = Spacing.md)
                    .weight(1f),
            ) {
                Text(
                    text = planTitle?.let { stringResource(R.string.activity_entry_linked_plan, it) }
                        ?: stringResource(R.string.activity_entry_linked_plan_untitled),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.activity_detail_linked_plan_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OpenVitalsTextButton(onClick = onOpenPlan) {
                Text(stringResource(R.string.activity_detail_open_plan))
            }
        }
    }
}
