package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/**
 * One reading of CoMaps' live navigation row.
 *
 * Every field is exactly what the provider hands over — the distances arrive
 * **already formatted for display** ("450 m", "1.2 km"), because CoMaps
 * formats them against its own locale and unit settings before exposing them.
 * We do not parse them back into numbers: the number we would recover is not
 * one we could re-format any better, and a distance the user reads in CoMaps
 * should read the same here.
 */
data class CoMapsNavigationSnapshot(
    val sampledAt: Instant,
    val sessionState: String,
    val currentStreet: String = "",
    val nextStreet: String = "",
    val distanceToTurn: String = "",
    val distanceToTarget: String = "",
    val distanceToNextStop: String = "",
    val totalTimeSeconds: Int? = null,
    val timeToNextStopSeconds: Int? = null,
    val completionPercent: Double? = null,
    val carDirection: String = "",
    val pedestrianDirection: String = "",
    val exitNumber: String = "",
) {
    /**
     * Everything except the timestamp, so two readings taken seconds apart that
     * say the same thing compare equal. This is what decides whether a sample
     * is worth keeping — see [CoMapsNavigationSampleRecorder].
     */
    val contentKey: String
        get() = listOf(
            sessionState,
            currentStreet,
            nextStreet,
            distanceToTurn,
            distanceToTarget,
            distanceToNextStop,
            totalTimeSeconds?.toString() ?: "",
            timeToNextStopSeconds?.toString() ?: "",
            completionPercent?.toString() ?: "",
            carDirection,
            pedestrianDirection,
            exitNumber,
        ).joinToString("|")
}

/**
 * What OpenVitals can currently learn from CoMaps.
 *
 * Every one of these is a *normal* state, not an error to shout about: the
 * user is recording an activity, and CoMaps guidance is a bonus. Recording
 * continues through all of them.
 */
sealed interface CoMapsNavigationState {
    /** The user has not switched the integration on. */
    data object Disabled : CoMapsNavigationState

    /** No known CoMaps package is installed. */
    data object AppUnavailable : CoMapsNavigationState

    /**
     * CoMaps is installed, but this build does not expose the navigation
     * provider (it predates the provider, or is a variant without it).
     */
    data object ProviderUnavailable : CoMapsNavigationState

    /**
     * The provider is there, but we have not been granted
     * `<comapsPackage>.permission.READ_NAVIGATION_DATA`.
     */
    data object PermissionMissing : CoMapsNavigationState

    /**
     * CoMaps is there and readable, but is not currently guiding anyone.
     * Either the provider answered with an empty row, or it answered with a
     * row whose `session_state` says nobody is being guided — see
     * [isCoMapsGuiding].
     */
    data object NotNavigating : CoMapsNavigationState

    /** CoMaps is navigating, and this is what it says. */
    data class Active(
        val snapshot: CoMapsNavigationSnapshot,
        /**
         * Null from a CoMaps predating the geometry contract. Here rather
         * than on [snapshot] because the snapshot is persisted, and a
         * revision is not history.
         */
        val routeRevision: Int? = null,
        val destination: CoMapsCoordinate? = null,
        val destinationName: String? = null,
    ) : CoMapsNavigationState

    /** The query itself failed. */
    data class Error(val message: String? = null) : CoMapsNavigationState
}

/**
 * CoMaps' `RoutingSessionState` values, as the provider spells them.
 * Only a subset means "someone is being guided right now".
 */
private val CoMapsGuidingSessionStates = setOf(
    // Following the line, and the one case that matters most for a recording:
    // off it, recalculating, still very much navigating.
    "OnRoute",
    "OffRoute",
    // The session is live; the instructions are momentarily stale rather than
    // gone. Blanking the panel here would make it flicker on every rebuild.
    "RouteNeedsRebuild",
    "RouteRebuilding",
)

/**
 * Whether `session_state` means CoMaps is actually guiding someone.
 *
 * This is load-bearing, and the reason is not obvious from our side.
 * **CoMaps never clears its cached routing info** — `RoutingController`
 * assigns `mCachedRoutingInfo` but nothing ever nulls it — and the provider
 * answers straight out of that cache. So the empty cursor we treat as "not
 * navigating" only ever happens *before the first route of the CoMaps
 * process*. Once any route has been built, the provider keeps returning that
 * route's row indefinitely, long after the user arrived and closed it.
 *
 * Without this check the panel pins the last turn of the last route and calls
 * it live guidance — and no amount of polling or observing would notice,
 * because nothing changes and nothing is notified. The column is the only
 * signal there is.
 *
 * Unknown values are treated as NOT guiding: a state we do not recognise is
 * not one we should draw a turn arrow for.
 */
fun isCoMapsGuiding(sessionState: String): Boolean =
    sessionState.trim() in CoMapsGuidingSessionStates

/**
 * The turn arrow to draw. CoMaps' own direction vocabulary is far richer than
 * this (it distinguishes, for instance, which side of a roundabout you leave
 * by), but a turn shown to someone mid-run has to be readable at a glance.
 */
enum class CoMapsTurnKind {
    UNKNOWN,
    STRAIGHT,
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    U_TURN,
    ROUNDABOUT,
    FINISH,
}

/**
 * Maps a raw CoMaps direction name to a turn arrow.
 *
 * CoMaps sends the *enum name* of its own direction type, and it has changed
 * spelling before — `TurnRight` in one build, `TURN_RIGHT` in another. So this
 * normalizes away case and separators and then matches on substrings, which
 * survives both. The order matters: `SHARPRIGHT` and `SLIGHTRIGHT` must be
 * tested before the bare `RIGHT` they both contain.
 */
fun coMapsTurnKindForDirection(direction: String): CoMapsTurnKind {
    val normalized = direction.uppercase().replace(Regex("[^A-Z]"), "")
    return when {
        normalized.isEmpty() -> CoMapsTurnKind.UNKNOWN
        "DESTINATION" in normalized || "FINISH" in normalized || "ARRIVE" in normalized ->
            CoMapsTurnKind.FINISH
        "UTURN" in normalized || "TURNBACK" in normalized -> CoMapsTurnKind.U_TURN
        "ROUNDABOUT" in normalized -> CoMapsTurnKind.ROUNDABOUT
        "SHARPRIGHT" in normalized -> CoMapsTurnKind.SHARP_RIGHT
        "SLIGHTRIGHT" in normalized -> CoMapsTurnKind.SLIGHT_RIGHT
        "RIGHT" in normalized -> CoMapsTurnKind.RIGHT
        "SHARPLEFT" in normalized -> CoMapsTurnKind.SHARP_LEFT
        "SLIGHTLEFT" in normalized -> CoMapsTurnKind.SLIGHT_LEFT
        "LEFT" in normalized -> CoMapsTurnKind.LEFT
        "STRAIGHT" in normalized || "NOTURN" in normalized || "NONE" in normalized ->
            CoMapsTurnKind.STRAIGHT
        else -> CoMapsTurnKind.UNKNOWN
    }
}

/**
 * A raw direction name rendered as something a person would read: `TURN_RIGHT`
 * and `TurnSlightLeft` both become "Turn right" / "Turn slight left".
 *
 * Deliberately not localized. It is CoMaps' vocabulary, not ours, and it is a
 * fallback for a direction we do not have an arrow for — inventing
 * translations for an enum we do not own would be worse than showing it.
 */
fun coMapsReadableDirection(direction: String): String {
    val words = direction
        .replace('_', ' ')
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
    if (words.isEmpty()) return ""
    return (listOf(words.first().replaceFirstChar { it.uppercase() }) + words.drop(1))
        .joinToString(" ")
}

/**
 * CoMaps drives cars and it walks people, and it fills exactly one of the two
 * direction fields depending on which.
 */
fun coMapsNavigationDirection(snapshot: CoMapsNavigationSnapshot): String =
    snapshot.carDirection.ifEmpty { snapshot.pedestrianDirection }

/**
 * Decides which live readings are worth keeping in the activity's history.
 *
 * The provider will happily answer every second, and almost every answer is
 * the same as the last one. A sample is kept when the guidance actually
 * *changed*, or when [minSampleInterval] has passed since the last one kept —
 * so a long straight road costs one sample every 15 seconds rather than
 * fifteen, and a flurry of turns is never missed.
 */
class CoMapsNavigationSampleRecorder(
    private val minSampleInterval: Duration = Duration.ofSeconds(15),
) {
    private val recorded = mutableListOf<CoMapsNavigationSnapshot>()

    fun reset() = recorded.clear()

    /** Returns whether the snapshot was kept. */
    fun accept(snapshot: CoMapsNavigationSnapshot): Boolean {
        val previous = recorded.lastOrNull()
        val keep = previous == null ||
            previous.contentKey != snapshot.contentKey ||
            Duration.between(previous.sampledAt, snapshot.sampledAt) >= minSampleInterval
        if (keep) recorded.add(snapshot)
        return keep
    }

    val samples: List<CoMapsNavigationSnapshot>
        get() = recorded.toList()
}

/**
 * One point of a planned route. Not an exercise route point: that one requires
 * a timestamp and means "where the user actually was".
 */
data class CoMapsCoordinate(
    val latitude: Double,
    val longitude: Double,
)

/**
 * The route CoMaps is guiding along.
 *
 * Never part of [CoMapsNavigationSnapshot]: that one is sampled into
 * `contentKey` and persisted per sample, so a polyline on it would be
 * stringified and written once per banked reading. This rides on live
 * recording state instead, and is never saved.
 */
class CoMapsRoutePolyline(
    /** Bumped by CoMaps on every build, rebuild and close. */
    val revision: Int,
    /** Interleaved `lat, lon`, one contiguous buffer straight off the cursor. */
    val points: DoubleArray,
    val destination: CoMapsCoordinate? = null,
    val destinationName: String? = null,
) {
    val pointCount: Int get() = points.size / 2

    val isEmpty: Boolean get() = points.size < 4

    fun latitudeAt(index: Int): Double = points[index * 2]

    fun longitudeAt(index: Int): Double = points[index * 2 + 1]

    /**
     * O(1) on purpose. This sits on state rebuilt every second, and a deep
     * compare of tens of thousands of doubles is the cost this feature must
     * not add. The length guards a process restart reissuing revision 0 for a
     * different route.
     */
    override fun equals(other: Any?): Boolean =
        other is CoMapsRoutePolyline &&
            other.revision == revision &&
            other.points.size == points.size &&
            other.destination == destination

    override fun hashCode(): Int =
        31 * (31 * revision + points.size) + (destination?.hashCode() ?: 0)
}
