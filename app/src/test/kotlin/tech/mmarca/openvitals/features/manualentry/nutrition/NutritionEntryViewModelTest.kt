package tech.mmarca.openvitals.features.manualentry.nutrition

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
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
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `a command at rest is idle`() = runTest {
        val vm = NutritionEntryViewModel(nutritionRepo())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `the form opens on energy, protein, fat and carbs`() = runTest {
        val vm = NutritionEntryViewModel(nutritionRepo())

        assertEquals(PrimaryNutritionEntryNutrients, vm.uiState.value.rows.map { it.nutrient })
    }

    @Test fun `caffeine is logged as a drink, so it cannot be added here`() = runTest {
        val vm = NutritionEntryViewModel(nutritionRepo())

        assertFalse(NutritionNutrient.CAFFEINE in vm.uiState.value.addableNutrients)
        vm.addNutrient(NutritionNutrient.CAFFEINE)

        assertFalse(vm.uiState.value.rows.any { it.nutrient == NutritionNutrient.CAFFEINE })
    }

    @Test fun `every other Health Connect nutrient can be added`() = runTest {
        val vm = NutritionEntryViewModel(nutritionRepo())

        assertEquals(
            NutritionNutrient.entries.toSet() - PrimaryNutritionEntryNutrients.toSet() - NutritionNutrient.CAFFEINE,
            vm.uiState.value.addableNutrients.toSet(),
        )
    }

    @Test fun `an added nutrient can be removed, a main one cannot`() = runTest {
        val vm = NutritionEntryViewModel(nutritionRepo())

        vm.addNutrient(NutritionNutrient.DIETARY_FIBER)
        assertFalse(NutritionNutrient.DIETARY_FIBER in vm.uiState.value.addableNutrients)
        vm.removeNutrient(NutritionNutrient.DIETARY_FIBER)
        vm.removeNutrient(NutritionNutrient.PROTEIN)

        assertEquals(PrimaryNutritionEntryNutrients, vm.uiState.value.rows.map { it.nutrient })
        assertTrue(NutritionNutrient.DIETARY_FIBER in vm.uiState.value.addableNutrients)
    }

    @Test fun `filled fields go into one record and blank ones are left out`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.ENERGY, "2150")
        vm.updateAmount(NutritionNutrient.PROTEIN, "120,5")
        vm.addNutrient(NutritionNutrient.SODIUM)
        vm.updateAmount(NutritionNutrient.SODIUM, "2.3")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.writeNutritionEntry(any()) }
        assertEquals(
            mapOf(
                NutritionNutrient.ENERGY to 2150.0,
                NutritionNutrient.PROTEIN to 120.5,
                NutritionNutrient.SODIUM to 2.3,
            ),
            request.captured.nutrientValues,
        )
        assertTrue(request.captured.isManualNutritionEntry)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertEquals(PrimaryNutritionEntryNutrients, vm.uiState.value.rows.map { it.nutrient })
        assertTrue(vm.uiState.value.rows.all { it.amountText.isEmpty() })

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `a backdated entry is written at the chosen time`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()
        val lastMonday = Instant.parse("2026-09-28T19:00:00Z")

        vm.updateTimestamp(lastMonday)
        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "240")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(lastMonday, request.captured.time)
        assertNull(vm.uiState.value.timestamp)
    }

    @Test fun `a future time is written as now`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateTimestamp(Instant.now().plusSeconds(3_600))
        vm.updateAmount(NutritionNutrient.PROTEIN, "30")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertFalse(request.captured.time.isAfter(Instant.now()))
    }

    @Test fun `an empty form does not write`() = runTest {
        val repo = nutritionRepo()
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.addEntry(UnitSystem.METRIC)

        assertEquals(NutritionEntryError.NO_VALUES, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `one invalid field stops the whole entry`() = runTest {
        val repo = nutritionRepo()
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.ENERGY, "2000")
        vm.updateAmount(NutritionNutrient.TOTAL_FAT, "0")
        vm.addEntry(UnitSystem.METRIC)

        assertEquals(NutritionEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `missing write permission prevents the write`() = runTest {
        val repo = nutritionRepo(canWrite = false)
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.PROTEIN, "25")
        vm.addEntry(UnitSystem.METRIC)

        assertEquals(NutritionEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `a failed save carries the failure to the form, not an exception`() = runTest {
        val repo = nutritionRepo()
        coEvery { repo.writeNutritionEntry(any()) } throws RuntimeException("the provider hung up")
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "45")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(NutritionEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("the provider hung up"), vm.uiState.value.writeError)
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertEquals("45", vm.uiState.value.rows.single { it.nutrient == NutritionNutrient.TOTAL_CARBOHYDRATE }.amountText)
    }

    @Test fun `editing a field clears the failure the last attempt left behind`() = runTest {
        val repo = nutritionRepo()
        coEvery { repo.writeNutritionEntry(any()) } throws RuntimeException("boom")
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()
        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "45")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()
        assertEquals(NutritionEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("boom"), vm.uiState.value.writeError)

        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "50")

        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `imperial gram nutrients are typed in ounces and written in grams`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = NutritionEntryViewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.ENERGY, "2000")
        vm.updateAmount(NutritionNutrient.PROTEIN, "4")
        vm.addNutrient(NutritionNutrient.SODIUM)
        vm.updateAmount(NutritionNutrient.SODIUM, "2.3")
        vm.addEntry(UnitSystem.IMPERIAL)
        advanceUntilIdle()

        val values = request.captured.nutrientValues
        assertEquals(2000.0, values.getValue(NutritionNutrient.ENERGY), 0.001)
        assertTrue(abs(values.getValue(NutritionNutrient.PROTEIN) - 113.398) < 0.001)
        assertEquals(2.3, values.getValue(NutritionNutrient.SODIUM), 0.001)
    }

    @Test fun `only gram nutrients enter in ounces for imperial users`() {
        assertTrue(NutritionNutrient.TOTAL_FAT.entersInOunces(UnitSystem.IMPERIAL))
        assertFalse(NutritionNutrient.TOTAL_FAT.entersInOunces(UnitSystem.METRIC))
        assertFalse(NutritionNutrient.ENERGY.entersInOunces(UnitSystem.IMPERIAL))
        assertFalse(NutritionNutrient.VITAMIN_C.entersInOunces(UnitSystem.IMPERIAL))
    }

    @Test fun `the range check applies to the metric value`() {
        // 400 oz is about 11 kg, past what Health Connect takes for one nutrient.
        val row = NutrientInputRow(NutritionNutrient.TOTAL_CARBOHYDRATE, amountText = "400")

        assertEquals(400.0, row.metricValueOrNull(UnitSystem.METRIC)!!, 0.001)
        assertNull(row.metricValueOrNull(UnitSystem.IMPERIAL))
        assertNull(NutrientInputRow(NutritionNutrient.PROTEIN, amountText = "abc").metricValueOrNull(UnitSystem.METRIC))
    }

    private fun nutritionRepo(
        canWrite: Boolean = true,
    ): NutritionRepository =
        mockk<NutritionRepository>().also { repo ->
            every { repo.nutritionWritePermissions } returns setOf("write_nutrition")
            coEvery { repo.hasNutritionWritePermission() } returns canWrite
            coEvery { repo.writeNutritionEntry(any()) } returns "record-id"
        }
}
