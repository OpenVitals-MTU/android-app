package tech.mmarca.openvitals.features.manualentry.food

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.FoodRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomFood
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.FoodCategory
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class FoodEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val banana = CustomFood(
        id = "banana",
        name = "Banana",
        amountGrams = 120.0,
        nutrientValues = mapOf(
            NutritionNutrient.ENERGY to 105.0,
            NutritionNutrient.TOTAL_CARBOHYDRATE to 27.0,
        ),
        category = FoodCategory.FRUIT,
    )

    /** An in-memory catalog: saves and deletes show up on the next read. */
    private fun foodRepo(initial: List<CustomFood> = emptyList()) = mockk<FoodRepository>().also { repo ->
        val foods = initial.toMutableList()
        coEvery { repo.customFoods() } answers { foods.toList() }
        coEvery { repo.saveCustomFood(any()) } answers {
            val food = firstArg<CustomFood>()
            foods.removeAll { it.id == food.id }
            foods += food
        }
        coEvery { repo.deleteCustomFood(any()) } answers {
            val id = firstArg<String>()
            foods.removeAll { it.id == id }
        }
    }

    private fun nutritionRepo(
        canWrite: Boolean = true,
        todayEnergyKcal: Double = 0.0,
    ) = mockk<NutritionRepository>().also { repo ->
        every { repo.nutritionWritePermissions } returns setOf("write_nutrition")
        coEvery { repo.hasNutritionWritePermission() } returns canWrite
        coEvery { repo.writeNutritionEntry(any()) } returns "nutrition-record-id"
        coEvery { repo.loadDailyMacros(any(), any()) } returns listOf(
            DailyMacros(date = LocalDate.now(), nutrientValues = mapOf(NutritionNutrient.ENERGY to todayEnergyKcal)),
        )
    }

    @Test
    fun `initial load checks the permission, lists the foods and reads today's energy`() = runTest {
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutritionRepo(todayEnergyKcal = 450.0))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isCheckingPermission)
        assertTrue(state.canWrite)
        assertEquals(setOf("write_nutrition"), state.writePermissions)
        assertEquals(listOf(banana), state.foodOptions)
        assertEquals(450.0, state.todayEnergyKcal, 1e-9)
    }

    @Test
    fun `saveCustomFood stores the food and refreshes the list`() = runTest {
        val repo = foodRepo()
        val vm = FoodEntryViewModel(repo, nutritionRepo())
        advanceUntilIdle()

        vm.saveCustomFood(
            CustomFoodInput(
                name = " Oats ",
                amountGrams = 40.0,
                category = FoodCategory.GRAIN,
                nutrientValues = mapOf(NutritionNutrient.ENERGY to 150.0),
            ),
        )
        advanceUntilIdle()

        val saved = vm.uiState.value.foodOptions.single()
        assertEquals("Oats", saved.name)
        assertEquals(FoodCategory.GRAIN, saved.category)
        assertNull(vm.uiState.value.entryError)
        coVerify(exactly = 1) { repo.saveCustomFood(any()) }
    }

    @Test
    fun `saveCustomFood with an existing id keeps that id`() = runTest {
        val repo = foodRepo(listOf(banana))
        val vm = FoodEntryViewModel(repo, nutritionRepo())
        advanceUntilIdle()

        vm.saveCustomFood(
            CustomFoodInput(name = "Big banana", amountGrams = 150.0, nutrientValues = banana.nutrientValues),
            existingFoodId = banana.id,
        )
        advanceUntilIdle()

        val edited = vm.uiState.value.foodOptions.single()
        assertEquals("banana", edited.id)
        assertEquals("Big banana", edited.name)
    }

    @Test
    fun `saveCustomFood without a nutrient reports an invalid food and saves nothing`() = runTest {
        val repo = foodRepo()
        val vm = FoodEntryViewModel(repo, nutritionRepo())
        advanceUntilIdle()

        vm.saveCustomFood(CustomFoodInput(name = "Air", amountGrams = 10.0))
        advanceUntilIdle()

        assertEquals(FoodEntryError.INVALID_FOOD, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.saveCustomFood(any()) }
    }

    @Test
    fun `deleteCustomFood removes the food from the list`() = runTest {
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutritionRepo())
        advanceUntilIdle()

        vm.deleteCustomFood(banana)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.foodOptions.isEmpty())
    }

    @Test
    fun `logFood scales the nutrients to the portion and tags the food`() = runTest {
        val nutrition = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { nutrition.writeNutritionEntry(capture(request)) } returns "id"
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutrition)
        advanceUntilIdle()
        val eightAm = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()

        vm.logFood(banana, amountGrams = 60.0, entryTime = eightAm)
        advanceUntilIdle()

        val written = request.captured
        assertEquals("Banana", written.name)
        assertEquals("banana", written.foodId)
        assertEquals(eightAm, written.time)
        assertEquals(52.5, written.nutrientValues.getValue(NutritionNutrient.ENERGY), 1e-9)
        assertEquals(13.5, written.nutrientValues.getValue(NutritionNutrient.TOTAL_CARBOHYDRATE), 1e-9)
        assertTrue(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.isSavingEntry)
        // Today's counter moves with the portion, not the food.
        assertEquals(52.5, vm.uiState.value.todayEnergyKcal, 1e-9)
    }

    @Test
    fun `logFood on a past day leaves today's energy alone`() = runTest {
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutritionRepo(todayEnergyKcal = 300.0))
        advanceUntilIdle()

        vm.logFood(banana, entryTime = Instant.now().minusSeconds(3 * 24 * 3600))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveCompleted)
        assertEquals(300.0, vm.uiState.value.todayEnergyKcal, 1e-9)
    }

    @Test
    fun `logFood without write permission reports it and writes nothing`() = runTest {
        val nutrition = nutritionRepo(canWrite = false)
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutrition)
        advanceUntilIdle()

        vm.logFood(banana)
        advanceUntilIdle()

        assertEquals(FoodEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `logFood rejects an amount out of range, and a portion that overflows a nutrient`() = runTest {
        val nutrition = nutritionRepo()
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutrition)
        advanceUntilIdle()

        vm.logFood(banana, amountGrams = 0.0)
        assertEquals(FoodEntryError.INVALID_AMOUNT, vm.uiState.value.entryError)

        // 105 kcal per 120 g; 10 kg of banana is 8750 kcal, but 27 g carbs scales past what Health Connect takes.
        val dense = banana.copy(nutrientValues = mapOf(NutritionNutrient.ENERGY to 9000.0))
        vm.logFood(dense, amountGrams = 240.0)
        advanceUntilIdle()

        assertEquals(FoodEntryError.INVALID_AMOUNT, vm.uiState.value.entryError)
        coVerify(exactly = 0) { nutrition.writeNutritionEntry(any()) }
    }

    @Test
    fun `a failed write surfaces as a screen error`() = runTest {
        val nutrition = nutritionRepo()
        coEvery { nutrition.writeNutritionEntry(any()) } throws IllegalStateException("boom")
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutrition)
        advanceUntilIdle()

        vm.logFood(banana)
        advanceUntilIdle()

        assertEquals(FoodEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertTrue(vm.uiState.value.writeError is ScreenError)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test
    fun `onSaveCompletedHandled clears the flag`() = runTest {
        val vm = FoodEntryViewModel(foodRepo(listOf(banana)), nutritionRepo())
        advanceUntilIdle()
        vm.logFood(banana)
        advanceUntilIdle()

        vm.onSaveCompletedHandled()

        assertFalse(vm.uiState.value.saveCompleted)
    }
}
