package tech.mmarca.openvitals.features.manualentry.vitals

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



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
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class VitalsMeasurementEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `setting type checks write permission`() = runTest {
        val vm = VitalsMeasurementEntryViewModel(vitalsRepo(canWrite = true))

        vm.setType(VitalsMeasurementType.SPO2)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWrite)
        assertEquals(VitalsMeasurementType.SPO2, vm.uiState.value.type)
    }

    @Test fun `blood pressure entry writes systolic and diastolic values`() = runTest {
        val repo = vitalsRepo()
        val vm = VitalsMeasurementEntryViewModel(repo)
        vm.setType(VitalsMeasurementType.BLOOD_PRESSURE)
        advanceUntilIdle()

        vm.updateInput("121")
        vm.updateSecondaryInput("78")
        vm.addEntry(121.0, 78.0)
        advanceUntilIdle()

        coVerify {
            repo.writeVitalsMeasurementEntry(match<VitalsMeasurementWriteRequest> { request ->
                request.type == VitalsMeasurementType.BLOOD_PRESSURE &&
                    abs(request.value - 121.0) < 0.001 &&
                    abs((request.secondaryValue ?: 0.0) - 78.0) < 0.001
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals("", vm.uiState.value.inputText)
        assertEquals("", vm.uiState.value.secondaryInputText)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `body temperature entry writes celsius value`() = runTest {
        val repo = vitalsRepo()
        val vm = VitalsMeasurementEntryViewModel(repo)
        vm.setType(VitalsMeasurementType.BODY_TEMPERATURE)
        advanceUntilIdle()

        vm.addEntry(36.8)
        advanceUntilIdle()

        coVerify {
            repo.writeVitalsMeasurementEntry(match<VitalsMeasurementWriteRequest> { request ->
                request.type == VitalsMeasurementType.BODY_TEMPERATURE &&
                    abs(request.value - 36.8) < 0.001 &&
                    request.secondaryValue == null
            })
        }
    }

    @Test fun `invalid vitals value does not write`() = runTest {
        val repo = vitalsRepo()
        val vm = VitalsMeasurementEntryViewModel(repo)
        vm.setType(VitalsMeasurementType.BLOOD_PRESSURE)
        advanceUntilIdle()

        vm.addEntry(70.0, 90.0)

        assertEquals(VitalsMeasurementEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeVitalsMeasurementEntry(any()) }
    }

    @Test fun `missing write permission prevents vitals writes`() = runTest {
        val repo = vitalsRepo(canWrite = false)
        val vm = VitalsMeasurementEntryViewModel(repo)
        vm.setType(VitalsMeasurementType.SPO2)
        advanceUntilIdle()

        vm.addEntry(98.0)

        assertEquals(VitalsMeasurementEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeVitalsMeasurementEntry(any()) }
    }

    private fun vitalsRepo(
        canWrite: Boolean = true,
    ): VitalsRepository =
        mockk<VitalsRepository>().also { repo ->
            every { repo.vitalsWritePermissions(any()) } returns setOf("write_vitals")
            coEvery { repo.hasVitalsWritePermission(any()) } returns canWrite
            coEvery { repo.writeVitalsMeasurementEntry(any()) } returns "record-id"
        }
}
