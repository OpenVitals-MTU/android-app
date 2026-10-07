package tech.mmarca.openvitals.features.vitals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.stats.averageOrNull
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.features.heart.VitalReadingStats
import tech.mmarca.openvitals.features.heart.metricModifier
import tech.mmarca.openvitals.ui.components.ChartGuideLine
import tech.mmarca.openvitals.ui.components.ComparisonDisplayStyle
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import java.time.ZoneId
import kotlin.math.abs

/*
 * Skin temperature is a signed variation from a baseline the device set, not
 * a temperature: Health Connect stores timed deltas and an optional absolute
 * baseline per record. Everything here keeps the two apart. Values are
 * variations in signed degrees; the baseline is shown on its own; nothing is
 * a percent of a delta, which is arbitrary around zero.
 */

/** Two records agree on a baseline within this: the device rounds it. */
private const val BaselineAgreementCelsius = 0.05

/** Only the entries that carry a delta, oldest first. */
internal fun skinTemperatureChartEntries(
    entries: List<SkinTemperatureEntry>,
): List<SkinTemperatureEntry> =
    entries
        .filter { it.averageDeltaCelsius != null }
        .sortedBy { it.time }

internal fun skinTemperatureStats(entries: List<SkinTemperatureEntry>): VitalReadingStats? {
    val deltas = entries.mapNotNull { it.averageDeltaCelsius }
    val average = deltas.averageOrNull() ?: return null
    // readings counts every entry, including the delta-less ones the chart drops.
    return VitalReadingStats(
        average = average,
        low = deltas.min(),
        high = deltas.max(),
        readings = entries.size,
    )
}

internal fun SkinTemperatureEntry.skinTemperatureBaselineValue(): BaselineValue? =
    averageDeltaCelsius?.let { delta ->
        BaselineValue(
            date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
            value = delta,
        )
    }

/** The absolute baseline a period's records were measured against. [shared] when they all agree. */
internal data class SkinTemperaturePeriodBaseline(val celsius: Double, val shared: Boolean)

/**
 * The baseline is per record, not per period, so a period only has one to
 * show when its records agree. Otherwise the newest record's, said so.
 */
internal fun List<SkinTemperatureEntry>.skinTemperaturePeriodBaseline(): SkinTemperaturePeriodBaseline? {
    val baselines = filter { it.baselineCelsius != null }.sortedBy { it.time }.map { it.baselineCelsius!! }
    val newest = baselines.lastOrNull() ?: return null
    return SkinTemperaturePeriodBaseline(
        celsius = newest,
        shared = baselines.all { abs(it - newest) <= BaselineAgreementCelsius },
    )
}

/** A row's text: the variation, with the record's own baseline beside it. Never the baseline alone as a reading. */
internal fun skinTemperatureRowLabel(
    entry: SkinTemperatureEntry,
    unitFormatter: UnitFormatter,
    withBaselineTemplate: String,
    withoutBaselineTemplate: String,
    noVariationLabel: String,
): String {
    val delta = entry.averageDeltaCelsius ?: return noVariationLabel
    val variation = unitFormatter.temperatureDelta(delta).text
    val baseline = entry.baselineCelsius ?: return withoutBaselineTemplate.format(variation)
    return withBaselineTemplate.format(variation, unitFormatter.temperature(baseline).text)
}

/** [skinTemperatureRowLabel] with its strings resolved, for a list that formats rows outside composition. */
@Composable
internal fun rememberSkinTemperatureRowLabel(unitFormatter: UnitFormatter): (SkinTemperatureEntry) -> String {
    val withBaseline = stringResource(R.string.skin_temperature_row_with_baseline)
    val withoutBaseline = stringResource(R.string.skin_temperature_row_without_baseline)
    val noVariation = stringResource(R.string.skin_temperature_no_variation)
    return remember(unitFormatter, withBaseline, withoutBaseline, noVariation) {
        { entry -> skinTemperatureRowLabel(entry, unitFormatter, withBaseline, withoutBaseline, noVariation) }
    }
}

/** The zero line a variation reads against: warmer above, cooler below. */
internal fun skinTemperatureZeroGuides(): List<ChartGuideLine> =
    listOf(ChartGuideLine(value = 0.0, color = temperatureColor.copy(alpha = 0.45f)))

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
    val variation: @Composable (Double) -> DisplayValue = { unitFormatter.temperatureDelta(it) }
    val periodBaseline = entries.skinTemperaturePeriodBaseline()
    val previousAverage = previousEntries.mapNotNull { it.averageDeltaCelsius }.averageOrNull()

    Column(modifier = metricModifier()) {
        SectionHeader(stringResource(R.string.skin_temperature_variation_title))
        Text(
            text = stringResource(R.string.skin_temperature_variation_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        InsightStatGrid(
            stats = skinTemperatureReadingStats(stats, periodBaseline, unitFormatter) +
                listOfNotNull(
                    previousAverage?.let { previous ->
                        previousPeriodInsightStat(
                            comparison = periodComparison(stats.average, previous),
                            selectedRange = selectedRange,
                            unitFormatter = unitFormatter,
                            valueFormatter = variation,
                            accentColor = temperatureColor,
                            style = ComparisonDisplayStyle.SIGNED_VALUE,
                        )
                    },
                ) +
                personalBaselineInsightStats(
                    insight = personalBaselineInsight(
                        currentValue = stats.average,
                        values = baselineEntries.mapNotNull { it.skinTemperatureBaselineValue() },
                        referenceDate = period.start.minusDays(1),
                        // A day below the baseline is a reading, not a gap.
                        includeNonPositive = true,
                    ),
                    unitFormatter = unitFormatter,
                    valueFormatter = variation,
                    accentColor = temperatureColor,
                    style = ComparisonDisplayStyle.SIGNED_VALUE,
                    // "Baseline deviation" would collide with the device's baseline shown beside it.
                    deviationTitleRes = R.string.stat_vs_usual_average,
                ),
        )
    }
}

/** Average, lowest, highest, readings, and the device baseline on its own tile. */
@Composable
private fun skinTemperatureReadingStats(
    stats: VitalReadingStats,
    periodBaseline: SkinTemperaturePeriodBaseline?,
    unitFormatter: UnitFormatter,
): List<InsightStat> {
    val average = unitFormatter.temperatureDelta(stats.average)
    val low = unitFormatter.temperatureDelta(stats.low)
    val high = unitFormatter.temperatureDelta(stats.high)
    val baseline = periodBaseline?.let { unitFormatter.temperature(it.celsius) }
    return listOf(
        InsightStat(
            title = stringResource(R.string.stat_average),
            value = average.value,
            unit = average.unit,
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
        ),
        InsightStat(
            title = stringResource(R.string.stat_lowest),
            value = low.value,
            unit = low.unit,
            icon = Icons.Outlined.Star,
            accentColor = temperatureColor,
        ),
        InsightStat(
            title = stringResource(R.string.stat_highest),
            value = high.value,
            unit = high.unit,
            icon = Icons.Outlined.CalendarMonth,
            accentColor = temperatureColor,
        ),
        InsightStat(
            title = stringResource(R.string.stat_readings),
            value = unitFormatter.count(stats.readings),
            unit = "",
            icon = Icons.Outlined.CheckCircle,
            accentColor = temperatureColor,
        ),
        InsightStat(
            title = stringResource(R.string.stat_baseline),
            value = baseline?.value ?: "\u2014",
            unit = baseline?.unit.orEmpty(),
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = temperatureColor,
            caption = when {
                periodBaseline == null -> stringResource(R.string.skin_temperature_baseline_not_provided)
                !periodBaseline.shared -> stringResource(R.string.skin_temperature_baseline_newest)
                else -> null
            },
        ),
    )
}
