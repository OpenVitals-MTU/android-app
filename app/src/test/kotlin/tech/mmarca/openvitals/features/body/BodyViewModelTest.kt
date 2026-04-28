package tech.mmarca.openvitals.features.body

import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
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

class BodyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<BodyRepository>().also { repo ->
        coEvery { repo.loadWeightEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestHeight() } returns null
        coEvery { repo.loadBodyFatEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestLeanBodyMass() } returns null
        coEvery { repo.loadLatestBMR() } returns null
        coEvery { repo.loadLatestBoneMass() } returns null
    }

    private fun weightAt(weightKg: Double, epochSeconds: Long) =
        WeightEntry(time = Instant.ofEpochSecond(epochSeconds), weightKg = weightKg, source = "test")

    private fun bodyFatAt(percent: Double, epochSeconds: Long) =
        BodyFatEntry(time = Instant.ofEpochSecond(epochSeconds), percent = percent, source = "test")

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test fun `initial range is MONTH`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and produces no error`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `initial state has empty weight entries and all nulls`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        val state = vm.uiState.value
        assertTrue(state.weightEntries.isEmpty())
        assertNull(state.heightCm)
        assertTrue(state.bodyFatEntries.isEmpty())
        assertNull(state.leanMassKg)
        assertNull(state.bmrKcal)
        assertNull(state.boneMassKg)
    }

    // ─── Load success ─────────────────────────────────────────────────────────

    @Test fun `load success populates weight entries`() = runTest {
        val entries = listOf(weightAt(75.0, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadWeightEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo)

        assertEquals(entries, vm.uiState.value.weightEntries)
    }

    @Test fun `load success populates height`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadLatestHeight() } returns 178.0

        val vm = BodyViewModel(repo)

        assertEquals(178.0, vm.uiState.value.heightCm!!, 0.01)
    }

    @Test fun `load success populates body fat entries`() = runTest {
        val entries = listOf(bodyFatAt(22.5, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadBodyFatEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo)

        assertEquals(entries, vm.uiState.value.bodyFatEntries)
    }

    @Test fun `load success populates lean mass`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadLatestLeanBodyMass() } returns 58.3

        val vm = BodyViewModel(repo)

        assertEquals(58.3, vm.uiState.value.leanMassKg!!, 0.01)
    }

    @Test fun `load success populates BMR`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadLatestBMR() } returns 1_750.0

        val vm = BodyViewModel(repo)

        assertEquals(1_750.0, vm.uiState.value.bmrKcal!!, 0.01)
    }

    @Test fun `load success populates bone mass`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadLatestBoneMass() } returns 3.2

        val vm = BodyViewModel(repo)

        assertEquals(3.2, vm.uiState.value.boneMassKg!!, 0.001)
    }

    // ─── Load failure ─────────────────────────────────────────────────────────

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<BodyRepository>()
        coEvery { repo.loadWeightEntries(any(), any()) } throws RuntimeException("timeout")
        coEvery { repo.loadLatestHeight() } returns null
        coEvery { repo.loadBodyFatEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestLeanBodyMass() } returns null
        coEvery { repo.loadLatestBMR() } returns null
        coEvery { repo.loadLatestBoneMass() } returns null

        val vm = BodyViewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("timeout", vm.uiState.value.error)
    }

    // ─── BodyUiState.bmi ─────────────────────────────────────────────────────

    @Test fun `bmi is null when height is missing`() {
        val state = BodyUiState(weightEntries = listOf(weightAt(75.0, 1_000)), heightCm = null)
        assertNull(state.bmi)
    }

    @Test fun `bmi is null when weight entries are empty`() {
        val state = BodyUiState(weightEntries = emptyList(), heightCm = 178.0)
        assertNull(state.bmi)
    }

    @Test fun `bmi is null for zero height`() {
        val state = BodyUiState(weightEntries = listOf(weightAt(75.0, 1_000)), heightCm = 0.0)
        assertNull(state.bmi)
    }

    @Test fun `bmi computed correctly from weight and height`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(75.0, 1_000)),
            heightCm = 178.0,
        )
        // 75 / (1.78^2) ≈ 23.67
        assertEquals(23.67, state.bmi!!, 0.01)
    }

    @Test fun `bmi uses most recent weight entry`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(80.0, 1_000), weightAt(76.0, 2_000)),
            heightCm = 180.0,
        )
        // 76 / (1.80^2) ≈ 23.46
        assertEquals(23.46, state.bmi!!, 0.01)
    }

    // ─── BodyUiState.latestWeightKg / firstWeightKg / weightChangKg ──────────

    @Test fun `latestWeightKg returns the most recent entry by time`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(72.0, 1_000), weightAt(74.5, 2_000)),
        )
        assertEquals(74.5, state.latestWeightKg!!, 0.01)
    }

    @Test fun `firstWeightKg returns the earliest entry by time`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(74.5, 2_000), weightAt(72.0, 1_000)),
        )
        assertEquals(72.0, state.firstWeightKg!!, 0.01)
    }

    @Test fun `weightChangKg is positive when weight increased`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(70.0, 1_000), weightAt(73.5, 2_000)),
        )
        assertEquals(3.5, state.weightChangKg!!, 0.01)
    }

    @Test fun `weightChangKg is negative when weight decreased`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(80.0, 1_000), weightAt(76.0, 2_000)),
        )
        assertEquals(-4.0, state.weightChangKg!!, 0.01)
    }

    @Test fun `weightChangKg is null when only one entry exists`() {
        val state = BodyUiState(weightEntries = listOf(weightAt(75.0, 1_000)))
        assertNull(state.weightChangKg)
    }

    @Test fun `weightChangKg is null when weight entries are empty`() {
        val state = BodyUiState(weightEntries = emptyList())
        assertNull(state.weightChangKg)
    }

    @Test fun `weightChangKg is null when first and latest weight are equal`() {
        val state = BodyUiState(
            weightEntries = listOf(weightAt(75.0, 1_000), weightAt(75.0, 2_000)),
        )
        assertNull(state.weightChangKg)
    }

    // ─── BodyUiState.latestBodyFatPercent ────────────────────────────────────

    @Test fun `latestBodyFatPercent returns most recent entry`() {
        val state = BodyUiState(
            bodyFatEntries = listOf(bodyFatAt(25.0, 1_000), bodyFatAt(22.0, 2_000)),
        )
        assertEquals(22.0, state.latestBodyFatPercent!!, 0.01)
    }

    @Test fun `latestBodyFatPercent is null when entries are empty`() {
        val state = BodyUiState(bodyFatEntries = emptyList())
        assertNull(state.latestBodyFatPercent)
    }

    // ─── selectRange ──────────────────────────────────────────────────────────

    @Test fun `selectRange updates selectedRange`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectRange(TimeRange.YEAR)
        assertEquals(TimeRange.YEAR, vm.uiState.value.selectedRange)
    }

    @Test fun `selectRange triggers reload`() = runTest {
        val repo = emptyRepo()
        val vm = BodyViewModel(repo)
        vm.selectRange(TimeRange.YEAR)
        // init load + selectRange load = 2 calls
        io.mockk.coVerify(atLeast = 2) { repo.loadWeightEntries(any(), any()) }
    }

    // ─── previousPeriod / nextPeriod ──────────────────────────────────────────

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectRange(TimeRange.WEEK)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod YEAR moves back one year`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectRange(TimeRange.YEAR)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusYears(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod MONTH is blocked when current month includes today`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        val before = vm.uiState.value.selectedDate
        vm.nextPeriod()
        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod MONTH advances from a past anchor`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectDate(today.minusMonths(2))
        val before = vm.uiState.value.selectedDate
        vm.nextPeriod()
        assertEquals(before.plusMonths(1), vm.uiState.value.selectedDate)
    }

    // ─── selectDate ───────────────────────────────────────────────────────────

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectDate(today.plusDays(5))
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate accepts past date unchanged`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        vm.selectDate(pastAnchor)
        assertEquals(pastAnchor, vm.uiState.value.selectedDate)
    }
}
