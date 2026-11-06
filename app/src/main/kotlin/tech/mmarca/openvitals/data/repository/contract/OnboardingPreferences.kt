package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.preferences.AppLanguage

/** What onboarding asks and records. */
interface OnboardingPreferences {

    var onboardingDone: Boolean

    /**
     * The one-time notice that watches and scales moved to OpenWatch. Set when
     * onboarding completes, so a fresh install never sees it.
     */
    var openWatchMoveNoticeSeen: Boolean

    var appLanguage: AppLanguage

    var mindfulnessOptIn: Boolean

    /** Off by default: asking for the mindfulness permission crashes Health Connect on some devices. */
    var healthConnectMindfulnessEnabled: Boolean

    /** Records the current policy version and the moment of consent. */
    fun acceptCurrentPrivacyPolicy()
}
