package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import java.time.LocalDate

/** Days Health Connect serves without the history grant, counted back from a range's end. */
internal const val HistoryReadWindowDays = 30L

/**
 * Where a range read may start: [start], or the last [HistoryReadWindowDays]
 * days ending at [end] when the platform defines the history grant and the
 * user has not given it.
 */
fun HealthConnectManager.historyReadStart(start: LocalDate, end: LocalDate, granted: Set<String>): LocalDate {
    val history = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    val clamped = history in additionalDataAccessPermissions && history !in granted
    return if (clamped) maxOf(start, end.minusDays(HistoryReadWindowDays - 1)) else start
}
