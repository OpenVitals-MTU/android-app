package tech.mmarca.openvitals.devices.garmin.wellness

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.core.fit.FitDecoder
import tech.mmarca.openvitals.core.fit.FitMessage
import tech.mmarca.openvitals.core.fit.fitInstant
import tech.mmarca.openvitals.devices.garmin.GarminLog

/**
 * A Garmin sleep stage, from the FIT `sleep_level` enum (message 275, field 0).
 */
enum class FitSleepLevel { UNMEASURABLE, AWAKE, LIGHT, DEEP, REM }

/** One stage span within a sleep session: `[start, end)` spent at [level]. */
data class FitSleepStage(
    val start: Instant,
    val end: Instant,
    val level: FitSleepLevel,
)

/**
 * A decoded Garmin sleep FIT file (file type 49): the night's bounds and its
 * stage timeline. The bounds come from the `event`/74 (sleep) start/stop pair;
 * each `sleep_level` message closes a stage that began at the previous one.
 */
data class FitSleepSession(
    val start: Instant,
    val end: Instant,
    val stages: List<FitSleepStage>,
    /**
     * The watch's own sleep score, 0..100, as shown on the wrist.
     *
     * Deliberately kept alongside [stages] rather than folded into them: this
     * is Garmin's verdict on the night, while the stages are transitions we
     * interpret ourselves. Where the two disagree, having both is what makes
     * the disagreement visible instead of silently picking one.
     */
    val overallScore: Int? = null,
    /** How many times the watch counted the sleeper waking. */
    val awakeningsCount: Int? = null,
)

/**
 * A decoded Garmin HRV nightly reading (file type 68):
 * `hrv_status_summary.last_night_average` as an RMSSD in milliseconds.
 */
data class FitHrvReading(
    val time: Instant,
    val rmssdMillis: Double,
)

/**
 * [FitMonitoringPoint.activityType] for a counter whose message named no
 * activity — neither `activity_type` (5) nor `current_activity_type_intensity`
 * (24).
 *
 * Message-local on purpose, matching Gadgetbridge's `getComputedActivityType`:
 * FIT fields are fixed per definition message, so a counter record without a
 * type field is untyped by design, not an abbreviation of the previous
 * message's type. Inheriting the last-declared type stamped whole-day-total
 * restatements with whatever type happened to come before them, and a total
 * landing on a small type's context minted the difference as fresh steps.
 *
 * Deliberately outside FIT's `activity_type` enum: it is not a kind of
 * activity, it is the absence of one, and the day-total sum has to be able to
 * tell it apart from a real bucket.
 */
const val UNKNOWN_FIT_ACTIVITY_TYPE: Int = -1

/**
 * A cumulative monitoring counter reading: a [value] for [activityType] at
 * [time]. Cumulative within a wear-session and per activity type, so a
 * per-file total is a sum of per-type within-file deltas (see the mapper).
 */
data class FitMonitoringPoint(
    val time: Instant,
    /**
     * FIT `activity_type` enum (walking 6, running 1, generic 0, …), or
     * [UNKNOWN_FIT_ACTIVITY_TYPE] when the message did not carry one.
     */
    val activityType: Int,
    val value: Int,
)

/**
 * Everything a monitoring file (type 32) carried. The one-per-file summaries
 * (resting HR, BMR) plus the high-frequency series (per-minute HR, breathing,
 * and the cumulative step/distance/calorie counters). Aggregation into Health
 * Connect records happens in the mapper.
 */
data class FitMonitoringSummary(
    val restingHeartRateTime: Instant? = null,
    val restingHeartRateBpm: Int? = null,
    val bmrTime: Instant? = null,
    val bmrKcalPerDay: Double? = null,
    /** Per-minute heart-rate samples `(time, bpm)`. */
    val heartRateSamples: List<Pair<Instant, Int>> = emptyList(),
    /** Breathing-rate readings `(time, breathsPerMinute)`. */
    val respiration: List<Pair<Instant, Double>> = emptyList(),
    /** Cumulative step (walk/run), distance (m) and active-calorie counters. */
    val stepPoints: List<FitMonitoringPoint> = emptyList(),
    val distancePoints: List<FitMonitoringPoint> = emptyList(),
    val caloriePoints: List<FitMonitoringPoint> = emptyList(),
    /**
     * Running daily totals of Garmin's intensity minutes `(time, minutes)`.
     * Cumulative like the step counter, not per-message increments.
     */
    val moderateMinutes: List<Pair<Instant, Int>> = emptyList(),
    val vigorousMinutes: List<Pair<Instant, Int>> = emptyList(),
    /**
     * Garmin stress score `(time, 0..100)`. Health Connect has no type for
     * this, so it is kept in the app's own database rather than exported.
     */
    val stress: List<Pair<Instant, Int>> = emptyList(),
    /**
     * Garmin Body Battery `(time, 0..100)`. Same story — no Health Connect
     * type.
     *
     * Note this is the WATCH's measure, distinct from the app's own computed
     * Body Energy timeline; they are two independent estimates of a similar
     * idea and must not be conflated.
     */
    val bodyEnergy: List<Pair<Instant, Int>> = emptyList(),
) {
    val isEmpty: Boolean
        get() = restingHeartRateBpm == null &&
            bmrKcalPerDay == null &&
            heartRateSamples.isEmpty() &&
            respiration.isEmpty() &&
            stepPoints.isEmpty() &&
            distancePoints.isEmpty() &&
            caloriePoints.isEmpty() &&
            moderateMinutes.isEmpty() &&
            vigorousMinutes.isEmpty() &&
            stress.isEmpty() &&
            bodyEnergy.isEmpty()
}

/**
 * The fitness metrics a metrics file (Garmin type 44) carried.
 *
 * Each is a snapshot the watch recomputes rather than a series, so at most one
 * of each survives a file — the last seen. Only VO2 max has a Health Connect
 * type; the rest are Garmin's own estimates and stay in the app's database,
 * the same split stress and Body Battery already follow.
 */
data class FitMetricsSummary(
    /**
     * When the watch computed these. Null when no message carried a timestamp,
     * which makes the whole snapshot unplaceable and therefore unusable.
     */
    val time: Instant? = null,
    /** mL/kg/min. */
    val vo2Max: Double? = null,
    /** How long the watch thinks recovery still needs, in minutes. */
    val recoveryTimeMinutes: Int? = null,
    /** 0..100. */
    val trainingReadiness: Int? = null,
    val trainingLoadAcute: Int? = null,
    val trainingLoadChronic: Int? = null,
) {
    val isEmpty: Boolean
        get() = vo2Max == null &&
            recoveryTimeMinutes == null &&
            trainingReadiness == null &&
            trainingLoadAcute == null &&
            trainingLoadChronic == null
}

/**
 * The watch's own summary of a night (`daily_sleep`), computed on the wrist.
 *
 * Entirely independent of [FitSleepSession]: that is built from stage
 * transitions this app interprets, while these are the numbers the watch shows
 * its wearer. Keeping both is what makes a disagreement between them visible.
 */
data class FitDailySleep(
    /** When the night ended, per the watch. */
    val endTime: Instant? = null,
    /** 0..100. */
    val score: Int? = null,
    /** How long the watch counted the sleeper as awake during the night. */
    val awakeDuration: Duration? = null,
    /**
     * Garmin's "sleep pressure" figure. Kept raw — its scale is undocumented
     * and guessing at units would be worse than passing the number through.
     */
    val pressure: Int? = null,
) {
    val isEmpty: Boolean
        get() = score == null && awakeDuration == null && pressure == null
}

/**
 * Sleep Coach (`sleep_demand`): how much sleep the watch thinks is normally
 * needed, and how much last night's strain called for.
 */
data class FitSleepDemand(
    val time: Instant? = null,
    /** The usual nightly need. */
    val normal: Duration? = null,
    /** What this particular night demanded — higher after a hard day. */
    val demand: Duration? = null,
) {
    val isEmpty: Boolean
        get() = normal == null && demand == null
}

/**
 * One Health Snapshot recording: the two-minute on-demand measurement the
 * watch takes when the wearer asks for it.
 *
 * Separate from the monitoring series even though three of the four metrics
 * overlap: these are a deliberate spot measurement at rest, sampled far more
 * densely, and averaging them into the all-day series would blur both.
 */
data class FitHealthSnapshot(
    /**
     * Blood oxygen `(time, percent)` — the only Pulse Ox this watch has been
     * seen to write anywhere.
     */
    val spo2: List<Pair<Instant, Int>> = emptyList(),
    val respiration: List<Pair<Instant, Double>> = emptyList(),
    val stress: List<Pair<Instant, Int>> = emptyList(),
    val bodyEnergy: List<Pair<Instant, Int>> = emptyList(),
) {
    val isEmpty: Boolean
        get() = spo2.isEmpty() &&
            respiration.isEmpty() &&
            stress.isEmpty() &&
            bodyEnergy.isEmpty()
}

/**
 * A daytime nap the watch recorded, bounded by its own start/end fields rather
 * than the `event`/74 pair that bounds a night.
 */
data class FitNap(
    val start: Instant,
    val end: Instant,
)

/**
 * The wellness data a FIT file carried, from one decode pass. Each Garmin file
 * is a single type, so at most one of these is populated (activities have
 * none).
 */
data class FitWellness(
    /**
     * `file_id.type` — lets the caller tell a non-activity file with no
     * mappable data (skip it) from an activity file (parse it as an exercise).
     */
    val fileType: Int? = null,
    val sleep: FitSleepSession? = null,
    val hrv: FitHrvReading? = null,
    val monitoring: FitMonitoringSummary? = null,
    val metrics: FitMetricsSummary? = null,
    /** Daytime naps. A list, not a single value: one sleep file can hold several. */
    val naps: List<FitNap> = emptyList(),
    /**
     * The watch's own nightly summary and Sleep Coach figures. These arrive in
     * the METRICS file on a vívoactive 5, not the sleep file.
     */
    val dailySleep: FitDailySleep? = null,
    val sleepDemand: FitSleepDemand? = null,
    /** From a Health Snapshot file (type 70). */
    val healthSnapshot: FitHealthSnapshot? = null,
) {
    val isEmpty: Boolean
        get() = sleep == null &&
            hrv == null &&
            monitoring == null &&
            metrics == null &&
            naps.isEmpty() &&
            dailySleep == null &&
            sleepDemand == null &&
            healthSnapshot == null

    /**
     * True for `activity` (4), `workout` (5) and `course` (6) — the types the
     * exercise/route importer handles. Everything else is wellness data.
     */
    val isActivityType: Boolean
        get() = fileType == 4 || fileType == 5 || fileType == 6
}

/**
 * Decodes the **wellness** data a Garmin FIT file carries (sleep, HRV,
 * monitoring, metrics, …) in one pass. Wellness files have no activity session
 * or route, so the activity parser rejects them — this is their path. Returns
 * an empty [FitWellness] for activity, course and workout files.
 *
 * Built on the generic [FitDecoder]: this file owns only the
 * Garmin-proprietary interpretation of the decoded messages. Port of the
 * Flutter build's `garmin_fit_wellness.dart` — the message and field numbers
 * mirror it exactly.
 */
fun parseGarminWellness(fitBytes: ByteArray, fileName: String? = null): FitWellness {
    val result = GarminWellnessDecoder(fitBytes).decode()
    return FitWellness(
        fileType = result.fileType,
        sleep = result.sleep.toSession(),
        hrv = result.hrv.toReading(),
        monitoring = result.monitoring.toSummary(),
        metrics = result.metrics.toSummary(),
        naps = result.sleep.naps,
        dailySleep = result.metrics.toDailySleep(),
        sleepDemand = result.metrics.toSleepDemand(),
        healthSnapshot = result.metrics.toHealthSnapshot(),
    )
}

/** The Garmin sleep session in [fitBytes], or null if it carries none. */
fun parseGarminSleepSession(fitBytes: ByteArray, fileName: String? = null): FitSleepSession? =
    parseGarminWellness(fitBytes, fileName = fileName).sleep

/**
 * One file's decoded wellness carriers, merged across a chained stream by
 * [GarminWellnessDecoder]. The switch cases fill disjoint carriers, so the
 * four raw structs never overlap.
 */
private class FitWellnessResult(
    val fileType: Int?,
    val sleep: FitSleepRaw,
    val hrv: FitHrvRaw,
    val monitoring: FitMonitoringRaw,
    val metrics: FitMetricsRaw,
) {
    fun merge(other: FitWellnessResult): FitWellnessResult = FitWellnessResult(
        // First file with a file type wins, matching the activity summary's
        // per-file merge (a chained stream is one logical export).
        fileType = fileType ?: other.fileType,
        sleep = sleep.merge(other.sleep),
        hrv = hrv.merge(other.hrv),
        monitoring = monitoring.merge(other.monitoring),
        metrics = metrics.merge(other.metrics),
    )
}

/**
 * The raw HRV reading a file carried (`hrv_status_summary.last_night_average`).
 * At most one is kept — the last seen — since a status file holds one summary.
 */
private class FitHrvRaw(
    val time: Instant? = null,
    val rmssdMillis: Double? = null,
) {
    fun merge(other: FitHrvRaw): FitHrvRaw = FitHrvRaw(
        time = other.time ?: time,
        rmssdMillis = other.rmssdMillis ?: rmssdMillis,
    )

    fun toReading(): FitHrvReading? =
        if (time != null && rmssdMillis != null) {
            FitHrvReading(time = time, rmssdMillis = rmssdMillis)
        } else {
            null
        }
}

/**
 * The one-per-file monitoring summaries (resting HR, BMR) plus the
 * high-frequency series collected from a type-32 file. The last seen of each
 * scalar wins.
 */
private class FitMonitoringRaw(
    val restingHrTime: Instant? = null,
    val restingHrBpm: Int? = null,
    val bmrTime: Instant? = null,
    val bmrKcalPerDay: Double? = null,
    val heartRate: List<Pair<Instant, Int>> = emptyList(),
    val respiration: List<Pair<Instant, Double>> = emptyList(),
    val stress: List<Pair<Instant, Int>> = emptyList(),
    val bodyEnergy: List<Pair<Instant, Int>> = emptyList(),
    val steps: List<FitMonitoringPoint> = emptyList(),
    val distance: List<FitMonitoringPoint> = emptyList(),
    val calories: List<FitMonitoringPoint> = emptyList(),
    val moderateMinutes: List<Pair<Instant, Int>> = emptyList(),
    val vigorousMinutes: List<Pair<Instant, Int>> = emptyList(),
) {
    fun merge(other: FitMonitoringRaw): FitMonitoringRaw = FitMonitoringRaw(
        restingHrTime = other.restingHrTime ?: restingHrTime,
        restingHrBpm = other.restingHrBpm ?: restingHrBpm,
        bmrTime = other.bmrTime ?: bmrTime,
        bmrKcalPerDay = other.bmrKcalPerDay ?: bmrKcalPerDay,
        heartRate = heartRate + other.heartRate,
        respiration = respiration + other.respiration,
        stress = stress + other.stress,
        bodyEnergy = bodyEnergy + other.bodyEnergy,
        steps = steps + other.steps,
        distance = distance + other.distance,
        calories = calories + other.calories,
        moderateMinutes = moderateMinutes + other.moderateMinutes,
        vigorousMinutes = vigorousMinutes + other.vigorousMinutes,
    )

    fun toSummary(): FitMonitoringSummary? {
        val summary = FitMonitoringSummary(
            restingHeartRateTime = restingHrTime,
            restingHeartRateBpm = restingHrBpm,
            bmrTime = bmrTime,
            bmrKcalPerDay = bmrKcalPerDay,
            heartRateSamples = heartRate,
            respiration = respiration,
            stress = stress,
            bodyEnergy = bodyEnergy,
            stepPoints = steps,
            distancePoints = distance,
            caloriePoints = calories,
            moderateMinutes = moderateMinutes,
            vigorousMinutes = vigorousMinutes,
        )
        return if (summary.isEmpty) null else summary
    }
}

/**
 * The raw sleep messages a single FIT file carried: the `event`/74 session
 * bounds and the `sleep_level` transitions. Turned into a [FitSleepSession]
 * once the whole file (or chain of files) is decoded.
 */
private class FitSleepRaw(
    val start: Instant? = null,
    val stop: Instant? = null,
    /** Each entry is `(transitionTime, sleepLevelEnumValue)`, in file order. */
    val levels: List<Pair<Instant, Int>> = emptyList(),
    val overallScore: Int? = null,
    val awakeningsCount: Int? = null,
    val naps: List<FitNap> = emptyList(),
) {
    fun merge(other: FitSleepRaw): FitSleepRaw = FitSleepRaw(
        start = start ?: other.start,
        stop = stop ?: other.stop,
        levels = levels + other.levels,
        overallScore = overallScore ?: other.overallScore,
        awakeningsCount = awakeningsCount ?: other.awakeningsCount,
        naps = naps + other.naps,
    )

    fun toSession(): FitSleepSession? {
        if (levels.isEmpty()) return null
        val sorted = levels.sortedBy { it.first }
        val sessionStart = start ?: sorted.first().first
        // Sleep never ends before it starts; a file that says so is unusable.
        val sessionEnd = if (stop != null && stop.isAfter(sessionStart)) {
            stop
        } else {
            sorted.last().first
        }
        if (!sessionStart.isBefore(sessionEnd)) return null
        val stages = mutableListOf<FitSleepStage>()
        // Each `sleep_level` timestamp is the UPPER BOUND (end) of the stage it
        // names, not its start: the stage runs from the previous transition
        // (the session start for the first) up to this timestamp. Reading it as
        // a start — the stage running forward to the NEXT transition — shifts
        // every span onto the wrong stage, which tripled REM and inflated Awake
        // against the watch's own screen. Confirmed against Gadgetbridge, which
        // fills these with an UPPER_BOUND RangeMap
        // (GarminActivitySampleProvider.overlaySleep).
        var boundary = sessionStart
        for ((transition, rawLevel) in sorted) {
            // Clamp into the session so a stray pre-start / post-stop
            // transition can neither widen a stage nor walk the boundary
            // outside the night.
            var stageEnd = transition
            if (stageEnd.isBefore(sessionStart)) stageEnd = sessionStart
            if (stageEnd.isAfter(sessionEnd)) stageEnd = sessionEnd
            val stageStart = boundary
            boundary = stageEnd
            // Advance the boundary for every sample, then skip only an unknown
            // raw value (null). An `unmeasurable` span is a real level here and
            // is emitted like any other — it is dropped downstream at the
            // Health Connect mapping, which has no stage for it.
            val level = fitSleepLevelFromRaw(rawLevel) ?: continue
            if (!stageStart.isBefore(stageEnd)) continue
            stages.add(FitSleepStage(start = stageStart, end = stageEnd, level = level))
        }
        if (stages.isEmpty()) return null
        // DIAGNOSTIC: the raw transitions and the per-stage totals they
        // produce, to diff against the watch's own screen. Garmin smooths the
        // displayed hypnogram further than the raw `sleep_level` series does,
        // so the two never match to the minute — this is how we see by how
        // much. GarminLog only speaks in debug builds with a sink installed, so
        // a release build neither prints a person's night nor computes these
        // totals.
        if (GarminLog.enabled) {
            val totals = mutableMapOf<FitSleepLevel, Long>()
            for (stage in stages) {
                totals[stage.level] = (totals[stage.level] ?: 0L) +
                    Duration.between(stage.start, stage.end).toMinutes()
            }
            val covered = totals.values.sum()
            GarminLog.log(
                "[FIT-SLEEP] session $sessionStart → $sessionEnd " +
                    "(${Duration.between(sessionStart, sessionEnd).toMinutes()}m) " +
                    "transitions=${sorted.size} stages=${stages.size} covered=${covered}m",
            )
            GarminLog.log(
                "[FIT-SLEEP] totals: " +
                    totals.entries.joinToString(" ") { "${it.key.name}=${it.value}m" },
            )
            for ((transition, rawLevel) in sorted) {
                GarminLog.log(
                    "[FIT-SLEEP]   $transition raw=$rawLevel " +
                        "(${fitSleepLevelFromRaw(rawLevel)?.name ?: "UNKNOWN"})",
                )
            }
            // The watch's own verdict on the same night, for comparison against
            // what the stages above add up to.
            GarminLog.log(
                "[FIT-SLEEP] watch says: score=${overallScore ?: "-"} " +
                    "awakenings=${awakeningsCount ?: "-"}",
            )
        }
        return FitSleepSession(
            start = sessionStart,
            end = sessionEnd,
            stages = stages,
            overallScore = overallScore,
            awakeningsCount = awakeningsCount,
        )
    }
}

/**
 * The metrics-file snapshots a decode pass collected. Last seen wins for each,
 * independently: one file can carry a VO2 max message and a training-load
 * message with nothing in common but the file they share.
 */
private class FitMetricsRaw(
    val time: Instant? = null,
    val vo2Max: Double? = null,
    val recoveryTimeMinutes: Int? = null,
    val trainingReadiness: Int? = null,
    val trainingLoadAcute: Int? = null,
    val trainingLoadChronic: Int? = null,
    // Sleep summaries that share the metrics file rather than the sleep file.
    val dailySleepEndTime: Instant? = null,
    val dailySleepScore: Int? = null,
    val dailySleepAwakeSeconds: Int? = null,
    val dailySleepPressure: Int? = null,
    val sleepDemandTime: Instant? = null,
    val sleepDemandNormalMinutes: Int? = null,
    val sleepDemandMinutes: Int? = null,
    // Health Snapshot samples. They ride here rather than in their own result
    // slot because this is already the "everything that is not a session, a
    // night or a monitoring series" carrier.
    val hsaSpo2: List<Pair<Instant, Int>> = emptyList(),
    val hsaRespiration: List<Pair<Instant, Double>> = emptyList(),
    val hsaStress: List<Pair<Instant, Int>> = emptyList(),
    val hsaBodyEnergy: List<Pair<Instant, Int>> = emptyList(),
) {
    fun merge(other: FitMetricsRaw): FitMetricsRaw = FitMetricsRaw(
        time = other.time ?: time,
        vo2Max = other.vo2Max ?: vo2Max,
        recoveryTimeMinutes = other.recoveryTimeMinutes ?: recoveryTimeMinutes,
        trainingReadiness = other.trainingReadiness ?: trainingReadiness,
        trainingLoadAcute = other.trainingLoadAcute ?: trainingLoadAcute,
        trainingLoadChronic = other.trainingLoadChronic ?: trainingLoadChronic,
        dailySleepEndTime = other.dailySleepEndTime ?: dailySleepEndTime,
        dailySleepScore = other.dailySleepScore ?: dailySleepScore,
        dailySleepAwakeSeconds = other.dailySleepAwakeSeconds ?: dailySleepAwakeSeconds,
        dailySleepPressure = other.dailySleepPressure ?: dailySleepPressure,
        sleepDemandTime = other.sleepDemandTime ?: sleepDemandTime,
        sleepDemandNormalMinutes = other.sleepDemandNormalMinutes ?: sleepDemandNormalMinutes,
        sleepDemandMinutes = other.sleepDemandMinutes ?: sleepDemandMinutes,
        hsaSpo2 = hsaSpo2 + other.hsaSpo2,
        hsaRespiration = hsaRespiration + other.hsaRespiration,
        hsaStress = hsaStress + other.hsaStress,
        hsaBodyEnergy = hsaBodyEnergy + other.hsaBodyEnergy,
    )

    fun toDailySleep(): FitDailySleep? {
        val summary = FitDailySleep(
            endTime = dailySleepEndTime,
            score = dailySleepScore,
            awakeDuration = dailySleepAwakeSeconds?.let { Duration.ofSeconds(it.toLong()) },
            pressure = dailySleepPressure,
        )
        return if (summary.isEmpty) null else summary
    }

    fun toHealthSnapshot(): FitHealthSnapshot? {
        val snapshot = FitHealthSnapshot(
            spo2 = hsaSpo2,
            respiration = hsaRespiration,
            stress = hsaStress,
            bodyEnergy = hsaBodyEnergy,
        )
        return if (snapshot.isEmpty) null else snapshot
    }

    fun toSleepDemand(): FitSleepDemand? {
        val summary = FitSleepDemand(
            time = sleepDemandTime,
            normal = sleepDemandNormalMinutes?.let { Duration.ofMinutes(it.toLong()) },
            demand = sleepDemandMinutes?.let { Duration.ofMinutes(it.toLong()) },
        )
        return if (summary.isEmpty) null else summary
    }

    fun toSummary(): FitMetricsSummary? {
        val summary = FitMetricsSummary(
            time = time,
            vo2Max = vo2Max,
            recoveryTimeMinutes = recoveryTimeMinutes,
            trainingReadiness = trainingReadiness,
            trainingLoadAcute = trainingLoadAcute,
            trainingLoadChronic = trainingLoadChronic,
        )
        return if (summary.isEmpty) null else summary
    }
}

private fun fitSleepLevelFromRaw(raw: Int): FitSleepLevel? = when (raw) {
    0 -> FitSleepLevel.UNMEASURABLE
    1 -> FitSleepLevel.AWAKE
    2 -> FitSleepLevel.LIGHT
    3 -> FitSleepLevel.DEEP
    4 -> FitSleepLevel.REM
    else -> null
}

/**
 * Walks a (possibly chained) FIT byte stream through the generic [FitDecoder]
 * and interprets each file's messages into the Garmin wellness raw structs.
 * One [GarminWellnessInterpreter] per file; the results merge across the
 * stream so a later file falls back to — rather than concatenates with — an
 * earlier file's one-per-file scalar fields.
 */
private class GarminWellnessDecoder(private val fileBytes: ByteArray) {

    fun decode(): FitWellnessResult {
        var result = FitWellnessResult(
            fileType = null,
            sleep = FitSleepRaw(),
            hrv = FitHrvRaw(),
            monitoring = FitMonitoringRaw(),
            metrics = FitMetricsRaw(),
        )
        var offset = 0
        var decodedAnyFile = false

        while (offset < fileBytes.size) {
            if (!FitDecoder.isFitFileAt(fileBytes, offset)) {
                if (!decodedAnyFile) {
                    throw IllegalArgumentException("FIT file header is invalid.")
                }
                break
            }
            val file = FitDecoder.readFile(fileBytes, offset)
            result = result.merge(GarminWellnessInterpreter().interpret(file.messages))
            decodedAnyFile = true
            offset = file.nextOffset
        }
        return result
    }
}

/**
 * Interprets one file's decoded [FitMessage]s into the Garmin wellness raw
 * structs. Its switch cases are disjoint from the activity interpreter's, so
 * an activity file simply yields empty carriers here.
 */
private class GarminWellnessInterpreter {
    private var fileType: Int? = null

    // Sleep (file type 49).
    private var sleepStart: Instant? = null
    private var sleepStop: Instant? = null
    private val sleepLevels = mutableListOf<Pair<Instant, Int>>()
    private var sleepOverallScore: Int? = null
    private var sleepAwakenings: Int? = null
    private val naps = mutableListOf<FitNap>()

    // Health Snapshot (file type 70): dense sample arrays, one recording.
    private val hsaSpo2 = mutableListOf<Pair<Instant, Int>>()
    private val hsaRespiration = mutableListOf<Pair<Instant, Double>>()
    private val hsaStress = mutableListOf<Pair<Instant, Int>>()
    private val hsaBodyEnergy = mutableListOf<Pair<Instant, Int>>()

    // daily_sleep / sleep_demand, which share the metrics file.
    private var dailySleepEndTime: Instant? = null
    private var dailySleepScore: Int? = null
    private var dailySleepAwakeSeconds: Int? = null
    private var dailySleepPressure: Int? = null
    private var sleepDemandTime: Instant? = null
    private var sleepDemandNormalMinutes: Int? = null
    private var sleepDemandMinutes: Int? = null

    // Metrics (file type 44): four one-per-file snapshots, last seen wins.
    private var metricsTime: Instant? = null
    private var vo2Max: Double? = null
    private var recoveryTimeMinutes: Int? = null
    private var trainingReadiness: Int? = null
    private var trainingLoadAcute: Int? = null
    private var trainingLoadChronic: Int? = null

    // HRV (file type 68): the last `hrv_status_summary.last_night_average` seen.
    private var hrvTime: Instant? = null
    private var hrvRmssdMillis: Double? = null

    // Monitoring (file type 32): the last one-per-file summary values seen.
    private var restingHrTime: Instant? = null
    private var restingHrBpm: Int? = null
    private var bmrTime: Instant? = null
    private var bmrKcalPerDay: Double? = null

    // Monitoring high-frequency series, and the running full timestamp used to
    // reconstruct each message's `timestamp_16`.
    private var monLastTimestampRaw: Long? = null
    private val monHeartRate = mutableListOf<Pair<Instant, Int>>()
    private val respiration = mutableListOf<Pair<Instant, Double>>()
    private val stress = mutableListOf<Pair<Instant, Int>>()
    private val bodyEnergy = mutableListOf<Pair<Instant, Int>>()
    private val monSteps = mutableListOf<FitMonitoringPoint>()
    private val monDistance = mutableListOf<FitMonitoringPoint>()
    private val monCalories = mutableListOf<FitMonitoringPoint>()
    private val monModerateMinutes = mutableListOf<Pair<Instant, Int>>()
    private val monVigorousMinutes = mutableListOf<Pair<Instant, Int>>()

    fun interpret(messages: List<FitMessage>): FitWellnessResult {
        // Dispatch in file order, so cases that depend on an earlier message
        // (monitoring_info before its series) still see it.
        messages.forEach(::dispatch)
        return FitWellnessResult(
            fileType = fileType,
            sleep = FitSleepRaw(
                start = sleepStart,
                stop = sleepStop,
                levels = sleepLevels,
                overallScore = sleepOverallScore,
                awakeningsCount = sleepAwakenings,
                naps = naps,
            ),
            hrv = FitHrvRaw(time = hrvTime, rmssdMillis = hrvRmssdMillis),
            monitoring = FitMonitoringRaw(
                restingHrTime = restingHrTime,
                restingHrBpm = restingHrBpm,
                bmrTime = bmrTime,
                bmrKcalPerDay = bmrKcalPerDay,
                heartRate = monHeartRate,
                respiration = respiration,
                stress = stress,
                bodyEnergy = bodyEnergy,
                steps = monSteps,
                distance = monDistance,
                calories = monCalories,
                moderateMinutes = monModerateMinutes,
                vigorousMinutes = monVigorousMinutes,
            ),
            metrics = FitMetricsRaw(
                time = metricsTime,
                vo2Max = vo2Max,
                recoveryTimeMinutes = recoveryTimeMinutes,
                trainingReadiness = trainingReadiness,
                trainingLoadAcute = trainingLoadAcute,
                trainingLoadChronic = trainingLoadChronic,
                dailySleepEndTime = dailySleepEndTime,
                dailySleepScore = dailySleepScore,
                dailySleepAwakeSeconds = dailySleepAwakeSeconds,
                dailySleepPressure = dailySleepPressure,
                sleepDemandTime = sleepDemandTime,
                sleepDemandNormalMinutes = sleepDemandNormalMinutes,
                sleepDemandMinutes = sleepDemandMinutes,
                hsaSpo2 = hsaSpo2,
                hsaRespiration = hsaRespiration,
                hsaStress = hsaStress,
                hsaBodyEnergy = hsaBodyEnergy,
            ),
        )
    }

    private fun dispatch(message: FitMessage) {
        val values = message.values
        val arrays = message.arrays
        val messageTimestamp = message.timestamp
        when (message.globalMessageNumber) {
            FitFileIdMessageNumber -> {
                // Only the file type is needed here — it tells the caller
                // whether the file was wellness at all. Everything else on
                // file_id is the activity parser's concern.
                fileType = values[FitFileIdTypeFieldNumber]?.toInt() ?: fileType
            }

            FitEventMessageNumber -> {
                // Only the sleep event (Garmin-proprietary value 74) bounds a
                // night; every other event (timer, lap, …) that an activity
                // file carries is ignored here.
                if (values[FitEventFieldNumber] == FitSleepEventValue.toLong() &&
                    messageTimestamp != null
                ) {
                    val at = fitInstant(messageTimestamp)
                    when (values[FitEventTypeFieldNumber]?.toInt()) {
                        FitEventTypeStart -> if (sleepStart == null) sleepStart = at
                        FitEventTypeStop -> sleepStop = at
                    }
                }
            }

            FitSleepLevelMessageNumber -> {
                val level = values[FitSleepLevelFieldNumber]
                if (level != null && messageTimestamp != null) {
                    sleepLevels.add(fitInstant(messageTimestamp) to level.toInt())
                }
            }

            FitHrvStatusSummaryMessageNumber -> {
                val raw = values[FitHrvLastNightAverageFieldNumber]
                if (raw != null && raw != FitUint16Invalid && messageTimestamp != null) {
                    hrvTime = fitInstant(messageTimestamp)
                    hrvRmssdMillis = raw / FitHrvRmssdScale
                }
            }

            FitMonitoringHrDataMessageNumber -> {
                val bpm = values[FitRestingHeartRateFieldNumber]
                if (bpm != null && bpm != FitUint8Invalid && bpm > 0) {
                    restingHrBpm = bpm.toInt()
                    if (messageTimestamp != null) {
                        restingHrTime = fitInstant(messageTimestamp)
                    }
                }
            }

            FitMonitoringInfoMessageNumber -> {
                // monitoring_info carries a full timestamp that anchors the
                // following messages' timestamp_16 values.
                if (messageTimestamp != null) monLastTimestampRaw = messageTimestamp
                val rmr = values[FitRestingMetabolicRateFieldNumber]
                if (rmr != null && rmr != FitUint16Invalid && rmr > 0) {
                    bmrKcalPerDay = rmr.toDouble()
                    if (messageTimestamp != null) {
                        bmrTime = fitInstant(messageTimestamp)
                    }
                }
            }

            FitMonitoringMessageNumber -> readMonitoring(values, messageTimestamp)

            FitStressLevelMessageNumber -> {
                // The stress message carries BOTH the stress score and Body
                // Battery — Body Battery has no message of its own. Its own
                // timestamp field is preferred over the record header's, as
                // Gadgetbridge does.
                val stressTimeRaw = values[FitStressLevelTimeFieldNumber] ?: messageTimestamp
                if (stressTimeRaw != null) {
                    val at = fitInstant(stressTimeRaw)
                    val stressValue = values[FitStressLevelValueFieldNumber]
                    // Negative is Garmin's "not measurable" (asleep, moving,
                    // poor contact), not a low score — dropped rather than
                    // clamped to 0.
                    if (stressValue != null && stressValue in 0..100) {
                        stress.add(at to stressValue.toInt())
                    }
                    val energy = values[FitStressBodyEnergyFieldNumber]
                    if (energy != null && energy in 0..100) {
                        bodyEnergy.add(at to energy.toInt())
                    }
                }
            }

            FitSleepStatsMessageNumber -> {
                val score = values[FitOverallSleepScoreFieldNumber]
                if (score != null && score != FitUint8Invalid && score <= 100) {
                    sleepOverallScore = score.toInt()
                }
                val awakenings = values[FitAwakeningsCountFieldNumber]
                if (awakenings != null && awakenings != FitUint8Invalid) {
                    sleepAwakenings = awakenings.toInt()
                }
            }

            FitNapMessageNumber -> {
                val napStart = values[FitNapStartFieldNumber]
                val napEnd = values[FitNapEndFieldNumber]
                if (napStart != null && napEnd != null && napEnd > napStart) {
                    naps.add(
                        FitNap(
                            start = fitInstant(napStart),
                            end = fitInstant(napEnd),
                        ),
                    )
                }
            }

            FitHsaSpo2MessageNumber,
            FitHsaStressMessageNumber,
            FitHsaRespirationMessageNumber,
            FitHsaBodyBatteryMessageNumber -> readHsaSamples(
                message.globalMessageNumber,
                values,
                arrays,
                messageTimestamp,
            )

            FitDailySleepMessageNumber -> {
                val dailyScore = values[FitDailySleepScoreFieldNumber]
                if (dailyScore != null && dailyScore != FitUint8Invalid && dailyScore <= 100) {
                    dailySleepScore = dailyScore.toInt()
                }
                val awake = values[FitDailySleepAwakeDurationFieldNumber]
                if (awake != null && awake != FitUint16Invalid) {
                    dailySleepAwakeSeconds = awake.toInt()
                }
                val endRaw = values[FitDailySleepEndTimeFieldNumber]
                if (endRaw != null) dailySleepEndTime = fitInstant(endRaw)
                val pressure = values[FitDailySleepPressureFieldNumber]
                if (pressure != null && pressure != FitSint16Invalid) {
                    dailySleepPressure = pressure.toInt()
                }
            }

            FitSleepDemandMessageNumber -> {
                val normal = values[FitSleepDemandNormalFieldNumber]
                if (normal != null && normal != FitUint16Invalid) {
                    sleepDemandNormalMinutes = normal.toInt()
                }
                val demand = values[FitSleepDemandDemandFieldNumber]
                if (demand != null && demand != FitUint16Invalid) {
                    sleepDemandMinutes = demand.toInt()
                }
                if (messageTimestamp != null) {
                    sleepDemandTime = fitInstant(messageTimestamp)
                }
            }

            FitMaxMetDataMessageNumber -> {
                val vo2 = values[FitVo2MaxFieldNumber]
                if (vo2 != null && vo2 != FitUint16Invalid && vo2 > 0) {
                    vo2Max = vo2 / FitVo2MaxScale
                    if (messageTimestamp != null) {
                        metricsTime = fitInstant(messageTimestamp)
                    }
                }
            }

            FitTrainingReadinessMessageNumber -> {
                val readiness = values[FitTrainingReadinessFieldNumber]
                if (readiness != null && readiness != FitUint8Invalid && readiness <= 100) {
                    trainingReadiness = readiness.toInt()
                    if (messageTimestamp != null && metricsTime == null) {
                        metricsTime = fitInstant(messageTimestamp)
                    }
                }
            }

            FitTrainingLoadMessageNumber -> {
                val acute = values[FitTrainingLoadAcuteFieldNumber]
                if (acute != null && acute != FitUint16Invalid) {
                    trainingLoadAcute = acute.toInt()
                }
                val chronic = values[FitTrainingLoadChronicFieldNumber]
                if (chronic != null && chronic != FitUint16Invalid) {
                    trainingLoadChronic = chronic.toInt()
                }
                if (messageTimestamp != null && metricsTime == null) {
                    metricsTime = fitInstant(messageTimestamp)
                }
            }

            FitPhysiologicalMetricsMessageNumber -> {
                // Only recovery_time is taken. This message also carries VO2
                // max under a different scale, but max_met_data above is the
                // one the watch keeps current, and reading both would let a
                // stale copy win at random.
                val recovery = values[FitRecoveryTimeFieldNumber]
                if (recovery != null && recovery != FitUint16Invalid) {
                    recoveryTimeMinutes = recovery.toInt()
                    if (messageTimestamp != null && metricsTime == null) {
                        metricsTime = fitInstant(messageTimestamp)
                    }
                }
            }

            FitRespirationRateMessageNumber -> {
                val rateRaw = values[FitRespirationRateFieldNumber]
                if (rateRaw != null && messageTimestamp != null) {
                    val rate = rateRaw / FitRespirationScale
                    // Negative / zero is the "not measuring" sentinel.
                    if (rate > 0 && rate < 100) {
                        respiration.add(fitInstant(messageTimestamp) to rate)
                    }
                }
            }
        }
    }

    /**
     * One Health Snapshot message: a whole recording packed into one record.
     *
     * The samples are laid out FORWARD from the record's timestamp, `interval`
     * seconds apart. That is an assumption — nothing documents it and
     * Gadgetbridge never parses these — so the shape is logged on every
     * record: compare the printed span against the Health Snapshot on the
     * watch, and if the window is shifted by its own length, the timestamp
     * marks the END and this needs inverting.
     */
    private fun readHsaSamples(
        messageNumber: Int,
        values: Map<Int, Long>,
        arrays: Map<Int, List<Long>>,
        messageTimestamp: Long?,
    ) {
        if (messageTimestamp == null) return
        val samples = arrays[FitHsaValueFieldNumber] ?: emptyList()
        if (samples.isEmpty()) return
        // A zero or missing interval would stack every sample on one instant.
        val interval = values[FitHsaIntervalFieldNumber] ?: 0L
        if (interval <= 0L) {
            GarminLog.logLazy {
                "[FIT-HSA] message $messageNumber: ${samples.size} samples " +
                    "with no usable interval ($interval) — dropped"
            }
            return
        }
        val start = fitInstant(messageTimestamp)
        for (i in samples.indices) {
            val at = start.plusSeconds(interval * i)
            val raw = samples[i]
            when (messageNumber) {
                FitHsaSpo2MessageNumber ->
                    if (raw in 1..100) hsaSpo2.add(at to raw.toInt())
                FitHsaStressMessageNumber ->
                    // Negative is Garmin's "not measurable", as in stress_level (227).
                    if (raw in 0..100) hsaStress.add(at to raw.toInt())
                FitHsaRespirationMessageNumber -> {
                    val rate = raw / FitHsaRespirationScale
                    if (rate > 0 && rate < 100) hsaRespiration.add(at to rate)
                }
                FitHsaBodyBatteryMessageNumber ->
                    if (raw in 0..100) hsaBodyEnergy.add(at to raw.toInt())
            }
        }
        GarminLog.logLazy {
            "[FIT-HSA] message $messageNumber: ${samples.size} samples " +
                "every ${interval}s from $start " +
                "spanning ${interval * (samples.size - 1)}s " +
                "(first=${samples.first()} last=${samples.last()})"
        }
    }

    /**
     * One `monitoring` message: resolve its timestamp (full or `timestamp_16`
     * relative to the running anchor) and pull HR + the cumulative counters.
     */
    private fun readMonitoring(values: Map<Int, Long>, fullTimestamp: Long?) {
        val tsRaw: Long?
        if (fullTimestamp != null) {
            tsRaw = fullTimestamp
            monLastTimestampRaw = fullTimestamp
        } else {
            val ts16 = values[FitMonitoringTimestamp16FieldNumber]
            val anchor = monLastTimestampRaw
            tsRaw = if (ts16 != null && anchor != null) {
                // Roll the low 16 bits forward from the anchor (FIT timestamp_16).
                val rolled = anchor + ((ts16 - (anchor and 0xFFFF)) and 0xFFFF)
                monLastTimestampRaw = rolled
                rolled
            } else {
                null
            }
        }
        if (tsRaw == null) return
        val time = fitInstant(tsRaw)

        val hr = values[FitMonitoringHeartRateFieldNumber]
        if (hr != null && hr != FitUint8Invalid && hr > 0) {
            monHeartRate.add(time to hr.toInt())
        }
        val intensityByte = values[FitMonitoringActivityTypeIntensityFieldNumber]
        val activityType = values[FitMonitoringActivityTypeFieldNumber]?.toInt()
            ?: if (intensityByte != null) {
                (intensityByte and FitMonitoringActivityTypeMask).toInt()
            } else {
                UNKNOWN_FIT_ACTIVITY_TYPE
            }
        val steps = values[FitMonitoringStepsFieldNumber]
        if (steps != null) {
            monSteps.add(
                FitMonitoringPoint(time = time, activityType = activityType, value = steps.toInt()),
            )
        }
        val distance = values[FitMonitoringDistanceFieldNumber]
        if (distance != null) {
            monDistance.add(
                FitMonitoringPoint(time = time, activityType = activityType, value = distance.toInt()),
            )
        }
        val calories = values[FitMonitoringActiveCaloriesFieldNumber]
        if (calories != null) {
            monCalories.add(
                FitMonitoringPoint(time = time, activityType = activityType, value = calories.toInt()),
            )
        }
        val moderate = values[FitMonitoringModerateMinutesFieldNumber]
            ?: values[FitMonitoringModerateMinutesAltFieldNumber]
        if (moderate != null && moderate != FitUint16Invalid) {
            monModerateMinutes.add(time to moderate.toInt())
        }
        val vigorous = values[FitMonitoringVigorousMinutesFieldNumber]
            ?: values[FitMonitoringVigorousMinutesAltFieldNumber]
        if (vigorous != null && vigorous != FitUint16Invalid) {
            monVigorousMinutes.add(time to vigorous.toInt())
        }
    }
}

private const val FitFileIdMessageNumber = 0
private const val FitFileIdTypeFieldNumber = 0

// Sleep (Garmin file type 49).
private const val FitEventMessageNumber = 21
private const val FitSleepLevelMessageNumber = 275
private const val FitEventFieldNumber = 0
private const val FitEventTypeFieldNumber = 1
private const val FitSleepLevelFieldNumber = 0
private const val FitSleepEventValue = 74 // `event` == sleep (Garmin-proprietary)
private const val FitEventTypeStart = 0
private const val FitEventTypeStop = 1

// HRV status (Garmin file type 68). `hrv_status_summary.last_night_average`
// (field 1, uint16, scale 128) is the night's RMSSD in ms.
private const val FitHrvStatusSummaryMessageNumber = 370
private const val FitHrvLastNightAverageFieldNumber = 1
private const val FitHrvRmssdScale = 128.0
private const val FitUint16Invalid = 0xFFFFL

// Monitoring (Garmin file type 32). One-per-file summaries:
private const val FitMonitoringHrDataMessageNumber = 211
private const val FitRestingHeartRateFieldNumber = 0
private const val FitMonitoringInfoMessageNumber = 103
private const val FitRestingMetabolicRateFieldNumber = 5
private const val FitUint8Invalid = 0xFFL

// Monitoring high-frequency series. `monitoring` (55) carries per-minute HR and
// the cumulative step/distance/calorie counters; `respiration_rate` (297) the
// breathing series. Most `monitoring` messages timestamp with `timestamp_16`
// (field 26) — the low 16 bits relative to the last full timestamp — not a full
// `timestamp` (253).
private const val FitMonitoringMessageNumber = 55
private const val FitRespirationRateMessageNumber = 297

// stress_level (227) carries the stress score AND Body Battery; the latter has
// no message of its own. Field numbers from Gadgetbridge's
// AbstractFitStressLevel (AGPLv3).
private const val FitStressLevelMessageNumber = 227
private const val FitStressLevelValueFieldNumber = 0 // sint8, 0..100 (negative = n/a)
private const val FitStressLevelTimeFieldNumber = 1 // uint32, Garmin epoch seconds
private const val FitStressBodyEnergyFieldNumber = 3 // uint8, 0..100
private const val FitMonitoringDistanceFieldNumber = 2 // uint32, ÷100 m, cumulative
private const val FitMonitoringStepsFieldNumber = 3 // uint32, raw == steps (walk/run)
private const val FitMonitoringActivityTypeFieldNumber = 5
private const val FitMonitoringActiveCaloriesFieldNumber = 19 // uint16, cumulative

// current_activity_type_intensity (byte): activity_type in the low 5 bits. Most
// monitoring messages carry the type here, not in field 5.
private const val FitMonitoringActivityTypeIntensityFieldNumber = 24
private const val FitMonitoringActivityTypeMask = 0x1FL
private const val FitMonitoringTimestamp16FieldNumber = 26
private const val FitMonitoringHeartRateFieldNumber = 27 // uint8, bpm
private const val FitRespirationRateFieldNumber = 0 // sint16, ÷100 breaths/min

// Intensity minutes. Garmin writes the running daily totals into 37/38 on this
// watch; 33/34 are the same quantity under the names the FIT profile documents.
// Both are read, later-wins, because which pair a device populates varies.
private const val FitMonitoringModerateMinutesFieldNumber = 37 // uint16, minutes
private const val FitMonitoringVigorousMinutesFieldNumber = 38 // uint16, minutes
private const val FitMonitoringModerateMinutesAltFieldNumber = 33
private const val FitMonitoringVigorousMinutesAltFieldNumber = 34

// Metrics (Garmin file type 44). Four unrelated messages share the file; each
// is a one-per-file snapshot rather than a series, so the last seen wins.
private const val FitMaxMetDataMessageNumber = 229
private const val FitVo2MaxFieldNumber = 2 // uint16, scale 10, mL/kg/min
private const val FitVo2MaxScale = 10.0
private const val FitTrainingReadinessMessageNumber = 369
private const val FitTrainingReadinessFieldNumber = 0 // uint8, 0..100
private const val FitTrainingLoadMessageNumber = 378
private const val FitTrainingLoadAcuteFieldNumber = 3 // uint16
private const val FitTrainingLoadChronicFieldNumber = 4 // uint16
private const val FitPhysiologicalMetricsMessageNumber = 140
private const val FitRecoveryTimeFieldNumber = 9 // uint16, minutes

// daily_sleep (384) and sleep_demand (410) — what a vívoactive 5 actually puts
// in its metrics file, in place of the training-load messages other Garmins
// use. This is the watch's own verdict on a night, computed on the wrist.
private const val FitDailySleepMessageNumber = 384
private const val FitDailySleepScoreFieldNumber = 2 // uint8, 0..100

// awake_duration is in SECONDS, not the minutes Garmin's FIT profile claims: a
// real night read 1020 here inside an 8.7-hour window, and 1020 minutes is 17
// hours. Reading it as minutes would report more time awake than time in bed.
private const val FitDailySleepAwakeDurationFieldNumber = 3 // uint16, seconds
private const val FitDailySleepEndTimeFieldNumber = 11 // uint32, Garmin epoch
private const val FitDailySleepPressureFieldNumber = 22 // sint16
private const val FitSleepDemandMessageNumber = 410
private const val FitSleepDemandNormalFieldNumber = 0 // uint16, minutes
private const val FitSleepDemandDemandFieldNumber = 1 // uint16, minutes
private const val FitSint16Invalid = 0x7FFFL

// Health Snapshot (Garmin file type 70). Each message packs a whole recording
// into ONE record: field 0 is the seconds between samples and field 1 (plus 2/3
// for Body Battery) is an ARRAY of readings. Gadgetbridge pulls this file and
// never parses it, so there is no port to follow and nothing documents how the
// samples line up against the record timestamp — see the diagnostic in
// [GarminWellnessInterpreter.readHsaSamples].
private const val FitHsaSpo2MessageNumber = 305
private const val FitHsaStressMessageNumber = 306
private const val FitHsaRespirationMessageNumber = 307
private const val FitHsaBodyBatteryMessageNumber = 314
private const val FitHsaIntervalFieldNumber = 0 // uint16, seconds between samples
private const val FitHsaValueFieldNumber = 1 // array of readings
private const val FitHsaRespirationScale = 100.0

// Sleep extras, in the same type-49 file the stage transitions come from.
// sleep_stats (346) is the watch's OWN assessment of the night — the scores it
// shows on the wrist — which is independent of the stages we derive ourselves.
private const val FitSleepStatsMessageNumber = 346
private const val FitOverallSleepScoreFieldNumber = 6 // uint8, 0..100
private const val FitAwakeningsCountFieldNumber = 11 // uint8

// nap (412) bounds a daytime sleep with its own start/end, separate from the
// night's event/74 pair.
private const val FitNapMessageNumber = 412
private const val FitNapStartFieldNumber = 0 // uint32, Garmin epoch seconds
private const val FitNapEndFieldNumber = 2 // uint32, Garmin epoch seconds
private const val FitRespirationScale = 100.0
