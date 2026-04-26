package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseLapData
import tech.mmarca.openvitals.data.model.ExerciseRouteData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.model.ExerciseRouteStatus
import tech.mmarca.openvitals.data.model.ExerciseSegmentData
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
fun ActivityDetailScreen(
    viewModel: ActivityDetailViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()

    val error = state.error
    val workout = state.workout

    when {
        state.isLoading -> FullScreenLoading()
        error != null -> ErrorMessage(message = error)
        workout != null -> ActivityDetailContent(
            workout = workout,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    }
}

@Composable
private fun ActivityDetailContent(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            WorkoutSummaryCard(
                workout = workout,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            MetricsCard(
                workout = workout,
                unitFormatter = unitFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            SessionDetailsCard(
                workout = workout,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            SegmentsCard(
                segments = workout.segments,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            LapsCard(
                laps = workout.laps,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            RouteCard(
                route = workout.route,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun WorkoutSummaryCard(
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
                            imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
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
private fun MetricsCard(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = "Metrics", modifier = modifier) {
        DetailRow("Duration", unitFormatter.duration(workout.durationMs))
        DetailRow("Steps", workout.steps?.let { unitFormatter.count(it) } ?: "Not available")
        DetailRow("Distance", workout.totalDistanceMeters?.let { unitFormatter.distance(it).text } ?: "Not available")
        DetailRow("Total calories", workout.totalCaloriesKcal?.let { unitFormatter.energy(it).text } ?: "Not available")
        DetailRow("Active calories", workout.activeCaloriesKcal?.let { unitFormatter.energy(it).text } ?: "Not available")
        DetailRow("Floors climbed", workout.floorsClimbed?.let { unitFormatter.count(it) } ?: "Not available")
        DetailRow("Elevation gained", workout.elevationGainedMeters?.let { unitFormatter.elevation(it).text } ?: "Not available")
    }
}

@Composable
private fun SessionDetailsCard(
    workout: ExerciseData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = workout.startTime.atZone(zone)
    val end = workout.endTime.atZone(zone)
    val device = workout.device

    DetailSectionCard(title = "Session details", modifier = modifier) {
        DetailRow("Type", exerciseTypeLabel(workout.exerciseType))
        DetailRow("Started", formatDateTime(start, dateTimeFormatterProvider))
        DetailRow("Ended", formatDateTime(end, dateTimeFormatterProvider))
        DetailRow("Start zone", workout.startZoneOffset?.id ?: "Not available")
        DetailRow("End zone", workout.endZoneOffset?.id ?: "Not available")
        DetailRow("Recording", recordingMethodLabel(workout.recordingMethod))
        DetailRow("Source package", workout.source)
        DetailRow("Device type", deviceTypeLabel(device?.type))
        DetailRow("Device maker", device?.manufacturer ?: "Not available")
        DetailRow("Device model", device?.model ?: "Not available")
        DetailRow("Last modified", workout.lastModifiedTime?.atZone(zone)?.let {
            formatDateTime(it, dateTimeFormatterProvider)
        } ?: "Not available")
        DetailRow("Record id", workout.id)
        DetailRow("Client record id", workout.clientRecordId ?: "Not available")
        DetailRow("Client version", workout.clientRecordVersion?.toString() ?: "Not available")
        DetailRow("Planned session id", workout.plannedExerciseSessionId ?: "Not available")
        DetailRow("Notes", workout.notes?.takeIf { it.isNotBlank() } ?: "Not available")
    }
}

@Composable
private fun SegmentsCard(
    segments: List<ExerciseSegmentData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = "Segments", modifier = modifier) {
        if (segments.isEmpty()) {
            Text(
                text = "No segments recorded.",
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
    DetailRow("Time", formatTimeRange(start, end, dateTimeFormatterProvider))
    DetailRow("Duration", unitFormatter.duration(segment.durationMs))
    DetailRow("Repetitions", if (segment.repetitions > 0) unitFormatter.count(segment.repetitions) else "Not recorded")
}

@Composable
private fun LapsCard(
    laps: List<ExerciseLapData>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = "Laps", modifier = modifier) {
        if (laps.isEmpty()) {
            Text(
                text = "No laps recorded.",
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
        text = "Lap ${index + 1}",
        style = MaterialTheme.typography.titleSmall,
    )
    DetailRow("Time", formatTimeRange(start, end, dateTimeFormatterProvider))
    DetailRow("Duration", unitFormatter.duration(lap.durationMs))
    DetailRow("Length", lap.lengthMeters?.let { unitFormatter.distance(it).text } ?: "Not recorded")
}

@Composable
private fun RouteCard(
    route: ExerciseRouteData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = "Route", modifier = modifier) {
        when (route.status) {
            ExerciseRouteStatus.DATA -> {
                DetailRow("Status", "Available")
                DetailRow("Points", route.points.size.toString())
                route.points.minByOrNull { it.time }?.let { point ->
                    DetailRow("Start point", formatRoutePoint(point, unitFormatter, dateTimeFormatterProvider))
                }
                route.points.maxByOrNull { it.time }?.let { point ->
                    DetailRow("End point", formatRoutePoint(point, unitFormatter, dateTimeFormatterProvider))
                }
            }
            ExerciseRouteStatus.CONSENT_REQUIRED -> {
                Text(
                    text = "Route data is available, but Health Connect requires separate route consent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExerciseRouteStatus.NO_DATA -> {
                Text(
                    text = "No route data recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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

private fun formatRoutePoint(
    point: ExerciseRoutePoint,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val zone = ZoneId.systemDefault()
    val parts = buildList {
        add(String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude))
        add(formatDateTime(point.time.atZone(zone), dateTimeFormatterProvider))
        point.altitudeMeters?.let { add("Altitude ${unitFormatter.elevation(it).text}") }
        point.horizontalAccuracyMeters?.let { add("Horizontal accuracy ${unitFormatter.elevation(it).text}") }
        point.verticalAccuracyMeters?.let { add("Vertical accuracy ${unitFormatter.elevation(it).text}") }
    }
    return parts.joinToString("\n")
}
