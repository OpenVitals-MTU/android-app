package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.HydrationPeriodData

interface HydrationRepository {
    val hydrationWritePermissions: Set<String>

    fun hydrationContainerVolumeMilliliters(): Map<String, Double>

    fun setHydrationContainerVolumeMilliliters(containerId: String, milliliters: Double)

    fun lastCustomHydrationAmountMilliliters(): Double?

    fun setLastCustomHydrationAmountMilliliters(milliliters: Double)

    fun hydrationDailyGoalLiters(): Double

    suspend fun loadHydrationPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): HydrationPeriodData

    suspend fun loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration>

    suspend fun loadHydrationEntries(start: LocalDate, end: LocalDate): List<HydrationEntry>

    suspend fun hasHydrationWritePermission(): Boolean

    suspend fun writeHydrationEntry(request: HydrationWriteRequest): String

    suspend fun loadHydrationEntry(id: String): HydrationEntry?

    suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequest)

    suspend fun deleteHydrationEntry(id: String)
}
