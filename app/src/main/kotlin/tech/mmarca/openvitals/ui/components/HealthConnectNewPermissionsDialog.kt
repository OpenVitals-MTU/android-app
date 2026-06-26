package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

@Composable
fun HealthConnectNewPermissionsDialog(
    onDismissRequest: () -> Unit,
    onReviewPermissions: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.health_connect_new_permissions_title)) },
        text = { Text(stringResource(R.string.health_connect_new_permissions_body)) },
        confirmButton = {
            TextButton(onClick = onReviewPermissions) {
                Text(stringResource(R.string.health_connect_new_permissions_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_not_now))
            }
        },
    )
}

@Composable
fun HealthConnectNewPermissionsPrompt() {
    val context = LocalContext.current
    val coordinator = rememberHealthConnectScreenUxCoordinator()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var resumeTick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
    }

    LaunchedEffect(resumeTick) {
        showDialog = coordinator.shouldShowNewPermissionsDialog()
    }

    if (showDialog) {
        HealthConnectNewPermissionsDialog(
            onDismissRequest = {
                coordinator.markNewPermissionsPrompted()
                showDialog = false
            },
            onReviewPermissions = {
                openHealthConnectPermissionSettings(context)
                coordinator.markNewPermissionsPrompted()
                showDialog = false
            },
        )
    }
}
