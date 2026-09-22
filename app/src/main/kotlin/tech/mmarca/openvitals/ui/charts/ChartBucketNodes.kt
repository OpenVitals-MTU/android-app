package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider

/**
 * One invisible node per bucket, laid over the plot, so a screen reader can walk the
 * days and select one. The nodes span the whole range; a pinch moves only the picture.
 */
@Composable
internal fun PeriodBucketNodes(
    dates: List<LocalDate>,
    values: List<Double?>,
    selectedRange: TimeRange,
    selectedDate: LocalDate?,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    valueFormatter: (Double) -> String,
    onDateSelected: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (dates.isEmpty()) return
    val noDataLabel = stringResource(R.string.no_data)
    val spokenDate = remember(dateTimeFormatterProvider, selectedRange) {
        if (selectedRange == TimeRange.YEAR) {
            dateTimeFormatterProvider.monthYear()
        } else {
            dateTimeFormatterProvider.mediumDate()
        }
    }
    Row(modifier = modifier) {
        dates.forEachIndexed { index, date ->
            val description = chartBucketDescription(
                dateText = spokenDate.format(date),
                valueText = values.getOrNull(index)?.let(valueFormatter),
                noDataLabel = noDataLabel,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        contentDescription = description
                        // Only the chosen day says so; "not selected" thirty times is noise.
                        if (date == selectedDate) selected = true
                        if (onDateSelected != null) {
                            role = Role.Button
                            onClick {
                                onDateSelected(date)
                                true
                            }
                        }
                    },
            )
        }
    }
}

/**
 * One value per entry of [dates]: the mean of the points on that day, or in that month
 * on a year chart. Null where there is none.
 */
internal fun lineChartBucketValues(
    points: List<MetricLinePoint>,
    dates: List<LocalDate>,
    selectedRange: TimeRange,
): List<Double?> {
    val bucketOf: (LocalDate) -> LocalDate =
        if (selectedRange == TimeRange.YEAR) { date -> date.withDayOfMonth(1) } else { date -> date }
    val byBucket = points.groupBy { bucketOf(it.date) }
    return dates.map { date -> byBucket[bucketOf(date)]?.map { it.value }?.average() }
}
