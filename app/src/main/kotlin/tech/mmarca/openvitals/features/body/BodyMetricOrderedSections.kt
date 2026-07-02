package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.absoluteValue

internal fun LazyListScope.renderBodyOverviewOrderedContent(
    sectionContext: MetricDetailSectionContext,
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val metricsData = bodyMetricData(state, state.summary, unitFormatter)
    val trackedMetricsData = metricsData.filter { it.hasTrackedValues }

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.STATISTICS) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BodyOverviewStatisticsContent(metricsData)
                BmiContextCardsContent(
                    bmi = state.summary.bmi,
                    ffmi = state.summary.ffmi,
                    adjustedFfmi = state.summary.adjustedFfmi,
                    unitFormatter = unitFormatter,
                )
            }
        }
        section(MetricDetailSectionId.PERIOD_CHART, trackedMetricsData.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(stringResource(R.string.section_body_trends))
                trackedMetricsData.forEach { metricData ->
                    BodyOverviewMetricChart(
                        metricData = metricData,
                        state = state,
                        period = period,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                    )
                }
            }
        }
        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, chartDaySelection.selectedDate != null) {
            chartDaySelection.selectedDate?.let { selectedDate ->
                SelectedDateBodyEntriesContent(
                    state = state,
                    selectedDate = selectedDate,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditBodyMeasurement = onEditBodyMeasurement,
                    onDeleteBodyMeasurement = onDeleteBodyMeasurement,
                )
            }
        }
        section(MetricDetailSectionId.ENTRIES) {
            BodyAllReadingEntriesContent(
                state = state,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditBodyMeasurement = onEditBodyMeasurement,
                onDeleteBodyMeasurement = onDeleteBodyMeasurement,
            )
        }
    }
}

@Composable
private fun BodyOverviewMetricChart(
    metricData: BodyMetricData,
    state: BodyUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY) {
        BodyIntradayMetricChartCard(
            selectedDate = state.selectedDate,
            metricData = metricData,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
        )
    } else {
        MetricBarChart(
            title = stringResource(metricData.titleRes),
            values = metricData.values,
            selectedRange = state.selectedRange,
            period = period,
            accentColor = metricData.color,
            summaryValue = metricData.latest?.text
                ?: stringResource(R.string.summary_entries, metricData.values.size.toString()),
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { metricData.valueDisplayFormatter(it).text },
        )
    }
}

@Composable
private fun BodyIntradayMetricChartCard(
    selectedDate: LocalDate,
    metricData: BodyMetricData,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dayStart = selectedDate.atStartOfDay(zone).toInstant()
    val isToday = selectedDate == LocalDate.now()
    val chartEnd = if (isToday) Instant.now() else selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
    val elapsedToday = Duration.between(dayStart, chartEnd).toMillis().coerceAtLeast(1L)
    val points = metricData.dayValues.sortedBy { it.time }
    val latest = points.lastOrNull()?.value
    val minValue = points.minOfOrNull { it.value } ?: 0.0
    val maxValue = points.maxOfOrNull { it.value } ?: 1.0
    val padding = ((maxValue - minValue).takeIf { it > 0.0 } ?: maxValue.absoluteValue.coerceAtLeast(1.0)) * 0.08
    val axisMin = (minValue - padding).coerceAtLeast(0.0)
    val axisMax = maxValue + padding
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = latest?.let { metricData.valueDisplayFormatter(it).text } ?: stringResource(R.string.no_data),
                style = MaterialTheme.typography.headlineMedium,
                color = metricData.color,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.summary_today, stringResource(metricData.titleRes))
                } else {
                    stringResource(
                        R.string.summary_on_date,
                        stringResource(metricData.titleRes),
                        dateFormatter.format(selectedDate),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (points.isNotEmpty()) {
                MetricLinePlot(
                    points = points.map { point ->
                        val elapsed = Duration.between(dayStart, point.time)
                            .toMillis()
                            .coerceIn(0L, elapsedToday)
                        MetricLinePlotPoint(
                            xFraction = elapsed.toFloat() / elapsedToday.toFloat(),
                            value = point.value,
                        )
                    },
                    minValue = axisMin,
                    maxValue = axisMax,
                    accentColor = metricData.color,
                    chartHeight = 180.dp,
                    valueFormatter = { metricData.valueDisplayFormatter(it).text },
                    lineStrokeWidth = 3.dp,
                )
                Spacer(Modifier.height(8.dp))
                ChartXAxisWithYAxis {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf(
                            "00:00",
                            "06:00",
                            "12:00",
                            "18:00",
                            if (isToday) stringResource(R.string.summary_now) else "24:00",
                        ).forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.summary_last_update,
                        timeFormatter.format(points.last().time.atZone(zone)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = if (isToday) {
                        stringResource(R.string.summary_empty_today, stringResource(R.string.screen_body))
                    } else {
                        stringResource(R.string.summary_empty_day, stringResource(R.string.screen_body))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal data class BodyMetricOrderedContentSpec(
    val state: BodyUiState,
    val period: DatePeriod,
    val selectedDate: java.time.LocalDate?,
    val sectionContext: MetricDetailSectionContext,
    val summaryContent: (@Composable () -> Unit)? = null,
    val periodChart: (@Composable () -> Unit)? = null,
    val selectedDayEntries: (@Composable () -> Unit)? = null,
    val dataConfidence: (@Composable () -> Unit)? = null,
    val statistics: (@Composable () -> Unit)? = null,
    val entries: (@Composable () -> Unit)? = null,
)

internal fun LazyListScope.renderBodyMetricOrderedContent(
    spec: BodyMetricOrderedContentSpec,
) {
    renderOrderedMetricDetailSections(spec.sectionContext) {
        spec.summaryContent?.let { content ->
            section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
                content()
            }
        }
        spec.periodChart?.let { content ->
            section(MetricDetailSectionId.PERIOD_CHART) {
                content()
            }
        }
        spec.selectedDayEntries?.let { content ->
            section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, spec.selectedDate != null) {
                content()
            }
        }
        spec.dataConfidence?.let { content ->
            section(MetricDetailSectionId.DATA_CONFIDENCE, spec.period.start != spec.period.end) {
                content()
            }
        }
        spec.statistics?.let { content ->
            section(MetricDetailSectionId.STATISTICS) {
                content()
            }
        }
        spec.entries?.let { content ->
            section(MetricDetailSectionId.ENTRIES) {
                content()
            }
        }
    }
}

internal fun LazyListScope.renderBodyOverviewPlaceholder(
    sectionContext: MetricDetailSectionContext,
) {
    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.ACTIVITY_SUMMARY) {
            MetricCardPlaceholder(
                title = stringResource(R.string.screen_body),
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                message = stringResource(R.string.message_no_readings_period),
                modifier = metricModifier(),
            )
        }
    }
}

@Composable
internal fun BodyOverviewStatisticsContent(
    metricsData: List<BodyMetricData>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.section_statistics))
        InsightStatGrid(
            stats = metricsData.map { metricData ->
                val value = metricData.latest
                InsightStat(
                    title = stringResource(metricData.titleRes),
                    value = value?.value ?: stringResource(R.string.no_data),
                    unit = value?.unit.orEmpty(),
                    icon = metricData.icon,
                    accentColor = metricData.color,
                )
            },
            modifier = metricModifier(),
        )
    }
}

@Composable
internal fun <T> BodyEntryDataConfidenceContent(
    period: DatePeriod,
    entries: List<T>,
    source: (T) -> String,
    time: (T) -> Instant,
    accentColor: androidx.compose.ui.graphics.Color,
    valueKind: DataValueKind = DataValueKind.MEASURED,
) {
    if (period.start == period.end) return

    val zone = ZoneId.systemDefault()
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = entries.map { time(it).atZone(zone).toLocalDate() },
            sampleCount = entries.size,
            sources = entries.map(source),
            valueKind = valueKind,
        ),
        accentColor = accentColor,
        modifier = metricModifier(),
    )
}

@Composable
internal fun SelectedDateBodyEntriesContent(
    state: BodyUiState,
    selectedDate: java.time.LocalDate,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val entries = bodyReadingItems(
        state = state,
        unitFormatter = unitFormatter,
        weightLabel = stringResource(R.string.metric_weight),
        heightLabel = stringResource(R.string.metric_height),
        bodyFatLabel = stringResource(R.string.metric_body_fat),
        leanMassLabel = stringResource(R.string.metric_lean_mass),
        bmrLabel = stringResource(R.string.metric_bmr),
        boneMassLabel = stringResource(R.string.metric_bone_mass),
        bodyWaterMassLabel = stringResource(R.string.metric_body_water_mass),
        onEditBodyMeasurement = onEditBodyMeasurement,
        onDeleteBodyMeasurement = onDeleteBodyMeasurement,
    ).filter { it.time.atZone(zone).toLocalDate() == selectedDate }
        .sortedByDescending { it.time }
    PaginatedEntryList(
        title = entryListTitle(selectedDate, dateTimeFormatterProvider),
        entries = entries,
    ) { entry, rowModifier ->
        BodyReadingRow(
            value = entry.value,
            source = entry.source,
            time = entry.time,
            accentColor = entry.accentColor,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = entry.onEdit,
            onDelete = entry.onDelete,
            modifier = rowModifier,
        )
    }
}

@Composable
internal fun BodyAllReadingEntriesContent(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val entries = bodyReadingItems(
        state = state,
        unitFormatter = unitFormatter,
        weightLabel = stringResource(R.string.metric_weight),
        heightLabel = stringResource(R.string.metric_height),
        bodyFatLabel = stringResource(R.string.metric_body_fat),
        leanMassLabel = stringResource(R.string.metric_lean_mass),
        bmrLabel = stringResource(R.string.metric_bmr),
        boneMassLabel = stringResource(R.string.metric_bone_mass),
        bodyWaterMassLabel = stringResource(R.string.metric_body_water_mass),
        onEditBodyMeasurement = onEditBodyMeasurement,
        onDeleteBodyMeasurement = onDeleteBodyMeasurement,
    ).sortedByDescending { it.time }
    if (entries.isNotEmpty()) {
        PaginatedEntryList(
            title = stringResource(R.string.section_entries),
            entries = entries,
        ) { entry, rowModifier ->
            BodyReadingRow(
                value = entry.value,
                source = entry.source,
                time = entry.time,
                accentColor = entry.accentColor,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = entry.onEdit,
                onDelete = entry.onDelete,
                modifier = rowModifier,
            )
        }
    }
}
