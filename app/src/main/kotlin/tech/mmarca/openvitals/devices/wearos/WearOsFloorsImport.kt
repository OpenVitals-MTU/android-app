package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant

/**
 * Maps the watch's floor minutes to `FloorsClimbedRecord`s, one per minute,
 * keyed on the minute's start.
 *
 * The watch counts every 3 m of ascent, an elevator ride and a drift of the
 * weather too. A floor counts only with at least [MIN_STEPS_PER_FLOOR] steps
 * in that minute and the one before; stairs take about 16 per floor. The
 * threshold is an app heuristic, no published source.
 *
 * The credited count is the record's version, so a minute whose steps or
 * floors grew replaces its record.
 *
 * Pure, so it is testable without a device.
 */
object WearOsFloorsImport {

    const val CLIENT_ID_PREFIX = "wearos_floors_"

    const val MIN_STEPS_PER_FLOOR = 10

    /** Records for [floors], given the [steps] pulled in the same sync. Minutes without enough steps write nothing. */
    fun records(floors: List<WearOsFloorMinute>, steps: List<WearOsStepMinute>): List<Record> {
        val stepsAt = HashMap<Instant, Int>()
        for (minute in steps) stepsAt.merge(minute.start, minute.steps, ::maxOf)
        return floors.groupBy { it.start }.mapNotNull { (start, same) ->
            val climbed = same.maxOf { it.floors }
            val walked = (stepsAt[start.minusSeconds(SECONDS_PER_MINUTE)] ?: 0) + (stepsAt[start] ?: 0)
            val credited = minOf(climbed, walked / MIN_STEPS_PER_FLOOR)
            if (credited <= 0) return@mapNotNull null
            FloorsClimbedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                floors = credited.toDouble(),
                metadata = Metadata.autoRecorded(
                    device = Device(type = Device.TYPE_WATCH),
                    clientRecordId = "$CLIENT_ID_PREFIX${start.toEpochMilli()}",
                    clientRecordVersion = credited.toLong(),
                ),
            )
        }
    }

    private const val SECONDS_PER_MINUTE = 60L
}
