package tech.mmarca.openvitals.features.settings

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import java.util.Locale
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportProgress
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportResult
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPack
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPackFormat
import tech.mmarca.openvitals.features.activity.maps.labelRes
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportProgress
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportResult
import tech.mmarca.openvitals.features.imports.applehealth.labelRes
import tech.mmarca.openvitals.features.caffeine.CaffeinePreferencesEditor
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.AppLanguageDropdown
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
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
    OpenVitalsCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),

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
        SettingsSection.SENSORS -> Icons.Outlined.Bluetooth
        SettingsSection.CALORIES -> Icons.Outlined.LocalFireDepartment
        SettingsSection.CAFFEINE -> Icons.Outlined.LocalDrink
        SettingsSection.SLEEP -> Icons.Outlined.Bedtime
        SettingsSection.BODY_ENERGY -> Icons.Outlined.BatteryChargingFull
        SettingsSection.CYCLE -> Icons.Outlined.CalendarMonth
        SettingsSection.DATA_IMPORT -> Icons.Outlined.FolderOpen
        SettingsSection.HEALTH_CONNECT -> Icons.Outlined.HealthAndSafety
        SettingsSection.PERMISSIONS -> Icons.Outlined.Lock
        SettingsSection.DEBUG_DIAGNOSTICS -> Icons.Outlined.BugReport
    }

internal val AppleHealthExportMimeTypes = arrayOf(
    "application/zip",
    "application/xml",
    "text/xml",
    "application/octet-stream",
    "*/*",
)

internal val OfflineMapMimeTypes = arrayOf(
    "application/vnd.pmtiles",
    "application/x-mapsforge-map",
    "application/octet-stream",
    "*/*",
)

private val OfflineMapPackFormat.settingsLabelRes: Int
    @StringRes
    get() = when (this) {
        OfflineMapPackFormat.PMTILES -> R.string.settings_offline_maps_format_pmtiles
        OfflineMapPackFormat.MAPSFORGE -> R.string.settings_offline_maps_format_mapsforge
}

@Composable
internal fun CaffeinePreferencesCard(
    preferences: CaffeinePreferences,
    unitSystem: UnitSystem,
    onSave: (CaffeinePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(preferences) { mutableStateOf(preferences) }
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.settings_caffeine_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_caffeine_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            CaffeinePreferencesEditor(
                preferences = draft,
                unitSystem = unitSystem,
                onChange = { draft = it },
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OpenVitalsTonalButton(onClick = { onSave(draft.copy(profileCompleted = true)) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
internal fun CalorieDataSourceCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
        OpenVitalsOutlinedButton(onClick = { expanded = true }) {
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
internal fun ActivityRecordingPreferencesCard(
    preferences: ActivityRecordingPreferences,
    onChange: (ActivityRecordingPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_activity_recording_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_activity_recording_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_keep_screen_on_title),
                body = stringResource(R.string.settings_activity_recording_keep_screen_on_body),
                checked = preferences.keepScreenOnDuringRecording,
                onCheckedChange = { enabled -> onChange(preferences.copy(keepScreenOnDuringRecording = enabled)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_auto_idle_title),
                body = stringResource(R.string.settings_activity_recording_auto_idle_body),
                checked = preferences.autoIdleEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(autoIdleEnabled = enabled)) },
            )

            ActivityRecordingSegmentedChoice(
                title = stringResource(R.string.settings_activity_recording_idle_timeout_title),
                options = listOf(5, 10, 30, 60),
                selected = preferences.autoIdleTimeoutSeconds,
                enabled = preferences.autoIdleEnabled,
                label = { seconds -> stringResource(R.string.settings_activity_recording_seconds, seconds) },
                onSelect = { seconds -> onChange(preferences.copy(autoIdleTimeoutSeconds = seconds)) },
            )

            ActivityRecordingSegmentedChoice(
                title = stringResource(R.string.settings_activity_recording_accuracy_title),
                options = ActivityRecordingPreferences.AllowedGpsAccuracyMeters,
                selected = preferences.requiredGpsAccuracyMeters,
                label = { meters -> stringResource(R.string.settings_activity_recording_meters, meters) },
                onSelect = { meters -> onChange(preferences.copy(requiredGpsAccuracyMeters = meters)) },
            )

            ActivityRecordingRouteGapChoice(
                selected = preferences.routeGapMeters,
                onSelect = { meters -> onChange(preferences.copy(routeGapMeters = meters)) },
            )

            ActivityRecordingTimeIntervalChoice(
                selected = preferences.recordingTimeIntervalMillis,
                onSelect = { millis -> onChange(preferences.copy(recordingTimeIntervalMillis = millis)) },
            )

            ActivityRecordingDistanceIntervalChoice(
                selected = preferences.recordingDistanceIntervalMeters,
                onSelect = { meters -> onChange(preferences.copy(recordingDistanceIntervalMeters = meters)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_barometer_title),
                body = stringResource(R.string.settings_activity_recording_barometer_body),
                checked = preferences.barometerClimbEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(barometerClimbEnabled = enabled)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_rest_bell_title),
                body = stringResource(R.string.settings_activity_recording_rest_bell_body),
                checked = preferences.restTimerBellEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(restTimerBellEnabled = enabled)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_voice_title),
                body = stringResource(R.string.settings_activity_recording_voice_body),
                checked = preferences.voiceAnnouncementsEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(voiceAnnouncementsEnabled = enabled)) },
            )

            ActivityRecordingNullableChoice(
                title = stringResource(R.string.settings_activity_recording_voice_time_title),
                options = ActivityRecordingPreferences.AllowedVoiceAnnouncementTimeIntervalMinutes,
                selected = preferences.voiceAnnouncementTimeIntervalMinutes,
                enabled = preferences.voiceAnnouncementsEnabled,
                label = { minutes -> stringResource(R.string.activity_entry_recording_split_minutes, minutes) },
                onSelect = { minutes -> onChange(preferences.copy(voiceAnnouncementTimeIntervalMinutes = minutes)) },
            )

            ActivityRecordingNullableChoice(
                title = stringResource(R.string.settings_activity_recording_voice_distance_title),
                options = ActivityRecordingPreferences.AllowedVoiceAnnouncementDistanceIntervalMeters,
                selected = preferences.voiceAnnouncementDistanceIntervalMeters,
                enabled = preferences.voiceAnnouncementsEnabled,
                label = { meters -> stringResource(R.string.settings_activity_recording_meters, meters) },
                onSelect = { meters -> onChange(preferences.copy(voiceAnnouncementDistanceIntervalMeters = meters)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_voice_idle_title),
                body = stringResource(R.string.settings_activity_recording_voice_idle_body),
                checked = preferences.voiceIdleAnnouncementsEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(voiceIdleAnnouncementsEnabled = enabled)) },
                modifier = Modifier.padding(start = 8.dp),
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_activity_recording_voice_lap_title),
                body = stringResource(R.string.settings_activity_recording_voice_lap_body),
                checked = preferences.voiceLapAnnouncementsEnabled,
                onCheckedChange = { enabled -> onChange(preferences.copy(voiceLapAnnouncementsEnabled = enabled)) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
internal fun OfflineMapsCard(
    mapPacks: List<OfflineMapPack>,
    activeFormat: OfflineMapPackFormat?,
    isImporting: Boolean,
    progress: OfflineMapImportProgress?,
    result: OfflineMapImportResult?,
    error: String?,
    onImport: () -> Unit,
    onSelectActiveFormat: (OfflineMapPackFormat?) -> Unit,
    onDeleteMap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val offlineMapsHelpUrl = stringResource(R.string.settings_offline_maps_help_url)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Map,
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
                        text = stringResource(R.string.settings_offline_maps_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_offline_maps_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.settings_offline_maps_help_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsTextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(offlineMapsHelpUrl)),
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.settings_offline_maps_help_link))
            }

            if (mapPacks.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_offline_maps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OfflineMapRenderFormatSelector(
                    mapPacks = mapPacks,
                    activeFormat = activeFormat,
                    onSelect = onSelectActiveFormat,
                )
                mapPacks.forEach { pack ->
                    OfflineMapPackRow(
                        pack = pack,
                        onDelete = { onDeleteMap(pack.id) },
                    )
                }
            }

            result?.let { importResult ->
                Text(
                    text = stringResource(
                        R.string.settings_offline_maps_import_result,
                        importResult.displayName,
                        formatOfflineMapSize(importResult.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.settings_offline_maps_import_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (isImporting) {
                AppleHealthImportProgressBar(modifier = Modifier.fillMaxWidth())
                val importProgress = progress ?: OfflineMapImportProgress()
                Text(
                    text = importProgress.percent?.let { percent ->
                        stringResource(
                            R.string.settings_offline_maps_import_progress_with_percent,
                            stringResource(importProgress.phase.labelRes),
                            percent,
                        )
                    } ?: stringResource(
                        R.string.settings_offline_maps_import_progress,
                        stringResource(importProgress.phase.labelRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.settings_offline_maps_import_background),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OpenVitalsOutlinedButton(
                onClick = onImport,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.widthIn(min = 6.dp))
                Text(
                    if (isImporting) {
                        stringResource(R.string.settings_offline_maps_importing)
                    } else {
                        stringResource(R.string.settings_offline_maps_import_action)
                    },
                )
            }
        }
    }
}

@Composable
private fun OfflineMapRenderFormatSelector(
    mapPacks: List<OfflineMapPack>,
    activeFormat: OfflineMapPackFormat?,
    onSelect: (OfflineMapPackFormat?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_offline_maps_render_format_title),
            style = MaterialTheme.typography.bodyMedium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            OfflineMapPackFormat.entries.forEachIndexed { index, format ->
                val packCount = mapPacks.count { it.format == format }
                SegmentedButton(
                    selected = activeFormat == format,
                    enabled = packCount > 0,
                    onClick = { onSelect(format) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = OfflineMapPackFormat.entries.size,
                    ),
                    label = {
                        Text(
                            stringResource(
                                R.string.settings_offline_maps_render_format_option,
                                stringResource(format.settingsLabelRes),
                                packCount,
                            ),
                        )
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_offline_maps_render_format_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OfflineMapPackRow(
    pack: OfflineMapPack,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.settings_offline_maps_pack_detail,
                    stringResource(pack.format.settingsLabelRes),
                    pack.originalFileName,
                    formatOfflineMapSize(pack.sizeBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OpenVitalsTextButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.action_delete),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatOfflineMapSize(bytes: Long): String {
    if (bytes < 1_000L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1_000.0
    var unitIndex = 0
    while (value >= 1_000.0 && unitIndex < units.lastIndex) {
        value /= 1_000.0
        unitIndex += 1
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ActivityRecordingSegmentedChoice(
    title: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (Int) -> String,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            options.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = selected == value,
                    enabled = enabled,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = { Text(label(value)) },
                )
            }
        }
    }
}

@Composable
private fun ActivityRecordingRouteGapChoice(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf<Int?>(100, 200, 500, null)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_activity_recording_route_gap_title),
            style = MaterialTheme.typography.bodyMedium,
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            options.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = {
                        Text(
                            if (value == null) {
                                stringResource(R.string.settings_activity_recording_off)
                            } else {
                                stringResource(R.string.settings_activity_recording_meters, value)
                            }
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivityRecordingTimeIntervalChoice(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ActivityRecordingSegmentedChoice(
        title = stringResource(R.string.settings_activity_recording_time_interval_title),
        options = ActivityRecordingPreferences.AllowedRecordingTimeIntervalMillis,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        label = { millis ->
            if (millis == 500) {
                stringResource(R.string.settings_activity_recording_half_second)
            } else {
                stringResource(R.string.settings_activity_recording_seconds, millis / 1_000)
            }
        },
    )
}

@Composable
private fun ActivityRecordingDistanceIntervalChoice(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ActivityRecordingNullableChoice(
        title = stringResource(R.string.settings_activity_recording_distance_interval_title),
        options = ActivityRecordingPreferences.AllowedRecordingDistanceIntervalMeters,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        label = { meters -> stringResource(R.string.settings_activity_recording_meters, meters) },
        offLabel = stringResource(R.string.settings_activity_recording_auto),
    )
}

@Composable
private fun ActivityRecordingNullableChoice(
    title: String,
    options: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    offLabel: String = stringResource(R.string.settings_activity_recording_off),
    label: @Composable (Int) -> String,
) {
    val allOptions = options.map<Int, Int?> { it } + null
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            allOptions.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = selected == value,
                    enabled = enabled,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = allOptions.size,
                    ),
                    label = {
                        Text(if (value == null) offLabel else label(value))
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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
internal fun CyclePermissionsCard(
    availability: HealthConnectAvailability,
    cyclePermissions: Set<String>,
    grantedPermissions: Set<String>,
    onGrantPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grantedCount = cyclePermissions.count { it in grantedPermissions }
    val missingPermissions = cyclePermissions - grantedPermissions
    val healthConnectAvailable = availability == HealthConnectAvailability.AVAILABLE
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
                Text(text = stringResource(R.string.settings_cycle_permissions_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_cycle_permissions_granted, grantedCount, cyclePermissions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (missingPermissions.isEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.onboarding_status_granted),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                OpenVitalsTonalButton(
                    onClick = onGrantPermissions,
                    enabled = healthConnectAvailable,
                ) {
                    Text(stringResource(R.string.action_grant))
                }
            }
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
    onCopyError: (String) -> Unit,
    onSaveReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grantedCount = importPermissions.count { it in grantedPermissions }
    val missingPermissions = importPermissions - grantedPermissions
    val healthConnectAvailable = availability == HealthConnectAvailability.AVAILABLE
    val canImport = healthConnectAvailable && missingPermissions.isEmpty() && !isImporting

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
                    OpenVitalsOutlinedButton(
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
                    OpenVitalsOutlinedButton(
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
                val errorText = stringResource(R.string.settings_apple_health_import_error, error)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OpenVitalsOutlinedButton(
                        onClick = { onCopyError(errorText) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.widthIn(min = 6.dp))
                        Text(stringResource(R.string.settings_apple_health_import_copy_error))
                    }
                    SelectionContainer {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
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
                OpenVitalsTonalButton(
                    onClick = onGrantPermissions,
                    enabled = healthConnectAvailable && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.settings_apple_health_import_grant))
                }
            }

            OpenVitalsOutlinedButton(
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
    val description = if (!category.available && unavailableReasonRes != null) {
        stringResource(unavailableReasonRes)
    } else if (category.manualPermissions.isNotEmpty() && missingManualCount > 0) {
        stringResource(
            R.string.onboarding_category_additional_data_access_manual_note,
            stringResource(category.descriptionRes),
        )
    } else {
        stringResource(category.descriptionRes)
    }

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (granted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
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
                }
                if (granted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.onboarding_status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                } else if (!category.available) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.onboarding_status_not_supported),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!granted && category.available) {
                OpenVitalsTonalButton(
                    onClick = onGrant,
                    enabled = availability == HealthConnectAvailability.AVAILABLE,
                    modifier = Modifier.align(Alignment.End),
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
internal fun HealthConnectSettingsCard(
    syncEnabled: Boolean,
    availability: HealthConnectAvailability,
    onSyncEnabledChange: (Boolean) -> Unit,
    onManageAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_health_connect_sync_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_health_connect_sync_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncEnabledChange,
                    enabled = availability == HealthConnectAvailability.AVAILABLE,
                )
            }
            OpenVitalsOutlinedButton(
                onClick = onManageAccess,
                enabled = availability == HealthConnectAvailability.AVAILABLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.settings_health_connect_manage_access))
            }
        }
    }
}

@Composable
internal fun AppLockCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_app_lock_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.settings_app_lock_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
internal fun DebugDiagnosticsCard(
    onSaveLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
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
                        text = stringResource(R.string.settings_debug_logs_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_debug_logs_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            OpenVitalsOutlinedButton(
                onClick = onSaveLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.widthIn(min = 6.dp))
                Text(stringResource(R.string.settings_debug_logs_save))
            }
        }
    }
}

@Composable
internal fun SupportOpenVitalsCard(
    onOpenIssues: () -> Unit,
    onOpenDiscussion: () -> Unit,
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
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
                        text = stringResource(R.string.settings_support_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_support_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            SupportLinkButton(
                labelRes = R.string.settings_support_issues_action,
                onClick = onOpenIssues,
                modifier = Modifier.padding(top = 12.dp),
            )
            SupportLinkButton(
                labelRes = R.string.settings_support_discussion_action,
                onClick = onOpenDiscussion,
                modifier = Modifier.padding(top = 8.dp),
            )
            SupportLinkButton(
                labelRes = R.string.settings_support_action,
                onClick = onOpenSupport,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SupportLinkButton(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsOutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.widthIn(min = 6.dp))
        Text(stringResource(labelRes))
    }
}

@Composable
internal fun PrivacyInfoCard(
    onOpenPrivacyPolicy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

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
            if (onOpenPrivacyPolicy != null) {
                OpenVitalsTextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_privacy_policy_link))
                }
            }
        }
    }
}
