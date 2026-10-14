package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Maps the watch's floor minutes to Health Connect records, one per minute
 * and type, keyed on the minute's start:
 *
 * - `FloorsClimbedRecord`: the credited floors.
 * - `ElevationGainedRecord`: the same floors as metres,
 *   [WearLinkProtocol.METRES_PER_FLOOR] each.
 *
 * The watch counts every 3 m of ascent, an elevator ride and a drift of the
 * weather too. A floor counts only with at least [MIN_STEPS_PER_FLOOR] steps
 * in that minute and the one before; stairs take about 16 per floor. The
 * threshold is an app heuristic, no published source.
 *
 * The credited count is the records' version, so a minute whose steps or
 * floors grew replaces its records.
 *
 * Pure, so it is testable without a device.
 */
object WearOsFloorsImport {

    const val CLIENT_ID_PREFIX = "wearos_floors_"
    const val ELEVATION_CLIENT_ID_PREFIX = "wearos_elevation_"

    const val MIN_STEPS_PER_FLOOR = 10

    /** Records for [floors], given the [steps] pulled in the same sync. Minutes without enough steps write nothing. */
    fun records(floors: List<WearOsFloorMinute>, steps: List<WearOsStepMinute>): List<Record> =
        credited(floors, steps).map { (start, credited) ->
            FloorsClimbedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                floors = credited.toDouble(),
                metadata = watchMetadata("$CLIENT_ID_PREFIX${start.toEpochMilli()}", credited),
            )
        }

    /** The ascent of the same credited floors as [records]. */
    fun elevationRecords(floors: List<WearOsFloorMinute>, steps: List<WearOsStepMinute>): List<Record> =
        credited(floors, steps).map { (start, credited) ->
            ElevationGainedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                elevation = Length.meters(credited * WearLinkProtocol.METRES_PER_FLOOR),
                metadata = watchMetadata("$ELEVATION_CLIENT_ID_PREFIX${start.toEpochMilli()}", credited),
            )
        }

    /** Per minute start, the floors that had enough steps around them. Minutes with none are left out. */
    private fun credited(floors: List<WearOsFloorMinute>, steps: List<WearOsStepMinute>): List<Pair<Instant, Int>> {
        val stepsAt = HashMap<Instant, Int>()
        for (minute in steps) stepsAt.merge(minute.start, minute.steps, ::maxOf)
        return floors.groupBy { it.start }.mapNotNull { (start, same) ->
            val climbed = same.maxOf { it.floors }
            val walked = (stepsAt[start.minusSeconds(SECONDS_PER_MINUTE)] ?: 0) + (stepsAt[start] ?: 0)
            val credited = minOf(climbed, walked / MIN_STEPS_PER_FLOOR)
            if (credited > 0) start to credited else null
        }
    }

    private fun watchMetadata(clientRecordId: String, credited: Int): Metadata =
        Metadata.autoRecorded(
            device = Device(type = Device.TYPE_WATCH),
            clientRecordId = clientRecordId,
            clientRecordVersion = credited.toLong(),
        )

    private const val SECONDS_PER_MINUTE = 60L
}
