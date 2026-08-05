package tech.mmarca.openvitals.features.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The configure step's contract: selection drives the build button, the
 * custom-range controls only exist while Custom is chosen, and every control
 * reports through its callback rather than mutating anything itself.
 */
class ReportBuilderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setConfigure(
        state: ReportBuilderState,
        onToggleMetric: (ReportMetric) -> Unit = {},
        onSetLookback: (Int?) -> Unit = {},
        onBuild: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                ReportConfigureStep(
                    state = state,
                    metricTitle = { it.name },
                    onToggleMetric = onToggleMetric,
                    onSelectAll = {},
                    onClear = {},
                    onSetGranularity = {},
                    onSetLookback = onSetLookback,
                    onSetCustomStart = {},
                    onSetCustomEnd = {},
                    onBuild = onBuild,
                )
            }
        }
    }

    private fun state(
        selected: Set<ReportMetric> = emptySet(),
        lookbackDays: Int? = 90,
    ) = ReportBuilderState(
        supportedMetrics = listOf(ReportMetric.STEPS, ReportMetric.SLEEP),
        selectedMetrics = selected,
        lookbackDays = lookbackDays,
    )

    @Test
    fun buildIsDeadUntilAMetricIsSelected() {
        setConfigure(state())

        composeRule.onNodeWithText(string(R.string.report_build_action))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun tappingAMetricRowReportsTheToggle() {
        var toggled: ReportMetric? = null
        setConfigure(state(), onToggleMetric = { toggled = it })

        composeRule.onNodeWithText(ReportMetric.SLEEP.name).performClick()

        assertEquals(ReportMetric.SLEEP, toggled)
    }

    @Test
    fun buildFiresOnceAMetricIsSelected() {
        var built = false
        setConfigure(state(selected = setOf(ReportMetric.STEPS)), onBuild = { built = true })

        composeRule.onNodeWithText(string(R.string.report_build_action))
            .performScrollTo()
            .performClick()

        assertTrue(built)
    }

    @Test
    fun customLookbackRevealsTheDateButtons() {
        setConfigure(state(lookbackDays = null))

        composeRule.onNodeWithText(string(R.string.report_custom_start_date))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.report_custom_end_date)).assertIsDisplayed()
    }

    @Test
    fun presetLookbackHidesTheDateButtons() {
        var picked: Int? = -1
        setConfigure(state(lookbackDays = 90), onSetLookback = { picked = it })

        composeRule.onNodeWithText(string(R.string.report_lookback_custom))
            .performScrollTo()
            .performClick()

        assertEquals(null, picked)
    }

    @Test
    fun buildingStepShowsProgressAndCancel() {
        composeRule.setContent {
            OpenVitalsTheme {
                ReportBuildingStep(
                    state = ReportBuilderState(
                        step = ReportBuilderStep.BUILDING,
                        progressMetricTitle = "Steps",
                    ),
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.report_building_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.report_building_metric, "Steps")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_cancel)).assertIsDisplayed()
    }

    @Test
    fun invalidCustomRangeShowsTheErrorAndKillsBuild() {
        setConfigure(
            state(selected = setOf(ReportMetric.STEPS), lookbackDays = null).copy(
                customStart = java.time.LocalDate.of(2026, 7, 15),
                customEnd = java.time.LocalDate.of(2026, 6, 1),
            ),
        )

        composeRule.onNodeWithText(string(R.string.report_custom_range_invalid))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.report_build_action))
            .performScrollTo()
            .assertIsNotEnabled()
    }
}
