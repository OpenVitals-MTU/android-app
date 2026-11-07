package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.preferences.AppLanguage

/** What onboarding asks and records. */
interface OnboardingPreferences {

    var onboardingDone: Boolean

    var appLanguage: AppLanguage

    var mindfulnessOptIn: Boolean

    /** Off by default: asking for the mindfulness permission crashes Health Connect on some devices. */
    var healthConnectMindfulnessEnabled: Boolean

    /** Records the current policy version and the moment of consent. */
    fun acceptCurrentPrivacyPolicy()
}
