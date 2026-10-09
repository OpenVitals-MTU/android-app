package tech.mmarca.openvitals.wear

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.wear.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.wear.ui.theme.DistanceColor
import tech.mmarca.openvitals.wear.ui.theme.ElevationColor
import tech.mmarca.openvitals.wear.ui.theme.FloorsColor
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.HydrationColor
import tech.mmarca.openvitals.wear.ui.theme.SleepColor
import tech.mmarca.openvitals.wear.ui.theme.StepsColor
import tech.mmarca.openvitals.wear.ui.theme.VitalsColor
import tech.mmarca.openvitals.wear.ui.theme.WeightColor
import tech.mmarca.openvitals.wear.ui.theme.WorkoutColor

/** How a metric's readings add up, which decides its chart and its stats. */
enum class MetricKind {
    /** A running total for the day: bars per hour, goal and daily averages. */
    CUMULATIVE,

    /** A value sampled over time: a line, with low, average and high. */
    SAMPLED,
}

/** Where a metric's readings come from, which decides whether a watch can offer it. */
enum class MetricSource {
    /** Measured by this watch. Offered only when its sensor is there. */
    SENSOR,

    /** Measured elsewhere and synced from the phone. Always offered. */
    PHONE,

    /** Entered by hand on the watch. Always offered. */
    LOGGED,
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
    val source: MetricSource = MetricSource.SENSOR,
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

    /** ml/kg/min. Estimated by the watch during outdoor workouts. */
    VO2_MAX(R.string.metric_vo2_max, Icons.Outlined.Speed, WorkoutColor, MetricKind.SAMPLED),

    /** Beats per minute, one value per day. */
    RESTING_HEART_RATE(
        R.string.metric_resting_heart_rate,
        Icons.Outlined.MonitorHeart,
        HeartColor,
        MetricKind.SAMPLED,
        MetricSource.PHONE,
    ),

    /** Milliseconds RMSSD. Synced, or measured on a watch that reports single beats. */
    HRV(R.string.metric_hrv, Icons.Outlined.Insights, VitalsColor, MetricKind.SAMPLED, MetricSource.PHONE),

    /** Minutes asleep. The stages arrive as `WearUiState.sleepStages`. */
    SLEEP(R.string.metric_sleep, Icons.Outlined.Bedtime, SleepColor, MetricKind.CUMULATIVE, MetricSource.PHONE),

    /** Millilitres. */
    HYDRATION(
        R.string.metric_hydration,
        Icons.Outlined.WaterDrop,
        HydrationColor,
        MetricKind.CUMULATIVE,
        MetricSource.LOGGED,
    ),

    /** Kilograms. */
    WEIGHT(R.string.metric_weight, Icons.Outlined.MonitorWeight, WeightColor, MetricKind.SAMPLED, MetricSource.LOGGED),
    ;

    companion object {
        /** The dashboard before the user has edited it. */
        val DefaultTiles = listOf(STEPS, HEART_RATE, SLEEP, ACTIVE_CALORIES, HYDRATION)
    }
}
