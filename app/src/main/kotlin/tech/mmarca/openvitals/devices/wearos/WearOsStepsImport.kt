package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import java.time.Instant
import tech.mmarca.openvitals.domain.insights.WalkingEnergy

/**
 * Maps the watch's step minutes to Health Connect records, one per minute
 * and type, keyed on the minute's start:
 *
 * - `StepsRecord`: the watch's count.
 * - `StepsCadenceRecord`: the same count as steps per minute.
 * - `ActiveCaloriesBurnedRecord`: from the cadence by [WalkingEnergy], only
 *   when the body weight is known.
 *
 * A minute's count only grows, so the count is also every record's version:
 * a minute that grew replaces its records, an unchanged one does nothing.
 *
 * Pure, so it is testable without a device.
 */
object WearOsStepsImport {

    const val CLIENT_ID_PREFIX = "wearos_steps_"
    const val CADENCE_CLIENT_ID_PREFIX = "wearos_cadence_"
    const val ACTIVE_CALORIES_CLIENT_ID_PREFIX = "wearos_walk_kcal_"

    fun records(minutes: List<WearOsStepMinute>): List<Record> =
        latestPerMinute(minutes).map { minute ->
            StepsRecord(
                startTime = minute.start,
                startZoneOffset = null,
                endTime = minute.end,
                endZoneOffset = null,
                count = minute.steps.toLong(),
                metadata = watchMetadata(clientRecordId(minute.start), minute.steps),
            )
        }

    fun cadenceRecords(minutes: List<WearOsStepMinute>): List<Record> =
        latestPerMinute(minutes).map { minute ->
            StepsCadenceRecord(
                startTime = minute.start,
                startZoneOffset = null,
                endTime = minute.end,
                endZoneOffset = null,
                samples = listOf(StepsCadenceRecord.Sample(time = minute.start, rate = minute.steps.toDouble())),
                metadata = watchMetadata("$CADENCE_CLIENT_ID_PREFIX${minute.start.toEpochMilli()}", minute.steps),
            )
        }

    /** Empty unless [weightKg] is known. A minute too slow to cost more than rest writes nothing. */
    fun activeCaloriesRecords(minutes: List<WearOsStepMinute>, weightKg: Double?): List<Record> {
        if (weightKg == null || weightKg <= 0.0) return emptyList()
        return latestPerMinute(minutes).mapNotNull { minute ->
            val kcal = WalkingEnergy.activeKcalPerMinute(minute.steps, weightKg)
            if (kcal <= 0.0) return@mapNotNull null
            ActiveCaloriesBurnedRecord(
                startTime = minute.start,
                startZoneOffset = null,
                endTime = minute.end,
                endZoneOffset = null,
                energy = Energy.kilocalories(kcal),
                // Computed on the phone, like the other OpenVitals estimates.
                metadata = Metadata.manualEntry(
                    device = Device(type = Device.TYPE_PHONE),
                    clientRecordId = "$ACTIVE_CALORIES_CLIENT_ID_PREFIX${minute.start.toEpochMilli()}",
                    clientRecordVersion = minute.steps.toLong(),
                ),
            )
        }
    }

    fun clientRecordId(minuteStart: Instant): String = "$CLIENT_ID_PREFIX${minuteStart.toEpochMilli()}"

    /** A minute twice in one page keeps the larger, later count. */
    private fun latestPerMinute(minutes: List<WearOsStepMinute>): List<WearOsStepMinute> =
        minutes.groupBy { it.start }.map { (_, same) -> same.maxBy { it.steps } }

    private fun watchMetadata(clientRecordId: String, steps: Int): Metadata =
        Metadata.autoRecorded(
            device = Device(type = Device.TYPE_WATCH),
            clientRecordId = clientRecordId,
            clientRecordVersion = steps.toLong(),
        )

    private val WearOsStepMinute.end: Instant get() = start.plusSeconds(SECONDS_PER_MINUTE)

    private const val SECONDS_PER_MINUTE = 60L
}
