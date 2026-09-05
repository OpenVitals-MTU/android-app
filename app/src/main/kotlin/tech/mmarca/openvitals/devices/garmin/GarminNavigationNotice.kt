package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsTurnKind
import tech.mmarca.openvitals.domain.model.coMapsNavigationDirection
import tech.mmarca.openvitals.domain.model.coMapsReadableDirection
import tech.mmarca.openvitals.domain.model.coMapsTurnKindForDirection

/**
 * CoMaps guidance worded for a watch face: the turn is the title, the
 * distance the subtitle, the rest body text. Distances are shown as sent.
 */
data class GarminNavigationNotice(
    val title: String,
    val subtitle: String,
    val body: String,
    /** What must change to re-announce at once: manoeuvre and street. Countdowns wait for the timer. */
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

        /** Plain words for the arrows. Not localized, like the rest of the CoMaps vocabulary. */
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
 * Decides when the watch hears about guidance. A new manoeuvre goes out at
 * once, a countdown change waits for [refreshIntervalMillis], an identical
 * notice is never re-sent. Testable with a clock.
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
