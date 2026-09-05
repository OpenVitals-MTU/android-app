package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `AppNavigation` compares the live destination against these routes after stripping the query.
 * [Screen.ActivityEntry] bakes a query into its route, so it never matched and the app bar
 * fell through: no title, no toggles, top bar visible in focus mode.
 */
class ScreenRouteMatchingTest {

    @Test
    fun `the activity entry route is matched on its path, not its full pattern`() {
        assertEquals("manual_entry/activity", Screen.ActivityEntry.basePath)
        assertNotEquals(
            "the query is what made the full route unmatchable; if this ever becomes equal the " +
                "distinction is gone and basePath can go with it",
            Screen.ActivityEntry.route,
            Screen.ActivityEntry.basePath,
        )
    }

    @Test
    fun `a path route is its own base path`() {
        assertEquals(Screen.Dashboard.route, Screen.Dashboard.basePath)
        assertEquals(Screen.ActivityEntryEdit.route, Screen.ActivityEntryEdit.basePath)
    }

    @Test
    fun `no two screens share a base path`() {
        // Matching happens on the base path, so a collision makes two screens indistinguishable.
        val byBasePath = AllScreens.groupBy { it.basePath }.filterValues { it.size > 1 }

        assertTrue(
            "screens sharing a base path: ${byBasePath.mapValues { entry -> entry.value.map { it.route } }}",
            byBasePath.isEmpty(),
        )
    }

    private companion object {
        val AllScreens: List<Screen> = Screen::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }
}
