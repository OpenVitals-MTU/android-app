package tech.mmarca.openvitals.features.body

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor

/**
 * Lean mass, BMR, bone mass and body water: one measured value per reading and no
 * derived context, so they share one layout. A reading written here can be edited.
 */
internal fun LazyListScope.bodyCompositionMetricContent(
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
    val spec = bodyCompositionMetricSpec(metric, state, unitFormatter)
    val metricData = state.metricDataFor(metric, unitFormatter)
    val readingEntries: @Composable (Boolean) -> Unit = { selectedOnly ->
        bodyMetricReadingEntriesContent(
            entries = spec.readings,
            selectedDate = chartDaySelection.selectedDate,
            value = { spec.format(it.value).text },
            source = { it.source },
            time = { it.time },
            accentColor = spec.accentColor,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            selectedOnly = selectedOnly,
            editable = { it.editable },
            onEdit = { onEditBodyMeasurement(spec.type, it.id) },
            onDelete = { onDeleteBodyMeasurement(spec.type, it.id) },
        )
    }
    singleBodyMetricContent(
        state = state,
        period = period,
        selectedDate = chartDaySelection.selectedDate,
        sectionContext = sectionContext,
        titleRes = spec.titleRes,
        value = spec.latest?.let(spec.format),
        comparison = spec.previousLatest?.let { previous ->
            periodComparison(currentValue = spec.latest ?: 0.0, previousValue = previous)
        },
        comparisonValueFormatter = { spec.format(it) },
        icon = spec.icon,
        accentColor = spec.accentColor,
        unitFormatter = unitFormatter,
        selectedRange = state.selectedRange,
        entryCount = spec.readings.size,
        baselineCurrentValue = spec.latest,
        baselineValues = spec.baselineValues,
        periodChart = if (metricData.hasTrackedValues) {
            {
                BodyPeriodMetricChart(
                    metricData = metricData,
                    selectedRange = state.selectedRange,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                    modifier = metricModifier(),
                )
            }
        } else {
            null
        },
        dataConfidence = {
            BodyEntryDataConfidenceContent(
                period = period,
                entries = spec.readings,
                source = { it.source },
                time = { it.time },
                accentColor = spec.accentColor,
            )
        },
        selectedDayEntries = { readingEntries(true) },
        entries = { readingEntries(false) },
    )
}

/** The fields every composition entry type has, under one name. */
internal data class BodyCompositionReading(
    val id: String,
    val time: Instant,
    val value: Double,
    val source: String,
    val isOpenVitalsEntry: Boolean,
) {
    val editable: Boolean
        get() = isOpenVitalsEntry && id.isNotBlank()
}

private class BodyCompositionMetricSpec(
    val type: BodyMeasurementType,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val latest: Double?,
    val previousLatest: Double?,
    val readings: List<BodyCompositionReading>,
    val baselineValues: List<BaselineValue>,
    val format: (Double) -> DisplayValue,
)

private fun bodyCompositionMetricSpec(
    metric: BodyMetric,
    state: BodyUiState,
    unitFormatter: UnitFormatter,
): BodyCompositionMetricSpec {
    val summary = state.summary
    return when (metric) {
        BodyMetric.LEAN_MASS -> BodyCompositionMetricSpec(
            type = BodyMeasurementType.LEAN_MASS,
            titleRes = R.string.metric_lean_mass,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            latest = summary.latestLeanMassKg,
            previousLatest = summary.previousLatestLeanMassKg,
            readings = state.leanMassEntries.map {
                BodyCompositionReading(it.id, it.time, it.massKg, it.source, it.isOpenVitalsEntry)
            },
            baselineValues = state.baselineLeanMassEntries.map { it.leanMassBaselineValue() },
            format = { unitFormatter.bodyMass(it) },
        )
        BodyMetric.BMR -> BodyCompositionMetricSpec(
            type = BodyMeasurementType.BMR,
            titleRes = R.string.metric_bmr,
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            latest = summary.latestBmrKcal,
            previousLatest = summary.previousLatestBmrKcal,
            readings = state.bmrEntries.map {
                BodyCompositionReading(it.id, it.time, it.kcalPerDay, it.source, it.isOpenVitalsEntry)
            },
            baselineValues = state.baselineBmrEntries.map { it.bmrBaselineValue() },
            format = { unitFormatter.energy(it) },
        )
        BodyMetric.BONE_MASS -> BodyCompositionMetricSpec(
            type = BodyMeasurementType.BONE_MASS,
            titleRes = R.string.metric_bone_mass,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            latest = summary.latestBoneMassKg,
            previousLatest = summary.previousLatestBoneMassKg,
            readings = state.boneMassEntries.map {
                BodyCompositionReading(it.id, it.time, it.massKg, it.source, it.isOpenVitalsEntry)
            },
            baselineValues = state.baselineBoneMassEntries.map { it.boneMassBaselineValue() },
            format = { unitFormatter.bodyMass(it, decimals = 2) },
        )
        BodyMetric.BODY_WATER_MASS -> BodyCompositionMetricSpec(
            type = BodyMeasurementType.BODY_WATER_MASS,
            titleRes = R.string.metric_body_water_mass,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            latest = summary.latestBodyWaterMassKg,
            previousLatest = summary.previousLatestBodyWaterMassKg,
            readings = state.bodyWaterMassEntries.map {
                BodyCompositionReading(it.id, it.time, it.massKg, it.source, it.isOpenVitalsEntry)
            },
            baselineValues = state.baselineBodyWaterMassEntries.map { it.bodyWaterMassBaselineValue() },
            format = { unitFormatter.bodyMass(it, decimals = 2) },
        )
        else -> error("$metric has its own layout")
    }
}
