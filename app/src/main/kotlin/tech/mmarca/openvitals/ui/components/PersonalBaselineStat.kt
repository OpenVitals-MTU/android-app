package tech.mmarca.openvitals.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.insights.BaselineStatus
import tech.mmarca.openvitals.core.insights.PersonalBaselineInsight
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun personalBaselineInsightStats(
    insight: PersonalBaselineInsight?,
    unitFormatter: UnitFormatter,
    valueFormatter: @Composable (Double) -> DisplayValue,
    accentColor: Color,
): List<InsightStat> {
    if (insight == null) return emptyList()

    val baselineStats = insight.summaries.map { summary ->
        val average = valueFormatter(summary.average)
        InsightStat(
            title = stringResource(
                when (summary.windowDays) {
                    30 -> R.string.stat_30_day_baseline
                    60 -> R.string.stat_60_day_baseline
                    90 -> R.string.stat_90_day_baseline
                    else -> R.string.stat_baseline
                }
            ),
            value = average.value,
            unit = average.unit,
            icon = Icons.Outlined.CalendarMonth,
            accentColor = accentColor,
        )
    }

    val deviation = insight.percentDeviation
        ?.let { percent ->
            DisplayValue(
                value = signedValue(
                    value = unitFormatter.count(abs(percent).roundToInt()),
                    status = insight.status,
                ),
                unit = stringResource(R.string.unit_percent_symbol),
            )
        }
        ?: valueFormatter(insight.absoluteDeviation).let { absolute ->
            DisplayValue(
                value = signedValue(absolute.value, insight.status),
                unit = absolute.unit,
            )
        }

    return baselineStats + listOf(
        InsightStat(
            title = stringResource(R.string.stat_usual_range),
            value = baselineStatusLabel(insight.status),
            unit = "",
            icon = when (insight.status) {
                BaselineStatus.USUAL -> Icons.AutoMirrored.Outlined.TrendingFlat
                BaselineStatus.ABOVE,
                BaselineStatus.UNUSUAL_HIGH -> Icons.AutoMirrored.Outlined.TrendingUp
                BaselineStatus.BELOW,
                BaselineStatus.UNUSUAL_LOW -> Icons.AutoMirrored.Outlined.TrendingDown
            },
            accentColor = accentColor,
        ),
        InsightStat(
            title = stringResource(R.string.stat_baseline_deviation),
            value = deviation.value,
            unit = deviation.unit,
            icon = Icons.Outlined.Star,
            accentColor = accentColor,
        ),
    )
}

@Composable
private fun baselineStatusLabel(status: BaselineStatus): String =
    stringResource(
        when (status) {
            BaselineStatus.USUAL -> R.string.baseline_status_usual
            BaselineStatus.ABOVE -> R.string.baseline_status_above
            BaselineStatus.BELOW -> R.string.baseline_status_below
            BaselineStatus.UNUSUAL_HIGH -> R.string.baseline_status_unusual_high
            BaselineStatus.UNUSUAL_LOW -> R.string.baseline_status_unusual_low
        }
    )

private fun signedValue(value: String, status: BaselineStatus): String =
    when (status) {
        BaselineStatus.ABOVE,
        BaselineStatus.UNUSUAL_HIGH -> "+$value"
        BaselineStatus.BELOW,
        BaselineStatus.UNUSUAL_LOW -> "-$value"
        BaselineStatus.USUAL -> value
    }
