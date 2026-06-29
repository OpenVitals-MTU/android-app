package tech.mmarca.openvitals.features.hydration

import android.text.format.DateFormat
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.components.personalBaselineInsightStats
import tech.mmarca.openvitals.ui.components.previousPeriodInsightStat
import tech.mmarca.openvitals.ui.theme.HydrationColor
import java.time.LocalTime

internal val HydrationWeekChartColor = Color(0xFFB8C0FF)

@Composable
internal fun HydrationDataConfidence(
    display: HydrationDisplayState,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    if (period.start == period.end) return

    DataConfidenceCard(
        confidence = dataConfidence(
            period = period,
            trackedDates = display.trackedDates,
            sampleCount = display.sampleCount,
            valueKind = DataValueKind.AGGREGATED,
        ),
        accentColor = HydrationColor,
        modifier = modifier,
    )
}

@Composable
internal fun HydrationSummary(
    state: HydrationUiState,
    display: HydrationDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val summary = display.summary
    val title = if (state.selectedRange == TimeRange.DAY) {
        stringResource(R.string.metric_hydration)
    } else {
        stringResource(R.string.metric_total_hydration)
    }
    val subtitle = if (state.selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(state.selectedRange, period)
    } else {
        stringResource(R.string.summary_daily_average, unitFormatter.hydration(summary.averageLiters).text)
    }
    val total = unitFormatter.hydration(summary.totalLiters)

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
            value = unitFormatter.count(summary.trackedDays),
            unit = stringResource(R.string.unit_days),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = stringResource(R.string.summary_days_in_range, unitFormatter.count(summary.loggedDays)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun HydrationStatistics(
    display: HydrationDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    selectedRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    val summary = display.summary
    val total = unitFormatter.hydration(summary.totalLiters)
    val average = unitFormatter.hydration(summary.averageLiters)
    val bestDay = unitFormatter.hydration(summary.bestDayLiters)
    InsightStatGrid(
        stats = listOf(
            InsightStat(
                title = stringResource(R.string.stat_goal_streak),
                value = unitFormatter.count(summary.currentGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_goals_met),
                value = unitFormatter.count(summary.goalMetDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_longest_goal_streak),
                value = unitFormatter.count(summary.longestGoalStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = HydrationColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_success_rate),
                value = unitFormatter.count(summary.goalSuccessRatePercent),
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
                comparison = display.periodComparison,
                selectedRange = selectedRange,
                unitFormatter = unitFormatter,
                valueFormatter = { unitFormatter.hydration(it) },
                accentColor = HydrationColor,
            ),
        ) + personalBaselineInsightStats(
            insight = personalBaselineInsight(
                currentValue = summary.averageLiters,
                values = display.baselineValues,
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
internal fun HydrationGoalCard(
    display: HydrationDisplayState,
    dailyGoalLiters: Double,
    unitFormatter: UnitFormatter,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = display.summary
    val goal = unitFormatter.hydration(dailyGoalLiters)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
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
                            summary.goalMetDays,
                            summary.trackedDays,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OpenVitalsIconButton(onClick = onDecreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_hydration_goal),
                    )
                }
                OpenVitalsIconButton(onClick = onIncreaseGoal) {
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
internal fun HydrationReminderCard(
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

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
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
                OpenVitalsOutlinedButton(onClick = onRequestNotificationPermission) {
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
        OpenVitalsIconButton(
            onClick = onDecreaseInterval,
            enabled = intervalMinutes > HydrationReminderConfig.MinIntervalMinutes,
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.cd_decrease_hydration_reminder_interval),
            )
        }
        OpenVitalsIconButton(
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
        OpenVitalsTextButton(onClick = onClick) {
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
            OpenVitalsTextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HydrationDayGoalProgress(
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

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
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
