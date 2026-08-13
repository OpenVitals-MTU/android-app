package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * How far the list has been expanded is the reader's, and it has to survive the
 * list changing under them.
 *
 * The reported flow: open the yearly weight view, scroll to the entries, tap
 * "Load 10 more" until the list is long, then swipe an entry away. The row went,
 * and so did the expansion — the list snapped back to its first page, which
 * pulled everything below it up the screen and left the reader looking at a
 * section they had never scrolled to.
 */
class PaginatedEntryListTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deletingAnEntry_keepsTheListExpanded() {
        val entries = mutableStateListOf<String>().apply {
            addAll(List(25) { "Reading ${it + 1}" })
        }

        composeRule.setContent {
            OpenVitalsTheme {
                PaginatedEntryList(title = TITLE, entries = entries.toList()) { entry, _ ->
                    Text(entry)
                }
            }
        }

        // One page to start with.
        composeRule.onNodeWithText("Reading 10").assertExists()
        composeRule.onNodeWithText("Reading 11").assertDoesNotExist()

        composeRule.onNodeWithText(LOAD_MORE).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reading 20").assertExists()

        // Swipe one away, the way the entry rows do.
        entries.removeAt(0)
        composeRule.waitForIdle()

        // Still twenty deep, not back to ten. "Reading 21" is the twentieth row
        // now that the first is gone — if the expansion had been lost it would
        // be nowhere on screen.
        composeRule.onNodeWithText("Reading 21").assertExists()
        composeRule.onNodeWithText("Reading 11").assertExists()
    }

    @Test
    fun deletingBelowThePageSize_showsWhatIsLeftRatherThanBlaming() {
        val entries = mutableStateListOf<String>().apply {
            addAll(List(12) { "Reading ${it + 1}" })
        }

        composeRule.setContent {
            OpenVitalsTheme {
                PaginatedEntryList(title = TITLE, entries = entries.toList()) { entry, _ ->
                    Text(entry)
                }
            }
        }

        composeRule.onNodeWithText(LOAD_MORE).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reading 12").assertExists()

        // Down to eleven: the count is clamped to what exists, and the button
        // that has nothing left to load goes away.
        entries.removeAt(11)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Reading 11").assertExists()
        composeRule.onNodeWithText(LOAD_MORE).assertDoesNotExist()
    }

    @Test
    fun anEntryArrivingDoesNotExpandTheListOnItsOwn() {
        // The clamp only ever shortens. A list left at one page must not grow
        // just because the period gained an entry.
        var entries by mutableStateOf(List(10) { "Reading ${it + 1}" })

        composeRule.setContent {
            OpenVitalsTheme {
                PaginatedEntryList(title = TITLE, entries = entries) { entry, _ ->
                    Text(entry)
                }
            }
        }

        // Ten entries exactly: nothing more to load.
        composeRule.onNodeWithText(LOAD_MORE).assertDoesNotExist()

        entries = entries + "Reading 11"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Reading 11").assertDoesNotExist()
        composeRule.onNodeWithText(LOAD_MORE).assertExists()
    }

    private companion object {
        const val TITLE = "Entries"
        const val LOAD_MORE = "Load 10 more"
    }
}
