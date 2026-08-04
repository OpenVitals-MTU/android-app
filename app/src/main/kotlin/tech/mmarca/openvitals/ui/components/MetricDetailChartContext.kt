package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import java.time.LocalDate

/**
 * Provided by [MetricDetailScaffold] so a month heatmap cell — buried deep inside
 * a screen's content — can drill into that day's Day view without every screen
 * threading a navigation callback down through its charts.
 *
 * When present, a tapped month cell opens the day; when absent (a preview, a
 * chart used outside the scaffold) the cell falls back to whatever
 * `onDateSelected` the host passed.
 */
val LocalMetricDayOpener: ProvidableCompositionLocal<((LocalDate) -> Unit)?> =
    compositionLocalOf { null }

/**
 * The week-period mode of the screen the chart is drawn in, so a chart can tell a
 * calendar month from a rolling "Last 30 days" window. Threading it through every
 * chart signature would touch every metric screen for a value only the heatmap
 * reads, and the scaffold already owns the period selection it comes from.
 */
val LocalPeriodWeekMode: ProvidableCompositionLocal<WeekPeriodMode> =
    staticCompositionLocalOf { WeekPeriodMode.MONDAY_TO_SUNDAY }
