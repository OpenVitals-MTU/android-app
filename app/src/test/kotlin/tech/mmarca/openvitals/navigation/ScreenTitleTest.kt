package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.R

/** Settings > Body profile once had no app bar title: its route was missing from a `when`. */
class ScreenTitleTest {

    @Test
    fun `the screen list holds every screen once`() {
        val declared = Screen::class.sealedSubclasses.mapNotNull { it.objectInstance }

        assertEquals(declared.toSet(), Screen.all.toSet())
        assertEquals("a screen is listed twice", Screen.all.size, Screen.all.toSet().size)
    }

    @Test
    fun `every screen has a title unless it titles itself`() {
        val untitled = Screen.all.filter { it.titleRes == null }.toSet()

        assertEquals(ScreensWithoutFixedTitle, untitled)
    }

    @Test
    fun `a live destination finds its title by base path`() {
        Screen.all.forEach { screen ->
            assertEquals(screen.route, screen.titleRes, Screen.titleResFor(screen.basePath))
        }
        assertNull(Screen.titleResFor("no/such/route"))
        assertNull(Screen.titleResFor(null))
    }

    @Test
    fun `body profile has a title`() {
        assertEquals(
            R.string.settings_body_profile_group_title,
            Screen.titleResFor(Screen.SettingsBodyProfile.route),
        )
    }

    @Test
    fun `the plain string detail routes have a title`() {
        val detailRoutes = listOf(
            CardioLoadDetailRoute,
            SleepEfficiencyDetailRoute,
            SleepScoreDetailRoute,
            HeartRecoveryDetailRoute,
        )

        assertTrue(detailRoutes.all { detailRouteTitleRes(it) != null })
        assertNull(detailRouteTitleRes(Screen.Dashboard.route))
    }

    private companion object {
        /** Onboarding shows no app bar. The other two take their title from their argument. */
        val ScreensWithoutFixedTitle: Set<Screen> =
            setOf(Screen.Onboarding, Screen.Metric, Screen.WatchDevice)
    }
}
