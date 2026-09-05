package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepDeviceData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The detail screen is the only place that shows what the tracker wrote down: which device,
 * recorded how, in which zone, split into which stages.
 */
class SleepDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDetailScreenShowsTheNightItsBreakdownItsMetadataAndItsStageEvents() {
        // Every one of these is a separate lazy item, so one dropping out is invisible to a test that only opens the screen.
        setDetail(session())

        // The summary card opens the screen. Its title repeats in the metadata card, hence the first match.
        composeRule.onAllNodesWithText("Night sleep").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(FORMATTER.duration(EIGHT_HOURS_MS)).assertIsDisplayed()

        assertShown(string(R.string.detail_stages))
        // Light is 4h of the 8h of recorded stage time.
        assertShown("${FORMATTER.duration(4 * HOUR_MS)} · ${FORMATTER.decimal(50.0, 0)}%")

        assertShown(string(R.string.detail_session_details))
        assertShown(string(R.string.recording_automatically_recorded))
        assertShown(string(R.string.device_watch))
        assertShown("Acme")
        assertShown("Watch 5")
        assertShown("Slept well")
        // Both ends of the night carry the offset.
        assertShown(string(R.string.detail_start_zone))
        composeRule.onAllNodesWithText(ZONE_OFFSET_TEXT).assertCountEquals(2)

        assertShown(string(R.string.detail_stage_events))
        assertShown(string(R.string.summary_recorded_stages, FORMATTER.count(4)))
        // The shortest stage has its own row: the list is per event, not per type.
        assertShown(FORMATTER.duration(30 * MINUTE_MS))
    }

    @Test
    fun aNightOfBackToBackStagesDrawsWithoutTearingTheChart() {
        // Contiguous stages make the hypnogram draw its cross-lane connector for every hop.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SleepStagesLaneChart(
                        stages = session().stages,
                        unitFormatter = FORMATTER,
                        timeFormatter = DateTimeFormatterProvider().shortTime(),
                        timelineStart = NIGHT_START,
                        timelineEnd = NIGHT_END,
                    )
                }
            }
        }

        // One labelled lane per stage type, each carrying that lane's total.
        composeRule
            .onNodeWithText(laneLabel(R.string.sleep_stage_light, 4 * HOUR_MS))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(laneLabel(R.string.sleep_stage_deep, 2 * HOUR_MS))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(laneLabel(R.string.sleep_stage_rem, 90 * MINUTE_MS))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(laneLabel(R.string.sleep_stage_awake, 30 * MINUTE_MS))
            .assertIsDisplayed()
    }

    @Test
    fun aNightWithoutStagesSaysSoRatherThanShowingAnEmptyChart() {
        // Many writers record only a start and an end. The screen must admit there are no stages,
        // and an untitled session still needs a name.
        setDetail(
            SleepData(
                id = "s1",
                startTime = NIGHT_END.minusSeconds(7 * 3600),
                endTime = NIGHT_END,
                durationMs = 7 * HOUR_MS,
                source = "com.test.tracker",
            )
        )

        assertShown(string(R.string.message_no_stages))
        assertShown(string(R.string.detail_sleep_session))

        // Scrolled to the last row of the last card, so the stage-event section is genuinely absent.
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.detail_notes)))
        composeRule.onNodeWithText(string(R.string.detail_stage_events)).assertDoesNotExist()
    }

    private fun setDetail(session: SleepData) {
        composeRule.setContent {
            OpenVitalsTheme {
                SleepDetailContent(
                    session = session,
                    unitFormatter = FORMATTER,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                )
            }
        }
    }

    private fun assertShown(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.onAllNodesWithText(text).onFirst().assertIsDisplayed()
    }

    private fun laneLabel(labelRes: Int, durationMs: Long): String =
        "${string(labelRes)} - ${FORMATTER.duration(durationMs)}"

    private fun session(): SleepData {
        val start = NIGHT_START
        return SleepData(
            id = "s1",
            startTime = start,
            endTime = NIGHT_END,
            durationMs = EIGHT_HOURS_MS,
            source = "com.test.tracker",
            title = "Night sleep",
            notes = "Slept well",
            startZoneOffset = ZoneOffset.ofHours(2),
            endZoneOffset = ZoneOffset.ofHours(2),
            recordingMethod = Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED,
            device = SleepDeviceData(type = Device.TYPE_WATCH, manufacturer = "Acme", model = "Watch 5"),
            stages = listOf(
                stage(start, start.plusSeconds(4 * 3600), SleepStage.STAGE_LIGHT),
                stage(start.plusSeconds(4 * 3600), start.plusSeconds(6 * 3600), SleepStage.STAGE_DEEP),
                stage(start.plusSeconds(6 * 3600), start.plusSeconds(27_000), SleepStage.STAGE_REM),
                stage(start.plusSeconds(27_000), NIGHT_END, SleepStage.STAGE_AWAKE),
            ),
        )
    }

    private fun stage(start: Instant, end: Instant, stageType: Int) =
        SleepStage(startTime = start, endTime = end, stageType = stageType)

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 3_600_000L
        const val EIGHT_HOURS_MS = 8 * HOUR_MS
        const val ZONE_OFFSET_TEXT = "+02:00"

        /** A fixed past night, so the fixture never drifts with the calendar. */
        val NIGHT_END: Instant = Instant.parse("2026-07-08T06:00:00Z")
        val NIGHT_START: Instant = NIGHT_END.minusSeconds(8 * 3600)

        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
