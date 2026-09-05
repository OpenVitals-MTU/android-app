package tech.mmarca.openvitals.data.repository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodWindows
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.sync.HistoryLookbackDays
import tech.mmarca.openvitals.data.sync.VitalsCacheKeys
import tech.mmarca.openvitals.data.sync.VitalsHistorySyncService
import tech.mmarca.openvitals.domain.model.DailyBloodPressurePoint
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.query.VitalsPeriodData
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class VitalsRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
    private val cacheDao: VitalsDailyCacheDao? = null,
    private val vitalsSync: VitalsHistorySyncService? = null,
) : VitalsRepository {

    companion object {
        private const val TAG = "VitalsRepository"

        /** Per-metric budget for the overview's daily reads. */
        private const val VitalsMetricBudgetMillis = 6_000L
    }

    override val phase3Permissions: Set<String> get() = hc.phase3Permissions

    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readRespiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val readBodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)
    private val readBloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val readSkinTemperaturePermission = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val writeBloodPressurePermission = HealthPermission.getWritePermission(BloodPressureRecord::class)
    private val writeSpO2Permission = HealthPermission.getWritePermission(OxygenSaturationRecord::class)
    private val writeRespiratoryRatePermission = HealthPermission.getWritePermission(RespiratoryRateRecord::class)
    private val writeBodyTemperaturePermission = HealthPermission.getWritePermission(BodyTemperatureRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val writeHrvPermission = HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class)

    override fun vitalsWritePermissions(type: VitalsMeasurementType): Set<String> = setOf(
        when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> writeBloodPressurePermission
            VitalsMeasurementType.SPO2 -> writeSpO2Permission
            VitalsMeasurementType.RESPIRATORY_RATE -> writeRespiratoryRatePermission
            VitalsMeasurementType.BODY_TEMPERATURE -> writeBodyTemperaturePermission
            VitalsMeasurementType.HRV -> writeHrvPermission
        }
    )

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    override suspend fun missingPermissions(): Set<String> {
        val granted = grantedPermissionsIfAvailable()
        return phase3Permissions.filterNot { it in granted }.toSet()
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadVitalsPeriod(
        query: PeriodLoadQuery,
        metric: VitalsPeriodMetric,
        refreshMode: RefreshMode,
    ): VitalsPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val missingPermissions = phase3Permissions.filterNot { it in granted }.toSet()
        return coroutineScope {
            when (metric) {
                VitalsPeriodMetric.ALL -> {
                    val current = windows.current
                    if (query.range == TimeRange.DAY) {
                        val bloodPressure = async { loadBloodPressure(current.start, current.end, granted) }
                        val spO2 = async { loadSpO2(current.start, current.end, granted) }
                        val vo2Max = async { loadVo2Max(current.start, current.end, granted) }
                        val respiratoryRate = async { loadRespiratoryRate(current.start, current.end, granted) }
                        val bodyTemperature = async { loadBodyTemperature(current.start, current.end, granted) }
                        val bloodGlucose = async { loadBloodGlucose(current.start, current.end, granted) }
                        val skinTemperature = async { loadSkinTemperature(current.start, current.end, granted) }
                        VitalsPeriodData(
                            missingVitalsPermissions = missingPermissions,
                            bloodPressure = bloodPressure.await(),
                            spO2 = spO2.await(),
                            respiratoryRate = respiratoryRate.await(),
                            bodyTemperature = bodyTemperature.await(),
                            vo2Max = vo2Max.await(),
                            bloodGlucose = bloodGlucose.await(),
                            skinTemperature = skinTemperature.await(),
                        )
                    } else {
                        loadOverviewDailyVitals(current.start, current.end, granted, missingPermissions)
                    }
                }
                VitalsPeriodMetric.BLOOD_PRESSURE -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadBloodPressure(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        bloodPressure = entries.current,
                        previousBloodPressure = entries.previous,
                        baselineBloodPressure = entries.baseline,
                    )
                }
                VitalsPeriodMetric.SPO2 -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadSpO2(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        spO2 = entries.current,
                        previousSpO2 = entries.previous,
                        baselineSpO2 = entries.baseline,
                    )
                }
                VitalsPeriodMetric.VO2_MAX -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadVo2Max(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        vo2Max = entries.current,
                        previousVo2Max = entries.previous,
                        baselineVo2Max = entries.baseline,
                    )
                }
                VitalsPeriodMetric.RESPIRATORY_RATE -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadRespiratoryRate(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        respiratoryRate = entries.current,
                        previousRespiratoryRate = entries.previous,
                        baselineRespiratoryRate = entries.baseline,
                    )
                }
                VitalsPeriodMetric.BODY_TEMPERATURE -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadBodyTemperature(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        bodyTemperature = entries.current,
                        previousBodyTemperature = entries.previous,
                        baselineBodyTemperature = entries.baseline,
                    )
                }
                VitalsPeriodMetric.BLOOD_GLUCOSE -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadBloodGlucose(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        bloodGlucose = entries.current,
                        previousBloodGlucose = entries.previous,
                        baselineBloodGlucose = entries.baseline,
                    )
                }
                VitalsPeriodMetric.SKIN_TEMPERATURE -> {
                    val entries = loadPeriodTriplet(windows) { start, end -> loadSkinTemperature(start, end, granted) }
                    VitalsPeriodData(
                        missingVitalsPermissions = missingPermissions,
                        skinTemperature = entries.current,
                        previousSkinTemperature = entries.previous,
                        baselineSkinTemperature = entries.baseline,
                    )
                }
            }
        }
    }

    /**
     * The overview load: one point per local day per metric, plus each
     * metric's latest reading. Every daily read has its own budget, so a
     * dense metric costs its own card, not the screen.
     */
    private suspend fun loadOverviewDailyVitals(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
        missingPermissions: Set<String>,
    ): VitalsPeriodData = coroutineScope {
        val startInstant = start.toInstant()
        val endInstant = end.plusDays(1).toInstant()
        val timedOut = ConcurrentHashMap.newKeySet<VitalsPeriodMetric>()

        suspend fun <T> budgeted(
            metric: VitalsPeriodMetric,
            permission: String,
            available: Boolean = true,
            read: suspend (Instant, Instant) -> List<T>,
        ): List<T> {
            if (!available || permission !in granted) return emptyList()
            return withTimeoutOrNull(VitalsMetricBudgetMillis) { read(startInstant, endInstant) }
                ?: run {
                    Log.w(TAG, "Vitals daily read for $metric blew its ${VitalsMetricBudgetMillis}ms budget")
                    timedOut.add(metric)
                    emptyList()
                }
        }

        suspend fun <T> latest(permission: String, available: Boolean = true, read: suspend (Instant, Instant) -> T?): T? =
            if (available && permission in granted) read(startInstant, endInstant) else null

        val skinTemperatureAvailable = hc.isSkinTemperatureAvailable()
        val bloodPressureDaily = async {
            budgeted(VitalsPeriodMetric.BLOOD_PRESSURE, readBloodPressurePermission) { _, _ ->
                dailyBloodPressureCore(start, end)
            }
        }
        val spO2Daily = async {
            budgeted(VitalsPeriodMetric.SPO2, readSpO2Permission) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.SPO2, start, end)
            }
        }
        val respiratoryRateDaily = async {
            budgeted(VitalsPeriodMetric.RESPIRATORY_RATE, readRespiratoryRatePermission) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.RESPIRATORY_RATE, start, end)
            }
        }
        val bodyTemperatureDaily = async {
            budgeted(VitalsPeriodMetric.BODY_TEMPERATURE, readBodyTemperaturePermission) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.BODY_TEMPERATURE, start, end)
            }
        }
        val vo2MaxDaily = async {
            budgeted(VitalsPeriodMetric.VO2_MAX, readVo2MaxPermission) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.VO2_MAX, start, end)
            }
        }
        val bloodGlucoseDaily = async {
            budgeted(VitalsPeriodMetric.BLOOD_GLUCOSE, readBloodGlucosePermission) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.BLOOD_GLUCOSE, start, end)
            }
        }
        val skinTemperatureDaily = async {
            budgeted(VitalsPeriodMetric.SKIN_TEMPERATURE, readSkinTemperaturePermission, skinTemperatureAvailable) { _, _ ->
                dailyVitalsCore(VitalsPeriodMetric.SKIN_TEMPERATURE, start, end)
            }
        }
        val latestBloodPressure = async { latest(readBloodPressurePermission, read = hc::readLatestBloodPressureInWindow) }
        val latestSpO2 = async { latest(readSpO2Permission, read = hc::readLatestSpO2InWindow) }
        val latestRespiratoryRate = async { latest(readRespiratoryRatePermission, read = hc::readLatestRespiratoryRateInWindow) }
        val latestBodyTemperature = async { latest(readBodyTemperaturePermission, read = hc::readLatestBodyTemperatureInWindow) }
        val latestVo2Max = async { latest(readVo2MaxPermission, read = hc::readLatestVo2MaxInWindow) }
        val latestBloodGlucose = async { latest(readBloodGlucosePermission, read = hc::readLatestBloodGlucoseInWindow) }
        val latestSkinTemperature = async {
            latest(readSkinTemperaturePermission, skinTemperatureAvailable, hc::readLatestSkinTemperatureInWindow)
        }

        VitalsPeriodData(
            missingVitalsPermissions = missingPermissions,
            bloodPressureDaily = bloodPressureDaily.await(),
            spO2Daily = spO2Daily.await(),
            respiratoryRateDaily = respiratoryRateDaily.await(),
            bodyTemperatureDaily = bodyTemperatureDaily.await(),
            vo2MaxDaily = vo2MaxDaily.await(),
            bloodGlucoseDaily = bloodGlucoseDaily.await(),
            skinTemperatureDaily = skinTemperatureDaily.await(),
            latestBloodPressure = latestBloodPressure.await(),
            latestSpO2 = latestSpO2.await(),
            latestRespiratoryRate = latestRespiratoryRate.await(),
            latestBodyTemperature = latestBodyTemperature.await(),
            latestVo2Max = latestVo2Max.await(),
            latestBloodGlucose = latestBloodGlucose.await(),
            latestSkinTemperature = latestSkinTemperature.await(),
            timedOutMetrics = timedOut.toSet(),
        )
    }

    /** The cache-first daily read for one metric, behind the overview and [loadDailyVitals]. */
    private suspend fun dailyVitalsCore(
        metric: VitalsPeriodMetric,
        start: LocalDate,
        end: LocalDate,
    ): List<DailyVitalPoint> {
        val startInstant = start.toInstant()
        val endInstant = end.plusDays(1).toInstant()
        return when (metric) {
            VitalsPeriodMetric.SPO2 ->
                cachedDaily(VitalsCacheKeys.SPO2, start, end) ?: hc.readDailySpO2(startInstant, endInstant)
            VitalsPeriodMetric.RESPIRATORY_RATE ->
                cachedDaily(VitalsCacheKeys.RESPIRATORY_RATE, start, end)
                    ?: hc.readDailyRespiratoryRate(startInstant, endInstant)
            VitalsPeriodMetric.BODY_TEMPERATURE ->
                cachedDaily(VitalsCacheKeys.BODY_TEMPERATURE, start, end)
                    ?: hc.readDailyBodyTemperature(startInstant, endInstant)
            VitalsPeriodMetric.VO2_MAX ->
                cachedDaily(VitalsCacheKeys.VO2_MAX, start, end) ?: hc.readDailyVo2Max(startInstant, endInstant)
            VitalsPeriodMetric.BLOOD_GLUCOSE ->
                cachedDaily(VitalsCacheKeys.BLOOD_GLUCOSE, start, end)
                    ?: hc.readDailyBloodGlucose(startInstant, endInstant)
            VitalsPeriodMetric.SKIN_TEMPERATURE ->
                cachedDaily(VitalsCacheKeys.SKIN_TEMPERATURE, start, end)
                    ?: hc.readDailySkinTemperature(startInstant, endInstant)
            VitalsPeriodMetric.ALL,
            VitalsPeriodMetric.BLOOD_PRESSURE,
            -> throw IllegalArgumentException("dailyVitalsCore cannot serve $metric")
        }
    }

    private suspend fun dailyBloodPressureCore(start: LocalDate, end: LocalDate): List<DailyBloodPressurePoint> =
        cachedDailyBloodPressure(start, end)
            ?: hc.readDailyBloodPressure(start.toInstant(), end.plusDays(1).toInstant())

    override suspend fun loadDailyVitals(
        metric: VitalsPeriodMetric,
        start: LocalDate,
        end: LocalDate,
    ): List<DailyVitalPoint> {
        require(metric != VitalsPeriodMetric.ALL && metric != VitalsPeriodMetric.BLOOD_PRESSURE) {
            "loadDailyVitals cannot serve $metric"
        }
        val permission = when (metric) {
            VitalsPeriodMetric.SPO2 -> readSpO2Permission
            VitalsPeriodMetric.RESPIRATORY_RATE -> readRespiratoryRatePermission
            VitalsPeriodMetric.BODY_TEMPERATURE -> readBodyTemperaturePermission
            VitalsPeriodMetric.VO2_MAX -> readVo2MaxPermission
            VitalsPeriodMetric.BLOOD_GLUCOSE -> readBloodGlucosePermission
            VitalsPeriodMetric.SKIN_TEMPERATURE -> readSkinTemperaturePermission
            VitalsPeriodMetric.ALL, VitalsPeriodMetric.BLOOD_PRESSURE -> error("unreachable")
        }
        if (permission !in grantedPermissionsIfAvailable()) {
            Log.w(TAG, "Skipping loadDailyVitals metric=$metric missingCount=1")
            return emptyList()
        }
        if (metric == VitalsPeriodMetric.SKIN_TEMPERATURE && !hc.isSkinTemperatureAvailable()) {
            Log.w(TAG, "Skipping loadDailyVitals metric=$metric: provider lacks skin temperature")
            return emptyList()
        }
        return dailyVitalsCore(metric, start, end)
    }

    override suspend fun loadDailyBloodPressure(start: LocalDate, end: LocalDate): List<DailyBloodPressurePoint> {
        if (readBloodPressurePermission !in grantedPermissionsIfAvailable()) {
            Log.w(TAG, "Skipping loadDailyBloodPressure missingCount=1")
            return emptyList()
        }
        return dailyBloodPressureCore(start, end)
    }

    /**
     * The cached daily points, or null to fall through to the live read.
     * Cursor presence is the whole freshness model. Ranges before the
     * lookback window fall through too.
     */
    private suspend fun cachedDaily(key: String, start: LocalDate, end: LocalDate): List<DailyVitalPoint>? {
        val dao = cacheDao ?: return null
        if (start.isBefore(LocalDate.now().minusDays(HistoryLookbackDays))) return null
        dao.cursor(key) ?: return null
        return dao.aggregatesBetween(key, start.toEpochDay(), end.toEpochDay()).map { row ->
            DailyVitalPoint(
                date = LocalDate.ofEpochDay(row.epochDay),
                value = row.valueSum / row.sampleCount,
                count = row.sampleCount.toInt(),
            )
        }
    }

    private suspend fun cachedDailyBloodPressure(start: LocalDate, end: LocalDate): List<DailyBloodPressurePoint>? {
        val dao = cacheDao ?: return null
        if (start.isBefore(LocalDate.now().minusDays(HistoryLookbackDays))) return null
        dao.cursor(VitalsCacheKeys.BLOOD_PRESSURE) ?: return null
        return dao.aggregatesBetween(VitalsCacheKeys.BLOOD_PRESSURE, start.toEpochDay(), end.toEpochDay())
            .map { row ->
                DailyBloodPressurePoint(
                    date = LocalDate.ofEpochDay(row.epochDay),
                    systolic = row.valueSum / row.sampleCount,
                    diastolic = (row.secondarySum ?: 0.0) / row.sampleCount,
                    count = row.sampleCount.toInt(),
                )
            }
    }

    private fun VitalsMeasurementType.cacheKey(): String = when (this) {
        VitalsMeasurementType.BLOOD_PRESSURE -> VitalsCacheKeys.BLOOD_PRESSURE
        VitalsMeasurementType.SPO2 -> VitalsCacheKeys.SPO2
        VitalsMeasurementType.RESPIRATORY_RATE -> VitalsCacheKeys.RESPIRATORY_RATE
        VitalsMeasurementType.BODY_TEMPERATURE -> VitalsCacheKeys.BODY_TEMPERATURE
        // HRV has no daily cache; patchDays is a no-op for a spec-less key.
        VitalsMeasurementType.HRV -> VitalsCacheKeys.HRV
    }

    /** The entry's local day, read only when a cache exists to patch. */
    private suspend fun dayOfEntry(type: VitalsMeasurementType, id: String): LocalDate? {
        if (cacheDao == null || vitalsSync == null) return null
        return runCatching { hc.readVitalsMeasurementEntry(type, id)?.time }
            .getOrNull()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()
    }

    private suspend fun patchCachedDays(type: VitalsMeasurementType, days: Set<LocalDate>) {
        vitalsSync?.patchDays(type.cacheKey(), days)
    }

    private suspend fun <T> loadPeriodTriplet(
        windows: PeriodWindows,
        loader: suspend (LocalDate, LocalDate) -> List<T>,
    ): VitalsPeriodTriplet<T> = coroutineScope {
        val current = async { loader(windows.current.start, windows.current.end) }
        val previous = async { loader(windows.previous.start, windows.previous.end) }
        val baseline = async { loader(windows.baseline.start, windows.baseline.end) }
        VitalsPeriodTriplet(
            current = current.await(),
            previous = previous.await(),
            baseline = baseline.await(),
        )
    }

    override suspend fun loadBloodPressure(start: LocalDate, end: LocalDate): List<BloodPressureEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBloodPressure(start, end, granted)
    }

    private suspend fun loadBloodPressure(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BloodPressureEntry> {
        if (readBloodPressurePermission !in granted) {
            Log.w(TAG, "Skipping loadBloodPressure missingCount=1")
            return emptyList()
        }
        return hc.readBloodPressureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadSpO2(start: LocalDate, end: LocalDate): List<SpO2Entry> {
        val granted = grantedPermissionsIfAvailable()
        return loadSpO2(start, end, granted)
    }

    private suspend fun loadSpO2(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<SpO2Entry> {
        if (readSpO2Permission !in granted) {
            Log.w(TAG, "Skipping loadSpO2 missingCount=1")
            return emptyList()
        }
        return hc.readSpO2Entries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadRespiratoryRate(start: LocalDate, end: LocalDate): List<RespiratoryRateEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadRespiratoryRate(start, end, granted)
    }

    private suspend fun loadRespiratoryRate(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<RespiratoryRateEntry> {
        if (readRespiratoryRatePermission !in granted) {
            Log.w(TAG, "Skipping loadRespiratoryRate missingCount=1")
            return emptyList()
        }
        return hc.readRespiratoryRateEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadBodyTemperature(start: LocalDate, end: LocalDate): List<BodyTempEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBodyTemperature(start, end, granted)
    }

    private suspend fun loadBodyTemperature(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BodyTempEntry> {
        if (readBodyTemperaturePermission !in granted) {
            Log.w(TAG, "Skipping loadBodyTemperature missingCount=1")
            return emptyList()
        }
        return hc.readBodyTemperatureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadVo2Max(start: LocalDate, end: LocalDate): List<Vo2MaxEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadVo2Max(start, end, granted)
    }

    private suspend fun loadVo2Max(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<Vo2MaxEntry> {
        if (readVo2MaxPermission !in granted) {
            Log.w(TAG, "Skipping loadVo2Max missingCount=1")
            return emptyList()
        }
        return hc.readVo2MaxEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadBloodGlucose(start: LocalDate, end: LocalDate): List<BloodGlucoseEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadBloodGlucose(start, end, granted)
    }

    private suspend fun loadBloodGlucose(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<BloodGlucoseEntry> {
        if (readBloodGlucosePermission !in granted) {
            Log.w(TAG, "Skipping loadBloodGlucose missingCount=1")
            return emptyList()
        }
        return hc.readBloodGlucoseEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun loadSkinTemperature(start: LocalDate, end: LocalDate): List<SkinTemperatureEntry> {
        val granted = grantedPermissionsIfAvailable()
        return loadSkinTemperature(start, end, granted)
    }

    private suspend fun loadSkinTemperature(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<SkinTemperatureEntry> {
        if (!hc.isSkinTemperatureAvailable() || readSkinTemperaturePermission !in granted) {
            Log.w(TAG, "Skipping loadSkinTemperature missingCount=1")
            return emptyList()
        }
        return hc.readSkinTemperatureEntries(start.toInstant(), end.plusDays(1).toInstant())
    }

    override suspend fun hasVitalsWritePermission(type: VitalsMeasurementType): Boolean =
        vitalsWritePermissions(type).all { permission -> permission in grantedPermissionsIfAvailable() }

    override suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String {
        val missingPermissions = vitalsWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeVitalsMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect write permission for ${request.type}")
        }
        val id = hc.writeVitalsMeasurementEntry(request)
        patchCachedDays(request.type, setOf(request.time.atZone(ZoneId.systemDefault()).toLocalDate()))
        return id
    }

    override suspend fun loadVitalsMeasurementEntry(type: VitalsMeasurementType, id: String): VitalsMeasurementEntry? {
        val readPermission = when (type) {
            VitalsMeasurementType.BLOOD_PRESSURE -> readBloodPressurePermission
            VitalsMeasurementType.SPO2 -> readSpO2Permission
            VitalsMeasurementType.RESPIRATORY_RATE -> readRespiratoryRatePermission
            VitalsMeasurementType.BODY_TEMPERATURE -> readBodyTemperaturePermission
            VitalsMeasurementType.HRV -> readHrvPermission
        }
        val granted = grantedPermissionsIfAvailable()
        if (readPermission !in granted) {
            Log.w(TAG, "Skipping loadVitalsMeasurementEntry type=$type missingCount=1")
            return null
        }
        return hc.readVitalsMeasurementEntry(type, id)
    }

    override suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequest) {
        val missingPermissions = vitalsWritePermissions(request.type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateVitalsMeasurementEntry type=${request.type} missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect write permission for ${request.type}")
        }
        // The pre-edit day is captured first, so a move across midnight patches both days.
        val oldDay = dayOfEntry(request.type, id)
        hc.updateVitalsMeasurementEntry(id, request)
        val newDay = request.time.atZone(ZoneId.systemDefault()).toLocalDate()
        patchCachedDays(request.type, setOfNotNull(newDay, oldDay))
    }

    override suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, id: String) {
        val missingPermissions = vitalsWritePermissions(type) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping deleteVitalsMeasurementEntry type=$type missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect write permission for $type")
        }
        val day = dayOfEntry(type, id)
        hc.deleteVitalsMeasurementEntry(type, id)
        if (day != null) patchCachedDays(type, setOf(day))
    }

    private fun LocalDate.toInstant() = atStartOfDay(ZoneId.systemDefault()).toInstant()
}

private data class VitalsPeriodTriplet<T>(
    val current: List<T>,
    val previous: List<T>,
    val baseline: List<T>,
)

enum class VitalsPeriodMetric {
    ALL,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
}
