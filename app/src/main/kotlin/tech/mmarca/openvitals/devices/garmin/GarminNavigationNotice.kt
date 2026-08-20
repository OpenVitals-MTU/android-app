package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsTurnKind
import tech.mmarca.openvitals.domain.model.coMapsNavigationDirection
import tech.mmarca.openvitals.domain.model.coMapsReadableDirection
import tech.mmarca.openvitals.domain.model.coMapsTurnKindForDirection

/**
 * CoMaps guidance, worded for a watch face.
 *
 * Garmin has no turn-by-turn channel a phone can drive, so the guidance goes
 * to the wrist as a notification: one notification, updated in place as the
 * route unfolds. The watch shows the title large and the body under it, so
 * the turn is the title, the distance to it the subtitle, and the rest is
 * body text. Distances and times arrive from CoMaps already formatted in
 * the wearer's own units and are shown as sent, exactly as on the phone.
 */
data class GarminNavigationNotice(
    val title: String,
    val subtitle: String,
    val body: String,
    /**
     * What must change for the notice to be worth re-announcing at once: the
     * manoeuvre and the street it leads onto. The distance counting down is
     * not in it, so a straight road is refreshed on a timer, not per fix.
     */
    val instructionKey: String,
) {
    /** Everything shown, so an unchanged notice is never re-sent. */
    val contentKey: String get() = "$title|$subtitle|$body"

    companion object {
        fun from(snapshot: CoMapsNavigationSnapshot): GarminNavigationNotice {
            val direction = coMapsNavigationDirection(snapshot)
            val kind = coMapsTurnKindForDirection(direction)
            val title = turnPhrase(kind, snapshot.exitNumber.trim())
                ?: coMapsReadableDirection(direction).ifEmpty { "Navigation" }
            val street = snapshot.nextStreet.trim()
            val body = buildList {
                if (street.isNotEmpty()) add(street)
                val remaining = listOfNotNull(
                    snapshot.distanceToTarget.trim().takeIf { it.isNotEmpty() }?.let { "$it left" },
                    snapshot.totalTimeSeconds?.takeIf { it > 0 }?.let(::formatDuration),
                ).joinToString(" · ")
                if (remaining.isNotEmpty()) add(remaining)
            }.joinToString("\n")
            return GarminNavigationNotice(
                title = title,
                subtitle = snapshot.distanceToTurn.trim(),
                body = body,
                instructionKey = "$title|$street",
            )
        }

        /**
         * Plain words for the arrows the phone draws. Not localized, like the
         * rest of the CoMaps vocabulary shown in the app: the watch cannot
         * draw an arrow, and the instruction must read the same on both.
         */
        private fun turnPhrase(kind: CoMapsTurnKind, exitNumber: String): String? = when (kind) {
            CoMapsTurnKind.STRAIGHT -> "Continue straight"
            CoMapsTurnKind.SLIGHT_LEFT -> "Bear left"
            CoMapsTurnKind.LEFT -> "Turn left"
            CoMapsTurnKind.SHARP_LEFT -> "Sharp left"
            CoMapsTurnKind.SLIGHT_RIGHT -> "Bear right"
            CoMapsTurnKind.RIGHT -> "Turn right"
            CoMapsTurnKind.SHARP_RIGHT -> "Sharp right"
            CoMapsTurnKind.U_TURN -> "Make a U-turn"
            CoMapsTurnKind.ROUNDABOUT -> exitNumber.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { "Roundabout, exit $it" }
                ?: "Roundabout"
            CoMapsTurnKind.FINISH -> "Arrive at destination"
            CoMapsTurnKind.UNKNOWN -> null
        }

        private fun formatDuration(totalSeconds: Int): String {
            val minutes = (totalSeconds + 30) / 60
            if (minutes < 60) return "$minutes min"
            val hours = minutes / 60
            val rest = minutes % 60
            return if (rest == 0) "$hours h" else "$hours h $rest min"
        }
    }
}

/**
 * Decides when the watch hears about guidance, kept apart from the radio so
 * it can be tested with a clock.
 *
 * CoMaps speaks at every location fix, about once a second, and nearly every
 * reading differs from the last only by a few metres on the countdown. A
 * notification MODIFY per fix would keep the link busy for nothing the
 * wearer can read, so: a new manoeuvre or street goes out at once, a mere
 * countdown change waits for [refreshIntervalMillis], and an identical
 * notice is never re-sent. Guidance ending withdraws the notification.
 */
class GarminNavigationRelayPolicy(
    private val refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
) {
    sealed interface Decision {
        data class Show(val notice: GarminNotificationNoticeUpdate) : Decision
        data object Withdraw : Decision
        data object Nothing : Decision
    }

    private var shown: GarminNavigationNotice? = null
    private var shownAtMillis: Long = 0L

    fun decide(state: CoMapsNavigationState, nowMillis: Long): Decision {
        val snapshot = (state as? CoMapsNavigationState.Active)?.snapshot
        if (snapshot == null) {
            if (shown == null) return Decision.Nothing
            shown = null
            return Decision.Withdraw
        }
        val notice = GarminNavigationNotice.from(snapshot)
        val previous = shown
        val send = when {
            previous == null -> true
            previous.contentKey == notice.contentKey -> false
            previous.instructionKey != notice.instructionKey -> true
            else -> nowMillis - shownAtMillis >= refreshIntervalMillis
        }
        if (!send) return Decision.Nothing
        shown = notice
        shownAtMillis = nowMillis
        return Decision.Show(GarminNotificationNoticeUpdate(notice, isUpdate = previous != null))
    }

    /** Forgets what is on the wrist, e.g. after the link that carried it dropped. */
    fun reset() {
        shown = null
        shownAtMillis = 0L
    }

    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLIS = 5_000L
    }
}

data class GarminNotificationNoticeUpdate(
    val notice: GarminNavigationNotice,
    /** Whether a notice is already on the wrist, so this replaces it rather than adds. */
    val isUpdate: Boolean,
)
