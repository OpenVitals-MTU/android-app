package tech.mmarca.openvitals.devices.notifications

/**
 * Decides whether a notification is worth forwarding to the watch. Pure
 * logic, JVM-tested: it runs on every notification the phone posts. Only
 * [Config.blockedPackages] is the user's; ongoing notifications are always
 * dropped. Rejecting here, before Bluetooth, is the feature's biggest
 * battery decision.
 */
object NotificationFilter {

    /** Android's `NotificationManager.IMPORTANCE_*` values, kept local. */
    const val IMPORTANCE_NONE = 0
    const val IMPORTANCE_MIN = 1
    const val IMPORTANCE_UNSPECIFIED = -1000

    /** Android's `NotificationManager.INTERRUPTION_FILTER_*` values. */
    const val INTERRUPTION_FILTER_ALL = 1
    const val INTERRUPTION_FILTER_PRIORITY = 2
    const val INTERRUPTION_FILTER_NONE = 3
    const val INTERRUPTION_FILTER_ALARMS = 4
    const val INTERRUPTION_FILTER_UNKNOWN = 0

    /** What the user configured, mirrored by the watch-notifications screen. */
    data class Config(
        val enabled: Boolean,
        val blockedPackages: Set<String>,
        /** Null when no watch is paired: there is nowhere to send anything. */
        val watchAddress: String?,
    ) {
        companion object {
            val disabled = Config(enabled = false, blockedPackages = emptySet(), watchAddress = null)
        }
    }

    /** One notification, reduced to what the decision actually depends on. */
    data class Candidate(
        val packageName: String,
        val title: String?,
        val body: String?,
        val ongoing: Boolean,
        val foregroundService: Boolean,
        val groupSummary: Boolean,
        val localOnly: Boolean,
        val channelImportance: Int,
    )

    /** Why a notification was dropped. [KEEP] means it was not. */
    enum class Verdict {
        KEEP,
        DISABLED,
        NO_WATCH,
        OWN_PACKAGE,
        BLOCKED,
        ONGOING,
        GROUP_SUMMARY,
        LOCAL_ONLY,
        LOW_IMPORTANCE,
        EMPTY,
        DO_NOT_DISTURB,
    }

    /** Whether [candidate] should reach the watch. [ownPackage] stops our own reminders looping. */
    fun verdict(
        candidate: Candidate,
        config: Config,
        ownPackage: String,
        interruptionFilter: Int = INTERRUPTION_FILTER_ALL,
    ): Verdict {
        if (!config.enabled) return Verdict.DISABLED
        if (config.watchAddress.isNullOrEmpty()) return Verdict.NO_WATCH
        if (candidate.packageName == ownPackage) return Verdict.OWN_PACKAGE
        if (candidate.packageName in config.blockedPackages) return Verdict.BLOCKED

        // Reuses the phone's quiet hours. UNKNOWN means allow: swallowing is worse.
        if (interruptionFilter == INTERRUPTION_FILTER_NONE ||
            interruptionFilter == INTERRUPTION_FILTER_ALARMS
        ) {
            return Verdict.DO_NOT_DISTURB
        }

        // Media players and downloads repaint ongoing notifications constantly.
        if (candidate.ongoing || candidate.foregroundService) return Verdict.ONGOING

        // The parent of a bundle: keeping it delivers every thread twice.
        if (candidate.groupSummary) return Verdict.GROUP_SUMMARY

        // The posting app explicitly said this should not leave the phone.
        if (candidate.localOnly) return Verdict.LOCAL_ONLY

        if (candidate.channelImportance == IMPORTANCE_NONE ||
            candidate.channelImportance == IMPORTANCE_MIN
        ) {
            return Verdict.LOW_IMPORTANCE
        }

        // Nothing to render on a watch face.
        if (candidate.title.isNullOrBlank() && candidate.body.isNullOrBlank()) {
            return Verdict.EMPTY
        }

        return Verdict.KEEP
    }

    /**
     * `GarminNotificationCategory` ordinals, duplicated so this file stays
     * dependency-free. The wire values are frozen.
     */
    object Category {
        const val OTHER = 0
        const val INCOMING_CALL = 1
        const val MISSED_CALL = 2
        const val VOICEMAIL = 3
        const val SOCIAL = 4
        const val SCHEDULE = 5
        const val EMAIL = 6
        const val NEWS = 7
        const val HEALTH_AND_FITNESS = 8
        const val BUSINESS_AND_FINANCE = 9
        const val LOCATION = 10
        const val ENTERTAINMENT = 11
        const val SMS = 12
    }

    /** Android's `Notification.category` as a GNCS category. Unrecognised is OTHER, not a guess. */
    fun categoryOrdinal(androidCategory: String?): Int = when (androidCategory) {
        "call" -> Category.INCOMING_CALL
        "missed_call" -> Category.MISSED_CALL
        "voicemail" -> Category.VOICEMAIL
        "msg" -> Category.SMS
        "email" -> Category.EMAIL
        "social" -> Category.SOCIAL
        "event", "alarm", "reminder" -> Category.SCHEDULE
        "promo", "recommendation" -> Category.NEWS
        "navigation" -> Category.LOCATION
        "transport" -> Category.ENTERTAINMENT
        "workout" -> Category.HEALTH_AND_FITNESS
        else -> Category.OTHER
    }
}
