package tech.mmarca.openvitals.features.watches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import androidx.compose.ui.res.pluralStringResource
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.wearos.WearOsAppStatus
import tech.mmarca.openvitals.devices.wearos.WearOsCompanionStatus
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.formatDeviceTime
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun WearOsStatusCard(
    status: WearOsCompanionStatus,
    error: ScreenError?,
    isChecking: Boolean,
    onCheck: () -> Unit,
    onGrantBluetooth: () -> Unit,
) {
    if (error == ScreenError.PermissionDenied) {
        PermissionCallout(
            title = stringResource(R.string.message_missing_permissions_title),
            body = stringResource(R.string.settings_watch_wearos_bt_permission),
            onGrant = onGrantBluetooth,
        )
        return
    }
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status.isPaired) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (status.isPaired) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(WearOsStatusIconSize),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = if (status.isPaired) {
                        stringResource(R.string.settings_watch_wearos_bt_paired)
                    } else {
                        stringResource(R.string.settings_watch_wearos_bt_not_paired)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status.isAppRunning) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (status.isAppRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(WearOsStatusIconSize),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = when (status.appStatus) {
                        WearOsAppStatus.APP_RUNNING ->
                            stringResource(R.string.settings_watch_wearos_app_running)
                        WearOsAppStatus.NO_ANSWER ->
                            stringResource(R.string.settings_watch_wearos_app_no_answer)
                        WearOsAppStatus.NOT_PAIRED ->
                            stringResource(R.string.settings_watch_not_connected)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.isAppRunning) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            error.resolve()?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OpenVitalsOutlinedButton(
                onClick = onCheck,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(WearOsButtonIconSize),
                        strokeWidth = WearOsProgressStroke,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.settings_watch_wearos_checking))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(WearOsButtonIconSize),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.settings_watch_wearos_check_app))
                }
            }
        }
    }
}

/**
 * The heart rate sync for a Wear OS watch: what it does, when it last ran,
 * what the last run brought, and the button. The sync itself is the shared
 * [DeviceSyncController] flow; this card only words it as samples.
 */
@Composable
internal fun WearOsSyncCard(
    device: BleSensorDevice,
    sync: DeviceSyncUiState,
    onSync: () -> Unit,
) {
    val syncingThis = sync.isSyncingDevice(device.id)
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.settings_watch_wearos_sync_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            val syncedAt = device.lastSyncedAt
            Text(
                text = if (syncedAt == null) {
                    stringResource(R.string.settings_watch_never_synced)
                } else {
                    stringResource(R.string.settings_watch_last_synced, formatDeviceTime(syncedAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Only after a run this session; the count is not persisted.
            val samples = sync.lastFileCount
            if (!sync.isSyncing && sync.phase == DeviceSyncPhase.COMPLETE && samples != null) {
                Text(
                    text = if (samples > 0) {
                        pluralStringResource(R.plurals.settings_watch_wearos_synced_samples, samples, samples)
                    } else {
                        stringResource(R.string.settings_watch_wearos_synced_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            sync.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OpenVitalsOutlinedButton(
                onClick = onSync,
                // One sync at a time, whichever watch it is for.
                enabled = !sync.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (syncingThis) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(WearOsButtonIconSize),
                        strokeWidth = WearOsProgressStroke,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.settings_watch_wearos_syncing))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(WearOsButtonIconSize),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.settings_watch_wearos_sync_now))
                }
            }
        }
    }
}

private val WearOsStatusIconSize = 20.dp
private val WearOsButtonIconSize = 18.dp
private val WearOsProgressStroke = 2.dp
