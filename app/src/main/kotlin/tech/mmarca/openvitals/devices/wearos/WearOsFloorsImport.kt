package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import java.time.Instant
import kotlin.math.roundToLong
import tech.mmarca.openvitals.wearlink.AscentCredit

/**
 * Maps the watch's ascent minutes to Health Connect records, one per minute
 * and type, keyed on the minute's start:
 *
 * - `ElevationGainedRecord`: the credited metres.
 * - `FloorsClimbedRecord`: the same metres over [METRES_PER_FLOOR], a
 *   fraction kept, so a day's partial floors add up.
 *
 * Height counts only with enough steps around it ([AscentCredit]), so
 * elevator rides and weather drift drop out.
 *
 * The credited decimetres are the records' version, so a minute whose steps
 * or ascent grew replaces its records.
 *
 * Pure, so it is testable without a device.
 */
object WearOsFloorsImport {

    const val CLIENT_ID_PREFIX = "wearos_floors_"
    const val ELEVATION_CLIENT_ID_PREFIX = "wearos_elevation_"

    /** Records for [ascent], given the [steps] pulled in the same sync. Minutes without enough steps write nothing. */
    fun records(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Record> =
        credited(ascent, steps).map { (start, metres) ->
            FloorsClimbedRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(SECONDS_PER_MINUTE),
                endZoneOffset = null,
                floors = metres / AscentCredit.METRES_PER_FLOOR,
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

    /** Per minute start, the metres that had enough steps around them ([AscentCredit.creditedPerMinute]). */
    private fun credited(ascent: List<WearOsAscentMinute>, steps: List<WearOsStepMinute>): List<Pair<Instant, Double>> =
        AscentCredit.creditedPerMinute(
            climbedMetres = ascent.map { it.start.toEpochMilli() to it.metres },
            steps = steps.map { it.start.toEpochMilli() to it.steps },
        ).map { (start, metres) -> Instant.ofEpochMilli(start) to metres }

    private fun watchMetadata(clientRecordId: String, metres: Double): Metadata =
        Metadata.autoRecorded(
            device = Device(type = Device.TYPE_WATCH),
            clientRecordId = clientRecordId,
            clientRecordVersion = (metres * 10).roundToLong(),
        )

    private const val SECONDS_PER_MINUTE = 60L
}
