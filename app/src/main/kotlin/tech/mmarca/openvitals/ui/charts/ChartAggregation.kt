package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode

/**
 * The "Aggregate charts" setting, as a CompositionLocal: the day charts sit
 * behind dozens of call sites. Static, since it changes only with the setting.
 */
val LocalChartAggregationMode = staticCompositionLocalOf { ChartAggregationMode.OFF }
