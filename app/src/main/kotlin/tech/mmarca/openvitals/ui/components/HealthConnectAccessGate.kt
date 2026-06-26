package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability

enum class HealthConnectAccessGateMode {
    INSUFFICIENT_ACCESS,
    DOUBLE_CANCEL_RECOVERY,
    SYNC_PAUSED,
}

@Composable
fun HealthConnectAccessGate(
    mode: HealthConnectAccessGateMode?,
    onGrant: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (mode == null) {
        content()
        return
    }

    val (titleRes, bodyRes, actionLabelRes, onAction) = when (mode) {
        HealthConnectAccessGateMode.INSUFFICIENT_ACCESS -> Quad(
            R.string.health_connect_access_insufficient_title,
            R.string.health_connect_access_insufficient_body,
            R.string.action_grant_permission,
            onGrant,
        )
        HealthConnectAccessGateMode.DOUBLE_CANCEL_RECOVERY -> Quad(
            R.string.health_connect_access_double_cancel_title,
            R.string.health_connect_access_double_cancel_body,
            R.string.settings_open_health_permissions,
            onOpenHealthConnectSettings,
        )
        HealthConnectAccessGateMode.SYNC_PAUSED -> Quad(
            R.string.settings_health_connect_sync_title,
            R.string.dashboard_health_connect_sync_paused_body,
            R.string.settings_health_connect_manage_access,
            onOpenHealthConnectSettings,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = when (mode) {
                HealthConnectAccessGateMode.SYNC_PAUSED -> Icons.Outlined.HealthAndSafety
                else -> Icons.Outlined.Lock
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OpenVitalsButton(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text(stringResource(actionLabelRes))
        }
    }
}

@Composable
fun HealthConnectSyncStatusBanner(
    syncPaused: Boolean,
    syncInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        syncPaused -> {
            OpenVitalsCard(
                modifier = modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.health_connect_sync_paused),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        syncInProgress -> {
            OpenVitalsCard(
                modifier = modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.health_connect_sync_in_progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private data class Quad(
    val titleRes: Int,
    val bodyRes: Int,
    val actionLabelRes: Int,
    val onAction: () -> Unit,
)

fun resolveHealthConnectAccessGateMode(
    availability: HealthConnectAvailability,
    syncEnabled: Boolean,
    requiredPermissions: Set<String>,
    grantedPermissions: Set<String>,
    showDoubleCancelRecovery: Boolean,
): HealthConnectAccessGateMode? {
    if (availability != HealthConnectAvailability.AVAILABLE) return null
    if (!syncEnabled) return HealthConnectAccessGateMode.SYNC_PAUSED
    if (showDoubleCancelRecovery && requiredPermissions.isNotEmpty()) {
        return HealthConnectAccessGateMode.DOUBLE_CANCEL_RECOVERY
    }
    val missing = requiredPermissions - grantedPermissions
    return if (missing.isNotEmpty()) {
        HealthConnectAccessGateMode.INSUFFICIENT_ACCESS
    } else {
        null
    }
}

fun shouldShowDashboardHealthConnectPromo(
    availability: HealthConnectAvailability,
    syncEnabled: Boolean,
    minimumPermissionsGranted: Boolean,
): Boolean =
    availability != HealthConnectAvailability.AVAILABLE ||
        !syncEnabled ||
        !minimumPermissionsGranted
