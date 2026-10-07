package tech.mmarca.openvitals.devices.xiaomi

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.reflect.KClass
import tech.mmarca.openvitals.domain.insights.BodyCompositionEstimate
import tech.mmarca.openvitals.domain.model.ScaleWeighIn

/**
 * Maps a weigh-in onto Health Connect [Record]s. Every id is a function of
 * the weigh-in's identity, and every version is the weigh-in's revision, so
 * a fuller rewrite replaces the earlier records and a repeat changes nothing.
 */

/** The records one weigh-in can give. [idPart] is stored in record ids: do not rename. */
enum class ScaleRecordKind(val idPart: String, val recordType: KClass<out Record>) {
    WEIGHT("weight", WeightRecord::class),
    HEART_RATE("heart_rate", HeartRateRecord::class),
    BODY_FAT("body_fat", BodyFatRecord::class),
    LEAN_MASS("lean_mass", LeanBodyMassRecord::class),
    BODY_WATER("body_water", BodyWaterMassRecord::class),
    ;

    val writePermission: String
        get() = HealthPermission.getWritePermission(recordType)
}

/** What a scale needs granted to save everything it measures. Weight alone is enough to save a weigh-in. */
val ScaleWritePermissions: Set<String> = ScaleRecordKind.entries.mapTo(linkedSetOf()) { it.writePermission }

/**
 * No address in it: ids travel to other phones and are visible to other apps.
 * The scale's own timestamp and user slot tell two weigh-ins apart.
 */
fun scaleRecordId(kind: ScaleRecordKind, scaleTimestamp: Long, profile: Int): String =
    "xiaomi_s400_${kind.idPart}_${scaleTimestamp}_p$profile"

/** How far the scale's clock may sit from the phone's and still be believed. */
private const val SCALE_CLOCK_TOLERANCE_SECONDS = 600L

/**
 * When a weigh-in heard at [now] happened. The scale stamps it, but nothing
 * says its clock is right or even set, and a broadcast is always heard live:
 * a stamp far from [now] is a clock problem, not an old weigh-in.
 */
fun scaleRecordTime(scaleTimestamp: Long, now: Instant): Instant =
    if (abs(now.epochSecond - scaleTimestamp) <= SCALE_CLOCK_TOLERANCE_SECONDS) {
        Instant.ofEpochSecond(scaleTimestamp)
    } else {
        now
    }

/**
 * The records [weighIn] gives, limited to the kinds whose write permission is
 * in [granted]. Empty before the weight is in. [composition] is the estimate
 * for this weigh-in, or null when the profile or the reading allowed none.
 */
fun scaleWeighInRecords(
    weighIn: ScaleWeighIn,
    composition: BodyCompositionEstimate?,
    granted: Set<String>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<Record> {
    val weightKg = weighIn.reading.weightKg ?: return emptyList()
    val time = weighIn.time
    val offset = zone.rules.getOffset(time)
    val records = mutableListOf<Record>()
    fun add(kind: ScaleRecordKind, build: (Metadata) -> Record) {
        if (kind.writePermission !in granted) return
        records += build(
            Metadata.autoRecorded(
                device = Device(type = Device.TYPE_SCALE, manufacturer = "Xiaomi", model = "S400"),
                clientRecordId = scaleRecordId(kind, weighIn.scaleTimestamp, weighIn.profile),
                clientRecordVersion = weighIn.updatedMillis,
            ),
        )
    }

    add(ScaleRecordKind.WEIGHT) { WeightRecord(time, offset, Mass.kilograms(weightKg), it) }
    weighIn.reading.heartRateBpm?.let { bpm ->
        add(ScaleRecordKind.HEART_RATE) { metadata ->
            HeartRateRecord(
                startTime = time,
                startZoneOffset = offset,
                // One sample, so a window of one instant. A longer one could end after the
                // weigh-in was heard, and Health Connect takes no record from the future.
                endTime = time,
                endZoneOffset = offset,
                samples = listOf(HeartRateRecord.Sample(time = time, beatsPerMinute = bpm.toLong())),
                metadata = metadata,
            )
        }
    }
    if (composition != null) {
        add(ScaleRecordKind.BODY_FAT) { BodyFatRecord(time, offset, Percentage(composition.bodyFatPercent), it) }
        add(ScaleRecordKind.LEAN_MASS) {
            LeanBodyMassRecord(time, offset, Mass.kilograms(composition.fatFreeMassKg), it)
        }
        add(ScaleRecordKind.BODY_WATER) {
            BodyWaterMassRecord(time, offset, Mass.kilograms(composition.bodyWaterKg), it)
        }
    }
    return records
}
