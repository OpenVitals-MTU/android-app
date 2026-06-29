package tech.mmarca.openvitals.data.repository.contract

import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.BodyPeriodMetric
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.BodyMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.query.BodyPeriodData

interface BodyRepository {
    fun bodyWritePermissions(type: BodyMeasurementType): Set<String>

    suspend fun loadBodyPeriod(
        query: PeriodLoadQuery,
        metric: BodyPeriodMetric,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): BodyPeriodData

    suspend fun loadWeightEntries(start: LocalDate, end: LocalDate): List<WeightEntry>

    suspend fun loadLatestHeight(): Double?

    suspend fun loadHeightEntries(start: LocalDate, end: LocalDate): List<HeightEntry>

    suspend fun loadBodyFatEntries(start: LocalDate, end: LocalDate): List<BodyFatEntry>

    suspend fun loadLatestLeanBodyMass(): Double?

    suspend fun loadLeanBodyMassEntries(start: LocalDate, end: LocalDate): List<LeanBodyMassEntry>

    suspend fun loadLatestBMR(): Double?

    suspend fun loadBmrEntries(start: LocalDate, end: LocalDate): List<BmrEntry>

    suspend fun loadLatestBoneMass(): Double?

    suspend fun loadBoneMassEntries(start: LocalDate, end: LocalDate): List<BoneMassEntry>

    suspend fun loadLatestBodyWaterMass(): Double?

    suspend fun loadBodyWaterMassEntries(start: LocalDate, end: LocalDate): List<BodyWaterMassEntry>

    suspend fun hasBodyWritePermission(type: BodyMeasurementType): Boolean

    suspend fun writeBodyMeasurementEntry(request: BodyMeasurementWriteRequest): String

    suspend fun loadBodyMeasurementEntry(type: BodyMeasurementType, id: String): BodyMeasurementEntry?

    suspend fun updateBodyMeasurementEntry(id: String, request: BodyMeasurementWriteRequest)

    suspend fun deleteBodyMeasurementEntry(type: BodyMeasurementType, id: String)
}
