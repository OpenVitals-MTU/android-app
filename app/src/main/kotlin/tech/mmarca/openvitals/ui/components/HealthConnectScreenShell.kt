package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.HealthConnectScreenUxCoordinator
import tech.mmarca.openvitals.healthconnect.HealthConnectScreenUxState
import tech.mmarca.openvitals.healthconnect.HealthConnectUiEntryPoint
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

@Composable
fun rememberHealthConnectScreenUxCoordinator(): HealthConnectScreenUxCoordinator {
    val context = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(
            context,
            HealthConnectUiEntryPoint::class.java,
        ).healthConnectScreenUxCoordinator()
    }
}

@Composable
fun WithHealthConnectScreenUx(
    feature: HealthConnectFeature,
    isLoading: Boolean = false,
    refreshKey: Any? = Unit,
    onGrantPermissions: (Set<String>) -> Unit,
    onDismissContextualPrompt: () -> Unit = {},
    showInlineSyncBanner: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (HealthConnectScreenUxState) -> Unit,
) {
    val coordinator = rememberHealthConnectScreenUxCoordinator()
    val scope = rememberCoroutineScope()
    var uxState by remember(feature) {
        mutableStateOf(HealthConnectScreenUxState(feature = feature, isLoading = true))
    }
    var resumeTick by remember { mutableIntStateOf(0) }

    suspend fun reload() {
        uxState = coordinator.loadState(feature = feature, isLoading = isLoading)
    }

    LaunchedEffect(feature, isLoading, refreshKey, resumeTick) {
        reload()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
    }

    val dismissPrompt: () -> Unit = {
        coordinator.acknowledgeFeaturePermissions(feature, uxState.contextualPromptPermissions)
        onDismissContextualPrompt()
        scope.launch { reload() }
    }

    HealthConnectScreenShell(
        uxState = uxState,
        onGrantPermissions = onGrantPermissions,
        onDismissContextualPrompt = dismissPrompt,
        showInlineSyncBanner = showInlineSyncBanner,
        modifier = modifier,
        content = { content(uxState) },
    )
}

@Composable
fun HealthConnectScreenShell(
    uxState: HealthConnectScreenUxState,
    onGrantPermissions: (Set<String>) -> Unit,
    onOpenHealthConnectSettings: () -> Unit = {},
    onDismissContextualPrompt: () -> Unit = {},
    modifier: Modifier = Modifier,
    showInlineSyncBanner: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val syncPausedDescription = stringResource(R.string.health_connect_sync_paused)
    val syncInProgressDescription = stringResource(R.string.health_connect_sync_in_progress)

    HealthConnectAccessGate(
        mode = uxState.accessGateMode,
        onGrant = { onGrantPermissions(uxState.requiredPermissions) },
        onOpenHealthConnectSettings = {
            openHealthConnectPermissionSettings(context)
            onOpenHealthConnectSettings()
        },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showInlineSyncBanner && (uxState.syncPaused || uxState.isLoading)) {
                HealthConnectSyncStatusBanner(
                    syncPaused = uxState.syncPaused,
                    syncInProgress = uxState.isLoading && !uxState.syncPaused,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics {
                            contentDescription = if (uxState.syncPaused) {
                                syncPausedDescription
                            } else {
                                syncInProgressDescription
                            }
                        },
                )
            }

            if (uxState.showContextualPermissionPrompt) {
                ContextualPermissionPrompt(
                    feature = uxState.feature,
                    onGrant = { onGrantPermissions(uxState.contextualPromptPermissions) },
                    onDismiss = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            content()
        }
    }
}

@Composable
fun ContextualPermissionPrompt(
    feature: HealthConnectFeature,
    onGrant: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (titleRes, bodyRes) = contextualPermissionCopy(feature)
    PermissionCallout(
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
        onGrant = onGrant,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

private fun contextualPermissionCopy(feature: HealthConnectFeature): Pair<Int, Int> = when (feature) {
    HealthConnectFeature.ACTIVITY -> R.string.health_connect_promote_activity_title to R.string.health_connect_promote_activity_body
    HealthConnectFeature.ACTIVITIES -> R.string.health_connect_promote_activities_title to R.string.health_connect_promote_activities_body
    HealthConnectFeature.CALORIES -> R.string.health_connect_promote_calories_title to R.string.health_connect_promote_calories_body
    HealthConnectFeature.SLEEP -> R.string.health_connect_promote_sleep_title to R.string.health_connect_promote_sleep_body
    HealthConnectFeature.HEART -> R.string.health_connect_promote_heart_title to R.string.health_connect_promote_heart_body
    HealthConnectFeature.HEART_VITALS -> R.string.health_connect_promote_vitals_title to R.string.health_connect_promote_vitals_body
    HealthConnectFeature.BODY -> R.string.health_connect_promote_body_title to R.string.health_connect_promote_body_body
    HealthConnectFeature.HYDRATION -> R.string.health_connect_promote_hydration_title to R.string.health_connect_promote_hydration_body
    HealthConnectFeature.NUTRITION -> R.string.health_connect_promote_nutrition_title to R.string.health_connect_promote_nutrition_body
    HealthConnectFeature.MINDFULNESS -> R.string.health_connect_promote_mindfulness_title to R.string.health_connect_promote_mindfulness_body
    HealthConnectFeature.CYCLE -> R.string.health_connect_promote_cycle_title to R.string.health_connect_promote_cycle_body
    HealthConnectFeature.READINESS -> R.string.health_connect_promote_readiness_title to R.string.health_connect_promote_readiness_body
    HealthConnectFeature.DASHBOARD,
    HealthConnectFeature.MANUAL_ENTRY,
    HealthConnectFeature.DATA_IMPORT,
    -> R.string.message_missing_permissions_title to R.string.message_missing_permissions_body
}
