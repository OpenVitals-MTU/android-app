package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyCalibrationCard
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HeartRateThresholds
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.Spacing

internal fun LazyListScope.settingsScreenContent(
    section: SettingsSection?,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    actions: SettingsScreenActions,
) {
    when (section) {
        null -> {
            SettingsSection.entries
                .filter { BuildConfig.OPENVITALS_DIAGNOSTICS || it != SettingsSection.DEBUG_DIAGNOSTICS }
                .forEach { settingsSection ->
                    item {
                        SettingsCategoryCard(
                            section = settingsSection,
                            onClick = { actions.onOpenSection(settingsSection) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }

            item { SectionHeader(stringResource(R.string.section_support)) }

            item {
                SupportOpenVitalsCard(
                    onOpenIssues = actions.onOpenIssues,
                    onOpenDiscussion = actions.onOpenDiscussion,
                    onOpenSupport = actions.onOpenSupport,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
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
                    selected = state.unitSystemPreference,
                    resolvedUnitSystem = state.unitSystem,
                    onSelect = viewModel::selectUnitSystem,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                UnitOverridesCard(
                    overrides = state.unitOverrides,
                    baseUnitSystem = state.unitSystem,
                    onSelect = viewModel::selectUnitOverride,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ThemeModeCard(
                    selected = state.appThemeMode,
                    onSelect = viewModel::selectAppThemeMode,
                    dynamicColor = state.dynamicColor,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ChartAggregationCard(
                    selected = state.chartAggregationMode,
                    onSelect = viewModel::setChartAggregationMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                DashboardSortEmptyTilesCard(
                    enabled = state.dashboardSortEmptyTilesLast,
                    onEnabledChange = viewModel::setDashboardSortEmptyTilesLast,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ActivityWeekModeCard(
                    selected = state.activityWeekMode,
                    onSelect = viewModel::selectActivityWeekMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.ACTIVITIES -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                FavoriteActivityCard(
                    selectedExerciseType = state.favoriteActivityExerciseType,
                    onSelect = viewModel::selectFavoriteActivity,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ActivitySplitDistanceCard(
                    selectedMeters = state.activitySplitDistanceMeters,
                    unitSystem = state.effectiveUnitSystem(UnitQuantity.DISTANCE),
                    onSelect = viewModel::setActivitySplitDistance,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                StepDistanceBackfillCard(
                    enabled = state.stepDistanceBackfillEnabled,
                    strideLengthMeters = state.strideLengthMeters,
                    unitSystem = state.effectiveUnitSystem(UnitQuantity.DISTANCE),
                    onSave = viewModel::saveStepDistanceBackfill,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ActivityRecordingPreferencesCard(
                    preferences = state.activityRecordingPreferences,
                    onChange = viewModel::updateActivityRecordingPreferences,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    coMapsPermissionName = viewModel::coMapsPermissionName,
                    onCoMapsPermissionResult = viewModel::onCoMapsPermissionChanged,
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
        // WATCHES routes straight to the bespoke WatchesSettingsScreen (see
        // AppNavigationSettingsRoutes), so it never renders card content here
        // — the branch exists only to keep this `when` exhaustive.
        SettingsSection.WATCHES -> Unit
        SettingsSection.NUTRITION -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                CalorieDataSourceCard(
                    enabled = state.showOpenVitalsCalculatedCalories,
                    onEnabledChange = viewModel::setShowOpenVitalsCalculatedCalories,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                NutritionAverageBasisCard(
                    loggedDaysOnly = state.nutritionAverageLoggedDaysOnly,
                    onLoggedDaysOnlyChange = viewModel::setNutritionAverageLoggedDaysOnly,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            item { SettingsCardSpacer() }
            item {
                HydrationGoalCard(
                    goalLiters = state.hydrationDailyGoalLiters,
                    unitSystem = state.effectiveUnitSystem(UnitQuantity.HYDRATION),
                    onGoalChange = viewModel::setHydrationDailyGoalLiters,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                CaffeinePreferencesCard(
                    preferences = state.caffeinePreferences,
                    bodyProfile = state.bodyProfile,
                    onSave = viewModel::updateCaffeinePreferences,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.BODY_PROFILE -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                BodyProfileCard(
                    profile = state.bodyProfile,
                    // The card's only unit-sensitive input is body weight.
                    unitSystem = state.effectiveUnitSystem(UnitQuantity.WEIGHT),
                    onSave = viewModel::updateBodyProfile,
                    weightMeasured = state.bodyProfileWeightMeasured,
                    heightMeasured = state.bodyProfileHeightMeasured,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                MetabolismCard(
                    preferences = state.caffeinePreferences,
                    onSave = viewModel::updateCaffeinePreferences,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        SettingsSection.RECOVERY -> {
            item { SectionHeader(stringResource(section.titleRes)) }
            item {
                SleepHourStepperCard(
                    title = stringResource(R.string.settings_sleep_night_start_title),
                    body = stringResource(R.string.settings_sleep_night_start_body),
                    hour = state.nightStartHour,
                    onHourChange = viewModel::setNightStartHour,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                SleepHourStepperCard(
                    title = stringResource(R.string.settings_sleep_night_end_title),
                    body = stringResource(R.string.settings_sleep_night_end_body),
                    hour = state.nightEndHour,
                    onHourChange = viewModel::setNightEndHour,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                SettingsStepperCard(
                    title = stringResource(R.string.settings_high_heart_rate_alert_title),
                    body = stringResource(R.string.settings_high_heart_rate_alert_body),
                    valueLabel = stringResource(
                        R.string.settings_heart_rate_bpm_value,
                        state.highHeartRateThresholdBpm,
                    ),
                    onIncrease = {
                        viewModel.setHighHeartRateThresholdBpm(
                            state.highHeartRateThresholdBpm + HeartRateThresholds.STEP_BPM,
                        )
                    },
                    onDecrease = {
                        viewModel.setHighHeartRateThresholdBpm(
                            state.highHeartRateThresholdBpm - HeartRateThresholds.STEP_BPM,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                SettingsStepperCard(
                    title = stringResource(R.string.settings_low_heart_rate_alert_title),
                    body = stringResource(R.string.settings_low_heart_rate_alert_body),
                    valueLabel = stringResource(
                        R.string.settings_heart_rate_bpm_value,
                        state.lowHeartRateThresholdBpm,
                    ),
                    onIncrease = {
                        viewModel.setLowHeartRateThresholdBpm(
                            state.lowHeartRateThresholdBpm + HeartRateThresholds.STEP_BPM,
                        )
                    },
                    onDecrease = {
                        viewModel.setLowHeartRateThresholdBpm(
                            state.lowHeartRateThresholdBpm - HeartRateThresholds.STEP_BPM,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                BodyEnergyCalibrationCard(
                    calibration = state.bodyEnergyCalibration,
                    bodyProfile = state.bodyProfile,
                    // The Body profile card right above owns the birth year;
                    // two boxes for one number would disagree until someone
                    // noticed they were the same number.
                    showBirthYear = false,
                    onSave = actions.onSaveBodyEnergyCalibration,
                    onResetPersonalTuning = actions.onResetBodyEnergyPersonalTuning,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                DerivedMetricsResetCard(
                    isResetting = state.isResettingDerivedMetrics,
                    onReset = actions.onResetDerivedMetrics,
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
	                    isAnalyzing = state.isAnalyzingAppleHealth,
	                    isImporting = state.isImportingAppleHealth,
	                    analysisProgress = state.appleHealthAnalysisProgress,
	                    analysis = state.appleHealthImportAnalysis,
	                    selectedCategories = state.selectedAppleHealthImportCategories,
	                    progress = state.appleHealthImportProgress,
	                    result = state.appleHealthImportResult,
	                    error = state.appleHealthImportError,
	                    permissionDenied = state.appleHealthImportPermissionDenied,
	                    onGrantPermissions = actions.onGrantDataImportPermissions,
	                    onImport = actions.onImportAppleHealth,
	                    onToggleCategory = actions.onToggleAppleHealthImportCategory,
	                    onImportSelected = actions.onImportSelectedAppleHealth,
	                    onCopyReport = actions.onCopyAppleHealthReport,
                    onCopyError = actions.onCopyAppleHealthError,
                    onSaveReport = actions.onSaveAppleHealthReport,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                RouteImportCard(
                    availability = state.availability,
                    importPermissions = state.routeImportWritePermissions,
                    grantedPermissions = state.grantedPermissions,
                    // One bulk importer serves both this card and the FIT
                    // card; each shows only the run it started.
                    isImporting = state.isImportingRouteFiles,
                    progress = state.routeImportProgress.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
                    result = state.routeImportResult.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
                    error = state.routeImportError.takeIf { state.routeImportSource == RouteBulkImportSource.ROUTE_FILES },
                    onGrantPermissions = actions.onGrantRouteImportPermissions,
                    onImportSingle = actions.onImportRouteFile,
                    onImportBulk = actions.onImportRouteFiles,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                FitImportCard(
                    availability = state.availability,
                    importPermissions = state.routeImportWritePermissions,
                    grantedPermissions = state.grantedPermissions,
                    isScanning = state.isScanningFitFolder,
                    folderHadNoFitFiles = state.fitFolderHadNoFitFiles,
                    truncatedAt = state.fitFolderTruncatedAt,
                    scanError = state.fitFolderScanError,
                    isImporting = state.isImportingRouteFiles,
                    progress = state.routeImportProgress.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
                    result = state.routeImportResult.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
                    error = state.routeImportError.takeIf { state.routeImportSource == RouteBulkImportSource.FIT_FOLDER },
                    onGrantPermissions = actions.onGrantRouteImportPermissions,
                    onImport = actions.onImportFitFile,
                    onImportFolder = actions.onImportFitFolder,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                CsvImportCard(
                    onOpenCsvImport = actions.onOpenCsvImport,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
            item {
                ReportExportCard(
                    onOpenReportExport = actions.onOpenReportExport,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        // DEVICE_SYNC routes straight to the bespoke DeviceSyncScreen wizard
        // (see AppNavigationSettingsRoutes), so it never renders card content
        // here — the branch exists only to keep this `when` exhaustive.
        SettingsSection.DEVICE_SYNC -> Unit
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
                MindfulnessIntegrationCard(
                    enabled = state.healthConnectMindfulnessEnabled,
                    onEnabledChange = viewModel::setHealthConnectMindfulnessEnabled,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }

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

            item { SettingsCardSpacer() }
            item {
                AppLockCard(
                    enabled = state.appLockEnabled,
                    onEnabledChange = viewModel::setAppLockEnabled,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { SettingsCardSpacer() }
        }
        SettingsSection.DEBUG_DIAGNOSTICS -> {
            if (BuildConfig.OPENVITALS_DIAGNOSTICS) {
                item { SectionHeader(stringResource(section.titleRes)) }
                item {
                    DebugDiagnosticsCard(
                        onSaveLogs = actions.onSaveDebugLogs,
                        onShareLogs = actions.onShareDebugLogs,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { SettingsCardSpacer() }
                item {
                    ReminderTestCard(
                        onShowTestReminder = viewModel::showTestHydrationReminder,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { SettingsCardSpacer() }
                item {
                    HealthConnectSourcesCard(
                        sources = state.healthConnectSources,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
