package tech.mmarca.openvitals.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleDayLogWrite
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.DayBleedingChoice

/**
 * Turns one day log save into the record writes it implies. Each kind is
 * compared with the app's own record of that day and left alone when equal,
 * so an unchanged kind never needs its write permission.
 */
internal class CycleDayLogWriter(
    private val repository: CycleRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) {

    suspend fun write(existing: CycleDayLog, write: CycleDayLogWrite) {
        val date = existing.date
        val flowLevel = (write.bleeding as? DayBleedingChoice.Flow)?.level
        reconcile(
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            ownId = existing.ownFlow?.id,
            ownTime = existing.ownFlow?.time,
            unchanged = existing.ownFlow?.flow == flowLevel,
            desired = flowLevel?.let { CycleEntryWriteRequest(CycleEntryKind.MENSTRUATION_FLOW, defaultTime(date), flow = it) },
        )
        val spotting = write.bleeding is DayBleedingChoice.Spotting
        reconcile(
            kind = CycleEntryKind.SPOTTING,
            ownId = existing.ownSpotting?.id,
            ownTime = existing.ownSpotting?.time,
            unchanged = (existing.ownSpotting != null) == spotting,
            desired = if (spotting) CycleEntryWriteRequest(CycleEntryKind.SPOTTING, defaultTime(date)) else null,
        )
        val bbt = existing.ownBasalBodyTemperature
        val bbtTime = write.basalBodyTemperatureTime?.let { date.atTime(it).atZone(zone).toInstant() }
        reconcile(
            kind = CycleEntryKind.BASAL_BODY_TEMPERATURE,
            ownId = bbt?.id,
            ownTime = bbtTime ?: bbt?.time,
            unchanged = bbt != null && write.basalBodyTemperatureCelsius != null &&
                abs(bbt.temperatureCelsius - write.basalBodyTemperatureCelsius) < TEMPERATURE_EPSILON &&
                bbt.measurementLocation == (write.basalBodyTemperatureLocation ?: CycleRecordValues.MEASUREMENT_LOCATION_UNKNOWN) &&
                (bbtTime == null || bbtTime == bbt.time),
            desired = write.basalBodyTemperatureCelsius?.let {
                CycleEntryWriteRequest(
                    kind = CycleEntryKind.BASAL_BODY_TEMPERATURE,
                    time = bbtTime ?: defaultTime(date),
                    temperatureCelsius = it,
                    measurementLocation = write.basalBodyTemperatureLocation,
                )
            },
        )
        val mucus = existing.ownCervicalMucus
        val appearance = write.mucusAppearance ?: CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN
        val amount = write.mucusAmount ?: CycleRecordValues.MUCUS_SENSATION_UNKNOWN
        reconcile(
            kind = CycleEntryKind.CERVICAL_MUCUS,
            ownId = mucus?.id,
            ownTime = mucus?.time,
            unchanged = if (write.hasCervicalMucus) {
                mucus != null && mucus.appearance == appearance && mucus.sensation == amount
            } else {
                mucus == null
            },
            desired = if (write.hasCervicalMucus) {
                CycleEntryWriteRequest(
                    kind = CycleEntryKind.CERVICAL_MUCUS,
                    time = defaultTime(date),
                    mucusAppearance = appearance,
                    mucusSensation = amount,
                )
            } else {
                null
            },
        )
        reconcile(
            kind = CycleEntryKind.OVULATION_TEST,
            ownId = existing.ownOvulationTest?.id,
            ownTime = existing.ownOvulationTest?.time,
            unchanged = existing.ownOvulationTest?.result == write.ovulationTestResult,
            desired = write.ovulationTestResult?.let {
                CycleEntryWriteRequest(CycleEntryKind.OVULATION_TEST, defaultTime(date), ovulationTestResult = it)
            },
        )
        reconcile(
            kind = CycleEntryKind.SEXUAL_ACTIVITY,
            ownId = existing.ownSexualActivity?.id,
            ownTime = existing.ownSexualActivity?.time,
            unchanged = existing.ownSexualActivity?.protectionUsed == write.sexualActivityProtection,
            desired = write.sexualActivityProtection?.let {
                CycleEntryWriteRequest(CycleEntryKind.SEXUAL_ACTIVITY, defaultTime(date), protectionUsed = it)
            },
        )
    }

    /** Deletes, updates or inserts the app's own record of one kind. */
    private suspend fun reconcile(
        kind: CycleEntryKind,
        ownId: String?,
        ownTime: Instant?,
        unchanged: Boolean,
        desired: CycleEntryWriteRequest?,
    ) {
        if (unchanged) return
        when {
            desired == null -> if (ownId != null) repository.deleteCycleEntry(kind, ownId)
            ownId != null -> repository.updateCycleEntry(ownId, desired.copy(time = ownTime ?: desired.time))
            else -> repository.writeCycleEntry(desired)
        }
    }

    /** Now for today, noon for a past day: the same choice as the entry screen. */
    private fun defaultTime(date: LocalDate): Instant {
        val current = now()
        return if (current.atZone(zone).toLocalDate() == date) current else date.atTime(LocalTime.NOON).atZone(zone).toInstant()
    }

    private companion object {
        const val TEMPERATURE_EPSILON = 0.005
    }
}
