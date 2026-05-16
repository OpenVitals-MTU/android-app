package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.ZoneId
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
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
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
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
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

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
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
