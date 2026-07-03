package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.valueFor

@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val nutritionRepository: NutritionRepository,
) : CaffeineRepository {

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadCaffeineData(
        period: DatePeriod,
        refreshMode: RefreshMode,
    ): CaffeinePeriodData {
        val entries = nutritionRepository
            .loadNutritionEntries(
                start = period.start.minusDays(ModelingLookbackDays),
                end = period.end,
            )
            .mapNotNull { it.toCaffeineEntryOrNull() }
        return CaffeinePeriodData(entries = entries)
    }

    private fun NutritionEntry.toCaffeineEntryOrNull(): CaffeineEntry? {
        val caffeineGrams = valueFor(NutritionNutrient.CAFFEINE)
            ?.takeIf { it > 0.0 && it.isFinite() }
            ?: return null
        val caffeineMg = caffeineGrams * 1000.0
        return CaffeineEntry(
            id = id.ifBlank { clientRecordId ?: "${time.toEpochMilli()}-$caffeineMg" },
            startTime = time,
            endTime = endTime,
            caffeineMg = caffeineMg,
            name = name,
            source = source,
            mealType = mealType,
            clientRecordId = clientRecordId,
            isOpenVitalsEntry = isOpenVitalsEntry,
        )
    }

    private companion object {
        const val ModelingLookbackDays = 7L
    }
}
