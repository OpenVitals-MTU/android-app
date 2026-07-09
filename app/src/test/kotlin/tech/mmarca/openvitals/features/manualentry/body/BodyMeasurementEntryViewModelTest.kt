package tech.mmarca.openvitals.features.manualentry.body

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import tech.mmarca.openvitals.navigation.BODY_ENTRY_ID_ARG
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMeasurementEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `setting type checks write permission`() = runTest {
        val vm = BodyMeasurementEntryViewModel(bodyRepo(canWrite = true))

        vm.setType(BodyMeasurementType.BODY_FAT)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWrite)
        assertEquals(BodyMeasurementType.BODY_FAT, vm.uiState.value.type)
    }

    @Test fun `weight entry writes canonical kg value`() = runTest {
        val repo = bodyRepo()
        val vm = BodyMeasurementEntryViewModel(repo)
        vm.setType(BodyMeasurementType.WEIGHT)
        advanceUntilIdle()

        vm.updateInput("82.4")
        vm.addEntry(82.4)
        advanceUntilIdle()

        coVerify {
            repo.writeBodyMeasurementEntry(match<BodyMeasurementWriteRequest> { request ->
                request.type == BodyMeasurementType.WEIGHT && abs(request.value - 82.4) < 0.001
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals("", vm.uiState.value.inputText)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `weight entry writes selected timestamp`() = runTest {
        val repo = bodyRepo()
        val vm = BodyMeasurementEntryViewModel(repo)
        val selectedTime = Instant.parse("2026-05-20T07:30:00Z")
        vm.setType(BodyMeasurementType.WEIGHT)
        advanceUntilIdle()

        vm.updateEntryTime(selectedTime)
        vm.updateInput("82.4")
        vm.addEntry(82.4)
        advanceUntilIdle()

        coVerify {
            repo.writeBodyMeasurementEntry(match<BodyMeasurementWriteRequest> { request ->
                request.type == BodyMeasurementType.WEIGHT &&
                    request.time == selectedTime &&
                    abs(request.value - 82.4) < 0.001
            })
        }
    }

    @Test fun `edit weight entry loads existing value and updates record`() = runTest {
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
        val vm = BodyMeasurementEntryViewModel(
            repository = repo,
            savedStateHandle = SavedStateHandle(mapOf(BODY_ENTRY_ID_ARG to "weight-id")),
        )
        val selectedTime = Instant.parse("2026-05-20T07:30:00Z")

        vm.setType(BodyMeasurementType.WEIGHT)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditMode)
        assertEquals("81", vm.uiState.value.inputText)
        assertEquals(Instant.parse("2026-05-19T07:30:00Z"), vm.uiState.value.editTime)

        vm.updateEntryTime(selectedTime)
        vm.updateInput("82.4")
        vm.addEntry(82.4)
        advanceUntilIdle()

        coVerify {
            repo.updateBodyMeasurementEntry(
                "weight-id",
                match<BodyMeasurementWriteRequest> { request ->
                    request.type == BodyMeasurementType.WEIGHT &&
                        request.time == selectedTime &&
                        abs(request.value - 82.4) < 0.001
                },
            )
        }
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntry(any()) }
        assertTrue(vm.uiState.value.saveCompleted)
        assertEquals("82.4", vm.uiState.value.inputText)
    }

    @Test fun `body fat entry writes percent value`() = runTest {
        val repo = bodyRepo()
        val vm = BodyMeasurementEntryViewModel(repo)
        vm.setType(BodyMeasurementType.BODY_FAT)
        advanceUntilIdle()

        vm.addEntry(18.5)
        advanceUntilIdle()

        coVerify {
            repo.writeBodyMeasurementEntry(match<BodyMeasurementWriteRequest> { request ->
                request.type == BodyMeasurementType.BODY_FAT && abs(request.value - 18.5) < 0.001
            })
        }
    }

    @Test fun `invalid body measurement value does not write`() = runTest {
        val repo = bodyRepo()
        val vm = BodyMeasurementEntryViewModel(repo)
        vm.setType(BodyMeasurementType.HEIGHT)
        advanceUntilIdle()

        vm.addEntry(400.0)

        assertEquals(BodyMeasurementEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntry(any()) }
    }

    @Test fun `missing write permission prevents body measurement writes`() = runTest {
        val repo = bodyRepo(canWrite = false)
        val vm = BodyMeasurementEntryViewModel(repo)
        vm.setType(BodyMeasurementType.WEIGHT)
        advanceUntilIdle()

        vm.addEntry(80.0)

        assertEquals(BodyMeasurementEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeBodyMeasurementEntry(any()) }
    }

    private fun bodyRepo(
        canWrite: Boolean = true,
        editEntry: BodyMeasurementEntry? = null,
    ): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            every { repo.bodyWritePermissions(any()) } returns setOf("write_body")
            coEvery { repo.hasBodyWritePermission(any()) } returns canWrite
            coEvery { repo.writeBodyMeasurementEntry(any()) } returns "record-id"
            coEvery { repo.loadBodyMeasurementEntry(any(), any()) } returns editEntry
            coEvery { repo.updateBodyMeasurementEntry(any(), any()) } returns Unit
        }
}
