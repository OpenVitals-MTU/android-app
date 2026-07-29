package tech.mmarca.openvitals.notification_listener_native

/**
 * Decides whether a notification is worth waking a Flutter isolate for.
 *
 * Pure logic over [Candidate], with no Android types anywhere, so it is unit
 * tested on the JVM without Robolectric. That matters more here than anywhere
 * else in this plugin: this is the code that runs on EVERY notification the
 * phone posts — a media player repainting its transport controls once a second,
 * a download progress bar ticking — and a bug in it means the watch buzzes
 * constantly and the battery goes.
 *
 * Everything is structural except [Config.blockedPackages]. The user chooses
 * which apps to silence; they do not get to choose to be spammed by ongoing
 * notifications, because nobody wants that and offering it is how a settings
 * screen grows a footgun.
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

    /** What the user configured, mirrored from Dart. */
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

    /**
     * Whether [candidate] should reach the watch.
     *
     * [ownPackage] is this app's own id. Forwarding our own reminders would loop:
     * a hydration reminder would arrive on the wrist as a mirrored phone
     * notification rather than as itself.
     */
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

        // The phone already has a quiet-hours setting, so this reuses it rather
        // than inventing a second one the user has to keep in sync. UNKNOWN is
        // treated as "allow": it means the listener has not been told yet, and
        // silently swallowing notifications is worse than one arriving during
        // Do Not Disturb.
        if (interruptionFilter == INTERRUPTION_FILTER_NONE ||
            interruptionFilter == INTERRUPTION_FILTER_ALARMS
        ) {
            return Verdict.DO_NOT_DISTURB
        }

        // The big one. A media player, a navigation session and a download all
        // post ongoing notifications and repaint them constantly; without this
        // the watch never stops buzzing.
        if (candidate.ongoing || candidate.foregroundService) return Verdict.ONGOING

        // The parent of a bundle. Keeping it would deliver every chat thread
        // twice — once as itself and once inside the summary.
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
     * Maps a notification to `GarminNotificationCategory`'s ordinal.
     *
     * The ordinals are the GNCS wire values and are duplicated from the Dart
     * enum rather than shared, because there is no way to share an enum across
     * the Pigeon boundary without generating one — and generating one would put
     * the wire format in two places instead of one. The Dart side is the source
     * of truth; this is a mirror, and its test names the values.
     */
    object Category {
        const val OTHER = 0L
        const val INCOMING_CALL = 1L
        const val MISSED_CALL = 2L
        const val VOICEMAIL = 3L
        const val SOCIAL = 4L
        const val SCHEDULE = 5L
        const val EMAIL = 6L
        const val NEWS = 7L
        const val HEALTH_AND_FITNESS = 8L
        const val BUSINESS_AND_FINANCE = 9L
        const val LOCATION = 10L
        const val ENTERTAINMENT = 11L
        const val SMS = 12L
    }

    /**
     * Translates Android's `Notification.category` into a GNCS category.
     *
     * A null or unrecognised category is OTHER rather than a guess: the watch
     * groups and prioritises by this, so mislabelling a message as a call is
     * worse than declining to label it.
     */
    fun categoryOrdinal(androidCategory: String?): Long = when (androidCategory) {
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
