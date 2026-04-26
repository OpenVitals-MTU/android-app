package tech.mmarca.openvitals.features.cycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.CycleData
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.CycleColor
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: CycleViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()
    val requestCyclePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onCyclePermissionsResult(granted)
    }

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
    ) { period ->
        if (state.missingPermissions.isNotEmpty()) {
            item {
                PermissionCallout(
                    title = "Cycle permissions missing",
                    body = "Grant cycle tracking permissions to show period days, ovulation tests, cervical mucus, and basal temperature.",
                    onGrant = { requestCyclePermissions.launch(viewModel.cyclePermissions) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.data.hasData) {
            item {
                CycleSummary(
                    data = state.data,
                    period = period,
                    subtitle = periodTitle(state.selectedRange, period),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { SectionHeader("Cycle calendar") }
            item {
                CycleCalendarCard(
                    data = state.data,
                    period = period,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            if (state.data.basalBodyTemperature.isNotEmpty()) {
                item { SectionHeader("Basal body temperature") }
                item {
                    BasalTemperatureTrendCard(
                        entries = state.data.basalBodyTemperature,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }

            val observations = observationsFor(state.data)
            if (observations.isNotEmpty()) {
                item { SectionHeader("Entries") }
                items(observations) { observation ->
                    CycleObservationRow(
                        observation = observation,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        } else if (!state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = "Cycle tracking",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                    message = "No cycle data was recorded for this period.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CycleSummary(
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
                title = "Period days",
                value = unitFormatter.count(periodDays),
                unit = "days",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Ovulation tests",
                value = unitFormatter.count(data.ovulationTests.size),
                unit = "tests",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                subtitle = "Selected period",
                modifier = Modifier.weight(1f),
            )
        }
        if (latestTemperature != null) {
            MetricCard(
                title = "Latest BBT",
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
private fun CycleCalendarCard(
    data: CycleData,
    period: DatePeriod,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val days = cycleDays(period, data, zone)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                text = label,
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
        else -> if (day.periodActive) CycleColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    }
    val contentColor = if (day.inSelectedPeriod) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier,
        color = if (day.inSelectedPeriod) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
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
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun BasalTemperatureTrendCard(
    entries: List<BasalBodyTemperatureEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val minC = sorted.minOfOrNull { it.temperatureCelsius } ?: 35.0
    val maxC = sorted.maxOfOrNull { it.temperatureCelsius } ?: 38.0
    val range = (maxC - minC).coerceAtLeast(0.2)
    val zone = ZoneId.systemDefault()
    val dayFormatter = dateTimeFormatterProvider.chartDay()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (sorted.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                ) {
                    val stepX = size.width / (sorted.size - 1)
                    val points = sorted.mapIndexed { index, entry ->
                        Offset(
                            x = index * stepX,
                            y = size.height * (1f - ((entry.temperatureCelsius - minC) / range).toFloat()),
                        )
                    }
                    for (index in 0 until points.lastIndex) {
                        drawLine(
                            color = CycleColor,
                            start = points[index],
                            end = points[index + 1],
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    points.forEach { point ->
                        drawCircle(color = CycleColor, radius = 4.dp.toPx(), center = point)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            val latest = sorted.last()
            val latestDate = latest.time.atZone(zone).toLocalDate()
            Text(
                text = "Latest ${unitFormatter.temperature(latest.temperatureCelsius).text} · ${dayFormatter.format(latestDate)}",
                style = MaterialTheme.typography.titleSmall,
                color = CycleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Range ${unitFormatter.temperature(minC).text}-${unitFormatter.temperature(maxC).text} · ${unitFormatter.count(sorted.size)} readings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CycleObservationRow(
    observation: CycleObservation,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val time = observation.time.atZone(zone)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = observation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = observation.source)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = observation.value,
                style = MaterialTheme.typography.bodyMedium,
                color = CycleColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
    }
}

private data class CycleDay(
    val date: LocalDate,
    val inSelectedPeriod: Boolean,
    val periodActive: Boolean,
    val flows: List<MenstruationFlowEntry>,
    val ovulationTests: List<OvulationTestEntry>,
    val basalBodyTemperature: BasalBodyTemperatureEntry?,
)

private data class CycleObservation(
    val time: Instant,
    val title: String,
    val value: String,
    val source: String,
)

private fun cycleDays(period: DatePeriod, data: CycleData, zone: ZoneId): List<CycleDay> {
    val gridStart = period.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridEnd = period.end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val flowsByDate = data.menstruationFlows.groupBy { it.time.atZone(zone).toLocalDate() }
    val ovulationByDate = data.ovulationTests.groupBy { it.time.atZone(zone).toLocalDate() }
    val bbtByDate = data.basalBodyTemperature
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .mapValues { (_, readings) -> readings.maxByOrNull { it.time } }
    val menstruationDates = data.menstruationPeriods.flatMap { it.dates(zone) }.toSet()

    return datesBetween(gridStart, gridEnd).map { date ->
        CycleDay(
            date = date,
            inSelectedPeriod = !date.isBefore(period.start) && !date.isAfter(period.end),
            periodActive = date in menstruationDates,
            flows = flowsByDate[date].orEmpty(),
            ovulationTests = ovulationByDate[date].orEmpty(),
            basalBodyTemperature = bbtByDate[date],
        )
    }
}

private fun observationsFor(data: CycleData): List<CycleObservation> {
    val zone = ZoneId.systemDefault()
    return buildList {
        data.menstruationPeriods.forEach { period ->
            val days = period.dates(zone).size.toLong().coerceAtLeast(1)
            add(
                CycleObservation(
                    time = period.startTime,
                    title = "Menstruation period",
                    value = "$days ${if (days == 1L) "day" else "days"}",
                    source = period.source,
                )
            )
        }
        data.menstruationFlows.forEach { flow ->
            add(
                CycleObservation(
                    time = flow.time,
                    title = "Menstruation flow",
                    value = flowLabel(flow.flow),
                    source = flow.source,
                )
            )
        }
        data.ovulationTests.forEach { test ->
            add(
                CycleObservation(
                    time = test.time,
                    title = "Ovulation test",
                    value = ovulationResultLabel(test.result),
                    source = test.source,
                )
            )
        }
        data.cervicalMucus.forEach { mucus ->
            add(
                CycleObservation(
                    time = mucus.time,
                    title = "Cervical mucus",
                    value = mucusLabel(mucus),
                    source = mucus.source,
                )
            )
        }
    }.sortedByDescending { it.time }
}

private fun MenstruationPeriodEntry.dates(zone: ZoneId): List<LocalDate> {
    val startDate = startTime.atZone(zone).toLocalDate()
    val endDate = endTime.minusMillis(1).atZone(zone).toLocalDate()
    return datesBetween(startDate, endDate)
}

private fun datesBetween(start: LocalDate, endInclusive: LocalDate): List<LocalDate> =
    generateSequence(start) { date ->
        val next = date.plusDays(1)
        if (next.isAfter(endInclusive)) null else next
    }.toList()

private fun flowLabel(flow: Int): String = when (flow) {
    FLOW_LIGHT -> "Light"
    FLOW_MEDIUM -> "Medium"
    FLOW_HEAVY -> "Heavy"
    else -> "Unknown"
}

private fun ovulationResultLabel(result: Int): String = when (result) {
    OVULATION_POSITIVE -> "Positive"
    OVULATION_HIGH -> "High"
    OVULATION_NEGATIVE -> "Negative"
    else -> "Inconclusive"
}

private fun mucusLabel(mucus: CervicalMucusEntry): String {
    val appearance = when (mucus.appearance) {
        MUCUS_DRY -> "Dry"
        MUCUS_STICKY -> "Sticky"
        MUCUS_CREAMY -> "Creamy"
        MUCUS_WATERY -> "Watery"
        MUCUS_EGG_WHITE -> "Egg white"
        MUCUS_UNUSUAL -> "Unusual"
        else -> "Unknown"
    }
    val sensation = when (mucus.sensation) {
        MUCUS_LIGHT -> "light"
        MUCUS_MEDIUM -> "medium"
        MUCUS_HEAVY -> "heavy"
        else -> "unknown"
    }
    return "$appearance, $sensation"
}

private fun measurementLocationLabel(location: Int): String = when (location) {
    1 -> "Armpit"
    2 -> "Finger"
    3 -> "Forehead"
    4 -> "Mouth"
    5 -> "Rectum"
    6 -> "Temporal artery"
    7 -> "Toe"
    8 -> "Ear"
    9 -> "Wrist"
    10 -> "Vagina"
    else -> "Measurement location unknown"
}

private const val FLOW_UNKNOWN = 0
private const val FLOW_LIGHT = 1
private const val FLOW_MEDIUM = 2
private const val FLOW_HEAVY = 3

private const val OVULATION_POSITIVE = 1
private const val OVULATION_HIGH = 2
private const val OVULATION_NEGATIVE = 3

private const val MUCUS_DRY = 1
private const val MUCUS_STICKY = 2
private const val MUCUS_CREAMY = 3
private const val MUCUS_WATERY = 4
private const val MUCUS_EGG_WHITE = 5
private const val MUCUS_UNUSUAL = 6

private const val MUCUS_LIGHT = 1
private const val MUCUS_MEDIUM = 2
private const val MUCUS_HEAVY = 3
