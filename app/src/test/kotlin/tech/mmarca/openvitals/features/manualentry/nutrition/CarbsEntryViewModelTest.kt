package tech.mmarca.openvitals.features.manualentry.nutrition

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CarbsEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `carbs entry writes grams value`() = runTest {
        val repo = nutritionRepo()
        val vm = CarbsEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateInput("42.5")
        vm.addEntry(42.5)
        advanceUntilIdle()

        coVerify {
            repo.writeCarbsEntry(match<NutritionWriteRequest> { request ->
                abs(request.carbsGrams - 42.5) < 0.001
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals("", vm.uiState.value.inputText)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `invalid carbs value does not write`() = runTest {
        val repo = nutritionRepo()
        val vm = CarbsEntryViewModel(repo)
        advanceUntilIdle()

        vm.addEntry(0.0)

        assertEquals(CarbsEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCarbsEntry(any()) }
    }

    @Test fun `missing write permission prevents carbs write`() = runTest {
        val repo = nutritionRepo(canWrite = false)
        val vm = CarbsEntryViewModel(repo)
        advanceUntilIdle()

        vm.addEntry(25.0)

        assertEquals(CarbsEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCarbsEntry(any()) }
    }

    @Test fun `metric carbs input stays grams`() {
        assertEquals(42.5, canonicalCarbsGrams("42.5", UnitSystem.METRIC)!!, 0.001)
    }

    @Test fun `imperial carbs input converts ounces to grams`() {
        assertEquals(28.3495, canonicalCarbsGrams("1", UnitSystem.IMPERIAL)!!, 0.001)
    }

    private fun nutritionRepo(
        canWrite: Boolean = true,
    ): NutritionRepository =
        mockk<NutritionRepository>().also { repo ->
            every { repo.nutritionWritePermissions } returns setOf("write_nutrition")
            coEvery { repo.hasNutritionWritePermission() } returns canWrite
            coEvery { repo.writeCarbsEntry(any()) } returns "record-id"
        }
}
