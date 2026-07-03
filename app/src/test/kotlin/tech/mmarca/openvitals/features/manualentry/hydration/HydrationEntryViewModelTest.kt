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
import java.time.Instant
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
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.NutritionWriteRequest
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
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
        lastCustomAmountMilliliters: Double? = null,
        customDrinks: List<CustomHydrationDrink> = emptyList(),
        hydrationEntries: List<HydrationEntry> = emptyList(),
    ) = mockk<HydrationRepository>().also { repo ->
        every { repo.hydrationWritePermissions } returns setOf("write_hydration")
        every { repo.hydrationContainerVolumeMilliliters() } returns containerVolumeMilliliters
        every { repo.lastCustomHydrationAmountMilliliters() } returns lastCustomAmountMilliliters
        every { repo.customHydrationDrinks() } returns customDrinks
        every { repo.hydrationDailyGoalLiters() } returns dailyGoalLiters
        every { repo.setHydrationContainerVolumeMilliliters(any(), any()) } returns Unit
        every { repo.setLastCustomHydrationAmountMilliliters(any()) } returns Unit
        every { repo.saveCustomHydrationDrink(any()) } returns Unit
        every { repo.deleteCustomHydrationDrink(any()) } returns Unit
        every { repo.reorderCustomHydrationDrinks(any()) } returns Unit
        every { repo.moveCustomHydrationDrinkToCategory(any(), any()) } returns Unit
        coEvery { repo.hasHydrationWritePermission() } returns canWrite
        coEvery { repo.writeHydrationEntry(any()) } returns "record-id"
        coEvery { repo.loadDailyHydration(any(), any()) } returns dailyHydration
        coEvery { repo.loadHydrationEntries(any(), any()) } returns hydrationEntries
    }

    private fun nutritionRepo(
        canWrite: Boolean = true,
        nutritionEntries: List<NutritionEntry> = emptyList(),
    ) = mockk<NutritionRepository>().also { repo ->
        every { repo.nutritionWritePermissions } returns setOf("write_nutrition")
        coEvery { repo.hasNutritionWritePermission() } returns canWrite
        coEvery { repo.writeNutritionEntry(any()) } returns "nutrition-record-id"
        coEvery { repo.writeCarbsEntry(any()) } returns "nutrition-record-id"
        coEvery { repo.loadNutritionEntries(any(), any()) } returns nutritionEntries
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

    @Test fun `initial state uses persisted custom hydration amount`() = runTest {
        val vm = HydrationEntryViewModel(entryRepo(lastCustomAmountMilliliters = 425.0))
        advanceUntilIdle()

        assertEquals(425.0, vm.uiState.value.lastCustomAmountMilliliters ?: 0.0, 0.0001)
    }

    @Test fun `initial state ignores invalid persisted custom hydration amount`() = runTest {
        val vm = HydrationEntryViewModel(entryRepo(lastCustomAmountMilliliters = 0.0))
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastCustomAmountMilliliters)
    }

    @Test fun `frequent drink options rank consumed beverages by history`() = runTest {
        val water = CustomHydrationDrink(
            id = "water",
            name = "Water",
            volumeMilliliters = 250.0,
        )
        val tea = CustomHydrationDrink(
            id = "tea",
            name = "Tea",
            volumeMilliliters = 200.0,
        )
        val coffee = CustomHydrationDrink(
            id = "coffee",
            name = "Coffee",
            volumeMilliliters = 150.0,
        )
        val repo = entryRepo(
            customDrinks = listOf(water, tea, coffee),
            hydrationEntries = listOf(
                hydrationEntry(drinkId = "water", time = "2026-05-20T08:00:00Z"),
                hydrationEntry(drinkId = "water", time = "2026-05-21T08:00:00Z"),
            ),
        )
        val nutritionRepo = nutritionRepo(
            nutritionEntries = listOf(
                nutritionEntry(name = "Coffee", time = "2026-05-22T08:00:00Z"),
                nutritionEntry(name = "coffee", time = "2026-05-23T08:00:00Z"),
                nutritionEntry(name = "Coffee", time = "2026-05-24T08:00:00Z"),
                nutritionEntry(name = "Tea", time = "2026-05-25T08:00:00Z"),
            ),
        )

        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        assertEquals(
            listOf("coffee", "water", "tea"),
            vm.uiState.value.frequentDrinkOptions.map { drink -> drink.id },
        )
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

    @Test fun `successful hydration entry hides reminder notification`() = runTest {
        val repo = entryRepo()
        val reminderController = mockk<HydrationReminderController>(relaxed = true)
        val vm = HydrationEntryViewModel(repo, reminderController)
        advanceUntilIdle()

        vm.addSelectedHydrationEntry()
        advanceUntilIdle()

        verify { reminderController.hideReminderNotification() }
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
        assertEquals(350.0, vm.uiState.value.lastCustomAmountMilliliters ?: 0.0, 0.0001)
        verify { repo.setLastCustomHydrationAmountMilliliters(350.0) }
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `saving custom drink creates reusable drink without writing entry`() = runTest {
        var savedDrinks = emptyList<CustomHydrationDrink>()
        val repo = entryRepo()
        every { repo.customHydrationDrinks() } answers { savedDrinks }
        every { repo.saveCustomHydrationDrink(any()) } answers {
            val savedDrink = firstArg<CustomHydrationDrink>()
            savedDrinks = listOf(savedDrink)
        }
        val nutritionRepo = nutritionRepo()
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.saveCustomDrink(
            CustomHydrationDrinkInput(
                name = "Coffee",
                volumeMilliliters = 150.0,
                nutrientValues = mapOf(
                    NutritionNutrient.CAFFEINE to 10.0,
                    NutritionNutrient.VITAMIN_B6 to 1.0,
                    NutritionNutrient.VITAMIN_C to 2.0,
                ),
            )
        )
        advanceUntilIdle()

        verify {
            repo.saveCustomHydrationDrink(match<CustomHydrationDrink> { drink ->
                drink.name == "Coffee" &&
                    drink.volumeMilliliters == 150.0 &&
                    drink.nutrientValues[NutritionNutrient.CAFFEINE] == 10.0
            })
        }
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
        coVerify(exactly = 0) { nutritionRepo.writeNutritionEntry(any()) }
        assertEquals("Coffee", vm.uiState.value.customDrinkOptions.single().name)
        assertEquals(0.0, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `saved custom drink entry writes hydration nutrients`() = runTest {
        val drink = CustomHydrationDrink(
            id = "coffee",
            name = "Coffee",
            volumeMilliliters = 150.0,
            nutrientValues = mapOf(
                NutritionNutrient.CAFFEINE to 10.0,
                NutritionNutrient.VITAMIN_B6 to 1.0,
                NutritionNutrient.VITAMIN_C to 2.0,
            ),
        )
        val repo = entryRepo(customDrinks = listOf(drink))
        val nutritionRepo = nutritionRepo()
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.addSavedCustomDrinkEntry(drink)
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                request.volumeLiters == 0.15 &&
                    request.drinkId == "coffee"
            })
        }
        coVerify {
            nutritionRepo.writeNutritionEntry(match<NutritionWriteRequest> { request ->
                request.name == "Coffee" &&
                    request.associatedHydrationClientRecordId == "record-id" &&
                    request.nutrientValues[NutritionNutrient.CAFFEINE] == 10.0 &&
                    request.nutrientValues[NutritionNutrient.VITAMIN_B6] == 1.0 &&
                    request.nutrientValues[NutritionNutrient.VITAMIN_C] == 2.0
            })
        }
        verify(exactly = 0) { repo.saveCustomHydrationDrink(any()) }
        assertEquals(0.15, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryNotice)
    }

    @Test fun `saved custom drink entry scales nutrients for selected portion and time`() = runTest {
        val entryTime = Instant.parse("2024-01-01T08:00:00Z")
        val drink = CustomHydrationDrink(
            id = "sports-drink",
            name = "Sports drink",
            volumeMilliliters = 100.0,
            nutrientValues = mapOf(
                NutritionNutrient.ENERGY to 40.0,
                NutritionNutrient.TOTAL_CARBOHYDRATE to 8.0,
            ),
        )
        val repo = entryRepo(customDrinks = listOf(drink))
        val nutritionRepo = nutritionRepo()
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.addSavedCustomDrinkEntry(
            drink = drink,
            amountMilliliters = 250.0,
            entryTime = entryTime,
        )
        advanceUntilIdle()

        coVerify {
            repo.writeHydrationEntry(match<HydrationWriteRequest> { request ->
                request.time == entryTime &&
                    request.volumeLiters == 0.25
            })
        }
        coVerify {
            nutritionRepo.writeNutritionEntry(match<NutritionWriteRequest> { request ->
                request.time == entryTime &&
                    request.name == "Sports drink" &&
                    request.associatedHydrationClientRecordId == "record-id" &&
                    request.nutrientValues[NutritionNutrient.ENERGY] == 100.0 &&
                    request.nutrientValues[NutritionNutrient.TOTAL_CARBOHYDRATE] == 20.0
            })
        }
        verify { repo.setLastCustomHydrationAmountMilliliters(250.0) }
        assertEquals(250.0, vm.uiState.value.lastCustomAmountMilliliters ?: 0.0, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `zero impact saved custom drink writes nutrients without hydration entry`() = runTest {
        val drink = CustomHydrationDrink(
            id = "wine",
            name = "Wine",
            volumeMilliliters = 150.0,
            hydrationMultiplier = 0.0,
            nutrientValues = mapOf(NutritionNutrient.ENERGY to 120.0),
        )
        val repo = entryRepo(customDrinks = listOf(drink))
        val nutritionRepo = nutritionRepo()
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.addSavedCustomDrinkEntry(drink)
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
        coVerify {
            nutritionRepo.writeNutritionEntry(match<NutritionWriteRequest> { request ->
                request.name == "Wine" &&
                    request.associatedHydrationClientRecordId == null &&
                    request.nutrientValues[NutritionNutrient.ENERGY] == 120.0
            })
        }
        verify(exactly = 0) { repo.saveCustomHydrationDrink(any()) }
        assertEquals(0.0, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertTrue(vm.uiState.value.saveCompleted)
        assertEquals(
            HydrationEntryNotice.NON_HYDRATING_DRINK_SAVED,
            vm.uiState.value.entryNotice,
        )
    }

    @Test fun `zero impact custom drink without nutrients saves reusable drink only`() = runTest {
        var savedDrinks = emptyList<CustomHydrationDrink>()
        val repo = entryRepo()
        every { repo.customHydrationDrinks() } answers { savedDrinks }
        every { repo.saveCustomHydrationDrink(any()) } answers {
            val savedDrink = firstArg<CustomHydrationDrink>()
            savedDrinks = listOf(savedDrink)
        }
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.saveCustomDrink(
            CustomHydrationDrinkInput(
                name = "Whiskey",
                volumeMilliliters = 45.0,
                hydrationMultiplier = 0.0,
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
        verify {
            repo.saveCustomHydrationDrink(match<CustomHydrationDrink> { drink ->
                drink.name == "Whiskey" &&
                    drink.volumeMilliliters == 45.0 &&
                    drink.hydrationMultiplier == 0.0 &&
                drink.nutrientValues.isEmpty()
            })
        }
        assertEquals(0.0, vm.uiState.value.todayHydrationLiters, 0.0001)
        assertEquals("Whiskey", vm.uiState.value.customDrinkOptions.single().name)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `saved custom drink entry reuses stored nutrients`() = runTest {
        val drink = CustomHydrationDrink(
            id = "coffee",
            name = "Coffee",
            volumeMilliliters = 150.0,
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 10.0),
        )
        val repo = entryRepo(customDrinks = listOf(drink))
        val nutritionRepo = nutritionRepo()
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.addSavedCustomDrinkEntry(drink)
        advanceUntilIdle()

        coVerify {
            nutritionRepo.writeNutritionEntry(match<NutritionWriteRequest> { request ->
                request.name == "Coffee" &&
                    request.nutrientValues[NutritionNutrient.CAFFEINE] == 10.0
            })
        }
        verify(exactly = 0) { repo.saveCustomHydrationDrink(any()) }
    }

    @Test fun `saving custom drink edit updates saved drink without writing hydration entry`() = runTest {
        var savedDrinks = listOf(
            CustomHydrationDrink(
                id = "coffee",
                name = "Coffee",
                volumeMilliliters = 150.0,
                category = CaffeineSourceCategory.COFFEE,
                nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 10.0),
            )
        )
        val repo = entryRepo()
        every { repo.customHydrationDrinks() } answers { savedDrinks }
        every { repo.saveCustomHydrationDrink(any()) } answers {
            val savedDrink = firstArg<CustomHydrationDrink>()
            savedDrinks = listOf(savedDrink)
        }
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.saveCustomDrink(
            CustomHydrationDrinkInput(
                name = "Latte",
                volumeMilliliters = 200.0,
                category = CaffeineSourceCategory.TEA,
                nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 20.0),
            ),
            "coffee",
        )

        verify {
            repo.saveCustomHydrationDrink(match<CustomHydrationDrink> { drink ->
                drink.id == "coffee" &&
                    drink.name == "Latte" &&
                    drink.volumeMilliliters == 200.0 &&
                    drink.category == CaffeineSourceCategory.TEA &&
                    drink.nutrientValues[NutritionNutrient.CAFFEINE] == 20.0
            })
        }
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
        assertEquals("Latte", vm.uiState.value.customDrinkOptions.single().name)
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `delete custom drink removes saved drink`() = runTest {
        val coffee = CustomHydrationDrink(
            id = "coffee",
            name = "Coffee",
            volumeMilliliters = 150.0,
        )
        val tea = CustomHydrationDrink(
            id = "tea",
            name = "Tea",
            volumeMilliliters = 200.0,
        )
        val repo = entryRepo(customDrinks = listOf(coffee, tea))
        every { repo.customHydrationDrinks() } returnsMany listOf(listOf(coffee, tea), listOf(tea))
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.deleteCustomDrink(coffee)

        verify { repo.deleteCustomHydrationDrink("coffee") }
        assertEquals(listOf("tea"), vm.uiState.value.customDrinkOptions.map { it.id })
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `move custom drink to target reorders and persists`() = runTest {
        val coffee = CustomHydrationDrink(
            id = "coffee",
            name = "Coffee",
            volumeMilliliters = 150.0,
        )
        val tea = CustomHydrationDrink(
            id = "tea",
            name = "Tea",
            volumeMilliliters = 200.0,
        )
        val juice = CustomHydrationDrink(
            id = "juice",
            name = "Juice",
            volumeMilliliters = 250.0,
        )
        val repo = entryRepo(customDrinks = listOf(coffee, tea, juice))
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.moveCustomDrinkToTarget("juice", "coffee")

        val expectedOrder = listOf("juice", "coffee", "tea")
        verify { repo.reorderCustomHydrationDrinks(expectedOrder) }
        assertEquals(expectedOrder, vm.uiState.value.customDrinkOptions.map { it.id })
        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `missing nutrition write permission prevents nutrient drink writes`() = runTest {
        val repo = entryRepo()
        val nutritionRepo = nutritionRepo(canWrite = false)
        val vm = HydrationEntryViewModel(repo, nutritionRepo)
        advanceUntilIdle()

        vm.addSavedCustomDrinkEntry(
            CustomHydrationDrink(
                id = "coffee",
                name = "Coffee",
                volumeMilliliters = 150.0,
                nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 10.0),
            )
        )
        advanceUntilIdle()

        assertEquals(HydrationEntryError.MISSING_NUTRITION_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeHydrationEntry(any()) }
        coVerify(exactly = 0) { nutritionRepo.writeNutritionEntry(any()) }
        verify(exactly = 0) { repo.saveCustomHydrationDrink(any()) }
    }

    @Test fun `invalid custom hydration entry keeps last custom amount`() = runTest {
        val repo = entryRepo()
        val vm = HydrationEntryViewModel(repo)
        advanceUntilIdle()

        vm.addCustomHydrationEntry(425.0)
        advanceUntilIdle()
        vm.addCustomHydrationEntry(0.0)
        advanceUntilIdle()

        assertEquals(425.0, vm.uiState.value.lastCustomAmountMilliliters ?: 0.0, 0.0001)
        assertEquals(HydrationEntryError.INVALID_AMOUNT, vm.uiState.value.entryError)
        verify(exactly = 0) { repo.setLastCustomHydrationAmountMilliliters(0.0) }
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
        assertEquals(ScreenError.Message("denied"), vm.uiState.value.writeError)
    }

    private fun hydrationEntry(
        drinkId: String,
        time: String,
    ): HydrationEntry {
        val instant = Instant.parse(time)
        return HydrationEntry(
            startTime = instant,
            endTime = instant.plusSeconds(1),
            liters = 0.25,
            source = "OpenVitals",
            clientRecordId = "openvitals_hydration_${instant.toEpochMilli()}_drink_${drinkId}_record",
            isOpenVitalsEntry = true,
        )
    }

    private fun nutritionEntry(
        name: String,
        time: String,
        clientRecordId: String = "openvitals_nutrition_${Instant.parse(time).toEpochMilli()}",
    ): NutritionEntry {
        val instant = Instant.parse(time)
        return NutritionEntry(
            time = instant,
            mealType = 0,
            name = name,
            energyKcal = null,
            proteinGrams = null,
            carbsGrams = null,
            fatGrams = null,
            fiberGrams = null,
            sugarGrams = null,
            source = "OpenVitals",
            clientRecordId = clientRecordId,
            isOpenVitalsEntry = true,
        )
    }
}
