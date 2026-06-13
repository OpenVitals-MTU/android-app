package tech.mmarca.openvitals.features.manualentry.hydration

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
import io.mockk.verify
import java.time.LocalDate
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
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun entryRepo(
        canWrite: Boolean = true,
        dailyHydration: List<DailyHydration> = emptyList(),
        dailyGoalLiters: Double = 2.0,
        containerVolumeMilliliters: Map<String, Double> = emptyMap(),
    ) = mockk<HydrationRepository>().also { repo ->
        every { repo.hydrationWritePermissions } returns setOf("write_hydration")
        every { repo.hydrationContainerVolumeMilliliters() } returns containerVolumeMilliliters
        every { repo.hydrationDailyGoalLiters() } returns dailyGoalLiters
        every { repo.setHydrationContainerVolumeMilliliters(any(), any()) } returns Unit
        coEvery { repo.hasHydrationWritePermission() } returns canWrite
        coEvery { repo.writeHydrationEntry(any()) } returns "record-id"
        coEvery { repo.loadDailyHydration(any(), any()) } returns dailyHydration
    }

    @Test fun `initial load checks write permission`() = runTest {
        val vm = HydrationEntryViewModel(entryRepo(canWrite = true))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWriteHydration)
    }

    @Test fun `initial state uses configured daily hydration goal`() = runTest {
        val vm = HydrationEntryViewModel(entryRepo(dailyGoalLiters = 2.75))
        advanceUntilIdle()

        assertEquals(2.75, vm.uiState.value.dailyGoalLiters, 0.0001)
    }

    @Test fun `container presets use expected defaults`() = runTest {
        val vm = HydrationEntryViewModel(entryRepo())
        advanceUntilIdle()

        assertEquals(
            listOf(100.0, 150.0, 175.0, 200.0, 300.0, 500.0, 1000.0),
            vm.uiState.value.containerOptions.map { it.volumeMilliliters },
        )
        assertEquals("coffee_cup", vm.uiState.value.selectedContainer.id)
    }

    @Test fun `container presets apply persisted volume overrides`() = runTest {
        val vm = HydrationEntryViewModel(
            entryRepo(
                containerVolumeMilliliters = mapOf(
                    "coffee_cup" to 125.0,
                    "water_bottle" to 650.0,
                    "unknown" to 900.0,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(125.0, 150.0, 175.0, 200.0, 300.0, 650.0, 1000.0),
            vm.uiState.value.containerOptions.map { it.volumeMilliliters },
        )
        assertEquals("coffee_cup", vm.uiState.value.selectedContainer.id)
        assertEquals(125.0, vm.uiState.value.selectedContainer.volumeMilliliters, 0.0001)
    }

    @Test fun `container size update changes and selects preset option`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        val bottle = vm.uiState.value.containerOptions.first { it.id == "water_bottle" }
        vm.updateContainerSize(bottle, 650.0)

        assertEquals("water_bottle", vm.uiState.value.selectedContainer.id)
        assertEquals(650.0, vm.uiState.value.selectedContainer.volumeMilliliters, 0.0001)
        assertEquals(
            650.0,
            vm.uiState.value.containerOptions.first { it.id == "water_bottle" }.volumeMilliliters,
            0.0001,
        )
        assertFalse(vm.uiState.value.containerOptions.any { it.id == "custom" })
        verify { repo.setHydrationContainerVolumeMilliliters("water_bottle", 650.0) }
    }

    @Test fun `invalid container size is rejected`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        val selectedContainer = vm.uiState.value.selectedContainer
        vm.updateContainerSize(selectedContainer, 0.0)

        assertEquals(HydrationEntryError.INVALID_AMOUNT, vm.uiState.value.entryError)
        assertEquals(selectedContainer, vm.uiState.value.selectedContainer)
        verify(exactly = 0) { repo.setHydrationContainerVolumeMilliliters(any(), any()) }
    }

    @Test fun `refresh today hydration loads only the current day total`() = runTest {
        val today = LocalDate.of(2026, 5, 25)
        val repo = entryRepo(
            dailyHydration = listOf(
                DailyHydration(date = today, liters = 0.25),
                DailyHydration(date = today, liters = 0.75),
            ),
        )
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.refreshTodayHydration(today)
        advanceUntilIdle()

        assertEquals(1.0, vm.uiState.value.todayHydrationLiters, 0.0001)
        coVerify { repo.loadDailyHydration(today, today) }
    }

    @Test fun `selected hydration entry writes selected container volume`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.selectContainer(HydrationContainerOption.Defaults.first { it.id == "water_bottle" })
        vm.addSelectedHydrationEntry()
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                request.volumeLiters == 0.5
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals(0.5, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)

        vm.onSaveCompletedHandled()
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `container tap writes tapped container volume`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        val largeGlass = HydrationContainerOption.Defaults.first { it.id == "large_glass" }
        vm.addContainerHydrationEntry(largeGlass)
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                request.volumeLiters == 0.3
            })
        }
        assertEquals(largeGlass, vm.uiState.value.selectedContainer)
        assertEquals(0.3, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `container tap writes tea cup as one hundred fifty milliliters`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        val teaCup = HydrationContainerOption.Defaults.first { it.id == "tea_cup" }
        vm.addContainerHydrationEntry(teaCup)
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                abs(request.volumeLiters - 0.15) < 0.0001
            })
        }
        assertEquals(0.15, vm.uiState.value.todayHydrationLiters, 0.0001)
    }

    @Test fun `custom hydration entry writes exact custom amount`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.addCustomHydrationEntry(350.0)
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                request.volumeLiters == 0.35
            })
        }
        assertEquals(0.35, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `invalid custom hydration entry is rejected`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.addCustomHydrationEntry(0.0)
        advanceUntilIdle()

        assertEquals(HydrationEntryError.INVALID_AMOUNT, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
    }

    @Test fun `beverage multiplier adjusts written hydration volume`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.selectBeverage(HydrationBeverage.MILK)
        vm.selectContainer(HydrationContainerOption.Defaults.first { it.id == "medium_glass" })
        vm.addSelectedHydrationEntry()
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                abs(request.volumeLiters - 0.3) < 0.001
            })
        }
    }

    @Test fun `missing write permission prevents hydration entry writes`() = runTest {
        val repo = entryRepo(canWrite = false)
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.addSelectedHydrationEntry()

        assertEquals(HydrationEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
    }

    @Test fun `write failure clears saving and exposes entry error`() = runTest {
        val repo = entryRepo()
        coEvery { repo.writeHydrationEntry(any()) } throws RuntimeException("denied")
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.addSelectedHydrationEntry()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertEquals(HydrationEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals("denied", vm.uiState.value.writeErrorMessage)
    }
}
