package tech.mmarca.openvitals.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.AppThemeMode

/**
 * The high-contrast scheme the recording surface is painted with in outdoor mode.
 * The flip is pinned in `ActivityRecordingScreenTest`; the scheme is pinned here.
 * A theme that silently stopped applying would look fine indoors.
 */
class ActivityRecordingThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun outdoorModeOnALightThemeIsBlackOnWhiteWithTheBurntOrangeAccent() {
        val (scheme, accent) = schemeUnder(outdoorMode = true, appThemeMode = AppThemeMode.LIGHT)

        assertEquals(RecordingOutdoorLightColorScheme.primary, scheme.primary)
        assertEquals(Color.White, scheme.background)
        assertEquals(Color.Black, scheme.onBackground)
        assertEquals(RecordingOutdoorLightAccent, accent)
    }

    @Test
    fun outdoorModeOnADarkThemeStaysDarkRatherThanFlashingWhite() {
        // Answering "outdoor" with a white screen at night would be the wrong high contrast.
        val (scheme, accent) = schemeUnder(outdoorMode = true, appThemeMode = AppThemeMode.DARK)

        assertEquals(RecordingOutdoorDarkColorScheme.primary, scheme.primary)
        assertEquals(Color.White, scheme.onBackground)
        assertEquals(RecordingOutdoorAccent, accent)
    }

    @Test
    fun withoutOutdoorModeTheAppsOwnThemeIsLeftAlone() {
        // The scheme must not leak into an ordinary session.
        val (scheme, accent) = schemeUnder(outdoorMode = false, appThemeMode = AppThemeMode.LIGHT)

        assertEquals(WorkoutColor, accent)
        assertNotEquals(RecordingOutdoorLightColorScheme.primary, scheme.primary)
        assertNotEquals(RecordingOutdoorDarkColorScheme.primary, scheme.primary)
    }

    /** The scheme and the accent the recording surface is composed with. */
    private fun schemeUnder(
        outdoorMode: Boolean,
        appThemeMode: AppThemeMode,
    ): Pair<ColorScheme, Color> {
        var scheme: ColorScheme? = null
        var accent: Color = Color.Unspecified
        composeRule.setContent {
            OpenVitalsTheme(themeMode = appThemeMode) {
                ActivityRecordingTheme(
                    outdoorModeEnabled = outdoorMode,
                    appThemeMode = appThemeMode,
                ) {
                    scheme = MaterialTheme.colorScheme
                    accent = activityRecordingAccentColor()
                }
            }
        }
        composeRule.waitForIdle()
        return requireNotNull(scheme) to accent
    }
}
