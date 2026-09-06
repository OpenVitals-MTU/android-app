package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.GarminSleepMinuteRepository
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.domain.insights.EstimatedSleepSession
import tech.mmarca.openvitals.domain.insights.EstimatedStage
import tech.mmarca.openvitals.domain.insights.SleepStageEstimator
import tech.mmarca.openvitals.domain.model.GarminSleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Turns stored per-minute rows into one estimated sleep session per night,
 * for watches that leave staging to Garmin's servers. The night is
 * re-estimated from the database on every sync that touches it and the
 * Health Connect record is replaced, never appended to.
 */

/** The window a night is estimated from: the evening before through the next early afternoon. */
fun sleepNightWindow(night: LocalDate, offset: ZoneOffset): Pair<Instant, Instant> = Pair(
    night.minusDays(1).atTime(NightWindowStart).toInstant(offset),
    night.atTime(NightWindowEnd).toInstant(offset),
)

/** One id per night, so a re-run updates the record in place. */
fun estimatedSleepClientRecordId(night: LocalDate): String = "$EstimatedSleepIdPrefix$night"

/** True for a session the watch staged itself. Those win over estimates. */
fun isWatchStagedSleepId(clientRecordId: String): Boolean =
    clientRecordId.startsWith(WatchSleepIdPrefix) && !clientRecordId.startsWith(EstimatedSleepIdPrefix)

/**
 * The Health Connect record for an estimated night, or null when nothing
 * would count as sleep. Stages are clamped into the session and merged so
 * Health Connect's overlap check cannot reject them.
 */
fun estimatedSleepImportRecord(
    session: EstimatedSleepSession,
    night: LocalDate,
    offset: ZoneOffset,
    version: Long,
): SleepSessionRecord? {
    val stages = mutableListOf<SleepSessionRecord.Stage>()
    for (span in session.stages.sortedBy { it.start }) {
        val start = maxOf(span.start, session.onset, stages.lastOrNull()?.endTime ?: session.onset)
        val end = minOf(span.end, session.end)
        if (!start.isBefore(end)) continue
        val stage = healthConnectStageFor(span.stage)
        val previous = stages.lastOrNull()
        if (previous != null && previous.stage == stage && previous.endTime == start) {
            stages[stages.size - 1] = SleepSessionRecord.Stage(previous.startTime, end, stage)
        } else {
            stages.add(SleepSessionRecord.Stage(start, end, stage))
        }
    }
    if (stages.none { it.stage in SleepingStages }) return null
    return SleepSessionRecord(
        startTime = session.onset,
        startZoneOffset = offset,
        endTime = session.end,
        endZoneOffset = offset,
        metadata = Metadata.manualEntry(
            clientRecordId = estimatedSleepClientRecordId(night),
            clientRecordVersion = version,
            device = Device(type = Device.TYPE_PHONE),
        ),
        title = EstimatedSleepTitle,
        notes = EstimatedSleepNotes,
        stages = stages,
    )
}

private fun healthConnectStageFor(stage: EstimatedStage): Int = when (stage) {
    EstimatedStage.AWAKE -> SleepSessionRecord.STAGE_TYPE_AWAKE
    EstimatedStage.LIGHT -> SleepSessionRecord.STAGE_TYPE_LIGHT
    EstimatedStage.DEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
    EstimatedStage.REM -> SleepSessionRecord.STAGE_TYPE_REM
    EstimatedStage.UNKNOWN -> SleepSessionRecord.STAGE_TYPE_UNKNOWN
}

/** The domain minute for a decoded file row. */
fun FitSleepMinute.toGarminSleepMinute(): GarminSleepMinute = GarminSleepMinute(
    time = time,
    kind = when (kind) {
        FitSleepMinuteKind.RAW -> SleepMinuteKind.RAW
        FitSleepMinuteKind.AWAKE -> SleepMinuteKind.AWAKE
        FitSleepMinuteKind.UNMEASURABLE -> SleepMinuteKind.UNMEASURABLE
    },
    heartRate = heartRate,
    movement = movement,
    activity = activity,
    zoneOffset = zoneOffset,
    features = features,
)

/** Estimates and writes the nights a sync touched. */
@Singleton
class GarminSleepEstimationWriter(
    private val importRepository: AppleHealthImportRepository,
    private val minuteRepository: GarminSleepMinuteRepository,
    private val healthConnect: HealthConnectManager,
    private val clock: () -> Instant,
) {
    @Inject
    constructor(
        importRepository: AppleHealthImportRepository,
        minuteRepository: GarminSleepMinuteRepository,
        healthConnect: HealthConnectManager,
    ) : this(importRepository, minuteRepository, healthConnect, Instant::now)

    /**
     * Estimates each night in [nights] from the stored minutes and writes it.
     * A night in [nightsWithRealStages], or one the watch already staged in
     * Health Connect, is left alone. Storage failures propagate so the sync
     * fails and the files are fetched again; an estimator failure is logged.
     */
    suspend fun estimateNights(
        nights: Set<LocalDate>,
        offsets: Map<LocalDate, ZoneOffset>,
        nightsWithRealStages: Set<LocalDate> = emptySet(),
    ) {
        for (night in nights.sorted()) {
            if (night in nightsWithRealStages) {
                GarminLog.log("[GARMIN-SLEEP-EST] $night: the watch staged it, skipping")
                continue
            }
            val offset = offsets[night] ?: ZoneOffset.UTC
            val (from, to) = sleepNightWindow(night, offset)
            if (hasWatchStagedSession(from, to)) {
                GarminLog.log("[GARMIN-SLEEP-EST] $night: Health Connect holds a watch-staged night, skipping")
                continue
            }
            val minutes = minuteRepository.minutesBetween(from, to)
            val session = try {
                SleepStageEstimator.estimate(minutes.map { it.toSleepMinute() })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SLEEP-EST] $night: estimator failed on ${minutes.size} minutes: $error")
                continue
            }
            if (session == null) {
                GarminLog.log("[GARMIN-SLEEP-EST] $night: no night in ${minutes.size} minutes")
                continue
            }
            val record = estimatedSleepImportRecord(session, night, offset, version = clock().toEpochMilli())
            if (record == null) {
                GarminLog.log("[GARMIN-SLEEP-EST] $night: nothing counted as sleep")
                continue
            }
            write(record)
            GarminLog.log(
                "[GARMIN-SLEEP-EST] $night: ${session.onset} → ${session.end} " +
                    "sleep=${session.sleepMinutes}m awake=${session.awakeMinutes}m " +
                    "unknown=${session.unknownMinutes}m stages=${record.stages.size} " +
                    "inProgress=${session.inProgress} td=${session.deepThreshold} tr=${session.remThreshold}",
            )
        }
    }

    /** Whether a watch-staged session already covers the window. A read failure counts as no. */
    private suspend fun hasWatchStagedSession(from: Instant, to: Instant): Boolean {
        var found = false
        try {
            healthConnect.forEachSyncRecordPage(SleepSessionRecord::class, from, to) { page ->
                if (page.any { it.metadata.clientRecordId?.let(::isWatchStagedSleepId) == true }) found = true
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-SLEEP-EST] could not read existing sessions: $error")
        }
        return found
    }

    /** A higher version replaces the earlier estimate. If Health Connect still objects, delete and retry once. */
    private suspend fun write(record: SleepSessionRecord) {
        try {
            importRepository.insertImportedRecords(listOf(record))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isDuplicateClientRecordFailure()) throw error
            val id = requireNotNull(record.metadata.clientRecordId)
            GarminLog.log("[GARMIN-SLEEP-EST] $id already present; replacing")
            healthConnect.deleteImportedRecordsByClientIds(SleepSessionRecord::class, listOf(id))
            importRepository.insertImportedRecords(listOf(record))
        }
    }
}

private val NightWindowStart: LocalTime = LocalTime.of(18, 0)
private val NightWindowEnd: LocalTime = LocalTime.of(14, 0)
private const val WatchSleepIdPrefix = "garmin_fit_sleep_"
private const val EstimatedSleepIdPrefix = "garmin_fit_sleep_est_"
private const val EstimatedSleepTitle = "Sleep"
private const val EstimatedSleepNotes =
    "Sleep stages estimated by OpenVitals from heart rate and movement. This watch does not record sleep stages."
private val SleepingStages = setOf(
    SleepSessionRecord.STAGE_TYPE_LIGHT,
    SleepSessionRecord.STAGE_TYPE_DEEP,
    SleepSessionRecord.STAGE_TYPE_REM,
)
