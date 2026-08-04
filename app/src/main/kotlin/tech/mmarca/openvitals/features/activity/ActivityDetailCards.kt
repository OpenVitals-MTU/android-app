package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.movingDurationMs
import tech.mmarca.openvitals.domain.model.pausedDurationMs
import tech.mmarca.openvitals.features.activity.maps.OfflineRouteMapOrPreview
import tech.mmarca.openvitals.ui.components.AccentIconChip
import tech.mmarca.openvitals.ui.components.DetailRow
import tech.mmarca.openvitals.ui.components.DetailSectionCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.DataSourceEducationLink
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
internal fun WorkoutSummaryCard(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onManageDataSources: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val end = workout.endTime.atZone(zone)

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccentIconChip(
                        icon = exerciseTypeIcon(workout.exerciseType),
                        color = WorkoutColor,
                        size = 40.dp,
                        iconSize = 20.dp,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = exerciseTypeLabel(workout.exerciseType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SourceChip(
                    source = workout.source,
                    synced = SyncedSourceOverlay.isSyncedRecord(workout.clientRecordId),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = WorkoutColor,
            )
            Text(
                text = "${formatDateTime(start, dateTimeFormatterProvider)} - ${formatDateTime(end, dateTimeFormatterProvider)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DataSourceEducationLink(
                onManageDataSources = onManageDataSources,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
internal fun MetricsCard(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val movingDurationMs = workout.movingDurationMs()
    val exerciseType = workout.exerciseType
    DetailSectionCard(title = stringResource(R.string.detail_metrics), modifier = modifier) {
        MetricRow(
            label = stringResource(R.string.detail_duration),
            value = unitFormatter.duration(workout.durationMs),
            metric = ActivityDetailMetric.DURATION,
            exerciseType = exerciseType,
        )
        if (workout.pausedDurationMs() > 0L) {
            MetricRow(
                label = stringResource(R.string.detail_moving_time),
                value = unitFormatter.duration(movingDurationMs),
                metric = ActivityDetailMetric.MOVING_TIME,
                exerciseType = exerciseType,
            )
        }
        MetricRow(
            label = stringResource(R.string.metric_steps),
            value = workout.steps?.let { unitFormatter.count(it) },
            metric = ActivityDetailMetric.STEPS,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_distance),
            value = workout.totalDistanceMeters?.let { unitFormatter.distance(it).text },
            metric = ActivityDetailMetric.DISTANCE,
            exerciseType = exerciseType,
        )
        // Pace is not merely hidden when irrelevant — it is not even computed.
        // Any distance-and-duration session yields a number, but "13:07 min/km"
        // for a bike ride is speed wearing the wrong clothes.
        MetricRow(
            label = stringResource(R.string.metric_average_pace),
            value = if (isMetricRelevant(ActivityDetailMetric.AVERAGE_PACE, exerciseType)) {
                workout.averagePace(unitFormatter)?.text
            } else {
                null
            },
            metric = ActivityDetailMetric.AVERAGE_PACE,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_average_speed),
            value = workout.averageSpeed(unitFormatter)?.text,
            metric = ActivityDetailMetric.AVERAGE_SPEED,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_recorded_speed),
            value = workout.averageSpeedMetersPerSecond?.let { unitFormatter.speed(it).text },
            metric = ActivityDetailMetric.RECORDED_SPEED,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_average_heart_rate),
            value = workout.averageHeartRateBpm?.let { unitFormatter.heartRate(it).text },
            metric = ActivityDetailMetric.AVERAGE_HEART_RATE,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_average_power),
            value = workout.averagePowerWatts?.let { unitFormatter.power(it).text },
            metric = ActivityDetailMetric.AVERAGE_POWER,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_steps_cadence),
            value = workout.averageStepsCadenceRate?.let { unitFormatter.cadence(it).text },
            metric = ActivityDetailMetric.STEPS_CADENCE,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_cycling_cadence),
            value = workout.averageCyclingCadenceRpm?.let { unitFormatter.cadence(it).text },
            metric = ActivityDetailMetric.CYCLING_CADENCE,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_calories_burned),
            value = workout.totalCaloriesKcal?.let { calories ->
                val value = unitFormatter.energy(calories).text
                if (workout.totalCaloriesSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
                    stringResource(R.string.calories_estimated_value, value)
                } else {
                    value
                }
            },
            metric = ActivityDetailMetric.CALORIES_BURNED,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_active_calories),
            value = workout.activeCaloriesKcal?.let { unitFormatter.energy(it).text },
            metric = ActivityDetailMetric.ACTIVE_CALORIES,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_wheelchair_pushes),
            value = workout.wheelchairPushes?.let { unitFormatter.count(it) },
            metric = ActivityDetailMetric.WHEELCHAIR_PUSHES,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_floors_climbed),
            value = workout.floorsClimbed?.let { unitFormatter.count(it) },
            metric = ActivityDetailMetric.FLOORS_CLIMBED,
            exerciseType = exerciseType,
        )
        MetricRow(
            label = stringResource(R.string.metric_elevation_gained),
            value = workout.elevationGainedMeters?.let { unitFormatter.elevation(it).text },
            metric = ActivityDetailMetric.ELEVATION_GAINED,
            exerciseType = exerciseType,
        )
    }
}

/**
 * One metrics row, or nothing: shown whenever the metric HAS a value, and for a
 * null value only when that absence is informative for this kind of exercise —
 * see [isMetricRelevant].
 */
@Composable
private fun MetricRow(
    label: String,
    value: String?,
    metric: ActivityDetailMetric,
    exerciseType: Int,
) {
    if (!showsMetricRow(hasValue = value != null, metric = metric, exerciseType = exerciseType)) return
    DetailRow(label, value ?: stringResource(R.string.not_available))
}

@Composable
internal fun SessionDetailsCard(
    workout: ExerciseData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val end = workout.endTime.atZone(zone)
    val device = workout.device
    val notAvailable = stringResource(R.string.not_available)

    DetailSectionCard(title = stringResource(R.string.detail_session_details), modifier = modifier) {
        DetailRow(stringResource(R.string.detail_type), exerciseTypeLabel(workout.exerciseType))
        DetailRow(stringResource(R.string.detail_started), formatDateTime(start, dateTimeFormatterProvider))
        DetailRow(stringResource(R.string.detail_ended), formatDateTime(end, dateTimeFormatterProvider))
        DetailRow(stringResource(R.string.detail_start_zone), workout.startZoneOffset?.id ?: notAvailable)
        DetailRow(stringResource(R.string.detail_end_zone), workout.endZoneOffset?.id ?: notAvailable)
        DetailRow(stringResource(R.string.detail_recording), recordingMethodLabel(workout.recordingMethod))
        DetailRow(stringResource(R.string.detail_source_package), workout.source)
        DetailRow(stringResource(R.string.detail_device_type), deviceTypeLabel(device?.type))
        DetailRow(stringResource(R.string.detail_device_maker), device?.manufacturer ?: notAvailable)
        DetailRow(stringResource(R.string.detail_device_model), device?.model ?: notAvailable)
        DetailRow(stringResource(R.string.detail_last_modified), workout.lastModifiedTime?.atZone(zone)?.let {
            formatDateTime(it, dateTimeFormatterProvider)
        } ?: notAvailable)
        DetailRow(stringResource(R.string.detail_record_id), workout.id)
        DetailRow(stringResource(R.string.detail_client_record_id), workout.clientRecordId ?: notAvailable)
        DetailRow(stringResource(R.string.detail_client_version), workout.clientRecordVersion?.toString() ?: notAvailable)
        DetailRow(stringResource(R.string.detail_planned_session_id), workout.plannedExerciseSessionId ?: notAvailable)
        DetailRow(stringResource(R.string.detail_notes), workout.notes?.takeIf { it.isNotBlank() } ?: notAvailable)
    }
}

@Composable
internal fun SegmentsCard(
    segments: List<ExerciseSegmentData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = stringResource(R.string.detail_segments), modifier = modifier) {
        if (segments.isEmpty()) {
            Text(
                text = stringResource(R.string.message_no_segments),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            segments.sortedBy { it.startTime }.forEachIndexed { index, segment ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                SegmentBlock(
                    segment = segment,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }
        }
    }
}

@Composable
internal fun LapsCard(
    laps: List<ExerciseLapData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = stringResource(R.string.detail_laps), modifier = modifier) {
        if (laps.isEmpty()) {
            Text(
                text = stringResource(R.string.message_no_laps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            laps.sortedBy { it.startTime }.forEachIndexed { index, lap ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                LapBlock(
                    index = index,
                    lap = lap,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }
        }
    }
}

@Composable
internal fun RouteCard(
    route: ExerciseRouteData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenRouteInMap: (() -> Unit)? = null,
    onSaveRouteAsGpx: (() -> Unit)? = null,
    onSaveRouteAsKmz: (() -> Unit)? = null,
    onShareRouteAsGpx: (() -> Unit)? = null,
    onShareRouteAsKmz: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = stringResource(R.string.detail_route), modifier = modifier) {
        when (route.status) {
            ExerciseRouteStatus.DATA -> {
                if (route.points.isEmpty()) {
                    Text(
                        text = stringResource(R.string.message_no_route_points),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OfflineRouteMapOrPreview(
                        points = route.points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (onOpenRouteInMap != null) {
                        OpenVitalsOutlinedButton(
                            onClick = onOpenRouteInMap,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.activity_route_open_in_map),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    if (onSaveRouteAsGpx != null && onSaveRouteAsKmz != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OpenVitalsOutlinedButton(
                                onClick = onSaveRouteAsGpx,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.activity_route_export_gpx),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            OpenVitalsOutlinedButton(
                                onClick = onSaveRouteAsKmz,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.activity_route_export_kmz),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                    if (onShareRouteAsGpx != null && onShareRouteAsKmz != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OpenVitalsOutlinedButton(
                                onClick = onShareRouteAsGpx,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.activity_route_share_gpx),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            OpenVitalsOutlinedButton(
                                onClick = onShareRouteAsKmz,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.activity_route_share_kmz),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                    DetailRow(stringResource(R.string.detail_status), stringResource(R.string.detail_status_available))
                    DetailRow(stringResource(R.string.detail_points), route.points.size.toString())
                    route.points.minByOrNull { it.time }?.let { point ->
                        DetailRow(stringResource(R.string.detail_start_point), formatRoutePoint(point, unitFormatter, dateTimeFormatterProvider))
                    }
                    route.points.maxByOrNull { it.time }?.let { point ->
                        DetailRow(stringResource(R.string.detail_end_point), formatRoutePoint(point, unitFormatter, dateTimeFormatterProvider))
                    }
                }
            }
            ExerciseRouteStatus.CONSENT_REQUIRED -> {
                Text(
                    text = stringResource(R.string.message_route_consent_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExerciseRouteStatus.NO_DATA -> {
                Text(
                    text = stringResource(R.string.message_no_route_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SegmentBlock(
    segment: ExerciseSegmentData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val zone = ZoneId.systemDefault()
    val start = segment.startTime.atZone(zone)
    val end = segment.endTime.atZone(zone)

    Text(
        text = exerciseSegmentLabel(segment.segmentType),
        style = MaterialTheme.typography.titleSmall,
    )
    DetailRow(stringResource(R.string.detail_time), formatTimeRange(start, end, dateTimeFormatterProvider))
    segment.setIndex?.let { setIndex ->
        DetailRow(stringResource(R.string.detail_set), unitFormatter.count(setIndex + 1))
    }
    DetailRow(stringResource(R.string.detail_duration), unitFormatter.duration(segment.durationMs))
    DetailRow(
        stringResource(R.string.detail_repetitions),
        if (segment.repetitions > 0) unitFormatter.count(segment.repetitions) else stringResource(R.string.not_recorded),
    )
}

@Composable
private fun LapBlock(
    index: Int,
    lap: ExerciseLapData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val zone = ZoneId.systemDefault()
    val start = lap.startTime.atZone(zone)
    val end = lap.endTime.atZone(zone)

    Text(
        text = stringResource(R.string.detail_lap, index + 1),
        style = MaterialTheme.typography.titleSmall,
    )
    DetailRow(stringResource(R.string.detail_time), formatTimeRange(start, end, dateTimeFormatterProvider))
    DetailRow(stringResource(R.string.detail_duration), unitFormatter.duration(lap.durationMs))
    DetailRow(
        stringResource(R.string.detail_length),
        lap.lengthMeters?.let { unitFormatter.distance(it).text } ?: stringResource(R.string.not_recorded),
    )
}

private fun formatDateTime(
    value: ZonedDateTime,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String = dateTimeFormatterProvider.mediumDateTime().format(value)

private fun formatTimeRange(
    start: ZonedDateTime,
    end: ZonedDateTime,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    if (start.toLocalDate() == end.toLocalDate()) {
        "${dateTimeFormatterProvider.shortTime().format(start)} - ${dateTimeFormatterProvider.shortTime().format(end)}"
    } else {
        "${formatDateTime(start, dateTimeFormatterProvider)} - ${formatDateTime(end, dateTimeFormatterProvider)}"
    }

@Composable
private fun formatRoutePoint(
    point: ExerciseRoutePoint,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val zone = ZoneId.systemDefault()
    val parts = mutableListOf(
        String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude),
        formatDateTime(point.time.atZone(zone), dateTimeFormatterProvider),
    )
    if (point.altitudeMeters != null) {
        parts += stringResource(R.string.detail_altitude, unitFormatter.elevation(point.altitudeMeters).text)
    }
    if (point.horizontalAccuracyMeters != null) {
        parts += stringResource(
            R.string.detail_horizontal_accuracy,
            unitFormatter.elevation(point.horizontalAccuracyMeters).text,
        )
    }
    if (point.verticalAccuracyMeters != null) {
        parts += stringResource(
            R.string.detail_vertical_accuracy,
            unitFormatter.elevation(point.verticalAccuracyMeters).text,
        )
    }
    return parts.joinToString("\n")
}
