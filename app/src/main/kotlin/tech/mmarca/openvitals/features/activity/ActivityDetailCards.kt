package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseLapData
import tech.mmarca.openvitals.data.model.ExerciseRouteData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.model.ExerciseRouteStatus
import tech.mmarca.openvitals.data.model.ExerciseSegmentData
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
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val end = workout.endTime.atZone(zone)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = exerciseTypeIcon(workout.exerciseType),
                            contentDescription = null,
                            tint = WorkoutColor,
                        )
                        Text(
                            text = workout.title ?: exerciseTypeLabel(workout.exerciseType),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        text = exerciseTypeLabel(workout.exerciseType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SourceChip(source = workout.source)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.headlineMedium,
                color = WorkoutColor,
            )
            Text(
                text = "${formatDateTime(start, dateTimeFormatterProvider)} - ${formatDateTime(end, dateTimeFormatterProvider)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val notAvailable = stringResource(R.string.not_available)
    DetailSectionCard(title = stringResource(R.string.detail_metrics), modifier = modifier) {
        DetailRow(stringResource(R.string.detail_duration), unitFormatter.duration(workout.durationMs))
        DetailRow(stringResource(R.string.metric_steps), workout.steps?.let { unitFormatter.count(it) } ?: notAvailable)
        DetailRow(stringResource(R.string.metric_distance), workout.totalDistanceMeters?.let { unitFormatter.distance(it).text } ?: notAvailable)
        DetailRow(stringResource(R.string.metric_calories_burned), workout.totalCaloriesKcal?.let { unitFormatter.energy(it).text } ?: notAvailable)
        DetailRow(stringResource(R.string.metric_active_calories), workout.activeCaloriesKcal?.let { unitFormatter.energy(it).text } ?: notAvailable)
        DetailRow(stringResource(R.string.metric_floors_climbed), workout.floorsClimbed?.let { unitFormatter.count(it) } ?: notAvailable)
        DetailRow(stringResource(R.string.metric_elevation_gained), workout.elevationGainedMeters?.let { unitFormatter.elevation(it).text } ?: notAvailable)
    }
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
                    RoutePreview(
                        points = route.points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                    if (onOpenRouteInMap != null) {
                        OutlinedButton(
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
                            OutlinedButton(
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
                            OutlinedButton(
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

@Composable
private fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
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
