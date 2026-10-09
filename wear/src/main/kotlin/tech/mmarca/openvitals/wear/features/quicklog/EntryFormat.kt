package tech.mmarca.openvitals.wear.features.quicklog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.HydrationColor
import tech.mmarca.openvitals.wear.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.wear.ui.theme.VitalsColor
import tech.mmarca.openvitals.wear.ui.theme.WeightColor

val EntryType.icon: ImageVector
    get() = when (this) {
        EntryType.WATER -> Icons.Outlined.WaterDrop
        EntryType.WEIGHT -> Icons.Outlined.MonitorWeight
        EntryType.BLOOD_PRESSURE -> Icons.Outlined.Speed
        EntryType.MINDFULNESS -> Icons.Outlined.SelfImprovement
        EntryType.HEART_RATE -> Icons.Outlined.Favorite
        EntryType.HRV -> Icons.Outlined.Insights
    }

val EntryType.color: Color
    get() = when (this) {
        EntryType.WATER -> HydrationColor
        EntryType.WEIGHT -> WeightColor
        EntryType.BLOOD_PRESSURE -> VitalsColor
        EntryType.MINDFULNESS -> MindfulnessColor
        EntryType.HEART_RATE -> HeartColor
        EntryType.HRV -> VitalsColor
    }

/** The entry's value with its unit, e.g. "72.4 kg" or "121/79 mmHg". */
@Composable
fun formatEntry(entry: LoggedEntry, unitSystem: UnitSystem): String = when (entry.type) {
    EntryType.WATER -> metricValue(WearMetric.HYDRATION, entry.value, unitSystem)
    EntryType.WEIGHT -> metricValue(WearMetric.WEIGHT, entry.value, unitSystem)
    EntryType.HEART_RATE -> metricValue(WearMetric.HEART_RATE, entry.value, unitSystem)
    EntryType.HRV -> metricValue(WearMetric.HRV, entry.value, unitSystem)
    EntryType.MINDFULNESS -> stringResource(R.string.quicklog_minutes, entry.value.roundToInt())
    EntryType.BLOOD_PRESSURE -> stringResource(
        R.string.quicklog_blood_pressure_value,
        formatCount(entry.value.roundToInt()),
        formatCount(entry.value2?.roundToInt() ?: 0),
    )
}

@Composable
private fun metricValue(metric: WearMetric, value: Double, unitSystem: UnitSystem): String =
    "${formatMetricValue(metric, value, unitSystem)} ${stringResource(metricUnitLabel(metric, unitSystem))}"
