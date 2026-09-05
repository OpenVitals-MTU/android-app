package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import java.time.LocalDate

/** Lets a month heatmap cell drill into its Day view without threading a callback through every chart. */
val LocalMetricDayOpener: ProvidableCompositionLocal<((LocalDate) -> Unit)?> =
    compositionLocalOf { null }

/** The screen's week-period mode, so a chart can tell a calendar month from a rolling window. */
val LocalPeriodWeekMode: ProvidableCompositionLocal<WeekPeriodMode> =
    staticCompositionLocalOf { WeekPeriodMode.MONDAY_TO_SUNDAY }
