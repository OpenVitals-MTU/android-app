package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.records.Record
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

@Singleton
class AppleHealthImportRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val dispatchers: DispatcherProvider,
) {
    fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    suspend fun insertImportedRecords(records: List<Record>) =
        withContext(dispatchers.io) {
            hc.insertImportedRecords(records)
        }

    suspend fun findMatchingImportedClientRecordIds(
        recordType: KClass<out Record>,
        start: Instant,
        end: Instant,
        wantedIds: Set<String>,
    ): Set<String> =
        withContext(dispatchers.io) {
            hc.findMatchingImportedClientRecordIds(recordType, start, end, wantedIds)
        }
}
