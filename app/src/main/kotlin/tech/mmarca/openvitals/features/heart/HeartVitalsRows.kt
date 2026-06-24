package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val VitalsEntryPageSize = 10

@Composable
internal fun VitalsSummaryRow(
    first: SummaryMetric?,
    second: SummaryMetric?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryMetricCard(first, Modifier.weight(1f))
        SummaryMetricCard(second, Modifier.weight(1f))
    }
}

@Composable
internal fun RespiratoryRateDayRow(
    summary: RespiratoryRateDaySummary,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val dayFormatter = dateTimeFormatterProvider.chartDay()
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayFormatter.format(summary.date),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.summary_value_avg, unitFormatter.respiratoryRate(summary.average).text),
                    style = MaterialTheme.typography.titleSmall,
                    color = respiratoryColor,
                )
                Text(
                    text = "${unitFormatter.respiratoryRate(summary.min).text}-${unitFormatter.respiratoryRate(summary.max).text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.summary_readings, unitFormatter.count(summary.readings)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun <T> SimpleVitalsList(
    title: String,
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    val sortedEntries = entries.sortedByDescending(time)
    var visibleCount by remember(sortedEntries) {
        mutableIntStateOf(sortedEntries.size.coerceAtMost(VitalsEntryPageSize))
    }
    val boundedVisibleCount = visibleCount.coerceAtMost(sortedEntries.size)

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            sortedEntries.take(boundedVisibleCount).forEach { entry ->
                VitalsReadingRow(
                    label = value(entry),
                    source = source(entry),
                    time = time(entry).atZone(ZoneId.systemDefault()),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEdit = onEdit
                        ?.takeIf { editable(entry) }
                        ?.let { edit -> { edit(entry) } },
                    onDelete = onDelete
                        ?.takeIf { editable(entry) }
                        ?.let { delete -> { delete(entry) } },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            if (boundedVisibleCount < sortedEntries.size) {
                OutlinedButton(
                    onClick = {
                        visibleCount = (boundedVisibleCount + VitalsEntryPageSize).coerceAtMost(sortedEntries.size)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.action_load_more_entries))
                }
            }
        }
    }
}

@Composable
internal fun VitalsReadingRow(
    label: String,
    source: String,
    time: ZonedDateTime,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
        ) {
            VitalsReadingRowContent(
                label = label,
                source = source,
                time = time,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit,
            )
        }
    } else {
        VitalsReadingRowContent(
            label = label,
            source = source,
            time = time,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun VitalsReadingRowContent(
    label: String,
    source: String,
    time: ZonedDateTime,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${dateTimeFormatterProvider.mediumDate().format(time)} · ${dateTimeFormatterProvider.shortTime().format(time)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        SourceChip(source = source)
        if (onEdit != null) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_entry),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(metric: SummaryMetric?, modifier: Modifier = Modifier) {
    if (metric == null) {
        MetricCardPlaceholder(
            title = stringResource(R.string.no_data),
            icon = Icons.Outlined.Favorite,
            accentColor = MaterialTheme.colorScheme.outline,
            message = stringResource(R.string.message_no_readings_period),
            modifier = modifier,
        )
    } else {
        MetricCard(
            title = metric.title,
            value = metric.value,
            unit = metric.unit,
            icon = metric.icon,
            accentColor = metric.color,
            source = metric.source,
            modifier = modifier,
        )
    }
}
