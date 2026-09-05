package tech.mmarca.openvitals.features.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The first screen of a fresh install, on a device that may not run the app yet, in a language the user may not read. */
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anOutdatedHealthConnectExplainsItselfAndOffersTheInstall() {
        setUnavailable(HealthConnectAvailability.NEEDS_PROVIDER_UPDATE)

        composeRule.onNodeWithText(string(R.string.onboarding_health_connect_update))
            .performScrollTo()
            .assertIsDisplayed()
        // Without this button the app is a dead end.
        composeRule.onNodeWithText(string(R.string.onboarding_install_health_connect))
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun theHeaderIdentifiesTheAppAndLetsTheLanguageBeChangedBeforeAnythingElse() {
        // Composing the header proves the wide logo drawable still resolves.
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OnboardingHeader(
                        state = OnboardingUiState(
                            availability = HealthConnectAvailability.AVAILABLE,
                            appLanguage = AppLanguage.SYSTEM,
                            isCheckingPermissions = false,
                        ),
                        onSelectLanguage = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.app_name)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_tagline))
            .performScrollTo()
            .assertIsDisplayed()
        // The picker sits above everything so a user who cannot read the copy can change the language first.
        composeRule.onNodeWithText(string(R.string.settings_language_system))
            .performScrollTo()
            .assertIsEnabled()
    }

    private fun setUnavailable(availability: HealthConnectAvailability) {
        composeRule.setContent {
            OpenVitalsTheme {
                UnavailableContent(
                    state = OnboardingUiState(
                        availability = availability,
                        appLanguage = AppLanguage.SYSTEM,
                        isCheckingPermissions = false,
                    ),
                    onSelectLanguage = {},
                )
            }
        }
    }
}
