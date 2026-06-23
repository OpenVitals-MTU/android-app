package tech.mmarca.openvitals.features.cycle

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
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.CyclePeriodData
import tech.mmarca.openvitals.data.repository.CycleRepository
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.util.MainDispatcherRule

class CycleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusMonths(4)

    private fun repo(
        data: CycleData = CycleData(),
        missingPermissions: Set<String> = emptySet(),
    ) = mockk<CycleRepository>().also { repo ->
        every { repo.phase4Permissions } returns setOf("cycle")
        coEvery { repo.missingPermissions() } returns missingPermissions
        coEvery { repo.loadCycleData(any(), any()) } returns data
        coEvery { repo.loadCyclePeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val period = query.windows.current
            CyclePeriodData(
                data = repo.loadCycleData(period.start, period.end),
                missingPermissions = repo.missingPermissions(),
            )
        }
        coEvery { repo.loadCyclePeriod(any(), any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val period = query.windows.current
            CyclePeriodData(
                data = repo.loadCycleData(period.start, period.end),
                missingPermissions = repo.missingPermissions(),
            )
        }
    }

    @Test fun `initial range is MONTH`() = runTest {
        val vm = CycleViewModel(repo())

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty data`() = runTest {
        val vm = CycleViewModel(repo())
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.data.hasData)
        assertNull(state.error)
    }

    @Test fun `load success populates cycle data and missing permissions`() = runTest {
        val cycleData = CycleData(
            menstruationFlows = listOf(
                MenstruationFlowEntry(
                    time = Instant.now(),
                    flow = 2,
                    source = "test",
                )
            )
        )
        val vm = CycleViewModel(
            repo(
                data = cycleData,
                missingPermissions = setOf("ovulation"),
            )
        )

        assertEquals(cycleData, vm.uiState.value.data)
        assertEquals(setOf("ovulation"), vm.uiState.value.missingPermissions)
        assertTrue(vm.uiState.value.data.hasData)
    }

    @Test fun `initial load requests the current month period`() = runTest {
        val repo = repo()

        CycleViewModel(repo)

        coVerify {
            repo.loadCycleData(today.withDayOfMonth(1), today)
        }
    }

    @Test fun `cyclePermissions exposes repository phase 4 permissions`() = runTest {
        val vm = CycleViewModel(repo())

        assertEquals(setOf("cycle"), vm.cyclePermissions)
    }

    @Test fun `onCyclePermissionsResult refreshes missing permissions`() = runTest {
        val repo = repo(missingPermissions = setOf("cycle"))
        coEvery { repo.missingPermissions() } returnsMany listOf(setOf("cycle"), emptySet())
        val vm = CycleViewModel(repo)

        vm.onCyclePermissionsResult(setOf("cycle"))

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<CycleRepository>()
        every { repo.phase4Permissions } returns setOf("cycle")
        coEvery { repo.loadCyclePeriod(any()) } throws RuntimeException("timeout")

        val vm = CycleViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = repo()
        val vm = CycleViewModel(repo)

        vm.selectRange(TimeRange.YEAR)

        assertEquals(TimeRange.YEAR, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadCycleData(any(), any()) }
    }

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = CycleViewModel(repo())
        val before = vm.uiState.value.selectedDate

        vm.previousPeriod()

        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val repo = repo()
        val vm = CycleViewModel(repo)
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod MONTH advances from a past month`() = runTest {
        val vm = CycleViewModel(repo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = CycleViewModel(repo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `onCyclePermissionsResult reloads data`() = runTest {
        val repo = repo()
        val vm = CycleViewModel(repo)

        vm.onCyclePermissionsResult(setOf("cycle"))

        coVerify { repo.loadCyclePeriod(any(), RefreshMode.FORCE) }
    }
}
