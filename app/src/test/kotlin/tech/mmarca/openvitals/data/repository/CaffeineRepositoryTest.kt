package tech.mmarca.openvitals.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient

class CaffeineRepositoryTest {

    @Test
    fun `loadCaffeinePeriod filters caffeine entries and uses lookback`() = runTest {
        val date = LocalDate.of(2026, 7, 1)
        val caffeineEntry = NutritionEntry(
            time = Instant.parse("2026-07-01T08:00:00Z"),
            endTime = Instant.parse("2026-07-01T08:10:00Z"),
            mealType = 0,
            name = "Coffee",
            energyKcal = null,
            proteinGrams = null,
            carbsGrams = null,
            fatGrams = null,
            fiberGrams = null,
            sugarGrams = null,
            source = "source.app",
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.095),
            id = "nutrition-1",
        )
        val nonCaffeineEntry = caffeineEntry.copy(
            id = "nutrition-2",
            nutrientValues = mapOf(NutritionNutrient.SUGAR to 8.0),
        )
        val nutritionRepository = mockk<NutritionRepository>()
        coEvery {
            nutritionRepository.loadNutritionEntries(any(), any())
        } returns listOf(caffeineEntry, nonCaffeineEntry)

        val result = CaffeineRepositoryImpl(nutritionRepository).loadCaffeinePeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = date)
        )

        assertEquals(1, result.entries.size)
        assertEquals("nutrition-1", result.entries.single().id)
        assertEquals(95.0, result.entries.single().caffeineMg, 0.001)
        assertEquals(caffeineEntry.endTime, result.entries.single().endTime)
        coVerify {
            nutritionRepository.loadNutritionEntries(date.minusDays(7), date)
        }
    }

    @Test
    fun `loadCaffeinePeriod returns empty list when Health Connect has no caffeine`() = runTest {
        val date = LocalDate.of(2026, 7, 1)
        val nutritionRepository = mockk<NutritionRepository>()
        coEvery {
            nutritionRepository.loadNutritionEntries(any(), any())
        } returns emptyList()

        val result = CaffeineRepositoryImpl(nutritionRepository).loadCaffeinePeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = date)
        )

        assertTrue(result.entries.isEmpty())
    }
}
