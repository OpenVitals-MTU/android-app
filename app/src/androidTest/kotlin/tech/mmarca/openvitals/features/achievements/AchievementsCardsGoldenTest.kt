package tech.mmarca.openvitals.features.achievements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden

/**
 * Port of Flutter's `test/goldens/charts/achievements_cards_golden_test.dart`.
 *
 * [AchievementSummaryCard] and [AchievementBadgeCard] — two of the app's
 * proportional bars, and the two that do the least to make themselves legible.
 * Everything the bar means is in the text around it, so a consolidation could swap
 * it for one that fills the other way, or that rounds a 3% badge down to an empty
 * track, and every assertion in the suite would still hold. These are the pictures
 * that would not.
 *
 * The badge card also changes its whole CONTAINER on unlock — a tinted fill, a lit
 * icon, a tick instead of a padlock — so locked and unlocked are two different cards,
 * not one card with a longer bar.
 */
class AchievementsCardsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryCard_partWayThroughTheCatalogue() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Summary(unlocked = 14, total = 32)
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_summary")
    }

    @Test
    fun summaryCard_beforeTheFirstLoadLands() {
        // Nothing unlocked: every counter falls back to zero and the bar to an empty
        // track. This is what the screen shows for the first frame of every visit, and
        // it is the state in which a bar that fills the wrong way is invisible.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Summary(unlocked = 0, total = 32)
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_summary_empty")
    }

    @Test
    fun badge_midProgress_lockedAndHonestAboutIt() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Badge(
                    AchievementProgress(
                        definition = STEP_BADGE,
                        currentValue = 14_200.0,
                        progressRatio = 0.71f,
                        isUnlocked = false,
                        timesEarned = 0,
                    ),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_badge_locked")
    }

    @Test
    fun badge_barelyStarted() {
        // 3% of the way to a hundred floors. The bar has almost nothing to draw, and
        // "almost nothing" and "nothing" have to stay distinguishable — otherwise the
        // card claims you have not started climbing.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Badge(
                    AchievementProgress(
                        definition = FLOOR_BADGE,
                        currentValue = 3.0,
                        progressRatio = 0.03f,
                        isUnlocked = false,
                        timesEarned = 0,
                    ),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_badge_barely_started")
    }

    @Test
    fun badge_earned_theWholeCardChangesNotJustTheBar() {
        // The container takes the category's tint, the icon lights up, the padlock
        // becomes a tick, and the bar is full. `progressRatio` is over 1 here on
        // purpose — you can walk past a target, and the card clamps rather than
        // overrunning its own track.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Badge(
                    AchievementProgress(
                        definition = DISTANCE_BADGE,
                        currentValue = 9_640_000.0,
                        progressRatio = 1.24f,
                        isUnlocked = true,
                        timesEarned = 1,
                        achievedOn = LocalDate.of(2026, 4, 8),
                    ),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_badge_unlocked")
    }

    @Test
    fun badge_earnedMoreThanOnce() {
        // A repeatable daily badge: the status line counts the times rather than
        // naming a date, which is the only branch of `statusText` a single unlocked
        // shot would miss.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Badge(
                    AchievementProgress(
                        definition = STEP_BADGE,
                        currentValue = 28_450.0,
                        progressRatio = 1.42f,
                        isUnlocked = true,
                        timesEarned = 9,
                    ),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("achievements_badge_repeat")
    }

    @Composable
    private fun Summary(unlocked: Int, total: Int) {
        AchievementSummaryCard(
            state = AchievementsUiState(
                isLoading = false,
                // The Kotlin card derives its counters from the badge list rather than
                // being handed them, so the list IS the fixture.
                badges = List(total) { index ->
                    AchievementProgress(
                        definition = STEP_BADGE.copy(id = "badge_$index"),
                        currentValue = 0.0,
                        progressRatio = 0f,
                        isUnlocked = index < unlocked,
                        timesEarned = if (index < unlocked) 1 else 0,
                    )
                },
                stats = STATS,
            ),
            unitFormatter = FORMATTER,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            onRefresh = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    @Composable
    private fun Badge(progress: AchievementProgress) {
        AchievementBadgeCard(
            progress = progress,
            unitFormatter = FORMATTER,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        // The legacy window the screen actually scans, on the golden clock. Never
        // `LocalDate.now()`: [AchievementStats] defaults `endDate` to it, and a card
        // that prints today's date draws a different picture every day the suite runs.
        val STATS = AchievementStats(
            startDate = LocalDate.of(2009, 1, 1),
            endDate = LocalDate.of(2026, 6, 22),
            trackedDays = 2_841,
            maxDailySteps = 28_450L,
            totalDistanceMeters = 9_640_000.0,
            maxDailyFloors = 62,
            totalFloors = 18_400,
            hasFloorData = true,
        )

        val STEP_BADGE = AchievementDefinition(
            id = "daily_steps_20k",
            name = "Twenty thousand",
            category = AchievementCategory.DAILY_STEPS,
            metric = AchievementMetric.DAILY_STEPS,
            target = 20_000.0,
        )
        val DISTANCE_BADGE = AchievementDefinition(
            id = "lifetime_distance_10000k",
            name = "Ten thousand kilometres",
            category = AchievementCategory.LIFETIME_DISTANCE,
            metric = AchievementMetric.LIFETIME_DISTANCE_METERS,
            target = 10_000_000.0,
        )
        val FLOOR_BADGE = AchievementDefinition(
            id = "daily_floors_100",
            name = "Century of stairs",
            category = AchievementCategory.DAILY_FLOORS,
            metric = AchievementMetric.DAILY_FLOORS,
            target = 100.0,
        )
    }
}
