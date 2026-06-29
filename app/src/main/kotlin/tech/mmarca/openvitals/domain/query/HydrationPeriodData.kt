package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry

data class HydrationPeriodData(
    val dailyHydration: List<DailyHydration> = emptyList(),
    val previousDailyHydration: List<DailyHydration> = emptyList(),
    val baselineDailyHydration: List<DailyHydration> = emptyList(),
    val hydrationEntries: List<HydrationEntry> = emptyList(),
)
