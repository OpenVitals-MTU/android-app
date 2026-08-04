package tech.mmarca.openvitals.features.cycle

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate

@Immutable
data class CycleDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val hasData: Boolean = false,
    val summary: CyclePeriodSummary = CyclePeriodSummary(),
    val calendarDays: List<CycleDay> = emptyList(),
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
    val sources: List<String> = emptyList(),
)

@Immutable
data class CyclePeriodSummary(
    val periodDays: Int = 0,
    val ovulationTestCount: Int = 0,
    val bbtReadingCount: Int = 0,
    val totalEntryCount: Int = 0,
    val latestBbtCelsius: Double? = null,
    val latestBbtMeasurementLocation: Int = 0,
)
