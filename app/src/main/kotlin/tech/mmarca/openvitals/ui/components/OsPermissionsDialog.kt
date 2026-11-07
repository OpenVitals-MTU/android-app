package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.domain.model.OsPermissionRow
import tech.mmarca.openvitals.ui.theme.Spacing

private val PermissionIconSize = 18.dp

/**
 * The checklist of Android permissions a device needs, shown before it is
 * added. Each row grants on its own; continuing anyway stays available.
 */
@Composable
fun OsPermissionsDialog(
    catalog: OsPermissionCatalog,
    title: String,
    body: String,
    /** The confirm label once everything is granted: the action the gate stood in front of. */
    continueLabel: String,
    onGrantAll: () -> Unit,
    onGrantRow: (OsPermissionRow) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Ordered once when the dialog opens, so rows do not move under the finger.
                val rowOrder = remember { catalog.rows.sortedBy { it.granted }.map { it.id } }
                rowOrder.mapNotNull { id -> catalog.rows.firstOrNull { it.id == id } }
                    .forEach { row -> PermissionRow(row, onGrant = { onGrantRow(row) }) }
            }
        },
        confirmButton = {
            if (catalog.allGranted) {
                TextButton(onClick = onContinue) { Text(text = continueLabel) }
            } else {
                TextButton(onClick = onGrantAll) {
                    Text(text = stringResource(R.string.onboarding_action_grant_all))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onContinue) {
                Text(text = stringResource(R.string.watch_permissions_continue))
            }
        },
    )
}

@Composable
private fun PermissionRow(row: OsPermissionRow, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (row.granted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (row.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PermissionIconSize),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md),
        ) {
            Text(text = stringResource(row.titleRes()), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(row.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!row.granted) {
            TextButton(onClick = onGrant) { Text(text = stringResource(R.string.action_grant)) }
        }
    }
}

private fun OsPermissionRow.titleRes(): Int = when (id) {
    OsPermissionId.BLUETOOTH -> R.string.onboarding_os_bluetooth
    OsPermissionId.NOTIFICATIONS -> R.string.onboarding_os_notifications
    OsPermissionId.LOCATION -> R.string.onboarding_os_location
    OsPermissionId.BATTERY_OPTIMIZATION -> R.string.onboarding_os_battery
    OsPermissionId.NOTIFICATION_FORWARDING -> R.string.onboarding_os_notification_forwarding
}

private fun OsPermissionRow.descriptionRes(): Int = when (id) {
    OsPermissionId.BLUETOOTH -> R.string.onboarding_os_bluetooth_desc
    OsPermissionId.NOTIFICATIONS -> R.string.onboarding_os_notifications_desc
    OsPermissionId.LOCATION -> R.string.onboarding_os_location_desc
    OsPermissionId.BATTERY_OPTIMIZATION -> R.string.onboarding_os_battery_desc
    OsPermissionId.NOTIFICATION_FORWARDING -> R.string.onboarding_os_notification_forwarding_desc
}
