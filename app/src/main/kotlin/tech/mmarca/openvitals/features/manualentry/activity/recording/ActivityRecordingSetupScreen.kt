package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.Instant
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import androidx.compose.foundation.layout.PaddingValues
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.features.workoutplans.isGuidedRunnable

@Composable
internal fun ActivityRecordingSetupScreen(
    state: ActivityEntryUiState,
    recordingState: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onSelectActivityType: (ActivityEntryType) -> Unit,
    onStartRecording: (Location?, Long, Boolean) -> Unit,
    onStartHeartRateRecoveryTest: (HeartRateRecoveryTestConfig) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onStartPlan: () -> Unit = {},
    onStartTodayPlan: (String) -> Unit = {},
    onRequestActivityRecognitionPermission: () -> Unit,
    onChooseSource: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedType = state.selectedActivityType
    var restSecondsText by rememberSaveable(selectedType.id) { mutableStateOf("") }
    var withoutGps by rememberSaveable(selectedType.id) { mutableStateOf(false) }
    var hrrTest by rememberSaveable(selectedType.id) { mutableStateOf(false) }
    var hrrWarmupMinutesText by rememberSaveable(selectedType.id) { mutableStateOf("3") }
    var hrrTargetBpmText by rememberSaveable(selectedType.id) { mutableStateOf("") }
    val recordingWithoutGps = selectedType.supportsGpsRoute && withoutGps
    val gpsFixState = rememberPreRecordingGpsFixState(
        enabled = selectedType.supportsGpsRoute &&
            !recordingWithoutGps &&
            state.canWrite &&
            !state.isCheckingPermission &&
            !state.isImportingRoute &&
            !state.isSavingEntry,
    )
    val sensorReadiness = rememberRecordingSensorReadiness(selectedType)
    val latestPreciseFix = gpsFixState.latestPreciseFix
    val baseEnabled = state.canWrite &&
        !state.isCheckingPermission &&
        !state.isImportingRoute &&
        !state.isSavingEntry
    val enabled = activityRecordingStartEnabled(
        baseEnabled = baseEnabled,
        recordingWithoutGps = recordingWithoutGps,
        supportsGpsRoute = selectedType.supportsGpsRoute,
        hasPrecisePermission = gpsFixState.hasPrecisePermission,
        hasPreciseFix = latestPreciseFix != null,
        hasRequiredSensor = sensorReadiness.hasRequiredSensor,
    )

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActivityEntryHeader(
                state = state,
                onRequestWritePermission = onRequestWritePermission,
            )

            Text(
                text = stringResource(R.string.activity_entry_recording_ready_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val guidedPlan = state.guidedPlan
            if (guidedPlan != null) {
                ActivityPlanRunSetupCard(guidedPlan = guidedPlan, recordingState = recordingState)
            } else {
                // A plan due today is very likely what the Start was for.
                val todayPlan = state.hubPlans.firstOrNull { plan ->
                    plan.isGuidedRunnable() &&
                        !plan.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate().isAfter(java.time.LocalDate.now())
                }
                if (todayPlan != null) {
                    TodayPlanShortcutCard(
                        plan = todayPlan,
                        enabled = baseEnabled,
                        onStart = { onStartTodayPlan(todayPlan.id) },
                    )
                }
            ActivityTypeSelector(
                types = state.activityTypes.filter { it.supportsLiveRecording },
                selectedType = state.selectedActivityType,
                onSelectActivityType = onSelectActivityType,
                errorText = state.validationErrorText(ActivityEntryField.ACTIVITY_TYPE),
            )

            RecordingGuidancePanel(
                activityType = selectedType,
                sensorReadiness = sensorReadiness,
            )

            if (selectedType.supportsGpsRoute) {
                RecordingWithoutGpsSwitch(
                    checked = withoutGps,
                    enabled = baseEnabled,
                    onCheckedChange = { withoutGps = it },
                )
                if (withoutGps) {
                    RecordingWithoutGpsWarning(
                        countsSteps = selectedType.supportsStepCounting,
                    )
                }
                // The fix status is about GPS, so it goes away with GPS.
                // Leaving "waiting for a fix" on screen under a recording that
                // will never use one would be telling the user to wait for
                // something that is not coming.
                if (!withoutGps) {
                    PreRecordingGpsFixStatus(
                        state = gpsFixState,
                    )
                }
                ActivityRecordingLiveSensorStats(
                    state = recordingState,
                    unitFormatter = unitFormatter,
                )
            } else if (selectedType.isRepetitionLike) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = restSecondsText,
                        onValueChange = { restSecondsText = it },
                        enabled = baseEnabled,
                        singleLine = true,
                        label = { Text(stringResource(R.string.activity_entry_recording_rest_seconds_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ActivityRecordingSensorStatusCard(deviceStatuses = recordingState.bleDeviceStatuses)
                }
            } else if (selectedType.recordingSensor == ActivityRecordingSensor.BLE) {
                ActivityRecordingSensorStatusCard(deviceStatuses = recordingState.bleDeviceStatuses)
                // The heart-rate-recovery test, offered only where it can
                // actually be measured: it needs a heart rate arriving live,
                // every second, right through the minutes after the effort —
                // which means a connected sensor. A watch cannot drive this;
                // Health Connect hands its data over long after the fact.
                val hasHeartRateSensor = recordingState.bleDeviceStatuses.any { status ->
                    status.status == BleConnectionStatus.CONNECTED &&
                        BleSensorCapability.HEART_RATE in status.capabilities
                }
                if (hasHeartRateSensor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.activity_recording_hrr_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.activity_recording_hrr_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hrrTest,
                            onCheckedChange = { hrrTest = it },
                            enabled = baseEnabled,
                        )
                    }
                    if (hrrTest) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = hrrWarmupMinutesText,
                                onValueChange = { hrrWarmupMinutesText = it },
                                enabled = baseEnabled,
                                singleLine = true,
                                label = { Text(stringResource(R.string.activity_recording_hrr_warmup_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = hrrTargetBpmText,
                                onValueChange = { hrrTargetBpmText = it },
                                enabled = baseEnabled,
                                singleLine = true,
                                label = { Text(stringResource(R.string.activity_recording_hrr_target_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = stringResource(R.string.activity_recording_hrr_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            }

            OpenVitalsButton(
                onClick = {
                    if (guidedPlan != null) {
                        onStartPlan()
                        return@OpenVitalsButton
                    }
                    when (
                        val action = activityRecordingStartAction(
                            supportsStepCounting = selectedType.supportsStepCounting,
                            hasActivityRecognitionPermission = sensorReadiness.hasActivityRecognitionPermission,
                            supportsGpsRoute = selectedType.supportsGpsRoute,
                            recordingWithoutGps = recordingWithoutGps,
                            hasPrecisePermission = gpsFixState.hasPrecisePermission,
                            hrrTest = hrrTest,
                            recordingSensor = selectedType.recordingSensor,
                            latestPreciseFix = latestPreciseFix,
                            restSecondsText = restSecondsText,
                            hrrWarmupMinutesText = hrrWarmupMinutesText,
                            hrrTargetBpmText = hrrTargetBpmText,
                        )
                    ) {
                        ActivityRecordingStartAction.RequestActivityRecognitionPermission ->
                            onRequestActivityRecognitionPermission()
                        ActivityRecordingStartAction.RequestLocationPermission ->
                            onRequestLocationPermission()
                        is ActivityRecordingStartAction.StartHeartRateRecoveryTest ->
                            onStartHeartRateRecoveryTest(action.config)
                        is ActivityRecordingStartAction.StartRecording ->
                            onStartRecording(action.initialFix, action.restSeconds, action.withoutGps)
                    }
                },
                enabled = if (guidedPlan != null) baseEnabled else enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.action_start),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            OpenVitalsOutlinedButton(
                onClick = onChooseSource,
                enabled = !state.isSavingEntry && !state.isImportingRoute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.activity_entry_choose_another_source))
            }

            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Whether Start can be pressed.
 *
 * Recording without GPS waits for nothing: there is no fix to acquire, no
 * location permission to ask for, and no sensor to require. A duration IS a
 * recording.
 */
internal fun activityRecordingStartEnabled(
    baseEnabled: Boolean,
    recordingWithoutGps: Boolean,
    supportsGpsRoute: Boolean,
    hasPrecisePermission: Boolean,
    hasPreciseFix: Boolean,
    hasRequiredSensor: Boolean,
): Boolean = baseEnabled && when {
    recordingWithoutGps -> true
    supportsGpsRoute -> !hasPrecisePermission || hasPreciseFix
    else -> hasRequiredSensor
}

/** What pressing Start does. */
internal sealed interface ActivityRecordingStartAction {
    data object RequestActivityRecognitionPermission : ActivityRecordingStartAction

    data object RequestLocationPermission : ActivityRecordingStartAction

    data class StartHeartRateRecoveryTest(
        val config: HeartRateRecoveryTestConfig,
    ) : ActivityRecordingStartAction

    data class StartRecording(
        val initialFix: Location?,
        val restSeconds: Long,
        val withoutGps: Boolean,
    ) : ActivityRecordingStartAction
}

internal fun activityRecordingStartAction(
    supportsStepCounting: Boolean,
    hasActivityRecognitionPermission: Boolean,
    supportsGpsRoute: Boolean,
    recordingWithoutGps: Boolean,
    hasPrecisePermission: Boolean,
    hrrTest: Boolean,
    recordingSensor: ActivityRecordingSensor,
    latestPreciseFix: Location?,
    restSecondsText: String,
    hrrWarmupMinutesText: String = "3",
    hrrTargetBpmText: String = "",
): ActivityRecordingStartAction = when {
    supportsStepCounting && !hasActivityRecognitionPermission ->
        ActivityRecordingStartAction.RequestActivityRecognitionPermission
    // Not asked for when the user has said they do not want GPS: demanding the
    // location permission for a recording that will never look at a location is
    // exactly the kind of thing that makes people distrust a health app.
    supportsGpsRoute && !recordingWithoutGps && !hasPrecisePermission ->
        ActivityRecordingStartAction.RequestLocationPermission
    hrrTest && recordingSensor == ActivityRecordingSensor.BLE -> {
        val warmupMinutes = hrrWarmupMinutesText.trim().toIntOrNull()
        val targetBpm = hrrTargetBpmText.trim().toIntOrNull()
        ActivityRecordingStartAction.StartHeartRateRecoveryTest(
            HeartRateRecoveryTestConfig(
                warmupSeconds = ((warmupMinutes ?: 3) * 60).coerceIn(0, 60 * 60),
                // A target is optional: the rider can always end the effort by
                // hand, and on a day when the legs are not there they will have to.
                targetHeartRateBpm = targetBpm?.takeIf { it > 0 },
            ),
        )
    }
    else -> ActivityRecordingStartAction.StartRecording(
        initialFix = if (supportsGpsRoute && !recordingWithoutGps) latestPreciseFix else null,
        restSeconds = restSecondsText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
        withoutGps = recordingWithoutGps,
    )
}

internal data class RecordingSensorReadiness(
    val hasRequiredSensor: Boolean,
    val hasActivityRecognitionPermission: Boolean,
)

@Composable
internal fun rememberRecordingSensorReadiness(activityType: ActivityEntryType): RecordingSensorReadiness {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val hasSensor = when (activityType.recordingSensor) {
        ActivityRecordingSensor.PROXIMITY -> sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
        ActivityRecordingSensor.ACCELEROMETER -> sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        ActivityRecordingSensor.STEP_DETECTOR -> sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        ActivityRecordingSensor.BLE -> true
        ActivityRecordingSensor.GPS,
        ActivityRecordingSensor.NONE -> if (activityType.supportsStepCounting) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        } else {
            true
        }
    }
    return RecordingSensorReadiness(
        hasRequiredSensor = hasSensor,
        hasActivityRecognitionPermission = ActivityRecordingController.hasActivityRecognitionPermission(context),
    )
}

@Composable
internal fun RecordingGuidancePanel(
    activityType: ActivityEntryType,
    sensorReadiness: RecordingSensorReadiness,
    modifier: Modifier = Modifier,
) {
    if (!activityType.isRepetitionLike) return

    val guidanceRes = when (activityType.id) {
        "push_ups" -> R.string.activity_recording_guidance_push_ups
        "squats" -> R.string.activity_recording_guidance_squats
        "pull_ups" -> R.string.activity_recording_guidance_pull_ups
        "rope_skipping" -> R.string.activity_recording_guidance_rope_skipping
        "trampoline_jumping" -> R.string.activity_recording_guidance_trampoline_jumping
        "treadmill" -> R.string.activity_recording_guidance_treadmill
        else -> null
    } ?: return
    val statusText = when {
        !sensorReadiness.hasRequiredSensor -> stringResource(R.string.activity_recording_sensor_unavailable_manual)
        activityType.supportsStepCounting &&
            !sensorReadiness.hasActivityRecognitionPermission -> {
            stringResource(R.string.activity_recording_activity_recognition_missing)
        }
        else -> stringResource(R.string.activity_recording_sensor_ready)
    }
    val statusColor = when {
        !sensorReadiness.hasRequiredSensor -> MaterialTheme.colorScheme.error
        activityType.supportsStepCounting &&
            !sensorReadiness.hasActivityRecognitionPermission -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.activity_recording_how_it_works),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(guidanceRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
        }
    }
}

internal data class PreRecordingGpsFixState(
    val hasPrecisePermission: Boolean,
    val gpsProviderEnabled: Boolean,
    val latestLocation: Location?,
    val fixQuality: ActivityGpsFixQuality?,
) {
    val latestPreciseFix: Location?
        get() = latestLocation?.takeIf {
            hasPrecisePermission && gpsProviderEnabled && fixQuality?.isPrecise == true
        }
}

@SuppressLint("MissingPermission")
@Composable
internal fun rememberPreRecordingGpsFixState(enabled: Boolean): PreRecordingGpsFixState {
    val context = LocalContext.current
    val hasPrecisePermission = hasActivityRecordingPreciseLocationPermission(context)
    var latestLocation by remember { mutableStateOf<Location?>(null) }
    var gpsProviderEnabled by remember {
        mutableStateOf(context.isGpsProviderEnabled())
    }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(context, enabled, hasPrecisePermission) {
        while (enabled) {
            now = Instant.now()
            if (hasPrecisePermission) {
                gpsProviderEnabled = context.isGpsProviderEnabled()
            }
            delay(1_000L)
        }
    }

    DisposableEffect(context, enabled, hasPrecisePermission, gpsProviderEnabled) {
        if (!enabled || !hasPrecisePermission || !gpsProviderEnabled) {
            onDispose { }
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    latestLocation = Location(location)
                }

                override fun onProviderDisabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        gpsProviderEnabled = false
                    }
                }

                override fun onProviderEnabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        gpsProviderEnabled = true
                    }
                }
            }

            runCatching {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    PreRecordingGpsIntervalMillis,
                    PreRecordingGpsDistanceMeters,
                    listener,
                    Looper.getMainLooper(),
                )
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?.let { latestLocation = Location(it) }
            }.onFailure {
                gpsProviderEnabled = false
            }

            onDispose {
                runCatching {
                    locationManager.removeUpdates(listener)
                }
            }
        }
    }

    val fixQuality = latestLocation?.activityGpsFixQuality(now = now)
    return PreRecordingGpsFixState(
        hasPrecisePermission = hasPrecisePermission,
        gpsProviderEnabled = gpsProviderEnabled,
        latestLocation = latestLocation,
        fixQuality = fixQuality,
    )
}

@Composable
internal fun PreRecordingGpsFixStatus(
    state: PreRecordingGpsFixState,
    modifier: Modifier = Modifier,
) {
    val isReady = state.latestPreciseFix != null
    val statusColor = if (isReady) {
        WorkoutColor
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusDescription = stringResource(
        if (isReady) {
            R.string.activity_entry_recording_gps_fix
        } else {
            R.string.activity_entry_recording_gps_waiting
        },
    )

    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = statusDescription,
            tint = statusColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

internal fun activityRecordingRuntimePermissions(): Array<String> =
    buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

internal fun activityRecordingLocationPermissions(): Array<String> =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

internal fun hasActivityRecordingPreciseLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

internal fun needsActivityRecordingRuntimePermission(context: Context): Boolean =
    !hasActivityRecordingNotificationPermission(context)

internal fun hasActivityRecordingNotificationPermission(context: Context): Boolean =
    ActivityRecordingController.hasNotificationPermission(context)

internal fun Context.isGpsProviderEnabled(): Boolean =
    runCatching {
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            .isProviderEnabled(LocationManager.GPS_PROVIDER)
    }.getOrDefault(false)

internal const val PreRecordingGpsIntervalMillis = 1_000L
internal const val PreRecordingGpsDistanceMeters = 0f

@Composable
private fun RecordingWithoutGpsSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.activity_recording_without_gps_title),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.activity_recording_without_gps_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/**
 * Says what a GPS-less recording will cost, before it costs it. The barometer
 * and the step detector never needed a position, so they keep running; what is
 * lost is only what is genuinely derived from location.
 */
@Composable
internal fun RecordingWithoutGpsWarning(
    countsSteps: Boolean,
    modifier: Modifier = Modifier,
) {
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.activity_recording_without_gps_warning_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.activity_recording_without_gps_warning_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        if (countsSteps) {
                            R.string.activity_recording_without_gps_warning_kept_steps
                        } else {
                            R.string.activity_recording_without_gps_warning_kept
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** The plan about to run: what it is, how long, and every step in order. */
@Composable
internal fun ActivityPlanRunSetupCard(
    guidedPlan: ActivityGuidedPlan,
    recordingState: ActivityRecordingState,
    modifier: Modifier = Modifier,
) {
    val plannedMinutes = java.time.Duration.ofMillis(guidedPlan.plan.durationMs).toMinutes().coerceAtLeast(1L)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(
                R.string.activity_recording_plan_setup_title,
                guidedPlan.plan.title ?: stringResource(guidedPlan.activityType.labelRes),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.activity_recording_plan_setup_summary, guidedPlan.steps.size, plannedMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.activity_recording_plan_setup_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OpenVitalsSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                guidedPlan.steps.forEachIndexed { index, step ->
                    val goal = when (step.goalKind) {
                        ActivityPlanGoalKind.REPS -> stringResource(R.string.activity_entry_plan_preview_reps, step.goalValue.toInt())
                        ActivityPlanGoalKind.SECONDS -> stringResource(R.string.workout_plan_preview_seconds, step.goalValue)
                    }
                    val rest = step.restSeconds.takeIf { it > 0L }?.let {
                        stringResource(R.string.activity_recording_plan_setup_rest, it)
                    }
                    Text(
                        text = "${index + 1}. ${step.displayLabel()} · $goal" + (rest?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        guidedPlan.steps.mapNotNull { activityEntryTypeById(it.sensorTypeId) }.distinctBy { it.id }.forEach { type ->
            RecordingGuidancePanel(activityType = type, sensorReadiness = rememberRecordingSensorReadiness(type))
        }
        ActivityRecordingSensorStatusCard(deviceStatuses = recordingState.bleDeviceStatuses)
    }
}

@Composable
private fun TodayPlanShortcutCard(
    plan: tech.mmarca.openvitals.domain.model.PlannedExerciseData,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.activity_recording_today_plan_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = plan.title ?: stringResource(R.string.activity_entry_linked_plan_untitled),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            OpenVitalsButton(onClick = onStart, enabled = enabled) {
                Text(stringResource(R.string.activity_entry_hub_start_plan))
            }
        }
    }
}
