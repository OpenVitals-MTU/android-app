package tech.mmarca.openvitals.features.manualentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.health.connect.client.PermissionController

/**
 * Owns an entry form's Health Connect write-permission request: the form's
 * Grant button asks Health Connect for exactly the metric's own write set,
 * and the result lands back on the form, granted or not.
 */
@Stable
class ManualEntryWritePermissionRequester internal constructor(
    private val launchDialog: (Set<String>) -> Unit,
) {
    fun launch(permissions: Set<String>) {
        if (permissions.isEmpty()) return
        launchDialog(permissions)
    }
}

@Composable
internal fun rememberManualEntryWritePermissionRequester(
    onResult: (granted: Set<String>) -> Unit,
): ManualEntryWritePermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        currentOnResult(granted)
    }
    return remember {
        ManualEntryWritePermissionRequester(launchDialog = { permissions -> launcher.launch(permissions) })
    }
}
