package tech.mmarca.openvitals.features.body

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
import tech.mmarca.openvitals.core.presentation.UnitFormatter
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
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val metricsData = bodyMetricData(state, state.summary, unitFormatter)
    val trackedMetricsData = metricsData.filter { it.hasTrackedValues }
    val hasAnyBodyData = metricsData.any { it.latest != null || it.values.isNotEmpty() } ||
        state.weightEntries.isNotEmpty() ||
        state.heightEntries.isNotEmpty() ||
        state.bodyFatEntries.isNotEmpty() ||
        state.leanMassEntries.isNotEmpty() ||
        state.bmrEntries.isNotEmpty() ||
        state.boneMassEntries.isNotEmpty() ||
        state.bodyWaterMassEntries.isNotEmpty()

    if (!hasAnyBodyData && !state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.screen_body),
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                message = stringResource(R.string.message_no_readings_period),
                modifier = metricModifier(),
            )
        }
        return
    }

    bodyOverviewStatistics(metricsData)

    if (trackedMetricsData.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_body_trends)) }
        trackedMetricsData.forEach { metricData ->
            item {
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

    chartDaySelection.selectedDate?.let { selectedDate ->
        selectedDateBodyEntries(
            state = state,
            selectedDate = selectedDate,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditBodyMeasurement = onEditBodyMeasurement,
            onDeleteBodyMeasurement = onDeleteBodyMeasurement,
        )
    }

    bodyAllReadingEntries(
        state = state,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditBodyMeasurement = onEditBodyMeasurement,
        onDeleteBodyMeasurement = onDeleteBodyMeasurement,
    )

    bmiContextCard(state.summary.bmi, state.summary.ffmi, state.summary.adjustedFfmi, unitFormatter)
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
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    if (state.weightEntries.isNotEmpty()) {
        val metricData = state.metricDataFor(BodyMetric.WEIGHT, unitFormatter)
        item { SectionHeader(stringResource(R.string.section_weight)) }
        item {
            WeightSummaryCard(
                latestKg = state.summary.latestWeightKg,
                changeKg = state.summary.weightChangeKg,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
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
        chartDaySelection.selectedDate?.let { selectedDate ->
            item {
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
        }
        bodyEntryDataConfidence(
            period = period,
            entries = state.weightEntries,
            source = { it.source },
            time = { it.time },
            accentColor = WeightColor,
        )
        weightStatistics(
            entries = state.weightEntries,
            previousEntries = state.previousWeightEntries,
            baselineEntries = state.baselineWeightEntries,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        item {
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
        }
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
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val latest = state.summary.latestBodyFatPercent
    if (latest != null) {
        val metricData = state.metricDataFor(BodyMetric.BODY_FAT, unitFormatter)
        item {
            val value = unitFormatter.percent(latest)
            MetricCard(
                title = stringResource(R.string.metric_body_fat),
                value = value.value,
                unit = value.unit,
                icon = Icons.Outlined.MonitorWeight,
                accentColor = BodyFatColor,
                modifier = metricModifier(),
            )
        }
        item {
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
        bodyEntryDataConfidence(
            period = period,
            entries = state.bodyFatEntries,
            source = { it.source },
            time = { it.time },
            accentColor = BodyFatColor,
        )
        bodyFatStatistics(
            entries = state.bodyFatEntries,
            previousEntries = state.previousBodyFatEntries,
            baselineEntries = state.baselineBodyFatEntries,
            period = period,
            selectedRange = state.selectedRange,
            unitFormatter = unitFormatter,
        )
        bodyMetricReadingEntries(
            entries = state.bodyFatEntries,
            selectedDate = chartDaySelection.selectedDate,
            value = { unitFormatter.percent(it.percent).text },
            source = { it.source },
            time = { it.time },
            accentColor = BodyFatColor,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
            onEdit = { onEditBodyMeasurement(BodyMeasurementType.BODY_FAT, it.id) },
            onDelete = { onDeleteBodyMeasurement(BodyMeasurementType.BODY_FAT, it.id) },
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
    contextContent: (LazyListScope.() -> Unit)? = null,
    entriesContent: (LazyListScope.() -> Unit)? = null,
    entryCount: Int = 1,
) {
    if (value != null) {
        item {
            MetricCard(
                title = stringResource(titleRes),
                value = value.value,
                unit = value.unit,
                icon = icon,
                accentColor = accentColor,
                modifier = metricModifier(),
            )
        }
        contextContent?.invoke(this)
        singleBodyMetricStatistics(
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
        entriesContent?.invoke(this)
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        ) { period ->
            bodyMetricContent(
                metric = metric,
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
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
        onEditBodyMeasurement,
        onDeleteBodyMeasurement,
    )
    BodyMetric.HEIGHT -> {
        val metricData = state.metricDataFor(BodyMetric.HEIGHT, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bodyEntryDataConfidence(
                    period = period,
                    entries = state.heightEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            entriesContent = {
                bodyMetricReadingEntries(
                    entries = state.heightEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.height(it.heightCm).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditBodyMeasurement(BodyMeasurementType.HEIGHT, it.id) },
                    onDelete = { onDeleteBodyMeasurement(BodyMeasurementType.HEIGHT, it.id) },
                )
            },
        )
    }
    BodyMetric.BMI -> {
        val metricData = state.metricDataFor(BodyMetric.BMI, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bmiDataConfidence(state, period)
                bmiContextCard(state.summary.bmi, state.summary.ffmi, state.summary.adjustedFfmi, unitFormatter)
            },
            entriesContent = {
                chartDaySelection.selectedDate?.let { selectedDate ->
                    bmiEntries(
                        entries = state.weightEntries.filter {
                            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        },
                        heightCm = state.summary.heightCm,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        titleDate = selectedDate,
                        onEditBodyMeasurement = onEditBodyMeasurement,
                        onDeleteBodyMeasurement = onDeleteBodyMeasurement,
                    )
                }
                bmiEntries(
                    entries = state.weightEntries,
                    heightCm = state.summary.heightCm,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditBodyMeasurement = onEditBodyMeasurement,
                    onDeleteBodyMeasurement = onDeleteBodyMeasurement,
                )
            },
        )
    }
    BodyMetric.BODY_FAT -> bodyFatContent(
        state,
        period,
        unitFormatter,
        dateTimeFormatterProvider,
        chartDaySelection,
        onEditBodyMeasurement,
        onDeleteBodyMeasurement,
    )
    BodyMetric.LEAN_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.LEAN_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bodyEntryDataConfidence(
                    period = period,
                    entries = state.leanMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            entriesContent = {
                bodyMetricReadingEntries(
                    entries = state.leanMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    }
    BodyMetric.BMR -> {
        val metricData = state.metricDataFor(BodyMetric.BMR, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bodyEntryDataConfidence(
                    period = period,
                    entries = state.bmrEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = CaloriesColor,
                )
            },
            entriesContent = {
                bodyMetricReadingEntries(
                    entries = state.bmrEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.energy(it.kcalPerDay).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = CaloriesColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    }
    BodyMetric.BONE_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.BONE_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bodyEntryDataConfidence(
                    period = period,
                    entries = state.boneMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            entriesContent = {
                bodyMetricReadingEntries(
                    entries = state.boneMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    }
    BodyMetric.BODY_WATER_MASS -> {
        val metricData = state.metricDataFor(BodyMetric.BODY_WATER_MASS, unitFormatter)
        singleBodyMetricContent(
            state = state,
            period = period,
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
            contextContent = {
                if (metricData.hasTrackedValues) {
                    item {
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
                bodyEntryDataConfidence(
                    period = period,
                    entries = state.bodyWaterMassEntries,
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                )
            },
            entriesContent = {
                bodyMetricReadingEntries(
                    entries = state.bodyWaterMassEntries,
                    selectedDate = chartDaySelection.selectedDate,
                    value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = WeightColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            },
        )
    }
}
}
