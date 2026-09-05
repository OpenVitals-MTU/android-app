package tech.mmarca.openvitals.features.watches

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval
import tech.mmarca.openvitals.devices.garmin.GarminAgpsFileState
import tech.mmarca.openvitals.devices.garmin.GarminAgpsKind
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminSettingsService
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton

/** The watch's settings tree browser exists, so the rows that open it are live. */
internal const val WatchSettingsTreeAvailable: Boolean = true

/** The root of the watch's settings tree. */
internal const val WatchSettingsRootScreenId: Int = GarminSettingsService.ROOT_SCREEN_ID

/** The alarms screen's well-known id. */
internal const val WatchSettingsAlarmsScreenId: Int = GarminSettingsService.ALARMS_SCREEN_ID

/**
 * One watch, and everything about it. Order: status, actions, configuration,
 * with removal at the bottom where a mis-tap cannot reach it.
 */
@Composable
fun WatchDeviceScreen(
    viewModel: WatchDeviceViewModel,
    onOpenData: (String) -> Unit,
    onOpenNotifications: (String) -> Unit,
    onOpenWatchSettings: (String, Int) -> Unit,
    onRemoved: () -> Unit,
    onTitleChanged: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = state.device

    LaunchedEffect(device?.displayName) { onTitleChanged(device?.displayName) }
    DisposableEffect(Unit) { onDispose { onTitleChanged(null) } }

    if (device == null) {
        // Removed while this screen was open, or opened from a stale entry.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data))
        }
        return
    }

    val calendarPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The toggle is set either way; without the grant the row says what is missing.
        viewModel.setCalendarSync(true)
        if (!granted) viewModel.refreshCalendarPermission()
    }

    // CoMaps' own permission, named after the installed flavour. Asked here so a
    // wearer who never records can still grant it.
    val coMapsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshCoMapsPermission() }

    val ephemerisPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importAgps) }

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

    // Only a Garmin GFDI device has sync, settings and find. WearOS speaks none of it.
    val isGarmin = device.isGarminGfdi
    // A bike computer records rides, not wrist wellness; its Data view is hidden.
    val isBikeComputer = device.isBikeComputer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatusCard(
            device = device,
            sync = state.sync,
            isGarmin = isGarmin,
            isBikeComputer = isBikeComputer,
            onRename = { showRenameDialog = true },
        )

        if (isGarmin) {
            ActionsRow(
                device = device,
                state = state,
                isBikeComputer = isBikeComputer,
                onOpenData = { onOpenData(device.id) },
                onSync = viewModel::syncNow,
                onOpenAlarms = {
                    onOpenWatchSettings(device.id, WatchSettingsAlarmsScreenId)
                },
                onToggleFind = viewModel::toggleFind,
            )
            val finding = state.find.isFindingDevice(device.id)
            if (finding || state.find.findFailed) {
                Text(
                    text = if (finding) {
                        stringResource(R.string.settings_watch_find_ringing)
                    } else {
                        stringResource(R.string.settings_watch_find_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.find.findFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            state.sync.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        // Only for a watch that has a settings tree.
        if (WatchSettingsTreeAvailable && isGarmin &&
            state.supports(GarminCapability.REALTIME_SETTINGS)
        ) {
            SectionHeader(stringResource(R.string.settings_watch_settings_section))
            OnDeviceSettingsRow(
                onOpen = { onOpenWatchSettings(device.id, WatchSettingsRootScreenId) },
            )
        }

        // `supports` treats an unknown capability as present, so an unsynced watch gets the row.
        if (isGarmin && state.supports(GarminCapability.GNCS)) {
            SectionHeader(stringResource(R.string.settings_watch_notifications_section))
            NotificationsRow(onOpen = { onOpenNotifications(device.id) })
        }

        // A bike computer can broadcast BLE sensors. Opt-in here, since broadcast
        // mode must be detected while the device is live.
        if (device.isLiveSensorCapable) {
            SectionHeader(stringResource(R.string.settings_bike_live_sensor_section))
            LiveSensorSection(
                device = device,
                isDetecting = state.isDetectingSensors,
                foundNothing = state.detectFoundNothing,
                onDetect = viewModel::detectBroadcastSensors,
            )
        }

        SectionHeader(stringResource(R.string.settings_watch_section_device))
        if (isGarmin) {
            // First: the schedule decides whether this screen is ever visited again.
            AutoSyncCard(
                selected = state.autoSync,
                onSelect = viewModel::setAutoSync,
            )
            OpenVitalsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_watch_stay_connected),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.stayConnected,
                            onCheckedChange = { viewModel.setStayConnected(it) },
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_watch_stay_connected_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Only with the link held: a stream needs a connection.
            if (state.stayConnected) {
                OpenVitalsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.settings_watch_live_readings),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = state.liveReadings,
                                onCheckedChange = { viewModel.setLiveReadings(it) },
                            )
                        }
                        Text(
                            text = state.liveHeartRateBpm
                                ?.let { stringResource(R.string.settings_watch_live_now, it) }
                                ?: stringResource(R.string.settings_watch_live_readings_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            CalendarSyncCard(
                state = state,
                onToggle = { enabled ->
                    if (enabled) {
                        calendarPermission.launch(android.Manifest.permission.READ_CALENDAR)
                    } else {
                        viewModel.setCalendarSync(false)
                    }
                },
            )
            NavigationOnWatchCard(
                state = state,
                onToggle = { enabled ->
                    viewModel.setNavigationOnWatch(enabled)
                    // The toggle sticks either way; without the grant the card says what is missing.
                    if (enabled) viewModel.coMapsPermissionName()?.let(coMapsPermission::launch)
                },
            )
            EphemerisCard(
                state = state,
                onImport = { ephemerisPicker.launch(arrayOf("*/*")) },
                onForget = viewModel::forgetAgps,
            )
        }
        OpenVitalsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.settings_watch_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = device.enabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                )
            }
        }
        // Last, and its own card: a destructive action wants distance.
        OpenVitalsCard {
            Text(
                text = stringResource(R.string.settings_watch_remove),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRemoveDialog = true }
                    .padding(Spacing.lg),
            )
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = device.displayName,
            onSave = { name ->
                viewModel.rename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
    if (showRemoveDialog) {
        ConfirmRemoveWatchDialog(
            deviceName = device.displayName,
            onConfirm = {
                showRemoveDialog = false
                viewModel.removeDevice()
                onRemoved()
            },
            onDismiss = { showRemoveDialog = false },
        )
    }
}

@Composable
private fun StatusCard(
    device: BleSensorDevice,
    sync: DeviceSyncUiState,
    isGarmin: Boolean,
    isBikeComputer: Boolean,
    onRename: () -> Unit,
) {
    // A WearOS watch has no sync, so the status line names the device.
    val statusLine = buildString {
        val syncedAt = device.lastSyncedAt
        if (!isGarmin) {
            append(device.bluetoothName ?: device.address)
        } else if (syncedAt == null) {
            append(stringResource(R.string.settings_watch_never_synced))
        } else {
            append(
                stringResource(
                    R.string.settings_watch_last_synced,
                    formatWatchSyncTime(syncedAt),
                ),
            )
            // Only after a sync this session; the count is not persisted.
            val files = sync.lastFileCount
            if (files != null && files > 0) {
                append(" · ")
                append(
                    pluralStringResource(
                        R.plurals.settings_watch_synced_files,
                        files,
                        files,
                    ),
                )
            }
        }
    }

    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
        ) {
            WatchAvatar(
                size = 44,
                icon = if (isBikeComputer) Icons.AutoMirrored.Outlined.DirectionsBike else null,
            )
            Spacer(modifier = Modifier.width(WatchRowIconGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (device.enabled) {
                        stringResource(R.string.settings_watch_connected)
                    } else {
                        stringResource(R.string.settings_watch_not_connected)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            device.batteryPercent?.let { battery ->
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = "$battery%",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            OpenVitalsIconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.settings_watch_rename),
                )
            }
        }
    }
}

@Composable
internal fun ActionsRow(
    device: BleSensorDevice,
    state: WatchDeviceUiState,
    isBikeComputer: Boolean,
    onOpenData: () -> Unit,
    onSync: () -> Unit,
    onOpenAlarms: () -> Unit,
    onToggleFind: () -> Unit,
) {
    val finding = state.find.isFindingDevice(device.id)
    // One radio: syncing and finding cannot overlap. Stopping this find stays available.
    val busy = (state.sync.isSyncing || state.find.findingDeviceId != null) && !finding
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
    ) {
        if (!isBikeComputer) {
            WatchAction(
                icon = Icons.Outlined.Insights,
                label = stringResource(R.string.settings_watch_action_data),
                enabled = true,
                onClick = onOpenData,
            )
        }
        WatchAction(
            icon = Icons.Outlined.Sync,
            label = stringResource(R.string.settings_watch_action_sync),
            busy = state.sync.isSyncingDevice(device.id),
            // One radio: disabled while any watch is syncing.
            enabled = !busy && !finding && !state.sync.isSyncing,
            onClick = onSync,
        )
        // Alarms are a screen in the settings tree at a well-known id.
        if (WatchSettingsTreeAvailable && state.supports(GarminCapability.REALTIME_SETTINGS)) {
            WatchAction(
                icon = Icons.Outlined.Alarm,
                label = stringResource(R.string.settings_watch_action_alarms),
                enabled = !busy,
                onClick = onOpenAlarms,
            )
        }
        if (state.supports(GarminCapability.FIND_MY_WATCH)) {
            // A toggle: the watch alerts for a minute unless stopped, so the same control stops it.
            WatchAction(
                icon = if (finding) Icons.Outlined.Stop else Icons.Outlined.WifiTethering,
                label = if (finding) {
                    stringResource(R.string.settings_watch_find_stop)
                } else {
                    stringResource(R.string.settings_watch_action_find)
                },
                enabled = !busy,
                onClick = onToggleFind,
            )
        }
    }
}

/**
 * How often the watch syncs on its own. A segmented row, so the choice in
 * force is visible at a glance. The body says the watch must be in range and
 * Android picks the moment, or a fifteen-minute drift reads as broken.
 */
@Composable
private fun AutoSyncCard(
    selected: AutoSyncInterval,
    onSelect: (AutoSyncInterval) -> Unit,
) {
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.settings_watch_auto_sync),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_watch_auto_sync_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
            ) {
                AutoSyncInterval.entries.forEachIndexed { index, interval ->
                    SegmentedButton(
                        selected = selected == interval,
                        onClick = { onSelect(interval) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AutoSyncInterval.entries.size,
                        ),
                        label = {
                            Text(
                                text = stringResource(
                                    when (interval) {
                                        AutoSyncInterval.OFF ->
                                            R.string.settings_watch_auto_sync_off
                                        AutoSyncInterval.EVERY_30_MINUTES ->
                                            R.string.settings_watch_auto_sync_30m
                                        AutoSyncInterval.HOURLY ->
                                            R.string.settings_watch_auto_sync_1h
                                        AutoSyncInterval.EVERY_2_HOURS ->
                                            R.string.settings_watch_auto_sync_2h
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

/** The watch's calendar glance. Off by default; asks for the OS permission when switched on. */
@Composable
private fun CalendarSyncCard(
    state: WatchDeviceUiState,
    onToggle: (Boolean) -> Unit,
) {
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_watch_calendar),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.calendarSync,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.settings_watch_calendar_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.calendarPermissionMissing) {
                Text(
                    text = stringResource(R.string.settings_watch_calendar_no_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * CoMaps guidance on the wrist, over the notification link: one
 * notification, updated in place. Off by default and complete in itself.
 */
@Composable
private fun NavigationOnWatchCard(
    state: WatchDeviceUiState,
    onToggle: (Boolean) -> Unit,
) {
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_watch_navigation),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.navigationOnWatch,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.settings_watch_navigation_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.coMapsPermissionMissing) {
                Text(
                    text = stringResource(R.string.settings_watch_navigation_no_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * GPS ephemeris, which turns a two-minute cold fix into seconds. This app has
 * no INTERNET permission, so the user supplies the file. The URL the watch
 * asked for says which file this chipset wants.
 */
@Composable
private fun EphemerisCard(
    state: WatchDeviceUiState,
    onImport: () -> Unit,
    onForget: (GarminAgpsKind) -> Unit,
) {
    val file = state.agps.files.firstOrNull()
    OpenVitalsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_watch_agps),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onImport) {
                    Text(
                        stringResource(
                            if (file == null) {
                                R.string.settings_watch_agps_import
                            } else {
                                R.string.settings_watch_agps_replace
                            },
                        ),
                    )
                }
            }
            Text(
                text = ephemerisSummary(file),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (file?.problem != null) {
                Text(
                    text = stringResource(R.string.settings_watch_agps_unusable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.agpsMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.agps.requestedUrls.lastOrNull()?.let { url ->
                Text(
                    text = stringResource(R.string.settings_watch_agps_asked_for, url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (file != null) {
                TextButton(onClick = { onForget(file.kind) }) {
                    Text(
                        text = stringResource(R.string.settings_watch_agps_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** What the card says under its title: the file, or the offer of one. */
@Composable
private fun ephemerisSummary(file: GarminAgpsFileState?): String {
    if (file == null) return stringResource(R.string.settings_watch_agps_body)
    val imported = DateUtils.getRelativeTimeSpanString(
        file.importedAt.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val kind = stringResource(
        when (file.kind) {
            GarminAgpsKind.CONSTELLATION_TAR -> R.string.settings_watch_agps_kind_tar
            GarminAgpsKind.RX_NETWORKS -> R.string.settings_watch_agps_kind_rx
            GarminAgpsKind.SONY_CPE -> R.string.settings_watch_agps_kind_sony
        },
    )
    val served = file.lastServedAt?.let {
        DateUtils.getRelativeTimeSpanString(
            it.toEpochMilli(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val detail = if (served == null) {
        stringResource(R.string.settings_watch_agps_never_taken, imported)
    } else {
        stringResource(R.string.settings_watch_agps_taken, imported, served)
    }
    return "$kind · $detail"
}

/** The way into the watch's own settings tree. */
@Composable
internal fun OnDeviceSettingsRow(onOpen: () -> Unit) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(Spacing.lg),
        ) {
            Icon(
                imageVector = Icons.Outlined.BrightnessMedium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_watch_on_device_settings),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_watch_on_device_settings_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

/** Mirrors phone notifications to the watch. The switches live on the notifications screen. */
@Composable
private fun NotificationsRow(onOpen: () -> Unit) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(Spacing.lg),
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_watch_notifications_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_watch_notifications_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

/** The BLE broadcast controls for a bike computer: its capabilities, and a redetect button. */
@Composable
internal fun LiveSensorSection(
    device: BleSensorDevice,
    isDetecting: Boolean,
    foundNothing: Boolean,
    onDetect: () -> Unit,
) {
    OpenVitalsCard {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(R.string.settings_bike_live_sensor_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (device.capabilities.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.md),
                ) {
                    device.capabilities.forEach { capability ->
                        AssistChip(
                            onClick = {},
                            label = { Text(capabilityLabel(capability)) },
                        )
                    }
                }
            }
            if (foundNothing) {
                Text(
                    text = stringResource(R.string.settings_bike_no_broadcast),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            OutlinedButton(
                onClick = onDetect,
                enabled = !isDetecting,
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.lg),
                        strokeWidth = WatchProgressStroke,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(WatchInlineIconSize),
                    )
                }
                Text(
                    text = if (isDetecting) {
                        stringResource(R.string.settings_bike_detecting)
                    } else {
                        stringResource(R.string.settings_bike_detect_sensors)
                    },
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(WatchChipGap))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_watch_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_watch_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Gap inside a status chip. */
private val WatchChipGap = 10.dp
/** Inline status icons, between bodySmall text sizes. */
private val WatchInlineIconSize = 18.dp
/** The small in-row progress spinner's stroke. */
private val WatchProgressStroke = 2.dp
/** Gap between a row's leading icon and its text. */
private val WatchRowIconGap = 14.dp
