package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.LocalDate

data class ChartDaySelection(
    val selectedDate: LocalDate?,
    val onDateSelected: (LocalDate) -> Unit,
)

@Composable
fun rememberChartDaySelection(
    selectedRange: TimeRange,
    selectedDate: LocalDate,
    key: Any? = Unit,
): ChartDaySelection {
    var chartSelectedDate by remember(selectedRange, selectedDate, key) {
        mutableStateOf<LocalDate?>(null)
    }
    val isActiveRange = selectedRange.supportsChartDaySelection()

    return ChartDaySelection(
        selectedDate = chartSelectedDate.takeIf { isActiveRange },
        onDateSelected = { date ->
            if (isActiveRange) {
                chartSelectedDate = if (chartSelectedDate == date) null else date
            }
        },
    )
}

fun TimeRange.supportsChartDaySelection(): Boolean =
    this == TimeRange.WEEK || this == TimeRange.MONTH
