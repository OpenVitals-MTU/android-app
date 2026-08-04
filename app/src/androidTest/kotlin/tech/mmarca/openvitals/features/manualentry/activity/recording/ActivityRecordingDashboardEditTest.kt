package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's
 * `test/features/manualentry/activity/recording/activity_recording_dashboard_edit_test.dart`.
 *
 * The dashboard is what the rider looks at while moving, and its layout is
 * theirs to arrange. Two things have to hold. While the dashboard is simply
 * being read, a tile must not be removable or movable — losing the distance
 * readout mid-ride because a pocket brushed the screen is not something you
 * recover from without stopping. And the add tray has to offer exactly the
 * fields that are off the grid, because a field that is available but
 * unreachable is a metric the user simply cannot get back.
 */
@OptIn(ExperimentalTestApi::class)
class ActivityRecordingDashboardEditTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun outsideEditModeTilesCarryNoEditControls() {
        setGrid(isEditing = false)

        // In edit mode each tile announces itself by name and carries the
        // remove/move/resize actions; at rest it is a readout and nothing more.
        DEFAULT_FIELDS.forEach { field ->
            composeRule.onNodeWithContentDescription(label(field)).assertDoesNotExist()
        }
    }

    @Test
    fun editModeGivesEveryTileItsOwnEditControls() {
        setGrid(isEditing = true)

        DEFAULT_FIELDS.forEach { field ->
            composeRule.onNodeWithContentDescription(label(field)).assertIsDisplayed()
        }
    }

    @Test
    fun removingATileTakesThatFieldOutOfTheLayout() {
        var updated: ActivityRecordingDashboardLayout? = null
        setGrid(isEditing = true, onUpdateLayout = { updated = it })

        composeRule.onNodeWithContentDescription(label(ActivityRecordingDashboardField.SPEED))
            .performCustomAccessibilityActionWithLabel(string(R.string.cd_remove_widget))

        assertEquals(
            DEFAULT_FIELDS - ActivityRecordingDashboardField.SPEED,
            checkNotNull(updated).fields,
        )
    }

    @Test
    fun movingATileLandsItOnTheTargetRatherThanTheGapBeforeIt() {
        // Drop-on-target, not insert-between: the tile the user aimed at is the
        // slot they get, and everything in between shuffles up by one.
        var updated: ActivityRecordingDashboardLayout? = null
        setGrid(isEditing = true, onUpdateLayout = { updated = it })

        composeRule.onNodeWithContentDescription(label(ActivityRecordingDashboardField.HEART_RATE))
            .performCustomAccessibilityActionWithLabel(string(R.string.cd_move_widget_down))

        assertEquals(
            listOf(
                ActivityRecordingDashboardField.CADENCE,
                ActivityRecordingDashboardField.HEART_RATE,
                ActivityRecordingDashboardField.SPEED,
                ActivityRecordingDashboardField.DISTANCE,
                ActivityRecordingDashboardField.DURATION,
            ),
            checkNotNull(updated).fields,
        )
    }

    @Test
    fun theAddTrayOffersTheFieldsThatAreNotOnTheGrid() {
        var updated: ActivityRecordingDashboardLayout? = null
        setContent {
            RecordingDashboardEditor(
                layout = layout(),
                availableFields = DEFAULT_FIELDS + ActivityRecordingDashboardField.MOVING_TIME,
                onUpdateLayout = { updated = it },
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_recording_dashboard_add_field)).assertIsDisplayed()
        // Already placed, so offering it again would let one field appear twice.
        composeRule.onNodeWithText(label(ActivityRecordingDashboardField.HEART_RATE)).assertDoesNotExist()
        composeRule.onNodeWithText(label(ActivityRecordingDashboardField.MOVING_TIME)).performClick()

        assertTrue(ActivityRecordingDashboardField.MOVING_TIME in checkNotNull(updated).fields)
    }

    @Test
    fun aGridThatAlreadyHasEverythingRendersNoAddTray() {
        // An empty "Add widget" heading is a promise of something to add.
        setContent {
            RecordingDashboardEditor(
                layout = layout(),
                availableFields = DEFAULT_FIELDS,
                onUpdateLayout = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.activity_entry_recording_dashboard_add_field)).assertDoesNotExist()
    }

    private fun label(field: ActivityRecordingDashboardField): String = string(field.labelRes)

    /** The default five fields, each 1x1 so every tile is its own drop target. */
    private fun layout() = ActivityRecordingDashboardLayout(
        fields = DEFAULT_FIELDS,
        sizes = DEFAULT_FIELDS.associateWith { ActivityRecordingDashboardItemSize.SMALL },
    )

    private fun stats(): Map<ActivityRecordingDashboardField, RecordingDashboardStat> =
        DEFAULT_FIELDS.associateWith { field ->
            RecordingDashboardStat(
                value = DisplayValue(field.ordinal.toString(), ""),
                label = label(field),
            )
        }

    private fun setGrid(
        isEditing: Boolean,
        onUpdateLayout: (ActivityRecordingDashboardLayout) -> Unit = {},
    ) {
        setContent {
            RecordingDashboardGrid(
                layout = layout(),
                stats = stats(),
                isEditingDashboard = isEditing,
                onUpdateLayout = onUpdateLayout,
            )
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier) { content() }
            }
        }
    }

    private companion object {
        val DEFAULT_FIELDS = listOf(
            ActivityRecordingDashboardField.HEART_RATE,
            ActivityRecordingDashboardField.CADENCE,
            ActivityRecordingDashboardField.SPEED,
            ActivityRecordingDashboardField.DISTANCE,
            ActivityRecordingDashboardField.DURATION,
        )
    }
}
