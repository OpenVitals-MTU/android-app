package tech.mmarca.openvitals.features.readiness

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.components.DayNavigator

@Composable
internal fun DailyReadinessContent(
    state: DailyReadinessUiState,
    canGoForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBodyEnergyDetails: () -> Unit,
    onOpenTrainingReadinessDetails: () -> Unit,
    onOpenStressDetails: () -> Unit,
) {
    val insight = state.insight ?: return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                DayNavigator(
                    date = state.selectedDate,
                    canGoForward = canGoForward,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onOpenCalendar = onOpenCalendar,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                DailyReadinessPanel(
                    insight = insight,
                    onOpenBodyEnergyDetails = onOpenBodyEnergyDetails,
                    onOpenTrainingReadinessDetails = onOpenTrainingReadinessDetails,
                    onOpenStressDetails = onOpenStressDetails,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
