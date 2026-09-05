package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.ui.components.OpenVitalsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.remember
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
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.ChartSkeleton
import tech.mmarca.openvitals.ui.components.ChartSkeletonShape
import tech.mmarca.openvitals.ui.components.ChartTokens
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
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
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
    onSectionEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sectionContext = rememberMetricDetailSectionOrdering(onSectionEditStateChanged)
    val chartDaySelection = rememberChartDaySelection(
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        key = "calories",
    )
    // Keyed on exactly the state the builder reads; recomputing per recomposition made the screen crawl.
    val period = remember(state.selectedRange, state.selectedDate, state.weekPeriodMode) {
        displayPeriodFor(state.selectedRange, state.selectedDate, weekPeriodMode = state.weekPeriodMode)
    }
    val rows = remember(
        state.nutrition,
        state.dailySteps,
        state.bmrEntries,
        state.displayBmrKcal,
        period,
    ) {
        caloriesBreakdownRows(state, period)
    }
    val statistics = remember(state.nutrition, state.dailySteps, state.bmrEntries, state.latestBmrKcal) {
        caloriesStatistics(state)
    }

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
            onSelectDay = viewModel::selectDay,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            sectionListState = sectionContext.listState,
        ) { scaffoldPeriod ->
            renderCaloriesOrderedContent(
                sectionContext = sectionContext,
                state = state,
                period = scaffoldPeriod,
                rows = rows,
                statistics = statistics,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
            )
        }
    }
}

private fun LazyListScope.renderCaloriesOrderedContent(
    sectionContext: MetricDetailSectionContext,
    state: CaloriesUiState,
    period: DatePeriod,
    rows: List<CaloriesBreakdownEntry>,
    statistics: CaloriesStatistics,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    chartDaySelection: tech.mmarca.openvitals.ui.components.ChartDaySelection,
) {
    if (!state.hasAnyCaloriesData() && state.isLoading) {
        // Still loading: a skeleton where the chart will be. Existing content is kept.
        item {
            ChartSkeleton(
                modifier = metricModifier(),
                shape = ChartSkeletonShape.BARS,
                height = ChartTokens.heightPeriodBar,
            )
        }
        return
    }

    val visibleRows = if (state.hasAnyCaloriesData()) rows else emptyList()
    val selectedDate = chartDaySelection.selectedDate
    val selectedRows = selectedDate?.let { date -> visibleRows.filter { it.date == date } }.orEmpty()

    renderOrderedMetricDetailSections(sectionContext) {
        section(MetricDetailSectionId.ACTIVITY_SUMMARY, state.hasAnyCaloriesData() || !state.isLoading) {
            if (state.hasAnyCaloriesData()) {
                CaloriesStatisticsContent(
                    statistics = statistics,
                    latestBmrKcal = state.displayBmrKcal,
                    unitFormatter = unitFormatter,
                )
            } else {
                MetricCardPlaceholder(
                    title = stringResource(R.string.screen_calories),
                    icon = Icons.Outlined.LocalFireDepartment,
                    accentColor = CaloriesColor,
                    message = stringResource(R.string.message_no_calorie_data_period),
                    modifier = metricModifier(),
                )
            }
        }

        section(
            if (state.selectedRange == TimeRange.DAY) {
                MetricDetailSectionId.INTRADAY_CHART
            } else {
                MetricDetailSectionId.PERIOD_CHART
            },
            state.hasAnyCaloriesData(),
        ) {
            CaloriesTrendContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                selectedDate = chartDaySelection.selectedDate,
                onDateSelected = chartDaySelection.onDateSelected,
            )
        }

        section(MetricDetailSectionId.SELECTED_DAY_ENTRIES, selectedDate != null && selectedRows.isNotEmpty()) {
            selectedDate?.let { date ->
                CaloriesBreakdownEntriesContent(
                    entries = selectedRows,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = date,
                )
            }
        }

        section(MetricDetailSectionId.ENTRIES, visibleRows.isNotEmpty()) {
            CaloriesBreakdownEntriesContent(
                entries = visibleRows,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            )
        }
    }
}

@Composable
private fun CaloriesStatisticsContent(
    statistics: CaloriesStatistics,
    latestBmrKcal: Double?,
    unitFormatter: UnitFormatter,
) {
    SectionHeader(stringResource(R.string.section_statistics))
    val totalDisplay = statistics.totalSum?.let(unitFormatter::energy)
    val activeDisplay = statistics.activeSum?.let(unitFormatter::energy)
    val bmrDisplay = latestBmrKcal?.let(unitFormatter::energy)
    val totalAverageDisplay = statistics.totalAverage?.let(unitFormatter::energy)
    val activeAverageDisplay = statistics.activeAverage?.let(unitFormatter::energy)
    val bmrReadingCount = statistics.bmrReadingCount
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

@Composable
private fun CaloriesTrendContent(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    SectionHeader(stringResource(R.string.section_calorie_trends))
    TotalCaloriesTrendContent(
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
    )
    ActiveCaloriesTrendContent(
        state = state,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
    )
    BmrTrendContent(
        entries = state.bmrEntries,
        selectedRange = state.selectedRange,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
    )
}

@Composable
private fun TotalCaloriesTrendContent(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.nutrition.any { it.hasCaloriesBurnedData }) {
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
            val total = remember(state.nutrition) { state.nutrition.sumOf { it.caloriesBurnedKcal } }
            MetricBarChart(
                title = stringResource(R.string.metric_calories_burned),
                data = state.nutrition,
                selectedRange = state.selectedRange,
                period = period,
                summaryValue = unitFormatter.energy(total).text,
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

@Composable
private fun ActiveCaloriesTrendContent(
    state: CaloriesUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (state.selectedRange == TimeRange.DAY || state.dailySteps.any { it.activeCaloriesKcal != null }) {
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
            val total = remember(state.dailySteps) {
                state.dailySteps.sumOf { it.activeCaloriesKcal ?: 0.0 }
            }
            MetricBarChart(
                title = stringResource(R.string.metric_active_calories),
                data = state.dailySteps,
                selectedRange = state.selectedRange,
                period = period,
                summaryValue = unitFormatter.energy(total).text,
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

@Composable
private fun BmrTrendContent(
    entries: List<BmrEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    if (entries.isEmpty()) return

    val latest = remember(entries) { entries.maxByOrNull { it.time }?.kcalPerDay }
    val historyValues = remember(entries) { bmrHistoryValues(entries) }
    MetricBarChart(
        title = stringResource(R.string.metric_bmr),
        values = historyValues,
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

@Composable
private fun CaloriesBreakdownEntriesContent(
    entries: List<CaloriesBreakdownEntry>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    titleDate: LocalDate? = null,
) {
    if (entries.isEmpty()) return

    val sortedEntries = remember(entries) { entries.sortedByDescending { it.date } }
    PaginatedEntryList(
        title = entryListTitle(titleDate, dateTimeFormatterProvider),
        entries = sortedEntries,
    ) { row, rowModifier ->
        CaloriesBreakdownRow(
            entry = row,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = rowModifier,
        )
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

internal fun caloriesBreakdownRows(
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

/** The Statistics grid's six numbers. Null means no readings, never a real zero. */
internal data class CaloriesStatistics(
    val totalSum: Double? = null,
    val totalAverage: Double? = null,
    val activeSum: Double? = null,
    val activeAverage: Double? = null,
    val bmrReadingCount: Int = 0,
)

internal fun caloriesStatistics(state: CaloriesUiState): CaloriesStatistics {
    val totalValues = state.nutrition.filter { it.hasCaloriesBurnedData }.map { it.caloriesBurnedKcal }
    val activeValues = state.dailySteps.mapNotNull { it.activeCaloriesKcal }
    return CaloriesStatistics(
        totalSum = totalValues.takeIf { it.isNotEmpty() }?.sum(),
        totalAverage = totalValues.takeIf { it.isNotEmpty() }?.average(),
        activeSum = activeValues.takeIf { it.isNotEmpty() }?.sum(),
        activeAverage = activeValues.takeIf { it.isNotEmpty() }?.average(),
        bmrReadingCount = when {
            state.bmrEntries.isNotEmpty() -> state.bmrEntries.size
            state.latestBmrKcal != null -> 1
            else -> 0
        },
    )
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

internal data class CaloriesBreakdownEntry(
    val date: LocalDate,
    val totalCaloriesKcal: Double?,
    val totalCaloriesSource: CaloriesBurnedSource,
    val activeCaloriesKcal: Double?,
    val bmrKcal: Double?,
)
