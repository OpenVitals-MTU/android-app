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

    /**
     * When CoMaps last said something moved, and whether it can say so at all.
     *
     * Held here rather than per screen because it describes the platform
     * feed, not any one view of it — and because the safety poll and the feed
     * have to agree, or they would take turns contradicting each other.
     */
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
     * [stateFrom], plus the one question the row cannot answer: is anyone
     * still driving this route?
     *
     * CoMaps assigns its cached `RoutingInfo` and never nulls it, and the
     * provider answers out of that cache — so a route the user finished an
     * hour ago still comes back complete, `session_state: OnRoute` and all.
     * Reading it is not wrong; believing it is. That is why a finished
     * recording used to hand its route to the NEXT one, instead of offering
     * to plan a new one.
     *
     * So a row only counts as guidance while CoMaps is still saying things
     * about it. Where nothing can say anything — no observer — the read is
     * all there is, and it is believed.
     */
    private fun resolveEvent(event: CoMapsLiveEvent): CoMapsNavigationState {
        observing = event.observing
        // A new watch — a new recording — starts knowing nothing. Without
        // this the clock ran on from the LAST recording, so starting a second
        // one inside the window showed the finished route for the rest of it.
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

    /**
     * Turns the platform's answer into the domain's answer. The unavailable
     * statuses are not failures — they are what the screen shows the user,
     * and each one says something different about what they could do about it.
     */
    private fun stateFrom(answer: CoMapsProviderAnswer): CoMapsNavigationState = when (answer) {
        is CoMapsProviderAnswer.Active -> {
            val snapshot = snapshotFrom(answer.row)
            // A row is not the same thing as live guidance. CoMaps answers
            // out of a cache it never clears, so a finished route keeps being
            // served; the session state says whether anyone is being guided.
            if (!isCoMapsGuiding(snapshot.sessionState)) {
                CoMapsNavigationState.NotNavigating
            } else {
                // Read off the row, never into the snapshot: the snapshot is
                // persisted, and a revision is not history.
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
        // Sub-pixel simplification, once per fetch: the drawn line still bends
        // at every bend the road has, but straight stretches stop costing the
        // renderers a point every segment. Off the caller's thread — the watch
        // collects on Main, and this walks a six-figure buffer.
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

    /**
     * A column read as display text, deliberately NOT a cast. `exit_num`
     * comes straight off `RoutingInfo.exitNum` as an int, and a provider is
     * free to change a column's type — no label on this panel is worth
     * taking the panel down for.
     */
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
            // `exit_num` is 0 whenever there is no exit to name, which is
            // most of any route. "Exit 0" is not a thing.
            exitNumber = when (val exit = row.whole("exit_num")) {
                null, 0 -> ""
                else -> "$exit"
            },
        )
    }

    private fun Map<String, Any?>.whole(column: String): Int? = when (val value = this[column]) {
        is Int -> value
        // The cursor hands over Kotlin Longs, and a provider may answer a
        // formatted string where we expected a number.
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

        /**
         * How long a route stays believable after the last thing CoMaps said
         * about it. Guidance notifies on every location fix, so a live route
         * refreshes this roughly once a second; a finished one never does
         * again.
         */
        val LivenessWindow: Duration = Duration.ofSeconds(15)
    }
}

/**
 * The samples as stored: one per line, comma-separated, free text Base64-url
 * encoded — the same shape the activity markers use. Unversioned by design: if
 * the shape ever changes, a decode failure drops the history for that activity
 * rather than taking the activity down with it.
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

/**
 * Decodes what [encodeCoMapsSamples] wrote, oldest first. Guidance context is
 * a nicety attached to an activity; a corrupt line must never cost the user
 * the activity itself, so anything unparseable simply yields no sample.
 */
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
