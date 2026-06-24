package tech.mmarca.openvitals.features.manualentry.activity.recording

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.Instant
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun ActivityRecordingSetupScreen(
    state: ActivityEntryUiState,
    unitFormatter: UnitFormatter,
    onSelectActivityType: (ActivityEntryType) -> Unit,
    onStartRecording: (Location?, Long) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestActivityRecognitionPermission: () -> Unit,
    onChooseSource: () -> Unit,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedType = state.selectedActivityType
    var restSecondsText by rememberSaveable(selectedType.id) { mutableStateOf("") }
    val gpsFixState = rememberPreRecordingGpsFixState(
        enabled = selectedType.supportsGpsRoute &&
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
    val enabled = baseEnabled && if (selectedType.supportsGpsRoute) {
        !gpsFixState.hasPrecisePermission || latestPreciseFix != null
    } else {
        sensorReadiness.hasRequiredSensor
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                PreRecordingGpsFixStatus(
                    state = gpsFixState,
                    unitFormatter = unitFormatter,
                )
            } else if (selectedType.isRepetitionLike) {
                OutlinedTextField(
                    value = restSecondsText,
                    onValueChange = { restSecondsText = it },
                    enabled = baseEnabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.activity_entry_recording_rest_seconds_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    if (selectedType.supportsStepCounting &&
                        !sensorReadiness.hasActivityRecognitionPermission
                    ) {
                        onRequestActivityRecognitionPermission()
                    } else if (selectedType.supportsGpsRoute && !gpsFixState.hasPrecisePermission) {
                        onRequestLocationPermission()
                    } else {
                        onStartRecording(
                            if (selectedType.supportsGpsRoute) latestPreciseFix else null,
                            restSecondsText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                        )
                    }
                },
                enabled = enabled,
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

            OutlinedButton(
                onClick = onChooseSource,
                enabled = !state.isSavingEntry && !state.isImportingRoute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.activity_entry_choose_another_source))
            }

            state.entryError?.let { error ->
                Text(
                    text = activityEntryErrorText(error, state.detailMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
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

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val fixQuality = state.fixQuality
    val accuracyText = fixQuality?.accuracyMeters?.let { unitFormatter.elevation(it).text }
    val statusText = when {
        !state.hasPrecisePermission -> stringResource(R.string.activity_entry_location_permission_needed)
        !state.gpsProviderEnabled -> stringResource(R.string.activity_entry_recording_gps_disabled)
        fixQuality?.isPrecise == true && accuracyText != null -> stringResource(
            R.string.activity_entry_recording_gps_ready,
            accuracyText,
        )
        accuracyText != null -> stringResource(
            R.string.activity_entry_recording_gps_waiting_accuracy,
            accuracyText,
        )
        else -> stringResource(R.string.activity_entry_recording_gps_waiting)
    }
    val statusColor = if (state.latestPreciseFix != null) {
        WorkoutColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                modifier = Modifier.weight(1f),
            )
        }
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
