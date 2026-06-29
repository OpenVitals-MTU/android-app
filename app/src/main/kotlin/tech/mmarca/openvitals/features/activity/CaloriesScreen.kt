package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricBarChart
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CaloriesScreen(
    viewModel: CaloriesViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "calories",
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod()
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.CALORIES,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
        ) { period ->
        calorieStatistics(
            state = state,
            period = period,
            unitFormatter = unitFormatter,
        )

        if (state.hasAnyCaloriesData()) {
            item { SectionHeader(stringResource(R.string.section_calorie_trends)) }
            totalCaloriesTrend(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
            activeCaloriesTrend(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
            bmrTrend(
                entries = state.bmrEntries,
                selectedRange = state.selectedRange,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )

            val rows = caloriesBreakdownRows(state, period)
            chartDaySelection.selectedDate?.let { selectedDate ->
                calorieBreakdownEntries(
                    entries = rows.filter { it.date == selectedDate },
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = selectedDate,
                )
            }
            calorieBreakdownEntries(
                entries = rows,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        } else if (!state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.screen_calories),
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                    message = stringResource(R.string.message_no_calorie_data_period),
                    modifier = metricModifier(),
                )
            }
        }
    }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.calorieStatistics(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val totalValues = state.nutrition.filter { it.hasCaloriesBurnedData }.map { it.caloriesBurnedKcal }
        val activeValues = state.dailySteps.mapNotNull { it.activeCaloriesKcal }
        val latestBmr = state.displayBmrKcal
        val bmrReadingCount = if (state.bmrEntries.isNotEmpty()) {
            state.bmrEntries.size
        } else if (state.latestBmrKcal != null) {
            1
        } else {
            0
        }
        val totalDisplay = totalValues.sumDisplay(unitFormatter)
        val activeDisplay = activeValues.sumDisplay(unitFormatter)
        val bmrDisplay = latestBmr?.let(unitFormatter::energy)
        val totalAverageDisplay = totalValues.averageDisplay(unitFormatter)
        val activeAverageDisplay = activeValues.averageDisplay(unitFormatter)
        val noData = stringResource(R.string.no_data)

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_calories_out),
                    value = totalDisplay?.value ?: noData,
                    unit = totalDisplay?.unit.orEmpty(),
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                ),
                InsightStat(
                    title = stringResource(R.string.metric_active_calories),
                    value = activeDisplay?.value ?: noData,
                    unit = activeDisplay?.unit.orEmpty(),
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = ActiveCaloriesColor,
                ),
                InsightStat(
                    title = stringResource(R.string.metric_bmr),
                    value = bmrDisplay?.value ?: noData,
                    unit = bmrDisplay?.unit.orEmpty(),
                    icon = Icons.Outlined.MonitorWeight,
                    accentColor = WeightColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_daily_average),
                    value = totalAverageDisplay?.value ?: noData,
                    unit = totalAverageDisplay?.unit.orEmpty(),
                    icon = Icons.Outlined.Star,
                    accentColor = CaloriesColor,
                ),
                InsightStat(
                    title = stringResource(R.string.calories_stat_active_average),
                    value = activeAverageDisplay?.value ?: noData,
                    unit = activeAverageDisplay?.unit.orEmpty(),
                    icon = Icons.Outlined.Star,
                    accentColor = ActiveCaloriesColor,
                ),
                InsightStat(
                    title = stringResource(R.string.calories_stat_bmr_readings),
                    value = if (bmrReadingCount == 0) {
                        noData
                    } else {
                        unitFormatter.count(bmrReadingCount)
                    },
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = WeightColor,
                ),
            ),
            modifier = metricModifier(),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.totalCaloriesTrend(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.nutrition.any { it.hasCaloriesBurnedData }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
                val value = state.nutrition
                    .firstOrNull { it.date == state.selectedDate && it.hasCaloriesBurnedData }
                    ?.caloriesBurnedKcal
                    ?: 0.0
                IntradayActivityChartCard(
                    selectedDate = state.selectedDate,
                    title = stringResource(R.string.metric_calories_out),
                    valueText = unitFormatter.energy(value).text,
                    emptyText = stringResource(R.string.message_no_calories_burned),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    points = state.activityProgress.mapNotNull { point ->
                        point.totalCaloriesBurnedKcal?.let { point.time to it }
                    },
                    accentColor = CaloriesColor,
                    yAxisValueFormatter = { unitFormatter.energy(it).text },
                    modifier = metricModifier(),
                )
            } else {
                MetricBarChart(
                    title = stringResource(R.string.metric_calories_burned),
                    data = state.nutrition,
                    selectedRange = state.selectedRange,
                    period = period,
                    summaryValue = unitFormatter.energy(state.nutrition.sumOf { it.caloriesBurnedKcal }).text,
                    accentColor = CaloriesColor,
                    accentAlpha = 0.8f,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    date = { it.date },
                    value = { it.caloriesBurnedKcal },
                    valueFormatter = { unitFormatter.energy(it).text },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.activeCaloriesTrend(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.activeCaloriesKcal != null }) {
        item {
            if (state.selectedRange == TimeRange.DAY) {
                val value = state.dailySteps
                    .firstOrNull { it.date == state.selectedDate }
                    ?.activeCaloriesKcal
                    ?: 0.0
                IntradayActivityChartCard(
                    selectedDate = state.selectedDate,
                    title = stringResource(R.string.metric_active_calories),
                    valueText = unitFormatter.energy(value).text,
                    emptyText = stringResource(R.string.message_no_active_calories),
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    points = state.activityProgress.mapNotNull { point ->
                        point.totalActiveCaloriesKcal?.let { point.time to it }
                    },
                    accentColor = ActiveCaloriesColor,
                    yAxisValueFormatter = { unitFormatter.energy(it).text },
                    modifier = metricModifier(),
                )
            } else {
                MetricBarChart(
                    title = stringResource(R.string.metric_active_calories),
                    data = state.dailySteps,
                    selectedRange = state.selectedRange,
                    period = period,
                    summaryValue = unitFormatter.energy(state.dailySteps.sumOf { it.activeCaloriesKcal ?: 0.0 }).text,
                    accentColor = ActiveCaloriesColor,
                    accentAlpha = 0.8f,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = metricModifier(),
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    date = { it.date },
                    value = { it.activeCaloriesKcal ?: 0.0 },
                    valueFormatter = { unitFormatter.energy(it).text },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bmrTrend(
    entries: List<BmrEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (entries.isEmpty()) return

    item {
        val latest = entries.maxByOrNull { it.time }?.kcalPerDay
        MetricBarChart(
            title = stringResource(R.string.metric_bmr),
            values = bmrHistoryValues(entries),
            selectedRange = selectedRange,
            period = period,
            accentColor = WeightColor,
            summaryValue = buildString {
                if (latest != null) {
                    append(stringResource(R.string.metric_latest))
                    append(" ")
                    append(unitFormatter.energy(latest).text)
                    append(" · ")
                }
                append(stringResource(R.string.summary_readings, unitFormatter.count(entries.size)))
            },
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = metricModifier(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { unitFormatter.energy(it).text },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.calorieBreakdownEntries(
    entries: List<CaloriesBreakdownEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
) {
    if (entries.isEmpty()) return

    item {
        PaginatedEntryList(
            title = entryListTitle(titleDate, dateTimeFormatterProvider),
            entries = entries.sortedByDescending { it.date },
        ) { row, rowModifier ->
            CaloriesBreakdownRow(
                entry = row,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = rowModifier,
            )
        }
    }
}

@Composable
private fun CaloriesBreakdownRow(
    entry: CaloriesBreakdownEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = dateTimeFormatterProvider.mediumDate().format(entry.date),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalorieBreakdownValue(
                    label = stringResource(R.string.stat_total),
                    value = entry.totalCaloriesKcal?.let { value ->
                        val text = unitFormatter.energy(value).text
                        if (entry.totalCaloriesSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
                            stringResource(R.string.calories_estimated_value, text)
                        } else {
                            text
                        }
                    } ?: stringResource(R.string.no_data),
                    color = CaloriesColor,
                    modifier = Modifier.weight(1f),
                )
                CalorieBreakdownValue(
                    label = stringResource(R.string.metric_active_calories),
                    value = entry.activeCaloriesKcal?.let { unitFormatter.energy(it).text }
                        ?: stringResource(R.string.no_data),
                    color = ActiveCaloriesColor,
                    modifier = Modifier.weight(1f),
                )
                CalorieBreakdownValue(
                    label = stringResource(R.string.metric_bmr),
                    value = entry.bmrKcal?.let { unitFormatter.energy(it).text }
                        ?: stringResource(R.string.no_data),
                    color = WeightColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CalorieBreakdownValue(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AutoResizeText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        AutoResizeText(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

private fun CaloriesUiState.hasAnyCaloriesData(): Boolean =
    selectedRange == TimeRange.DAY ||
        nutrition.any { it.hasCaloriesBurnedData } ||
        dailySteps.any { it.activeCaloriesKcal != null } ||
        displayBmrKcal != null

private fun caloriesBreakdownRows(
    state: CaloriesUiState,
    period: DatePeriod,
): List<CaloriesBreakdownEntry> {
    val nutritionByDate = state.nutrition.associateBy { it.date }
    val stepsByDate = state.dailySteps.associateBy { it.date }
    val bmrByDate = state.bmrEntries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .mapValues { (_, entries) -> entries.maxByOrNull { it.time } }
    val dates = (nutritionByDate.keys + stepsByDate.keys + bmrByDate.keys)
        .filter { date -> !date.isBefore(period.start) && !date.isAfter(period.end) }
        .toSet()

    return dates.mapNotNull { date ->
        val nutrition = nutritionByDate[date]
        val steps = stepsByDate[date]
        val bmrKcal = bmrByDate[date]?.kcalPerDay ?: state.displayBmrKcal
        val total = nutrition?.takeIf { it.hasCaloriesBurnedData }
        val active = steps?.activeCaloriesKcal
        if (total == null && active == null && bmrKcal == null) {
            null
        } else {
            CaloriesBreakdownEntry(
                date = date,
                totalCaloriesKcal = total?.caloriesBurnedKcal,
                totalCaloriesSource = total?.caloriesBurnedSource ?: CaloriesBurnedSource.NO_DATA,
                activeCaloriesKcal = active,
                bmrKcal = bmrKcal,
            )
        }
    }
}

private fun bmrHistoryValues(entries: List<BmrEntry>): List<PeriodChartValue> =
    entries
        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
        .mapNotNull { (date, dayEntries) ->
            dayEntries.maxByOrNull { it.time }?.let { latest ->
                PeriodChartValue(date = date, value = latest.kcalPerDay)
            }
        }

private fun List<Double>.sumDisplay(unitFormatter: UnitFormatter): DisplayValue? =
    takeIf { it.isNotEmpty() }?.let { values -> unitFormatter.energy(values.sum()) }

private fun List<Double>.averageDisplay(unitFormatter: UnitFormatter): DisplayValue? =
    takeIf { it.isNotEmpty() }?.let { values -> unitFormatter.energy(values.average()) }

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

private data class CaloriesBreakdownEntry(
    val date: LocalDate,
    val totalCaloriesKcal: Double?,
    val totalCaloriesSource: CaloriesBurnedSource,
    val activeCaloriesKcal: Double?,
    val bmrKcal: Double?,
)
