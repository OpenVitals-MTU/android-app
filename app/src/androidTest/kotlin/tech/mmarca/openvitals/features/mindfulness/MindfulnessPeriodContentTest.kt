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
 * Port of the rendering cases of Flutter's
 * `test/features/mindfulness/mindfulness_screen_test.dart`.
 *
 * The totals and the empty-state decision are already covered on the JVM by
 * `MindfulnessPresentationMapperTest`. What is only answerable here is whether
 * the content actually draws what the display state decided — the gap between
 * "the mapper says there is no data" and "the user is told there is no data" is
 * a blank screen with no explanation.
 *
 * The access-gate case from that file is not repeated here: Kotlin routes every
 * screen's gate through `HealthConnectScreenShell`, and it is pinned once in
 * `HealthConnectAccessGateTest`.
 *
 * The session-order case from `mindfulness_display_test.dart` lives here as
 * well: Kotlin sorts inside the content, so there is no display field to read.
 */
class MindfulnessPeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheTotalCardAndSessionListOnceLoaded() {
        setContent(state(sessions = listOf(session()), hasData = true))

        composeRule.onNodeWithText(string(R.string.metric_mindfulness)).assertIsDisplayed()
        // The session itself, not just the section heading — a heading over an
        // empty list is the failure this is meant to catch.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(SESSION_TITLE))
        composeRule.onNodeWithText(SESSION_TITLE).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyPlaceholderWithNoSessions() {
        // Not the loading state and not a zeroed chart: an explicit "nothing
        // recorded", because a period with no sessions and a period that failed
        // to load look identical otherwise.
        setContent(state(sessions = emptyList(), hasData = false, isLoading = false))

        composeRule.onNodeWithText(string(R.string.message_no_mindfulness_period)).assertIsDisplayed()
    }

    @Test
    fun aLoadingPeriodDoesNotClaimThereIsNothingToShow() {
        // The same empty display, mid-load. Announcing "no sessions" before the
        // read returns tells the user something false about their own history.
        setContent(state(sessions = emptyList(), hasData = false, isLoading = true))

        composeRule.onNodeWithText(string(R.string.message_no_mindfulness_period)).assertDoesNotExist()
    }

    @Test
    fun sessionsAreListedNewestFirst() {
        // The list is a history, and the session someone just finished is the one
        // they look for. Sorted the other way it lands at the bottom, under every
        // sit from the start of the week.
        val older = session(dayOffset = 2, title = OLDER_SESSION_TITLE)
        val newer = session(dayOffset = 0, title = SESSION_TITLE)
        // Handed over oldest-first, so a content that renders its input order
        // fails here rather than passing by luck.
        setContent(state(sessions = listOf(older, newer), hasData = true))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(OLDER_SESSION_TITLE))

        val newestRow = composeRule.onNodeWithText(SESSION_TITLE).getUnclippedBoundsInRoot()
        val oldestRow = composeRule.onNodeWithText(OLDER_SESSION_TITLE).getUnclippedBoundsInRoot()

        assertTrue("today's session should sit above the one from two days ago", newestRow.top < oldestRow.top)
    }

    @Test
    fun calendarWeekModeNamesEveryPeriodTitleTheSameWay() {
        // Three surfaces name the window the user is looking at: the navigator at
        // the top, the total card's subtitle and the chart's summary line. They
        // have to agree — a screen that calls the same month two different things
        // reads as two different periods stacked on one page.
        //
        // Anchored on today rather than a fixed past date on purpose: "This month"
        // is only the name of a month that contains today, which is the naming
        // branch this pins.
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

        // Unmerged, so this counts the three labels themselves rather than
        // however many groups the semantics merger happened to fold them into.
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
