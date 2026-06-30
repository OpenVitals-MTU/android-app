package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader

internal fun LazyListScope.settingsScreenContent(
    section: SettingsSection?,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    when (section) {
        null -> {
            SettingsSection.entries
                .filter { BuildConfig.DEBUG || it != SettingsSection.DEBUG_DIAGNOSTICS }
                .forEach { settingsSection ->
                    item {
                        SettingsCategoryCard(
                            section = settingsSection,
                            onClick = { actions.onOpenSection(settingsSection) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }

            item { SectionHeader(stringResource(R.string.section_privacy)) }

            item {
                PrivacyInfoCard(
                    onOpenPrivacyPolicy = actions.onOpenPrivacyPolicy,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                SettingsVersionText()
            }
        }
        SettingsSection.DISPLAY -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                LanguageCard(
                    selected = state.appLanguage,
                    onSelect = viewModel::selectAppLanguage,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                UnitSystemCard(
                    selected = state.unitSystem,
                    onSelect = viewModel::selectUnitSystem,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ThemeModeCard(
                    selected = state.appThemeMode,
                    onSelect = viewModel::selectAppThemeMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.ACTIVITIES -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                ActivityWeekModeCard(
                    selected = state.activityWeekMode,
                    onSelect = viewModel::selectActivityWeekMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                FavoriteActivityCard(
                    selectedExerciseType = state.favoriteActivityExerciseType,
                    onSelect = viewModel::selectFavoriteActivity,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ActivityRecordingPreferencesCard(
                    preferences = state.activityRecordingPreferences,
                    onChange = viewModel::updateActivityRecordingPreferences,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                OfflineMapsCard(
                    mapPacks = state.offlineMapPacks,
                    activeFormat = state.activeOfflineMapFormat,
                    isImporting = state.isImportingOfflineMap,
                    progress = state.offlineMapImportProgress,
                    result = state.offlineMapImportResult,
                    error = state.offlineMapImportError,
                    onImport = actions.onImportOfflineMap,
                    onSelectActiveFormat = viewModel::selectOfflineMapFormat,
                    onDeleteMap = viewModel::deleteOfflineMap,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.SENSORS -> {
            item {
                BleDevicesSettingsSection()
            }
        }
        SettingsSection.CALORIES -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                CalorieDataSourceCard(
                    enabled = state.showOpenVitalsCalculatedCalories,
                    onEnabledChange = viewModel::setShowOpenVitalsCalculatedCalories,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.SLEEP -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                SleepRangeModeCard(
                    selected = state.sleepRangeMode,
                    onSelect = viewModel::selectSleepRangeMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.CYCLE -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                CyclePermissionsCard(
                    availability = state.availability,
                    cyclePermissions = state.cyclePermissions,
                    grantedPermissions = state.grantedPermissions,
                    onGrantPermissions = actions.onGrantCyclePermissions,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.DATA_IMPORT -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                AppleHealthImportCard(
                    availability = state.availability,
                    importPermissions = state.dataImportWritePermissions,
                    grantedPermissions = state.grantedPermissions,
                    isImporting = state.isImportingAppleHealth,
                    progress = state.appleHealthImportProgress,
                    result = state.appleHealthImportResult,
                    error = state.appleHealthImportError,
                    onGrantPermissions = actions.onGrantDataImportPermissions,
                    onImport = actions.onImportAppleHealth,
                    onCopyReport = actions.onCopyAppleHealthReport,
                    onCopyError = actions.onCopyAppleHealthError,
                    onSaveReport = actions.onSaveAppleHealthReport,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.HEALTH_CONNECT -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                HealthConnectSettingsCard(
                    syncEnabled = state.healthConnectSyncEnabled,
                    availability = state.availability,
                    onSyncEnabledChange = viewModel::setHealthConnectSyncEnabled,
                    onManageAccess = actions.onOpenManualPermissionSettings,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                AppLockCard(
                    enabled = state.appLockEnabled,
                    onEnabledChange = viewModel::setAppLockEnabled,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ClearCacheCard(
                    onClear = actions.onClearCache,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.PERMISSIONS -> {
            item { SectionHeader(stringResource(section.titleRes)) }

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
                                requestablePermissions.isNotEmpty() ->
                                    actions.onGrantPermissions(requestablePermissions)
                                manualPermissions.isNotEmpty() -> actions.onOpenManualPermissionSettings()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (state.permissionCategories.isEmpty()) {
                item {
                    OpenVitalsCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
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
                        onGrant = actions.onOpenManualPermissionSettings,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
        SettingsSection.DEBUG_DIAGNOSTICS -> {
            if (BuildConfig.DEBUG) {
                item { SectionHeader(stringResource(section.titleRes)) }
                item {
                    DebugDiagnosticsCard(
                        onSaveLogs = actions.onSaveDebugLogs,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
