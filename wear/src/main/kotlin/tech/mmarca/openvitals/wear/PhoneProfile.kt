package tech.mmarca.openvitals.wear

import tech.mmarca.openvitals.wearlink.WearLinkProfile

/**
 * The phone's settings and today's Health Connect totals, as the phone last
 * sent them ([WearLinkProfile]), kept in [MetricStore]'s state table as their
 * protocol lines, so the format is defined once, in `:wearlink`. The newest
 * replaces the last; a line this build cannot read counts as none.
 */
internal object PhoneProfile {

    private const val PROFILE_KEY = "phone_profile"
    private const val TOTALS_KEY = "phone_totals"

    fun save(store: MetricStore, profile: WearLinkProfile.Profile) {
        store.putState(PROFILE_KEY, WearLinkProfile.formatProfile(profile))
    }

    fun save(store: MetricStore, totals: WearLinkProfile.Totals) {
        store.putState(TOTALS_KEY, WearLinkProfile.formatTotals(totals))
    }

    /** The settings the phone last sent, or null before the first sync with a phone that sends them. */
    fun profile(store: MetricStore): WearLinkProfile.Profile? =
        store.state(PROFILE_KEY)?.let(WearLinkProfile::parseProfile)

    /** The phone's totals for [day], or null when the last ones it sent are for another day. */
    fun totals(store: MetricStore, day: TileDay): WearLinkProfile.Totals? =
        store.state(TOTALS_KEY)?.let(WearLinkProfile::parseTotals)?.takeIf { it.dayStartEpochMillis == day.startMillis }
}
