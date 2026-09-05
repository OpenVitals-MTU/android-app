package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
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
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.CYCLE_ENTRY_KIND_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

/** Exactly the filled sections are written, a partial failure keeps the failed ones filled, and edit mode is scoped to one record. */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun cycleRepo(grantedKinds: Set<CycleEntryKind> = CycleEntryKind.entries.toSet()): CycleRepository =
        mockk(relaxed = true) {
            every { cycleWritePermissions(any()) } answers {
                setOf("write_${firstArg<CycleEntryKind>().name.lowercase()}")
            }
            coEvery { hasCycleWritePermission(any()) } answers { firstArg<CycleEntryKind>() in grantedKinds }
            coEvery { writeCycleEntry(any()) } returns "client-id"
        }

    @Test fun `start probes write permissions for all kinds`() = runTest {
        val vm = CycleEntryViewModel(cycleRepo())

        vm.start()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertEquals(CycleEntryKind.entries.toSet(), vm.uiState.value.grantedKinds)
        assertEquals(6, vm.uiState.value.writePermissions.size)
    }

    @Test fun `a command at rest is idle`() = runTest {
        val vm = CycleEntryViewModel(cycleRepo())

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
    }

    @Test fun `saving with nothing filled reports NOTHING_TO_SAVE and writes nothing`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.NOTHING_TO_SAVE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCycleEntry(any()) }
    }

    @Test fun `saving writes only the section on screen, never a hidden one`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        // The spotting toggle sits behind another tab and must not ride along.
        vm.selectFlow(CycleRecordValues.FLOW_MEDIUM)
        vm.toggleSpotting()
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.flowSelection)
        assertTrue(vm.uiState.value.spottingLogged)
        coVerify(exactly = 1) {
            repo.writeCycleEntry(match { it.kind == CycleEntryKind.MENSTRUATION_FLOW && it.flow == CycleRecordValues.FLOW_MEDIUM })
        }
        coVerify(exactly = 1) { repo.writeCycleEntry(any()) }
    }

    @Test fun `switching category switches what save writes`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.selectSection(CycleEntryKind.SPOTTING)
        vm.toggleSpotting()
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.spottingLogged)
        coVerify(exactly = 1) { repo.writeCycleEntry(match { it.kind == CycleEntryKind.SPOTTING }) }
        coVerify(exactly = 1) { repo.writeCycleEntry(any()) }
    }

    @Test fun `an unfilled selected section is NOTHING_TO_SAVE even when another is filled`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.toggleSpotting()
        // Selected category is still Period flow, which is empty.
        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.NOTHING_TO_SAVE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCycleEntry(any()) }
    }

    @Test fun `a backdated entry is stamped at noon of the chosen day`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        val yesterday = LocalDate.now().minusDays(1)
        vm.updateDate(yesterday)
        vm.selectSection(CycleEntryKind.SPOTTING)
        vm.toggleSpotting()
        vm.save()
        advanceUntilIdle()

        coVerify {
            repo.writeCycleEntry(
                match {
                    it.time.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == yesterday
                }
            )
        }
    }

    @Test fun `an invalid BBT reports INVALID_VALUE and writes nothing`() = runTest {
        val repo = cycleRepo()
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.selectSection(CycleEntryKind.BASAL_BODY_TEMPERATURE)
        vm.updateBbtInput("34.2")
        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCycleEntry(any()) }
    }

    @Test fun `a filled section without its permission reports MISSING_WRITE_PERMISSION`() = runTest {
        val repo = cycleRepo(grantedKinds = setOf(CycleEntryKind.MENSTRUATION_FLOW))
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.selectSection(CycleEntryKind.SPOTTING)
        vm.toggleSpotting()
        vm.save()
        advanceUntilIdle()

        assertEquals(CycleEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeCycleEntry(any()) }
    }

    @Test fun `a failed write keeps the section filled and surfaces the error`() = runTest {
        val repo = cycleRepo()
        coEvery {
            repo.writeCycleEntry(match { it.kind == CycleEntryKind.MENSTRUATION_FLOW })
        } throws IllegalStateException("hc down")
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.selectFlow(CycleRecordValues.FLOW_LIGHT)
        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(CycleEntryError.WRITE_FAILED, state.entryError)
        assertFalse(state.saveCompleted)
        assertEquals(CycleRecordValues.FLOW_LIGHT, state.flowSelection)
        assertTrue(state.writeError is ScreenError)
    }

    @Test fun `editing a field clears a previous failure`() = runTest {
        val repo = cycleRepo(grantedKinds = emptySet())
        val vm = CycleEntryViewModel(repo)
        vm.start()
        advanceUntilIdle()

        vm.selectSection(CycleEntryKind.SPOTTING)
        vm.toggleSpotting()
        vm.save()
        advanceUntilIdle()
        assertEquals(CycleEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)

        vm.selectFlow(CycleRecordValues.FLOW_LIGHT)
        assertNull(vm.uiState.value.entryError)
    }

    // Edit.

    private fun editHandle(kind: CycleEntryKind, id: String) = SavedStateHandle(
        mapOf(CYCLE_ENTRY_KIND_ARG to kind.name, CYCLE_ENTRY_ID_ARG to id)
    )

    @Test fun `edit mode loads the record into its section`() = runTest {
        val repo = cycleRepo()
        coEvery { repo.loadCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            time = Instant.now().minusSeconds(3600),
            flow = CycleRecordValues.FLOW_HEAVY,
            isOpenVitalsEntry = true,
        )
        val vm = CycleEntryViewModel(repo, editHandle(CycleEntryKind.MENSTRUATION_FLOW, "uid"))

        vm.start()
        advanceUntilIdle()

        assertEquals(CycleRecordValues.FLOW_HEAVY, vm.uiState.value.flowSelection)
        assertTrue(vm.uiState.value.isEditMode)
    }

    @Test fun `edit mode refuses a non-OpenVitals record`() = runTest {
        val repo = cycleRepo()
        coEvery { repo.loadCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            time = Instant.now(),
            flow = CycleRecordValues.FLOW_LIGHT,
            isOpenVitalsEntry = false,
        )
        val vm = CycleEntryViewModel(repo, editHandle(CycleEntryKind.MENSTRUATION_FLOW, "uid"))

        vm.start()
        advanceUntilIdle()

        assertEquals(CycleEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertTrue(vm.uiState.value.writeError is ScreenError.Message)
    }

    @Test fun `saving in edit mode routes to update`() = runTest {
        val repo = cycleRepo()
        coEvery { repo.loadCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            time = Instant.now().minusSeconds(3600),
            flow = CycleRecordValues.FLOW_LIGHT,
            isOpenVitalsEntry = true,
        )
        val vm = CycleEntryViewModel(repo, editHandle(CycleEntryKind.MENSTRUATION_FLOW, "uid"))
        vm.start()
        advanceUntilIdle()

        vm.selectFlow(CycleRecordValues.FLOW_MEDIUM)
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.saveCompleted)
        coVerify(exactly = 1) {
            repo.updateCycleEntry("uid", match { it.flow == CycleRecordValues.FLOW_MEDIUM })
        }
        coVerify(exactly = 0) { repo.writeCycleEntry(any()) }
    }

    @Test fun `updateEntryTime clamps to now`() = runTest {
        val repo = cycleRepo()
        coEvery { repo.loadCycleEntry(CycleEntryKind.SPOTTING, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.SPOTTING,
            time = Instant.now().minusSeconds(3600),
            isOpenVitalsEntry = true,
        )
        val vm = CycleEntryViewModel(repo, editHandle(CycleEntryKind.SPOTTING, "uid"))
        vm.start()
        advanceUntilIdle()

        vm.updateEntryTime(Instant.now().plusSeconds(7200))

        assertFalse(vm.uiState.value.editTime!!.isAfter(Instant.now()))
    }
}
