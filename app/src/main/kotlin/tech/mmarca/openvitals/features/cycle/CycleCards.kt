package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

private val MarkerDotSize: Dp = 5.dp
private val LegendSwatchSize: Dp = 12.dp
private val CellBorder: Dp = 1.dp
private val CellPadding: Dp = 5.dp
private const val HeavyFlowAlpha = 0.38f
private const val MediumFlowAlpha = 0.26f
private const val LightFlowAlpha = 0.16f

/** The month grid: recorded flow as fills, the estimated window as an outline, dots for starts and observations. */
@Composable
internal fun CycleCalendarCard(
    days: List<CycleDay>,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onSelectDay: ((LocalDate) -> Unit)? = null,
    today: LocalDate = LocalDate.now(),
) {
    OpenVitalsCard(modifier = modifier) {
        Column(modifier = Modifier.padding(LayoutMetrics.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = dateTimeFormatterProvider.monthYear().format(period.start),
                style = MaterialTheme.typography.titleSmall,
            )
            WeekdayHeader()
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    week.forEach { day ->
                        CycleDayCell(
                            day = day,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            // A log can open for a day of this month up to today; other cells are not buttons.
                            onClick = onSelectDay
                                ?.takeIf { day.inSelectedPeriod && !day.date.isAfter(today) }
                                ?.let { select -> { select(day.date) } },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
            CycleCalendarLegend()
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        listOf(
            R.string.weekday_monday_short,
            R.string.weekday_tuesday_short,
            R.string.weekday_wednesday_short,
            R.string.weekday_thursday_short,
            R.string.weekday_friday_short,
            R.string.weekday_saturday_short,
            R.string.weekday_sunday_short,
        ).forEach { labelRes ->
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CycleDayCell(
    day: CycleDay,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val flow = day.flows.maxOfOrNull { it.flow } ?: FLOW_UNKNOWN
    val hasPeriod = day.periodActive || flow != FLOW_UNKNOWN
    val containerColor = when (flow) {
        FLOW_HEAVY -> CycleColor.copy(alpha = HeavyFlowAlpha)
        FLOW_MEDIUM -> CycleColor.copy(alpha = MediumFlowAlpha)
        FLOW_LIGHT -> CycleColor.copy(alpha = LightFlowAlpha)
        else -> when {
            day.periodActive -> CycleColor.copy(alpha = Emphasis.wash)
            day.spotting -> CycleColor.copy(alpha = Emphasis.wash)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Emphasis.subtle)
        }
    }
    val contentColor = if (day.inSelectedPeriod) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Emphasis.disabled)
    }
    val estimatedBorder = if (day.predictedPeriod && !hasPeriod) {
        Modifier.border(width = CellBorder, color = CycleColor.copy(alpha = Emphasis.fill), shape = MaterialTheme.shapes.small)
    } else {
        Modifier
    }
    val description = buildString {
        append(dateTimeFormatterProvider.mediumDate().format(day.date))
        if (hasPeriod) {
            append(", ").append(stringResource(R.string.cycle_legend_recorded))
            if (flow != FLOW_UNKNOWN) append(", ").append(stringResource(flowLabelRes(flow)))
        } else if (day.predictedPeriod) {
            append(", ").append(stringResource(R.string.cycle_legend_estimated))
        }
        if (day.spotting) append(", ").append(stringResource(R.string.cycle_entry_section_spotting))
        if (day.isCycleStart) append(", ").append(stringResource(R.string.cycle_legend_cycle_start))
        if (day.hasObservations) append(", ").append(stringResource(R.string.cycle_legend_observations))
    }
    val clickable = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.cycle_calendar_open_day_log), onClick = onClick)
    } else {
        Modifier
    }

    OpenVitalsSurface(
        modifier = modifier
            .then(estimatedBorder)
            .then(clickable)
            // One utterance per cell: the day number below is already in the description.
            .clearAndSetSemantics { contentDescription = description },
        containerColor = if (day.inSelectedPeriod) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Emphasis.wash),
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(CellPadding), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (hasPeriod || day.isCycleStart) FontWeight.Bold else FontWeight.Normal,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs / 2)) {
                if (day.isCycleStart) MarkerDot(CycleColor)
                if (day.hasObservations) MarkerDot(MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .size(MarkerDotSize)
            .background(color = color, shape = CircleShape),
    )
}

/** What the fills, the outline and the dots mean. Colour is never the only carrier: the labels say it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CycleCalendarLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        LegendItem(stringResource(R.string.cycle_legend_recorded)) {
            Box(
                modifier = Modifier
                    .size(LegendSwatchSize)
                    .background(CycleColor.copy(alpha = MediumFlowAlpha), MaterialTheme.shapes.extraSmall),
            )
        }
        LegendItem(stringResource(R.string.cycle_legend_estimated)) {
            Box(
                modifier = Modifier
                    .size(LegendSwatchSize)
                    .border(CellBorder, CycleColor.copy(alpha = Emphasis.fill), MaterialTheme.shapes.extraSmall),
            )
        }
        LegendItem(stringResource(R.string.cycle_legend_cycle_start)) { MarkerDot(CycleColor) }
        LegendItem(stringResource(R.string.cycle_legend_observations)) { MarkerDot(MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        swatch()
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
