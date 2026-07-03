package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.beverage.BeverageStore
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.CaffeineSourceCategory
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.HydrationPeriodData
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class HydrationRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
    private val preferencesRepository: PreferencesRepository? = null,
    private val beverageStore: BeverageStore? = null,
) : HydrationRepository {

    companion object {
        private const val TAG = "HydrationRepository"
        private const val DefaultHydrationDailyGoalLiters = 2.0
    }

    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val writeHydrationPermission = HealthPermission.getWritePermission(HydrationRecord::class)
    private val writeNutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)
    override val hydrationWritePermissions: Set<String> get() = setOf(writeHydrationPermission)

    override fun hydrationContainerVolumeMilliliters(): Map<String, Double> =
        preferencesRepository?.hydrationContainerVolumeMilliliters().orEmpty()

    override fun setHydrationContainerVolumeMilliliters(containerId: String, milliliters: Double) {
        preferencesRepository?.setHydrationContainerVolumeMilliliters(containerId, milliliters)
    }

    override fun lastCustomHydrationAmountMilliliters(): Double? =
        preferencesRepository?.lastCustomHydrationAmountMilliliters()

    override fun setLastCustomHydrationAmountMilliliters(milliliters: Double) {
        preferencesRepository?.setLastCustomHydrationAmountMilliliters(milliliters)
    }

    override fun customHydrationDrinks(): List<CustomHydrationDrink> =
        beverageStore?.beverages()
            ?: preferencesRepository?.customHydrationDrinks().orEmpty()

    override fun saveCustomHydrationDrink(drink: CustomHydrationDrink) {
        beverageStore?.save(drink) ?: preferencesRepository?.saveCustomHydrationDrink(drink)
    }

    override fun deleteCustomHydrationDrink(drinkId: String) {
        beverageStore?.delete(drinkId) ?: preferencesRepository?.deleteCustomHydrationDrink(drinkId)
    }

    override fun reorderCustomHydrationDrinks(drinkIds: List<String>) {
        beverageStore?.reorder(drinkIds) ?: preferencesRepository?.reorderCustomHydrationDrinks(drinkIds)
    }

    override fun moveCustomHydrationDrinkToCategory(
        drinkId: String,
        category: CaffeineSourceCategory?,
    ) {
        beverageStore?.moveToCategory(drinkId, category)
    }

    override fun hydrationDailyGoalLiters(): Double =
        preferencesRepository?.hydrationDailyGoalLiters
            ?.takeIf { it > 0.0 && it.isFinite() }
            ?: DefaultHydrationDailyGoalLiters

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadHydrationPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): HydrationPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        return coroutineScope {
            val dailyHydration = async {
                if (query.range == TimeRange.DAY) {
                    emptyList()
                } else {
                    loadDailyHydration(windows.current.start, windows.current.end, granted)
                }
            }
            val previousDailyHydration = async {
                loadDailyHydration(windows.previous.start, windows.previous.end, granted)
            }
            val baselineDailyHydration = async {
                loadDailyHydration(windows.baseline.start, windows.baseline.end, granted)
            }
            val hydrationEntries = async { loadHydrationEntries(windows.current.start, windows.current.end, granted) }
            val currentEntries = hydrationEntries.await()
            HydrationPeriodData(
                dailyHydration = if (query.range == TimeRange.DAY) {
                    currentEntries.toDailyHydrationForDay(query.selectedDate)
                } else {
                    dailyHydration.await()
                },
                previousDailyHydration = previousDailyHydration.await(),
                baselineDailyHydration = baselineDailyHydration.await(),
                hydrationEntries = currentEntries,
            )
        }
    }

    override suspend fun loadDailyHydration(start: LocalDate, end: LocalDate): List<DailyHydration> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailyHydration(start, end, granted)
    }

    private suspend fun loadDailyHydration(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyHydration> {
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyHydration missingCount=1")
            return emptyList()
        }
        return hc.readDailyHydration(start, end)
    }

    override suspend fun loadHydrationEntries(start: LocalDate, end: LocalDate): List<HydrationEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadHydrationEntries(start, end, granted)
    }

    private suspend fun loadHydrationEntries(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<HydrationEntry> {
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadHydrationEntries missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        return hc.readHydrationEntries(
            start = start.atStartOfDay(zone).toInstant(),
            end = end.plusDays(1).atStartOfDay(zone).toInstant(),
        )
    }

    override suspend fun hasHydrationWritePermission(): Boolean =
        writeHydrationPermission in grantedPermissionsIfAvailable()

    private fun List<HydrationEntry>.toDailyHydrationForDay(date: LocalDate): List<DailyHydration> {
        val zone = ZoneId.systemDefault()
        val liters = filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.liters }
        return if (liters > 0.0) {
            listOf(DailyHydration(date = date, liters = liters))
        } else {
            emptyList()
        }
    }

    override suspend fun writeHydrationEntry(request: HydrationWriteRequest): String {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping writeHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        return hc.writeHydrationEntry(request)
    }

    override suspend fun loadHydrationEntry(id: String): HydrationEntry? {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted) {
            Log.w(TAG, "Skipping loadHydrationEntry missingCount=1")
            return null
        }
        return hc.readHydrationEntry(id)
    }

    override suspend fun updateHydrationEntry(id: String, request: HydrationWriteRequest) {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping updateHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        hc.updateHydrationEntry(id, request)
    }

    override suspend fun deleteHydrationEntry(id: String) {
        val granted = grantedPermissionsIfAvailable()
        if (writeHydrationPermission !in granted) {
            Log.w(TAG, "Skipping deleteHydrationEntry missingCount=1")
            throw SecurityException("Missing Health Connect hydration write permission.")
        }
        val hydrationClientRecordId = hc.deleteHydrationEntry(id)
        if (hydrationClientRecordId != null && writeNutritionPermission in granted) {
            runCatching {
                hc.deleteHydrationNutritionEntry(hydrationClientRecordId)
            }.onFailure { error ->
                Log.w(TAG, "Could not delete paired nutrition record.", error)
            }
        }
    }
}
