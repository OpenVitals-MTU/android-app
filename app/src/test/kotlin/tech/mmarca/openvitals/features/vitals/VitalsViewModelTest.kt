package tech.mmarca.openvitals.features.vitals

import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.repository.VitalsRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VitalsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val phase3 = setOf("blood", "oxygen")

    private fun emptyRepo() = mockk<VitalsRepository>().also { repo ->
        every { repo.phase3Permissions } returns phase3
        coEvery { repo.missingPermissions() } returns emptySet()
        coEvery { repo.loadBloodPressure(any(), any()) } returns emptyList()
        coEvery { repo.loadSpO2(any(), any()) } returns emptyList()
        coEvery { repo.loadRespiratoryRate(any(), any()) } returns emptyList()
        coEvery { repo.loadBodyTemperature(any(), any()) } returns emptyList()
        coEvery { repo.loadVo2Max(any(), any()) } returns emptyList()
    }

    @Test fun `initial range is WEEK`() = runTest {
        val vm = VitalsViewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and exposes missing permissions`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.missingPermissions() } returns phase3

        val vm = VitalsViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(phase3, vm.uiState.value.missingPermissions)
        assertFalse(vm.uiState.value.hasData)
    }

    @Test fun `load success populates vitals and latest values`() = runTest {
        val bloodPressure = listOf(
            BloodPressureEntry(Instant.ofEpochSecond(1_000), 118, 76, "test"),
            BloodPressureEntry(Instant.ofEpochSecond(2_000), 122, 78, "test"),
        )
        val spO2 = listOf(SpO2Entry(Instant.ofEpochSecond(1_500), 97.5, "test"))
        val repo = emptyRepo()
        coEvery { repo.loadBloodPressure(any(), any()) } returns bloodPressure
        coEvery { repo.loadSpO2(any(), any()) } returns spO2

        val vm = VitalsViewModel(repo)

        assertTrue(vm.uiState.value.hasData)
        assertEquals(122, vm.uiState.value.latestBloodPressure?.systolicMmHg)
        assertEquals(97.5, vm.uiState.value.latestSpO2?.percent!!, 0.01)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadBloodPressure(any(), any()) } throws RuntimeException("timeout")

        val vm = VitalsViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = VitalsViewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadBloodPressure(any(), any()) }
    }

    @Test fun `phase3Permissions delegates to repository`() = runTest {
        val vm = VitalsViewModel(emptyRepo())
        assertEquals(phase3, vm.phase3Permissions)
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val vm = VitalsViewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val vm = VitalsViewModel(emptyRepo())
        vm.selectDate(today.minusWeeks(4))
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = VitalsViewModel(emptyRepo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }
}
