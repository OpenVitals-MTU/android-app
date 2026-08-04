package tech.mmarca.openvitals

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.testing.string

/**
 * Port of `renders onboarding start screen when onboarding incomplete` from
 * Flutter's `test/widget_test.dart`.
 *
 * The start gate is computed inside [MainActivity]'s `setContent` from the
 * injected `PreferencesRepository`, so there is no seam to call: the only way
 * to assert it is to launch the real activity against a preferences file that
 * says onboarding is unfinished. The flag is saved and put back afterwards so a
 * shared device is left as it was found.
 *
 * The onboarding tagline is the fingerprint because it is in the onboarding
 * header, which BOTH branches of the screen render — the app must not depend on
 * whether the device running the suite happens to have Health Connect.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityStartDestinationTest {

    private val preferences: SharedPreferences
        get() = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)

    /**
     * Runs BEFORE the activity is launched — the gate is read once, on the
     * first composition, so a `@Before` method would be too late.
     */
    private val unfinishedOnboarding = object : ExternalResource() {
        private var previousOnboardingDone = false
        private var previousAppLockEnabled = false

        override fun before() {
            previousOnboardingDone = preferences.getBoolean(OnboardingDoneKey, false)
            previousAppLockEnabled = preferences.getBoolean(AppLockEnabledKey, false)
            preferences.edit(commit = true) {
                putBoolean(OnboardingDoneKey, false)
                // The lock gate would sit in front of whatever the start
                // destination is, so it is out of the way for this assertion.
                putBoolean(AppLockEnabledKey, false)
            }
        }

        override fun after() {
            preferences.edit(commit = true) {
                putBoolean(OnboardingDoneKey, previousOnboardingDone)
                putBoolean(AppLockEnabledKey, previousAppLockEnabled)
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(unfinishedOnboarding).around(composeRule)

    @Test
    fun rendersTheOnboardingStartScreenWhenOnboardingIsIncomplete() {
        val tagline = string(R.string.onboarding_tagline)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(tagline).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(tagline).assertIsDisplayed()
    }

    private companion object {
        const val OnboardingDoneKey = "onboarding_done"
        const val AppLockEnabledKey = "app_lock_enabled"
    }
}
