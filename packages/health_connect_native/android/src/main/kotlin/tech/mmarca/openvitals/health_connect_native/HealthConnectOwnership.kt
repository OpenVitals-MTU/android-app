package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.Record

/**
 * Ported from the native OpenVitals app (`healthconnect/HealthConnectOwnership.kt`).
 *
 * Ownership is by `metadata.dataOrigin.packageName` (NOT clientRecordId): only
 * records this app authored may be edited or deleted. clientRecordIds are used
 * for addressing/dedup, not authorization.
 */
internal fun isOpenVitalsRecord(sourcePackageName: String, appPackageName: String): Boolean =
  sourcePackageName == appPackageName

internal fun Record.requireOpenVitalsOrigin(appPackageName: String) {
  val sourcePackageName = metadata.dataOrigin.packageName
  require(isOpenVitalsRecord(sourcePackageName, appPackageName)) {
    "Only records created by OpenVitals can be edited."
  }
}
