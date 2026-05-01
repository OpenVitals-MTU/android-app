package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val missingRequestablePermissions = state.missingRequestableVisiblePermissions
    val openManualPermissionSettings = {
        if (!openHealthConnectPermissionSettings(context)) {
            Toast.makeText(
                context,
                unableToOpenPermissions,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    val requestAllPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val requestCyclePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    if (state.isLoading) {
        FullScreenLoading()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // ─── Health Connect status ────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.section_health_connect)) }

        item {
            StatusCard(
                label = stringResource(R.string.section_health_connect),
                status = when (state.availability) {
                    HealthConnectAvailability.AVAILABLE -> stringResource(R.string.settings_status_available)
                    HealthConnectAvailability.NEEDS_PROVIDER_UPDATE -> stringResource(R.string.settings_status_needs_update)
                    HealthConnectAvailability.NOT_SUPPORTED -> stringResource(R.string.onboarding_status_not_supported)
                },
                ok = state.availability == HealthConnectAvailability.AVAILABLE,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ─── Display preferences ─────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.section_display)) }

        item {
            LanguageCard(
                selected = state.appLanguage,
                onSelect = viewModel::selectAppLanguage,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            UnitSystemCard(
                selected = state.unitSystem,
                onSelect = viewModel::selectUnitSystem,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ─── Cycle tracking ──────────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.metric_cycle_tracking)) }

        item {
            CycleTrackingCard(
                enabled = state.trackCycle,
                availability = state.availability,
                cyclePermissions = state.cyclePermissions,
                grantedPermissions = state.grantedPermissions,
                onEnabledChange = { enabled ->
                    viewModel.setTrackCycle(enabled)
                    if (enabled && state.availability == HealthConnectAvailability.AVAILABLE) {
                        requestCyclePermissions.launch(state.cyclePermissions)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ─── Permissions ─────────────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.section_permissions)) }

        item {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column {
                    state.visiblePermissions.forEachIndexed { index, perm ->
                        val granted = perm in state.grantedPermissions
                        PermissionRow(permission = perm, granted = granted)
                        if (index < state.visiblePermissions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { requestAllPermissions.launch(missingRequestablePermissions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = state.availability == HealthConnectAvailability.AVAILABLE &&
                    missingRequestablePermissions.isNotEmpty(),
            ) {
                Text(
                    if (missingRequestablePermissions.isEmpty()) {
                        stringResource(R.string.settings_all_requestable_granted)
                    } else {
                        stringResource(R.string.settings_request_missing_permissions)
                    }
                )
            }
        }

        if (state.missingManualVisiblePermissions.isNotEmpty()) {
            item {
                PermissionCallout(
                    title = stringResource(R.string.settings_manual_permissions_title),
                    body = stringResource(R.string.settings_manual_permissions_body),
                    actionLabel = stringResource(R.string.settings_open_health_permissions),
                    onGrant = openManualPermissionSettings,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // ─── Privacy info ─────────────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.section_privacy)) }

        item {
            PrivacyInfoCard(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // ─── Debug ───────────────────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.section_debug)) }

        item {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val visibleGranted = state.visiblePermissions.filter { it in state.grantedPermissions }
                    Text(
                        text = stringResource(R.string.settings_debug_availability, state.availability),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_debug_granted_permissions,
                            visibleGranted.size,
                            state.visiblePermissions.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                    visibleGranted.sorted().forEach { perm ->
                        Text(
                            text = "  ✓ ${perm.substringAfterLast('.')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun LanguageCard(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_language_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                AppLanguage.entries.forEach { appLanguage ->
                    FilterChip(
                        selected = selected == appLanguage,
                        onClick = { onSelect(appLanguage) },
                        label = {
                            Text(
                                when (appLanguage) {
                                    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                                    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                                    AppLanguage.SPANISH -> stringResource(R.string.settings_language_spanish)
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitSystemCard(
    selected: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_units_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_units_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                UnitSystem.entries.forEach { unitSystem ->
                    FilterChip(
                        selected = selected == unitSystem,
                        onClick = { onSelect(unitSystem) },
                        label = {
                            Text(
                                when (unitSystem) {
                                    UnitSystem.METRIC -> stringResource(R.string.settings_unit_metric)
                                    UnitSystem.IMPERIAL -> stringResource(R.string.settings_unit_imperial)
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleTrackingCard(
    enabled: Boolean,
    availability: HealthConnectAvailability,
    cyclePermissions: Set<String>,
    grantedPermissions: Set<String>,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grantedCount = cyclePermissions.count { it in grantedPermissions }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
            ) {
                Text(text = stringResource(R.string.settings_track_cycle), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (enabled) {
                        stringResource(R.string.settings_cycle_permissions_granted, grantedCount, cyclePermissions.size)
                    } else {
                        stringResource(R.string.settings_cycle_off_body)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = availability == HealthConnectAvailability.AVAILABLE,
            )
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    status: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: String, granted: Boolean) {
    val shortName = permission
        .substringAfterLast('.')
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = shortName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun PrivacyInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            listOf(
                R.string.settings_privacy_no_account,
                R.string.settings_privacy_no_cloud,
                R.string.settings_privacy_no_analytics,
                R.string.settings_privacy_no_ads,
                R.string.settings_privacy_on_device,
                R.string.settings_privacy_read_only,
            ).forEach { pointRes ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                    )
                    Text(
                        text = stringResource(pointRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
