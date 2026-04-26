package tech.mmarca.openvitals.features.sleep

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
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
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SleepDetailScreen(
    viewModel: SleepDetailViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()
    val error = state.error
    val session = state.session

    when {
        state.isLoading -> FullScreenLoading()
        error != null -> ErrorMessage(message = error)
        session != null -> SleepDetailContent(
            session = session,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    }
}

@Composable
private fun SleepDetailContent(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            SleepSummaryCard(
                session = session,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            SleepStageBreakdownCard(
                session = session,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            SleepSessionDetailsCard(
                session = session,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (session.stages.isNotEmpty()) {
            item {
                DetailSectionCard(
                    title = "Stage events",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${unitFormatter.count(session.stages.size)} recorded stages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(session.stages.sortedBy { it.startTime }) { stage ->
                SleepStageEventRow(
                    stage = stage,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SleepSummaryCard(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)

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
                            imageVector = Icons.Outlined.Bed,
                            contentDescription = null,
                            tint = SleepColor,
                        )
                        Text(
                            text = session.title?.takeIf { it.isNotBlank() } ?: "Sleep session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        text = dateTimeFormatterProvider.mediumDate().format(end),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SourceChip(source = session.source)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = unitFormatter.duration(session.durationMs),
                style = MaterialTheme.typography.headlineMedium,
                color = SleepColor,
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
private fun SleepStageBreakdownCard(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = "Stages", modifier = modifier) {
        if (session.stages.isEmpty()) {
            Text(
                text = "No stages recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val orderedStages = session.stages.sortedBy { it.startTime }
            val start = orderedStages.first().startTime.atZone(ZoneId.systemDefault())
            val end = orderedStages.last().endTime.atZone(ZoneId.systemDefault())
            SleepStagesBar(
                stages = orderedStages,
                totalMs = session.durationMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = dateTimeFormatterProvider.shortTime().format(start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = dateTimeFormatterProvider.shortTime().format(end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            SleepStageLegend(stages = orderedStages, unitFormatter = unitFormatter)
            Spacer(Modifier.height(8.dp))
            stageTotals(orderedStages).forEach { (stageType, durationMs) ->
                val percent = if (session.durationMs > 0) {
                    durationMs * 100.0 / session.durationMs
                } else {
                    0.0
                }
                DetailRow(
                    label = SleepStage.stageLabel(stageType),
                    value = "${unitFormatter.duration(durationMs)} · ${unitFormatter.decimal(percent, 0)}%",
                )
            }
        }
    }
}

@Composable
private fun SleepSessionDetailsCard(
    session: SleepData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)
    val device = session.device

    DetailSectionCard(title = "Session details", modifier = modifier) {
        DetailRow("Started", formatDateTime(start, dateTimeFormatterProvider))
        DetailRow("Ended", formatDateTime(end, dateTimeFormatterProvider))
        DetailRow("Start zone", session.startZoneOffset?.id ?: "Not available")
        DetailRow("End zone", session.endZoneOffset?.id ?: "Not available")
        DetailRow("Recording", recordingMethodLabel(session.recordingMethod))
        DetailRow("Source package", session.source)
        DetailRow("Device type", deviceTypeLabel(device?.type))
        DetailRow("Device maker", device?.manufacturer ?: "Not available")
        DetailRow("Device model", device?.model ?: "Not available")
        DetailRow("Last modified", session.lastModifiedTime?.atZone(zone)?.let {
            formatDateTime(it, dateTimeFormatterProvider)
        } ?: "Not available")
        DetailRow("Record id", session.id)
        DetailRow("Client record id", session.clientRecordId ?: "Not available")
        DetailRow("Client version", session.clientRecordVersion?.toString() ?: "Not available")
        DetailRow("Title", session.title?.takeIf { it.isNotBlank() } ?: "Not available")
        DetailRow("Notes", session.notes?.takeIf { it.isNotBlank() } ?: "Not available")
    }
}

@Composable
private fun SleepStageEventRow(
    stage: SleepStage,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = stage.startTime.atZone(zone)
    val end = stage.endTime.atZone(zone)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = SleepStage.stageLabel(stage.stageType),
                style = MaterialTheme.typography.titleSmall,
            )
            DetailRow("Time", formatTimeRange(start, end, dateTimeFormatterProvider))
            DetailRow("Duration", unitFormatter.duration(stage.durationMs))
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

private fun stageTotals(stages: List<SleepStage>): List<Pair<Int, Long>> =
    stages
        .groupBy { it.stageType }
        .mapValues { (_, stageList) -> stageList.sumOf { it.durationMs } }
        .toList()
        .sortedByDescending { it.second }

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

private fun recordingMethodLabel(method: Int?): String = when (method) {
    Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> "Actively recorded"
    Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> "Automatically recorded"
    Metadata.RECORDING_METHOD_MANUAL_ENTRY -> "Manual entry"
    Metadata.RECORDING_METHOD_UNKNOWN -> "Unknown"
    else -> "Not available"
}

private fun deviceTypeLabel(type: Int?): String = when (type) {
    Device.TYPE_WATCH -> "Watch"
    Device.TYPE_PHONE -> "Phone"
    Device.TYPE_SCALE -> "Scale"
    Device.TYPE_RING -> "Ring"
    Device.TYPE_HEAD_MOUNTED -> "Head-mounted"
    Device.TYPE_FITNESS_BAND -> "Fitness band"
    Device.TYPE_CHEST_STRAP -> "Chest strap"
    Device.TYPE_SMART_DISPLAY -> "Smart display"
    Device.TYPE_UNKNOWN -> "Unknown"
    else -> "Not available"
}
