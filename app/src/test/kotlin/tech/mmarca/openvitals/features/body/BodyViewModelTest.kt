package tech.mmarca.openvitals.features.body

import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.data.repository.BodyPeriodData
import tech.mmarca.openvitals.data.repository.BodyPeriodMetric
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<BodyRepository>().also { repo ->
        coEvery { repo.loadWeightEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestHeight() } returns null
        coEvery { repo.loadHeightEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadBodyFatEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestLeanBodyMass() } returns null
        coEvery { repo.loadLeanBodyMassEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestBMR() } returns null
        coEvery { repo.loadBmrEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestBoneMass() } returns null
        coEvery { repo.loadBoneMassEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadLatestBodyWaterMass() } returns null
        coEvery { repo.loadBodyWaterMassEntries(any(), any()) } returns emptyList()
        coEvery { repo.deleteBodyMeasurementEntry(any(), any()) } returns Unit
        coEvery { repo.loadBodyPeriod(any(), any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val metric = secondArg<BodyPeriodMetric>()
            val windows = query.windows
            when (metric) {
                BodyPeriodMetric.WEIGHT -> BodyPeriodData(
                    weightEntries = repo.loadWeightEntries(windows.current.start, windows.current.end),
                    previousWeightEntries = repo.loadWeightEntries(windows.previous.start, windows.previous.end),
                    baselineWeightEntries = repo.loadWeightEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.HEIGHT -> BodyPeriodData(
                    heightEntries = repo.loadHeightEntries(windows.current.start, windows.current.end),
                    previousHeightEntries = repo.loadHeightEntries(windows.previous.start, windows.previous.end),
                    baselineHeightEntries = repo.loadHeightEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.BMI -> BodyPeriodData(
                    weightEntries = repo.loadWeightEntries(windows.current.start, windows.current.end),
                    previousWeightEntries = repo.loadWeightEntries(windows.previous.start, windows.previous.end),
                    baselineWeightEntries = repo.loadWeightEntries(windows.baseline.start, windows.baseline.end),
                    heightCm = repo.loadLatestHeight(),
                )
                BodyPeriodMetric.BODY_FAT -> BodyPeriodData(
                    bodyFatEntries = repo.loadBodyFatEntries(windows.current.start, windows.current.end),
                    previousBodyFatEntries = repo.loadBodyFatEntries(windows.previous.start, windows.previous.end),
                    baselineBodyFatEntries = repo.loadBodyFatEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.LEAN_MASS -> BodyPeriodData(
                    leanMassEntries = repo.loadLeanBodyMassEntries(windows.current.start, windows.current.end),
                    previousLeanMassEntries = repo.loadLeanBodyMassEntries(windows.previous.start, windows.previous.end),
                    baselineLeanMassEntries = repo.loadLeanBodyMassEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.BMR -> BodyPeriodData(
                    bmrEntries = repo.loadBmrEntries(windows.current.start, windows.current.end),
                    previousBmrEntries = repo.loadBmrEntries(windows.previous.start, windows.previous.end),
                    baselineBmrEntries = repo.loadBmrEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.BONE_MASS -> BodyPeriodData(
                    boneMassEntries = repo.loadBoneMassEntries(windows.current.start, windows.current.end),
                    previousBoneMassEntries = repo.loadBoneMassEntries(windows.previous.start, windows.previous.end),
                    baselineBoneMassEntries = repo.loadBoneMassEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.BODY_WATER_MASS -> BodyPeriodData(
                    bodyWaterMassEntries = repo.loadBodyWaterMassEntries(windows.current.start, windows.current.end),
                    previousBodyWaterMassEntries = repo.loadBodyWaterMassEntries(windows.previous.start, windows.previous.end),
                    baselineBodyWaterMassEntries = repo.loadBodyWaterMassEntries(windows.baseline.start, windows.baseline.end),
                )
                BodyPeriodMetric.ALL -> BodyPeriodData(
                    weightEntries = repo.loadWeightEntries(windows.current.start, windows.current.end),
                    heightEntries = repo.loadHeightEntries(windows.current.start, windows.current.end),
                    bodyFatEntries = repo.loadBodyFatEntries(windows.current.start, windows.current.end),
                    leanMassEntries = repo.loadLeanBodyMassEntries(windows.current.start, windows.current.end),
                    bmrEntries = repo.loadBmrEntries(windows.current.start, windows.current.end),
                    boneMassEntries = repo.loadBoneMassEntries(windows.current.start, windows.current.end),
                    bodyWaterMassEntries = repo.loadBodyWaterMassEntries(windows.current.start, windows.current.end),
                )
            }
        }
    }

    private fun weightAt(weightKg: Double, epochSeconds: Long) =
        WeightEntry(time = Instant.ofEpochSecond(epochSeconds), weightKg = weightKg, source = "test")

    private fun bodyFatAt(percent: Double, epochSeconds: Long) =
        BodyFatEntry(time = Instant.ofEpochSecond(epochSeconds), percent = percent, source = "test")

    private fun heightAt(heightCm: Double, epochSeconds: Long) =
        HeightEntry(time = Instant.ofEpochSecond(epochSeconds), heightCm = heightCm, source = "test")

    private fun leanMassAt(massKg: Double, epochSeconds: Long) =
        LeanBodyMassEntry(time = Instant.ofEpochSecond(epochSeconds), massKg = massKg, source = "test")

    private fun bmrAt(kcalPerDay: Double, epochSeconds: Long) =
        BmrEntry(time = Instant.ofEpochSecond(epochSeconds), kcalPerDay = kcalPerDay, source = "test")

    private fun boneMassAt(massKg: Double, epochSeconds: Long) =
        BoneMassEntry(time = Instant.ofEpochSecond(epochSeconds), massKg = massKg, source = "test")

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test fun `initial range is MONTH`() = runTest {
        val vm = BodyViewModel(emptyRepo())
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `initial range can be restored`() = runTest {
        val vm = BodyViewModel(emptyRepo(), initialRange = TimeRange.YEAR)
        assertEquals(TimeRange.YEAR, vm.uiState.value.selectedRange)
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
        assertTrue(state.heightEntries.isEmpty())
        assertTrue(state.bodyFatEntries.isEmpty())
        assertNull(state.leanMassKg)
        assertTrue(state.leanMassEntries.isEmpty())
        assertNull(state.bmrKcal)
        assertTrue(state.bmrEntries.isEmpty())
        assertNull(state.boneMassKg)
        assertTrue(state.boneMassEntries.isEmpty())
    }

    // ─── Load success ─────────────────────────────────────────────────────────

    @Test fun `load success populates weight entries`() = runTest {
        val entries = listOf(weightAt(75.0, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadWeightEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo)

        assertEquals(entries, vm.uiState.value.weightEntries)
    }

    @Test fun `deleteBodyMeasurementEntry removes OpenVitals weight and reloads`() = runTest {
        val entry = WeightEntry(
            time = Instant.ofEpochSecond(1_000),
            weightKg = 75.0,
            source = "tech.mmarca.openvitals.debug",
            id = "weight-id",
            isOpenVitalsEntry = true,
        )
        var entries = listOf(entry)
        val repo = emptyRepo()
        coEvery { repo.loadWeightEntries(any(), any()) } answers { entries }
        coEvery { repo.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id") } coAnswers {
            entries = emptyList()
        }
        val vm = BodyViewModel(repo)

        vm.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.weightEntries.isEmpty())
        coVerify { repo.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "weight-id") }
        coVerify(atLeast = 2) { repo.loadBodyPeriod(any(), any()) }
    }

    @Test fun `deleteBodyMeasurementEntry ignores weight not created by OpenVitals`() = runTest {
        val entries = listOf(
            WeightEntry(
                time = Instant.ofEpochSecond(1_000),
                weightKg = 75.0,
                source = "com.example",
                id = "external-weight-id",
                isOpenVitalsEntry = false,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadWeightEntries(any(), any()) } returns entries
        val vm = BodyViewModel(repo)

        vm.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "external-weight-id")
        advanceUntilIdle()

        assertEquals(entries, vm.uiState.value.weightEntries)
        coVerify(exactly = 0) { repo.deleteBodyMeasurementEntry(BodyMeasurementType.WEIGHT, "external-weight-id") }
    }

    @Test fun `load success populates height`() = runTest {
        val entries = listOf(heightAt(178.0, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadHeightEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo, selectedMetric = BodyMetric.HEIGHT)

        assertEquals(178.0, vm.uiState.value.heightCm!!, 0.01)
        assertEquals(entries, vm.uiState.value.heightEntries)
    }

    @Test fun `load success populates body fat entries`() = runTest {
        val entries = listOf(bodyFatAt(22.5, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadBodyFatEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo, selectedMetric = BodyMetric.BODY_FAT)

        assertEquals(entries, vm.uiState.value.bodyFatEntries)
    }

    @Test fun `load success populates lean mass`() = runTest {
        val entries = listOf(leanMassAt(58.3, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadLeanBodyMassEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo, selectedMetric = BodyMetric.LEAN_MASS)

        assertEquals(58.3, vm.uiState.value.leanMassKg!!, 0.01)
        assertEquals(entries, vm.uiState.value.leanMassEntries)
    }

    @Test fun `load success populates BMR`() = runTest {
        val entries = listOf(bmrAt(1_750.0, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadBmrEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo, selectedMetric = BodyMetric.BMR)

        assertEquals(1_750.0, vm.uiState.value.bmrKcal!!, 0.01)
        assertEquals(entries, vm.uiState.value.bmrEntries)
    }

    @Test fun `load success populates bone mass`() = runTest {
        val entries = listOf(boneMassAt(3.2, 1_000))
        val repo = emptyRepo()
        coEvery { repo.loadBoneMassEntries(any(), any()) } returns entries

        val vm = BodyViewModel(repo, selectedMetric = BodyMetric.BONE_MASS)

        assertEquals(3.2, vm.uiState.value.boneMassKg!!, 0.001)
        assertEquals(entries, vm.uiState.value.boneMassEntries)
    }

    // ─── Load failure ─────────────────────────────────────────────────────────

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<BodyRepository>()
        coEvery { repo.loadBodyPeriod(any(), any()) } throws RuntimeException("timeout")

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

    @Test fun `selectRange saves selected range`() = runTest {
        var savedRange: TimeRange? = null
        val vm = BodyViewModel(
            repository = emptyRepo(),
            onRangeSelected = { range -> savedRange = range },
        )

        vm.selectRange(TimeRange.WEEK)

        assertEquals(TimeRange.WEEK, savedRange)
    }

    @Test fun `selectRange triggers reload`() = runTest {
        val repo = emptyRepo()
        val vm = BodyViewModel(repo)
        vm.selectRange(TimeRange.YEAR)
        // init load + selectRange load = 2 calls
        io.mockk.coVerify(atLeast = 2) { repo.loadBodyPeriod(any(), any()) }
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
