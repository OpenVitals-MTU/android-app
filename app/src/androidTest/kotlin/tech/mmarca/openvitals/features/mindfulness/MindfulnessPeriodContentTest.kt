package tech.mmarca.openvitals.features.mindfulness

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Whether the content draws what the display state decided. The totals are covered by
 * `MindfulnessPresentationMapperTest`; the access gate by `HealthConnectAccessGateTest`.
 * Session order is tested here because Kotlin sorts inside the content.
 */
class MindfulnessPeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheTotalCardAndSessionListOnceLoaded() {
        setContent(state(sessions = listOf(session()), hasData = true))

        composeRule.onNodeWithText(string(R.string.metric_mindfulness)).assertIsDisplayed()
        // The session itself, not only the heading.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(SESSION_TITLE))
        composeRule.onNodeWithText(SESSION_TITLE).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyPlaceholderWithNoSessions() {
        // An explicit "nothing recorded", because an empty period and a failed load look identical otherwise.
        setContent(state(sessions = emptyList(), hasData = false, isLoading = false))

        composeRule.onNodeWithText(string(R.string.message_no_mindfulness_period)).assertIsDisplayed()
    }

    @Test
    fun aLoadingPeriodDoesNotClaimThereIsNothingToShow() {
        // Announcing "no sessions" before the read returns tells the user something false.
        setContent(state(sessions = emptyList(), hasData = false, isLoading = true))

        composeRule.onNodeWithText(string(R.string.message_no_mindfulness_period)).assertDoesNotExist()
    }

    @Test
    fun sessionsAreListedNewestFirst() {
        // The session someone just finished is the one they look for.
        val older = session(dayOffset = 2, title = OLDER_SESSION_TITLE)
        val newer = session(dayOffset = 0, title = SESSION_TITLE)
        // Handed over oldest-first, so rendering the input order fails here.
        setContent(state(sessions = listOf(older, newer), hasData = true))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(OLDER_SESSION_TITLE))

        val newestRow = composeRule.onNodeWithText(SESSION_TITLE).getUnclippedBoundsInRoot()
        val oldestRow = composeRule.onNodeWithText(OLDER_SESSION_TITLE).getUnclippedBoundsInRoot()

        assertTrue("today's session should sit above the one from two days ago", newestRow.top < oldestRow.top)
    }

    @Test
    fun calendarWeekModeNamesEveryPeriodTitleTheSameWay() {
        // The navigator, the total card and the chart summary all name the window and must agree.
        // Anchored on today because "This month" only names a month containing today.
        val today = LocalDate.now()
        val state = MindfulnessUiState(
            isLoading = false,
            selectedRange = TimeRange.MONTH,
            selectedDate = today,
            sessions = listOf(session(dayOffset = 0)),
            display = MindfulnessDisplayState(
                selectedPeriod = DatePeriod(today.withDayOfMonth(1), today),
                hasData = true,
                summary = MindfulnessPeriodSummary(
                    totalMinutes = 15L,
                    totalMs = 15L * 60_000L,
                    sessionCount = 1,
                    averageDurationMs = 15L * 60_000L,
                    longestSessionMs = 15L * 60_000L,
                ),
            ),
        )

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.MONTH,
                    selectedDate = today,
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
                ) { period ->
                    mindfulnessPeriodContent(
                        state = state,
                        period = period,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        hasNotificationPermission = true,
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                        onToggleReminders = {},
                        onRequestNotificationPermission = {},
                        onSelectReminderTime = { _: LocalTime -> },
                        onEditMindfulnessSession = {},
                        onDeleteMindfulnessSession = {},
                    )
                }
            }
        }

        // Unmerged, so this counts the three labels themselves.
        composeRule
            .onAllNodesWithText(
                string(R.string.period_this_month),
                substring = true,
                useUnmergedTree = true,
            )
            .assertCountEquals(3)
        composeRule
            .onAllNodesWithText(
                string(R.string.period_last_30_days),
                substring = true,
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
    }

    private fun state(
        sessions: List<MindfulnessSession>,
        hasData: Boolean,
        isLoading: Boolean = false,
    ) = MindfulnessUiState(
        isLoading = isLoading,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        sessions = sessions,
        display = MindfulnessDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
            hasData = hasData,
            summary = MindfulnessPeriodSummary(
                totalMinutes = 45L,
                totalMs = 45L * 60_000L,
                sessionCount = sessions.size,
                averageDurationMs = 15L * 60_000L,
                longestSessionMs = 20L * 60_000L,
            ),
        ),
    )

    private fun session(
        dayOffset: Long = 0,
        title: String = SESSION_TITLE,
    ): MindfulnessSession {
        val start = ANCHOR.minusDays(dayOffset).atStartOfDay(ZoneId.systemDefault()).plusHours(7)
        return MindfulnessSession(
            id = "session-$dayOffset",
            title = title,
            startTime = start.toInstant(),
            endTime = start.plusMinutes(15).toInstant(),
            durationMs = 15L * 60_000L,
            source = "tech.mmarca.openvitals",
            isOpenVitalsEntry = true,
        )
    }

    private fun setContent(state: MindfulnessUiState) {
        composeRule.setContent {
            OpenVitalsTheme {
                LazyColumn {
                    mindfulnessPeriodContent(
                        state = state,
                        period = state.display.selectedPeriod,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        hasNotificationPermission = true,
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                        onToggleReminders = {},
                        onRequestNotificationPermission = {},
                        onSelectReminderTime = { _: LocalTime -> },
                        onEditMindfulnessSession = {},
                        onDeleteMindfulnessSession = {},
                    )
                }
            }
        }
    }

    private companion object {
        /** A fixed past date, so the period never straddles today. */
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        const val SESSION_TITLE = "Morning sit"
        const val OLDER_SESSION_TITLE = "Evening breathing"
    }
}
