package tech.mmarca.openvitals.features.manualentry

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
import tech.mmarca.openvitals.data.model.BodyMeasurementType
import tech.mmarca.openvitals.data.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.data.repository.BodyRepository
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
        assertNull(vm.uiState.value.entryError)
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
    ): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            every { repo.bodyWritePermissions(any()) } returns setOf("write_body")
            coEvery { repo.hasBodyWritePermission(any()) } returns canWrite
            coEvery { repo.writeBodyMeasurementEntry(any()) } returns "record-id"
        }
}
