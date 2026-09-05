package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.comaps.CoMapsLiveEvent
import tech.mmarca.openvitals.comaps.CoMapsNavigationSource
import tech.mmarca.openvitals.comaps.CoMapsProviderAnswer
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsCoordinate
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.isCoMapsGuiding
import tech.mmarca.openvitals.domain.model.simplifyCoMapsRoutePoints

@Singleton
class CoMapsNavigationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val source: CoMapsNavigationSource,
) : CoMapsNavigationRepository {

    /** Injectable clock: the liveness window is time arithmetic tests must own. */
    internal var now: () -> Instant = Instant::now

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    /** When CoMaps last said something moved. Shared by the feed and the safety poll. */
    private var lastLiveChangeAt: Instant? = null
    private var observing = false

    override suspend fun readLive(): CoMapsNavigationState = try {
        resolveAnswer(source.queryLive())
    } catch (error: Exception) {
        CoMapsNavigationState.Error(error.message)
    }

    override fun watchLive(): Flow<CoMapsNavigationState> =
        source.liveUpdates().map { event ->
            try {
                resolveEvent(event)
            } catch (error: Exception) {
                CoMapsNavigationState.Error(error.message)
            }
        }

    /**
     * [stateFrom], plus whether anyone is still driving the route. CoMaps
     * never clears its cached routing info, so a row only counts as guidance
     * while CoMaps is still saying things about it. With no observer the
     * read is believed.
     */
    private fun resolveEvent(event: CoMapsLiveEvent): CoMapsNavigationState {
        observing = event.observing
        // A new recording starts knowing nothing.
        if (event.initial) lastLiveChangeAt = null
        if (event.live) lastLiveChangeAt = now()
        return resolveAnswer(event.answer)
    }

    private fun resolveAnswer(answer: CoMapsProviderAnswer): CoMapsNavigationState {
        val state = stateFrom(answer)
        if (state !is CoMapsNavigationState.Active || !observing) return state

        val lastChange = lastLiveChangeAt
        val isLive = lastChange != null &&
            Duration.between(lastChange, now()) <= LivenessWindow
        return if (isLive) state else CoMapsNavigationState.NotNavigating
    }

    /** The platform's answer as the domain's. Unavailable statuses are states, not failures. */
    private fun stateFrom(answer: CoMapsProviderAnswer): CoMapsNavigationState = when (answer) {
        is CoMapsProviderAnswer.Active -> {
            val snapshot = snapshotFrom(answer.row)
            // A row is not live guidance: the session state says whether anyone is guided.
            if (!isCoMapsGuiding(snapshot.sessionState)) {
                CoMapsNavigationState.NotNavigating
            } else {
                // Read off the row, never into the persisted snapshot.
                CoMapsNavigationState.Active(
                    snapshot = snapshot,
                    routeRevision = answer.row.whole("route_revision"),
                    destination = coordinate(
                        answer.row.fraction("dest_lat"),
                        answer.row.fraction("dest_lon"),
                    ),
                    destinationName = (answer.row["dest_title"] as? String)?.trim(),
                )
            }
        }
        CoMapsProviderAnswer.NotNavigating -> CoMapsNavigationState.NotNavigating
        CoMapsProviderAnswer.PermissionMissing -> CoMapsNavigationState.PermissionMissing
        CoMapsProviderAnswer.ProviderUnavailable -> CoMapsNavigationState.ProviderUnavailable
        CoMapsProviderAnswer.AppUnavailable -> CoMapsNavigationState.AppUnavailable
        is CoMapsProviderAnswer.Failure -> CoMapsNavigationState.Error(answer.message)
    }

    override suspend fun readRouteGeometry(revision: Int): CoMapsRoutePolyline? {
        val points = source.queryRoute() ?: return null
        if (points.size < 4) return null
        // Sub-pixel simplification once per fetch, off the caller's thread.
        val simplified = withContext(Dispatchers.Default) {
            simplifyCoMapsRoutePoints(points)
        }
        // The destination rides on the live row, so the caller fills it in.
        return CoMapsRoutePolyline(revision = revision, points = simplified)
    }

    override fun permissionName(): String? = source.permissionName()

    override fun hasPermission(): Boolean = source.hasPermission()

    override fun onPermissionChanged() = source.invalidateResolvedPackage()

    override fun canLaunchCoMaps(): Boolean = source.canLaunchCoMaps()

    override fun launchForPlanning(latitude: Double?, longitude: Double?): Boolean =
        source.launchForPlanning(latitude, longitude)

    override fun saveSamples(activityId: String, samples: List<CoMapsNavigationSnapshot>) {
        preferences.edit {
            if (activityId.isBlank() || samples.isEmpty()) {
                remove(activityId.key())
            } else {
                putString(activityId.key(), encodeCoMapsSamples(samples))
            }
        }
    }

    override fun loadSamples(activityId: String): List<CoMapsNavigationSnapshot> =
        decodeCoMapsSamples(preferences.getString(activityId.key(), null).orEmpty())

    override fun deleteSamples(activityId: String) {
        if (activityId.isBlank()) return
        preferences.edit { remove(activityId.key()) }
    }

    private fun String.key(): String = "activity_comaps_navigation_$this"

    /** A column read as display text, not cast: a provider may change a column's type. */
    private fun snapshotFrom(row: Map<String, Any?>): CoMapsNavigationSnapshot {
        fun text(column: String): String = when (val value = row[column]) {
            null -> ""
            is String -> value.trim()
            else -> value.toString().trim()
        }

        return CoMapsNavigationSnapshot(
            sampledAt = now(),
            sessionState = text("session_state"),
            currentStreet = text("current_street"),
            nextStreet = text("next_street"),
            distanceToTurn = text("dist_to_turn"),
            distanceToTarget = text("dist_to_target"),
            distanceToNextStop = text("dist_to_next_stop"),
            totalTimeSeconds = row.whole("total_time_seconds"),
            timeToNextStopSeconds = row.whole("time_to_next_stop"),
            completionPercent = row.fraction("completion_percent"),
            carDirection = text("car_direction"),
            pedestrianDirection = text("pedestrian_direction"),
            // `exit_num` is 0 when there is no exit to name.
            exitNumber = when (val exit = row.whole("exit_num")) {
                null, 0 -> ""
                else -> "$exit"
            },
        )
    }

    private fun Map<String, Any?>.whole(column: String): Int? = when (val value = this[column]) {
        is Int -> value
        // The cursor hands over Longs, and a provider may answer a string.
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun Map<String, Any?>.fraction(column: String): Double? = when (val value = this[column]) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun coordinate(latitude: Double?, longitude: Double?): CoMapsCoordinate? =
        if (latitude == null || longitude == null) null else CoMapsCoordinate(latitude, longitude)

    companion object {
        private const val PreferencesName = "openvitals_comaps_navigation"

        /** How long a route stays believable after CoMaps last spoke. A live route refreshes every second. */
        val LivenessWindow: Duration = Duration.ofSeconds(15)
    }
}

/**
 * The samples as stored: one per line, comma-separated, free text
 * Base64-url. Unversioned: a decode failure drops the history, not the activity.
 */
internal fun encodeCoMapsSamples(samples: List<CoMapsNavigationSnapshot>): String =
    samples.joinToString(separator = "\n") { sample ->
        listOf(
            sample.sampledAt.toEpochMilli().toString(),
            sample.sessionState.encodeCompact(),
            sample.currentStreet.encodeCompact(),
            sample.nextStreet.encodeCompact(),
            sample.distanceToTurn.encodeCompact(),
            sample.distanceToTarget.encodeCompact(),
            sample.distanceToNextStop.encodeCompact(),
            sample.totalTimeSeconds?.toString().orEmpty(),
            sample.timeToNextStopSeconds?.toString().orEmpty(),
            sample.completionPercent?.toString().orEmpty(),
            sample.carDirection.encodeCompact(),
            sample.pedestrianDirection.encodeCompact(),
            sample.exitNumber.encodeCompact(),
        ).joinToString(separator = ",")
    }

/** Decodes what [encodeCoMapsSamples] wrote, oldest first. Unparseable lines yield nothing. */
internal fun decodeCoMapsSamples(raw: String): List<CoMapsNavigationSnapshot> =
    raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 13) return@mapNotNull null
            CoMapsNavigationSnapshot(
                sampledAt = parts[0].toLongOrNull()?.let(Instant::ofEpochMilli)
                    ?: return@mapNotNull null,
                sessionState = parts[1].decodeCompact(),
                currentStreet = parts[2].decodeCompact(),
                nextStreet = parts[3].decodeCompact(),
                distanceToTurn = parts[4].decodeCompact(),
                distanceToTarget = parts[5].decodeCompact(),
                distanceToNextStop = parts[6].decodeCompact(),
                totalTimeSeconds = parts[7].toIntOrNull(),
                timeToNextStopSeconds = parts[8].toIntOrNull(),
                completionPercent = parts[9].toDoubleOrNull(),
                carDirection = parts[10].decodeCompact(),
                pedestrianDirection = parts[11].decodeCompact(),
                exitNumber = parts[12].decodeCompact(),
            )
        }
        .sortedBy { it.sampledAt }
        .toList()

private fun String.encodeCompact(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))

private fun String.decodeCompact(): String = try {
    String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
} catch (error: IllegalArgumentException) {
    ""
}
