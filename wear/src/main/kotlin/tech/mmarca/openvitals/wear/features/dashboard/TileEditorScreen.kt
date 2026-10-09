package tech.mmarca.openvitals.wear.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

/**
 * Adds, removes and reorders dashboard tiles. Active tiles are reordered by
 * long-press and drag; tapping a row switches it on or off. Only [available]
 * metrics are listed: a metric the watch has no sensor for cannot be switched on.
 *
 * This uses a plain [LazyColumn] rather than a TransformingLazyColumn, because
 * the reorder library only drives a LazyListState.
 */
@Composable
fun TileEditorScreen(
    available: List<WearMetric>,
    tiles: List<WearMetric>,
    onToggle: (WearMetric, Boolean) -> Unit,
    onMove: (moved: WearMetric, target: WearMetric) -> Unit,
    onReset: () -> Unit,
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val inactive = available.filterNot { it in tiles }
    // Only active tiles are ReorderableItems, so a drag never lands on a header
    // or on an inactive row.
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val moved = from.key as? WearMetric
        val target = to.key as? WearMetric
        if (moved != null && target != null) onMove(moved, target)
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "title") {
                ListHeader { Text(stringResource(R.string.tiles_title)) }
            }

            if (tiles.isNotEmpty()) {
                item(key = "active_header") {
                    ListHeader { Text(stringResource(R.string.tiles_active_header)) }
                }
                items(tiles, key = { it }) { metric ->
                    ReorderableItem(reorderState, key = metric) { isDragging ->
                        TileSwitch(
                            metric = metric,
                            checked = true,
                            onCheckedChange = { onToggle(metric, it) },
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                )
                                .scale(if (isDragging) 1.05f else 1f),
                        )
                    }
                }
                item(key = "reorder_hint") {
                    Hint(stringResource(R.string.tiles_reorder_hint))
                }
            }

            if (inactive.isNotEmpty()) {
                item(key = "inactive_header") {
                    ListHeader { Text(stringResource(R.string.tiles_available_header)) }
                }
                items(inactive, key = { it }) { metric ->
                    TileSwitch(
                        metric = metric,
                        checked = false,
                        onCheckedChange = { onToggle(metric, it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item(key = "reset") {
                Button(
                    onClick = onReset,
                    label = { Text(stringResource(R.string.tiles_reset_default)) },
                    icon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    colors = ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "sensor_hint") {
                Hint(stringResource(R.string.tiles_sensor_hint))
            }
        }
    }
}

@Composable
private fun TileSwitch(
    metric: WearMetric,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(stringResource(metric.label)) },
        icon = { Icon(metric.icon, contentDescription = null, tint = metric.accentColor) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
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
                onMove = { _, _ -> },
                onReset = {},
            )
        }
    }
}
