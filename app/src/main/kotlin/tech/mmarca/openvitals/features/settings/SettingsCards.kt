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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportProgress
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.imports.applehealth.labelRes
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.HydrationColor

@Composable
internal fun SettingsCardSpacer() {
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun SettingsVersionText() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryCard(
    section: SettingsSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
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
                imageVector = section.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(section.summaryRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal val SettingsSection.icon: ImageVector
    get() = when (this) {
        SettingsSection.DISPLAY -> Icons.Outlined.Settings
        SettingsSection.ACTIVITIES -> Icons.AutoMirrored.Outlined.DirectionsRun
        SettingsSection.CALORIES -> Icons.Outlined.LocalFireDepartment
        SettingsSection.SLEEP -> Icons.Outlined.Bedtime
        SettingsSection.CYCLE -> Icons.Outlined.CalendarMonth
        SettingsSection.DATA_IMPORT -> Icons.Outlined.FolderOpen
        SettingsSection.PERMISSIONS -> Icons.Outlined.Lock
    }

internal val AppleHealthExportMimeTypes = arrayOf(
    "application/zip",
    "application/xml",
    "text/xml",
    "application/octet-stream",
    "*/*",
)

@Composable
internal fun CalorieDataSourceCard(
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
internal fun LanguageCard(
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
internal fun ThemeModeCard(
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
internal fun ActivityWeekModeCard(
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
internal fun FavoriteActivityCard(
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
internal fun FavoriteActivityDropdown(
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
internal fun SleepRangeModeCard(
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
internal fun UnitSystemCard(
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
internal fun CycleTrackingCard(
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
internal fun AppleHealthImportCard(
    availability: HealthConnectAvailability,
    importPermissions: Set<String>,
    grantedPermissions: Set<String>,
    isImporting: Boolean,
    progress: AppleHealthImportProgress?,
    result: AppleHealthImportResult?,
    error: String?,
    onGrantPermissions: () -> Unit,
    onImport: () -> Unit,
    onCopyReport: (String) -> Unit,
    onSaveReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grantedCount = importPermissions.count { it in grantedPermissions }
    val missingPermissions = importPermissions - grantedPermissions
    val healthConnectAvailable = availability == HealthConnectAvailability.AVAILABLE
    val canImport = healthConnectAvailable && missingPermissions.isEmpty() && !isImporting

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_apple_health_import_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_apple_health_import_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.settings_apple_health_import_permissions,
                    grantedCount,
                    importPermissions.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            result?.let { importResult ->
                Text(
                    text = stringResource(
                        R.string.settings_apple_health_import_result,
                        importResult.importedRecords,
                        importResult.duplicateSkippedRecords,
                        importResult.unsupportedElements,
                        importResult.skippedRecords,
                        importResult.failedRecords,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onCopyReport(importResult.shareableReportText) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.widthIn(min = 6.dp))
                        Text(stringResource(R.string.settings_apple_health_import_copy_report))
                    }
                    OutlinedButton(
                        onClick = onSaveReport,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.widthIn(min = 6.dp))
                        Text(stringResource(R.string.settings_apple_health_import_save_report))
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.settings_apple_health_import_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (isImporting) {
                AppleHealthImportProgressBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                val importProgress = progress ?: AppleHealthImportProgress()
                Text(
                    text = stringResource(
                        R.string.settings_apple_health_import_progress,
                        stringResource(importProgress.phase.labelRes),
                        importProgress.parsedElements,
                        importProgress.importedRecords,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.settings_apple_health_import_background),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (missingPermissions.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onGrantPermissions,
                    enabled = healthConnectAvailable && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.settings_apple_health_import_grant))
                }
            }

            OutlinedButton(
                onClick = onImport,
                enabled = canImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    if (isImporting) {
                        stringResource(R.string.settings_apple_health_importing)
                    } else {
                        stringResource(R.string.settings_apple_health_import_action)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppleHealthImportProgressBar(modifier: Modifier = Modifier) {
    val strokeWidth = with(LocalDensity.current) { 5.dp.toPx() }
    val progressStroke = remember(strokeWidth) {
        Stroke(width = strokeWidth, cap = StrokeCap.Round)
    }
    LinearWavyProgressIndicator(
        modifier = modifier.height(18.dp),
        color = HydrationColor.copy(alpha = 0.86f),
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        stroke = progressStroke,
        trackStroke = progressStroke,
        wavelength = 34.dp,
        waveSpeed = 34.dp,
    )
}

@Composable
internal fun PermissionCategoryCard(
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
internal fun PrivacyInfoCard(modifier: Modifier = Modifier) {
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
