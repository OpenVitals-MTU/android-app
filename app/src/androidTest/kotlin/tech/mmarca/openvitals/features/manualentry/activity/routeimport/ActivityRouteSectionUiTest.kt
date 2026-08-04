package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.time.Duration
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryUiState
import tech.mmarca.openvitals.features.manualentry.activity.TestActivityEntryCard
import tech.mmarca.openvitals.features.manualentry.activity.pushUpsEntryType
import tech.mmarca.openvitals.features.manualentry.activity.runningEntryType
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Ports the `ActivityEntryCard with an imported route` group of Flutter's
 * `test/features/manualentry/activity/activity_route_section_test.dart`.
 *
 * An imported route is a list of places and times. Saving it against push-ups
 * would attach a map to an activity Health Connect will not carry one for, so
 * the type list narrows the moment a route is attached — and widens again when
 * there is none, or half the app's activities become unreachable. The summary
 * beside it is the only chance a user gets to notice the file they picked is
 * the wrong ride before it is written.
 */
class ActivityRouteSectionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anAttachedRouteNarrowsTheTypeSelectorToGpsCapableTypes() {
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = runningEntryType,
                    importedRoute = route(),
                ),
            )
        }

        openTypeSelector()

        composeRule.onAllNodesWithText(string(R.string.exercise_type_running)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(pushUpsEntryType.labelRes)).assertDoesNotExist()
    }

    @Test
    fun withNoRouteEveryTypeIsStillOnOffer() {
        // Guards the test above from becoming vacuous, and pins the thing that
        // would otherwise quietly hide indoor activities from manual entry.
        setContent {
            TestActivityEntryCard(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = runningEntryType,
                ),
            )
        }

        openTypeSelector()

        // The menu scrolls, so existing in the list is the assertion — being on
        // screen is a matter of where in the list the type happens to sit.
        composeRule.onNodeWithText(string(pushUpsEntryType.labelRes)).assertExists()
    }

    @Test
    fun theRouteSectionNamesTheFileAndItsAverageMetrics() {
        val formatter = testUnitFormatter()
        setContent {
            ImportedActivityRouteSection(
                state = ActivityEntryUiState(
                    canWrite = true,
                    isCheckingPermission = false,
                    selectedActivityType = runningEntryType,
                    importedRoute = route(),
                    recordedPauseIntervals = listOf(
                        ActivityPauseInterval(
                            startTime = START.plus(Duration.ofMinutes(5)),
                            endTime = START.plus(Duration.ofMinutes(15)),
                        ),
                    ),
                ),
                unitFormatter = formatter,
                onClearRoute = {},
            )
        }

        composeRule
            .onNodeWithText(
                string(
                    R.string.activity_entry_route_summary,
                    "Morning run",
                    formatter.distance(5_000.0).text,
                    formatter.elevation(42.0).text,
                    3,
                ),
            )
            .performScrollTo()
            .assertIsDisplayed()

        // 5 km in 30 minutes is 6:00 /km; ten of those minutes were paused, so
        // the pace reported over moving time only is 4:00 /km.
        val movingMillis = Duration.ofMinutes(20).toMillis()
        composeRule
            .onNodeWithText(
                string(
                    R.string.activity_entry_route_average_metrics,
                    checkNotNull(formatter.averagePace(5_000.0, movingMillis)).text,
                    formatter.averageSpeed(5_000.0, movingMillis).text,
                ),
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun withNoRouteThereIsNoRouteSectionAtAll() {
        setContent {
            ImportedActivityRouteSection(
                state = ActivityEntryUiState(canWrite = true, isCheckingPermission = false),
                unitFormatter = testUnitFormatter(),
                onClearRoute = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_imported_route)).assertDoesNotExist()
    }

    private fun openTypeSelector() {
        // The selector is a read-only field that doubles as its own menu anchor.
        composeRule.onNodeWithText(string(R.string.activity_entry_type_label))
            .performScrollTo()
            .performClick()
    }

    private fun route(): RouteFileImport = RouteFileImport(
        fileName = "morning.gpx",
        name = "Morning run",
        points = List(3) { index ->
            ExerciseRoutePoint(
                time = START.plus(Duration.ofMinutes(index * 10L)),
                latitude = 59.0 + index * 0.001,
                longitude = 24.0,
                altitudeMeters = 10.0,
                horizontalAccuracyMeters = null,
                verticalAccuracyMeters = null,
            )
        },
        distanceMeters = 5_000.0,
        elevationGainedMeters = 42.0,
        startTime = START,
        endTime = START.plus(Duration.ofMinutes(30)),
    )

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private companion object {
        val START: Instant = Instant.parse("2026-07-09T08:00:00Z")
    }
}
