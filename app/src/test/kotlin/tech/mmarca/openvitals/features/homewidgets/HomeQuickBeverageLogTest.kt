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
 * Dart counterpart: test/features/homewidgets/home_widget_beverage_log_test.dart.
 *
 * What a quick-beverage widget tap actually writes. The Glance action itself
 * needs a device, but everything the tap decides — which records land, which
 * permission refuses the whole drink, what the tile is told afterwards — is
 * [logCustomHydrationDrinkEntry] over two repository interfaces, which is
 * plain JVM code. The parity sheet marks these rows "instrumentation-only, no
 * JVM seam"; this file is the seam.
 *
 * An espresso is the drink that catches a naive log path: it hydrates at half
 * strength AND carries caffeine, so a path that writes only the hydration
 * record silently drops the caffeine.
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
        // volumeLiters * hydrationMultiplier — 30ml of espresso at 50% impact.
        assertThat(hydrationRequest.captured.volumeLiters).isWithin(1e-9).of(0.015)
        assertThat(hydrationRequest.captured.drinkId).isEqualTo("espresso")
        // The bug a hydration-only path would cause: the caffeine is dropped. It
        // must ride along, paired to the hydration record it belongs to.
        assertThat(nutritionRequest.captured.nutrientValues)
            .containsExactly(NutritionNutrient.CAFFEINE, 63.0)
        assertThat(nutritionRequest.captured.name).isEqualTo("Espresso")
        assertThat(nutritionRequest.captured.associatedHydrationClientRecordId)
            .isEqualTo("openvitals_hydration_1_drink_espresso_uuid")
    }

    @Test
    fun `a nutrition-only drink writes no hydration record`() = runTest {
        // A zero-multiplier drink records the caffeine and nothing else, and says
        // so — that is what puts "Saved as nutrition" on the tile rather than the
        // plain "Saved", and what leaves the hydration reminder alone.
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
        // Rejecting the entry beats logging the water and losing the caffeine:
        // a half-written drink cannot be told apart from a whole one later.
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
        // The stored selection outlives the drink: delete the beverage in the app
        // and the widget still carries its id. The tap must refuse rather than
        // write a nameless record.
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
        // HomeQuickBeverageLogAction wraps the call in runCatching and puts
        // "Unable to update" on the tile; the contract it relies on is that a
        // failed write propagates rather than being swallowed as a success.
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
