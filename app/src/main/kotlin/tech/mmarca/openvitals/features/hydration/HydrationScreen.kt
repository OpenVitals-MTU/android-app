package tech.mmarca.openvitals.features.hydration

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val HydrationWeekChartColor = Color(0xFFB8C0FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditHydrationEntry: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(HydrationReminderController.hasNotificationPermission(context))
    }
    var enableRemindersAfterPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (hasNotificationPermission && enableRemindersAfterPermission) {
            viewModel.setHydrationRemindersEnabled(true)
        }
        enableRemindersAfterPermission = false
    }
    val requestNotificationPermission = {
        enableRemindersAfterPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
            viewModel.setHydrationRemindersEnabled(true)
            enableRemindersAfterPermission = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = HydrationReminderController.hasNotificationPermission(context)
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
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
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        if (state.dailyHydration.none { it.liters > 0.0 }) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_hydration),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    message = stringResource(R.string.message_no_hydration_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            hydrationGoalAndReminderItems(
                state = state,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onToggleReminders = viewModel::setHydrationRemindersEnabled,
                onRequestNotificationPermission = requestNotificationPermission,
                onDecreaseInterval = viewModel::decreaseHydrationReminderInterval,
                onIncreaseInterval = viewModel::increaseHydrationReminderInterval,
                onSelectActiveStartTime = viewModel::setHydrationReminderActiveStartTime,
                onSelectActiveEndTime = viewModel::setHydrationReminderActiveEndTime,
            )
        } else {
            item {
                HydrationSummary(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                HydrationHistoryChart(
                    data = state.dailyHydration,
                    selectedRange = state.selectedRange,
                    period = period,
                    dailyGoalLiters = state.dailyGoalLiters,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                hydrationEntries(
                    entries = state.hydrationEntries.filter {
                        it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                    },
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    titleDate = selectedDate,
                    onEditHydrationEntry = onEditHydrationEntry,
                    onDeleteHydrationEntry = viewModel::deleteHydrationEntry,
                )
            }
            item {
                HydrationDataConfidence(
                    data = state.dailyHydration,
                    period = period,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            hydrationGoalAndReminderItems(
                state = state,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onToggleReminders = viewModel::setHydrationRemindersEnabled,
                onRequestNotificationPermission = requestNotificationPermission,
                onDecreaseInterval = viewModel::decreaseHydrationReminderInterval,
                onIncreaseInterval = viewModel::increaseHydrationReminderInterval,
                onSelectActiveStartTime = viewModel::setHydrationReminderActiveStartTime,
                onSelectActiveEndTime = viewModel::setHydrationReminderActiveEndTime,
            )
            item { SectionHeader(stringResource(R.string.section_statistics)) }
            item {
                HydrationStatistics(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    selectedRange = state.selectedRange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            hydrationWeightInsight(
                hydration = state.dailyHydration,
                weightEntries = state.crossWeightEntries,
            )
            hydrationEntries(
                entries = state.hydrationEntries,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEditHydrationEntry = onEditHydrationEntry,
                onDeleteHydrationEntry = viewModel::deleteHydrationEntry,
            )
        }
    }
}

private fun LazyListScope.hydrationGoalAndReminderItems(
    state: HydrationUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
) {
    item {
        HydrationGoalCard(
            state = state,
            unitFormatter = unitFormatter,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    item {
        HydrationReminderCard(
            config = state.reminderConfig,
            hasNotificationPermission = hasNotificationPermission,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDecreaseInterval = onDecreaseInterval,
            onIncreaseInterval = onIncreaseInterval,
            onSelectActiveStartTime = onSelectActiveStartTime,
            onSelectActiveEndTime = onSelectActiveEndTime,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HydrationDataConfidence(
    data: List<DailyHydration>,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    if (period.start == period.end) return

    val tracked = data.filter { it.liters > 0.0 }
    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = tracked.map { it.date },
            sampleCount = tracked.size,
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = HydrationColor,
        modifier = modifier,
    )
}

@Composable
private fun HydrationSummary(
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val title = if (state.selectedRange == TimeRange.DAY) {
        stringResource(R.string.metric_hydration)
    } else {
        stringResource(R.string.metric_total_hydration)
    }
    val subtitle = if (state.selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(state.selectedRange, period)
    } else {
        stringResource(R.string.summary_daily_average, unitFormatter.hydration(state.averageLiters).text)
    }
    val total = unitFormatter.hydration(state.totalLiters)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = title,
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.metric_logged_days),
            value = unitFormatter.count(state.dailyHydration.count { it.liters > 0.0 }),
            unit = stringResource(R.string.unit_days),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = stringResource(R.string.summary_days_in_range, unitFormatter.count(state.dailyHydration.size)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HydrationStatistics(
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val total = unitFormatter.hydration(state.totalLiters)
    val average = unitFormatter.hydration(state.averageLiters)
    val bestDay = unitFormatter.hydration(state.bestDayLiters)
    InsightStatGrid(
        stats = listOf(
            InsightStat(
                title = stringResource(R.string.stat_goal_streak),
                value = unitFormatter.count(state.currentGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_goals_met),
                value = unitFormatter.count(state.goalMetDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_longest_goal_streak),
                value = unitFormatter.count(state.longestGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_success_rate),
                value = unitFormatter.count(state.goalSuccessRatePercent),
                unit = stringResource(R.string.unit_percent_symbol),
                icon = Icons.Outlined.Star,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_daily_average),
                value = average.value,
                unit = average.unit,
                icon = Icons.Outlined.Star,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_total_intake),
                value = total.value,
                unit = total.unit,
                icon = Icons.Outlined.LocalDrink,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_best_day),
                value = bestDay.value,
                unit = bestDay.unit,
                icon = Icons.Outlined.CalendarMonth,
                accentColor = HydrationColor,
            ),
            previousPeriodInsightStat(
                comparison = periodComparison(
                    currentValue = state.totalLiters,
                    previousValue = state.previousDailyHydration.sumOf { it.liters },
                ),
                selectedRange = selectedRange,
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.hydration(it) },
                accentColor = HydrationColor,
            ),
        ) + personalBaselineInsightStats(
            insight = personalBaselineInsight(
                currentValue = state.averageLiters,
                values = state.baselineDailyHydration.map { BaselineValue(it.date, it.liters) },
                referenceDate = period.start.minusDays(1),
            ),
            unitFormatter = unitFormatter,
            valueFormatter = { unitFormatter.hydration(it) },
            accentColor = HydrationColor,
        ),
        modifier = modifier,
    )
}

@Composable
private fun HydrationGoalCard(
    state: HydrationUiState,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goal = unitFormatter.hydration(state.dailyGoalLiters)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.hydration_daily_goal),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.hydration_goal_progress,
                            state.goalMetDays,
                            state.trackedDays,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDecreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_hydration_goal),
                    )
                }
                IconButton(onClick = onIncreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_increase_hydration_goal),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = goal.value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = goal.unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 3.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HydrationReminderCard(
    config: HydrationReminderConfig,
    hasNotificationPermission: Boolean,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onSelectActiveStartTime: (LocalTime) -> Unit,
    onSelectActiveEndTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalized = config.normalized()
    var editingTime by remember { mutableStateOf<HydrationReminderTimeField?>(null) }
    val startTime = dateTimeFormatterProvider.shortTime().format(normalized.activeStartTime)
    val endTime = dateTimeFormatterProvider.shortTime().format(normalized.activeEndTime)
    val body = when {
        normalized.enabled && !hasNotificationPermission -> {
            stringResource(R.string.hydration_reminders_permission_needed)
        }
        normalized.enabled -> {
            stringResource(R.string.hydration_reminders_summary_on, normalized.intervalMinutes, startTime, endTime)
        }
        else -> stringResource(R.string.hydration_reminders_summary_off)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.hydration_reminders_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = normalized.enabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission) {
                            onRequestNotificationPermission()
                        } else {
                            onToggleReminders(enabled)
                        }
                    },
                )
            }

            if (normalized.enabled && !hasNotificationPermission) {
                OutlinedButton(onClick = onRequestNotificationPermission) {
                    Text(stringResource(R.string.action_grant_permission))
                }
            }

            if (normalized.enabled) {
                HydrationReminderIntervalRow(
                    intervalMinutes = normalized.intervalMinutes,
                    onDecreaseInterval = onDecreaseInterval,
                    onIncreaseInterval = onIncreaseInterval,
                )
                HydrationReminderTimeRow(
                    label = stringResource(R.string.hydration_reminders_active_start),
                    value = startTime,
                    onClick = { editingTime = HydrationReminderTimeField.START },
                )
                HydrationReminderTimeRow(
                    label = stringResource(R.string.hydration_reminders_active_end),
                    value = endTime,
                    onClick = { editingTime = HydrationReminderTimeField.END },
                )
                Text(
                    text = stringResource(R.string.hydration_reminders_goal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    editingTime?.let { field ->
        HydrationReminderTimePickerDialog(
            title = stringResource(
                when (field) {
                    HydrationReminderTimeField.START -> R.string.hydration_reminders_active_start
                    HydrationReminderTimeField.END -> R.string.hydration_reminders_active_end
                }
            ),
            selectedTime = when (field) {
                HydrationReminderTimeField.START -> normalized.activeStartTime
                HydrationReminderTimeField.END -> normalized.activeEndTime
            },
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                editingTime = null
                when (field) {
                    HydrationReminderTimeField.START -> onSelectActiveStartTime(time)
                    HydrationReminderTimeField.END -> onSelectActiveEndTime(time)
                }
            },
        )
    }
}

@Composable
private fun HydrationReminderIntervalRow(
    intervalMinutes: Int,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = stringResource(R.string.hydration_reminders_interval),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.hydration_reminders_interval_value, intervalMinutes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(
            onClick = onDecreaseInterval,
            enabled = intervalMinutes > HydrationReminderConfig.MinIntervalMinutes,
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.cd_decrease_hydration_reminder_interval),
            )
        }
        IconButton(
            onClick = onIncreaseInterval,
            enabled = intervalMinutes < HydrationReminderConfig.MaxIntervalMinutes,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_increase_hydration_reminder_interval),
            )
        }
    }
}

@Composable
private fun HydrationReminderTimeRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.action_select))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HydrationReminderTimePickerDialog(
    title: String,
    selectedTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

private enum class HydrationReminderTimeField {
    START,
    END,
}

@Composable
private fun HydrationHistoryChart(
    data: List<DailyHydration>,
    selectedRange: TimeRange,
    period: DatePeriod,
    dailyGoalLiters: Double,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    if (selectedRange == TimeRange.DAY) {
        HydrationDayGoalProgress(
            liters = data.sumOf { it.liters },
            dailyGoalLiters = dailyGoalLiters,
            period = period,
            unitFormatter = unitFormatter,
            modifier = modifier,
        )
        return
    }

    val values = data.map { PeriodChartValue(date = it.date, value = it.liters) }
    val summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${
        unitFormatter.hydration(data.sumOf { it.liters }).text
    }"

    PeriodHistoryChart(
        title = stringResource(R.string.metric_hydration_trend),
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = if (selectedRange == TimeRange.WEEK) {
            HydrationWeekChartColor
        } else {
            HydrationColor.copy(alpha = 0.85f)
        },
        summaryText = summaryText,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.hydration(it).text },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HydrationDayGoalProgress(
    liters: Double,
    dailyGoalLiters: Double,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val hydration = unitFormatter.hydration(liters)
    val goal = unitFormatter.hydration(dailyGoalLiters)
    val targetProgress = if (dailyGoalLiters > 0.0) {
        (liters / dailyGoalLiters).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "HydrationDetailDayGoalProgress",
    )
    val strokeWidth = with(LocalDensity.current) { 5.dp.toPx() }
    val progressStroke = remember(strokeWidth) {
        Stroke(width = strokeWidth, cap = StrokeCap.Round)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.metric_hydration_trend),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localizedPeriodTitle(TimeRange.DAY, period),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${hydration.text} / ${goal.text}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                color = HydrationColor.copy(alpha = 0.86f),
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                stroke = progressStroke,
                trackStroke = progressStroke,
                wavelength = 34.dp,
                waveSpeed = 34.dp,
            )
        }
    }
}

private fun LazyListScope.hydrationWeightInsight(
    hydration: List<DailyHydration>,
    weightEntries: List<WeightEntry>,
) {
    val insight = crossMetricInsight(
        primaryValues = hydration.map { CrossMetricValue(it.date, it.liters) },
        secondaryValues = weightFluctuationValues(weightEntries),
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_hydration_weight_title),
            positiveMessage = stringResource(R.string.cross_hydration_weight_positive),
            negativeMessage = stringResource(R.string.cross_hydration_weight_negative),
            neutralMessage = stringResource(R.string.cross_hydration_weight_neutral),
            accentColor = HydrationColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
