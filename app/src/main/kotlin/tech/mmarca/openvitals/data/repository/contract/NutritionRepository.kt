package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.NutritionPeriodData

interface NutritionRepository {
    val nutritionWritePermissions: Set<String>

    suspend fun loadNutritionPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): NutritionPeriodData

    suspend fun loadDailyMacros(start: LocalDate, end: LocalDate): List<DailyMacros>

    suspend fun loadNutritionEntries(start: LocalDate, end: LocalDate): List<NutritionEntry>

    suspend fun hasNutritionWritePermission(): Boolean

    suspend fun writeCarbsEntry(request: NutritionWriteRequest): String

    suspend fun writeNutritionEntry(request: NutritionWriteRequest): String

    suspend fun deleteNutritionEntry(id: String)
}
