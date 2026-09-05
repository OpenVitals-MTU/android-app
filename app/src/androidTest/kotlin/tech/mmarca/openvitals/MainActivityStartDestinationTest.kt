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
 * The start gate is computed inside [MainActivity]'s `setContent`, so the real activity is
 * launched against a preferences file that says onboarding is unfinished. The flag is put back
 * afterwards. The onboarding tagline is the fingerprint because both branches render it.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityStartDestinationTest {

    private val preferences: SharedPreferences
        get() = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)

    /** Runs before the activity is launched; the gate is read once, on the first composition. */
    private val unfinishedOnboarding = object : ExternalResource() {
        private var previousOnboardingDone = false
        private var previousAppLockEnabled = false

        override fun before() {
            previousOnboardingDone = preferences.getBoolean(OnboardingDoneKey, false)
            previousAppLockEnabled = preferences.getBoolean(AppLockEnabledKey, false)
            preferences.edit(commit = true) {
                putBoolean(OnboardingDoneKey, false)
                // The lock gate would sit in front of the start destination.
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
