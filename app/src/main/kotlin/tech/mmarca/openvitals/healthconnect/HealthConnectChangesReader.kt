package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

/** One poll of a changes token: the days to recompute, whether anything was deleted, the next token. */
data class HealthConnectChanges(
    val upsertedDays: List<LocalDate>,
    val hasDeletions: Boolean,
    val nextToken: String,
    val tokenExpired: Boolean,
    val hasMore: Boolean,
)

/**
 * The Changes API for the daily-aggregate cache: one token per record
 * type, so a year is read raw once and then kept current cheaply.
 */
internal class HealthConnectChangesReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun getChangesToken(recordType: KClass<out Record>): String =
        support.withLogging("getChangesToken[${recordType.simpleName}]", "") {
            support.client().getChangesToken(ChangesTokenRequest(setOf(recordType)))
        }

    suspend fun getChanges(token: String): HealthConnectChanges =
        support.withLogging(
            "getChanges",
            HealthConnectChanges(
                upsertedDays = emptyList(),
                hasDeletions = false,
                nextToken = token,
                tokenExpired = false,
                hasMore = false,
            ),
        ) {
            val response = support.client().getChanges(token)
            val zone = ZoneId.systemDefault()
            val days = LinkedHashSet<LocalDate>()
            var hasDeletions = false
            for (change in response.changes) {
                when (change) {
                    is UpsertionChange -> instantOf(change.record)?.let { instant ->
                        days.add(instant.atZone(zone).toLocalDate())
                    }
                    is DeletionChange -> hasDeletions = true
                }
            }
            HealthConnectChanges(
                upsertedDays = days.toList(),
                hasDeletions = hasDeletions,
                nextToken = response.nextChangesToken,
                tokenExpired = response.changesTokenExpired,
                hasMore = response.hasMore,
            )
        }

    // Only registered types reach here. Calorie interval records bucket by their start.
    private fun instantOf(record: Record): Instant? = when (record) {
        is BloodPressureRecord -> record.time
        is OxygenSaturationRecord -> record.time
        is RespiratoryRateRecord -> record.time
        is BodyTemperatureRecord -> record.time
        is Vo2MaxRecord -> record.time
        is BloodGlucoseRecord -> record.time
        is SkinTemperatureRecord -> record.startTime
        is TotalCaloriesBurnedRecord -> record.startTime
        is ActiveCaloriesBurnedRecord -> record.startTime
        is BasalMetabolicRateRecord -> record.time
        else -> null
    }
}
