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

internal fun LazyListScope.bmiContextCard(bmi: Double?) {
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

internal fun LazyListScope.weightStatistics(
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

internal fun LazyListScope.bodyFatStatistics(
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

internal fun LazyListScope.singleBodyMetricStatistics(
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

internal fun LazyListScope.bodyNumericStatistics(
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

internal fun LazyListScope.noBodyMetricData(
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

internal fun <T> LazyListScope.bodyReadingEntries(
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    val sortedEntries = entries.sortedByDescending(time)
    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = sortedEntries,
        ) { entry, rowModifier ->
            BodyReadingRow(
                value = value(entry),
                source = source(entry),
                time = time(entry),
                accentColor = accentColor,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit
                    ?.takeIf { editable(entry) }
                    ?.let { edit -> { edit(entry) } },
                onDelete = onDelete
                    ?.takeIf { editable(entry) }
                    ?.let { delete -> { delete(entry) } },
                modifier = rowModifier,
            )
        }
    }
}

internal fun LazyListScope.bmiEntries(
    entries: List<WeightEntry>,
    heightCm: Double?,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit = { _, _ -> },
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
        editable = { it.isOpenVitalsEntry && it.id.isNotBlank() },
        onEdit = { onEditBodyMeasurement(BodyMeasurementType.WEIGHT, it.id) },
        onDelete = { onDeleteBodyMeasurement(BodyMeasurementType.WEIGHT, it.id) },
    )
}

internal data class BodyMetricData(
    val metric: BodyMetric,
    val titleRes: Int,
    val latest: DisplayValue?,
    val values: List<PeriodChartValue>,
    val color: Color,
    val icon: ImageVector,
    val valueDisplayFormatter: (Double) -> DisplayValue,
) {
    val hasTrackedValues: Boolean = values.isNotEmpty()
}

internal data class BodyReadingItem(
    val value: String,
    val source: String,
    val time: Instant,
    val accentColor: Color,
    val onEdit: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
)

internal fun bodyMetricData(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
): List<BodyMetricData> =
    listOf(
        BodyMetricData(
            metric = BodyMetric.WEIGHT,
            titleRes = R.string.metric_weight,
            latest = state.latestWeightKg?.let(unitFormatter::weight),
            values = dailyLatestValues(state.weightEntries, time = { it.time }, value = { it.weightKg }),
            color = WeightColor,
            icon = Icons.Outlined.MonitorWeight,
            valueDisplayFormatter = { unitFormatter.weight(it) },
        ),
        BodyMetricData(
            metric = BodyMetric.HEIGHT,
            titleRes = R.string.metric_height,
            latest = state.latestHeightCm?.let(unitFormatter::height),
            values = dailyLatestValues(state.heightEntries, time = { it.time }, value = { it.heightCm }),
            color = WeightColor,
            icon = Icons.Outlined.Straighten,
            valueDisplayFormatter = { unitFormatter.height(it) },
        ),
        BodyMetricData(
            metric = BodyMetric.BMI,
            titleRes = R.string.metric_bmi,
            latest = state.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            values = bmiHistoryValues(state.weightEntries, state.heightCm),
            color = WeightColor,
            icon = Icons.Outlined.MonitorWeight,
            valueDisplayFormatter = { DisplayValue(unitFormatter.decimal(it, 1), "") },
        ),
        BodyMetricData(
            metric = BodyMetric.BODY_FAT,
            titleRes = R.string.metric_body_fat,
            latest = state.latestBodyFatPercent?.let(unitFormatter::percent),
            values = dailyLatestValues(state.bodyFatEntries, time = { it.time }, value = { it.percent }),
            color = BodyFatColor,
            icon = Icons.Outlined.MonitorWeight,
            valueDisplayFormatter = { unitFormatter.percent(it) },
        ),
        BodyMetricData(
            metric = BodyMetric.LEAN_MASS,
            titleRes = R.string.metric_lean_mass,
            latest = state.latestLeanMassKg?.let(unitFormatter::bodyMass),
            values = dailyLatestValues(state.leanMassEntries, time = { it.time }, value = { it.massKg }),
            color = WeightColor,
            icon = Icons.Outlined.MonitorWeight,
            valueDisplayFormatter = { unitFormatter.bodyMass(it) },
        ),
        BodyMetricData(
            metric = BodyMetric.BONE_MASS,
            titleRes = R.string.metric_bone_mass,
            latest = state.latestBoneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            values = dailyLatestValues(state.boneMassEntries, time = { it.time }, value = { it.massKg }),
            color = WeightColor,
            icon = Icons.Outlined.MonitorWeight,
            valueDisplayFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
        ),
        BodyMetricData(
            metric = BodyMetric.BODY_WATER_MASS,
            titleRes = R.string.metric_body_water_mass,
            latest = state.latestBodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            values = dailyLatestValues(state.bodyWaterMassEntries, time = { it.time }, value = { it.massKg }),
            color = WeightColor,
            icon = Icons.Outlined.LocalDrink,
            valueDisplayFormatter = { unitFormatter.bodyMass(it, decimals = 2) },
        ),
        BodyMetricData(
            metric = BodyMetric.BMR,
            titleRes = R.string.metric_bmr,
            latest = state.latestBmrKcal?.let(unitFormatter::energy),
            values = dailyLatestValues(state.bmrEntries, time = { it.time }, value = { it.kcalPerDay }),
            color = CaloriesColor,
            icon = Icons.Outlined.LocalFireDepartment,
            valueDisplayFormatter = { unitFormatter.energy(it) },
        ),
    )

internal fun bodyReadingItems(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
    weightLabel: String,
    heightLabel: String,
    bodyFatLabel: String,
    leanMassLabel: String,
    bmrLabel: String,
    boneMassLabel: String,
    bodyWaterMassLabel: String,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
): List<BodyReadingItem> =
    buildList {
        state.weightEntries.forEach { entry ->
            val editable = entry.isOpenVitalsEntry && entry.id.isNotBlank()
            add(
                BodyReadingItem(
                    value = "$weightLabel · ${unitFormatter.weight(entry.weightKg).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = WeightColor,
                    onEdit = if (editable) {
                        { onEditBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                    } else {
                        null
                    },
                    onDelete = if (editable) {
                        { onDeleteBodyMeasurement(BodyMeasurementType.WEIGHT, entry.id) }
                    } else {
                        null
                    },
                )
            )
        }
        state.heightEntries.forEach { entry ->
            val editable = entry.isOpenVitalsEntry && entry.id.isNotBlank()
            add(
                BodyReadingItem(
                    value = "$heightLabel · ${unitFormatter.height(entry.heightCm).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = WeightColor,
                    onEdit = if (editable) {
                        { onEditBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                    } else {
                        null
                    },
                    onDelete = if (editable) {
                        { onDeleteBodyMeasurement(BodyMeasurementType.HEIGHT, entry.id) }
                    } else {
                        null
                    },
                )
            )
        }
        state.bodyFatEntries.forEach { entry ->
            val editable = entry.isOpenVitalsEntry && entry.id.isNotBlank()
            add(
                BodyReadingItem(
                    value = "$bodyFatLabel · ${unitFormatter.percent(entry.percent).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = BodyFatColor,
                    onEdit = if (editable) {
                        { onEditBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                    } else {
                        null
                    },
                    onDelete = if (editable) {
                        { onDeleteBodyMeasurement(BodyMeasurementType.BODY_FAT, entry.id) }
                    } else {
                        null
                    },
                )
            )
        }
        state.leanMassEntries.forEach { entry ->
            add(
                BodyReadingItem(
                    value = "$leanMassLabel · ${unitFormatter.bodyMass(entry.massKg).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = WeightColor,
                )
            )
        }
        state.bmrEntries.forEach { entry ->
            add(
                BodyReadingItem(
                    value = "$bmrLabel · ${unitFormatter.energy(entry.kcalPerDay).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = CaloriesColor,
                )
            )
        }
        state.boneMassEntries.forEach { entry ->
            add(
                BodyReadingItem(
                    value = "$boneMassLabel · ${unitFormatter.bodyMass(entry.massKg, decimals = 2).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = WeightColor,
                )
            )
        }
        state.bodyWaterMassEntries.forEach { entry ->
            add(
                BodyReadingItem(
                    value = "$bodyWaterMassLabel · ${unitFormatter.bodyMass(entry.massKg, decimals = 2).text}",
                    source = entry.source,
                    time = entry.time,
                    accentColor = WeightColor,
                )
            )
        }
    }

internal fun <T> dailyLatestValues(
    entries: List<T>,
    time: (T) -> Instant,
    value: (T) -> Double,
): List<PeriodChartValue> =
    entries
        .groupBy { time(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, dayEntries) ->
            PeriodChartValue(
                date = date,
                value = dayEntries.maxByOrNull(time)?.let(value) ?: 0.0,
            )
        }
        .sortedBy { it.date }

internal fun bmiHistoryValues(
    entries: List<WeightEntry>,
    heightCm: Double?,
): List<PeriodChartValue> {
    val heightMeters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return emptyList()
    return entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .mapNotNull { (date, dayEntries) ->
            dayEntries.maxByOrNull { it.time }?.let { entry ->
                PeriodChartValue(
                    date = date,
                    value = entry.weightKg / (heightMeters * heightMeters),
                )
            }
        }
        .sortedBy { it.date }
}

internal fun WeightEntry.editAction(
    type: BodyMeasurementType,
    onEditBodyMeasurement: (BodyMeasurementType, String) -> Unit,
): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditBodyMeasurement(type, id) }
    } else {
        null
    }

internal fun WeightEntry.deleteAction(
    type: BodyMeasurementType,
    onDeleteBodyMeasurement: (BodyMeasurementType, String) -> Unit,
): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onDeleteBodyMeasurement(type, id) }
    } else {
        null
    }

internal fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun WeightEntry.weightBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = weightKg,
    )

internal fun BodyFatEntry.bodyFatBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = percent,
    )

internal fun HeightEntry.heightBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = heightCm,
    )

internal fun LeanBodyMassEntry.leanMassBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = massKg,
    )

internal fun BmrEntry.bmrBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = kcalPerDay,
    )

internal fun BoneMassEntry.boneMassBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = massKg,
    )

internal fun BodyWaterMassEntry.bodyWaterMassBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = massKg,
    )

internal fun bmiBaselineValues(
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
