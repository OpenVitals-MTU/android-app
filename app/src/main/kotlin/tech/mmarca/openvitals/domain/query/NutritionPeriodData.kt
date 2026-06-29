package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry

data class NutritionPeriodData(
    val dailyMacros: List<DailyMacros> = emptyList(),
    val previousDailyMacros: List<DailyMacros> = emptyList(),
    val baselineDailyMacros: List<DailyMacros> = emptyList(),
    val entries: List<NutritionEntry> = emptyList(),
)
