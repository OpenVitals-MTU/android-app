package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.DailyGoalBalance
import tech.mmarca.openvitals.domain.insights.DailyGoalDirection
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import kotlin.math.abs

/**
 * A [DailyGoalBalance] already put into the metric's own units, ready to render.
 *
 * @property amount the unsigned distance from goal × elapsed days.
 * @property isAhead true when the balance is in the user's favour (ahead of an
 *   at-least goal, under an at-most one). Ignored when [isOnTrack].
 * @property isOnTrack the balance rounds to nothing either way.
 * @property catchUpPerDay what each of the [remainingDays] needs to average for
 *   the whole period to finish on goal; null when there is nothing to say.
 */
data class DailyGoalBalanceDisplay(
    val amount: DisplayValue,
    val isAhead: Boolean,
    val isOnTrack: Boolean,
    val direction: DailyGoalDirection,
    val catchUpPerDay: DisplayValue?,
    val remainingDays: Int,
)

/**
 * Formats a domain balance with the metric's own [formatter] — the same one the
 * goal and the average gap go through, so steps stay steps and distance keeps
 * the unit preference.
 */
@Composable
fun dailyGoalBalanceDisplay(
    balance: DailyGoalBalance?,
    direction: DailyGoalDirection,
    formatter: @Composable (Double) -> DisplayValue,
): DailyGoalBalanceDisplay? {
    balance ?: return null
    val amount = formatter(abs(balance.balance))
    val isOnTrack = amount.value.none { it.isDigit() && it != '0' }
    return DailyGoalBalanceDisplay(
        amount = amount,
        isAhead = balance.balance > 0.0,
        isOnTrack = isOnTrack,
        direction = direction,
        catchUpPerDay = balance.catchUpPerDay?.let { formatter(it) },
        remainingDays = balance.remainingDays,
    )
}

@Composable
private fun DailyGoalBalanceDisplay.standingText(): String {
    val amountText = listOf(amount.value, amount.unit).filter { it.isNotBlank() }.joinToString(" ")
    return when {
        isOnTrack -> stringResource(R.string.goal_balance_on_track)
        direction == DailyGoalDirection.AT_LEAST && isAhead -> stringResource(R.string.goal_balance_ahead, amountText)
        direction == DailyGoalDirection.AT_LEAST -> stringResource(R.string.goal_balance_behind, amountText)
        isAhead -> stringResource(R.string.goal_balance_under, amountText)
        else -> stringResource(R.string.goal_balance_over, amountText)
    }
}

@Composable
private fun DailyGoalBalanceDisplay.catchUpText(): String? {
    val perDay = catchUpPerDay ?: return null
    val perDayText = listOf(perDay.value, perDay.unit).filter { it.isNotBlank() }.joinToString(" ")
    return pluralStringResource(R.plurals.goal_catch_up_per_day, remainingDays, perDayText, remainingDays)
}

/** Unicode minus rather than a hyphen, so the sign reads as a sign and not a dash. */
private const val MINUS_SIGN = "−"

@Composable
fun DailyGoalCard(
    goal: DisplayValue,
    progress: DailyGoalProgress,
    icon: ImageVector,
    accentColor: Color,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
    modifier: Modifier = Modifier,
    balance: DailyGoalBalanceDisplay? = null,
) {
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.daily_goal),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.goal_progress,
                            progress.goalMetDays,
                            progress.trackedDays,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (balance != null) {
                        Text(
                            text = balance.standingText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                balance.isOnTrack || balance.isAhead -> accentColor
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                        balance.catchUpText()?.let { catchUp ->
                            Text(
                                text = catchUp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OpenVitalsIconButton(onClick = onDecreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_daily_goal),
                    )
                }
                OpenVitalsIconButton(onClick = onIncreaseGoal) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_increase_daily_goal),
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
                if (goal.unit.isNotBlank()) {
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
}

/**
 * @param averageGap the mean distance from the goal over the period, or null to leave the stat
 *   out. A period one day long has no mean to take — the gap IS the day — so a day view passes
 *   null rather than labelling one night's shortfall an average.
 */
@Composable
fun DailyGoalStatistics(
    progress: DailyGoalProgress,
    averageGap: DisplayValue?,
    unitFormatter: UnitFormatter,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    balance: DailyGoalBalanceDisplay? = null,
) {
    InsightStatGrid(
        stats = listOfNotNull(
            InsightStat(
                title = stringResource(R.string.stat_goals_met),
                value = unitFormatter.count(progress.goalMetDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CheckCircle,
                accentColor = accentColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_success_rate),
                value = unitFormatter.count(progress.successRatePercent),
                unit = stringResource(R.string.unit_percent_symbol),
                icon = Icons.Outlined.Star,
                accentColor = accentColor,
            ),
            balance?.let { standing ->
                InsightStat(
                    title = stringResource(R.string.stat_goal_balance),
                    value = when {
                        standing.isOnTrack -> standing.amount.value
                        standing.isAhead -> "+${standing.amount.value}"
                        else -> "$MINUS_SIGN${standing.amount.value}"
                    },
                    unit = standing.amount.unit,
                    icon = when {
                        standing.isOnTrack || standing.isAhead -> Icons.AutoMirrored.Outlined.TrendingUp
                        else -> Icons.AutoMirrored.Outlined.TrendingDown
                    },
                    accentColor = accentColor,
                    caption = standing.catchUpText() ?: standing.standingText(),
                )
            },
            InsightStat(
                title = stringResource(R.string.stat_goal_streak),
                value = unitFormatter.count(progress.currentStreakDays()),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = accentColor,
            ),
            InsightStat(
                title = stringResource(R.string.stat_longest_goal_streak),
                value = unitFormatter.count(progress.longestStreakDays),
                unit = stringResource(R.string.unit_days),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = accentColor,
            ),
            averageGap?.let { gap ->
                InsightStat(
                    title = stringResource(R.string.stat_average_gap),
                    value = gap.value,
                    unit = gap.unit,
                    icon = icon,
                    accentColor = accentColor,
                )
            },
        ),
        modifier = modifier,
    )
}
