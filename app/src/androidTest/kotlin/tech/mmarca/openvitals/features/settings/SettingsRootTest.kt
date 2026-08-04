package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's `test/features/settings/settings_screen_test.dart`.
 *
 * Settings is a routing surface: every section card is the only way into the
 * screen behind it, so a card that renders without its click, or a section that
 * silently stops being listed, strands a whole feature with nothing to report.
 * The version footer matters for the same reason — it is what a bug report
 * quotes.
 */
class SettingsRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everySectionRendersACardThatOpensIt() {
        var opened: SettingsSection? = null
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsSection.entries.forEach { section ->
                        SettingsCategoryCard(section = section, onClick = { opened = section })
                    }
                }
            }
        }

        // Every section is reachable, not merely the first few that fit.
        SettingsSection.entries.forEach { section ->
            composeRule.onNodeWithText(string(section.titleRes)).performScrollTo().assertIsDisplayed()
        }

        val last = SettingsSection.entries.last()
        composeRule.onNodeWithText(string(last.titleRes)).performScrollTo().performClick()

        assertEquals(last, opened)
    }

    @Test
    fun theSupportCardOffersItsThreeRoutesOut() {
        // Each button is the only way to its destination, so what matters is
        // that each one reaches its OWN handler. An earlier version of this
        // test declared these counters, never read them, and passed with all
        // three buttons deleted.
        var issues = 0
        var discussion = 0
        var support = 0
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SupportOpenVitalsCard(
                        onOpenIssues = { issues++ },
                        onOpenDiscussion = { discussion++ },
                        onOpenSupport = { support++ },
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.settings_support_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.settings_support_issues_action))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_support_discussion_action))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_support_action))
            .performScrollTo().performClick()

        // Not just "three clicks happened": each handler exactly once, so a
        // pair of buttons wired to the same lambda fails here.
        assertEquals(1, issues)
        assertEquals(1, discussion)
        assertEquals(1, support)
    }

    @Test
    fun theVersionFooterNamesTheBuildABugReportWouldQuote() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { SettingsVersionText() }
            }
        }

        composeRule
            .onNodeWithText(
                string(R.string.settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            )
            .assertIsDisplayed()
    }
}
