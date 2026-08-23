package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `AppNavigation` decides what the app bar shows by comparing the live destination against these
 * routes, and it strips the query first (`route.substringBefore('?')`) because a destination
 * registered with optional arguments reports them as part of its own pattern.
 *
 * [Screen.ActivityEntry] is the one screen that bakes a query into its route, so comparing the
 * stripped live route against its full [Screen.route] never matched. Everything the app bar keys
 * on that screen fell through in silence: no title, no edit toggle, no outdoor-mode toggle, and
 * the top bar stayed on screen in the recording focus mode that exists to hide it.
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
        // Matching happens on the base path, so a collision would make two screens
        // indistinguishable to every app-bar decision.
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
