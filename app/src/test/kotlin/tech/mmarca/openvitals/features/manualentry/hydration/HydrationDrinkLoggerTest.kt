package tech.mmarca.openvitals.features.manualentry.hydration

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionNutrient

/**
 * Dart counterpart: test/domain/usecase/save_hydration_entry_use_case_test.dart.
 *
 * Flutter's `SaveHydrationEntryUseCase` is [writeHydrationAndNutritionEntry]
 * here; the two-record rollback it guards is the same one.
 */
class HydrationDrinkLoggerTest {

    @Test
    fun `rolls the hydration half back when the nutrition write fails`() = runTest {
        val hydration = mockk<HydrationRepository>(relaxed = true)
        coEvery { hydration.writeHydrationEntry(any()) } returns "client-123"
        val nutrition = mockk<NutritionRepository>(relaxed = true)
        coEvery { nutrition.writeNutritionEntry(any()) } throws
            IllegalStateException("nutrition write failed")

        val thrown = runCatching {
            writeHydrationAndNutritionEntry(
                repository = hydration,
                nutritionRepository = nutrition,
                rawLiters = 0.25,
                hydrationMultiplier = 1.0,
                nutritionName = null,
                nutrientValues = mapOf(NutritionNutrient.PROTEIN to 10.0),
                canWriteHydration = true,
                canWriteNutrition = true,
            )
        }.exceptionOrNull()

        assertEquals("nutrition write failed", thrown?.message)
        // The just-written hydration record is deleted by its clientRecordId, so a
        // retry cannot leave a duplicate hydration entry with no nutrition.
        coVerify(exactly = 1) { hydration.deleteHydrationEntryByClientRecordId("client-123") }
    }
}
