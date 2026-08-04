package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode

@Composable
fun PeriodNavigator(
    selectedRange: TimeRange,
    period: DatePeriod,
    title: String? = null,
    subtitle: String? = null,
    canGoForward: Boolean,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    weekPeriodMode: WeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .dateNavigationSwipe(
                    canGoForward = canGoForward,
                    onPrevious = onPreviousPeriod,
                    onNext = onNextPeriod,
                )
                .clickable(onClick = onOpenCalendar),
        ) {
            Text(
                text = title ?: localizedPeriodTitle(selectedRange, period, weekPeriodMode = weekPeriodMode),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subtitle ?: localizedPeriodSubtitle(selectedRange, period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OpenVitalsIconSurfaceButton(onClick = onPreviousPeriod) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(R.string.cd_previous_period),
                )
            }

            OpenVitalsIconSurfaceButton(
                onClick = onNextPeriod,
                enabled = canGoForward,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.cd_next_period),
                )
            }

            OpenVitalsIconSurfaceButton(onClick = onOpenCalendar) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.cd_open_calendar),
                )
            }
        }
    }
}
