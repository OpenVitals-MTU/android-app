package tech.mmarca.openvitals.wear.features.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

/**
 * Adds and removes dashboard tiles. Only [available] metrics are listed: a
 * metric the watch has no sensor for cannot be switched on.
 */
@Composable
fun TileEditorScreen(
    available: List<WearMetric>,
    tiles: List<WearMetric>,
    onToggle: (WearMetric, Boolean) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.tiles_title))
                }
            }
            items(available, key = { it.name }) { metric ->
                SwitchButton(
                    checked = metric in tiles,
                    onCheckedChange = { checked -> onToggle(metric, checked) },
                    label = { Text(stringResource(metric.label)) },
                    icon = { Icon(metric.icon, contentDescription = null, tint = metric.accentColor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.tiles_sensor_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@WearPreviews
@Composable
private fun TileEditorScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            TileEditorScreen(
                available = SampleData.uiState.availableMetrics.toList(),
                tiles = WearMetric.DefaultTiles,
                onToggle = { _, _ -> },
            )
        }
    }
}
