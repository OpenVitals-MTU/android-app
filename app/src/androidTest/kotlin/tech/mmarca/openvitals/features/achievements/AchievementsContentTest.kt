package tech.mmarca.openvitals.features.achievements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** An unearned badge says how far along it is, and an earned one changes the whole card. */
class AchievementsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theSummaryCardCountsUnlockedAgainstTotal() {
        setContent {
            AchievementSummaryCard(
                state = AchievementsUiState(
                    isLoading = false,
                    badges = listOf(badge(unlocked = true), badge(id = "b2", unlocked = false)),
                ),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                onRefresh = {},
            )
        }

        composeRule
            .onNodeWithText(string(R.string.achievements_progress_summary, 1, 2))
            .assertIsDisplayed()
    }

    @Test
    fun anEarnedBadgeNamesTheDayItWasEarned() {
        setContent {
            AchievementBadgeCard(
                progress = badge(unlocked = true, achievedOn = LocalDate.of(2026, 6, 23)),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText(BADGE_NAME).assertIsDisplayed()
    }

    @Test
    fun aLockedBadgeIsStillDrawnAndStillNamed() {
        // Hiding what has not been earned leaves nothing to aim at.
        setContent {
            AchievementBadgeCard(
                progress = badge(unlocked = false, ratio = 0.4f, current = 4_000.0),
                unitFormatter = FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText(BADGE_NAME).assertIsDisplayed()
    }

    private fun badge(
        id: String = "b1",
        unlocked: Boolean,
        ratio: Float = if (unlocked) 1f else 0.1f,
        current: Double = if (unlocked) 10_000.0 else 1_000.0,
        achievedOn: LocalDate? = null,
    ) = AchievementProgress(
        definition = AchievementDefinition(
            id = id,
            name = if (id == "b1") BADGE_NAME else "Another badge",
            category = AchievementCategory.entries.first(),
            metric = AchievementMetric.entries.first(),
            target = 10_000.0,
        ),
        currentValue = current,
        progressRatio = ratio,
        isUnlocked = unlocked,
        timesEarned = if (unlocked) 1 else 0,
        achievedOn = achievedOn,
    )

    private fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private companion object {
        const val BADGE_NAME = "Sneakers"
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
