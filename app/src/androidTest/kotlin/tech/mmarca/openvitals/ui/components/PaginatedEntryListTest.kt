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
 * How far the list has been expanded is the reader's, and it has to survive the list changing.
 * Swiping an entry away used to snap the list back to its first page.
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

        // Still twenty deep, not back to ten.
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

        // The count is clamped to what exists, and the exhausted button goes away.
        entries.removeAt(11)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Reading 11").assertExists()
        composeRule.onNodeWithText(LOAD_MORE).assertDoesNotExist()
    }

    @Test
    fun anEntryArrivingDoesNotExpandTheListOnItsOwn() {
        // The clamp only ever shortens.
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
