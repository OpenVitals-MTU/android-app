package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.data.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalMindfulnessSessionApi::class)
@Singleton
class MindfulnessRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "MindfulnessRepository"
    }

    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    val mindfulnessWritePermissions: Set<String> get() = setOf(writeMindfulnessPermission)
    private val writeMindfulnessPermission = HealthPermission.getWritePermission(MindfulnessSessionRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadMindfulnessPeriod(query: PeriodLoadQuery): MindfulnessPeriodData {
        val windows = query.windows
        return MindfulnessPeriodData(
            sessions = loadMindfulnessSessions(windows.current.start, windows.current.end),
            previousSessions = loadMindfulnessSessions(windows.previous.start, windows.previous.end),
            baselineSessions = loadMindfulnessSessions(windows.baseline.start, windows.baseline.end),
        )
    }

    suspend fun loadMindfulnessSessions(start: LocalDate, end: LocalDate): List<MindfulnessSession> {
        val granted = grantedPermissionsIfAvailable()
        if (readMindfulnessPermission !in granted) {
            Log.w(TAG, "Skipping loadMindfulnessSessions start=$start end=$end missing=$readMindfulnessPermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readMindfulnessSessions(startInstant, endInstant)
    }

    fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    suspend fun hasMindfulnessWritePermission(): Boolean =
        isMindfulnessAvailable() &&
            mindfulnessWritePermissions.all { permission -> permission in grantedPermissionsIfAvailable() }

    suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequest): String {
        if (!isMindfulnessAvailable()) {
            Log.w(TAG, "Skipping writeMindfulnessSessionEntry because mindfulness sessions are unavailable")
            throw IllegalStateException("Mindfulness sessions are not available from this Health Connect provider.")
        }
        val missingPermissions = mindfulnessWritePermissions - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeMindfulnessSessionEntry missing=$missingPermissions")
            throw IllegalStateException("Missing Health Connect write permission for mindfulness.")
        }
        return hc.writeMindfulnessSessionEntry(request)
    }

    suspend fun loadMindfulnessSession(id: String): MindfulnessSession? {
        val granted = grantedPermissionsIfAvailable()
        if (readMindfulnessPermission !in granted) {
            Log.w(TAG, "Skipping loadMindfulnessSession id=$id missing=$readMindfulnessPermission")
            return null
        }
        return hc.readMindfulnessSession(id)
    }

    suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequest) {
        if (!isMindfulnessAvailable()) {
            Log.w(TAG, "Skipping updateMindfulnessSessionEntry because mindfulness sessions are unavailable")
            throw IllegalStateException("Mindfulness sessions are not available from this Health Connect provider.")
        }
        val missingPermissions = mindfulnessWritePermissions - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateMindfulnessSessionEntry id=$id missing=$missingPermissions")
            throw IllegalStateException("Missing Health Connect write permission for mindfulness.")
        }
        hc.updateMindfulnessSessionEntry(id, request)
    }
}

data class MindfulnessPeriodData(
    val sessions: List<MindfulnessSession> = emptyList(),
    val previousSessions: List<MindfulnessSession> = emptyList(),
    val baselineSessions: List<MindfulnessSession> = emptyList(),
)
