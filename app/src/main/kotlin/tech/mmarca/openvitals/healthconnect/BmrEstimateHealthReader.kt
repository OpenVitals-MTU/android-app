package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Power
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Health Connect boundary of the basal metabolic rate estimate: reads
 * the window's BasalMetabolicRateRecords, partitions ours from foreign, and
 * applies [bmrReconcileActions]. Mirrors [StepDistanceHealthReader].
 */
internal class BmrEstimateHealthReader(
    private val support: HealthConnectReaderSupport,
    private val appPackageName: String,
) {
    suspend fun reconcileEstimatedBmr(
        window: ClosedRange<LocalDate>,
        estimateByDay: Map<LocalDate, Double>,
    ) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val (own, foreignBmrDays) = readWindow(window, zone)

        val actions = bmrReconcileActions(
            days = daysIn(window),
            estimateByDay = estimateByDay,
            foreignBmrDays = foreignBmrDays,
            ownByDay = own.filter { it.day != null }.associateBy { it.day!! }
                .mapValues { (day, record) ->
                    OwnBmrRecord(uid = record.uid, day = day, kcalPerDay = record.kcalPerDay)
                },
        )
        val malformedOwnUids = own.filter { it.day == null }.map { it.uid }
        if (actions.isEmpty && malformedOwnUids.isEmpty()) return@withContext

        val deleteUids = actions.toDeleteUids + malformedOwnUids
        if (deleteUids.isNotEmpty()) {
            support.client().deleteRecords(
                recordType = BasalMetabolicRateRecord::class,
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

    suspend fun purgeEstimatedBmr(window: ClosedRange<LocalDate>) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val (own, _) = readWindow(window, zone)
        if (own.isEmpty()) return@withContext
        support.client().deleteRecords(
            recordType = BasalMetabolicRateRecord::class,
            recordIdsList = own.map { it.uid },
            clientRecordIdsList = emptyList(),
        )
    }

    private data class OwnRecordInWindow(val uid: String, val day: LocalDate?, val kcalPerDay: Double)

    private suspend fun readWindow(
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Pair<List<OwnRecordInWindow>, Set<LocalDate>> {
        val startInstant = window.start.atStartOfDay(zone).toInstant()
        val endInstant = window.endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
        val records = support.client().readRecordsPaged(
            recordType = BasalMetabolicRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
            ascendingOrder = true,
            pageSize = 500,
        )

        val own = mutableListOf<OwnRecordInWindow>()
        val foreignBmrDays = mutableSetOf<LocalDate>()
        for (record in records) {
            val clientRecordId = record.metadata.clientRecordId
            val isOwn = clientRecordId?.startsWith(BmrEstimateClientRecordIdPrefix) == true &&
                isOpenVitalsRecord(record.metadata.dataOrigin.packageName, appPackageName)
            if (isOwn) {
                val day = runCatching {
                    LocalDate.parse(clientRecordId!!.removePrefix(BmrEstimateClientRecordIdPrefix))
                }.getOrNull()
                own.add(OwnRecordInWindow(record.metadata.id, day, record.basalMetabolicRate.inKilocaloriesPerDay))
            } else {
                // A watch import or a CSV row is not ours to replace, even from this package.
                foreignBmrDays.add(record.time.atZone(zone).toLocalDate())
            }
        }
        return own to foreignBmrDays
    }

    private fun BmrUpsert.toRecord(zone: ZoneId, now: Instant): BasalMetabolicRateRecord {
        val time = day.atStartOfDay(zone).toInstant()
        return BasalMetabolicRateRecord(
            time = time,
            zoneOffset = zone.rules.getOffset(time),
            basalMetabolicRate = Power.kilocaloriesPerDay(kcalPerDay),
            // clientRecordVersion must strictly increase: equal-version behaviour is unspecified.
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = "$BmrEstimateClientRecordIdPrefix$day",
                clientRecordVersion = now.toEpochMilli(),
            ),
        )
    }

    private fun daysIn(window: ClosedRange<LocalDate>): List<LocalDate> =
        generateSequence(window.start) { day ->
            day.plusDays(1).takeIf { !it.isAfter(window.endInclusive) }
        }.toList()
}
