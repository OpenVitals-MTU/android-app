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
import tech.mmarca.openvitals.core.stats.averageOrNull
import tech.mmarca.openvitals.core.stats.timeBucketedAverageOrNull
import tech.mmarca.openvitals.features.vitals.respiratoryRateAverage
import tech.mmarca.openvitals.features.vitals.respiratoryRateBuckets
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
import tech.mmarca.openvitals.features.vitals.glucoseColor
import tech.mmarca.openvitals.features.vitals.oxygenColor
import tech.mmarca.openvitals.features.vitals.respiratoryColor
import tech.mmarca.openvitals.features.vitals.temperatureColor
import tech.mmarca.openvitals.features.vitals.VitalsReadingRow
import tech.mmarca.openvitals.features.vitals.vo2Color
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.MetricInterpretationCard
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
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

@Composable
internal fun HeartAggregateDataConfidenceContent(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
) {
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

internal fun LazyListScope.heartAggregateDataConfidence(
    period: DatePeriod,
    trackedDates: Collection<LocalDate>,
    sampleCount: Int,
    accentColor: Color,
) {
    if (period.start == period.end) return

    item {
        HeartAggregateDataConfidenceContent(
            period = period,
            trackedDates = trackedDates,
            sampleCount = sampleCount,
            accentColor = accentColor,
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
        HeartRawDataConfidenceContent(
            period = period,
            entries = entries,
            source = source,
            time = time,
            accentColor = accentColor,
        )
    }
}

@Composable
internal fun <T> HeartRawDataConfidenceContent(
    period: DatePeriod,
    entries: List<T>,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    accentColor: Color,
) {
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

@Composable
internal fun BloodPressureContextCardContent(entry: BloodPressureEntry?) {
    val interpretation = entry
        ?.let { bloodPressureInterpretation(it.systolicMmHg, it.diastolicMmHg) }
        ?: return
    Column(modifier = metricModifier()) {
        SectionHeader(stringResource(R.string.section_metric_context))
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
        )
    }
}

@Composable
internal fun RestingHeartRateContextCardContent(bpm: Long) {
    val interpretation = restingHeartRateContext(bpm) ?: return
    VitalContextCardContent(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_resting_hr_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
    )
}

@Composable
internal fun OxygenSaturationContextCardContent(entry: SpO2Entry?) {
    val interpretation = entry?.let { oxygenSaturationContext(it.percent) } ?: return
    VitalContextCardContent(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_oxygen_body,
        sourceRes = R.string.interpretation_oxygen_source,
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
    )
}

@Composable
internal fun RespiratoryRateContextCardContent(breathsPerMinute: Double) {
    val interpretation = respiratoryRateContext(breathsPerMinute) ?: return
    VitalContextCardContent(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_respiratory_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
    )
}

@Composable
internal fun BodyTemperatureContextCardContent(entry: BodyTempEntry?) {
    val interpretation = entry?.let { bodyTemperatureContext(it.temperatureCelsius) } ?: return
    VitalContextCardContent(
        interpretation = interpretation,
        bodyRes = R.string.interpretation_vital_temperature_body,
        sourceRes = R.string.interpretation_vital_source,
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
    )
}

@Composable
internal fun VitalContextCardContent(
    interpretation: VitalContextInterpretation,
    bodyRes: Int,
    sourceRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    Column(modifier = metricModifier()) {
        SectionHeader(stringResource(R.string.section_metric_context))
        MetricInterpretationCard(
            title = stringResource(R.string.interpretation_vital_title),
            status = vitalContextStatusText(interpretation.status),
            body = stringResource(bodyRes),
            source = stringResource(sourceRes),
            icon = icon,
            accentColor = accentColor,
            severity = interpretation.severity,
        )
    }
}

internal fun LazyListScope.bloodPressureContextCard(entry: BloodPressureEntry?) {
    if (entry?.let { bloodPressureInterpretation(it.systolicMmHg, it.diastolicMmHg) } == null) return
    item { BloodPressureContextCardContent(entry) }
}

internal fun LazyListScope.restingHeartRateContextCard(bpm: Long) {
    if (restingHeartRateContext(bpm) == null) return
    item { RestingHeartRateContextCardContent(bpm) }
}

internal fun LazyListScope.oxygenSaturationContextCard(entry: SpO2Entry?) {
    if (entry?.let { oxygenSaturationContext(it.percent) } == null) return
    item { OxygenSaturationContextCardContent(entry) }
}

internal fun LazyListScope.respiratoryRateContextCard(breathsPerMinute: Double) {
    if (respiratoryRateContext(breathsPerMinute) == null) return
    item { RespiratoryRateContextCardContent(breathsPerMinute) }
}

internal fun LazyListScope.bodyTemperatureContextCard(entry: BodyTempEntry?) {
    if (entry?.let { bodyTemperatureContext(it.temperatureCelsius) } == null) return
    item { BodyTemperatureContextCardContent(entry) }
}

internal fun LazyListScope.vitalContextCard(
    interpretation: VitalContextInterpretation,
    bodyRes: Int,
    sourceRes: Int,
    icon: ImageVector,
    accentColor: Color,
) {
    item {
        VitalContextCardContent(
            interpretation = interpretation,
            bodyRes = bodyRes,
            sourceRes = sourceRes,
            icon = icon,
            accentColor = accentColor,
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

/**
 * Day-view heart rate statistics, hoisted for JVM tests. The mean is
 * minute-bucketed, so a 1 Hz workout does not outvote the background series.
 */
internal data class HeartRateSampleStats(
    val average: Double,
    val low: Long,
    val high: Long,
    val readings: Int,
)

internal fun heartRateSampleAverage(samples: List<HeartRateSample>): Double? =
    samples.timeBucketedAverageOrNull(time = { it.time }, value = { it.beatsPerMinute.toDouble() })

internal fun heartRateSampleStats(samples: List<HeartRateSample>): HeartRateSampleStats? {
    val average = heartRateSampleAverage(samples) ?: return null
    val low = samples.minOfOrNull { it.beatsPerMinute } ?: return null
    val high = samples.maxOfOrNull { it.beatsPerMinute } ?: return null
    return HeartRateSampleStats(average = average, low = low, high = high, readings = samples.size)
}

@Composable
internal fun HeartRateSampleStatisticsContent(
    samples: List<HeartRateSample>,
    previousSamples: List<HeartRateSample>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val stats = heartRateSampleStats(samples) ?: return
    val average = stats.average
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(average.roundToInt().toLong()),
        low = unitFormatter.heartRate(stats.low),
        high = unitFormatter.heartRate(stats.high),
        readings = stats.readings,
        comparison = heartRateSampleAverage(previousSamples)?.let { periodComparison(average, it) },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

@Composable
internal fun HeartRateSummaryStatisticsContent(
    summaries: List<HeartRateSummary>,
    previousSummaries: List<HeartRateSummary>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val average = summaries.map { it.avgBpm }.averageOrNull() ?: return
    val low = summaries.minOfOrNull { it.minBpm } ?: return
    val high = summaries.maxOfOrNull { it.maxBpm } ?: return
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(average.roundToInt().toLong()),
        low = unitFormatter.heartRate(low),
        high = unitFormatter.heartRate(high),
        readings = summaries.size,
        comparison = previousSummaries.map { it.avgBpm }.averageOrNull()?.let {
            periodComparison(currentValue = average, previousValue = it)
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineSummaries.map { BaselineValue(it.date, it.avgBpm.toDouble()) },
    )
}

@Composable
internal fun RestingHeartRateStatisticsContent(
    entries: List<DailyRestingHR>,
    previousEntries: List<DailyRestingHR>,
    baselineEntries: List<DailyRestingHR>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val average = entries.map { it.bpm }.averageOrNull() ?: return
    val low = entries.minOfOrNull { it.bpm } ?: return
    val high = entries.maxOfOrNull { it.bpm } ?: return
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.heartRate(average.roundToInt().toLong()),
        low = unitFormatter.heartRate(low),
        high = unitFormatter.heartRate(high),
        readings = entries.size,
        comparison = previousEntries.map { it.bpm }.averageOrNull()?.let {
            periodComparison(currentValue = average, previousValue = it)
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.heartRate(it.roundToInt().toLong()) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.bpm.toDouble()) },
    )
}

@Composable
internal fun HrvStatisticsContent(
    entries: List<DailyHrv>,
    previousEntries: List<DailyHrv>,
    baselineEntries: List<DailyHrv>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val average = entries.map { it.rmssdMs }.averageOrNull() ?: return
    val low = entries.minOfOrNull { it.rmssdMs } ?: return
    val high = entries.maxOfOrNull { it.rmssdMs } ?: return
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.hrv(average),
        low = unitFormatter.hrv(low),
        high = unitFormatter.hrv(high),
        readings = entries.size,
        comparison = previousEntries.map { it.rmssdMs }.averageOrNull()?.let {
            periodComparison(currentValue = average, previousValue = it)
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.hrv(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        countTitleRes = R.string.metric_logged_days,
        countUnitRes = R.string.unit_days,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { BaselineValue(it.date, it.rmssdMs) },
    )
}

/** Blood-pressure statistics, hoisted for JVM tests. */
internal data class BloodPressureStats(
    val latest: BloodPressureEntry,
    val highest: BloodPressureEntry,
    val averageSystolic: Double,
    val averageDiastolic: Double,
    val readings: Int,
)

internal fun bloodPressureStats(entries: List<BloodPressureEntry>): BloodPressureStats? {
    // An empty list averages to NaN, and roundToInt() throws on it.
    val averageSystolic = entries.map { it.systolicMmHg }.averageOrNull() ?: return null
    val averageDiastolic = entries.map { it.diastolicMmHg }.averageOrNull() ?: return null
    val highestEntry = entries
        .maxWithOrNull(compareBy<BloodPressureEntry> { it.systolicMmHg }.thenBy { it.diastolicMmHg })
        ?: return null
    val latest = entries.maxByOrNull { it.time } ?: return null
    return BloodPressureStats(
        latest = latest,
        highest = highestEntry,
        averageSystolic = averageSystolic,
        averageDiastolic = averageDiastolic,
        readings = entries.size,
    )
}

/** Average/low/high over a metric's readings; `readings` counts every entry passed. */
internal data class VitalReadingStats(
    val average: Double,
    val low: Double,
    val high: Double,
    val readings: Int,
)

private fun vitalReadingStats(
    values: List<Double>,
    readings: Int,
    average: Double? = values.averageOrNull(),
): VitalReadingStats? {
    if (average == null) return null
    val low = values.minOrNull() ?: return null
    val high = values.maxOrNull() ?: return null
    return VitalReadingStats(average = average, low = low, high = high, readings = readings)
}

internal fun spO2Stats(entries: List<SpO2Entry>): VitalReadingStats? =
    vitalReadingStats(
        values = entries.map { it.percent },
        readings = entries.size,
        // Minute-bucketed, so overnight monitoring does not drown spot checks.
        average = entries.timeBucketedAverageOrNull(time = { it.time }, value = { it.percent }),
    )

internal fun skinTemperatureStats(entries: List<SkinTemperatureEntry>): VitalReadingStats? =
    // readings counts every entry, including the delta-less ones the chart drops.
    vitalReadingStats(entries.mapNotNull { it.averageDeltaCelsius }, entries.size)

@Composable
internal fun BloodPressureStatisticsContent(
    entries: List<BloodPressureEntry>,
    previousEntries: List<BloodPressureEntry>,
    baselineEntries: List<BloodPressureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val stats = bloodPressureStats(entries) ?: return
    val averageSystolic = stats.averageSystolic
    val averageDiastolic = stats.averageDiastolic
    val highestEntry = stats.highest
    Column(modifier = metricModifier()) {
        SectionHeader(stringResource(R.string.section_statistics))
        val latest: BloodPressureEntry? = stats.latest
        val average = unitFormatter.bloodPressure(
            averageSystolic.roundToInt(),
            averageDiastolic.roundToInt(),
        )
        val highest = unitFormatter.bloodPressure(highestEntry.systolicMmHg, highestEntry.diastolicMmHg)
        val previousAverageSystolic = previousEntries.map { it.systolicMmHg }.averageOrNull()

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
                            currentValue = averageSystolic,
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
                    currentValue = averageSystolic,
                    values = baselineEntries.map { it.systolicBaselineValue() },
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { value -> DisplayValue(unitFormatter.count(value.roundToInt()), "mmHg") },
                accentColor = VitalsColor,
            ),
        )
    }
}

@Composable
internal fun SpO2StatisticsContent(
    entries: List<SpO2Entry>,
    previousEntries: List<SpO2Entry>,
    baselineEntries: List<SpO2Entry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val stats = spO2Stats(entries) ?: return
    val average = stats.average
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.percent(average),
        low = unitFormatter.percent(stats.low),
        high = unitFormatter.percent(stats.high),
        readings = stats.readings,
        comparison = previousEntries.map { it.percent }.averageOrNull()?.let {
            periodComparison(currentValue = average, previousValue = it)
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.percent(it) },
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = oxygenColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { it.spO2BaselineValue() },
    )
}

@Composable
internal fun Vo2MaxStatisticsContent(
    entries: List<Vo2MaxEntry>,
    previousEntries: List<Vo2MaxEntry>,
    baselineEntries: List<Vo2MaxEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.vo2MaxMlPerKgPerMin }
    val average = values.averageOrNull() ?: return
    val low = values.minOrNull() ?: return
    val high = values.maxOrNull() ?: return
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.vo2Max(average),
        low = unitFormatter.vo2Max(low),
        high = unitFormatter.vo2Max(high),
        readings = entries.size,
        comparison = previousEntries.map { it.vo2MaxMlPerKgPerMin }.averageOrNull()?.let {
            periodComparison(currentValue = average, previousValue = it)
        },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.vo2Max(it) },
        icon = Icons.Outlined.Speed,
        accentColor = vo2Color,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { it.vo2BaselineValue() },
    )
}

@Composable
internal fun RespiratoryRateStatisticsContent(
    entries: List<RespiratoryRateEntry>,
    previousEntries: List<RespiratoryRateEntry>,
    baselineEntries: List<RespiratoryRateEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.breathsPerMinute }
    val low = values.minOrNull() ?: return
    val high = values.maxOrNull() ?: return
    // One average per screen: the mean of the daily points, as the chart summarises.
    val average = respiratoryRateAverage(respiratoryRateBuckets(entries, selectedRange, period))
        .takeIf { it > 0.0 } ?: return
    val previousAverage = respiratoryRateAverage(
        respiratoryRateBuckets(previousEntries, selectedRange, period)
    ).takeIf { it > 0.0 }
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.respiratoryRate(average),
        low = unitFormatter.respiratoryRate(low),
        high = unitFormatter.respiratoryRate(high),
        readings = entries.size,
        comparison = previousAverage?.let { periodComparison(average, it) },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.respiratoryRate(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = respiratoryColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { it.respiratoryRateBaselineValue() },
    )
}

@Composable
internal fun BodyTemperatureStatisticsContent(
    entries: List<BodyTempEntry>,
    previousEntries: List<BodyTempEntry>,
    baselineEntries: List<BodyTempEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.temperatureCelsius }
    val average = values.averageOrNull() ?: return
    val low = values.minOrNull() ?: return
    val high = values.maxOrNull() ?: return
    val previousValues = previousEntries.map { it.temperatureCelsius }
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperature(average),
        low = unitFormatter.temperature(low),
        high = unitFormatter.temperature(high),
        readings = entries.size,
        comparison = previousValues.averageOrNull()?.let { periodComparison(average, it) },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperature(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { it.bodyTemperatureBaselineValue() },
    )
}

@Composable
internal fun BloodGlucoseStatisticsContent(
    entries: List<BloodGlucoseEntry>,
    previousEntries: List<BloodGlucoseEntry>,
    baselineEntries: List<BloodGlucoseEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val values = entries.map { it.millimolesPerLiter }
    val average = values.averageOrNull() ?: return
    val low = values.minOrNull() ?: return
    val high = values.maxOrNull() ?: return
    val previousValues = previousEntries.map { it.millimolesPerLiter }
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.bloodGlucose(average),
        low = unitFormatter.bloodGlucose(low),
        high = unitFormatter.bloodGlucose(high),
        readings = entries.size,
        comparison = previousValues.averageOrNull()?.let { periodComparison(average, it) },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.bloodGlucose(it) },
        icon = Icons.Outlined.Favorite,
        accentColor = glucoseColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.map { it.bloodGlucoseBaselineValue() },
    )
}

@Composable
internal fun SkinTemperatureStatisticsContent(
    entries: List<SkinTemperatureEntry>,
    previousEntries: List<SkinTemperatureEntry>,
    baselineEntries: List<SkinTemperatureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    val stats = skinTemperatureStats(entries) ?: return
    val average = stats.average
    val previousValues = previousEntries.mapNotNull { it.averageDeltaCelsius }
    HeartNumericStatisticsContent(
        unitFormatter = unitFormatter,
        average = unitFormatter.temperatureDelta(average),
        low = unitFormatter.temperatureDelta(stats.low),
        high = unitFormatter.temperatureDelta(stats.high),
        readings = stats.readings,
        comparison = previousValues.averageOrNull()?.let { periodComparison(average, it) },
        selectedRange = selectedRange,
        comparisonValueFormatter = { unitFormatter.temperatureDelta(it) },
        icon = Icons.Outlined.DeviceThermostat,
        accentColor = temperatureColor,
        period = period,
        baselineCurrentValue = average,
        baselineValues = baselineEntries.mapNotNull { it.skinTemperatureBaselineValue() },
    )
}

internal fun LazyListScope.heartRateSampleStatistics(
    samples: List<HeartRateSample>,
    previousSamples: List<HeartRateSample>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        HeartRateSampleStatisticsContent(
            samples = samples,
            previousSamples = previousSamples,
            baselineSummaries = baselineSummaries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.heartRateSummaryStatistics(
    summaries: List<HeartRateSummary>,
    previousSummaries: List<HeartRateSummary>,
    baselineSummaries: List<HeartRateSummary>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        HeartRateSummaryStatisticsContent(
            summaries = summaries,
            previousSummaries = previousSummaries,
            baselineSummaries = baselineSummaries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.restingHeartRateStatistics(
    entries: List<DailyRestingHR>,
    previousEntries: List<DailyRestingHR>,
    baselineEntries: List<DailyRestingHR>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        RestingHeartRateStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.hrvStatistics(
    entries: List<DailyHrv>,
    previousEntries: List<DailyHrv>,
    baselineEntries: List<DailyHrv>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        HrvStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.bloodPressureStatistics(
    entries: List<BloodPressureEntry>,
    previousEntries: List<BloodPressureEntry>,
    baselineEntries: List<BloodPressureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        BloodPressureStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
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
    item {
        SpO2StatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.vo2MaxStatistics(
    entries: List<Vo2MaxEntry>,
    previousEntries: List<Vo2MaxEntry>,
    baselineEntries: List<Vo2MaxEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        Vo2MaxStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.respiratoryRateStatistics(
    entries: List<RespiratoryRateEntry>,
    previousEntries: List<RespiratoryRateEntry>,
    baselineEntries: List<RespiratoryRateEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        RespiratoryRateStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.bodyTemperatureStatistics(
    entries: List<BodyTempEntry>,
    previousEntries: List<BodyTempEntry>,
    baselineEntries: List<BodyTempEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        BodyTemperatureStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.bloodGlucoseStatistics(
    entries: List<BloodGlucoseEntry>,
    previousEntries: List<BloodGlucoseEntry>,
    baselineEntries: List<BloodGlucoseEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        BloodGlucoseStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
}

internal fun LazyListScope.skinTemperatureStatistics(
    entries: List<SkinTemperatureEntry>,
    previousEntries: List<SkinTemperatureEntry>,
    baselineEntries: List<SkinTemperatureEntry>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
) {
    item {
        SkinTemperatureStatisticsContent(
            entries = entries,
            previousEntries = previousEntries,
            baselineEntries = baselineEntries,
            period = period,
            selectedRange = selectedRange,
            unitFormatter = unitFormatter,
        )
    }
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
    item {
        HeartNumericStatisticsContent(
            unitFormatter = unitFormatter,
            average = average,
            low = low,
            high = high,
            readings = readings,
            comparison = comparison,
            selectedRange = selectedRange,
            comparisonValueFormatter = comparisonValueFormatter,
            icon = icon,
            accentColor = accentColor,
            countTitleRes = countTitleRes,
            countUnitRes = countUnitRes,
            period = period,
            baselineCurrentValue = baselineCurrentValue,
            baselineValues = baselineValues,
        )
    }
}

@Composable
internal fun HeartNumericStatisticsContent(
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
    Column(modifier = metricModifier()) {
        SectionHeader(stringResource(R.string.section_statistics))
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
        HeartEntryListContent(
            entries = entries,
            value = value,
            source = source,
            time = time,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            titleDate = titleDate,
            editable = editable,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

@Composable
internal fun <T> HeartEntryListContent(
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
        HeartDailyEntryListContent(
            entries = entries,
            date = date,
            value = value,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            accentColor = accentColor,
            titleDate = titleDate,
        )
    }
}

@Composable
internal fun <T> HeartDailyEntryListContent(
    entries: List<T>,
    date: (T) -> LocalDate,
    value: (T) -> String,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    accentColor: Color,
    titleDate: LocalDate? = null,
) {
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
