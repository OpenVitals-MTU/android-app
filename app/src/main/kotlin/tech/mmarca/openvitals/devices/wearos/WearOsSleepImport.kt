package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import tech.mmarca.openvitals.devices.core.sync.estimatedSleepSessionRecord
import tech.mmarca.openvitals.devices.core.sync.sleepNightWindow
import tech.mmarca.openvitals.domain.insights.SleepStageEstimator
import tech.mmarca.openvitals.domain.model.SleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

/**
 * Turns the per-minute rows the watch recorded into one estimated sleep
 * session per night, through the same `SleepStageEstimator` the Garmin path
 * uses. The watch is the store: the phone keeps no minutes, only a cursor,
 * and re-pulls a whole night's window to estimate it again as the night
 * fills in. One id per night, so the later estimate replaces the earlier.
 *
 * Pure: no Android, so it is testable without a device.
 */
object WearOsSleepImport {

    const val CLIENT_ID_PREFIX = "wearos_sleep_est_"

    fun clientRecordId(night: LocalDate): String = "$CLIENT_ID_PREFIX$night"

    /** True for a session this import wrote. Any other session in the window wins over a new estimate. */
    fun isOwnRecordId(clientRecordId: String?): Boolean = clientRecordId?.startsWith(CLIENT_ID_PREFIX) == true

    /**
     * Where a pull starts so the night the cursor sits in can be estimated
     * whole: the start of that night's window, or the cursor itself when it
     * falls in the afternoon gap between nights. Never before the epoch.
     */
    fun pullStart(cursor: Instant, offset: ZoneOffset): Instant {
        if (cursor == Instant.EPOCH) return cursor
        val night = SleepStageEstimator.nightDateOf(cursor, offset) ?: return cursor
        val (windowStart, _) = sleepNightWindow(night, offset)
        // The watch answers "newer than", so one millisecond before the window includes its first minute.
        return minOf(cursor, windowStart.minusMillis(1))
    }

    /**
     * The nights that minutes newer than [cursor] belong to, each with the
     * offset of its last such minute: a DST change mid-night shifts the
     * window by an hour at most.
     */
    fun touchedNights(minutes: List<WearOsSleepMinute>, cursor: Instant): Map<LocalDate, ZoneOffset> {
        val offsets = LinkedHashMap<LocalDate, ZoneOffset>()
        for (minute in minutes.filter { it.time.isAfter(cursor) }.sortedBy { it.time }) {
            val night = SleepStageEstimator.nightDateOf(minute.time, minute.zoneOffset) ?: continue
            offsets[night] = minute.zoneOffset
        }
        return offsets
    }

    /** The estimator's input for [night]: the minutes inside its window. */
    fun minutesOfNight(minutes: List<WearOsSleepMinute>, night: LocalDate, offset: ZoneOffset): List<SleepMinute> {
        val (from, to) = sleepNightWindow(night, offset)
        return minutes
            .filter { !it.time.isBefore(from) && it.time.isBefore(to) }
            .map { it.toSleepMinute() }
    }

    /** The record for [night], or null when the minutes hold no night or nothing counted as sleep. */
    fun record(
        minutes: List<WearOsSleepMinute>,
        night: LocalDate,
        offset: ZoneOffset,
        version: Long,
    ): SleepSessionRecord? {
        val session = SleepStageEstimator.estimate(minutesOfNight(minutes, night, offset)) ?: return null
        return estimatedSleepSessionRecord(
            session = session,
            offset = offset,
            clientRecordId = clientRecordId(night),
            version = version,
            notes = NOTES,
        )
    }

    private fun WearOsSleepMinute.toSleepMinute(): SleepMinute = SleepMinute(
        time = time,
        kind = when (kind) {
            WearLinkProtocol.MinuteKind.RAW -> SleepMinuteKind.RAW
            WearLinkProtocol.MinuteKind.AWAKE -> SleepMinuteKind.AWAKE
            WearLinkProtocol.MinuteKind.UNMEASURABLE -> SleepMinuteKind.UNMEASURABLE
        },
        movement = movement,
        heartRate = heartRate,
    )

    private const val NOTES =
        "Sleep stages estimated by OpenVitals from the heart rate and movement the OpenVitals watch app recorded."
}
