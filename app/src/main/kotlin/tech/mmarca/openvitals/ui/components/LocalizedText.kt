package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun timeRangeLabel(range: TimeRange): String = stringResource(
    when (range) {
        TimeRange.DAY -> R.string.range_day
        TimeRange.WEEK -> R.string.range_week
        TimeRange.MONTH -> R.string.range_month
        TimeRange.YEAR -> R.string.range_year
    }
)

@Composable
fun localizedDayTitle(date: LocalDate): String = when (date) {
    LocalDate.now() -> stringResource(R.string.period_today)
    LocalDate.now().minusDays(1) -> stringResource(R.string.period_yesterday)
    else -> DateTimeFormatter.ofPattern("EEE, d MMM", LocalLocale.current.platformLocale).format(date)
}

fun localizedDaySubtitle(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(date)

@Composable
fun localizedPeriodTitle(
    range: TimeRange,
    period: DatePeriod,
    today: LocalDate = LocalDate.now(),
): String {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy", locale)

    return when (range) {
        TimeRange.DAY -> when (period.start) {
            today -> stringResource(R.string.period_today)
            today.minusDays(1) -> stringResource(R.string.period_yesterday)
            else -> dateFormatter.format(period.start)
        }

        TimeRange.WEEK -> if (today in period.start..period.end) {
            stringResource(R.string.period_this_week)
        } else {
            stringResource(R.string.period_week_of, dateFormatter.format(period.start))
        }

        TimeRange.MONTH -> if (period.end == today) {
            stringResource(R.string.period_this_month)
        } else {
            monthFormatter.format(period.start)
        }

        TimeRange.YEAR -> if (period.end == today) {
            stringResource(R.string.period_this_year)
        } else {
            yearFormatter.format(period.start)
        }
    }
}

fun localizedPeriodSubtitle(range: TimeRange, period: DatePeriod): String {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    return when (range) {
        TimeRange.DAY -> dateFormatter.format(period.start)
        TimeRange.WEEK,
        TimeRange.MONTH,
        TimeRange.YEAR -> "${dateFormatter.format(period.start)} - ${dateFormatter.format(period.end)}"
    }
}
