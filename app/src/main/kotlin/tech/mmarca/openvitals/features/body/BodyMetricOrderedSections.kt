package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.Instant
import java.time.ZoneId

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
                        selectedDate = chartDaySelection.selectedDate,
                        onDateSelected = chartDaySelection.onDateSelected,
                        valueFormatter = { metricData.valueDisplayFormatter(it).text },
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
