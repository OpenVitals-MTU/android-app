package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.valueFor

@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val nutritionRepository: NutritionRepository,
) : CaffeineRepository {

    override suspend fun loadCaffeinePeriod(
        query: PeriodLoadQuery,
    ): CaffeinePeriodData = coroutineScope {
        val windows = query.windows
        val current = async { loadCaffeineData(windows.current) }
        // Only a total is needed, so the daily aggregate beats reading every record.
        val previousTotalMg = async {
            nutritionRepository
                .loadDailyMacros(windows.previous.start, windows.previous.end)
                .sumOf { it.nutrientValues[NutritionNutrient.CAFFEINE] ?: 0.0 } * 1000.0
        }
        current.await().copy(previousTotalMg = previousTotalMg.await())
    }

    override suspend fun loadCaffeineData(
        period: DatePeriod,
    ): CaffeinePeriodData {
        val entries = nutritionRepository
            .loadNutritionEntries(
                start = period.start.minusDays(ModelingLookbackDays),
                end = period.end,
            )
            .mapNotNull { it.toCaffeineEntry() }
        return CaffeinePeriodData(entries = entries)
    }

    private companion object {
        const val ModelingLookbackDays = 7L
    }
}

/** Shared with the dashboard's active-caffeine read. */
internal fun NutritionEntry.toCaffeineEntry(): CaffeineEntry? {
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
