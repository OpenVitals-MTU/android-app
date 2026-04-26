package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PeriodBarAggregation
import tech.mmarca.openvitals.ui.components.PeriodChartXAxis
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.periodBarBuckets
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate
import java.time.ZoneId

private val oxygenColor = Color(0xFF00897B)
private val respiratoryColor = Color(0xFF5E97F6)
private val temperatureColor = Color(0xFFFF7043)
private val vo2Color = Color(0xFF7E57C2)

fun LazyListScope.HeartVitalsContent(
    state: HeartUiState,
    phase3Permissions: Set<String>,
    onGrantPermissions: (Set<String>) -> Unit,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.missingVitalsPermissions.isNotEmpty()) {
        item {
            PermissionCallout(
                title = "Vitals permissions needed",
                body = "Grant blood pressure, oxygen saturation, respiratory rate, temperature, and VO2 max permissions to fill this screen.",
                onGrant = {
                    onGrantPermissions(phase3Permissions)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (!state.hasVitalsData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = "Vitals",
                icon = Icons.Outlined.Favorite,
                accentColor = VitalsColor,
                message = "No vitals were recorded for this period.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

        if (state.bloodPressure.isNotEmpty() || state.spO2.isNotEmpty() || state.vo2Max.isNotEmpty()) {
            item { SectionHeader("Cardiovascular") }
            item {
                VitalsSummaryRow(
                    first = state.latestBloodPressure?.let {
                        val value = unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg)
                        SummaryMetric("Blood pressure", value.value, value.unit, Icons.Outlined.Favorite, VitalsColor, it.source)
                    },
                    second = state.latestSpO2?.let {
                        val value = unitFormatter.percent(it.percent)
                        SummaryMetric("SpO2", value.value, value.unit, Icons.Outlined.Favorite, oxygenColor, it.source)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.bloodPressure.isNotEmpty()) {
                item {
                    BloodPressureChart(
                        entries = state.bloodPressure,
                        selectedRange = selectedRange,
                        period = period,
                        unitFormatter = unitFormatter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.spO2.isNotEmpty()) {
                item {
                    VitalsLineChart(
                        title = "Oxygen saturation",
                        values = state.spO2.sortedBy { it.time }.map { it.percent },
                        dates = state.spO2.sortedBy { it.time }.map { it.time.atZone(ZoneId.systemDefault()).toLocalDate() },
                        selectedRange = selectedRange,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        accentColor = oxygenColor,
                        summary = "${periodTitle(selectedRange, period)} · ${unitFormatter.percent(state.spO2.map { it.percent }.average()).text} avg",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            state.latestVo2Max?.let { latest ->
                val vo2Max = unitFormatter.vo2Max(latest.vo2MaxMlPerKgPerMin)
                item {
                    MetricCard(
                        title = "VO2 max",
                        value = vo2Max.value,
                        unit = vo2Max.unit,
                        icon = Icons.Outlined.Speed,
                        accentColor = vo2Color,
                        source = latest.source,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (state.respiratoryRate.isNotEmpty() || state.bodyTemperature.isNotEmpty()) {
            item { SectionHeader("Respiratory") }
            item {
                VitalsSummaryRow(
                    first = respiratoryRateSummaryMetric(
                        entries = state.respiratoryRate,
                        selectedRange = selectedRange,
                        period = period,
                        unitFormatter = unitFormatter,
                    ),
                    second = state.latestBodyTemperature?.let {
                        val value = unitFormatter.temperature(it.temperatureCelsius)
                        SummaryMetric("Body temp", value.value, value.unit, Icons.Outlined.DeviceThermostat, temperatureColor, it.source)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.respiratoryRate.isNotEmpty()) {
                item {
                    if (selectedRange == TimeRange.DAY) {
                        SimpleVitalsList(
                            title = "Respiratory rate readings",
                            entries = state.respiratoryRate,
                            value = { unitFormatter.respiratoryRate(it.breathsPerMinute).text },
                            source = { it.source },
                            time = { it.time },
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else {
                        RespiratoryRateChart(
                            entries = state.respiratoryRate,
                            selectedRange = selectedRange,
                            period = period,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                if (selectedRange != TimeRange.DAY) {
                    item { SectionHeader("Respiratory rate daily breakdown") }
                    items(respiratoryRateDaySummaries(state.respiratoryRate).sortedByDescending { it.date }) { summary ->
                        RespiratoryRateDayRow(
                            summary = summary,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (state.bodyTemperature.isNotEmpty()) {
                item {
                    SimpleVitalsList(
                        title = "Body temperature readings",
                        entries = state.bodyTemperature,
                        value = { unitFormatter.temperature(it.temperatureCelsius).text },
                        source = { it.source },
                        time = { it.time },
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (state.vo2Max.size > 1) {
            item { SectionHeader("VO2 max history") }
            items(state.vo2Max.sortedByDescending { it.time }) { entry ->
                VitalsReadingRow(
                    label = unitFormatter.vo2Max(entry.vo2MaxMlPerKgPerMin).text,
                    source = entry.source,
                    time = entry.time.atZone(ZoneId.systemDefault()),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
}

private data class SummaryMetric(
    val title: String,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color,
    val source: String,
)

@Composable
private fun VitalsSummaryRow(
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

@Composable
private fun RespiratoryRateChart(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val buckets = respiratoryRateBuckets(entries, selectedRange, period)
    val plotted = buckets.mapIndexedNotNull { index, bucket ->
        bucket.value.takeIf { it > 0.0 }?.let { RespiratoryRatePlotPoint(index, it) }
    }
    val average = respiratoryRateAverage(buckets)
    val max = plotted.maxOfOrNull { it.value }?.plus(1.0) ?: 1.0
    val min = plotted.minOfOrNull { it.value }?.minus(1.0)?.coerceAtLeast(0.0) ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Respiratory rate", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (plotted.isEmpty()) return@Canvas

                val lastIndex = buckets.lastIndex.coerceAtLeast(1)
                val points = plotted.map { point ->
                    Offset(
                        x = point.index * size.width / lastIndex,
                        y = size.height * (1f - ((point.value - min) / range).toFloat()),
                    )
                }

                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = respiratoryColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point ->
                    drawCircle(color = respiratoryColor, radius = 4.dp.toPx(), center = point)
                }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = buckets.map { it.date },
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${periodTitle(selectedRange, period)} · ${unitFormatter.respiratoryRate(average).text} avg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class RespiratoryRatePlotPoint(
    val index: Int,
    val value: Double,
)

private fun respiratoryRateSummaryMetric(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
): SummaryMetric? {
    if (entries.isEmpty()) return null

    if (selectedRange == TimeRange.DAY) {
        val latest = entries.maxByOrNull { it.time } ?: return null
        val value = unitFormatter.respiratoryRate(latest.breathsPerMinute)
        return SummaryMetric("Respiratory rate", value.value, value.unit, Icons.Outlined.Air, respiratoryColor, latest.source)
    }

    val average = respiratoryRateAverage(respiratoryRateBuckets(entries, selectedRange, period))
    val value = unitFormatter.respiratoryRate(average)
    val source = entries.map { it.source }.distinct().singleOrNull() ?: "${entries.size} readings"
    return SummaryMetric("Avg respiratory rate", value.value, value.unit, Icons.Outlined.Air, respiratoryColor, source)
}

private fun respiratoryRateBuckets(
    entries: List<RespiratoryRateEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
) = periodBarBuckets(
    values = entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(
                date = date,
                value = dayEntries.map { it.breathsPerMinute }.average(),
            )
        },
    selectedRange = selectedRange,
    period = period,
    yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
)

private fun respiratoryRateAverage(
    buckets: List<tech.mmarca.openvitals.ui.components.PeriodChartBucket>,
): Double = buckets
    .map { it.value }
    .filter { it > 0.0 }
    .takeIf { it.isNotEmpty() }
    ?.average()
    ?: 0.0

private data class RespiratoryRateDaySummary(
    val date: LocalDate,
    val average: Double,
    val min: Double,
    val max: Double,
    val readings: Int,
)

private fun respiratoryRateDaySummaries(entries: List<RespiratoryRateEntry>): List<RespiratoryRateDaySummary> =
    entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            val values = dayEntries.map { it.breathsPerMinute }
            RespiratoryRateDaySummary(
                date = date,
                average = values.average(),
                min = values.minOrNull() ?: 0.0,
                max = values.maxOrNull() ?: 0.0,
                readings = values.size,
            )
        }

@Composable
private fun RespiratoryRateDayRow(
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
private fun BloodPressureChart(
    entries: List<BloodPressureEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val max = sorted.maxOfOrNull { it.systolicMmHg }?.coerceAtLeast(140) ?: 140
    val min = sorted.minOfOrNull { it.diastolicMmHg }?.coerceAtMost(60) ?: 60
    val range = (max - min).coerceAtLeast(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Blood pressure", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                if (sorted.isEmpty()) return@Canvas
                val stepX = if (sorted.size > 1) size.width / (sorted.size - 1) else size.width
                sorted.forEachIndexed { index, entry ->
                    val x = if (sorted.size > 1) index * stepX else size.width / 2f
                    val ySystolic = size.height * (1f - (entry.systolicMmHg - min).toFloat() / range)
                    val yDiastolic = size.height * (1f - (entry.diastolicMmHg - min).toFloat() / range)
                    drawLine(
                        color = VitalsColor.copy(alpha = 0.35f),
                        start = Offset(x, ySystolic),
                        end = Offset(x, yDiastolic),
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = VitalsColor, radius = 4.dp.toPx(), center = Offset(x, ySystolic))
                    drawCircle(color = HeartColor, radius = 4.dp.toPx(), center = Offset(x, yDiastolic))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${periodTitle(selectedRange, period)} · ${unitFormatter.count(sorted.size)} readings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VitalsLineChart(
    title: String,
    values: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val min = values.minOrNull()?.coerceAtMost(max - 1.0) ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (values.size < 2) return@Canvas
                val points = values.mapIndexed { index, value ->
                    Offset(
                        x = index * size.width / (values.size - 1),
                        y = size.height * (1f - ((value - min) / range).toFloat()),
                    )
                }
                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = accentColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point -> drawCircle(color = accentColor, radius = 4.dp.toPx(), center = point) }
            }
            Spacer(Modifier.height(8.dp))
            PeriodChartXAxis(
                dates = dates,
                selectedRange = selectedRange,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> SimpleVitalsList(
    title: String,
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
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
private fun VitalsReadingRow(
    label: String,
    source: String,
    time: java.time.ZonedDateTime,
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
