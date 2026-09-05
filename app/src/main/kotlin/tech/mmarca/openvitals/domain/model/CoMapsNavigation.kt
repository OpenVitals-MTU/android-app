package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/**
 * One reading of CoMaps' navigation row. Distances arrive already formatted
 * for display and are never parsed back.
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
    /** Everything except the timestamp, so equal readings compare equal. */
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

/** What OpenVitals can learn from CoMaps. Every state is normal; recording continues. */
sealed interface CoMapsNavigationState {
    /** The user has not switched the integration on. */
    data object Disabled : CoMapsNavigationState

    /** No known CoMaps package is installed. */
    data object AppUnavailable : CoMapsNavigationState

    /** CoMaps is installed but this build has no navigation provider. */
    data object ProviderUnavailable : CoMapsNavigationState

    /** The provider is there, but READ_NAVIGATION_DATA is not granted. */
    data object PermissionMissing : CoMapsNavigationState

    /** CoMaps is readable but not guiding anyone; see [isCoMapsGuiding]. */
    data object NotNavigating : CoMapsNavigationState

    /** CoMaps is navigating, and this is what it says. */
    data class Active(
        val snapshot: CoMapsNavigationSnapshot,
        /** Null from a CoMaps predating the geometry contract. Not on [snapshot]: that is persisted. */
        val routeRevision: Int? = null,
        val destination: CoMapsCoordinate? = null,
        val destinationName: String? = null,
    ) : CoMapsNavigationState

    /** The query itself failed. */
    data class Error(val message: String? = null) : CoMapsNavigationState
}

/** CoMaps' `RoutingSessionState` values that mean someone is being guided. */
private val CoMapsGuidingSessionStates = setOf(
    // Off the line and recalculating is still navigating.
    "OnRoute",
    "OffRoute",
    // Live session, stale instructions. Blanking here would flicker.
    "RouteNeedsRebuild",
    "RouteRebuilding",
)

/**
 * Whether `session_state` means CoMaps is guiding. Load-bearing: CoMaps never
 * clears its cached routing info, so the provider keeps returning the last
 * route's row forever. Unknown values count as not guiding.
 */
fun isCoMapsGuiding(sessionState: String): Boolean =
    sessionState.trim() in CoMapsGuidingSessionStates

/** The turn arrow to draw. Coarser than CoMaps' vocabulary, readable mid-run. */
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
 * Maps a raw CoMaps direction name to an arrow. Spelling has changed between
 * builds, so this normalizes and matches substrings. Order matters:
 * SHARPRIGHT before RIGHT.
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

/** A raw direction name as readable text. Not localized: it is CoMaps' vocabulary. */
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

/** CoMaps fills one of two direction fields, depending on driving or walking. */
fun coMapsNavigationDirection(snapshot: CoMapsNavigationSnapshot): String =
    snapshot.carDirection.ifEmpty { snapshot.pedestrianDirection }

/**
 * Keeps a live reading when the guidance changed or [minSampleInterval]
 * passed since the last kept one.
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

/** One point of a planned route, unlike an exercise route point. */
data class CoMapsCoordinate(
    val latitude: Double,
    val longitude: Double,
)

/**
 * The route CoMaps is guiding along. Never on [CoMapsNavigationSnapshot],
 * which is persisted per sample.
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

    /** O(1) on purpose: this sits on state rebuilt every second. */
    override fun equals(other: Any?): Boolean =
        other is CoMapsRoutePolyline &&
            other.revision == revision &&
            other.points.size == points.size &&
            other.destination == destination

    override fun hashCode(): Int =
        31 * (31 * revision + points.size) + (destination?.hashCode() ?: 0)
}
