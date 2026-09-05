package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.meters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Health Connect boundary of the step-derived distance backfill: reads
 * the window's DistanceRecords, partitions ours from foreign, and applies
 * [stepDistanceReconcileActions].
 */
internal class StepDistanceHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun reconcileStepDerivedDistance(
        window: ClosedRange<LocalDate>,
        stepsByDay: Map<LocalDate, Long>,
        strideMeters: Double,
    ) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val (own, foreignDistanceDays) = readWindow(window, zone)

        val actions = stepDistanceReconcileActions(
            days = daysIn(window),
            stepsByDay = stepsByDay,
            foreignDistanceDays = foreignDistanceDays,
            ownByDay = own.filter { it.day != null }.associateBy { it.day!! }
                .mapValues { (day, record) ->
                    OwnStepDistanceRecord(uid = record.uid, day = day, meters = record.meters)
                },
            strideMeters = strideMeters,
        )
        val malformedOwnUids = own.filter { it.day == null }.map { it.uid }
        if (actions.isEmpty && malformedOwnUids.isEmpty()) return@withContext

        val deleteUids = actions.toDeleteUids + malformedOwnUids
        if (deleteUids.isNotEmpty()) {
            support.client().deleteRecords(
                recordType = DistanceRecord::class,
                recordIdsList = deleteUids,
                clientRecordIdsList = emptyList(),
            )
        }
        if (actions.toUpsert.isNotEmpty()) {
            val now = Instant.now()
            support.client().insertRecords(
                actions.toUpsert.map { upsert -> upsert.toRecord(zone, now) },
            )
        }
    }

    suspend fun purgeStepDerivedDistance(window: ClosedRange<LocalDate>) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val (own, _) = readWindow(window, zone)
        if (own.isEmpty()) return@withContext
        support.client().deleteRecords(
            recordType = DistanceRecord::class,
            recordIdsList = own.map { it.uid },
            clientRecordIdsList = emptyList(),
        )
    }

    private data class OwnRecordInWindow(val uid: String, val day: LocalDate?, val meters: Double)

    private suspend fun readWindow(
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Pair<List<OwnRecordInWindow>, Set<LocalDate>> {
        val startInstant = window.start.atStartOfDay(zone).toInstant()
        val endInstant = window.endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
        val records = support.client().readRecordsPaged(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
            ascendingOrder = true,
            pageSize = 500,
        )

        val own = mutableListOf<OwnRecordInWindow>()
        val foreignDistanceDays = mutableSetOf<LocalDate>()
        for (record in records) {
            val clientRecordId = record.metadata.clientRecordId
            val isOwn = clientRecordId?.startsWith(StepDistanceClientRecordIdPrefix) == true &&
                isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName)
            if (isOwn) {
                val day = runCatching {
                    LocalDate.parse(clientRecordId!!.removePrefix(StepDistanceClientRecordIdPrefix))
                }.getOrNull()
                own.add(OwnRecordInWindow(record.metadata.id, day, record.distance.inMeters))
            } else if (record.distance.inMeters > 0.0) {
                var day = record.startTime.atZone(zone).toLocalDate()
                val last = record.endTime.minusMillis(1).atZone(zone).toLocalDate()
                while (!day.isAfter(last)) {
                    foreignDistanceDays.add(day)
                    day = day.plusDays(1)
                }
            }
        }
        return own to foreignDistanceDays
    }

    private fun StepDistanceUpsert.toRecord(zone: ZoneId, now: Instant): DistanceRecord {
        val startInstant = day.atStartOfDay(zone).toInstant()
        val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant()
        // Today's record must not end in the future.
        val endInstant = minOf(nextMidnight, now).coerceAtLeast(startInstant.plusSeconds(1))
        return DistanceRecord(
            startTime = startInstant,
            startZoneOffset = zone.rules.getOffset(startInstant),
            endTime = endInstant,
            endZoneOffset = zone.rules.getOffset(endInstant),
            distance = meters.meters,
            // clientRecordVersion must strictly increase: equal-version behaviour is unspecified.
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = "$StepDistanceClientRecordIdPrefix$day",
                clientRecordVersion = now.toEpochMilli(),
            ),
        )
    }

    private fun daysIn(window: ClosedRange<LocalDate>): List<LocalDate> =
        generateSequence(window.start) { day ->
            day.plusDays(1).takeIf { !it.isAfter(window.endInclusive) }
        }.toList()
}
