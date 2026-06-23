package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader

enum class SettingsSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
) {
    DISPLAY(
        titleRes = R.string.settings_display_group_title,
        summaryRes = R.string.settings_display_group_body,
    ),
    ACTIVITIES(
        titleRes = R.string.settings_activities_group_title,
        summaryRes = R.string.settings_activities_group_body,
    ),
    CALORIES(
        titleRes = R.string.settings_calories_group_title,
        summaryRes = R.string.settings_calories_group_body,
    ),
    SLEEP(
        titleRes = R.string.settings_sleep_group_title,
        summaryRes = R.string.settings_sleep_group_body,
    ),
    CYCLE(
        titleRes = R.string.settings_cycle_group_title,
        summaryRes = R.string.settings_cycle_group_body,
    ),
    DATA_IMPORT(
        titleRes = R.string.settings_data_import_group_title,
        summaryRes = R.string.settings_data_import_group_body,
    ),
    PERMISSIONS(
        titleRes = R.string.settings_permissions_group_title,
        summaryRes = R.string.settings_permissions_group_body,
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    section: SettingsSection? = null,
    onOpenSection: (SettingsSection) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val unableToOpenPermissions = stringResource(R.string.onboarding_unable_open_permissions)
    val reportCopied = stringResource(R.string.settings_apple_health_import_report_copied)
    val reportSaved = stringResource(R.string.settings_apple_health_import_report_saved)
    val reportSaveFailed = stringResource(R.string.settings_apple_health_import_report_save_failed)
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

    val requestDataImportPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    val appleHealthExportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importAppleHealthExport(uri)
        }
    }

    val appleHealthReportSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val reportText = state.appleHealthImportResult?.shareableReportText.orEmpty()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(reportText.toByteArray())
                } ?: error("Unable to open destination.")
            }.fold(
                onSuccess = {
                    Toast.makeText(context, reportSaved, Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, reportSaveFailed, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    if (state.isLoading) {
        FullScreenLoading()
        return
    }

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
            when (section) {
                null -> {
                    SettingsSection.entries.forEach { settingsSection ->
                        item {
                            SettingsCategoryCard(
                                section = settingsSection,
                                onClick = { onOpenSection(settingsSection) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }

                    item { SectionHeader(stringResource(R.string.section_privacy)) }

                    item {
                        PrivacyInfoCard(modifier = Modifier.padding(horizontal = 16.dp))
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
                            onGrantPermissions = {
                                if (state.availability == HealthConnectAvailability.AVAILABLE) {
                                    requestCyclePermissions.launch(state.cyclePermissions - state.grantedPermissions)
                                }
                            },
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
                            onGrantPermissions = {
                                requestDataImportPermissions.launch(state.missingDataImportWritePermissions)
                            },
                            onImport = {
                                appleHealthExportPicker.launch(AppleHealthExportMimeTypes)
                            },
                            onCopyReport = { reportText ->
                                clipboardManager.setText(AnnotatedString(reportText))
                                Toast.makeText(context, reportCopied, Toast.LENGTH_SHORT).show()
                            },
                            onSaveReport = {
                                appleHealthReportSaver.launch("openvitals-apple-health-import-report.txt")
                            },
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
                                            requestAllPermissions.launch(requestablePermissions)
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
                }
            }
        }
    }
}

