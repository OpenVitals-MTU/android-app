package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode

/**
 * How the intraday vitals charts should summarise their data — the "Aggregate
 * charts" setting, delivered as a CompositionLocal (the [LocalMetricSectionEditMode]
 * precedent) because the day charts sit behind dozens of call sites and threading a
 * parameter through every one of them would touch far more code than the feature is
 * worth. Provided once at the app root from the preferences flow; consumers read it
 * where the points are built.
 *
 * Static because it changes only when the user flips the setting — a full
 * recomposition then is exactly right.
 */
val LocalChartAggregationMode = staticCompositionLocalOf { ChartAggregationMode.OFF }
