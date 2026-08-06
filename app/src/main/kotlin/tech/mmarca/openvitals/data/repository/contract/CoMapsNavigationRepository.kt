package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

/**
 * What OpenVitals can learn from a CoMaps that is navigating, and the samples
 * it keeps per saved activity.
 *
 * CoMaps plans and navigates; OpenVitals records. Everything here READS what
 * CoMaps is already doing — it cannot start, stop or steer a route. Nothing
 * read from CoMaps is written to Health Connect; the saved samples are
 * app-local activity history.
 */
interface CoMapsNavigationRepository {
    /** One reading of the live row, never throwing — failures become [CoMapsNavigationState.Error]. */
    suspend fun readLive(): CoMapsNavigationState

    /**
     * The live feed: an initial reading, then one per CoMaps `notifyChange`.
     * A row only counts as guidance while CoMaps is still saying things about
     * it — see the implementation's liveness window.
     */
    fun watchLive(): Flow<CoMapsNavigationState>

    /** The followed route's polyline, or null when there is none to read. */
    suspend fun readRouteGeometry(revision: Int): CoMapsRoutePolyline?

    /** The flavour-specific runtime permission to request, null without a CoMaps installed. */
    fun permissionName(): String?

    fun hasPermission(): Boolean

    /** Call after a grant result: the package probe may now answer differently. */
    fun onPermissionChanged()

    fun canLaunchCoMaps(): Boolean

    fun launchForPlanning(latitude: Double?, longitude: Double?): Boolean

    fun saveSamples(activityId: String, samples: List<CoMapsNavigationSnapshot>)

    fun loadSamples(activityId: String): List<CoMapsNavigationSnapshot>

    fun deleteSamples(activityId: String)
}
