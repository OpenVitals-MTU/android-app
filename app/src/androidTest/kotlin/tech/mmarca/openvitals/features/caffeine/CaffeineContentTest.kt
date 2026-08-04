package tech.mmarca.openvitals.features.caffeine

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.Instant
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering case of Flutter's
 * `test/features/caffeine/caffeine_screen_test.dart`; the access-gate case is
 * pinned once for every screen in `HealthConnectAccessGateTest`.
 *
 * The caffeine screen is a long lazy list, and the two things a user opens it
 * for — how much is in them right now, and the curve that says when it will be
 * gone — are the first cards on it.
 */
class CaffeineContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aLoadedDayLeadsWithTheActiveDoseAndTheDecayCurve() {
        setContent(
            CaffeineUiState(
                isLoading = false,
                homeDisplay = INSIGHTS,
                analyticsDisplay = INSIGHTS,
            ),
        )

        composeRule.onNodeWithText(string(R.string.caffeine_section_dashboard)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.caffeine_current_title)).assertIsDisplayed()
        // The curve card used to draw with no title of its own while the
        // translated string sat unused — the chart is unreadable without one,
        // because nothing else on it says what the line is.
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.caffeine_curve_title)))
        composeRule.onNodeWithText(string(R.string.caffeine_curve_title)).assertIsDisplayed()
    }

    private fun setContent(state: CaffeineUiState) {
        composeRule.setContent {
            OpenVitalsTheme {
                LazyColumn {
                    caffeineHomeAndAnalyticsContent(
                        state = state,
                        screenError = state.error,
                        unitFormatter = FORMATTER,
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onCompleteSetup = {},
                        onSkipSetup = {},
                        onSelectAnalyticsRange = {},
                        onSelectEntry = {},
                        onOpenDrink = {},
                        onDeleteEntry = {},
                    )
                }
            }
        }
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        /** A fixed afternoon, so the curve never depends on when the suite runs. */
        val ANCHOR: Instant = Instant.parse("2026-06-23T14:00:00Z")

        val INSIGHTS = CaffeineInsights(
            currentMg = 142.0,
            todayTotalMg = 215.0,
            sleepThresholdMg = 50,
            bedtime = LocalTime.of(23, 0),
            timeToThresholdMinutes = 240L,
            curvePoints = List(12) { step ->
                CaffeinePoint(
                    time = ANCHOR.plusSeconds(step * 3_600L),
                    valueMg = 215.0 - step * 12.0,
                )
            },
        )
    }
}
