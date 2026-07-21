package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
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
import java.time.ZoneId

/**
 * The Health Connect Changes API, exposed for the local daily-aggregate cache.
 *
 * A token is registered per record type (respiratory rate, SpO2, …); polling it
 * returns which local days had records inserted/updated (so the cache recomputes
 * just those days) and whether anything was deleted (deletions carry only an id,
 * so the cache full-rebuilds that metric then). This is what lets a year of
 * densely-sampled data be read raw ONCE and then kept current cheaply.
 */
internal class HealthConnectChangesReader(
  private val support: HealthConnectReaderSupport,
) {
  suspend fun getChangesToken(recordType: String): String =
    support.withLogging("getVitalsChangesToken[$recordType]", "") {
      val recordClass = recordClassFor(recordType)
        ?: throw IllegalArgumentException("Unknown record type: $recordType")
      support.client().getChangesToken(ChangesTokenRequest(setOf(recordClass)))
    }

  suspend fun getChanges(token: String): VitalsChangesMsg =
    support.withLogging(
      "getVitalsChanges",
      VitalsChangesMsg(
        upsertedDayEpochMs = emptyList(),
        hasDeletions = false,
        nextToken = token,
        tokenExpired = false,
        hasMore = false,
      ),
    ) {
      val response = support.client().getChanges(token)
      val zone = ZoneId.systemDefault()
      val days = LinkedHashSet<Long>()
      var hasDeletions = false
      for (change in response.changes) {
        when (change) {
          is UpsertionChange -> instantOf(change.record)?.let { instant ->
            days.add(
              instant.atZone(zone).toLocalDate()
                .atStartOfDay(zone).toInstant().toEpochMilli(),
            )
          }
          is DeletionChange -> hasDeletions = true
        }
      }
      VitalsChangesMsg(
        upsertedDayEpochMs = days.toList(),
        hasDeletions = hasDeletions,
        nextToken = response.nextChangesToken,
        tokenExpired = response.changesTokenExpired,
        hasMore = response.hasMore,
      )
    }

  // Tokens are registered per record type, so only these reach here. The
  // calorie records are interval records (startTime/endTime), bucketed by their
  // start to match the daily calories-burned cache.
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
    else -> null
  }
}
