package tech.mmarca.openvitals.features.homewidgets

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogSuccess

/**
 * One tap on the quick-beverage tile. The write comes first, and the
 * broadcast never waits for the label revert or past its budget.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeQuickBeverageLogActionTest {
    private val water = CustomHydrationDrink(
        id = "water",
        name = "Water",
        volumeMilliliters = 250.0,
        hydrationMultiplier = 1.0,
    )
    private val espresso = CustomHydrationDrink(
        id = "espresso",
        name = "Espresso",
        volumeMilliliters = 30.0,
        hydrationMultiplier = 0.5,
        nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 63.0),
    )

    private val hydration = mockk<HydrationRepository>(relaxed = true)
    private val nutrition = mockk<NutritionRepository>(relaxed = true)

    /** Every call in order: "write", then each status as it is shown. */
    private val events = mutableListOf<String>()
    private val logged = mutableListOf<HydrationDrinkLogSuccess>()

    private val deps = QuickBeverageLogDeps(
        hydrationRepository = hydration,
        nutritionRepository = nutrition,
        onLogged = { logged += it },
        showStatus = { _, status -> events += status.name },
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        coEvery { hydration.customHydrationDrinks() } returns listOf(water, espresso)
        coEvery { hydration.hasHydrationWritePermission() } returns true
        coEvery { nutrition.hasNutritionWritePermission() } returns true
        coEvery { hydration.writeHydrationEntry(any()) } coAnswers {
            events += "write"
            "record-1"
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `the write lands before the tile changes, and Saved comes before the revert`() = runTest {
        runQuickBeverageTap("water", deps, scope = this)
        advanceUntilIdle()

        assertThat(events).containsExactly("write", "SAVED", "TAP_TO_LOG").inOrder()
        assertThat(logged).hasSize(1)
    }

    @Test
    fun `one drink read and one check per permission`() = runTest {
        runQuickBeverageTap("water", deps, scope = this)
        advanceUntilIdle()

        coVerify(exactly = 1) { hydration.customHydrationDrinks() }
        coVerify(exactly = 1) { hydration.hasHydrationWritePermission() }
        coVerify(exactly = 1) { nutrition.hasNutritionWritePermission() }
    }

    @Test
    fun `the broadcast ends at Saved and does not wait for the revert`() = runTest {
        runQuickBeverageTap("water", deps, scope = this, revertDelayMillis = 1_200L)

        assertThat(currentTime).isEqualTo(0L)
        assertThat(events).containsExactly("write", "SAVED").inOrder()

        advanceUntilIdle()
        assertThat(currentTime).isEqualTo(1_200L)
        assertThat(events.last()).isEqualTo("TAP_TO_LOG")
    }

    @Test
    fun `a slow write frees the broadcast at the budget and still lands`() = runTest {
        val release = CompletableDeferred<Unit>()
        coEvery { hydration.writeHydrationEntry(any()) } coAnswers {
            release.await()
            events += "write"
            "record-1"
        }
        val appScope = TestScope(testScheduler)

        runQuickBeverageTap("water", deps, scope = appScope, budgetMillis = 8_000L)

        assertThat(currentTime).isEqualTo(8_000L)
        assertThat(events).isEmpty()

        release.complete(Unit)
        advanceUntilIdle()
        assertThat(events).containsExactly("write", "SAVED", "TAP_TO_LOG").inOrder()
    }

    @Test
    fun `a failed nutrition write rolls the hydration record back and says so`() = runTest {
        coEvery { nutrition.writeNutritionEntry(any()) } throws IllegalStateException("rate limited")

        runQuickBeverageTap("espresso", deps, scope = this)
        advanceUntilIdle()

        coVerify(exactly = 1) { hydration.deleteHydrationEntryByClientRecordId("record-1") }
        assertThat(events).containsExactly("write", "FAILED").inOrder()
        assertThat(logged).isEmpty()
    }

    @Test
    fun `a missing permission writes nothing`() = runTest {
        coEvery { hydration.hasHydrationWritePermission() } returns false

        runQuickBeverageTap("water", deps, scope = this)
        advanceUntilIdle()

        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        assertThat(events).containsExactly("PERMISSION_NEEDED")
    }

    @Test
    fun `a drink that no longer exists shows not configured`() = runTest {
        runQuickBeverageTap("gone", deps, scope = this)
        advanceUntilIdle()

        coVerify(exactly = 0) { hydration.writeHydrationEntry(any()) }
        assertThat(events).containsExactly("NOT_CONFIGURED")
    }

    @Test
    fun `a follow-up failure after the write still shows Saved`() = runTest {
        every { hydration.recordRecentHydrationAmountMilliliters(any()) } throws IllegalStateException("prefs")

        runQuickBeverageTap("water", deps, scope = this)
        advanceUntilIdle()

        assertThat(events).containsExactly("write", "SAVED", "TAP_TO_LOG").inOrder()
    }
}
