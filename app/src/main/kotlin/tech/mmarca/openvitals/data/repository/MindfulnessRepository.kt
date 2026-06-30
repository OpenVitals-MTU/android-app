package tech.mmarca.openvitals.data.repository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.MindfulnessPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class MindfulnessRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : MindfulnessRepository {

    companion object {
        private const val TAG = "MindfulnessRepository"
    }

    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    private val writeMindfulnessPermission = HealthPermission.getWritePermission(MindfulnessSessionRecord::class)
    override val mindfulnessWritePermissions: Set<String> get() = setOf(writeMindfulnessPermission)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadMindfulnessPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): MindfulnessPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        return coroutineScope {
            val sessions = async { loadMindfulnessSessions(windows.current.start, windows.current.end, granted) }
            val previousSessions = async {
                loadMindfulnessSessions(windows.previous.start, windows.previous.end, granted)
            }
            val baselineSessions = async {
                loadMindfulnessSessions(windows.baseline.start, windows.baseline.end, granted)
            }
            MindfulnessPeriodData(
                sessions = sessions.await(),
                previousSessions = previousSessions.await(),
                baselineSessions = baselineSessions.await(),
            )
        }
    }

    override suspend fun loadMindfulnessSessions(start: LocalDate, end: LocalDate): List<MindfulnessSession> {
        val granted = grantedPermissionsIfAvailable()
        return loadMindfulnessSessions(start, end, granted)
    }

    private suspend fun loadMindfulnessSessions(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<MindfulnessSession> {
        if (readMindfulnessPermission !in granted) {
            Log.w(TAG, "Skipping loadMindfulnessSessions missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readMindfulnessSessions(startInstant, endInstant)
    }

    override fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    override suspend fun hasMindfulnessWritePermission(): Boolean =
        isMindfulnessAvailable() &&
            mindfulnessWritePermissions.all { permission -> permission in grantedPermissionsIfAvailable() }

    override suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequest): String {
        if (!isMindfulnessAvailable()) {
            Log.w(TAG, "Skipping writeMindfulnessSessionEntry because mindfulness sessions are unavailable")
            throw IllegalStateException("Mindfulness sessions are not available from this Health Connect provider.")
        }
        val missingPermissions = mindfulnessWritePermissions - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeMindfulnessSessionEntry missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for mindfulness.")
        }
        return hc.writeMindfulnessSessionEntry(request)
    }

    override suspend fun loadMindfulnessSession(id: String): MindfulnessSession? {
        val granted = grantedPermissionsIfAvailable()
        if (readMindfulnessPermission !in granted) {
            Log.w(TAG, "Skipping loadMindfulnessSession missingCount=1")
            return null
        }
        return hc.readMindfulnessSession(id)
    }

    override suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequest) {
        if (!isMindfulnessAvailable()) {
            Log.w(TAG, "Skipping updateMindfulnessSessionEntry because mindfulness sessions are unavailable")
            throw IllegalStateException("Mindfulness sessions are not available from this Health Connect provider.")
        }
        val missingPermissions = mindfulnessWritePermissions - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateMindfulnessSessionEntry missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for mindfulness.")
        }
        hc.updateMindfulnessSessionEntry(id, request)
    }

    override suspend fun deleteMindfulnessSessionEntry(id: String) {
        if (!isMindfulnessAvailable()) {
            Log.w(TAG, "Skipping deleteMindfulnessSessionEntry because mindfulness sessions are unavailable")
            throw IllegalStateException("Mindfulness sessions are not available from this Health Connect provider.")
        }
        val missingPermissions = mindfulnessWritePermissions - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping deleteMindfulnessSessionEntry missingCount=${missingPermissions.size}")
            throw IllegalStateException("Missing Health Connect write permission for mindfulness.")
        }
        hc.deleteMindfulnessSessionEntry(id)
    }
}
