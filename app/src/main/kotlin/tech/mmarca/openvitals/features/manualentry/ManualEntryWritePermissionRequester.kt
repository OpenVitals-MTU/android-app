package tech.mmarca.openvitals.features.manualentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.health.connect.client.PermissionController

/** Owns an entry form's write-permission request: the Grant button asks for the metric's own set. */
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
