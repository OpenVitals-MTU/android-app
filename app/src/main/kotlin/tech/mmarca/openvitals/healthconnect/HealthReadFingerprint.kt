package tech.mmarca.openvitals.healthconnect

import java.time.ZoneId
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability

/**
 * Everything a screen's Health Connect result depends on besides its query: the read
 * grants, whether other apps' records are visible now, whether access is paused, and the
 * time zone that cut the days. A screen snapshot is shown only under the one it was read
 * with, so a revoked grant or a background read never stands in for a live answer.
 */
data class HealthReadFingerprint(
    val readGrants: Set<String>,
    val readsOtherAppsData: Boolean,
    val accessEnabled: Boolean,
    val zone: ZoneId,
)

suspend fun HealthConnectManager.readFingerprint(accessEnabled: Boolean): HealthReadFingerprint {
    val available = availability() == HealthConnectAvailability.AVAILABLE
    val granted = if (available) runCatching { grantedPermissions() }.getOrDefault(emptySet()) else emptySet()
    return HealthReadFingerprint(
        readGrants = granted.filterTo(sortedSetOf()) { it.startsWith(ReadPermissionPrefix) },
        readsOtherAppsData = available && runCatching { readsOtherAppsDataNow() }.getOrDefault(false),
        accessEnabled = accessEnabled,
        zone = ZoneId.systemDefault(),
    )
}

private const val ReadPermissionPrefix = "android.permission.health.READ_"
