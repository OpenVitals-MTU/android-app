package tech.mmarca.openvitals.features.homewidgets

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogOutcome
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationEntryError
import tech.mmarca.openvitals.features.manualentry.hydration.logCustomHydrationDrinkEntry

/**
 * What a quick-beverage widget tap writes: [logCustomHydrationDrinkEntry] over two repositories.
 * An espresso hydrates at half strength and carries caffeine, so a hydration-only path drops the caffeine.
 */
class HomeQuickBeverageLogTest {
    private val espresso = CustomHydrationDrink(
        id = "espresso",
        name = "Espresso",
        volumeMilliliters = 30.0,
        hydrationMultiplier = 0.5,
        nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 63.0),
    )

    @Test
    fun `logs the hydration AND the nutrition entry for a drink with caffeine`() = runTest {
        val hydration = mockk<HydrationRepository>(relaxed = true)
        coEvery { hydration.writeHydrationEntry(any()) } returns
            "openvitals_hydration_1_drink_espresso_uuid"
        val nutrition = mockk<NutritionRepository>(relaxed = true)
        val hydrationRequest = slot<HydrationWriteRequest>()
        val nutritionRequest = slot<NutritionWriteRequest>()

        val outcome = logCustomHydrationDrinkEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            drink = espresso,
            canWriteHydration = true,
            canWriteNutrition = true,
        )

        assertThat(outcome).isInstanceOf(HydrationDrinkLogOutcome.Success::class.java)
        coVerify(exactly = 1) { hydration.writeHydrationEntry(capture(hydrationRequest)) }
        coVerify(exactly = 1) { nutrition.writeNutritionEntry(capture(nutritionRequest)) }
        // volumeLiters * hydrationMultiplier: 30 ml at 50%.
        assertThat(hydrationRequest.captured.volumeLiters).isWithin(1e-9).of(0.015)
        assertThat(hydrationRequest.captured.drinkId).isEqualTo("espresso")
        // The caffeine must ride along, paired to the hydration record.
        assertThat(nutritionRequest.captured.nutrientValues)
            .containsExactly(NutritionNutrient.CAFFEINE, 63.0)
        assertThat(nutritionRequest.captured.name).isEqualTo("Espresso")
        assertThat(nutritionRequest.captured.associatedHydrationClientRecordId)
            .isEqualTo("openvitals_hydration_1_drink_espresso_uuid")
    }

    @Test
    fun `a nutrition-only drink writes no hydration record`() = runTest {
        // A zero-multiplier drink records the caffeine only, puts "Saved as nutrition" on the tile,
        // and leaves the hydration reminder alone.
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)

        val outcome = logCustomHydrationDrinkEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            drink = espresso.copy(hydrationMultiplier = 0.0),
            canWriteHydration = true,
            canWriteNutrition = true,
        )

        val success = (outcome as HydrationDrinkLogOutcome.Success).value
        assertThat(success.wroteHydration).isFalse()
        assertThat(success.wroteNutrition).isTrue()
        assertThat(success.effectiveLiters).isEqualTo(0.0)
        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        coVerify(exactly = 1) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `a missing hydration permission writes nothing`() = runTest {
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)

        val outcome = logCustomHydrationDrinkEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            drink = espresso,
            canWriteHydration = false,
            canWriteNutrition = true,
        )

        assertThat((outcome as HydrationDrinkLogOutcome.Invalid).error)
            .isEqualTo(HydrationEntryError.MISSING_WRITE_PERMISSION)
        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `a missing nutrition permission blocks the whole drink`() = runTest {
        // Rejecting beats a half-written drink that cannot be told apart from a whole one.
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)

        val outcome = logCustomHydrationDrinkEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            drink = espresso,
            canWriteHydration = true,
            canWriteNutrition = false,
        )

        assertThat((outcome as HydrationDrinkLogOutcome.Invalid).error)
            .isEqualTo(HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION)
        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `a drink that is no longer valid is refused before any write`() = runTest {
        // The widget still carries a deleted beverage's id. The tap must refuse rather than write a nameless record.
        val hydration = mockk<HydrationRepository>(relaxed = true)
        val nutrition = mockk<NutritionRepository>(relaxed = true)

        val outcome = logCustomHydrationDrinkEntry(
            repository = hydration,
            nutritionRepository = nutrition,
            drink = espresso.copy(name = ""),
            canWriteHydration = true,
            canWriteNutrition = true,
        )

        assertThat((outcome as HydrationDrinkLogOutcome.Invalid).error)
            .isEqualTo(HydrationEntryError.INVALID_CUSTOM_DRINK)
        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `a failed write surfaces as a throw the tile can report`() = runTest {
        // A failed write must propagate, so HomeQuickBeverageLogAction can show "Unable to update".
        val hydration = mockk<HydrationRepository>(relaxed = true)
        coEvery { hydration.writeHydrationEntry(any()) } throws
            IllegalStateException("Health Connect is unavailable")
        val nutrition = mockk<NutritionRepository>(relaxed = true)

        val thrown = runCatching {
            logCustomHydrationDrinkEntry(
                repository = hydration,
                nutritionRepository = nutrition,
                drink = espresso,
                canWriteHydration = true,
                canWriteNutrition = true,
            )
        }.exceptionOrNull()

        assertThat(thrown).hasMessageThat().isEqualTo("Health Connect is unavailable")
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }
}
