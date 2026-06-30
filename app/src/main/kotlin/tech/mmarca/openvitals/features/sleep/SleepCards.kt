package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.ui.components.DetailRow
import tech.mmarca.openvitals.ui.components.DetailSectionCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepSessionTimelineCard(
    session: SleepData,
    selectedDate: LocalDate,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    timeRangeText: String? = null,
    preserveTimelineGaps: Boolean = false,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    if (onClick != null) {
        OpenVitalsCard(
            onClick = onClick,
            modifier = modifier,

        ) {
            SleepSessionTimelineCardContent(
                session = session,
                selectedDate = selectedDate,
                unitFormatter = unitFormatter,
                dateFormatter = dateFormatter,
                startText = timeFormatter.format(start),
                endText = timeFormatter.format(end),
                timeRangeText = timeRangeText,
                showDetails = true,
                preserveTimelineGaps = preserveTimelineGaps,
            )
        }
    } else {
        OpenVitalsCard(
            modifier = modifier,

        ) {
            SleepSessionTimelineCardContent(
                session = session,
                selectedDate = selectedDate,
                unitFormatter = unitFormatter,
                dateFormatter = dateFormatter,
                startText = timeFormatter.format(start),
                endText = timeFormatter.format(end),
                timeRangeText = timeRangeText,
                showDetails = false,
                preserveTimelineGaps = preserveTimelineGaps,
            )
        }
    }
}

@Composable
private fun SleepSessionTimelineCardContent(
    session: SleepData,
    selectedDate: LocalDate,
    unitFormatter: UnitFormatter,
    dateFormatter: DateTimeFormatter,
    startText: String,
    endText: String,
    timeRangeText: String?,
    showDetails: Boolean,
    preserveTimelineGaps: Boolean,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unitFormatter.duration(session.durationMs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SleepColor,
                )
                Text(
                    text = if (selectedDate == LocalDate.now()) {
                        stringResource(R.string.summary_sleep_ending_today)
                    } else {
                        stringResource(R.string.summary_sleep_ending_on, dateFormatter.format(selectedDate))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SourceChip(source = session.source)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = timeRangeText ?: singleSessionTimeRangeText(session, dateFormatter, startText, endText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (session.stages.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SleepStagesBar(
                stages = session.stages,
                totalMs = session.durationMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                timelineStart = session.startTime.takeIf { preserveTimelineGaps },
                timelineEnd = session.endTime.takeIf { preserveTimelineGaps },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = startText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = endText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            SleepStageLegend(stages = session.stages, unitFormatter = unitFormatter)
        }

        if (showDetails) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.action_details),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun singleSessionTimeRangeText(
    session: SleepData,
    dateFormatter: DateTimeFormatter,
    startText: String,
    endText: String,
): String =
    "${dateFormatter.format(session.startTime.atZone(ZoneId.systemDefault()))}  ·  $startText - $endText"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepSessionItem(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(
        onClick = onClick,
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(end),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${timeFormatter.format(start)} - ${timeFormatter.format(end)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = unitFormatter.duration(session.durationMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = SleepColor,
                    )
                    SourceChip(source = session.source)
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (session.stages.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SleepStagesBar(
                    stages = session.stages,
                    totalMs = session.durationMs,
                )
                Spacer(Modifier.height(8.dp))
                SleepStageLegend(stages = session.stages, unitFormatter = unitFormatter)
            }
        }
    }
}

@Composable
internal fun SleepSummaryCard(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)

    OpenVitalsCard(
        modifier = modifier,

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
                            text = session.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.detail_sleep_session),
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
internal fun SleepStageBreakdownCard(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    DetailSectionCard(title = stringResource(R.string.detail_stages), modifier = modifier) {
        if (session.stages.isEmpty()) {
            Text(
                text = stringResource(R.string.message_no_stages),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val orderedStages = session.stages.sortedBy { it.startTime }
            SleepStagesLaneChart(
                stages = orderedStages,
                unitFormatter = unitFormatter,
                timeFormatter = dateTimeFormatterProvider.shortTime(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                timelineStart = session.startTime,
                timelineEnd = session.endTime,
            )
            Spacer(Modifier.height(12.dp))
            val stageTotalMs = orderedStages.sumOf { it.durationMs.coerceAtLeast(0L) }
            stageTotals(orderedStages).forEach { (stageType, durationMs) ->
                val percent = if (stageTotalMs > 0) {
                    durationMs * 100.0 / stageTotalMs
                } else {
                    0.0
                }
                DetailRow(
                    label = sleepStageLabel(stageType),
                    value = "${unitFormatter.duration(durationMs)} · ${unitFormatter.decimal(percent, 0)}%",
                )
            }
        }
    }
}

@Composable
internal fun SleepSessionDetailsCard(
    session: SleepData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val end = session.endTime.atZone(zone)
    val device = session.device
    val notAvailable = stringResource(R.string.not_available)

    DetailSectionCard(title = stringResource(R.string.detail_session_details), modifier = modifier) {
        DetailRow(stringResource(R.string.detail_started), formatDateTime(start, dateTimeFormatterProvider))
        DetailRow(stringResource(R.string.detail_ended), formatDateTime(end, dateTimeFormatterProvider))
        DetailRow(stringResource(R.string.detail_start_zone), session.startZoneOffset?.id ?: notAvailable)
        DetailRow(stringResource(R.string.detail_end_zone), session.endZoneOffset?.id ?: notAvailable)
        DetailRow(stringResource(R.string.detail_recording), recordingMethodLabel(session.recordingMethod))
        DetailRow(stringResource(R.string.detail_source_package), session.source)
        DetailRow(stringResource(R.string.detail_device_type), deviceTypeLabel(device?.type))
        DetailRow(stringResource(R.string.detail_device_maker), device?.manufacturer ?: notAvailable)
        DetailRow(stringResource(R.string.detail_device_model), device?.model ?: notAvailable)
        DetailRow(stringResource(R.string.detail_last_modified), session.lastModifiedTime?.atZone(zone)?.let {
            formatDateTime(it, dateTimeFormatterProvider)
        } ?: notAvailable)
        DetailRow(stringResource(R.string.detail_record_id), session.id)
        DetailRow(stringResource(R.string.detail_client_record_id), session.clientRecordId ?: notAvailable)
        DetailRow(stringResource(R.string.detail_client_version), session.clientRecordVersion?.toString() ?: notAvailable)
        DetailRow(stringResource(R.string.detail_title), session.title?.takeIf { it.isNotBlank() } ?: notAvailable)
        DetailRow(stringResource(R.string.detail_notes), session.notes?.takeIf { it.isNotBlank() } ?: notAvailable)
    }
}

@Composable
internal fun SleepStageEventRow(
    stage: SleepStage,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = stage.startTime.atZone(zone)
    val end = stage.endTime.atZone(zone)

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = sleepStageLabel(stage.stageType),
                style = MaterialTheme.typography.titleSmall,
            )
            DetailRow(stringResource(R.string.detail_time), formatTimeRange(start, end, dateTimeFormatterProvider))
            DetailRow(stringResource(R.string.detail_duration), unitFormatter.duration(stage.durationMs))
        }
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

@Composable
private fun recordingMethodLabel(method: Int?): String = stringResource(
    when (method) {
        Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> R.string.recording_actively_recorded
        Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> R.string.recording_automatically_recorded
        Metadata.RECORDING_METHOD_MANUAL_ENTRY -> R.string.recording_manual_entry
        Metadata.RECORDING_METHOD_UNKNOWN -> R.string.recording_unknown
        else -> R.string.not_available
    }
)

@Composable
private fun deviceTypeLabel(type: Int?): String = stringResource(
    when (type) {
        Device.TYPE_WATCH -> R.string.device_watch
        Device.TYPE_PHONE -> R.string.device_phone
        Device.TYPE_SCALE -> R.string.device_scale
        Device.TYPE_RING -> R.string.device_ring
        Device.TYPE_HEAD_MOUNTED -> R.string.device_head_mounted
        Device.TYPE_FITNESS_BAND -> R.string.device_fitness_band
        Device.TYPE_CHEST_STRAP -> R.string.device_chest_strap
        Device.TYPE_SMART_DISPLAY -> R.string.device_smart_display
        Device.TYPE_UNKNOWN -> R.string.recording_unknown
        else -> R.string.not_available
    }
)
