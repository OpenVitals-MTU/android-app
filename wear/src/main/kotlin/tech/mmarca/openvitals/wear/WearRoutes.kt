package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wear.features.measure.Measurement

/** Navigation routes. Shared with the tiles, which open the app at some of them. */
object WearRoutes {
    const val DASHBOARD = "dashboard"
    const val TILE_EDITOR = "tile_editor"
    const val METRIC_ARG = "metric"
    const val METRIC = "metric/{$METRIC_ARG}"
    fun metric(metric: WearMetric) = "metric/${metric.name}"
    const val ACTIVITY_PICKER = "activity_picker"
    const val RECORDING = "recording"
    const val WORKOUT_SUMMARY = "workout_summary"
    const val SETTINGS = "settings"
    const val MEASURE = "measure"
    const val MEASUREMENT_ARG = "measurement"
    const val MEASUREMENT = "measure/{$MEASUREMENT_ARG}"
    fun measurement(measurement: Measurement) = "measure/${measurement.name}"
    const val QUICK_LOG = "quick_log"
    const val LOG_WEIGHT = "log_weight"
    const val LOG_BLOOD_PRESSURE = "log_blood_pressure"
    const val BREATHE = "breathe"

    /** Routes an outside caller, such as a tile, may open. Anything else is ignored. */
    fun isDeepLinkable(route: String): Boolean =
        route in setOf(QUICK_LOG, MEASURE, LOG_WEIGHT, LOG_BLOOD_PRESSURE, BREATHE) ||
            Measurement.entries.any { route == measurement(it) }
}
