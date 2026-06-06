package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.repository.ActivitiesPeriodData
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun emptyRepo() = mockk<ActivityRepository>().also { repo ->
        coEvery { repo.loadWorkouts(any(), any()) } returns emptyList()
        coEvery { repo.deleteActivityEntry(any()) } returns Unit
        coEvery { repo.loadActivitiesPeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            ActivitiesPeriodData(
                workouts = repo.loadWorkouts(windows.current.start, windows.current.end),
                previousWorkouts = repo.loadWorkouts(windows.previous.start, windows.previous.end),
                baselineWorkouts = repo.loadWorkouts(windows.baseline.start, windows.baseline.end),
            )
        }
    }

    @Test fun `deleteActivityEntry removes OpenVitals workout and reloads`() = runTest {
        val workout = workout(
            id = "activity-id",
            source = "tech.mmarca.openvitals.debug",
            isOpenVitalsEntry = true,
        )
        var workouts = listOf(workout)
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } answers { workouts }
        coEvery { repo.deleteActivityEntry("activity-id") } coAnswers {
            workouts = emptyList()
        }
        val vm = ActivitiesViewModel(repo)

        vm.deleteActivityEntry("activity-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.workouts.isEmpty())
        coVerify { repo.deleteActivityEntry("activity-id") }
        coVerify(atLeast = 2) { repo.loadActivitiesPeriod(any()) }
    }

    @Test fun `deleteActivityEntry ignores workout not created by OpenVitals`() = runTest {
        val workouts = listOf(
            workout(
                id = "external-activity-id",
                source = "com.example",
                isOpenVitalsEntry = false,
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadWorkouts(any(), any()) } returns workouts
        val vm = ActivitiesViewModel(repo)

        vm.deleteActivityEntry("external-activity-id")
        advanceUntilIdle()

        assertEquals(workouts, vm.uiState.value.workouts)
        coVerify(exactly = 0) { repo.deleteActivityEntry("external-activity-id") }
    }

    private fun workout(
        id: String,
        source: String,
        isOpenVitalsEntry: Boolean,
    ): ExerciseData {
        val start = Instant.parse("2026-05-26T08:00:00Z")
        val end = start.plusSeconds(1_800)
        return ExerciseData(
            id = id,
            title = "Walk",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            startTime = start,
            endTime = end,
            durationMs = 1_800_000,
            source = source,
            isOpenVitalsEntry = isOpenVitalsEntry,
        )
    }
}
