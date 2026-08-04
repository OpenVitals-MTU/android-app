package tech.mmarca.openvitals.features.bodyenergy

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Port of test/features/bodyenergy/body_energy_view_model_test.dart: the
 * repository returns whatever it is told to, so the view-model's own behaviour
 * — the display precompute, the failure mapping, the staleness guard — is what
 * is under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BodyEnergyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()

    private fun prefs(): PreferencesRepository = mockk<PreferencesRepository>().also {
        every { it.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
        every { it.bodyProfile() } returns BodyProfile()
    }

    private fun point(date: LocalDate, hour: Long, score: Int): BodyEnergyTimelinePoint =
        BodyEnergyTimelinePoint(
            time = date.atStartOfDay(zone).toInstant().plusSeconds(hour * 3600),
            score = score,
            delta = 1.0,
            state = BodyEnergyBucketState.REST,
            confidence = BodyEnergyConfidence.HIGH,
        )

    private fun timeline(date: LocalDate, currentScore: Int = 62): BodyEnergyTimeline =
        BodyEnergyTimeline(
            date = date,
            startScore = 50,
            currentScore = currentScore,
            charged = 14,
            drained = 2,
            points = listOf(
                point(date, 7, 54),
                point(date, 12, 60),
                point(date, 17, currentScore),
            ),
            confidence = BodyEnergyConfidence.HIGH,
            confidenceReason = "test",
        )

    private fun viewModel(repository: BodyEnergyRepository): BodyEnergyViewModel =
        BodyEnergyViewModel(
            repository = repository,
            preferencesRepository = prefs(),
        )

    @Test
    fun `a loaded day lands with its display precomputed`() = runTest {
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } coAnswers {
            BodyEnergyTimelineResult(firstArg(), listOf(timeline(today)))
        }

        val vm = viewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        // The screen renders this; it must exist by the time loading ends.
        assertFalse(state.display.isEmpty)
        assertEquals(3, state.display.chartPoints.size)
        assertEquals(62, state.display.timeline?.currentScore)
        assertTrue(state.display.inputRows.isNotEmpty())
    }

    @Test
    fun `a day with no timeline at all still gives the screen a display`() = runTest {
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } coAnswers {
            BodyEnergyTimelineResult(firstArg(), emptyList())
        }

        val vm = viewModel(repo)
        advanceUntilIdle()

        val display = vm.uiState.value.display
        assertTrue(display.isEmpty)
        assertTrue(display.chartPoints.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } throws SecurityException("heart rate read")

        val vm = viewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ScreenError.PermissionDenied, state.error)
        assertTrue(state.display.isEmpty)
    }

    @Test
    fun `an unexpected failure carries its message to the screen`() = runTest {
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } throws RuntimeException("the timeline blew up")

        val vm = viewModel(repo)
        advanceUntilIdle()

        assertEquals(
            ScreenError.Message("the timeline blew up"),
            vm.uiState.value.error,
        )
    }

    @Test
    fun `a future day is clamped to today`() = runTest {
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } coAnswers {
            BodyEnergyTimelineResult(firstArg(), listOf(timeline(today)))
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.selectDate(today.plusDays(5))
        advanceUntilIdle()

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test
    fun `a stale load cannot overwrite the newer one it lost to`() = runTest {
        val yesterday = today.minusDays(1)
        val staleDay = today.minusDays(2)
        val gate = CompletableDeferred<Unit>()
        val repo = mockk<BodyEnergyRepository>()
        coEvery { repo.loadTimeline(any()) } coAnswers {
            val query = firstArg<BodyEnergyTimelineQuery>()
            when (query.period.start) {
                staleDay -> {
                    // Held in flight; by the time it answers, a newer load won.
                    gate.await()
                    BodyEnergyTimelineResult(query, listOf(timeline(staleDay, currentScore = 11)))
                }
                yesterday ->
                    BodyEnergyTimelineResult(query, listOf(timeline(yesterday, currentScore = 71)))
                else -> BodyEnergyTimelineResult(query, emptyList())
            }
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        // Two loads race; the FIRST one answers last.
        vm.selectDate(staleDay)
        vm.selectDate(yesterday)
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        // The newer day won: the older day's late answer is dropped, not painted.
        val state = vm.uiState.value
        assertEquals(yesterday, state.selectedDate)
        assertEquals(71, state.display.timeline?.currentScore)
        assertFalse(state.isLoading)
    }
}
