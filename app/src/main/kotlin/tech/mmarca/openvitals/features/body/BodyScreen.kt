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
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineValue
import tech.mmarca.openvitals.core.insights.BmiCategory
import tech.mmarca.openvitals.core.insights.DataValueKind
import tech.mmarca.openvitals.core.insights.PeriodComparison
import tech.mmarca.openvitals.core.insights.bmiInterpretation
import tech.mmarca.openvitals.core.insights.dataConfidence
import tech.mmarca.openvitals.core.insights.periodComparison
import tech.mmarca.openvitals.core.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.BmrEntry
import tech.mmarca.openvitals.data.model.BoneMassEntry
import tech.mmarca.openvitals.data.model.HeightEntry
import tech.mmarca.openvitals.data.model.LeanBodyMassEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.ZoneId

enum class BodyMetric {
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.WEIGHT,
    )
}

@Composable
fun HeightScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.HEIGHT,
    )
}

@Composable
fun BmiScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BMI,
    )
}

@Composable
fun BodyFatScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BODY_FAT,
    )
}

@Composable
fun LeanMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.LEAN_MASS,
    )
}

@Composable
fun BmrScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BMR,
    )
}

@Composable
fun BoneMassScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    BodyMetricScreen(
        viewModel = viewModel,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        metric = BodyMetric.BONE_MASS,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BodyMetricScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: BodyMetric,
) {
    val state by viewModel.uiState.collectAsState()

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
    ) { period ->
        when (metric) {
            BodyMetric.WEIGHT -> weightContent(state, period, unitFormatter, dateTimeFormatterProvider)
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
                    )
                },
            )
            BodyMetric.BODY_FAT -> bodyFatContent(state, period, unitFormatter, dateTimeFormatterProvider)
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
            )
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

private fun LazyListScope.bmiContextCard(bmi: Double?) {
    val interpretation = bmi?.let { bmiInterpretation(it) } ?: return

    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_bmi_title),
            status = when (interpretation.category) {
                BmiCategory.UNDERWEIGHT -> stringResource(R.string.interpretation_bmi_underweight)
                BmiCategory.HEALTHY -> stringResource(R.string.interpretation_bmi_healthy)
                BmiCategory.OVERWEIGHT -> stringResource(R.string.interpretation_bmi_overweight)
                BmiCategory.OBESITY_CLASS_1 -> stringResource(R.string.interpretation_bmi_obesity_1)
                BmiCategory.OBESITY_CLASS_2 -> stringResource(R.string.interpretation_bmi_obesity_2)
                BmiCategory.OBESITY_CLASS_3 -> stringResource(R.string.interpretation_bmi_obesity_3)
            },
            body = stringResource(R.string.interpretation_bmi_body),
            source = stringResource(R.string.interpretation_bmi_source),
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.weightStatistics(
    entries: List<WeightEntry>,
    previousEntries: List<WeightEntry>,
    baselineEntries: List<WeightEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.weightKg }
    val latest = entries.maxByOrNull { it.time }?.let { unitFormatter.weight(it.weightKg) } ?: unitFormatter.weight(0.0)
    val previousLatestKg = previousEntries.maxByOrNull { it.time }?.weightKg
    bodyNumericStatistics(
        latest = latest,
        average = unitFormatter.weight(values.average()),
        low = unitFormatter.weight(values.minOrNull() ?: 0.0),
        high = unitFormatter.weight(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousLatestKg?.let {
            periodComparison(
                currentValue = entries.maxByOrNull { entry -> entry.time }?.weightKg ?: 0.0,
                previousValue = it,
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.weight(it) },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        unitFormatter = unitFormatter,
        period = period,
        baselineCurrentValue = entries.maxByOrNull { entry -> entry.time }?.weightKg ?: 0.0,
        baselineValues = baselineEntries.map { it.weightBaselineValue() },
    )
}

private fun LazyListScope.bodyFatStatistics(
    entries: List<BodyFatEntry>,
    previousEntries: List<BodyFatEntry>,
    baselineEntries: List<BodyFatEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.percent }
    val previousLatestPercent = previousEntries.maxByOrNull { it.time }?.percent
    bodyNumericStatistics(
        latest = entries.maxByOrNull { it.time }?.let { unitFormatter.percent(it.percent) } ?: unitFormatter.percent(0.0),
        average = unitFormatter.percent(values.average()),
        low = unitFormatter.percent(values.minOrNull() ?: 0.0),
        high = unitFormatter.percent(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousLatestPercent?.let {
            periodComparison(
                currentValue = entries.maxByOrNull { entry -> entry.time }?.percent ?: 0.0,
                previousValue = it,
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.percent(it) },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = BodyFatColor,
        unitFormatter = unitFormatter,
        period = period,
        baselineCurrentValue = entries.maxByOrNull { entry -> entry.time }?.percent ?: 0.0,
        baselineValues = baselineEntries.map { it.bodyFatBaselineValue() },
    )
}

private fun LazyListScope.singleBodyMetricStatistics(
    value: DisplayValue,
    comparison: PeriodComparison?,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    unitFormatter: UnitFormatter,
    selectedRange: TimeRange?,
    period: DatePeriod,
    baselineCurrentValue: Double?,
    baselineValues: List<BaselineValue>,
    readings: Int,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_latest),
                    value = value.value,
                    unit = value.unit,
                    icon = icon,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_readings),
                    value = unitFormatter.count(readings),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = accentColor,
                ),
            ) + if (comparison != null && selectedRange != null) {
                listOf(
                    previousPeriodInsightStat(
                        comparison = comparison,
                        selectedRange = selectedRange,
                        unitFormatter = unitFormatter,
                        valueFormatter = comparisonValueFormatter,
                        accentColor = accentColor,
                    )
                )
            } else {
                emptyList()
            } + baselineCurrentValue?.let { current ->
                personalBaselineInsightStats(
                    insight = personalBaselineInsight(
                        currentValue = current,
                        values = baselineValues,
                        referenceDate = period.start.minusDays(1),
                    ),
                    unitFormatter = unitFormatter,
                    valueFormatter = comparisonValueFormatter,
                    accentColor = accentColor,
                )
            }.orEmpty(),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.bodyNumericStatistics(
    latest: DisplayValue,
    average: DisplayValue,
    low: DisplayValue,
    high: DisplayValue,
    readings: Int,
    comparison: PeriodComparison?,
    selectedRange: TimeRange,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    unitFormatter: UnitFormatter,
    period: DatePeriod,
    baselineCurrentValue: Double,
    baselineValues: List<BaselineValue>,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_latest),
                    value = latest.value,
                    unit = latest.unit,
                    icon = icon,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_lowest),
                    value = low.value,
                    unit = low.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = high.value,
                    unit = high.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_readings),
                    value = unitFormatter.count(readings),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = accentColor,
                ),
            ) + comparison?.let {
                listOf(
                    previousPeriodInsightStat(
                        comparison = it,
                        selectedRange = selectedRange,
                        unitFormatter = unitFormatter,
                        valueFormatter = comparisonValueFormatter,
                        accentColor = accentColor,
                    )
                )
            }.orEmpty() + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = baselineCurrentValue,
                    values = baselineValues,
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = comparisonValueFormatter,
                accentColor = accentColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun LazyListScope.noBodyMetricData(
    titleRes: Int,
    icon: ImageVector,
    accentColor: Color,
    messageRes: Int = R.string.message_no_readings_period,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = metricModifier(),
        )
    }
}

private fun <T> LazyListScope.bodyReadingEntries(
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val sortedEntries = entries.sortedByDescending(time)
    item {
        PaginatedEntryList(
            title = stringResource(R.string.section_entries),
            entries = sortedEntries,
        ) { entry, rowModifier ->
            BodyReadingRow(
                value = value(entry),
                source = source(entry),
                time = time(entry),
                accentColor = accentColor,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = rowModifier,
            )
        }
    }
}

private fun LazyListScope.bmiEntries(
    entries: List<WeightEntry>,
    heightCm: Double?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val heightMeters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return
    bodyReadingEntries(
        entries = entries,
        value = {
            DisplayValue(unitFormatter.decimal(it.weightKg / (heightMeters * heightMeters), 1), "").text
        },
        source = { it.source },
        time = { it.time },
        accentColor = WeightColor,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
    )
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

private fun WeightEntry.weightBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = weightKg,
    )

private fun BodyFatEntry.bodyFatBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = percent,
    )

private fun HeightEntry.heightBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = heightCm,
    )

private fun LeanBodyMassEntry.leanMassBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = massKg,
    )

private fun BmrEntry.bmrBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = kcalPerDay,
    )

private fun BoneMassEntry.boneMassBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = massKg,
    )

private fun bmiBaselineValues(
    entries: List<WeightEntry>,
    heightCm: Double?,
): List<BaselineValue> {
    val heightMeters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return emptyList()
    return entries.map { entry ->
        BaselineValue(
            date = entry.time.atZone(ZoneId.systemDefault()).toLocalDate(),
            value = entry.weightKg / (heightMeters * heightMeters),
        )
    }
}
