package tech.mmarca.openvitals.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.HealthConnectScreenUxCoordinator
import tech.mmarca.openvitals.healthconnect.HealthConnectScreenUxState

class HealthConnectPermissionLauncherState internal constructor(
    internal val launch: (Set<String>) -> Unit,
)

@Composable
fun rememberHealthConnectPermissionLauncher(
    coordinator: HealthConnectScreenUxCoordinator = rememberHealthConnectScreenUxCoordinator(),
    onResult: () -> Unit = {},
): HealthConnectPermissionLauncherState {
    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.isNotEmpty()) {
            coordinator.recordPermissionRequestGranted()
        } else {
            coordinator.recordPermissionRequestCancelled()
        }
        onResult()
    }
    return HealthConnectPermissionLauncherState(
        launch = { permissions ->
            if (permissions.isEmpty()) return@HealthConnectPermissionLauncherState
            launcher.launch(permissions)
        },
    )
}

@Composable
fun WithHealthConnectFeatureScreen(
    feature: HealthConnectFeature,
    isLoading: Boolean = false,
    refreshKey: Any? = Unit,
    showInlineSyncBanner: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (HealthConnectScreenUxState) -> Unit,
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(
        onResult = { reloadKey++ },
    )

    WithHealthConnectScreenUx(
        feature = feature,
        isLoading = isLoading,
        refreshKey = refreshKey to reloadKey,
        showInlineSyncBanner = showInlineSyncBanner,
        modifier = modifier,
        onGrantPermissions = { permissions -> permissionLauncher.launch(permissions) },
        content = content,
    )
}
