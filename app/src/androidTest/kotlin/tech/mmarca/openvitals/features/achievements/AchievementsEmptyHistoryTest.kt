package tech.mmarca.openvitals.features.achievements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of "shows the no-activity message with empty history" from Flutter's
 * `test/features/achievements/achievements_screen_test.dart`.
 *
 * Every badge is locked on a phone that has never recorded a step, and a wall
 * of locked badges looks exactly like a broken permission. The screen has to
 * say which of the two it is, because the fix is different: grant history
 * access, or go for a walk.
 */
class AchievementsEmptyHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anEmptyHistorySaysSoRatherThanLeavingEveryBadgeUnexplained() {
        val viewModel = AchievementsViewModel(
            activityRepository = NoActivityRepository,
            dispatchers = DefaultDispatcherProvider,
        )
        composeRule.setContent {
            OpenVitalsTheme {
                AchievementsScreen(
                    viewModel = viewModel,
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }
        composeRule.waitUntil(TIMEOUT_MS) { !viewModel.uiState.value.isLoading }

        val noData = string(R.string.achievements_no_data_title)
        // The screen's outer LazyColumn. The stats strip and the category filter
        // chips are lazy rows of their own, so the matcher is not unique — and
        // the outer list is the ancestor, so it is the one traversed first.
        composeRule
            .onAllNodes(hasScrollToIndexAction())
            .onFirst()
            .performScrollToNode(hasText(noData))
        composeRule.onNodeWithText(noData).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.achievements_no_data_body))
            .assertIsDisplayed()
        // Not the error message: nothing failed, there is simply nothing there.
        composeRule.onNodeWithText(string(R.string.achievements_error_title)).assertDoesNotExist()
    }

    /** A phone Health Connect has no step or distance history for. */
    private object NoActivityRepository : ActivityRepository {
        override suspend fun loadDailySteps(
            start: LocalDate,
            end: LocalDate,
            includeWheelchairPushes: Boolean,
        ): List<DailySteps> = emptyList()

        override suspend fun loadActivityPeriod(
            query: tech.mmarca.openvitals.core.period.PeriodLoadQuery,
            includeSteps: Boolean,
            includeNutrition: Boolean,
            includeWheelchairPushes: Boolean,
            includeActivityProgress: Boolean,
            includeComparisonWindows: Boolean,
            refreshMode: tech.mmarca.openvitals.domain.model.RefreshMode,
        ) = error("unused")

        override suspend fun loadActivitiesPeriod(
            query: tech.mmarca.openvitals.core.period.PeriodLoadQuery,
            refreshMode: tech.mmarca.openvitals.domain.model.RefreshMode,
        ) = error("unused")

        override suspend fun loadActivityProgress(date: LocalDate) = error("unused")

        override suspend fun loadWorkouts(start: LocalDate, end: LocalDate) = error("unused")

        override suspend fun loadWorkoutsWithMetrics(start: LocalDate, end: LocalDate) =
            error("unused")

        override suspend fun loadWorkout(id: String) = error("unused")

        override suspend fun loadSpeedSamples(start: Instant, end: Instant) = error("unused")

        override suspend fun loadActivityCadenceSamples(start: Instant, end: Instant) =
            error("unused")

        override suspend fun loadPlannedWorkouts(start: LocalDate, end: LocalDate) = error("unused")

        override suspend fun loadExistingPlannedWorkouts(anchorDate: LocalDate) = error("unused")

        override suspend fun writePlannedWorkout(
            request: tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest,
        ) = error("unused")

        override suspend fun loadPlannedWorkout(id: String) = error("unused")

        override suspend fun deletePlannedWorkout(id: String) = error("unused")

        override suspend fun loadDailyNutrition(start: LocalDate, end: LocalDate) = error("unused")

        override fun activityWritePermissions(): Set<String> = emptySet()

        override fun activityWritePermissions(
            includeRoute: Boolean,
            includeDistance: Boolean,
            includeElevation: Boolean,
            includeActiveCalories: Boolean,
            includeTotalCalories: Boolean,
            includeSteps: Boolean,
        ): Set<String> = emptySet()

        override fun activityWritePermissions(request: ActivityWriteRequest): Set<String> =
            emptySet()

        override fun plannedWorkoutWritePermissions(): Set<String> = emptySet()

        override suspend fun hasActivityWritePermission(): Boolean = false

        override suspend fun hasActivityWritePermission(
            includeRoute: Boolean,
            includeDistance: Boolean,
            includeElevation: Boolean,
            includeActiveCalories: Boolean,
            includeTotalCalories: Boolean,
            includeSteps: Boolean,
        ): Boolean = false

        override suspend fun hasActivityWritePermission(request: ActivityWriteRequest): Boolean =
            false

        override suspend fun writeActivityEntry(request: ActivityWriteRequest): String =
            error("unused")

        override suspend fun writeActivityEntries(
            requests: List<ActivityWriteRequest>,
        ): List<String> = error("unused")

        override suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest) =
            error("unused")

        override suspend fun deleteActivityEntry(id: String) = error("unused")
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
