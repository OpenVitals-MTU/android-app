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
 * The outdoor half of Flutter's `the outdoor toggle applies the high-contrast
 * theme` (`test/features/manualentry/activity/recording/activity_recording_screen_test.dart`).
 *
 * That case does two things: it flips the toggle, and it then reads the
 * `ColorScheme` back out of the widget tree to prove the high-contrast scheme
 * is what the recording surface is actually painted with. The flip is pinned
 * through the screen in `ActivityRecordingScreenTest`; the scheme itself is
 * pinned here, where it is installed.
 *
 * It is worth pinning separately because outdoor mode is not decoration. It is
 * for reading a number in direct sunlight at arm's length, and the contrast
 * ratio is the whole feature — a theme that silently stopped being applied
 * would look perfectly fine indoors, on every device a developer holds, and be
 * unreadable exactly where it is needed.
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
        // Someone who has asked for a dark app has usually asked for it because
        // of when they use it. Answering "outdoor" with a white screen at night
        // would be the wrong high contrast.
        val (scheme, accent) = schemeUnder(outdoorMode = true, appThemeMode = AppThemeMode.DARK)

        assertEquals(RecordingOutdoorDarkColorScheme.primary, scheme.primary)
        assertEquals(Color.White, scheme.onBackground)
        assertEquals(RecordingOutdoorAccent, accent)
    }

    @Test
    fun withoutOutdoorModeTheAppsOwnThemeIsLeftAlone() {
        // The recording screen is the only place this scheme exists; leaking it
        // into an ordinary session would repaint the app for everyone.
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
