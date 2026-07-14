package tech.mmarca.openvitals.health_connect_native

import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ported from the native OpenVitals app (`healthconnect/ActivityHealthReader.kt`
 * + the `toExerciseData` mapper). Covers the record-based activity surface —
 * exercise sessions (read/write/update/delete) and raw speed samples. The
 * aggregate reads (steps/distance/floors/daily/calories) stay on the generic
 * aggregate ops in the data source. Exercise-session dedup stays in Dart.
 */
internal class ActivityHealthReader(
  private val support: HealthConnectReaderSupport,
  private val appPackageName: String,
) {
  suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseDataMsg> =
    support.withLogging("readExerciseSessions[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = ExerciseSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 50,
      ).map { it.toMsg() }
    }

  /**
   * Port of the native app's `ActivityHealthReader.readExerciseSessionsWithMetrics`.
   *
   * `ExerciseSessionRecord` itself carries no distance/speed — those live in
   * sibling `DistanceRecord` / `SpeedRecord` series, so the only way to attach
   * them to a session is to aggregate over the session's own window. One
   * `AggregateRequest` per session, with only the metrics the caller holds a
   * read permission for; an aggregate failure degrades that session to null
   * metrics rather than failing the whole read (rate limits still propagate so
   * [HealthConnectReaderSupport] can wait them out and retry).
   */
  suspend fun readExerciseSessionsWithMetrics(
    start: Instant,
    end: Instant,
    includeDistance: Boolean,
    includeSpeed: Boolean,
  ): List<ExerciseDataMsg> =
    support.withLogging("readExerciseSessionsWithMetrics[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = ExerciseSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = false,
        pageSize = 50,
      ).map { record ->
        readExerciseSessionMetrics(
          record = record,
          includeDistance = includeDistance,
          includeSpeed = includeSpeed,
        )
      }
    }

  private suspend fun readExerciseSessionMetrics(
    record: ExerciseSessionRecord,
    includeDistance: Boolean,
    includeSpeed: Boolean,
  ): ExerciseDataMsg {
    val metrics = buildSet {
      if (includeDistance) add(DistanceRecord.DISTANCE_TOTAL)
      if (includeSpeed) add(SpeedRecord.SPEED_AVG)
    }
    val aggregate = if (metrics.isEmpty()) {
      null
    } else {
      runCatching {
        support.client().aggregate(
          AggregateRequest(
            metrics = metrics,
            timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime),
          ),
        )
      }.onFailure {
        if (HealthConnectRateLimitBackoff.isRateLimitFailure(it)) throw it
        Log.e(TAG, "Failed readExerciseSessionMetrics aggregate ${support.diagnosticsSummary()}", it)
      }.getOrNull()
    }
    return record.toMsg(
      totalDistanceMeters = if (includeDistance && aggregate != null) {
        aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
      } else {
        null
      },
      averageSpeedMetersPerSecond = if (includeSpeed && aggregate != null) {
        aggregate[SpeedRecord.SPEED_AVG]?.inMetersPerSecond
      } else {
        null
      },
    )
  }

  suspend fun readExerciseSessionById(id: String): ExerciseDataMsg? =
    support.withNullableLogging("readExerciseSessionById[$id]") {
      support.client().readRecord(ExerciseSessionRecord::class, id).record.toMsg()
    }

  /**
   * Every sibling-record total for one session's window.
   *
   * A watch records a walk as an `ExerciseSessionRecord` plus separate Steps /
   * Distance / Calories / Elevation records covering the same span, so the
   * session on its own knows nothing but its duration. Aggregating its window is
   * the only way to get the rest back, and it is what the activity detail screen
   * needs in order to stop reporting "Not available" for numbers the watch did
   * in fact record.
   *
   * [metrics] carries only what the caller holds a read permission for. An
   * unknown name is skipped rather than throwing, so an older host stays
   * compatible with a newer caller. An aggregate failure degrades to null metrics
   * rather than failing the read (rate limits still propagate, so
   * [HealthConnectReaderSupport] can wait them out and retry).
   */
  suspend fun readExerciseSessionMetrics(
    start: Instant,
    end: Instant,
    metrics: List<String>,
  ): ExerciseSessionMetricsMsg =
    support.withLogging(
      "readExerciseSessionMetrics[$start..$end]",
      EMPTY_SESSION_METRICS,
    ) {
      val requested = metrics.mapNotNull { SESSION_METRICS[it] }.toSet()
      if (requested.isEmpty() || !end.isAfter(start)) return@withLogging EMPTY_SESSION_METRICS

      val aggregate = runCatching {
        support.client().aggregate(
          AggregateRequest(
            metrics = requested,
            timeRangeFilter = TimeRangeFilter.between(start, end),
          ),
        )
      }.onFailure {
        if (HealthConnectRateLimitBackoff.isRateLimitFailure(it)) throw it
        Log.e(TAG, "Failed readExerciseSessionMetrics aggregate ${support.diagnosticsSummary()}", it)
      }.getOrNull() ?: return@withLogging EMPTY_SESSION_METRICS

      // Only report a metric that was actually asked for: a total absent from an
      // aggregate the caller never requested is "unknown", not "zero".
      fun <T : Any> read(metric: AggregateMetric<T>): T? =
        if (metric in requested) aggregate[metric] else null

      ExerciseSessionMetricsMsg(
        totalDistanceMeters = read(DistanceRecord.DISTANCE_TOTAL)?.inMeters,
        averageSpeedMetersPerSecond = read(SpeedRecord.SPEED_AVG)?.inMetersPerSecond,
        steps = read(StepsRecord.COUNT_TOTAL),
        totalCaloriesKcal = read(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
        activeCaloriesKcal =
          read(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories,
        elevationGainedMeters =
          read(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters,
        floorsClimbed = read(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)?.toLong(),
        wheelchairPushes = read(WheelchairPushesRecord.COUNT_TOTAL),
        averagePowerWatts = read(PowerRecord.POWER_AVG)?.inWatts,
      )
    }

  suspend fun readSpeedSamples(start: Instant, end: Instant): List<SpeedSampleMsg> =
    support.withLogging("readSpeedSamples[$start..$end]", emptyList()) {
      support.client().readSeriesSamples(SpeedRecord::class, start, end) { record ->
        val source = record.metadata.dataOrigin.packageName
        record.samples.map { sample ->
          TimedSample(
            sample.time,
            SpeedSampleMsg(
              timeEpochMs = sample.time.toEpochMilli(),
              metersPerSecond = sample.speed.inMetersPerSecond,
              source = source,
            ),
          )
        }
      }
    }

  /**
   * Cycling-pedaling and steps cadence samples, merged into one time-ordered
   * list. The two records share a shape but not a unit (rpm vs steps/min), so
   * each sample carries [ActivityCadenceSampleMsg.isCycling] to say which.
   */
  suspend fun readActivityCadenceSamples(
    start: Instant,
    end: Instant,
  ): List<ActivityCadenceSampleMsg> =
    support.withLogging("readActivityCadenceSamples[$start..$end]", emptyList()) {
      val cycling = support.client()
        .readSeriesSamples(CyclingPedalingCadenceRecord::class, start, end) { record ->
          val source = record.metadata.dataOrigin.packageName
          record.samples.map { sample ->
            TimedSample(
              sample.time,
              ActivityCadenceSampleMsg(
                timeEpochMs = sample.time.toEpochMilli(),
                rate = sample.revolutionsPerMinute,
                isCycling = true,
                source = source,
              ),
            )
          }
        }
      val steps = support.client()
        .readSeriesSamples(StepsCadenceRecord::class, start, end) { record ->
          val source = record.metadata.dataOrigin.packageName
          record.samples.map { sample ->
            TimedSample(
              sample.time,
              ActivityCadenceSampleMsg(
                timeEpochMs = sample.time.toEpochMilli(),
                rate = sample.rate,
                isCycling = false,
                source = source,
              ),
            )
          }
        }
      (cycling + steps).sortedBy { it.timeEpochMs }
    }

  suspend fun readPlannedExerciseSessions(
    start: Instant,
    end: Instant,
  ): List<PlannedExerciseSessionMsg> =
    support.withLogging("readPlannedExerciseSessions[$start..$end]", emptyList()) {
      support.client().readRecordsPaged(
        recordType = PlannedExerciseSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(start, end),
        ascendingOrder = true,
        pageSize = 100,
      ).map { record ->
        PlannedExerciseSessionMsg(
          id = record.metadata.id,
          title = record.title,
          exerciseType = record.exerciseType.toLong(),
          startEpochMs = record.startTime.toEpochMilli(),
          endEpochMs = record.endTime.toEpochMilli(),
          hasExplicitTime = record.hasExplicitTime,
          completedExerciseSessionId = record.completedExerciseSessionId,
          notes = record.notes,
          source = record.metadata.dataOrigin.packageName,
          blocks = record.blocks.map { it.toMsg() },
        )
      }
    }

  /**
   * Health Connect cannot update a planned session in place, so an edit deletes
   * the old record and inserts a replacement.
   */
  suspend fun writePlannedExerciseSession(request: PlannedExerciseWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      request.id?.let { existingId ->
        support.client().deleteRecords(
          recordType = PlannedExerciseSessionRecord::class,
          recordIdsList = listOf(existingId),
          clientRecordIdsList = emptyList(),
        )
      }
      val zone = ZoneId.systemDefault()
      val startTime = Instant.ofEpochMilli(request.startEpochMs)
      val endTime = Instant.ofEpochMilli(request.endEpochMs)
      val record = PlannedExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = zone.rules.getOffset(startTime),
        endTime = endTime,
        endZoneOffset = zone.rules.getOffset(endTime),
        metadata = Metadata.manualEntry(
          clientRecordId = "openvitals_planned_activity_${startTime.toEpochMilli()}_${UUID.randomUUID()}",
          device = Device(type = Device.TYPE_PHONE),
        ),
        blocks = request.blocks.map { it.toRecord() },
        exerciseType = request.exerciseType.toInt(),
        title = request.title?.trim()?.takeIf { it.isNotBlank() },
        notes = request.notes?.trim()?.takeIf { it.isNotBlank() },
      )
      support.client()
        .insertRecords(listOf(record))
        .recordIdsList
        .firstOrNull()
        ?: record.metadata.clientRecordId.orEmpty()
    }

  suspend fun writeActivityEntry(request: ActivityWriteRequestMsg): String =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val startTime = Instant.ofEpochMilli(request.startEpochMs)
      val clientRecordId = "openvitals_activity_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
      val metadata = Metadata.manualEntry(
        clientRecordId = clientRecordId,
        device = Device(type = Device.TYPE_PHONE),
      )
      val extraRecords = request.toManualActivityMetricRecords(ZoneId.systemDefault())
      Log.d(
        TAG,
        "Writing exercise session extras=${extraRecords.size} ${support.diagnosticsSummary()}",
      )
      support.client().insertRecords(listOf(buildSession(request, metadata)) + extraRecords)
      clientRecordId
    }

  /**
   * Writes several activities in ONE Health Connect call.
   *
   * Health Connect charges its rate limit per API CALL, not per record: a quota
   * failure reads `requested: 1` no matter how many records the call carried. A
   * bulk route import that calls [writeActivityEntry] per file therefore spends a
   * unit of quota per file, and a folder of a couple of thousand exhausts the
   * daily allowance -- so the same records go in one call instead.
   *
   * Insertion is atomic: if one record is rejected, none of the batch is written.
   * The caller isolates the offending file by retrying singly (see
   * `RouteBulkImportViewModel`).
   */
  suspend fun writeActivityEntries(requests: List<ActivityWriteRequestMsg>): List<String> =
    withContext(Dispatchers.IO) {
      if (requests.isEmpty()) return@withContext emptyList()
      support.requireSyncEnabled()
      val zone = ZoneId.systemDefault()
      val clientRecordIds = ArrayList<String>(requests.size)
      val records = ArrayList<Record>(requests.size * 2)
      for (request in requests) {
        val startTime = Instant.ofEpochMilli(request.startEpochMs)
        val clientRecordId = "openvitals_activity_${startTime.toEpochMilli()}_${UUID.randomUUID()}"
        val metadata = Metadata.manualEntry(
          clientRecordId = clientRecordId,
          device = Device(type = Device.TYPE_PHONE),
        )
        clientRecordIds.add(clientRecordId)
        records.add(buildSession(request, metadata))
        records.addAll(request.toManualActivityMetricRecords(zone))
      }
      Log.d(
        TAG,
        "Writing ${requests.size} exercise sessions as ${records.size} records in one call " +
          support.diagnosticsSummary(),
      )
      support.client().insertRecords(records)
      clientRecordIds
    }

  suspend fun updateActivityEntry(id: String, request: ActivityWriteRequestMsg) =
    withContext(Dispatchers.IO) {
      support.requireSyncEnabled()
      val existing = support.client().readRecord(ExerciseSessionRecord::class, id).record
      existing.requireOpenVitalsOrigin(appPackageName)
      val metadata = Metadata.manualEntryWithId(
        id = id,
        device = existing.metadata.device ?: Device(type = Device.TYPE_PHONE),
      )
      val extraRecords = request.toManualActivityMetricRecords(ZoneId.systemDefault())
      Log.d(
        TAG,
        "Updating exercise session extras=${extraRecords.size} ${support.diagnosticsSummary()}",
      )
      support.client().updateRecords(listOf(buildSession(request, metadata)))
      deleteManualActivityMetricRecords(existing.startTime, existing.endTime)
      if (extraRecords.isNotEmpty()) {
        support.client().insertRecords(extraRecords)
      }
    }

  suspend fun deleteActivityEntry(id: String) = withContext(Dispatchers.IO) {
    support.requireSyncEnabled()
    val existing = support.client().readRecord(ExerciseSessionRecord::class, id).record
    existing.requireOpenVitalsOrigin(appPackageName)
    deleteManualActivityMetricRecords(existing.startTime, existing.endTime)
    support.client().deleteRecords(
      recordType = ExerciseSessionRecord::class,
      recordIdsList = listOf(existing.metadata.id),
      clientRecordIdsList = emptyList(),
    )
  }

  private fun buildSession(request: ActivityWriteRequestMsg, metadata: Metadata): ExerciseSessionRecord {
    val startTime = Instant.ofEpochMilli(request.startEpochMs)
    val endTime = Instant.ofEpochMilli(request.endEpochMs)
    val zone = ZoneId.systemDefault()
    val segments = request.toExerciseSegments()
    val laps = request.laps.map { l ->
      ExerciseLap(
        startTime = Instant.ofEpochMilli(l.startEpochMs),
        endTime = Instant.ofEpochMilli(l.endEpochMs),
        length = l.lengthMeters?.let { Length.meters(it) },
      )
    }
    val route: ExerciseRoute? = request.routePoints
      .takeIf { it.isNotEmpty() }
      ?.map { p ->
        ExerciseRoute.Location(
          time = Instant.ofEpochMilli(p.timeEpochMs),
          latitude = p.latitude,
          longitude = p.longitude,
          horizontalAccuracy = p.horizontalAccuracyMeters?.let { Length.meters(it) },
          verticalAccuracy = p.verticalAccuracyMeters?.let { Length.meters(it) },
          altitude = p.altitudeMeters?.let { Length.meters(it) },
        )
      }
      ?.let { ExerciseRoute(it) }
    return ExerciseSessionRecord(
      startTime = startTime,
      startZoneOffset = zone.rules.getOffset(startTime),
      endTime = endTime,
      endZoneOffset = zone.rules.getOffset(endTime),
      metadata = metadata,
      exerciseType = request.exerciseType.toInt(),
      title = request.title,
      notes = request.notes,
      segments = segments,
      laps = laps,
      exerciseRoute = route,
      plannedExerciseSessionId = request.plannedExerciseSessionId,
    )
  }

  /**
   * Ported from the native app's `ActivityWriteRequest.toExerciseSegments()`:
   * explicit segments win; otherwise an active segment (typed after the
   * exercise) is synthesized around the recorded pause intervals, with the
   * pauses written as `EXERCISE_SEGMENT_TYPE_PAUSE` segments.
   */
  private fun ActivityWriteRequestMsg.toExerciseSegments(): List<ExerciseSegment> {
    if (segments.isNotEmpty()) {
      return segments
        .sortedBy { it.startEpochMs }
        .map { s ->
          ExerciseSegment(
            startTime = Instant.ofEpochMilli(s.startEpochMs),
            endTime = Instant.ofEpochMilli(s.endEpochMs),
            segmentType = s.segmentType.toInt(),
            repetitions = s.repetitions.toInt(),
          )
        }
    }

    val startTime = Instant.ofEpochMilli(startEpochMs)
    val endTime = Instant.ofEpochMilli(endEpochMs)
    val activeSegmentType = exerciseType.toInt().toActiveExerciseSegmentType()
    val sortedPauses = pauseIntervals.orEmpty().sortedBy { it.startEpochMs }
    return buildList {
      var activeStart = startTime
      sortedPauses.forEach { pause ->
        val pauseStart = Instant.ofEpochMilli(pause.startEpochMs)
        val pauseEnd = Instant.ofEpochMilli(pause.endEpochMs)
        if (activeStart.isBefore(pauseStart)) {
          add(
            ExerciseSegment(
              startTime = activeStart,
              endTime = pauseStart,
              segmentType = activeSegmentType,
            )
          )
        }
        add(
          ExerciseSegment(
            startTime = pauseStart,
            endTime = pauseEnd,
            segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
          )
        )
        if (activeStart.isBefore(pauseEnd)) {
          activeStart = pauseEnd
        }
      }
      if (activeStart.isBefore(endTime)) {
        add(
          ExerciseSegment(
            startTime = activeStart,
            endTime = endTime,
            segmentType = activeSegmentType,
          )
        )
      }
    }
  }

  /**
   * Ported from the native app's `toManualActivityMetricRecords`: the manual
   * totals (distance/elevation/calories/steps) plus the recorded BLE sensor
   * series, written as standalone records alongside the exercise session.
   */
  private fun ActivityWriteRequestMsg.toManualActivityMetricRecords(zone: ZoneId): List<Record> {
    val startTime = Instant.ofEpochMilli(startEpochMs)
    val endTime = Instant.ofEpochMilli(endEpochMs)
    val startOffset = zone.rules.getOffset(startTime)
    val endOffset = zone.rules.getOffset(endTime)
    return buildList {
      distanceMeters?.let { meters ->
        add(
          DistanceRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            distance = Length.meters(meters),
            metadata = manualActivityMetricMetadata("distance", startTime),
          )
        )
      }
      elevationGainedMeters?.let { meters ->
        add(
          ElevationGainedRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            elevation = Length.meters(meters),
            metadata = manualActivityMetricMetadata("elevation", startTime),
          )
        )
      }
      activeCaloriesKcal?.let { kcal ->
        add(
          ActiveCaloriesBurnedRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            energy = Energy.kilocalories(kcal),
            metadata = manualActivityMetricMetadata("active_calories", startTime),
          )
        )
      }
      totalCaloriesKcal?.let { kcal ->
        add(
          TotalCaloriesBurnedRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            energy = Energy.kilocalories(kcal),
            metadata = manualActivityMetricMetadata("total_calories", startTime),
          )
        )
      }
      stepsCount?.let { steps ->
        add(
          StepsRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            count = steps,
            metadata = manualActivityMetricMetadata("steps", startTime),
          )
        )
      }
      bleSamples?.let { samples ->
        addAll(samples.toManualActivitySensorRecords(startTime, endTime, zone))
      }
    }
  }

  /**
   * Ported from the native app's `BleRecordingSampleBuffer.toManualActivitySensorRecords`.
   * Cycling speed and running speed are separate `SpeedRecord`s (clientRecordId
   * kinds `speed` vs `running_speed`), matching the Kotlin split on `isRunning`.
   */
  private fun ActivityBleSamplesMsg.toManualActivitySensorRecords(
    startTime: Instant,
    endTime: Instant,
    zone: ZoneId,
  ): List<Record> {
    if (heartRateSamples.isEmpty() &&
      powerSamples.isEmpty() &&
      cyclingCadenceSamples.isEmpty() &&
      speedSamples.isEmpty() &&
      stepsCadenceSamples.isEmpty()
    ) {
      return emptyList()
    }
    val startOffset = zone.rules.getOffset(startTime)
    val endOffset = zone.rules.getOffset(endTime)
    return buildList {
      if (heartRateSamples.isNotEmpty()) {
        add(
          HeartRateRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = heartRateSamples.map {
              HeartRateRecord.Sample(
                time = Instant.ofEpochMilli(it.timeEpochMs).coerceInSession(startTime, endTime),
                beatsPerMinute = it.beatsPerMinute,
              )
            },
            metadata = manualActivityMetricMetadata("heart_rate", startTime),
          )
        )
      }
      if (powerSamples.isNotEmpty()) {
        add(
          PowerRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = powerSamples.map { sample ->
              PowerRecord.Sample(
                time = Instant.ofEpochMilli(sample.timeEpochMs).coerceInSession(startTime, endTime),
                power = Power.watts(sample.watts),
              )
            },
            metadata = manualActivityMetricMetadata("power", startTime),
          )
        )
      }
      if (cyclingCadenceSamples.isNotEmpty()) {
        add(
          CyclingPedalingCadenceRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = cyclingCadenceSamples.map {
              CyclingPedalingCadenceRecord.Sample(
                time = Instant.ofEpochMilli(it.timeEpochMs).coerceInSession(startTime, endTime),
                revolutionsPerMinute = it.rpm.toDouble(),
              )
            },
            metadata = manualActivityMetricMetadata("cycling_cadence", startTime),
          )
        )
      }
      val cyclingSpeedSamples = speedSamples.filterNot { it.isRunning }
      if (cyclingSpeedSamples.isNotEmpty()) {
        add(
          SpeedRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = cyclingSpeedSamples.map { sample ->
              SpeedRecord.Sample(
                time = Instant.ofEpochMilli(sample.timeEpochMs).coerceInSession(startTime, endTime),
                speed = Velocity.metersPerSecond(sample.metersPerSecond),
              )
            },
            metadata = manualActivityMetricMetadata("speed", startTime),
          )
        )
      }
      val runningSpeedSamples = speedSamples.filter { it.isRunning }
      if (runningSpeedSamples.isNotEmpty()) {
        add(
          SpeedRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = runningSpeedSamples.map { sample ->
              SpeedRecord.Sample(
                time = Instant.ofEpochMilli(sample.timeEpochMs).coerceInSession(startTime, endTime),
                speed = Velocity.metersPerSecond(sample.metersPerSecond),
              )
            },
            metadata = manualActivityMetricMetadata("running_speed", startTime),
          )
        )
      }
      if (stepsCadenceSamples.isNotEmpty()) {
        add(
          StepsCadenceRecord(
            startTime = startTime,
            startZoneOffset = startOffset,
            endTime = endTime,
            endZoneOffset = endOffset,
            samples = stepsCadenceSamples.map {
              StepsCadenceRecord.Sample(
                time = Instant.ofEpochMilli(it.timeEpochMs).coerceInSession(startTime, endTime),
                rate = it.stepsPerMinute.toDouble(),
              )
            },
            metadata = manualActivityMetricMetadata("steps_cadence", startTime),
          )
        )
      }
    }
  }

  private fun Instant.coerceInSession(startTime: Instant, endTime: Instant): Instant =
    when {
      isBefore(startTime) -> startTime
      isAfter(endTime) -> endTime
      else -> this
    }

  /**
   * Deletes the standalone metric/sensor records a previous write attached to
   * an OpenVitals session in [start]..[end], so an edit (or session delete)
   * replaces rather than duplicates them. Ported from the native app.
   */
  private suspend fun deleteManualActivityMetricRecords(start: Instant, end: Instant) {
    deleteManualActivityMetricRecords(StepsRecord::class, "steps", start, end)
    deleteManualActivityMetricRecords(DistanceRecord::class, "distance", start, end)
    deleteManualActivityMetricRecords(ElevationGainedRecord::class, "elevation", start, end)
    deleteManualActivityMetricRecords(ActiveCaloriesBurnedRecord::class, "active_calories", start, end)
    deleteManualActivityMetricRecords(TotalCaloriesBurnedRecord::class, "total_calories", start, end)
    deleteManualActivityMetricRecords(HeartRateRecord::class, "heart_rate", start, end)
    deleteManualActivityMetricRecords(PowerRecord::class, "power", start, end)
    deleteManualActivityMetricRecords(CyclingPedalingCadenceRecord::class, "cycling_cadence", start, end)
    deleteManualActivityMetricRecords(SpeedRecord::class, "speed", start, end)
    deleteManualActivityMetricRecords(SpeedRecord::class, "running_speed", start, end)
    deleteManualActivityMetricRecords(StepsCadenceRecord::class, "steps_cadence", start, end)
  }

  private suspend fun <T : Record> deleteManualActivityMetricRecords(
    recordType: KClass<T>,
    kind: String,
    start: Instant,
    end: Instant,
  ) {
    val recordIds = support.client().readRecordsPaged(
      recordType = recordType,
      timeRangeFilter = TimeRangeFilter.between(start, end),
      ascendingOrder = true,
    ).filter { record ->
      record.metadata.dataOrigin.packageName == appPackageName &&
        record.metadata.clientRecordId?.startsWith("openvitals_activity_${kind}_") == true
    }.map { record -> record.metadata.id }

    if (recordIds.isNotEmpty()) {
      support.client().deleteRecords(
        recordType = recordType,
        recordIdsList = recordIds,
        clientRecordIdsList = emptyList(),
      )
    }
  }

  private fun manualActivityMetricMetadata(kind: String, startTime: Instant): Metadata =
    Metadata.manualEntry(
      clientRecordId = "openvitals_activity_${kind}_${startTime.toEpochMilli()}_${UUID.randomUUID()}",
      device = Device(type = Device.TYPE_PHONE),
    )

  private fun Int.toActiveExerciseSegmentType(): Int =
    when (this) {
      ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
      ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
      ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL
      ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
      ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
      ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
      ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
      ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES
      ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE
      ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING
      ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL
      ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING
      ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE ->
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE
      ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER
      ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL
      ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING
      ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
      ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR
      ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA
      else -> ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT
    }

  private fun ExerciseSessionRecord.toMsg(
    totalDistanceMeters: Double? = null,
    averageSpeedMetersPerSecond: Double? = null,
  ) = ExerciseDataMsg(
    id = metadata.id,
    title = title,
    exerciseType = exerciseType.toLong(),
    startEpochMs = startTime.toEpochMilli(),
    endEpochMs = endTime.toEpochMilli(),
    source = metadata.dataOrigin.packageName,
    notes = notes,
    clientRecordId = metadata.clientRecordId,
    plannedExerciseSessionId = plannedExerciseSessionId,
    device = metadata.device?.let {
      ExerciseDeviceDataMsg(type = it.type.toLong(), manufacturer = it.manufacturer, model = it.model)
    },
    // The record's provenance. Zone offsets are the WRITER's, passed through as
    // seconds rather than derived here — an activity recorded in another timezone
    // is why Health Connect keeps them.
    startZoneOffsetSeconds = startZoneOffset?.totalSeconds?.toLong(),
    endZoneOffsetSeconds = endZoneOffset?.totalSeconds?.toLong(),
    lastModifiedEpochMs = metadata.lastModifiedTime.toEpochMilli(),
    clientRecordVersion = metadata.clientRecordVersion,
    recordingMethod = metadata.recordingMethod.toLong(),
    segments = segments.map { s ->
      ExerciseSegmentMsg(
        startEpochMs = s.startTime.toEpochMilli(),
        endEpochMs = s.endTime.toEpochMilli(),
        segmentType = s.segmentType.toLong(),
        repetitions = s.repetitions.toLong(),
        setIndex = null,
      )
    },
    laps = laps.map { l ->
      ExerciseLapMsg(
        startEpochMs = l.startTime.toEpochMilli(),
        endEpochMs = l.endTime.toEpochMilli(),
        lengthMeters = l.length?.inMeters,
      )
    },
    route = exerciseRouteResult.toRouteMsg(),
    isOpenVitalsEntry = isOpenVitalsRecord(metadata.dataOrigin.packageName, appPackageName),
    totalDistanceMeters = totalDistanceMeters,
    averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
  )

  private fun ExerciseRouteResult.toRouteMsg(): ExerciseRouteMsg = when (this) {
    is ExerciseRouteResult.Data -> ExerciseRouteMsg(
      status = ExerciseRouteStatusMsg.DATA,
      points = exerciseRoute.route.map { point ->
        ExerciseRoutePointMsg(
          timeEpochMs = point.time.toEpochMilli(),
          latitude = point.latitude,
          longitude = point.longitude,
          altitudeMeters = point.altitude?.inMeters,
          horizontalAccuracyMeters = point.horizontalAccuracy?.inMeters,
          verticalAccuracyMeters = point.verticalAccuracy?.inMeters,
        )
      },
    )
    is ExerciseRouteResult.ConsentRequired ->
      ExerciseRouteMsg(ExerciseRouteStatusMsg.CONSENT_REQUIRED, emptyList())
    is ExerciseRouteResult.NoData ->
      ExerciseRouteMsg(ExerciseRouteStatusMsg.NO_DATA, emptyList())
    else -> ExerciseRouteMsg(ExerciseRouteStatusMsg.NO_DATA, emptyList())
  }

  private companion object {
    private const val TAG = "ActivityHealthReader"
  }
}

private fun PlannedExerciseBlock.toMsg(): PlannedExerciseBlockMsg =
  PlannedExerciseBlockMsg(
    repetitions = repetitions.toLong(),
    description = description?.toString(),
    steps = steps.map { it.toMsg() },
  )

private fun PlannedExerciseStep.toMsg(): PlannedExerciseStepMsg {
  val goal = completionGoal
  return PlannedExerciseStepMsg(
    exerciseType = exerciseType.toLong(),
    exercisePhase = exercisePhase.toLong(),
    description = description?.toString(),
    completionKind = when (goal) {
      is ExerciseCompletionGoal.RepetitionsGoal -> PlannedExerciseCompletionKindMsg.REPETITIONS
      is ExerciseCompletionGoal.DurationGoal -> PlannedExerciseCompletionKindMsg.DURATION_SECONDS
      ExerciseCompletionGoal.ManualCompletion -> PlannedExerciseCompletionKindMsg.MANUAL
      else -> PlannedExerciseCompletionKindMsg.UNKNOWN
    },
    completionRepetitions = (goal as? ExerciseCompletionGoal.RepetitionsGoal)?.repetitions?.toLong(),
    completionSeconds = (goal as? ExerciseCompletionGoal.DurationGoal)?.duration?.seconds,
  )
}

private fun PlannedExerciseBlockMsg.toRecord(): PlannedExerciseBlock =
  PlannedExerciseBlock(
    repetitions = repetitions.toInt(),
    description = description,
    steps = steps.map { it.toRecord() },
  )

private fun PlannedExerciseStepMsg.toRecord(): PlannedExerciseStep =
  PlannedExerciseStep(
    exerciseType = exerciseType.toInt(),
    exercisePhase = exercisePhase.toInt(),
    description = description,
    completionGoal = when (completionKind) {
      PlannedExerciseCompletionKindMsg.REPETITIONS ->
        ExerciseCompletionGoal.RepetitionsGoal((completionRepetitions ?: 1L).toInt())
      PlannedExerciseCompletionKindMsg.DURATION_SECONDS ->
        // Health Connect rejects a zero-length duration goal.
        ExerciseCompletionGoal.DurationGoal(Duration.ofSeconds((completionSeconds ?: 1L).coerceAtLeast(1L)))
      // A goal the app did not understand round-trips as manual completion
      // rather than being dropped.
      PlannedExerciseCompletionKindMsg.MANUAL,
      PlannedExerciseCompletionKindMsg.UNKNOWN -> ExerciseCompletionGoal.ManualCompletion
    },
    performanceTargets = emptyList(),
  )

/// An all-null result: nothing was asked for, or nothing could be read.
private val EMPTY_SESSION_METRICS = ExerciseSessionMetricsMsg(
  totalDistanceMeters = null,
  averageSpeedMetersPerSecond = null,
  steps = null,
  totalCaloriesKcal = null,
  activeCaloriesKcal = null,
  elevationGainedMeters = null,
  floorsClimbed = null,
  wheelchairPushes = null,
)

/// Wire name -> aggregate, kept in step with Dart's `ExerciseSessionMetric`. A name
/// missing from this map is skipped, so a newer caller cannot break an older host.
private val SESSION_METRICS: Map<String, AggregateMetric<*>> = mapOf(
  "distance" to DistanceRecord.DISTANCE_TOTAL,
  "speed" to SpeedRecord.SPEED_AVG,
  "steps" to StepsRecord.COUNT_TOTAL,
  "totalCalories" to TotalCaloriesBurnedRecord.ENERGY_TOTAL,
  "activeCalories" to ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
  "elevation" to ElevationGainedRecord.ELEVATION_GAINED_TOTAL,
  "floors" to FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL,
  "wheelchairPushes" to WheelchairPushesRecord.COUNT_TOTAL,
  "power" to PowerRecord.POWER_AVG,
)
