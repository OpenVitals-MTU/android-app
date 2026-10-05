package tech.mmarca.openvitals.features.manualentry.nutrition

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.navigation.NUTRITION_ENTRY_ID_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `a command at rest is idle`() = runTest {
        val vm = viewModel(nutritionRepo())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `the form opens on energy, protein, fat and carbs`() = runTest {
        val vm = viewModel(nutritionRepo())

        assertEquals(PrimaryNutritionEntryNutrients, vm.uiState.value.rows.map { it.nutrient })
    }

    @Test fun `caffeine is logged as a drink, so it cannot be added here`() = runTest {
        val vm = viewModel(nutritionRepo())

        assertFalse(NutritionNutrient.CAFFEINE in vm.uiState.value.addableNutrients)
        vm.addNutrient(NutritionNutrient.CAFFEINE)

        assertFalse(vm.uiState.value.rows.any { it.nutrient == NutritionNutrient.CAFFEINE })
    }

    @Test fun `every other Health Connect nutrient can be added`() = runTest {
        val vm = viewModel(nutritionRepo())

        assertEquals(
            NutritionNutrient.entries.toSet() - PrimaryNutritionEntryNutrients.toSet() - NutritionNutrient.CAFFEINE,
            vm.uiState.value.addableNutrients.toSet(),
        )
    }

    @Test fun `an added nutrient can be removed, a main one cannot`() = runTest {
        val vm = viewModel(nutritionRepo())

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
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.ENERGY, "2150")
        vm.updateAmount(NutritionNutrient.PROTEIN, "120,5")
        vm.addNutrient(NutritionNutrient.SODIUM)
        vm.updateAmount(NutritionNutrient.SODIUM, "2300")
        vm.addEntry()
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
        val vm = viewModel(repo)
        advanceUntilIdle()
        val lastMonday = Instant.parse("2026-09-28T19:00:00Z")

        vm.updateTimestamp(lastMonday)
        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "240")
        vm.addEntry()
        advanceUntilIdle()

        assertEquals(lastMonday, request.captured.time)
        assertNull(vm.uiState.value.timestamp)
    }

    @Test fun `a future time is written as now`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateTimestamp(Instant.now().plusSeconds(3_600))
        vm.updateAmount(NutritionNutrient.PROTEIN, "30")
        vm.addEntry()
        advanceUntilIdle()

        assertFalse(request.captured.time.isAfter(Instant.now()))
    }

    @Test fun `amounts are grams and kcal whatever the unit system, as the nutrition screens show them`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.PROTEIN, "25")
        vm.updateAmount(NutritionNutrient.TOTAL_FAT, "10")
        vm.addEntry()
        advanceUntilIdle()

        assertEquals(
            mapOf(NutritionNutrient.PROTEIN to 25.0, NutritionNutrient.TOTAL_FAT to 10.0),
            request.captured.nutrientValues,
        )
    }

    @Test fun `a vitamin typed in micrograms is stored in grams and edits back in micrograms`() = runTest {
        val repo = nutritionRepo()
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.writeNutritionEntry(capture(request)) } returns "record-id"
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.addNutrient(NutritionNutrient.VITAMIN_K)
        vm.updateAmount(NutritionNutrient.VITAMIN_K, "15")
        vm.addEntry()
        advanceUntilIdle()

        assertEquals(0.000015, request.captured.nutrientValues.getValue(NutritionNutrient.VITAMIN_K), 1e-12)
        assertEquals(
            "15",
            mapOf(NutritionNutrient.VITAMIN_K to 0.000015).toNutritionEntryRows()
                .single { it.nutrient == NutritionNutrient.VITAMIN_K }.amountText,
        )
    }

    @Test fun `an empty form does not write`() = runTest {
        val repo = nutritionRepo()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.addEntry()

        assertEquals(NutritionEntryError.NO_VALUES, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `one invalid field stops the whole entry`() = runTest {
        val repo = nutritionRepo()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.ENERGY, "2000")
        vm.updateAmount(NutritionNutrient.TOTAL_FAT, "0")
        vm.addEntry()

        assertEquals(NutritionEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `missing write permission prevents the write`() = runTest {
        val repo = nutritionRepo(canWrite = false)
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.PROTEIN, "25")
        vm.addEntry()

        assertEquals(NutritionEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
    }

    @Test fun `a failed save carries the failure to the form, not an exception`() = runTest {
        val repo = nutritionRepo()
        coEvery { repo.writeNutritionEntry(any()) } throws RuntimeException("the provider hung up")
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "45")
        vm.addEntry()
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
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "45")
        vm.addEntry()
        advanceUntilIdle()
        assertEquals(NutritionEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("boom"), vm.uiState.value.writeError)

        vm.updateAmount(NutritionNutrient.TOTAL_CARBOHYDRATE, "50")

        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    // Editing.

    @Test fun `editing a typed entry fills the form and updates it in place`() = runTest {
        val repo = nutritionRepo()
        val typedAt = Instant.parse("2026-09-28T19:00:00Z")
        coEvery { repo.loadNutritionEntry("record-1") } returns typedEntry(
            time = typedAt,
            values = mapOf(
                NutritionNutrient.ENERGY to 2150.0,
                NutritionNutrient.PROTEIN to 120.5,
                NutritionNutrient.DIETARY_FIBER to 31.0,
            ),
        )
        val request = slot<NutritionWriteRequest>()
        coEvery { repo.updateNutritionEntry("record-1", capture(request)) } returns Unit
        val vm = viewModel(repo, editRecordId = "record-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditMode)
        assertTrue(state.canSave)
        assertEquals(typedAt, state.timestamp)
        assertEquals(
            PrimaryNutritionEntryNutrients + NutritionNutrient.DIETARY_FIBER,
            state.rows.map { it.nutrient },
        )
        assertEquals("2150", state.amountOf(NutritionNutrient.ENERGY))
        assertEquals("120.5", state.amountOf(NutritionNutrient.PROTEIN))
        assertEquals("", state.amountOf(NutritionNutrient.TOTAL_FAT))
        assertEquals("31", state.amountOf(NutritionNutrient.DIETARY_FIBER))

        vm.updateAmount(NutritionNutrient.PROTEIN, "125")
        vm.addEntry()
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.writeNutritionEntry(any()) }
        assertEquals(typedAt, request.captured.time)
        assertEquals(
            mapOf(
                NutritionNutrient.ENERGY to 2150.0,
                NutritionNutrient.PROTEIN to 125.0,
                NutritionNutrient.DIETARY_FIBER to 31.0,
            ),
            request.captured.nutrientValues,
        )
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `a record the form did not write cannot be edited here`() = runTest {
        val repo = nutritionRepo()
        coEvery { repo.loadNutritionEntry("food-1") } returns typedEntry().copy(
            name = "Banana",
            clientRecordId = "openvitals_food_1_banana_u",
        )
        val vm = viewModel(repo, editRecordId = "food-1")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canSave)
        assertEquals(NutritionEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Text(R.string.screen_error_entry_not_editable), vm.uiState.value.writeError)
        vm.addEntry()
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.updateNutritionEntry(any(), any()) }
    }

    @Test fun `an old carbs entry is edited as a typed entry`() = runTest {
        val repo = nutritionRepo()
        coEvery { repo.loadNutritionEntry("carbs-1") } returns typedEntry(
            values = mapOf(NutritionNutrient.TOTAL_CARBOHYDRATE to 45.0),
        ).copy(name = "OpenVitals carbs", clientRecordId = "openvitals_nutrition_1_u")
        val vm = viewModel(repo, editRecordId = "carbs-1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.canSave)
        assertEquals("45", vm.uiState.value.amountOf(NutritionNutrient.TOTAL_CARBOHYDRATE))
    }

    private fun NutritionEntryUiState.amountOf(nutrient: NutritionNutrient): String =
        rows.single { it.nutrient == nutrient }.amountText

    private fun typedEntry(
        time: Instant = Instant.parse("2026-09-28T19:00:00Z"),
        values: Map<NutritionNutrient, Double> = mapOf(NutritionNutrient.ENERGY to 2000.0),
    ) = NutritionEntry(
        time = time,
        mealType = 0,
        name = "OpenVitals nutrition",
        energyKcal = values[NutritionNutrient.ENERGY],
        proteinGrams = values[NutritionNutrient.PROTEIN],
        carbsGrams = values[NutritionNutrient.TOTAL_CARBOHYDRATE],
        fatGrams = values[NutritionNutrient.TOTAL_FAT],
        fiberGrams = values[NutritionNutrient.DIETARY_FIBER],
        sugarGrams = values[NutritionNutrient.SUGAR],
        source = "tech.mmarca.openvitals",
        nutrientValues = values,
        id = "record-1",
        clientRecordId = "openvitals_manual_nutrition_1_u",
        isOpenVitalsEntry = true,
    )

    private fun viewModel(
        repo: NutritionRepository,
        editRecordId: String? = null,
    ) = NutritionEntryViewModel(
        repository = repo,
        savedStateHandle = SavedStateHandle(
            editRecordId?.let { mapOf(NUTRITION_ENTRY_ID_ARG to it) }.orEmpty(),
        ),
    )

    private fun nutritionRepo(
        canWrite: Boolean = true,
    ): NutritionRepository =
        mockk<NutritionRepository>().also { repo ->
            every { repo.nutritionWritePermissions } returns setOf("write_nutrition")
            coEvery { repo.hasNutritionWritePermission() } returns canWrite
            coEvery { repo.writeNutritionEntry(any()) } returns "record-id"
        }
}
