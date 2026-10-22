package tech.mmarca.openvitals.features.manualentry.body

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.navigation.BODY_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.BODY_MEASUREMENT_TYPE_ARG
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMeasurementEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `a single type checks its write permission`() = runTest {
        val vm = viewModel(bodyRepo(), type = BodyMeasurementType.BODY_FAT)

        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(BodyMeasurementType.BODY_FAT), state.types)
        assertFalse(state.isCheckingPermission)
        assertEquals(setOf(BodyMeasurementType.BODY_FAT), state.grantedTypes)
        assertFalse(state.needsWritePermission)
        // The save command starts at rest.
        assertFalse(state.isSavingEntry)
        assertFalse(state.saveCompleted)
        assertNull(state.entryError)
        assertNull(state.writeError)
    }

    @Test fun `a command at rest is idle`() = runTest {
        val vm = viewModel(bodyRepo())

        // Straight out of the constructor, before anything has been dispatched.
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)

        // ...and still at rest once the permission probe settled.
        vm.start()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `opened without a type the form is a weigh-in`() = runTest {
        val vm = viewModel(bodyRepo())

        vm.start()
        advanceUntilIdle()

        assertEquals(BodyCompositionTypes, vm.uiState.value.types)
        assertTrue(vm.uiState.value.isComposition)
        assertTrue(vm.uiState.value.offersBodyWaterPercent)
    }

    @Test fun `a failed save carries the failure to the form, not an exception`() = runTest {
        val repo = bodyRepo()
        coEvery { repo.writeBodyMeasurementEntries(any()) } throws RuntimeException("the provider hung up")
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.WEIGHT, "72.5")
        vm.save()
        advanceUntilIdle()

        assertEquals(BodyMeasurementEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("the provider hung up"), vm.uiState.value.writeError)
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `editing a field clears the failure the last attempt left behind`() = runTest {
        val repo = bodyRepo()
        coEvery { repo.writeBodyMeasurementEntries(any()) } throws RuntimeException("boom")
        val vm = startedViewModel(repo)
        vm.updateInput(BodyMeasurementType.WEIGHT, "72.5")
        vm.save()
        advanceUntilIdle()
        assertEquals(ScreenError.Message("boom"), vm.uiState.value.writeError)

        vm.updateInput(BodyMeasurementType.WEIGHT, "73")

        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.writeError)
    }

    @Test fun `a weigh-in writes every filled value in one call, at one time`() = runTest {
        val repo = bodyRepo()
        val written = slot<List<BodyMeasurementWriteRequest>>()
        coEvery { repo.writeBodyMeasurementEntries(capture(written)) } answers { written.captured.map { "id" } }
        val vm = startedViewModel(repo)
        val selectedTime = Instant.parse("2026-05-20T07:30:00Z")

        vm.updateEntryTime(selectedTime)
        vm.updateInput(BodyMeasurementType.BMR, "1712")
        vm.updateInput(BodyMeasurementType.WEIGHT, "80,4")
        vm.updateInput(BodyMeasurementType.BODY_FAT, "21.5")
        vm.updateInput(BodyMeasurementType.LEAN_MASS, "60.1")
        vm.updateInput(BodyMeasurementType.BONE_MASS, "3.2")
        vm.save()
        advanceUntilIdle()

        // Form order, the blank body water skipped.
        assertEquals(
            listOf(
                BodyMeasurementType.WEIGHT,
                BodyMeasurementType.BODY_FAT,
                BodyMeasurementType.LEAN_MASS,
                BodyMeasurementType.BONE_MASS,
                BodyMeasurementType.BMR,
            ),
            written.captured.map { it.type },
        )
        assertEquals(listOf(80.4, 21.5, 60.1, 3.2, 1712.0), written.captured.map { it.value })
        assertTrue(written.captured.all { it.time == selectedTime })
        assertTrue(vm.uiState.value.saveCompleted)
        assertTrue(vm.uiState.value.inputs.isEmpty())

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `body water as a percentage is saved as that share of the weight on the form`() = runTest {
        val repo = bodyRepo()
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.WEIGHT, "80")
        vm.updateInput(BodyMeasurementType.BODY_WATER_MASS, "55")
        vm.save()
        advanceUntilIdle()

        coVerify {
            repo.writeBodyMeasurementEntries(match { requests ->
                requests.single { it.type == BodyMeasurementType.BODY_WATER_MASS }.value == 44.0
            })
        }
    }

    @Test fun `body water as a percentage needs the weight beside it`() = runTest {
        val repo = bodyRepo()
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.BODY_WATER_MASS, "55")
        vm.save()

        assertEquals(BodyMeasurementEntryError.BODY_WATER_NEEDS_WEIGHT, vm.uiState.value.entryError)
        assertEquals(setOf(BodyMeasurementType.WEIGHT), vm.uiState.value.invalidTypes)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntries(any()) }
    }

    @Test fun `body water typed as a mass is read in the weight unit`() = runTest {
        val repo = bodyRepo()
        val vm = startedViewModel(repo)
        val imperial = BodyEntryUnits(mass = UnitSystem.IMPERIAL)

        vm.setBodyWaterInput(BodyWaterInput.MASS)
        vm.updateInput(BodyMeasurementType.BODY_WATER_MASS, "100")
        vm.save(imperial)
        advanceUntilIdle()

        coVerify {
            repo.writeBodyMeasurementEntries(match { requests ->
                val request = requests.single()
                request.type == BodyMeasurementType.BODY_WATER_MASS && abs(request.value - 45.359) < 0.001
            })
        }
    }

    @Test fun `an empty weigh-in saves nothing`() = runTest {
        val repo = bodyRepo()
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.WEIGHT, "  ")

        assertFalse(vm.uiState.value.canSave)
        vm.save()
        assertEquals(BodyMeasurementEntryError.NOTHING_TO_SAVE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntries(any()) }
    }

    @Test fun `a value out of range marks its field and writes nothing`() = runTest {
        val repo = bodyRepo()
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.WEIGHT, "80")
        vm.updateInput(BodyMeasurementType.BODY_FAT, "120")
        vm.updateInput(BodyMeasurementType.BMR, "lots")
        vm.save()

        assertEquals(BodyMeasurementEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        assertEquals(setOf(BodyMeasurementType.BODY_FAT, BodyMeasurementType.BMR), vm.uiState.value.invalidTypes)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntries(any()) }

        vm.updateInput(BodyMeasurementType.BODY_FAT, "20")
        assertEquals(setOf(BodyMeasurementType.BMR), vm.uiState.value.invalidTypes)
    }

    @Test fun `a value whose write permission is missing asks for it and is not written`() = runTest {
        val repo = bodyRepo(granted = BodyCompositionTypes.toSet() - BodyMeasurementType.LEAN_MASS)
        val vm = startedViewModel(repo)

        vm.updateInput(BodyMeasurementType.WEIGHT, "80")
        assertFalse(vm.uiState.value.needsWritePermission)
        assertTrue(vm.uiState.value.canSave)

        vm.updateInput(BodyMeasurementType.LEAN_MASS, "60")
        assertTrue(vm.uiState.value.needsWritePermission)
        assertFalse(vm.uiState.value.canSave)

        vm.save()
        assertEquals(BodyMeasurementEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntries(any()) }
    }

    @Test fun `editing a weight loads the stored value and updates that record`() = runTest {
        val repo = bodyRepo(
            editEntry = BodyMeasurementEntry(
                id = "weight-id",
                type = BodyMeasurementType.WEIGHT,
                time = Instant.parse("2026-05-19T07:30:00Z"),
                value = 81.0,
                source = "OpenVitals",
                isOpenVitalsEntry = true,
            ),
        )
        val vm = viewModel(repo, type = BodyMeasurementType.WEIGHT, editId = "weight-id")
        val selectedTime = Instant.parse("2026-05-20T07:30:00Z")

        vm.start()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditMode)
        assertEquals("81", vm.uiState.value.inputs[BodyMeasurementType.WEIGHT])
        assertEquals(Instant.parse("2026-05-19T07:30:00Z"), vm.uiState.value.entryTime)

        vm.updateEntryTime(selectedTime)
        vm.updateInput(BodyMeasurementType.WEIGHT, "82.4")
        vm.save()
        advanceUntilIdle()

        coVerify {
            repo.updateBodyMeasurementEntry(
                "weight-id",
                BodyMeasurementWriteRequest(BodyMeasurementType.WEIGHT, selectedTime, 82.4),
            )
        }
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntries(any()) }
        assertTrue(vm.uiState.value.saveCompleted)
        assertEquals("82.4", vm.uiState.value.inputs[BodyMeasurementType.WEIGHT])
    }

    @Test fun `editing body water shows the stored mass, since no weight is on the form`() = runTest {
        val repo = bodyRepo(
            editEntry = BodyMeasurementEntry(
                id = "water-id",
                type = BodyMeasurementType.BODY_WATER_MASS,
                time = Instant.parse("2026-05-19T07:30:00Z"),
                value = 44.0,
                source = "OpenVitals",
                isOpenVitalsEntry = true,
            ),
        )
        val vm = viewModel(repo, type = BodyMeasurementType.BODY_WATER_MASS, editId = "water-id")

        vm.start()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.offersBodyWaterPercent)
        assertEquals(BodyWaterInput.MASS, vm.uiState.value.bodyWaterUnit)
        assertEquals("44", vm.uiState.value.inputs[BodyMeasurementType.BODY_WATER_MASS])
    }

    @Test fun `another app's record is not opened for editing`() = runTest {
        val repo = bodyRepo(
            editEntry = BodyMeasurementEntry(
                id = "lean-id",
                type = BodyMeasurementType.LEAN_MASS,
                time = Instant.parse("2026-05-19T07:30:00Z"),
                value = 60.0,
                source = "Scale app",
                isOpenVitalsEntry = false,
            ),
        )
        val vm = viewModel(repo, type = BodyMeasurementType.LEAN_MASS, editId = "lean-id")

        vm.start()
        advanceUntilIdle()

        assertEquals(BodyMeasurementEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertTrue(vm.uiState.value.inputs.isEmpty())
    }

    private fun viewModel(
        repo: BodyRepository,
        type: BodyMeasurementType? = null,
        editId: String? = null,
    ): BodyMeasurementEntryViewModel =
        BodyMeasurementEntryViewModel(
            repository = repo,
            savedStateHandle = SavedStateHandle(
                buildMap {
                    type?.let { put(BODY_MEASUREMENT_TYPE_ARG, it.name) }
                    editId?.let { put(BODY_ENTRY_ID_ARG, it) }
                },
            ),
        )

    private fun TestScope.startedViewModel(repo: BodyRepository): BodyMeasurementEntryViewModel =
        viewModel(repo).also {
            it.start()
            advanceUntilIdle()
        }

    private fun bodyRepo(
        granted: Set<BodyMeasurementType> = BodyMeasurementType.entries.toSet(),
        editEntry: BodyMeasurementEntry? = null,
    ): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            every { repo.bodyWritePermissions(any()) } answers { setOf("write_${firstArg<BodyMeasurementType>()}") }
            coEvery { repo.hasBodyWritePermission(any()) } answers { firstArg<BodyMeasurementType>() in granted }
            coEvery { repo.writeBodyMeasurementEntries(any()) } answers { firstArg<List<Any>>().map { "record-id" } }
            coEvery { repo.loadBodyMeasurementEntry(any(), any()) } returns editEntry
            coEvery { repo.updateBodyMeasurementEntry(any(), any()) } returns Unit
        }
}
