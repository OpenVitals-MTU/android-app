package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.MindfulnessSessionWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.MindfulnessPeriodData

interface MindfulnessRepository {
    val mindfulnessWritePermissions: Set<String>

    suspend fun loadMindfulnessPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): MindfulnessPeriodData

    suspend fun loadMindfulnessSessions(start: LocalDate, end: LocalDate): List<MindfulnessSession>

    fun isMindfulnessAvailable(): Boolean

    suspend fun hasMindfulnessWritePermission(): Boolean

    suspend fun writeMindfulnessSessionEntry(request: MindfulnessSessionWriteRequest): String

    suspend fun loadMindfulnessSession(id: String): MindfulnessSession?

    suspend fun updateMindfulnessSessionEntry(id: String, request: MindfulnessSessionWriteRequest)

    suspend fun deleteMindfulnessSessionEntry(id: String)
}
