package tech.mmarca.openvitals.data.repository.contract

import kotlinx.coroutines.flow.Flow
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

/**
 * What OpenVitals can learn from a navigating CoMaps, and the samples kept
 * per activity. Read-only; nothing is written to Health Connect.
 */
interface CoMapsNavigationRepository {
    /** One reading of the live row, never throwing — failures become [CoMapsNavigationState.Error]. */
    suspend fun readLive(): CoMapsNavigationState

    /** The live feed: an initial reading, then one per `notifyChange`. */
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
