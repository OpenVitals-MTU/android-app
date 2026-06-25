package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.BloodPressureCategory
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.VitalContextInterpretation
import tech.mmarca.openvitals.domain.insights.VitalContextStatus
import tech.mmarca.openvitals.domain.insights.bloodPressureInterpretation
import tech.mmarca.openvitals.domain.insights.bodyTemperatureContext
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.oxygenSaturationContext
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.insights.respiratoryRateContext
import tech.mmarca.openvitals.domain.insights.restingHeartRateContext
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

internal fun LazyListScope.heartAggregateDataConfidence(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = trackedDates,
                sampleCount = sampleCount,
                valueKind = DataValueKind.AGGREGATED,
            ),
            accentColor = accentColor,
            modifier = metricModifier(),
        )
    }
}

internal fun <T> LazyListScope.heartRawDataConfidence(
    period: DatePeriod,
    entries: List<T>,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
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
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = accentColor,
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.bloodPressureContextCard(entry: BloodPressureEntry?) {
    val interpretation = entry
        ?.let { bloodPressureInterpretation(it.systolicMmHg, it.diastolicMmHg) }
        ?: return
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        val status = bloodPressureCategoryText(interpretation.category)
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_bp_title),
            status = status,
            body = if (interpretation.category == BloodPressureCategory.SEVERE_REFERENCE) {
                stringResource(R.string.interpretation_bp_severe_body)
            } else {
                stringResource(R.string.interpretation_bp_body, status)
            },
            source = stringResource(R.string.interpretation_bp_source),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.restingHeartRateContextCard(bpm: Long) {
    val interpretation = restingHeartRateContext(bpm) ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_resting_hr_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
    )
}

internal fun LazyListScope.oxygenSaturationContextCard(entry: SpO2Entry?) {
    val interpretation = entry?.let { oxygenSaturationContext(it.percent) } ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_oxygen_body,
        sourceRes = R.string.interpretation_oxygen_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
    )
}

internal fun LazyListScope.respiratoryRateContextCard(breathsPerMinute: Double) {
    val interpretation = respiratoryRateContext(breathsPerMinute) ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_respiratory_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
    )
}

internal fun LazyListScope.bodyTemperatureContextCard(entry: BodyTempEntry?) {
    val interpretation = entry?.let { bodyTemperatureContext(it.temperatureCelsius) } ?: return
    vitalContextCard(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_temperature_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
    )
}

internal fun LazyListScope.vitalContextCard(
    interpretation: VitalContextInterpretation,
    bodyRes: Int,
    sourceRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item { SectionHeader(stringResource(R.string.section_metric_context)) }
    item {
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_vital_title),
            status = vitalContextStatusText(interpretation.status),
            body = stringResource(bodyRes),
            source = stringResource(sourceRes),
            icon = icon,
            accentColor = accentColor,
            severity = interpretation.severity,
            modifier = metricModifier(),
        )
    }
}

@Composable
internal fun bloodPressureCategoryText(category: BloodPressureCategory): String =
    when (category) {
        BloodPressureCategory.NORMAL -> stringResource(R.string.interpretation_bp_normal)
        BloodPressureCategory.ELEVATED -> stringResource(R.string.interpretation_bp_elevated)
        BloodPressureCategory.STAGE_1 -> stringResource(R.string.interpretation_bp_stage_1)
        BloodPressureCategory.STAGE_2 -> stringResource(R.string.interpretation_bp_stage_2)
        BloodPressureCategory.SEVERE_REFERENCE -> stringResource(R.string.interpretation_bp_severe)
    }

@Composable
internal fun vitalContextStatusText(status: VitalContextStatus): String =
    when (status) {
        VitalContextStatus.WITHIN_REFERENCE -> stringResource(R.string.interpretation_vital_within)
        VitalContextStatus.BELOW_REFERENCE -> stringResource(R.string.interpretation_vital_below)
        VitalContextStatus.ABOVE_REFERENCE -> stringResource(R.string.interpretation_vital_above)
        VitalContextStatus.BELOW_TYPICAL_OXYGEN ->
            stringResource(R.string.interpretation_vital_oxygen_below_typical)
        VitalContextStatus.LOW_OXYGEN_REFERENCE -> stringResource(R.string.interpretation_vital_oxygen_low)
        VitalContextStatus.VERY_LOW_OXYGEN_REFERENCE ->
            stringResource(R.string.interpretation_vital_oxygen_very_low)
    }

internal fun LazyListScope.heartRateSampleStatistics(
    samples: List<HeartRateSample>,
    previousSamples: List<HeartRateSample>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = samples.map { it.beatsPerMinute }
    val previousValues = previousSamples.map { it.beatsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(values.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(values.minOrNull() ?: 0L),
        high = unitFormatter.heartRate(values.maxOrNull() ?: 0L),
        readings = samples.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

internal fun LazyListScope.heartRateSummaryStatistics(
    summaries: List<HeartRateSummary>,
    previousSummaries: List<HeartRateSummary>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(summaries.map { it.avgBpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(summaries.minOfOrNull { it.minBpm } ?: 0L),
        high = unitFormatter.heartRate(summaries.maxOfOrNull { it.maxBpm } ?: 0L),
        readings = summaries.size,
        comparison = previousSummaries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = summaries.map { summary -> summary.avgBpm }.average(),
                previousValue = it.map { summary -> summary.avgBpm }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = summaries.map { it.avgBpm }.average(),
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

internal fun LazyListScope.restingHeartRateStatistics(
    entries: List<DailyRestingHR>,
    previousEntries: List<DailyRestingHR>,
    baselineEntries: List<DailyRestingHR>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(entries.map { it.bpm }.average().roundToInt().toLong()),
        low = unitFormatter.heartRate(entries.minOfOrNull { it.bpm } ?: 0L),
        high = unitFormatter.heartRate(entries.maxOfOrNull { it.bpm } ?: 0L),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.bpm }.average(),
                previousValue = it.map { entry -> entry.bpm }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = entries.map { it.bpm }.average(),
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.bpm.toDouble()) },
    )
}

internal fun LazyListScope.hrvStatistics(
    entries: List<DailyHrv>,
    previousEntries: List<DailyHrv>,
    baselineEntries: List<DailyHrv>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.hrv(entries.map { it.rmssdMs }.average()),
        low = unitFormatter.hrv(entries.minOfOrNull { it.rmssdMs } ?: 0.0),
        high = unitFormatter.hrv(entries.maxOfOrNull { it.rmssdMs } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.rmssdMs }.average(),
                previousValue = it.map { entry -> entry.rmssdMs }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.hrv(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = entries.map { it.rmssdMs }.average(),
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.rmssdMs) },
    )
}

internal fun LazyListScope.bloodPressureStatistics(
    entries: List<BloodPressureEntry>,
    previousEntries: List<BloodPressureEntry>,
    baselineEntries: List<BloodPressureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val latest = entries.maxByOrNull { it.time }
        val average = unitFormatter.bloodPressure(
            entries.map { it.systolicMmHg }.average().roundToInt(),
            entries.map { it.diastolicMmHg }.average().roundToInt(),
        )
        val highest = entries
            .maxWithOrNull(compareBy<BloodPressureEntry> { it.systolicMmHg }.thenBy { it.diastolicMmHg })
            ?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg) }
            ?: unitFormatter.bloodPressure(0, 0)
        val previousAverageSystolic = previousEntries.takeIf { it.isNotEmpty() }
            ?.map { it.systolicMmHg }
            ?.average()

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_latest),
                    value = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).value }.orEmpty(),
                    unit = latest?.let { unitFormatter.bloodPressure(it.systolicMmHg, it.diastolicMmHg).unit }.orEmpty(),
                    icon = Icons.Outlined.Favorite,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = Icons.Outlined.Star,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_highest),
                    value = highest.value,
                    unit = highest.unit,
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = VitalsColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_readings),
                    value = unitFormatter.count(entries.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = VitalsColor,
                ),
            ) + previousAverageSystolic?.let {
                listOf(
                    previousPeriodInsightStat(
                        comparison = periodComparison(
                            currentValue = entries.map { entry -> entry.systolicMmHg }.average(),
                            previousValue = it,
                        ),
                        selectedRange = selectedRange,
                        unitFormatter = unitFormatter,
                        valueFormatter = { value -> DisplayValue(unitFormatter.count(value.roundToInt()), "mmHg") },
                        accentColor = VitalsColor,
                    )
                )
            }.orEmpty() + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = entries.map { entry -> entry.systolicMmHg }.average(),
                    values = baselineEntries.map { it.systolicBaselineValue() },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { value -> DisplayValue(unitFormatter.count(value.roundToInt()), "mmHg") },
                accentColor = VitalsColor,
            ),
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.spO2Statistics(
    entries: List<SpO2Entry>,
    previousEntries: List<SpO2Entry>,
    baselineEntries: List<SpO2Entry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.percent(entries.map { it.percent }.average()),
        low = unitFormatter.percent(entries.minOfOrNull { it.percent } ?: 0.0),
        high = unitFormatter.percent(entries.maxOfOrNull { it.percent } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.percent }.average(),
                previousValue = it.map { entry -> entry.percent }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.percent(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
        period = period,
        baselineCurrentValue = entries.map { it.percent }.average(),
        baselineValues = baselineEntries.map { it.spO2BaselineValue() },
    )
}

internal fun LazyListScope.vo2MaxStatistics(
    entries: List<Vo2MaxEntry>,
    previousEntries: List<Vo2MaxEntry>,
    baselineEntries: List<Vo2MaxEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.vo2Max(entries.map { it.vo2MaxMlPerKgPerMin }.average()),
        low = unitFormatter.vo2Max(entries.minOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        high = unitFormatter.vo2Max(entries.maxOfOrNull { it.vo2MaxMlPerKgPerMin } ?: 0.0),
        readings = entries.size,
        comparison = previousEntries.takeIf { it.isNotEmpty() }?.let {
            periodComparison(
                currentValue = entries.map { entry -> entry.vo2MaxMlPerKgPerMin }.average(),
                previousValue = it.map { entry -> entry.vo2MaxMlPerKgPerMin }.average(),
            )
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.vo2Max(it) },
        icon = Icons.Outlined.Speed,
        accentColor = vo2Color,
        period = period,
        baselineCurrentValue = entries.map { it.vo2MaxMlPerKgPerMin }.average(),
        baselineValues = baselineEntries.map { it.vo2BaselineValue() },
    )
}

internal fun LazyListScope.respiratoryRateStatistics(
    entries: List<RespiratoryRateEntry>,
    previousEntries: List<RespiratoryRateEntry>,
    baselineEntries: List<RespiratoryRateEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.breathsPerMinute }
    val previousValues = previousEntries.map { it.breathsPerMinute }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.respiratoryRate(values.average()),
        low = unitFormatter.respiratoryRate(values.minOrNull() ?: 0.0),
        high = unitFormatter.respiratoryRate(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.respiratoryRate(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.respiratoryRateBaselineValue() },
    )
}

internal fun LazyListScope.bodyTemperatureStatistics(
    entries: List<BodyTempEntry>,
    previousEntries: List<BodyTempEntry>,
    baselineEntries: List<BodyTempEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.temperatureCelsius }
    val previousValues = previousEntries.map { it.temperatureCelsius }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperature(values.average()),
        low = unitFormatter.temperature(values.minOrNull() ?: 0.0),
        high = unitFormatter.temperature(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperature(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.bodyTemperatureBaselineValue() },
    )
}

internal fun LazyListScope.bloodGlucoseStatistics(
    entries: List<BloodGlucoseEntry>,
    previousEntries: List<BloodGlucoseEntry>,
    baselineEntries: List<BloodGlucoseEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.millimolesPerLiter }
    val previousValues = previousEntries.map { it.millimolesPerLiter }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.bloodGlucose(values.average()),
        low = unitFormatter.bloodGlucose(values.minOrNull() ?: 0.0),
        high = unitFormatter.bloodGlucose(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.bloodGlucose(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = glucoseColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.map { it.bloodGlucoseBaselineValue() },
    )
}

internal fun LazyListScope.skinTemperatureStatistics(
    entries: List<SkinTemperatureEntry>,
    previousEntries: List<SkinTemperatureEntry>,
    baselineEntries: List<SkinTemperatureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.mapNotNull { it.averageDeltaCelsius }
    if (values.isEmpty()) return
    val previousValues = previousEntries.mapNotNull { it.averageDeltaCelsius }
    heartNumericStatistics(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperatureDelta(values.average()),
        low = unitFormatter.temperatureDelta(values.minOrNull() ?: 0.0),
        high = unitFormatter.temperatureDelta(values.maxOrNull() ?: 0.0),
        readings = entries.size,
        comparison = previousValues.takeIf { it.isNotEmpty() }?.let {
            periodComparison(values.average(), it.average())
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperatureDelta(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = values.average(),
        baselineValues = baselineEntries.mapNotNull { it.skinTemperatureBaselineValue() },
    )
}

internal fun LazyListScope.heartNumericStatistics(
    unitFormatter: UnitFormatter,
    average: DisplayValue,
    low: DisplayValue,
    high: DisplayValue,
    readings: Int,
    comparison: PeriodComparison? = null,
    selectedRange: TimeRange,
    comparisonValueFormatter: @Composable (Double) -> DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    countTitleRes: Int = R.string.stat_readings,
    countUnitRes: Int? = null,
    period: DatePeriod? = null,
    baselineCurrentValue: Double? = null,
    baselineValues: List<BaselineValue> = emptyList(),
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_average),
                    value = average.value,
                    unit = average.unit,
                    icon = icon,
                    accentColor = accentColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_lowest),
                    value = low.value,
                    unit = low.unit,
                    icon = Icons.Outlined.Star,
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
                    title = stringResource(countTitleRes),
                    value = unitFormatter.count(readings),
                    unit = countUnitRes?.let { stringResource(it) }.orEmpty(),
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
            }.orEmpty() + if (period != null && baselineCurrentValue != null) {
                personalBaselineInsightStats(
                    insight = personalBaselineInsight(
                        currentValue = baselineCurrentValue,
                        values = baselineValues,
                        referenceDate = period.start.minusDays(1),
                    ),
                    unitFormatter = unitFormatter,
                    valueFormatter = comparisonValueFormatter,
                    accentColor = accentColor,
                )
            } else {
                emptyList()
            },
            modifier = metricModifier(),
        )
    }
}

internal fun LazyListScope.noHeartMetricData(
    titleRes: Int,
    messageRes: Int,
    icon: ImageVector,
    accentColor: Color,
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

internal fun <T> LazyListScope.heartEntryRows(
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
    editable: (T) -> Boolean = { false },
    onEdit: ((T) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
) {
    if (entries.isEmpty()) return

    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = entries.sortedByDescending(time),
        ) { entry, rowModifier ->
            VitalsReadingRow(
                label = value(entry),
                source = source(entry),
                time = time(entry).atZone(ZoneId.systemDefault()),
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

internal fun <T> LazyListScope.heartDailyEntries(
    entries: List<T>,
    date: (T) -> LocalDate,
    value: (T) -> String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    titleDate: LocalDate? = null,
) {
    if (entries.isEmpty()) return

    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = entries.sortedByDescending(date),
        ) { entry, rowModifier ->
            HeartDailyEntryRow(
                date = date(entry),
                value = value(entry),
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                accentColor = accentColor,
                modifier = rowModifier,
            )
        }
    }
}

@Composable
internal fun HeartDailyEntryRow(
    date: LocalDate,
    value: String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier,

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(date),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
            )
        }
    }
}

internal fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal fun BloodPressureEntry.systolicBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = systolicMmHg.toDouble(),
    )

internal fun SpO2Entry.spO2BaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = percent,
    )

internal fun RespiratoryRateEntry.respiratoryRateBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = breathsPerMinute,
    )

internal fun BodyTempEntry.bodyTemperatureBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = temperatureCelsius,
    )

internal fun Vo2MaxEntry.vo2BaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = vo2MaxMlPerKgPerMin,
    )

internal fun BloodGlucoseEntry.bloodGlucoseBaselineValue(): BaselineValue =
    BaselineValue(
        date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
        value = millimolesPerLiter,
    )

internal fun SkinTemperatureEntry.skinTemperatureBaselineValue(): BaselineValue? =
    averageDeltaCelsius?.let { delta ->
        BaselineValue(
            date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
            value = delta,
        )
    }

internal fun SkinTemperatureEntry.skinTemperatureValue(unitFormatter: UnitFormatter): String =
    averageDeltaCelsius
        ?.let { unitFormatter.temperatureDelta(it).text }
        ?: baselineCelsius?.let { unitFormatter.temperature(it).text }
        ?: ""
