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
 * The walking energy also depends on the weight and on the exercise sessions
 * present when it was written, which the version does not carry: the
 * estimate is frozen at its first write, and only a minute whose count grows
 * is worked out again.
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

    /**
     * Empty unless [weightKg] is known; minutes too slow to cost more than rest
     * or inside an exercise session write nothing.
     */
    fun activeCaloriesRecords(
        minutes: List<WearOsStepMinute>,
        weightKg: Double?,
        exerciseSessions: List<ClosedRange<Instant>> = emptyList(),
    ): List<Record> {
        if (weightKg == null || weightKg <= 0.0) return emptyList()
        return latestPerMinute(minutes).mapNotNull { minute ->
            if (!WalkingEnergy.isAboveRest(minute.steps) || isInsideExerciseSession(minute, exerciseSessions)) {
                return@mapNotNull null
            }
            val kcal = WalkingEnergy.activeKcalPerMinute(minute.steps, weightKg)
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

    /** The minutes no exercise session in [exerciseSessions] overlaps. */
    fun outsideExerciseSessions(
        minutes: List<WearOsStepMinute>,
        exerciseSessions: List<ClosedRange<Instant>>,
    ): List<WearOsStepMinute> = minutes.filterNot { isInsideExerciseSession(it, exerciseSessions) }

    /** Whether a minute would cost more than rest, so a known weight would write walking energy for it. */
    fun hasWalkingEnergy(minutes: List<WearOsStepMinute>): Boolean = minutes.any { WalkingEnergy.isAboveRest(it.steps) }

    fun clientRecordId(minuteStart: Instant): String = "$CLIENT_ID_PREFIX${minuteStart.toEpochMilli()}"

    /** A minute twice in one page keeps the larger, later count. */
    private fun latestPerMinute(minutes: List<WearOsStepMinute>): List<WearOsStepMinute> =
        minutes.groupBy { it.start }.map { (_, same) -> same.maxBy { it.steps } }

    private fun isInsideExerciseSession(minute: WearOsStepMinute, exerciseSessions: List<ClosedRange<Instant>>): Boolean {
        if (exerciseSessions.isEmpty()) return false
        val minuteStart = minute.start
        val minuteEnd = minute.end
        return exerciseSessions.any { session ->
            val sessionStart = session.start
            val sessionEnd = session.endInclusive
            sessionEnd > sessionStart && minuteStart < sessionEnd && minuteEnd > sessionStart
        }
    }

    private fun watchMetadata(clientRecordId: String, steps: Int): Metadata =
        Metadata.autoRecorded(
            device = Device(type = Device.TYPE_WATCH),
            clientRecordId = clientRecordId,
            clientRecordVersion = steps.toLong(),
        )

    private val WearOsStepMinute.end: Instant get() = start.plusSeconds(SECONDS_PER_MINUTE)

    private const val SECONDS_PER_MINUTE = 60L
}
