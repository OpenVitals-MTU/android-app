package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of Flutter's `test/ui/components/swipe_to_delete_entry_row_test.dart`.
 *
 * Deletion is one-directional on purpose: a row that deletes whichever way it
 * is pushed loses entries to the horizontal component of a vertical scroll.
 */
class SwipeToDeleteEntryRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun endToStartSwipe_deletes() {
        var deleted = 0

        composeRule.setContent {
            OpenVitalsTheme {
                SwipeToDeleteEntryRow(onDelete = { deleted++ }) {
                    ListItem(headlineContent = { Text(ENTRY) })
                }
            }
        }

        composeRule.onNodeWithText(ENTRY).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(1, deleted)
    }

    @Test
    fun startToEndSwipe_doesNothing() {
        var deleted = 0

        composeRule.setContent {
            OpenVitalsTheme {
                SwipeToDeleteEntryRow(onDelete = { deleted++ }) {
                    ListItem(headlineContent = { Text(ENTRY) })
                }
            }
        }

        composeRule.onNodeWithText(ENTRY).performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(0, deleted)
        composeRule.onNodeWithText(ENTRY).assertExists()
    }

    private companion object {
        const val ENTRY = "Morning reading"
    }
}
