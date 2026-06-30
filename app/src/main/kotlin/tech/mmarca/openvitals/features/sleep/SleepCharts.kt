package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.MetricSparklineChart
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.SleepColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

private val SleepOverviewTopCardHeight = 124.dp
private val SleepOverviewMetricCardHeight = 112.dp
private val SleepOverviewChartWidth = 168.dp
private val SleepOverviewChartHeight = 48.dp

@Composable
internal fun SleepOverviewSectionContent(
    summary: SleepOverviewSummary,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SleepOverviewTopCards(
            summary = summary,
            selectedRange = selectedRange,
            period = period,
            unitFormatter = unitFormatter,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onOpenSleepScore = onOpenSleepScore,
        )
        SectionHeader(
            text = stringResource(R.string.activities_key_metrics),
            modifier = Modifier.padding(top = 12.dp),
        )
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_sleep_schedule),
            value = sleepScheduleOrNoData(summary, dateTimeFormatterProvider),
            subtitle = localizedPeriodTitle(selectedRange, period),
            icon = Icons.Outlined.Schedule,
            chartValues = emptyList(),
            dates = summary.dates,
            selectedRange = selectedRange,
            valueEmphasis = SleepOverviewValueEmphasis.Small,
            modifier = metricModifier(),
            chartContent = null,
        )
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_rem_sleep),
            value = durationOrNoData(summary.remDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.DarkMode,
            chartValues = summary.remValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
        )
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_deep_sleep),
            value = durationOrNoData(summary.deepDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Bed,
            chartValues = summary.deepValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
        )
        SleepOverviewMetricCard(
            title = stringResource(R.string.recovery_sleep_efficiency),
            value = sleepEfficiencyOrNoData(summary, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Speed,
            chartValues = summary.efficiencyValues,
            dates = summary.dates,
            selectedRange = selectedRange,
            modifier = metricModifier(),
            onClick = onOpenSleepEfficiency,
        )
    }
}

internal fun LazyListScope.sleepOverview(
    summary: SleepOverviewSummary,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
) {
    item {
        SleepOverviewSectionContent(
            summary = summary,
            selectedRange = selectedRange,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenSleepScore = onOpenSleepScore,
            onOpenSleepEfficiency = onOpenSleepEfficiency,
        )
    }
}

@Composable
private fun SleepOverviewTopCards(
    summary: SleepOverviewSummary,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    onOpenSleepScore: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SleepOverviewTopCard(
            title = stringResource(R.string.recovery_sleep_score),
            value = sleepScoreOrNoData(summary, unitFormatter),
            subtitle = sleepScoreConfidenceLabel(summary.sleepScoreConfidence),
            icon = Icons.Outlined.DarkMode,
            onClick = onOpenSleepScore,
            modifier = Modifier.weight(1f),
        )
        SleepOverviewTopCard(
            title = stringResource(R.string.recovery_sleep_duration),
            value = durationOrNoData(summary.sleepDurationMs, unitFormatter),
            subtitle = sleepOverviewAverageSubtitle(selectedRange, period),
            icon = Icons.Outlined.Bed,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SleepOverviewTopCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    OpenVitalsCard(
        modifier = modifier
            .height(SleepOverviewTopCardHeight),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SleepColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SleepColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                AutoResizeText(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                SleepOverviewValue(
                    value = value,
                    emphasis = SleepOverviewValueEmphasis.Medium,
                )
                AutoResizeText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SleepOverviewMetricCard(
    title: String,
    value: DisplayValue,
    subtitle: String,
    icon: ImageVector,
    chartValues: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    valueEmphasis: SleepOverviewValueEmphasis = SleepOverviewValueEmphasis.Large,
    chartContent: (@Composable () -> Unit)? = {
        SleepOverviewSparkline(
            values = chartValues,
            dates = dates,
            selectedRange = selectedRange,
            accentColor = SleepColor,
        )
    },
) {
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .height(SleepOverviewMetricCardHeight),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SleepColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    AutoResizeText(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SleepOverviewValue(
                    value = value,
                    emphasis = valueEmphasis,
                )
                AutoResizeText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            chartContent?.invoke()
        }
    }
}

@Composable
private fun SleepOverviewSparkline(
    values: List<Double>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
    accentColor: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    val labelDates = sleepOverviewLabelDates(dates, selectedRange)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MetricSparklineChart(
            values = values,
            accentColor = accentColor,
            modifier = Modifier
                .width(SleepOverviewChartWidth)
                .height(SleepOverviewChartHeight),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(SleepOverviewChartWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labelDates.forEach { date ->
                Text(
                    text = sleepOverviewSparklineLabel(date, selectedRange, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SleepOverviewValue(
    value: DisplayValue,
    emphasis: SleepOverviewValueEmphasis = SleepOverviewValueEmphasis.Large,
) {
    val valueStyle = when (emphasis) {
        SleepOverviewValueEmphasis.Large -> MaterialTheme.typography.headlineMedium
        SleepOverviewValueEmphasis.Medium -> MaterialTheme.typography.headlineSmall
        SleepOverviewValueEmphasis.Small -> MaterialTheme.typography.titleLarge
    }
    val unitStyle = when (emphasis) {
        SleepOverviewValueEmphasis.Large -> MaterialTheme.typography.bodyMedium
        SleepOverviewValueEmphasis.Medium,
        SleepOverviewValueEmphasis.Small -> MaterialTheme.typography.bodySmall
    }
    Row(verticalAlignment = Alignment.Bottom) {
        AutoResizeText(
            text = value.value,
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (value.unit.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = value.unit,
                style = unitStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun sleepOverviewAverageSubtitle(
    selectedRange: TimeRange,
    period: DatePeriod,
): String =
    if (selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(selectedRange, period)
    } else {
        stringResource(R.string.stat_daily_average)
    }

@Composable
private fun durationOrNoData(durationMs: Long, unitFormatter: UnitFormatter): DisplayValue =
    if (durationMs > 0L) {
        DisplayValue(unitFormatter.duration(durationMs), "")
    } else {
        DisplayValue(stringResource(R.string.no_data), "")
    }

@Composable
private fun sleepScheduleOrNoData(
    summary: SleepOverviewSummary,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): DisplayValue {
    val schedule = summary.schedule ?: return DisplayValue(stringResource(R.string.no_data), "")
    val formatter = dateTimeFormatterProvider.shortTime()
    val start = LocalTime.of(schedule.startMinute / 60, schedule.startMinute % 60)
    val end = LocalTime.of(schedule.endMinute / 60, schedule.endMinute % 60)
    return DisplayValue("${formatter.format(start)} - ${formatter.format(end)}", "")
}

@Composable
private fun sleepScoreOrNoData(summary: SleepOverviewSummary, unitFormatter: UnitFormatter): DisplayValue =
    summary.sleepScore?.let { score ->
        DisplayValue(unitFormatter.count(score), "")
    } ?: DisplayValue(stringResource(R.string.no_data), "")

@Composable
private fun sleepEfficiencyOrNoData(summary: SleepOverviewSummary, unitFormatter: UnitFormatter): DisplayValue =
    summary.sleepEfficiencyPercent?.let { efficiency ->
        unitFormatter.percent(efficiency, 0)
    } ?: DisplayValue(stringResource(R.string.no_data), "")

@Composable
private fun sleepScoreConfidenceLabel(confidence: SleepScoreConfidence): String =
    stringResource(
        when (confidence) {
            SleepScoreConfidence.HIGH -> R.string.sleep_score_confidence_high
            SleepScoreConfidence.MEDIUM -> R.string.sleep_score_confidence_medium
            SleepScoreConfidence.LOW -> R.string.sleep_score_confidence_low
            SleepScoreConfidence.NO_DATA -> R.string.sleep_score_confidence_no_data
        }
    )

private fun sleepOverviewLabelDates(
    dates: List<LocalDate>,
    selectedRange: TimeRange,
): List<LocalDate> {
    if (dates.isEmpty()) return emptyList()
    if (dates.size <= 3) return dates

    return when (selectedRange) {
        TimeRange.DAY,
        TimeRange.WEEK -> dates
        TimeRange.MONTH,
        TimeRange.YEAR -> listOf(dates.first(), dates[dates.lastIndex / 2], dates.last()).distinct()
    }
}

private fun sleepOverviewSparklineLabel(
    date: LocalDate,
    selectedRange: TimeRange,
    locale: java.util.Locale,
): String = when (selectedRange) {
    TimeRange.YEAR -> date.month.getDisplayName(TextStyle.SHORT, locale).take(3)
    TimeRange.MONTH -> date.dayOfMonth.toString()
    TimeRange.DAY,
    TimeRange.WEEK -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(1)
}

private enum class SleepOverviewValueEmphasis {
    Large,
    Medium,
    Small,
}
