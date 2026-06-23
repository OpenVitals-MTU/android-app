package tech.mmarca.openvitals.features.mindfulness

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.CrossMetricValue
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.insights.crossMetricInsight
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.dailySleepSummary
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController
import tech.mmarca.openvitals.ui.components.CrossMetricInsightCard
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.DailyGoalCard
import tech.mmarca.openvitals.ui.components.DailyGoalStatistics
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.PeriodHistoryChart
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.SwipeToDeleteEntryRow
import tech.mmarca.openvitals.ui.components.entryListTitle
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    viewModel: MindfulnessViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditMindfulnessSession: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(MindfulnessReminderController.hasNotificationPermission(context))
    }
    var enableRemindersAfterPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (hasNotificationPermission && enableRemindersAfterPermission) {
            viewModel.setMindfulnessRemindersEnabled(true)
        }
        enableRemindersAfterPermission = false
    }
    val requestNotificationPermission = {
        enableRemindersAfterPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
            viewModel.setMindfulnessRemindersEnabled(true)
            enableRemindersAfterPermission = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = MindfulnessReminderController.hasNotificationPermission(context)
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
        if (state.sessions.isEmpty() && !state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_mindfulness),
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                    message = stringResource(R.string.message_no_mindfulness_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            mindfulnessGoalAndReminderItems(
                state = state,
                period = period,
                values = mindfulnessDailyGoalValues(state.sessions),
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onToggleReminders = viewModel::setMindfulnessRemindersEnabled,
                onRequestNotificationPermission = requestNotificationPermission,
                onSelectReminderTime = viewModel::setMindfulnessReminderTime,
            )
        }

        if (state.sessions.isNotEmpty()) {
            item {
                MindfulnessSummary(
                    state = state,
                    subtitle = localizedPeriodTitle(state.selectedRange, period),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                MindfulnessHistoryChart(
                    sessions = state.sessions,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedDate = chartDaySelection.selectedDate,
                    onDateSelected = chartDaySelection.onDateSelected,
                )
            }
            chartDaySelection.selectedDate?.let { selectedDate ->
                item {
                    val zone = ZoneId.systemDefault()
                    PaginatedEntryList(
                        title = entryListTitle(selectedDate, dateTimeFormatterProvider),
                        entries = state.sessions
                            .filter { it.startTime.atZone(zone).toLocalDate() == selectedDate }
                            .sortedByDescending { it.startTime },
                    ) { session, rowModifier ->
                        MindfulnessSessionRow(
                            session = session,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onEdit = session.editAction(onEditMindfulnessSession),
                            onDelete = session.deleteAction(viewModel::deleteMindfulnessSessionEntry),
                            modifier = rowModifier,
                        )
                    }
                }
            }
            mindfulnessDataConfidence(
                sessions = state.sessions,
                period = period,
            )
            mindfulnessGoalAndReminderItems(
                state = state,
                period = period,
                values = mindfulnessDailyGoalValues(state.sessions),
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onToggleReminders = viewModel::setMindfulnessRemindersEnabled,
                onRequestNotificationPermission = requestNotificationPermission,
                onSelectReminderTime = viewModel::setMindfulnessReminderTime,
            )
            mindfulnessStatistics(
                sessions = state.sessions,
                previousSessions = state.previousSessions,
                baselineSessions = state.baselineSessions,
                period = period,
                selectedRange = state.selectedRange,
                unitFormatter = unitFormatter,
                includeHeader = false,
            )
            mindfulnessSleepInsight(
                sessions = state.sessions,
                sleepSessions = state.crossSleepSessions,
                period = period,
                sleepRangeMode = state.sleepRangeMode,
            )
            item {
                PaginatedEntryList(
                    title = stringResource(R.string.section_sessions),
                    entries = state.sessions.sortedByDescending { it.startTime },
                ) { session, rowModifier ->
                    MindfulnessSessionRow(
                        session = session,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onEdit = session.editAction(onEditMindfulnessSession),
                        onDelete = session.deleteAction(viewModel::deleteMindfulnessSessionEntry),
                        modifier = rowModifier,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.mindfulnessDataConfidence(
    sessions: List<MindfulnessSession>,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    val zone = ZoneId.systemDefault()
    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = sessions.map { it.startTime.atZone(zone).toLocalDate() },
                sampleCount = sessions.size,
                sources = sessions.map { it.source },
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

@Composable
private fun MindfulnessHistoryChart(
    sessions: List<MindfulnessSession>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
) {
    val zone = ZoneId.systemDefault()
    val values = sessions
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, daySessions) ->
            PeriodChartValue(
                date = date,
                value = daySessions.sumOf { it.durationMs }.toDouble() / 60_000.0,
            )
        }
    val totalMinutes = sessions.sumOf { it.durationMinutes }

    PeriodHistoryChart(
        title = stringResource(R.string.metric_mindfulness),
        values = values,
        selectedRange = selectedRange,
        period = period,
        accentColor = MindfulnessColor.copy(alpha = 0.85f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.minutes(totalMinutes).text}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier.fillMaxWidth(),
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        valueFormatter = { unitFormatter.minutes(it.roundToLong()).text },
    )
}

private fun LazyListScope.mindfulnessGoalAndReminderItems(
    state: MindfulnessUiState,
    period: DatePeriod,
    values: List<DailyGoalValue>,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    hasNotificationPermission: Boolean,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectReminderTime: (LocalTime) -> Unit,
) {
    val goalKey = MetricDailyGoalKey.MINDFULNESS_MINUTES
    val progress = dailyGoalProgress(
        values = values,
        period = period,
        target = state.dailyGoalMinutes,
        direction = goalKey.direction,
    )
    item {
        DailyGoalCard(
            goal = unitFormatter.minutes(state.dailyGoalMinutes.roundToLong()),
            progress = progress,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            onDecreaseGoal = onDecreaseGoal,
            onIncreaseGoal = onIncreaseGoal,
            modifier = metricModifier(),
        )
    }
    item {
        MindfulnessReminderCard(
            config = state.reminderConfig,
            hasNotificationPermission = hasNotificationPermission,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onToggleReminders = onToggleReminders,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSelectReminderTime = onSelectReminderTime,
            modifier = metricModifier(),
        )
    }
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        DailyGoalStatistics(
            progress = progress,
            averageGap = unitFormatter.minutes(progress.averageGapToGoal.roundToLong()),
            unitFormatter = unitFormatter,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MindfulnessReminderCard(
    config: MindfulnessReminderConfig,
    hasNotificationPermission: Boolean,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onToggleReminders: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectReminderTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalized = config.normalized()
    var editingTime by remember { mutableStateOf(false) }
    val reminderTime = dateTimeFormatterProvider.shortTime().format(normalized.reminderTime)
    val body = when {
        normalized.enabled && !hasNotificationPermission -> {
            stringResource(R.string.mindfulness_reminders_permission_needed)
        }
        normalized.enabled -> {
            stringResource(R.string.mindfulness_reminders_summary_on, reminderTime)
        }
        else -> stringResource(R.string.mindfulness_reminders_summary_off)
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
                    tint = MindfulnessColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.mindfulness_reminders_title),
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
                MindfulnessReminderTimeRow(
                    label = stringResource(R.string.mindfulness_reminders_time),
                    value = reminderTime,
                    onClick = { editingTime = true },
                )
                Text(
                    text = stringResource(R.string.mindfulness_reminders_goal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (editingTime) {
        MindfulnessReminderTimePickerDialog(
            title = stringResource(R.string.mindfulness_reminders_time),
            selectedTime = normalized.reminderTime,
            onDismiss = { editingTime = false },
            onConfirm = { time ->
                editingTime = false
                onSelectReminderTime(time)
            },
        )
    }
}

@Composable
private fun MindfulnessReminderTimeRow(
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
private fun MindfulnessReminderTimePickerDialog(
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

private fun LazyListScope.mindfulnessStatistics(
    sessions: List<MindfulnessSession>,
    previousSessions: List<MindfulnessSession>,
    baselineSessions: List<MindfulnessSession>,
    period: DatePeriod,
    selectedRange: TimeRange,
    unitFormatter: UnitFormatter,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        item { SectionHeader(stringResource(R.string.section_statistics)) }
    }
    item {
        val totalMs = sessions.sumOf { it.durationMs.coerceAtLeast(0L) }
        val averageMs = sessions.takeIf { it.isNotEmpty() }
            ?.let { totalMs / it.size }
            ?: 0L
        val longestMs = sessions.maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L
        val previousTotalMs = previousSessions.sumOf { it.durationMs.coerceAtLeast(0L) }
        val dailyMinutes = mindfulnessDailyGoalValues(sessions).map { it.value }
        val baselineValues = mindfulnessDailyGoalValues(baselineSessions)
            .map { BaselineValue(it.date, it.value) }

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration(totalMs),
                    unit = "",
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_sessions),
                    value = unitFormatter.count(sessions.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average_duration),
                    value = unitFormatter.duration(averageMs),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_session),
                    value = unitFormatter.duration(longestMs),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = MindfulnessColor,
                ),
                previousPeriodInsightStat(
                    comparison = periodComparison(
                        currentValue = totalMs.toDouble(),
                        previousValue = previousTotalMs.toDouble(),
                    ),
                    selectedRange = selectedRange,
                    unitFormatter = unitFormatter,
                    valueFormatter = { DisplayValue(unitFormatter.duration(it.roundToLong()), "") },
                    accentColor = MindfulnessColor,
                ),
            ) + personalBaselineInsightStats(
                insight = personalBaselineInsight(
                    currentValue = dailyMinutes.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
                    values = baselineValues,
                    referenceDate = period.start.minusDays(1),
                ),
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.minutes(it.roundToLong()) },
                accentColor = MindfulnessColor,
            ),
            modifier = metricModifier(),
        )
    }
}

private fun mindfulnessDailyGoalValues(sessions: List<MindfulnessSession>): List<DailyGoalValue> {
    val zone = ZoneId.systemDefault()
    return sessions
        .groupBy { it.startTime.atZone(zone).toLocalDate() }
        .map { (date, daySessions) ->
            DailyGoalValue(
                date = date,
                value = daySessions.sumOf { it.durationMs.coerceAtLeast(0L) }.toDouble() / 60_000.0,
            )
        }
}

private fun LazyListScope.mindfulnessSleepInsight(
    sessions: List<MindfulnessSession>,
    sleepSessions: List<SleepData>,
    period: DatePeriod,
    sleepRangeMode: SleepRangeMode,
) {
    val insight = crossMetricInsight(
        primaryValues = mindfulnessDailyGoalValues(sessions)
            .map { CrossMetricValue(it.date, it.value) },
        secondaryValues = sleepDurationValues(
            sessions = sleepSessions,
            period = period,
            sleepRangeMode = sleepRangeMode,
        ),
    ) ?: return

    item { SectionHeader(stringResource(R.string.section_cross_metric_insights)) }
    item {
        CrossMetricInsightCard(
            insight = insight,
            title = stringResource(R.string.cross_mindfulness_sleep_title),
            positiveMessage = stringResource(R.string.cross_mindfulness_sleep_positive),
            negativeMessage = stringResource(R.string.cross_mindfulness_sleep_negative),
            neutralMessage = stringResource(R.string.cross_mindfulness_sleep_neutral),
            accentColor = MindfulnessColor,
            modifier = metricModifier(),
        )
    }
}

private fun sleepDurationValues(
    sessions: List<SleepData>,
    period: DatePeriod,
    sleepRangeMode: SleepRangeMode,
): List<CrossMetricValue> {
    val zone = ZoneId.systemDefault()
    return generateSequence(period.start) { current ->
        current.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        CrossMetricValue(
            date = date,
            value = dailySleepSummary(
                sessions = sessions,
                selectedDate = date,
                sleepRangeMode = sleepRangeMode,
                zone = zone,
            )?.durationHours ?: 0.0,
        )
    }.toList()
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun MindfulnessSummary(
    state: MindfulnessUiState,
    subtitle: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = unitFormatter.minutes(state.totalMinutes)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = stringResource(R.string.metric_total_mindfulness),
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.section_sessions),
            value = unitFormatter.count(state.sessions.size),
            unit = stringResource(R.string.unit_total),
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = stringResource(R.string.period_selected),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MindfulnessSessionRow(
    session: MindfulnessSession,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onDelete != null) {
        SwipeToDeleteEntryRow(
            onDelete = onDelete,
            modifier = modifier,
        ) {
            MindfulnessSessionRowContent(
                session = session,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onEdit = onEdit,
            )
        }
    } else {
        MindfulnessSessionRowContent(
            session = session,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEdit = onEdit,
            modifier = modifier,
        )
    }
}

@Composable
private fun MindfulnessSessionRowContent(
    session: MindfulnessSession,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MindfulnessColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: stringResource(R.string.metric_mindfulness),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = unitFormatter.duration(session.durationMs),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = session.source)
            }
            if (onEdit != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun MindfulnessSession.editAction(onEditMindfulnessSession: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onEditMindfulnessSession(id) }
    } else {
        null
    }

private fun MindfulnessSession.deleteAction(onDeleteMindfulnessSession: (String) -> Unit): (() -> Unit)? =
    if (isOpenVitalsEntry && id.isNotBlank()) {
        { onDeleteMindfulnessSession(id) }
    } else {
        null
    }
