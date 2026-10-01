package tech.mmarca.openvitals.wear.features.heart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.VitalsUiState
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.Sparkline
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

@Composable
fun HeartScreen(state: VitalsUiState) {
    val low = remember(state.heartRateSamples) { state.heartRateSamples.minOrNull() }
    val high = remember(state.heartRateSamples) { state.heartRateSamples.maxOrNull() }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = stringResource(R.string.metric_heart_rate),
                tint = HeartColor,
                modifier = Modifier.size(20.dp),
            )
            MetricValueRow(
                value = state.heartRateBpm?.toString(),
                unit = stringResource(R.string.unit_bpm),
                valueStyle = MaterialTheme.typography.displayMedium,
            )
            Sparkline(
                samples = state.heartRateSamples,
                color = HeartColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RangeStat(label = stringResource(R.string.heart_low), bpm = low)
                RangeStat(label = stringResource(R.string.heart_high), bpm = high)
            }
        }
    }
}

@Composable
private fun RangeStat(label: String, bpm: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetricValueRow(
            value = bpm?.toString(),
            valueStyle = MaterialTheme.typography.titleMedium,
        )
    }
}

@WearPreviews
@Composable
private fun HeartScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            HeartScreen(SampleData.vitals)
        }
    }
}
