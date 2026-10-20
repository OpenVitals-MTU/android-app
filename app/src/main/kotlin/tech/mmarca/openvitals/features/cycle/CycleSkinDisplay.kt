package tech.mmarca.openvitals.features.cycle

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.ZoneId
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureNight
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureNights
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureShiftCalculator
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureShiftResult
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry

/**
 * The current cycle's night skin temperature and the retrospective verdict on it.
 * It is drawn beside the morning temperatures and never mixed with them; nothing here
 * reaches the estimate.
 */
@Immutable
data class CycleSkinDisplay(
    val cycleStart: LocalDate,
    /** The nights of the source the verdict rests on, or of the source with most nights. */
    val nights: List<SkinTemperatureNight>,
    val source: String?,
    val result: SkinTemperatureShiftResult,
)

internal fun skinDisplay(
    statistics: CycleStatistics?,
    entries: List<SkinTemperatureEntry>,
    zone: ZoneId,
): CycleSkinDisplay? {
    val cycle = statistics?.currentCycle ?: return null
    val nights = SkinTemperatureNights.fromEntries(entries, zone).filter { !it.date.isBefore(cycle.startDate) }
    if (nights.isEmpty()) return null
    // The current cycle has not ended: its next period start is unknown.
    val analysis = SkinTemperatureShiftCalculator.evaluateCycle(cycle.startDate, null, nights)
    val shown = analysis.source ?: nights.groupingBy { it.source }.eachCount().maxBy { it.value }.key
    return CycleSkinDisplay(
        cycleStart = cycle.startDate,
        nights = nights.filter { it.source == shown },
        source = shown,
        result = analysis.result,
    )
}
