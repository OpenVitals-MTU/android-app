package tech.mmarca.openvitals.features.readiness

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/**
 * The readiness verdict as a card on the Body Energy screen. [date] is the
 * host's day; a placeholder shows while the two are out of step.
 */
@Composable
fun DailyReadinessCard(
    viewModel: DailyReadinessViewModel,
    date: LocalDate,
    onOpenTrainingReadinessDetails: (LocalDate) -> Unit,
    onOpenStressDetails: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(date) {
        if (state.selectedDate != date) viewModel.selectDate(date)
    }

    val insight = state.insight
    when {
        state.selectedDate != date || (state.isLoading && insight == null) ->
            PlaceholderCard(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        insight == null -> PlaceholderCard(modifier = modifier) {
            Text(
                text = state.error?.resolve() ?: stringResource(R.string.message_no_dashboard_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        else -> DailyReadinessPanel(
            insight = insight,
            onOpenTrainingReadinessDetails = { onOpenTrainingReadinessDetails(date) },
            onOpenStressDetails = { onOpenStressDetails(date) },
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaceholderCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) { content() }
}
