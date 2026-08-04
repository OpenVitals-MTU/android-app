package tech.mmarca.openvitals.ui.components

import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.PeriodComparisonDirection
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun previousPeriodInsightStat(
    comparison: PeriodComparison,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    valueFormatter: @Composable (Double) -> DisplayValue,
    accentColor: Color,
): InsightStat {
    val title = stringResource(
        when (selectedRange) {
            TimeRange.DAY -> R.string.stat_vs_previous_day
            TimeRange.WEEK -> R.string.stat_vs_previous_week
            TimeRange.MONTH -> R.string.stat_vs_previous_month
            TimeRange.YEAR -> R.string.stat_vs_previous_year
        }
    )
    val display = comparison.percentChange
        ?.let { percent ->
            DisplayValue(
                value = signedValue(
                    value = unitFormatter.count(abs(percent.roundToInt())),
                    direction = comparison.direction,
                ),
                unit = stringResource(R.string.unit_percent_symbol),
            )
        }
        ?: valueFormatter(comparison.absoluteChange).let { absolute ->
            DisplayValue(
                value = signedValue(absolute.value, comparison.direction),
                unit = absolute.unit,
            )
        }

    return InsightStat(
        title = title,
        value = display.value,
        unit = display.unit,
        icon = when (comparison.direction) {
            PeriodComparisonDirection.UP -> Icons.AutoMirrored.Outlined.TrendingUp
            PeriodComparisonDirection.DOWN -> Icons.AutoMirrored.Outlined.TrendingDown
            PeriodComparisonDirection.SAME -> Icons.AutoMirrored.Outlined.TrendingFlat
        },
        accentColor = accentColor,
    )
}

private fun signedValue(value: String, direction: PeriodComparisonDirection): String =
    when (direction) {
        PeriodComparisonDirection.UP -> "+$value"
        PeriodComparisonDirection.DOWN -> "-$value"
        PeriodComparisonDirection.SAME -> value
    }
