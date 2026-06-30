package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun LazyListScope.bodyContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val hasAnyBodyData = bodyMetricData(state, state.summary, unitFormatter).any { it.latest != null || it.values.isNotEmpty() } ||
        state.weightEntries.isNotEmpty() ||
        state.heightEntries.isNotEmpty() ||
        state.bodyFatEntries.isNotEmpty() ||
        state.leanMassEntries.isNotEmpty() ||
        state.bmrEntries.isNotEmpty() ||
        state.boneMassEntries.isNotEmpty() ||
        state.bodyWaterMassEntries.isNotEmpty()

    if (!hasAnyBodyData && !state.isLoading) {
        renderBodyOverviewPlaceholder(sectionContext)
        return
    }

    renderBodyOverviewOrderedContent(
        sectionContext = sectionContext,
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        chartDaySelection = chartDaySelection,
        onEditBodyMeasurement = onEditBodyMeasurement,
        onDeleteBodyMeasurement = onDeleteBodyMeasurement,
    )
}

internal fun LazyListScope.bodyOverviewStatistics(
    metricsData: List<BodyMetricData>,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
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

internal fun BodyUiState.metricDataFor(
    metric: BodyMetric,
    unitFormatter: UnitFormatter,
): BodyMetricData =
    bodyMetricData(this, summary, unitFormatter).first { it.metric == metric }

internal fun <T> LazyListScope.bodyMetricReadingEntries(
    entries: List<T>,
    selectedDate: LocalDate?,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> Instant,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    selectedDate?.let { date ->
        bodyReadingEntries(
            entries = entries.filter { time(it).atZone(ZoneId.systemDefault()).toLocalDate() == date },
            value = value,
            source = source,
            time = time,
            accentColor = accentColor,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            titleDate = date,
            editable = editable,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
    bodyReadingEntries(
        entries = entries,
        value = value,
        source = source,
        time = time,
        accentColor = accentColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        editable = editable,
        onEdit = onEdit,
        onDelete = onDelete,
    )
}

@Composable
internal fun <T> bodyMetricReadingEntriesContent(
    entries: List<T>,
    selectedDate: LocalDate?,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> Instant,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedOnly: Boolean,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    val zone = ZoneId.systemDefault()
    val listEntries = if (selectedOnly) {
        selectedDate?.let { date ->
            entries.filter { time(it).atZone(zone).toLocalDate() == date }
        }.orEmpty()
    } else {
        entries
    }
    if (listEntries.isEmpty()) return

    PaginatedEntryList(
        title = if (selectedOnly) {
            entryListTitle(selectedDate, dateTimeFormatterProvider)
        } else {
            stringResource(R.string.section_entries)
        },
        entries = listEntries.sortedByDescending(time),
    ) { entry, rowModifier ->
        BodyReadingRow(
            value = value(entry),
            source = source(entry),
            time = time(entry),
            accentColor = accentColor,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit?.takeIf { editable(entry) }?.let { edit -> { edit(entry) } },
            onDelete = onDelete?.takeIf { editable(entry) }?.let { delete -> { delete(entry) } },
            modifier = rowModifier,
        )
    }
}

internal fun LazyListScope.selectedDateBodyEntries(
    state: BodyUiState,
    selectedDate: LocalDate,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    item {
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
}

internal fun LazyListScope.bodyAllReadingEntries(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    item {
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
}

internal fun LazyListScope.bmiDataConfidence(
    state: BodyUiState,
    period: DatePeriod,
) {
    bodyEntryDataConfidence(
        period = period,
        entries = state.weightEntries,
        source = { it.source },
        time = { it.time },
        accentColor = WeightColor,
        valueKind = DataValueKind.CALCULATED,
    )
}

internal fun <T> LazyListScope.bodyEntryDataConfidence(
    period: DatePeriod,
    entries: List<T>,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
    valueKind: DataValueKind = DataValueKind.MEASURED,
) {
    if (period.start == period.end) return

    item {
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
}

internal fun LazyListScope.weightContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    if (state.weightEntries.isNotEmpty()) {
        val metricData = state.metricDataFor(BodyMetric.WEIGHT, unitFormatter)
        val values = state.weightEntries.map { it.weightKg }
        val latest = state.weightEntries.maxByOrNull { it.time }?.let { unitFormatter.weight(it.weightKg) }
            ?: unitFormatter.weight(0.0)
        val previousLatestKg = state.previousWeightEntries.maxByOrNull { it.time }?.weightKg
        renderBodyMetricOrderedContent(
            BodyMetricOrderedContentSpec(
                state = state,
                period = period,
                selectedDate = chartDaySelection.selectedDate,
                sectionContext = sectionContext,
                summaryContent = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(stringResource(R.string.section_weight))
                        WeightSummaryCard(
                            latestKg = state.summary.latestWeightKg,
                            changeKg = state.summary.weightChangeKg,
                            unitFormatter = unitFormatter,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                },
                periodChart = {
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
                },
                selectedDayEntries = {
                    chartDaySelection.selectedDate?.let { selectedDate ->
                        val zone = ZoneId.systemDefault()
                        PaginatedEntryList(
                            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                            entries = state.weightEntries
                                .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                                .sortedByDescending { it.time },
                        ) { entry, rowModifier ->
                            WeightEntryRow(
                                entry = entry,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onEdit = entry.editAction(BodyMeasurementType.WEIGHT, onEditBodyMeasurement),
                                onDelete = entry.deleteAction(BodyMeasurementType.WEIGHT, onDeleteBodyMeasurement),
                                modifier = rowModifier,
                            )
                        }
                    }
                },
                dataConfidence = {
                    BodyEntryDataConfidenceContent(
                        period = period,
                        entries = state.weightEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                    )
                },
                statistics = {
                    BodyNumericStatisticsContent(
                        latest = latest,
                        average = unitFormatter.weight(values.average()),
                        low = unitFormatter.weight(values.minOrNull() ?: 0.0),
                        high = unitFormatter.weight(values.maxOrNull() ?: 0.0),
                        readings = state.weightEntries.size,
                        comparison = previousLatestKg?.let {
                            periodComparison(
                                currentValue = state.weightEntries.maxByOrNull { entry -> entry.time }?.weightKg ?: 0.0,
                                previousValue = it,
                            )
                        },
                        selectedRange = state.selectedRange,
                        comparisonValueFormatter = { unitFormatter.weight(it) },
                        icon = Icons.Outlined.MonitorWeight,
                        accentColor = WeightColor,
                        unitFormatter = unitFormatter,
                        period = period,
                        baselineCurrentValue = state.weightEntries.maxByOrNull { entry -> entry.time }?.weightKg ?: 0.0,
                        baselineValues = state.baselineWeightEntries.map { it.weightBaselineValue() },
                    )
                },
                entries = {
                    PaginatedEntryList(
                        title = stringResource(R.string.section_entries),
                        entries = state.weightEntries.sortedByDescending { it.time },
                    ) { entry, rowModifier ->
                        WeightEntryRow(
                            entry = entry,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onEdit = entry.editAction(BodyMeasurementType.WEIGHT, onEditBodyMeasurement),
                            onDelete = entry.deleteAction(BodyMeasurementType.WEIGHT, onDeleteBodyMeasurement),
                            modifier = rowModifier,
                        )
                    }
                },
            ),
        )
    } else if (!state.isLoading) {
        noBodyMetricData(
            titleRes = R.string.metric_weight,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            messageRes = R.string.message_no_weight_period,
        )
    }
}

internal fun LazyListScope.bodyFatContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val latest = state.summary.latestBodyFatPercent
    if (latest != null) {
        val metricData = state.metricDataFor(BodyMetric.BODY_FAT, unitFormatter)
        val values = state.bodyFatEntries.map { it.percent }
        val previousLatestPercent = state.previousBodyFatEntries.maxByOrNull { it.time }?.percent
        renderBodyMetricOrderedContent(
            BodyMetricOrderedContentSpec(
                state = state,
                period = period,
                selectedDate = chartDaySelection.selectedDate,
                sectionContext = sectionContext,
                summaryContent = {
                    val value = unitFormatter.percent(latest)
                    MetricCard(
                        title = stringResource(R.string.metric_body_fat),
                        value = value.value,
                        unit = value.unit,
                        icon = Icons.Outlined.MonitorWeight,
                        accentColor = BodyFatColor,
                        modifier = metricModifier(),
                    )
                },
                periodChart = {
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
                },
                selectedDayEntries = {
                    chartDaySelection.selectedDate?.let { selectedDate ->
                        val zone = ZoneId.systemDefault()
                        PaginatedEntryList(
                            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                            entries = state.bodyFatEntries
                                .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                                .sortedByDescending { it.time },
                        ) { entry, rowModifier ->
                            BodyReadingRow(
                                value = unitFormatter.percent(entry.percent).text,
                                source = entry.source,
                                time = entry.time,
                                accentColor = BodyFatColor,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                    { onEditBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                                } else {
                                    null
                                },
                                onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                    { onDeleteBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                                } else {
                                    null
                                },
                                modifier = rowModifier,
                            )
                        }
                    }
                },
                dataConfidence = {
                    BodyEntryDataConfidenceContent(
                        period = period,
                        entries = state.bodyFatEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = BodyFatColor,
                    )
                },
                statistics = {
                    BodyNumericStatisticsContent(
                        latest = state.bodyFatEntries.maxByOrNull { it.time }?.let { unitFormatter.percent(it.percent) }
                            ?: unitFormatter.percent(0.0),
                        average = unitFormatter.percent(values.average()),
                        low = unitFormatter.percent(values.minOrNull() ?: 0.0),
                        high = unitFormatter.percent(values.maxOrNull() ?: 0.0),
                        readings = state.bodyFatEntries.size,
                        comparison = previousLatestPercent?.let {
                            periodComparison(
                                currentValue = state.bodyFatEntries.maxByOrNull { entry -> entry.time }?.percent ?: 0.0,
                                previousValue = it,
                            )
                        },
                        selectedRange = state.selectedRange,
                        comparisonValueFormatter = { unitFormatter.percent(it) },
                        icon = Icons.Outlined.MonitorWeight,
                        accentColor = BodyFatColor,
                        unitFormatter = unitFormatter,
                        period = period,
                        baselineCurrentValue = state.bodyFatEntries.maxByOrNull { entry -> entry.time }?.percent ?: 0.0,
                        baselineValues = state.baselineBodyFatEntries.map { it.bodyFatBaselineValue() },
                    )
                },
                entries = {
                    PaginatedEntryList(
                        title = stringResource(R.string.section_entries),
                        entries = state.bodyFatEntries.sortedByDescending { it.time },
                    ) { entry, rowModifier ->
                        BodyReadingRow(
                            value = unitFormatter.percent(entry.percent).text,
                            source = entry.source,
                            time = entry.time,
                            accentColor = BodyFatColor,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onEditBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                            } else {
                                null
                            },
                            onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onDeleteBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                            } else {
                                null
                            },
                            modifier = rowModifier,
                        )
                    }
                },
            ),
        )
    } else if (!state.isLoading) {
        noBodyMetricData(
            titleRes = R.string.metric_body_fat,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = BodyFatColor,
        )
    }
}

internal fun LazyListScope.singleBodyMetricContent(
    state: BodyUiState,
    period: DatePeriod,
    selectedDate: LocalDate?,
    sectionContext: MetricDetailSectionContext,
    titleRes: Int,
    value: DisplayValue?,
    comparison: PeriodComparison? = null,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue = { value ?: DisplayValue("", "") },
    icon: ImageVector,
    accentColor: Color,
    unitFormatter: UnitFormatter,
    selectedRange: TimeRange? = null,
    baselineCurrentValue: Double? = null,
    baselineValues: List<BaselineValue> = emptyList(),
    periodChart: (@Composable () -> Unit)? = null,
    dataConfidence: (@Composable () -> Unit)? = null,
    extraStatistics: (@Composable () -> Unit)? = null,
    selectedDayEntries: (@Composable () -> Unit)? = null,
    entries: (@Composable () -> Unit)? = null,
    entryCount: Int = 1,
) {
    if (value != null) {
        renderBodyMetricOrderedContent(
            BodyMetricOrderedContentSpec(
                state = state,
                period = period,
                selectedDate = selectedDate,
                sectionContext = sectionContext,
                summaryContent = {
                    MetricCard(
                        title = stringResource(titleRes),
                        value = value.value,
                        unit = value.unit,
                        icon = icon,
                        accentColor = accentColor,
                        modifier = metricModifier(),
                    )
                },
                periodChart = periodChart,
                selectedDayEntries = selectedDayEntries,
                dataConfidence = dataConfidence,
                statistics = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SingleBodyMetricStatisticsContent(
                            value = value,
                            comparison = comparison,
                            comparisonValueFormatter = comparisonValueFormatter,
                            icon = icon,
                            accentColor = accentColor,
                            unitFormatter = unitFormatter,
                            selectedRange = selectedRange,
                            period = period,
                            baselineCurrentValue = baselineCurrentValue,
                            baselineValues = baselineValues,
                            readings = entryCount,
                        )
                        extraStatistics?.invoke()
                    }
                },
                entries = entries,
            ),
        )
    } else if (!state.isLoading) {
        noBodyMetricData(titleRes, icon, accentColor)
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyMetricScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: BodyMetric,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.BODY,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
        ) { period ->
            bodyMetricContent(
                metric = metric,
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                sectionContext = sectionContext,
                onEditBodyMeasurement = onEditBodyMeasurement,
                onDeleteBodyMeasurement = viewModel::deleteBodyMeasurementEntry,
            )
        }
    }
}

internal fun LazyListScope.bodyMetricContent(
    metric: BodyMetric,
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    sectionContext: MetricDetailSectionContext,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
when (metric) {
    BodyMetric.WEIGHT -> weightContent(
        state,
        period,
        unitFormatter,
        dateTimeFormatterProvider,
        chartDaySelection,
        sectionContext,
        onEditBodyMeasurement,
        onDeleteBodyMeasurement,
    )
    BodyMetric.HEIGHT -> {
        val metricData = state.metricDataFor(BodyMetric.HEIGHT, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_height,
            value = state.summary.latestHeightCm?.let(unitFormatter::height),
            comparison = state.summary.previousLatestHeightCm?.let { previous ->
                periodComparison(currentValue = state.summary.latestHeightCm ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { unitFormatter.height(it) },
            icon = Icons.Outlined.Straighten,
            accentColor = WeightColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            entryCount = state.heightEntries.size,
            baselineCurrentValue = state.summary.latestHeightCm,
            baselineValues = state.baselineHeightEntries.map { it.heightBaselineValue() },
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.heightEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            selectedDayEntries = {
                chartDaySelection.selectedDate?.let { selectedDate ->
                    val zone = ZoneId.systemDefault()
                    PaginatedEntryList(
                        title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                        entries = state.heightEntries
                            .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                            .sortedByDescending { it.time },
                    ) { entry, rowModifier ->
                        BodyReadingRow(
                            value = unitFormatter.height(entry.heightCm).text,
                            source = entry.source,
                            time = entry.time,
                            accentColor = WeightColor,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onEditBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                            } else {
                                null
                            },
                            onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onDeleteBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                            } else {
                                null
                            },
                            modifier = rowModifier,
                        )
                    }
                }
            },
            entries = {
                PaginatedEntryList(
                    title = stringResource(R.string.section_entries),
                    entries = state.heightEntries.sortedByDescending { it.time },
                ) { entry, rowModifier ->
                    BodyReadingRow(
                        value = unitFormatter.height(entry.heightCm).text,
                        source = entry.source,
                        time = entry.time,
                        accentColor = WeightColor,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                            { onEditBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                        } else {
                            null
                        },
                        onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                            { onDeleteBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                        } else {
                            null
                        },
                        modifier = rowModifier,
                    )
                }
            },
        )
    }
    BodyMetric.BMI -> {
        val metricData = state.metricDataFor(BodyMetric.BMI, unitFormatter)
        val heightMeters = state.summary.heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 }
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_bmi,
            value = state.summary.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            comparison = state.summary.previousBmi?.let { previous ->
                periodComparison(currentValue = state.summary.bmi ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { DisplayValue(unitFormatter.decimal(it, 1), "") },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            baselineCurrentValue = state.summary.bmi,
            baselineValues = bmiBaselineValues(state.baselineWeightEntries, state.summary.heightCm),
            entryCount = state.weightEntries.size,
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.weightEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    valueKind = DataValueKind.CALCULATED,
                )
            },
            extraStatistics = {
                BmiContextCardsContent(
                    bmi = state.summary.bmi,
                    ffmi = state.summary.ffmi,
                    adjustedFfmi = state.summary.adjustedFfmi,
                    unitFormatter = unitFormatter,
                )
            },
            selectedDayEntries = if (heightMeters != null) {
                {
                    chartDaySelection.selectedDate?.let { selectedDate ->
                        val zone = ZoneId.systemDefault()
                        PaginatedEntryList(
                            title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                            entries = state.weightEntries
                                .filter { it.time.atZone(zone).toLocalDate() == selectedDate }
                                .sortedByDescending { it.time },
                        ) { entry, rowModifier ->
                            BodyReadingRow(
                                value = unitFormatter.decimal(entry.weightKg / (heightMeters * heightMeters), 1),
                                source = entry.source,
                                time = entry.time,
                                accentColor = WeightColor,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                    { onEditBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                                } else {
                                    null
                                },
                                onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                    { onDeleteBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                                } else {
                                    null
                                },
                                modifier = rowModifier,
                            )
                        }
                    }
                }
            } else {
                null
            },
            entries = if (heightMeters != null) {
                {
                    PaginatedEntryList(
                        title = entryListTitle(null, dateTimeFormatterProvider),
                        entries = state.weightEntries.sortedByDescending { it.time },
                    ) { entry, rowModifier ->
                        BodyReadingRow(
                            value = unitFormatter.decimal(entry.weightKg / (heightMeters * heightMeters), 1),
                            source = entry.source,
                            time = entry.time,
                            accentColor = WeightColor,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onEdit = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onEditBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                            } else {
                                null
                            },
                            onDelete = if (entry.isOpenVitalsEntry && entry.id.isNotBlank()) {
                                { onDeleteBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                            } else {
                                null
                            },
                            modifier = rowModifier,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
    BodyMetric.BODY_FAT -> bodyFatContent(
        state,
        period,
        unitFormatter,
        dateTimeFormatterProvider,
        chartDaySelection,
        sectionContext,
        onEditBodyMeasurement,
        onDeleteBodyMeasurement,
    )
    BodyMetric.LEAN_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.LEAN_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_lean_mass,
            value = state.summary.latestLeanMassKg?.let(unitFormatter::bodyMass),
            comparison = state.summary.previousLatestLeanMassKg?.let { previous ->
                periodComparison(currentValue = state.summary.latestLeanMassKg ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { unitFormatter.bodyMass(it) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            entryCount = state.leanMassEntries.size,
            baselineCurrentValue = state.summary.latestLeanMassKg,
            baselineValues = state.baselineLeanMassEntries.map { it.leanMassBaselineValue() },
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.leanMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            selectedDayEntries = {
                bodyMetricReadingEntriesContent(
                    entries = state.leanMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = true,
                )
            },
            entries = {
                bodyMetricReadingEntriesContent(
                    entries = state.leanMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = false,
                )
            },
        )
    }
    BodyMetric.BMR -> {
        val metricData = state.metricDataFor(BodyMetric.BMR, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_bmr,
            value = state.summary.latestBmrKcal?.let(unitFormatter::energy),
            comparison = state.summary.previousLatestBmrKcal?.let { previous ->
                periodComparison(currentValue = state.summary.latestBmrKcal ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { unitFormatter.energy(it) },
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            entryCount = state.bmrEntries.size,
            baselineCurrentValue = state.summary.latestBmrKcal,
            baselineValues = state.baselineBmrEntries.map { it.bmrBaselineValue() },
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.bmrEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = CaloriesColor,
                )
            },
            selectedDayEntries = {
                bodyMetricReadingEntriesContent(
                    entries = state.bmrEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.energy(it.kcalPerDay).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = CaloriesColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = true,
                )
            },
            entries = {
                bodyMetricReadingEntriesContent(
                    entries = state.bmrEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.energy(it.kcalPerDay).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = CaloriesColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = false,
                )
            },
        )
    }
    BodyMetric.BONE_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.BONE_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_bone_mass,
            value = state.summary.latestBoneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            comparison = state.summary.previousLatestBoneMassKg?.let { previous ->
                periodComparison(currentValue = state.summary.latestBoneMassKg ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            entryCount = state.boneMassEntries.size,
            baselineCurrentValue = state.summary.latestBoneMassKg,
            baselineValues = state.baselineBoneMassEntries.map { it.boneMassBaselineValue() },
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.boneMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            selectedDayEntries = {
                bodyMetricReadingEntriesContent(
                    entries = state.boneMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = true,
                )
            },
            entries = {
                bodyMetricReadingEntriesContent(
                    entries = state.boneMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = false,
                )
            },
        )
    }
    BodyMetric.BODY_WATER_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.BODY_WATER_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
            selectedDate = chartDaySelection.selectedDate,
            sectionContext = sectionContext,
            titleRes = R.string.metric_body_water_mass,
            value = state.summary.latestBodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            comparison = state.summary.previousLatestBodyWaterMassKg?.let { previous ->
                periodComparison(currentValue = state.summary.latestBodyWaterMassKg ?: 0.0, previousValue = previous)
            },
            comparisonValueFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            unitFormatter = unitFormatter,
            selectedRange = state.selectedRange,
            entryCount = state.bodyWaterMassEntries.size,
            baselineCurrentValue = state.summary.latestBodyWaterMassKg,
            baselineValues = state.baselineBodyWaterMassEntries.map { it.bodyWaterMassBaselineValue() },
            periodChart = if (metricData.hasTrackedValues) {
                {
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
            } else {
                null
            },
            dataConfidence = {
                BodyEntryDataConfidenceContent(
                    period = period,
                    entries = state.bodyWaterMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            selectedDayEntries = {
                bodyMetricReadingEntriesContent(
                    entries = state.bodyWaterMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = true,
                )
            },
            entries = {
                bodyMetricReadingEntriesContent(
                    entries = state.bodyWaterMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedOnly = false,
                )
            },
        )
    }
}
}
