package tech.mmarca.openvitals.features.cycle

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.ui.theme.CycleColor
import java.time.ZoneId

@Composable
internal fun CycleSummary(
    data: CycleData,
    period: DatePeriod,
    subtitle: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val periodDays = cycleDays(period, data, zone)
        .count { it.inSelectedPeriod && (it.periodActive || it.flows.isNotEmpty()) }
    val latestBbt = data.basalBodyTemperature.maxByOrNull { it.time }
    val latestTemperature = latestBbt?.let { unitFormatter.temperature(it.temperatureCelsius) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = stringResource(R.string.metric_period_days),
                value = unitFormatter.count(periodDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = stringResource(R.string.metric_ovulation_tests),
                value = unitFormatter.count(data.ovulationTests.size),
                unit = stringResource(R.string.unit_tests),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                subtitle = stringResource(R.string.period_selected),
                modifier = Modifier.weight(1f),
            )
        }
        if (latestTemperature != null) {
            MetricCard(
                title = stringResource(R.string.metric_latest_bbt),
                value = latestTemperature.value,
                unit = latestTemperature.unit,
                icon = Icons.Outlined.DeviceThermostat,
                accentColor = CycleColor,
                subtitle = measurementLocationLabel(latestBbt?.measurementLocation ?: 0),
            )
        }
    }
}

@Composable
internal fun CycleCalendarCard(
    data: CycleData,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val days = cycleDays(period, data, zone)

    OpenVitalsCard(
        modifier = modifier,

    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = dateTimeFormatterProvider.monthYear().format(period.start),
                style = MaterialTheme.typography.titleSmall,
            )
            WeekdayHeader()
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        CycleDayCell(
                            day = day,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun CycleDayCell(day: CycleDay, modifier: Modifier = Modifier) {
    val flow = day.flows.maxOfOrNull { it.flow } ?: FLOW_UNKNOWN
    val hasPeriod = day.periodActive || flow != FLOW_UNKNOWN
    val containerColor = when (flow) {
        FLOW_HEAVY -> CycleColor.copy(alpha = 0.38f)
        FLOW_MEDIUM -> CycleColor.copy(alpha = 0.26f)
        FLOW_LIGHT -> CycleColor.copy(alpha = 0.16f)
        else -> if (day.periodActive) {
            CycleColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        }
    }
    val contentColor = if (day.inSelectedPeriod) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    OpenVitalsSurface(
        modifier = modifier,
        containerColor = if (day.inSelectedPeriod) {
            containerColor
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        },
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (hasPeriod) MarkerDot(CycleColor)
                if (day.ovulationTests.isNotEmpty()) MarkerDot(MaterialTheme.colorScheme.primary)
                if (day.basalBodyTemperature != null) MarkerDot(MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color = color, shape = CircleShape),
    )
}
