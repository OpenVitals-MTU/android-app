package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton

@Composable
internal fun DashboardQuickActions(
    isEditingDashboard: Boolean,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardActionsSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OpenVitalsTonalButton(
            onClick = onOpenLog,
            contentPadding = DashboardQuickActionContentPadding,
            modifier = Modifier
                .weight(1f)
                .height(DashboardQuickActionHeight),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(DashboardQuickActionIconSize),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.dashboard_action_log),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OpenVitalsButton(
            onClick = onStartActivity,
            contentPadding = DashboardQuickActionContentPadding,
            modifier = Modifier
                .weight(1f)
                .height(DashboardQuickActionHeight),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(DashboardQuickActionIconSize),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.dashboard_action_start_workout),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OpenVitalsIconButton(
            onClick = onToggleDashboardEdit,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(
                    if (isEditingDashboard) {
                        R.string.cd_finish_dashboard_editing
                    } else {
                        R.string.cd_edit_dashboard
                    }
                ),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
