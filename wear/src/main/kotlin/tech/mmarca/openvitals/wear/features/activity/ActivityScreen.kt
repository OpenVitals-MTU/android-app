package tech.mmarca.openvitals.wear.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.VitalsUiState
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatKilometers
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.StepsColor

/** Today's movement: steps against the goal as a bezel ring, distance and calories below. */
@Composable
fun ActivityScreen(state: VitalsUiState) {
    val goalFraction = if (state.stepGoal > 0) {
        ((state.steps ?: 0) / state.stepGoal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { goalFraction },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                // The ring opens at the top, where the scaffold draws the time.
                startAngle = 295f,
                endAngle = 245f,
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = StepsColor,
                    trackColor = StepsColor.copy(alpha = Emphasis.subtle),
                ),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    contentDescription = stringResource(R.string.metric_steps),
                    tint = StepsColor,
                    modifier = Modifier.size(20.dp),
                )
                MetricValueRow(
                    value = state.steps?.let(::formatCount),
                    valueStyle = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = stringResource(R.string.steps_goal, formatCount(state.stepGoal)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricValueRow(
                        value = state.distanceMeters?.let(::formatKilometers),
                        unit = stringResource(R.string.unit_km),
                        valueStyle = MaterialTheme.typography.titleMedium,
                    )
                    MetricValueRow(
                        value = state.activeCalories?.let(::formatCount),
                        unit = stringResource(R.string.unit_kcal),
                        valueStyle = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@WearPreviews
@Composable
private fun ActivityScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            ActivityScreen(SampleData.vitals)
        }
    }
}
