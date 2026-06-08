package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.period.displayPeriodFor
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScaffold(
    isLoading: Boolean,
    selectedRange: TimeRange,
    selectedDate: LocalDate,
    error: String?,
    onRefresh: () -> Unit,
    onSelectRange: (TimeRange) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    primaryAction: MetricAction? = null,
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    periodOverride: (DatePeriod) -> DatePeriod = { it },
    periodTitle: @Composable ((DatePeriod) -> String)? = null,
    headerItems: LazyListScope.() -> Unit = {},
    content: LazyListScope.(period: DatePeriod) -> Unit,
) {
    val period = periodOverride(displayPeriodFor(selectedRange, selectedDate, weekPeriodMode = weekPeriodMode))
    val today = LocalDate.now()
    var showDatePicker by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                headerItems()
                item {
                    TimeRangeSelector(
                        selected = selectedRange,
                        onSelect = onSelectRange,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                item {
                    PeriodNavigator(
                        selectedRange = selectedRange,
                        period = period,
                        title = periodTitle?.invoke(period),
                        canGoForward = period.end.isBefore(today),
                        onPreviousPeriod = onPreviousPeriod,
                        onNextPeriod = onNextPeriod,
                        onOpenCalendar = { showDatePicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                primaryAction?.let { action ->
                    item {
                        CompactMetricActionButton(
                            action = action,
                            expanded = true,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                error?.let { err ->
                    item { ErrorMessage(err) }
                }
                content(period)
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                onSelectDate(date)
            },
        )
    }
}
