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
import tech.mmarca.openvitals.devices.garmin.GarminAgpsFileState
import tech.mmarca.openvitals.devices.garmin.GarminAgpsKind
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminSettingsService
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton

/**
 * The watch's on-device settings tree shipped with sub-milestone 7f
 * (`WatchSettingsScreen` behind the already-registered route), so the rows
 * that open it are live.
 */
internal const val WatchSettingsTreeAvailable: Boolean = true

/** The root of the watch's settings tree (7f); Flutter's `rootScreenId`. */
internal const val WatchSettingsRootScreenId: Int = GarminSettingsService.ROOT_SCREEN_ID

/** The alarms screen's well-known id inside that tree (7f). */
internal const val WatchSettingsAlarmsScreenId: Int = GarminSettingsService.ALARMS_SCREEN_ID

/**
 * One watch, and everything about it. Port of the Flutter build's
 * `watch_device_screen.dart`.
 *
 * The order is fixed and means something: **status** (what you came to
 * check), **actions** (verbs, as icons), **configuration** (least often
 * touched, so last, with removal at the bottom where a mis-tap cannot reach
 * it).
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
        // The toggle is set either way; without the grant the row says what
        // is missing rather than silently syncing nothing.
        viewModel.setCalendarSync(true)
        if (!granted) viewModel.refreshCalendarPermission()
    }

    val ephemerisPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importAgps) }

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

    // Only a Garmin GFDI device (watch or Edge bike computer) has GFDI
    // sync/settings/find. A WearOS watch speaks none of it — its heart rate
    // streams over BLE and its recorded data arrives via Health Connect — so
    // those controls are hidden rather than shown dead.
    val isGarmin = device.isGarminGfdi
    // A bike computer records rides, not wrist wellness — its "Data" view
    // would always be empty, so it is hidden. Its rides import into the
    // normal activity history.
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

        // Only for a Garmin watch that says it HAS a settings tree — and only
        // once 7f's browser exists to open. A watch without REALTIME_SETTINGS
        // (or any WearOS watch) has no such screen to browse.
        if (WatchSettingsTreeAvailable && isGarmin &&
            state.supports(GarminCapability.REALTIME_SETTINGS)
        ) {
            SectionHeader(stringResource(R.string.settings_watch_settings_section))
            OnDeviceSettingsRow(
                onOpen = { onOpenWatchSettings(device.id, WatchSettingsRootScreenId) },
            )
        }

        // Only for a watch that says it speaks GNCS. `supports` treats an
        // unknown capability as present, so a watch that has never synced
        // still gets the row rather than having the feature hidden from it.
        if (isGarmin && state.supports(GarminCapability.GNCS)) {
            SectionHeader(stringResource(R.string.settings_watch_notifications_section))
            NotificationsRow(onOpen = { onOpenNotifications(device.id) })
        }

        // A bike computer can broadcast standard-BLE sensors into a
        // recording. The role is opt-in from here because broadcast mode is
        // usually only on during a ride, so it must be detected while the
        // device is live.
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
            // Only offered with the link held: a stream needs a connection to
            // ride on, and a switch that silently does nothing is worse than
            // one that is not there.
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
        // Last, and its own card: a destructive action wants distance from
        // the switch above it, not adjacency.
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
    // A WearOS watch has no sync concept, so its status line names the device
    // rather than a last-sync time it will never have.
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
            // Only after a sync THIS session — the count is not persisted,
            // and an invented one would be worse than none.
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
    // One radio: syncing and finding cannot overlap, and neither can a find
    // on a second watch. Stopping THIS find stays available throughout.
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
            // One radio, one sync: disabled while ANY watch is syncing.
            enabled = !busy && !finding && !state.sync.isSyncing,
            onClick = onSync,
        )
        // Alarms are not a feature of their own: they are a screen in the
        // watch's settings tree, reached at a well-known id — so the action
        // arrives with 7f's browser.
        if (WatchSettingsTreeAvailable && state.supports(GarminCapability.REALTIME_SETTINGS)) {
            WatchAction(
                icon = Icons.Outlined.Alarm,
                label = stringResource(R.string.settings_watch_action_alarms),
                enabled = !busy,
                onClick = onOpenAlarms,
            )
        }
        if (state.supports(GarminCapability.FIND_MY_WATCH)) {
            // A toggle, not a one-shot: the watch alerts for a minute unless
            // stopped, so the same control stops it — in place, because you
            // are rummaging through a bag one-handed.
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
 * The watch's calendar glance, fed from the phone's calendar. The single most
 * personal thing this app can hand a watch, so it is off by default, asks for
 * the OS permission the moment it is switched on, and says so plainly when
 * that grant has since been revoked.
 */
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
 * GPS ephemeris: a few days of predicted satellite orbits, which is what turns
 * a two-minute cold fix into a two-second one.
 *
 * Garmin's own app downloads these silently; this app has no INTERNET
 * permission and will not grow one for it, so the file comes in by the user's
 * own hand and is handed over when the watch asks. The URL the watch asked for
 * is shown because it is the only way to know WHICH file this particular
 * chipset wants.
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

/**
 * The way into the watch's OWN settings, at the root of its tree. Hidden
 * until 7f lands ([WatchSettingsTreeAvailable]).
 */
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

/**
 * Mirrors the phone's notifications to the watch. The master switch, its two
 * gates and the per-app blocklist all live on the notifications screen (7e
 * folded them into one), so this row only identifies and opens.
 */
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

/**
 * The live-sensor (BLE broadcast) controls for a bike computer: its currently
 * assigned capabilities, and a button to (re)detect what the device is
 * broadcasting right now.
 */
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
