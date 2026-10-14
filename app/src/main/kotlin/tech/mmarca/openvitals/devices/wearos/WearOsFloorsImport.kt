package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Maps the watch's ascent minutes to Health Connect records, one per minute
 * and type, keyed on the minute's start:
 *
 * - `ElevationGainedRecord`: the credited metres.
 * - `FloorsClimbedRecord`: the same metres over [METRES_PER_FLOOR], a
 *   fraction kept, so a day's partial floors add up.
 *
 * The watch counts every rise, an elevator ride and a drift of the weather
 * too. Height counts only with at least [MIN_STEPS_PER_FLOOR] steps per
 * floor's height in that minute and the one before; stairs take about 16 per
 * floor. The threshold is an app heuristic, no published source.
 *
 * The credited decimetres are the records' version, so a minute whose steps
 * or ascent grew replaces its records.
 *
 * Pure, so it is testable without a device.
 */
object WearOsFloorsImport {

    const val CLIENT_ID_PREFIX = "wearos_floors_"
    const val ELEVATION_CLIENT_ID_PREFIX = "wearos_elevation_"

    const val MIN_STEPS_PER_FLOOR = 10

    /** One floor: 3 m, about 10 ft, the common definition wearables use. */
    const val METRES_PER_FLOOR = 3.0

    /** Records for [ascent], given the [steps] pulled in the same sync. Minutes without enough steps write nothing. */
    fun records(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Record> =
        credited(ascent, steps).map { (start, metres) ->
            FloorsClimbedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                floors = metres / METRES_PER_FLOOR,
                metadata = watchMetadata("$CLIENT_ID_PREFIX${start.toEpochMilli()}", metres),
            )
        }

    /** The same credited metres as [records], as elevation gained. */
    fun elevationRecords(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Record> =
        credited(ascent, steps).map { (start, metres) ->
            ElevationGainedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                elevation = Length.meters(metres),
                metadata = watchMetadata("$ELEVATION_CLIENT_ID_PREFIX${start.toEpochMilli()}", metres),
            )
        }

    /** Per minute start, the metres that had enough steps around them. Minutes with none are left out. */
    private fun credited(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Pair<Instant, Double>> {
        val stepsAt = HashMap<Instant, Int>()
        for (minute in steps) stepsAt.merge(minute.start, minute.steps, ::maxOf)
        return ascent.groupBy { it.start }.mapNotNull { (start, same) ->
            val climbed = same.maxOf { it.metres }
            val walked = (stepsAt[start.minusSeconds(SECONDS_PER_MINUTE)] ?: 0) + (stepsAt[start] ?: 0)
            val credited = minOf(climbed, walked * METRES_PER_FLOOR / MIN_STEPS_PER_FLOOR)
            if (credited > 0.0) start to credited else null
        }
    }

    private fun watchMetadata(clientRecordId: String, metres: Double): Metadata =
        Metadata.autoRecorded(
            device = Device(type = Device.TYPE_WATCH),
            clientRecordId = clientRecordId,
            clientRecordVersion = (metres * 10).roundToLong(),
        )

    private const val SECONDS_PER_MINUTE = 60L
}
