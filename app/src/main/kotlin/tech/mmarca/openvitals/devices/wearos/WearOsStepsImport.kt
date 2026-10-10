package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant

/**
 * Maps the watch's step minutes onto Health Connect records: one
 * `StepsRecord` per minute, keyed on the minute's start. A minute's count only
 * grows while late steps arrive, so the count is also the record's version: a
 * re-pull of a minute that grew replaces it, one that did not changes nothing.
 *
 * Pure: no Android, so it is testable without a device.
 */
object WearOsStepsImport {

    const val CLIENT_ID_PREFIX = "wearos_steps_"

    fun records(minutes: List<WearOsStepMinute>): List<Record> =
        minutes
            .groupBy { it.start }
            .map { (start, same) ->
                val steps = same.maxOf { it.steps }
                StepsRecord(
                    startTime = start,
                    startZoneOffset = null,
                    endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                    endZoneOffset = null,
                    count = steps.toLong(),
                    metadata = Metadata.autoRecorded(
                        device = Device(type = Device.TYPE_WATCH),
                        clientRecordId = clientRecordId(start),
                        clientRecordVersion = steps.toLong(),
                    ),
                )
            }

    fun clientRecordId(minuteStart: Instant): String = "$CLIENT_ID_PREFIX${minuteStart.toEpochMilli()}"

    private const val SECONDS_PER_MINUTE = 60L
}
