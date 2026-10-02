package tech.mmarca.openvitals.wear

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.wear.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.wear.ui.theme.DistanceColor
import tech.mmarca.openvitals.wear.ui.theme.ElevationColor
import tech.mmarca.openvitals.wear.ui.theme.FloorsColor
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.StepsColor
import tech.mmarca.openvitals.wear.ui.theme.VitalsColor

/** How a metric's readings add up, which decides its chart and its stats. */
enum class MetricKind {
    /** A running total for the day: bars per hour, goal and daily averages. */
    CUMULATIVE,

    /** A value sampled over time: a line, with low, average and high. */
    SAMPLED,
}

/**
 * Everything the watch can show as a dashboard tile and a detail screen.
 * Which of these a given watch offers depends on its sensors and arrives as
 * `WearUiState.availableMetrics`. Adding a metric here is all the UI needs:
 * the tile, the tile editor and the detail screen are driven by this enum.
 *
 * Readings are stored in the base unit noted per entry; the unit system
 * only changes how they are displayed.
 */
enum class WearMetric(
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val kind: MetricKind,
) {
    /** Count. */
    STEPS(R.string.metric_steps, Icons.AutoMirrored.Outlined.DirectionsWalk, StepsColor, MetricKind.CUMULATIVE),

    /** Beats per minute. */
    HEART_RATE(R.string.metric_heart_rate, Icons.Outlined.Favorite, HeartColor, MetricKind.SAMPLED),

    /** Metres. */
    DISTANCE(R.string.metric_distance, Icons.Outlined.Straighten, DistanceColor, MetricKind.CUMULATIVE),

    /** Kilocalories. */
    ACTIVE_CALORIES(
        R.string.metric_active_calories,
        Icons.Outlined.LocalFireDepartment,
        ActiveCaloriesColor,
        MetricKind.CUMULATIVE,
    ),

    /** Count. Needs a barometer. */
    FLOORS(R.string.metric_floors, Icons.Outlined.Stairs, FloorsColor, MetricKind.CUMULATIVE),

    /** Metres gained. Needs a barometer. */
    ELEVATION(R.string.metric_elevation, Icons.Outlined.Terrain, ElevationColor, MetricKind.CUMULATIVE),

    /** Percent. Needs a vendor SpO2 sensor. */
    BLOOD_OXYGEN(R.string.metric_blood_oxygen, Icons.Outlined.Bloodtype, VitalsColor, MetricKind.SAMPLED),
    ;

    companion object {
        /** The dashboard before the user has edited it. */
        val DefaultTiles = listOf(STEPS, HEART_RATE, DISTANCE, ACTIVE_CALORIES)
    }
}
