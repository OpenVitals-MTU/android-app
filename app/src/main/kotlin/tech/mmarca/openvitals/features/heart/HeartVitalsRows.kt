package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.SourceChip
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                    text = "${unitFormatter.respiratoryRate(summary.average).text} avg",
                    style = MaterialTheme.typography.titleSmall,
                    color = respiratoryColor,
                )
                Text(
                    text = "${unitFormatter.respiratoryRate(summary.min).text}-${unitFormatter.respiratoryRate(summary.max).text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${unitFormatter.count(summary.readings)} readings",
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
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            entries.sortedByDescending(time).take(8).forEach { entry ->
                VitalsReadingRow(
                    label = value(entry),
                    source = source(entry),
                    time = time(entry).atZone(ZoneId.systemDefault()),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
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
    }
}

@Composable
private fun SummaryMetricCard(metric: SummaryMetric?, modifier: Modifier = Modifier) {
    if (metric == null) {
        MetricCardPlaceholder(
            title = "No data",
            icon = Icons.Outlined.Favorite,
            accentColor = MaterialTheme.colorScheme.outline,
            message = "No readings in this period.",
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
