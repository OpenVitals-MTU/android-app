package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.AppThemeMode
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.features.manualentry.DefaultActivityEntryTypes
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
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
    var debugExpanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
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
                    HealthConnectAvailability.NEEDS_PLAY_STORE -> stringResource(R.string.settings_status_needs_play_store)
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

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            ThemeModeCard(
                selected = state.appThemeMode,
                onSelect = viewModel::selectAppThemeMode,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            ActivityWeekModeCard(
                selected = state.activityWeekMode,
                onSelect = viewModel::selectActivityWeekMode,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            FavoriteActivityCard(
                selectedExerciseType = state.favoriteActivityExerciseType,
                onSelect = viewModel::selectFavoriteActivity,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
        }

        item {
            CalorieDataSourceCard(
                enabled = state.showOpenVitalsCalculatedCalories,
                onEnabledChange = viewModel::setShowOpenVitalsCalculatedCalories,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ─── Sleep preferences ───────────────────────────────────────────
        item { SectionHeader(stringResource(R.string.metric_sleep)) }

        item {
            SleepRangeModeCard(
                selected = state.sleepRangeMode,
                onSelect = viewModel::selectSleepRangeMode,
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

        state.permissionCategories.forEach { category ->
            item {
                PermissionCategoryCard(
                    category = category,
                    grantedPermissions = state.grantedPermissions,
                    availability = state.availability,
                    onGrant = {
                        val missingPermissions = category.permissions - state.grantedPermissions
                        val requestablePermissions = missingPermissions - category.manualPermissions
                        val manualPermissions = missingPermissions.intersect(category.manualPermissions)
                        when {
                            requestablePermissions.isNotEmpty() -> requestAllPermissions.launch(requestablePermissions)
                            manualPermissions.isNotEmpty() -> openManualPermissionSettings()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.permissionCategories.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_all_requestable_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.section_debug),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { debugExpanded = !debugExpanded }) {
                                Text(stringResource(if (debugExpanded) R.string.action_close else R.string.action_details))
                            }
                        }
                        if (debugExpanded) {
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
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.settings_debug_granted_permissions,
                                    state.grantedPermissions.size,
                                    state.visiblePermissions.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

        item {
            Text(
                text = stringResource(
                    R.string.settings_app_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }
}
}

@Composable
private fun CalorieDataSourceCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
            ) {
                Text(text = stringResource(R.string.settings_calorie_data_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_calorie_data_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
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
            AppLanguageDropdown(
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ThemeModeCard(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_theme_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppThemeMode.entries.size,
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                    AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                    AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                    AppThemeMode.AMOLED -> stringResource(R.string.settings_theme_amoled)
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityWeekModeCard(
    selected: ActivityWeekMode,
    onSelect: (ActivityWeekMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_activity_week_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_activity_week_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                ActivityWeekMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ActivityWeekMode.entries.size,
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    ActivityWeekMode.MONDAY_TO_SUNDAY -> {
                                        stringResource(R.string.settings_activity_week_monday_to_sunday)
                                    }
                                    ActivityWeekMode.LAST_7_DAYS -> {
                                        stringResource(R.string.settings_activity_week_last_7_days)
                                    }
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteActivityCard(
    selectedExerciseType: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_favorite_activity_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_favorite_activity_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            FavoriteActivityDropdown(
                selectedExerciseType = selectedExerciseType,
                onSelect = onSelect,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun FavoriteActivityDropdown(
    selectedExerciseType: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val activityTypes = DefaultActivityEntryTypes.filter { it.supportsGpsRoute }
    val selectedLabel = activityTypes
        .firstOrNull { it.exerciseType == selectedExerciseType }
        ?.let { stringResource(it.labelRes) }
        ?: stringResource(R.string.settings_favorite_activity_latest)

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedLabel)
            Spacer(Modifier.widthIn(min = 4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_favorite_activity_latest)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            activityTypes.forEach { activityType ->
                DropdownMenuItem(
                    text = { Text(stringResource(activityType.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelect(activityType.exerciseType)
                    },
                )
            }
        }
    }
}

@Composable
private fun SleepRangeModeCard(
    selected: SleepRangeMode,
    onSelect: (SleepRangeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.settings_sleep_range_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.settings_sleep_range_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                SleepRangeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SleepRangeMode.entries.size,
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    SleepRangeMode.ROLLING_24H -> {
                                        stringResource(R.string.settings_sleep_range_rolling_24h)
                                    }
                                    SleepRangeMode.NOON -> stringResource(R.string.settings_sleep_range_noon)
                                    SleepRangeMode.EVENING_18H -> {
                                        stringResource(R.string.settings_sleep_range_evening)
                                    }
                                }
                            )
                        },
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                UnitSystem.entries.forEachIndexed { index, unitSystem ->
                    SegmentedButton(
                        selected = selected == unitSystem,
                        onClick = { onSelect(unitSystem) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = UnitSystem.entries.size,
                        ),
                        label = {
                            Text(
                                when (unitSystem) {
                                    UnitSystem.METRIC -> stringResource(R.string.settings_unit_metric)
                                    UnitSystem.IMPERIAL -> stringResource(R.string.settings_unit_imperial)
                                }
                            )
                        },
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
private fun PermissionCategoryCard(
    category: SettingsPermissionCategory,
    grantedPermissions: Set<String>,
    availability: HealthConnectAvailability,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grantedCount = category.permissions.count { it in grantedPermissions }
    val granted = category.available && grantedCount == category.permissions.size
    val partial = category.available && grantedCount > 0 && !granted
    val missingPermissions = category.permissions - grantedPermissions
    val missingRequestableCount = (missingPermissions - category.manualPermissions).size
    val missingManualCount = missingPermissions.intersect(category.manualPermissions).size
    val isManualGrant = missingRequestableCount == 0 && missingManualCount > 0
    val unavailableReasonRes = category.unavailableReasonRes
    val status = when {
        !category.available -> stringResource(R.string.onboarding_status_not_supported)
        granted -> stringResource(R.string.onboarding_status_granted)
        partial -> stringResource(
            R.string.onboarding_status_partially_granted,
            grantedCount,
            category.permissions.size,
        )
        isManualGrant -> stringResource(R.string.onboarding_status_manual)
        else -> stringResource(R.string.onboarding_status_optional)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (!category.available && unavailableReasonRes != null) {
                        stringResource(unavailableReasonRes)
                    } else if (category.manualPermissions.isNotEmpty() && missingManualCount > 0) {
                        stringResource(
                            R.string.onboarding_category_additional_data_access_manual_note,
                            stringResource(category.descriptionRes),
                        )
                    } else {
                        stringResource(category.descriptionRes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.onboarding_status_granted),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else if (!category.available) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.onboarding_status_not_supported),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilledTonalButton(
                    onClick = onGrant,
                    enabled = availability == HealthConnectAvailability.AVAILABLE,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(
                        when {
                            isManualGrant -> stringResource(R.string.action_open)
                            partial -> stringResource(R.string.action_review)
                            else -> stringResource(R.string.action_grant)
                        }
                    )
                }
            }
        }
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.health_disclaimer_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.health_disclaimer_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
