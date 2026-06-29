package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.query.VitalsPeriodData

interface VitalsRepository {
    val phase3Permissions: Set<String>

    fun vitalsWritePermissions(type: VitalsMeasurementType): Set<String>

    suspend fun missingPermissions(): Set<String>

    suspend fun loadVitalsPeriod(
        query: PeriodLoadQuery,
        metric: VitalsPeriodMetric,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): VitalsPeriodData

    suspend fun loadBloodPressure(start: LocalDate, end: LocalDate): List<BloodPressureEntry>

    suspend fun loadSpO2(start: LocalDate, end: LocalDate): List<SpO2Entry>

    suspend fun loadRespiratoryRate(start: LocalDate, end: LocalDate): List<RespiratoryRateEntry>

    suspend fun loadBodyTemperature(start: LocalDate, end: LocalDate): List<BodyTempEntry>

    suspend fun loadVo2Max(start: LocalDate, end: LocalDate): List<Vo2MaxEntry>

    suspend fun loadBloodGlucose(start: LocalDate, end: LocalDate): List<BloodGlucoseEntry>

    suspend fun loadSkinTemperature(start: LocalDate, end: LocalDate): List<SkinTemperatureEntry>

    suspend fun hasVitalsWritePermission(type: VitalsMeasurementType): Boolean

    suspend fun writeVitalsMeasurementEntry(request: VitalsMeasurementWriteRequest): String

    suspend fun loadVitalsMeasurementEntry(type: VitalsMeasurementType, id: String): VitalsMeasurementEntry?

    suspend fun updateVitalsMeasurementEntry(id: String, request: VitalsMeasurementWriteRequest)

    suspend fun deleteVitalsMeasurementEntry(type: VitalsMeasurementType, id: String)
}
