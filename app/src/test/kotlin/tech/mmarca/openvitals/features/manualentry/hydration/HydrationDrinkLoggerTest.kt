package tech.mmarca.openvitals.features.manualentry.hydration

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest

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

    @Test
    fun `a drink sipped over time ends its nutrition record that long after it started`() = runTest {
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)
        val request = slot<NutritionWriteRequest>()
        coEvery { nutrition.writeNutritionEntry(capture(request)) } returns "nutrition-1"
        val startedAt = Instant.now().minus(Duration.ofMinutes(30))

        writeHydrationAndNutritionEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            rawLiters = 0.473,
            hydrationMultiplier = 1.0,
            nutritionName = "Monster",
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.16),
            requestedEntryTime = startedAt,
            consumptionDurationMinutes = 120,
            canWriteHydration = true,
            canWriteNutrition = true,
        )

        // The caffeine model spreads the dose across the record's interval, so the end
        // time — not a separate field — is what carries "drank over two hours".
        assertEquals(startedAt, request.captured.time)
        assertEquals(startedAt.plus(Duration.ofHours(2)), request.captured.endTime)
    }

    @Test
    fun `a drink taken at once leaves the nutrition end time unset`() = runTest {
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)
        val request = slot<NutritionWriteRequest>()
        coEvery { nutrition.writeNutritionEntry(capture(request)) } returns "nutrition-1"

        writeHydrationAndNutritionEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            rawLiters = 0.25,
            hydrationMultiplier = 1.0,
            nutritionName = "Espresso",
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 0.06),
            consumptionDurationMinutes = null,
            canWriteHydration = true,
            canWriteNutrition = true,
        )

        assertNull(request.captured.endTime)
    }
}
