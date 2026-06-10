package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.BmiCategory
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.bmiInterpretation
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class BodyMetric {
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    BODY_WATER_MASS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "body",
    )

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        bodyContent(
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

@Composable
fun WeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun HeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun BmiScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun BodyFatScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onEditBodyMeasurement = onEditBodyMeasurement,
    )
}

@Composable
fun LeanMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun BmrScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun BoneMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
fun BodyWaterMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BodyMetricScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: BodyMetric,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate, metric)

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        when (metric) {
            BodyMetric.WEIGHT -> weightContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditBodyMeasurement,
                viewModel::deleteBodyMeasurementEntry,
            )
            BodyMetric.HEIGHT -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_height,
                value = state.latestHeightCm?.let(unitFormatter::height),
                comparison = state.previousLatestHeightCm?.let { previous ->
                    periodComparison(currentValue = state.latestHeightCm ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { unitFormatter.height(it) },
                icon = Icons.Outlined.Straighten,
                accentColor = WeightColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                entryCount = state.heightEntries.size,
                baselineCurrentValue = state.latestHeightCm,
                baselineValues = state.baselineHeightEntries.map { it.heightBaselineValue() },
                contextContent = {
                    bodyEntryDataConfidence(
                        period = period,
                        entries = state.heightEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                    )
                },
                entriesContent = {
                    bodyReadingEntries(
                        entries = state.heightEntries,
                        value = { unitFormatter.height(it.heightCm).text },
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                        onEdit = { onEditBodyMeasurement(BodyMeasurementType.HEIGHT, it.id) },
                        onDelete = { viewModel.deleteBodyMeasurementEntry(BodyMeasurementType.HEIGHT, it.id) },
                    )
                },
            )
            BodyMetric.BMI -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_bmi,
                value = state.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
                comparison = state.previousBmi?.let { previous ->
                    periodComparison(currentValue = state.bmi ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { DisplayValue(unitFormatter.decimal(it, 1), "") },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                baselineCurrentValue = state.bmi,
                baselineValues = bmiBaselineValues(state.baselineWeightEntries, state.heightCm),
                entryCount = state.weightEntries.size,
                contextContent = {
                    bmiDataConfidence(state, period)
                    bmiContextCard(state.bmi)
                },
                entriesContent = {
                    bmiEntries(
                        entries = state.weightEntries,
                        heightCm = state.heightCm,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onEditBodyMeasurement = onEditBodyMeasurement,
                        onDeleteBodyMeasurement = viewModel::deleteBodyMeasurementEntry,
                    )
                },
            )
            BodyMetric.BODY_FAT -> bodyFatContent(
                state,
                period,
                unitFormatter,
                dateTimeFormatterProvider,
                chartDaySelection,
                onEditBodyMeasurement,
                viewModel::deleteBodyMeasurementEntry,
            )
            BodyMetric.LEAN_MASS -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_lean_mass,
                value = state.latestLeanMassKg?.let(unitFormatter::bodyMass),
                comparison = state.previousLatestLeanMassKg?.let { previous ->
                    periodComparison(currentValue = state.latestLeanMassKg ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { unitFormatter.bodyMass(it) },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                entryCount = state.leanMassEntries.size,
                baselineCurrentValue = state.latestLeanMassKg,
                baselineValues = state.baselineLeanMassEntries.map { it.leanMassBaselineValue() },
                contextContent = {
                    bodyEntryDataConfidence(
                        period = period,
                        entries = state.leanMassEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                    )
                },
                entriesContent = {
                    bodyReadingEntries(
                        entries = state.leanMassEntries,
                        value = { unitFormatter.bodyMass(it.massKg).text },
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                },
            )
            BodyMetric.BMR -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_bmr,
                value = state.latestBmrKcal?.let(unitFormatter::energy),
                comparison = state.previousLatestBmrKcal?.let { previous ->
                    periodComparison(currentValue = state.latestBmrKcal ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { unitFormatter.energy(it) },
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = CaloriesColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                entryCount = state.bmrEntries.size,
                baselineCurrentValue = state.latestBmrKcal,
                baselineValues = state.baselineBmrEntries.map { it.bmrBaselineValue() },
                contextContent = {
                    bodyEntryDataConfidence(
                        period = period,
                        entries = state.bmrEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = CaloriesColor,
                    )
                },
                entriesContent = {
                    bodyReadingEntries(
                        entries = state.bmrEntries,
                        value = { unitFormatter.energy(it.kcalPerDay).text },
                        source = { it.source },
                        time = { it.time },
                        accentColor = CaloriesColor,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                },
            )
            BodyMetric.BONE_MASS -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_bone_mass,
                value = state.latestBoneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
                comparison = state.previousLatestBoneMassKg?.let { previous ->
                    periodComparison(currentValue = state.latestBoneMassKg ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                entryCount = state.boneMassEntries.size,
                baselineCurrentValue = state.latestBoneMassKg,
                baselineValues = state.baselineBoneMassEntries.map { it.boneMassBaselineValue() },
                contextContent = {
                    bodyEntryDataConfidence(
                        period = period,
                        entries = state.boneMassEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                    )
                },
                entriesContent = {
                    bodyReadingEntries(
                        entries = state.boneMassEntries,
                        value = { unitFormatter.bodyMass(it.massKg, decimals = 2).text },
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                    )
                },
            )
            BodyMetric.BODY_WATER_MASS -> singleBodyMetricContent(
                state = state,
                period = period,
                titleRes = R.string.metric_body_water_mass,
                value = state.latestBodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
                comparison = state.previousLatestBodyWaterMassKg?.let { previous ->
                    periodComparison(currentValue = state.latestBodyWaterMassKg ?: 0.0, previousValue = previous)
                },
                comparisonValueFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
                unitFormatter = unitFormatter,
                selectedRange = state.selectedRange,
                entryCount = state.bodyWaterMassEntries.size,
                baselineCurrentValue = state.latestBodyWaterMassKg,
                baselineValues = state.baselineBodyWaterMassEntries.map { it.bodyWaterMassBaselineValue() },
                contextContent = {
                    bodyEntryDataConfidence(
                        period = period,
                        entries = state.bodyWaterMassEntries,
                        source = { it.source },
                        time = { it.time },
                        accentColor = WeightColor,
                    )
                },
                entriesContent = {
                    bodyReadingEntries(
                        entries = state.bodyWaterMassEntries,
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

private fun LazyListScope.bodyContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val metricsData = bodyMetricData(state, unitFormatter)
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
            bodyMetricTrend(
                metricData = metricData,
                state = state,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
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

    if (state.bmi != null) {
        bmiContextCard(state.bmi)
    }
}

private fun LazyListScope.bodyOverviewStatistics(
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

private fun LazyListScope.bodyMetricTrend(
    metricData: BodyMetricData,
    state: BodyUiState,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    item {
        val summary = metricData.latest?.text
            ?: stringResource(R.string.summary_entries, metricData.values.size.toString())
        PeriodHistoryChart(
            title = stringResource(metricData.titleRes),
            values = metricData.values,
            selectedRange = state.selectedRange,
            period = period,
            accentColor = metricData.color.copy(alpha = 0.85f),
            summaryText = "${localizedPeriodTitle(state.selectedRange, period)} · $summary",
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { metricData.valueDisplayFormatter(it).text },
        )
    }
}

private fun LazyListScope.selectedDateBodyEntries(
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

private fun LazyListScope.bodyAllReadingEntries(
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

private fun LazyListScope.bmiDataConfidence(
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

private fun <T> LazyListScope.bodyEntryDataConfidence(
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

private fun LazyListScope.weightContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    if (state.weightEntries.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_weight)) }
        item {
            WeightSummaryCard(
                latestKg = state.latestWeightKg,
                changeKg = state.weightChangKg,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            WeightLineChart(
                entries = state.weightEntries,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = metricModifier(),
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
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

private fun LazyListScope.bodyFatContent(
    state: BodyUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: ChartDaySelection,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
) {
    val latest = state.latestBodyFatPercent
    if (latest != null) {
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
        if (state.bodyFatEntries.size >= 2) {
            item {
                BodyFatLineChart(
                    entries = state.bodyFatEntries,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                bodyReadingEntries(
                    entries = state.bodyFatEntries.filter {
                        it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                    },
                    value = { unitFormatter.percent(it.percent).text },
                    source = { it.source },
                    time = { it.time },
                    accentColor = BodyFatColor,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = selectedDate,
                    editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
                    onEdit = { onEditBodyMeasurement(BodyMeasurementType.BODY_FAT, it.id) },
                    onDelete = { onDeleteBodyMeasurement(BodyMeasurementType.BODY_FAT, it.id) },
                )
            }
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
        bodyReadingEntries(
            entries = state.bodyFatEntries,
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

private fun LazyListScope.singleBodyMetricContent(
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


