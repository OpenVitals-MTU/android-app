package tech.mmarca.openvitals.wear.features.quicklog

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import java.util.Date
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearUiState
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.HydrationColor
import tech.mmarca.openvitals.wear.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.VitalsColor
import tech.mmarca.openvitals.wear.ui.theme.WeightColor

/** Millilitres one tap on "water" adds: a glass. */
const val WaterServingMl = 250.0

private const val RecentEntries = 6

/**
 * Fast entries, the watch side of the phone's manual entry: one tap for a
 * glass of water, a stepper for weight and blood pressure, a guided breath.
 * The latest entries are listed underneath so a mistake is easy to spot.
 */
@Composable
fun QuickLogScreen(
    state: WearUiState,
    onAddWater: () -> Unit,
    onLogWeight: () -> Unit,
    onLogBloodPressure: () -> Unit,
    onBreathe: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val unitSystem = state.preferences.unitSystem
    var showSaved by remember { mutableStateOf(false) }
    val lastWeight = state.entries.lastOrNull { it.type == EntryType.WEIGHT }
    val lastPressure = state.entries.lastOrNull { it.type == EntryType.BLOOD_PRESSURE }
    val waterToday = state.metrics[WearMetric.HYDRATION]?.current ?: 0.0
    val waterUnit = stringResource(metricUnitLabel(WearMetric.HYDRATION, unitSystem))
    val notLogged = stringResource(R.string.quicklog_not_logged)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.quicklog_title))
                }
            }
            item {
                QuickLogButton(
                    label = stringResource(
                        R.string.quicklog_add_water,
                        formatMetricValue(WearMetric.HYDRATION, WaterServingMl, unitSystem),
                        waterUnit,
                    ),
                    secondary = stringResource(
                        R.string.quicklog_water_today,
                        formatMetricValue(WearMetric.HYDRATION, waterToday, unitSystem),
                        formatMetricValue(WearMetric.HYDRATION, state.preferences.waterGoalMl.toDouble(), unitSystem),
                        waterUnit,
                    ),
                    icon = Icons.Outlined.WaterDrop,
                    color = HydrationColor,
                    onClick = {
                        onAddWater()
                        showSaved = true
                    },
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                QuickLogButton(
                    label = stringResource(R.string.metric_weight),
                    secondary = lastWeight?.let { formatEntry(it, unitSystem) } ?: notLogged,
                    icon = Icons.Outlined.MonitorWeight,
                    color = WeightColor,
                    onClick = onLogWeight,
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                QuickLogButton(
                    label = stringResource(R.string.quicklog_blood_pressure),
                    secondary = lastPressure?.let { formatEntry(it, unitSystem) } ?: notLogged,
                    icon = Icons.Outlined.Speed,
                    color = VitalsColor,
                    onClick = onLogBloodPressure,
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            item {
                QuickLogButton(
                    label = stringResource(R.string.breathe_title),
                    secondary = stringResource(R.string.breathe_description),
                    icon = Icons.Outlined.SelfImprovement,
                    color = MindfulnessColor,
                    onClick = onBreathe,
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
            val recent = state.entries.takeLast(RecentEntries).asReversed()
            if (recent.isNotEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.quicklog_recent)) } }
                items(recent) { entry -> RecentEntryRow(entry, unitSystem) }
            }
        }
    }

    SuccessConfirmationDialog(
        visible = showSaved,
        onDismissRequest = { showSaved = false },
        curvedText = null,
    )
}

@Composable
private fun QuickLogButton(
    label: String,
    secondary: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
) {
    Button(
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = { Text(secondary) },
        icon = { Icon(icon, contentDescription = null, tint = color) },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
    )
}

@Composable
private fun RecentEntryRow(entry: LoggedEntry, unitSystem: UnitSystem) {
    val context = LocalContext.current
    val time = remember(entry.timeMillis) { DateFormat.getTimeFormat(context).format(Date(entry.timeMillis)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(entry.type.icon, contentDescription = null, tint = entry.type.color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatEntry(entry, unitSystem),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@WearPreviews
@Composable
private fun QuickLogScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            QuickLogScreen(SampleData.uiState, {}, {}, {}, {})
        }
    }
}
