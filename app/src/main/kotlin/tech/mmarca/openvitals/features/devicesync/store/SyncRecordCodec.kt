package tech.mmarca.openvitals.features.devicesync.store

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseCompletionGoal
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseBlock
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Serializes Health Connect [Record]s to and from the sync wire, and derives
 * each record's deterministic content fingerprint (the dedup key).
 *
 * Ported from the Flutter `import_record_sync_codec.dart` — but implemented
 * DIRECTLY over the androidx `Record` subclasses; the Dart build needed an
 * `ImportRecord` intermediate that Kotlin does not. Kotlin↔Kotlin only: the
 * construction mirrors the Dart codec but is not byte-compatible with it (enum
 * fields ride as Health Connect int constants rather than Dart enum names).
 *
 * WHY A FINGERPRINT
 * -----------------
 * Records read natively from Health Connect carry an HC id that differs per
 * device and usually a null `clientRecordId`. So dedup must key on CONTENT,
 * not the HC id. The record's identifying fields are hashed into a
 * `sync_<hex>` id — the same construction the Apple Health importer uses for
 * its `apple_health_` ids. Both phones compute the SAME fingerprint for the
 * same logical record, so the bidirectional merge converges and a re-sync
 * writes nothing. When received records are written they carry this
 * `sync_<hex>` id as their `clientRecordId`, so Health Connect upserts on it.
 *
 * DELIBERATELY CONTENT-ONLY — the fingerprint does NOT include the source app
 * (`dataOrigin`). When phone B writes a record received from A, Health Connect
 * re-stamps the record's `dataOrigin` with B's own package, so an
 * origin-inclusive fingerprint would differ across the two phones and every
 * re-sync would accumulate duplicates. Content-only keeps the same logical
 * record mapping to the same fingerprint on both phones. The cost — two
 * genuinely distinct records with identical type+time+values from different
 * source apps collide into one — is rare and the lesser evil.
 */

/** The prefix on every sync-assigned clientRecordId. */
const val SYNC_CLIENT_RECORD_ID_PREFIX: String = "sync_"

/**
 * Thrown when a record type has no codec entry (should never happen for a
 * negotiated type; a guard against a protocol/version mismatch).
 */
class UnsupportedSyncRecordType(val recordType: String) :
    Exception("UnsupportedSyncRecordType: $recordType")

/** The Health Connect record class for a wire type name, or null if unknown. */
fun syncRecordClassFor(recordType: String): KClass<out Record>? =
    SYNC_RECORD_CLASSES[recordType]

/** The wire type name for [record] (its simple class name, e.g. `StepsRecord`). */
fun syncRecordTypeName(record: Record): String = record::class.simpleName.orEmpty()

private val SYNC_RECORD_CLASSES: Map<String, KClass<out Record>> = listOf(
    StepsRecord::class,
    DistanceRecord::class,
    ActiveCaloriesBurnedRecord::class,
    TotalCaloriesBurnedRecord::class,
    FloorsClimbedRecord::class,
    ElevationGainedRecord::class,
    WheelchairPushesRecord::class,
    SpeedRecord::class,
    StepsCadenceRecord::class,
    CyclingPedalingCadenceRecord::class,
    PowerRecord::class,
    HeartRateRecord::class,
    RestingHeartRateRecord::class,
    HeartRateVariabilityRmssdRecord::class,
    WeightRecord::class,
    HeightRecord::class,
    BodyFatRecord::class,
    LeanBodyMassRecord::class,
    BasalMetabolicRateRecord::class,
    BoneMassRecord::class,
    BodyWaterMassRecord::class,
    HydrationRecord::class,
    NutritionRecord::class,
    BloodPressureRecord::class,
    OxygenSaturationRecord::class,
    RespiratoryRateRecord::class,
    BodyTemperatureRecord::class,
    Vo2MaxRecord::class,
    BloodGlucoseRecord::class,
    BasalBodyTemperatureRecord::class,
    SkinTemperatureRecord::class,
    SleepSessionRecord::class,
    ExerciseSessionRecord::class,
    PlannedExerciseSessionRecord::class,
    MindfulnessSessionRecord::class,
    MenstruationFlowRecord::class,
    MenstruationPeriodRecord::class,
    OvulationTestRecord::class,
    CervicalMucusRecord::class,
    IntermenstrualBleedingRecord::class,
    SexualActivityRecord::class,
).associateBy { it.simpleName.orEmpty() }

/**
 * Computes the deterministic `sync_<hex>` fingerprint for [record] from its
 * content. Independent of the record's current clientRecordId and metadata.
 */
fun syncFingerprint(record: Record): String {
    val parts = fingerprintParts(record).joinToString("|") { fp(it) }
    val digest = MessageDigest.getInstance("SHA-256").digest(parts.toByteArray(Charsets.UTF_8))
    val hex = buildString(32) {
        for (index in 0 until 16) {
            val byte = digest[index].toInt() and 0xFF
            append(HEX_DIGITS[byte ushr 4])
            append(HEX_DIGITS[byte and 0x0F])
        }
    }
    return "$SYNC_CLIENT_RECORD_ID_PREFIX$hex"
}

/**
 * Encodes [record]'s times + values to wire bytes (JSON), plus the record's
 * recording [Device] (manufacturer/model/type) when it has one, so hardware
 * provenance survives in the receiver's Health Connect store itself — visible
 * to EVERY consumer, not just OpenVitals. The record type and clientRecordId
 * travel separately on the `SyncItem`, so they are not repeated here.
 *
 * The `device` key is optional in both directions (the codec's JSON parser
 * ignores unknown keys, so a build predating it skips it, and its absence
 * decodes to the phone-device default) and, like everything metadata, it is
 * NOT part of [fingerprintParts] — payload bytes may differ across versions,
 * but the dedup key never does.
 */
fun encodeSyncRecordPayload(record: Record): ByteArray {
    val json = encode(record)
    val device = record.metadata.device
    val withDevice = if (device == null) {
        json
    } else {
        JsonObject(
            json + (
                "device" to buildJsonObject {
                    put("t", device.type)
                    put("mf", device.manufacturer)
                    put("md", device.model)
                }
                ),
        )
    }
    return withDevice.toString().toByteArray(Charsets.UTF_8)
}

/**
 * Reconstructs a [Record] of [recordType] carrying [clientRecordId] from a
 * [payload] produced by [encodeSyncRecordPayload].
 */
fun decodeSyncRecord(recordType: String, clientRecordId: String, payload: ByteArray): Record {
    val json = CodecJson.parseToJsonElement(payload.toString(Charsets.UTF_8)).jsonObject
    return decode(recordType, syncMetadata(clientRecordId, decodeDevice(json["device"])), json)
}

private val CodecJson = Json { ignoreUnknownKeys = true }
private const val HEX_DIGITS = "0123456789abcdef"

private fun syncMetadata(clientRecordId: String, device: Device?): Metadata =
    Metadata.manualEntry(
        // The sender's original recording device when the wire carried one
        // (older builds do not send it); the pre-field placeholder otherwise.
        device = device ?: Device(type = Device.TYPE_PHONE),
        clientRecordId = clientRecordId,
    )

/** The wire `device` object as a [Device], or null when absent/malformed. */
private fun decodeDevice(element: JsonElement?): Device? {
    val json = (element as? JsonObject) ?: return null
    return Device(
        manufacturer = json.strOrNull("mf"),
        model = json.strOrNull("md"),
        type = deviceType(json.intOrNull("t")),
    )
}

/**
 * A peer's device type, or UNKNOWN if it is not one we recognise — the same
 * closed-set policy as [bloodPressureBodyPosition]: the wire carries a bare
 * int from another phone, and an unrecognised constant must not be handed to
 * the platform verbatim.
 */
private fun deviceType(raw: Int?): Int = when (raw) {
    Device.TYPE_WATCH,
    Device.TYPE_PHONE,
    Device.TYPE_SCALE,
    Device.TYPE_RING,
    Device.TYPE_HEAD_MOUNTED,
    Device.TYPE_FITNESS_BAND,
    Device.TYPE_CHEST_STRAP,
    Device.TYPE_SMART_DISPLAY,
    -> raw
    else -> Device.TYPE_UNKNOWN
}

/**
 * Renders a fingerprint part as a stable string. Doubles are quantized to 6
 * decimals so a value that came back from a Health Connect unit round-trip
 * (grams↔kilograms, watts↔kcal/day) with a bit or two of binary drift still
 * hashes identically — otherwise phone B, after writing A's record and reading
 * it back, computes a different fingerprint and a re-sync re-imports it as a
 * new record, accumulating duplicates. Ints/strings pass through.
 */
private fun fp(part: Any?): String =
    if (part is Double) String.format(Locale.US, "%.6f", part) else part.toString()

// ── Time helpers ─────────────────────────────────────────────────────────────

private fun ms(time: Instant): Long = time.toEpochMilli()
private fun inst(time: Instant): String = time.toString()
private fun offSec(offset: ZoneOffset?): Int? = offset?.totalSeconds
private fun sample(time: Instant, value: Any?): String = "${inst(time)}:${fp(value)}"

// ── Fingerprint parts (identifying content per type) ─────────────────────────

private fun fingerprintParts(r: Record): List<Any?> {
    val type = syncRecordTypeName(r)
    return when (r) {
        is StepsRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.count)
        is DistanceRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.distance.inMeters)
        is ActiveCaloriesBurnedRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.energy.inKilocalories)
        is TotalCaloriesBurnedRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.energy.inKilocalories)
        is BasalMetabolicRateRecord ->
            listOf(type, inst(r.time), r.basalMetabolicRate.inKilocaloriesPerDay)
        is FloorsClimbedRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.floors)
        is WheelchairPushesRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.count)
        is ElevationGainedRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.elevation.inMeters)
        is SpeedRecord -> listOf(type, inst(r.startTime), inst(r.endTime)) +
            r.samples.map { sample(it.time, it.speed.inMetersPerSecond) }
        is StepsCadenceRecord -> listOf(type, inst(r.startTime), inst(r.endTime)) +
            r.samples.map { sample(it.time, it.rate) }
        is CyclingPedalingCadenceRecord -> listOf(type, inst(r.startTime), inst(r.endTime)) +
            r.samples.map { sample(it.time, it.revolutionsPerMinute) }
        is PowerRecord -> listOf(type, inst(r.startTime), inst(r.endTime)) +
            r.samples.map { sample(it.time, it.power.inWatts) }
        is HeartRateRecord -> listOf(type, inst(r.startTime), inst(r.endTime)) +
            r.samples.map { sample(it.time, it.beatsPerMinute) }
        is RestingHeartRateRecord -> listOf(type, inst(r.time), r.beatsPerMinute)
        is HeartRateVariabilityRmssdRecord ->
            listOf(type, inst(r.time), r.heartRateVariabilityMillis)
        is WeightRecord -> listOf(type, inst(r.time), r.weight.inKilograms)
        is HeightRecord -> listOf(type, inst(r.time), r.height.inMeters)
        is BodyFatRecord -> listOf(type, inst(r.time), r.percentage.value)
        is LeanBodyMassRecord -> listOf(type, inst(r.time), r.mass.inKilograms)
        is BoneMassRecord -> listOf(type, inst(r.time), r.mass.inKilograms)
        is BodyWaterMassRecord -> listOf(type, inst(r.time), r.mass.inKilograms)
        is HydrationRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.volume.inMilliliters)
        is OxygenSaturationRecord -> listOf(type, inst(r.time), r.percentage.value)
        is RespiratoryRateRecord -> listOf(type, inst(r.time), r.rate)
        is BodyTemperatureRecord -> listOf(type, inst(r.time), r.temperature.inCelsius)
        is BloodGlucoseRecord -> listOf(type, inst(r.time), r.level.inMillimolesPerLiter)
        is Vo2MaxRecord -> listOf(type, inst(r.time), r.vo2MillilitersPerMinuteKilogram)
        is BasalBodyTemperatureRecord -> listOf(type, inst(r.time), r.temperature.inCelsius)
        is SkinTemperatureRecord -> listOf(
            type,
            inst(r.startTime),
            inst(r.endTime),
            r.baseline?.inCelsius,
            r.measurementLocation,
        ) + r.deltas.map { sample(it.time, it.delta.inCelsius) }
        is SleepSessionRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.title) +
            r.stages.map { "${inst(it.startTime)}:${inst(it.endTime)}:${it.stage}" }
        is ExerciseSessionRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.exerciseType, r.title)
        is PlannedExerciseSessionRecord ->
            listOf(type, inst(r.startTime), inst(r.endTime), r.exerciseType, r.title.orEmpty()) +
                r.blocks.map { block ->
                    "${block.repetitions}:" + block.steps.joinToString(",") { step ->
                        val goal = step.completionGoal.toCodecGoal()
                        "${step.exerciseType}/${goal.kind}/${goal.repetitions}/${goal.seconds}"
                    }
                }
        is MindfulnessSessionRecord -> listOf(type, inst(r.startTime), inst(r.endTime), r.title)
        is NutritionRecord -> {
            val nutrients = r.nutrientGrams()
            listOf(
                type,
                inst(r.startTime),
                inst(r.endTime),
                r.name.orEmpty(),
                r.energy?.inKilocalories,
            ) + nutrients.keys.sorted().map { "$it=${fp(nutrients.getValue(it))}" }
        }
        is BloodPressureRecord -> listOf(
            type,
            inst(r.time),
            r.systolic.inMillimetersOfMercury,
            r.diastolic.inMillimetersOfMercury,
        )
        is MenstruationFlowRecord -> listOf(type, inst(r.time), r.flow)
        is MenstruationPeriodRecord -> listOf(type, inst(r.startTime), inst(r.endTime))
        is OvulationTestRecord -> listOf(type, inst(r.time), r.result)
        is CervicalMucusRecord -> listOf(type, inst(r.time), r.appearance, r.sensation)
        is IntermenstrualBleedingRecord -> listOf(type, inst(r.time))
        is SexualActivityRecord -> listOf(type, inst(r.time), r.protectionUsed)
        else -> throw UnsupportedSyncRecordType(type)
    }
}

// ── Encode (value fields to JSON) ────────────────────────────────────────────

private fun JsonObjectBuilderScope.interval(
    start: Instant,
    startOffset: ZoneOffset?,
    end: Instant,
    endOffset: ZoneOffset?,
) {
    builder.put("s", ms(start))
    builder.put("so", offSec(startOffset))
    builder.put("e", ms(end))
    builder.put("eo", offSec(endOffset))
}

private fun JsonObjectBuilderScope.instant(time: Instant, offset: ZoneOffset?) {
    builder.put("i", ms(time))
    builder.put("io", offSec(offset))
}

private class JsonObjectBuilderScope(val builder: kotlinx.serialization.json.JsonObjectBuilder)

private inline fun jsonObject(block: JsonObjectBuilderScope.() -> Unit): JsonObject =
    buildJsonObject { JsonObjectBuilderScope(this).block() }

private fun samplesArray(samples: List<Pair<Instant, Double>>): JsonArray = buildJsonArray {
    samples.forEach { (time, value) ->
        add(buildJsonObject { put("t", ms(time)); put("v", value) })
    }
}

private fun encode(r: Record): JsonObject = when (r) {
    is StepsRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("count", r.count)
    }
    is DistanceRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("meters", r.distance.inMeters)
    }
    is ActiveCaloriesBurnedRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("kcal", r.energy.inKilocalories)
    }
    is TotalCaloriesBurnedRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("kcal", r.energy.inKilocalories)
    }
    is BasalMetabolicRateRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("kcalDay", r.basalMetabolicRate.inKilocaloriesPerDay)
    }
    is FloorsClimbedRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("floors", r.floors)
    }
    is WheelchairPushesRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("count", r.count)
    }
    is ElevationGainedRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("meters", r.elevation.inMeters)
    }
    is SpeedRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("samples", samplesArray(r.samples.map { it.time to it.speed.inMetersPerSecond }))
    }
    is StepsCadenceRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("samples", samplesArray(r.samples.map { it.time to it.rate }))
    }
    is CyclingPedalingCadenceRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("samples", samplesArray(r.samples.map { it.time to it.revolutionsPerMinute }))
    }
    is PowerRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("samples", samplesArray(r.samples.map { it.time to it.power.inWatts }))
    }
    is HeartRateRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put(
            "samples",
            buildJsonArray {
                r.samples.forEach { s ->
                    add(buildJsonObject { put("t", ms(s.time)); put("v", s.beatsPerMinute) })
                }
            },
        )
    }
    is RestingHeartRateRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("bpm", r.beatsPerMinute)
    }
    is HeartRateVariabilityRmssdRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("rmssd", r.heartRateVariabilityMillis)
    }
    is WeightRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("kg", r.weight.inKilograms)
    }
    is HeightRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("meters", r.height.inMeters)
    }
    is BodyFatRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("pct", r.percentage.value)
    }
    is LeanBodyMassRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("kg", r.mass.inKilograms)
    }
    is BoneMassRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("kg", r.mass.inKilograms)
    }
    is BodyWaterMassRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("kg", r.mass.inKilograms)
    }
    is HydrationRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("ml", r.volume.inMilliliters)
    }
    is OxygenSaturationRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("pct", r.percentage.value)
    }
    is RespiratoryRateRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("rate", r.rate)
    }
    is BodyTemperatureRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("c", r.temperature.inCelsius)
    }
    is BloodGlucoseRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("mmol", r.level.inMillimolesPerLiter)
    }
    is Vo2MaxRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("vo2", r.vo2MillilitersPerMinuteKilogram)
    }
    is BasalBodyTemperatureRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("c", r.temperature.inCelsius)
    }
    is SkinTemperatureRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("baseline", r.baseline?.inCelsius)
        builder.put("loc", r.measurementLocation)
        builder.put("deltas", samplesArray(r.deltas.map { it.time to it.delta.inCelsius }))
    }
    is SleepSessionRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("title", r.title)
        builder.put("notes", r.notes)
        builder.put(
            "stages",
            buildJsonArray {
                r.stages.forEach { stage ->
                    add(
                        buildJsonObject {
                            put("s", ms(stage.startTime))
                            put("e", ms(stage.endTime))
                            put("stage", stage.stage)
                        },
                    )
                }
            },
        )
    }
    is ExerciseSessionRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("exerciseType", r.exerciseType)
        builder.put("title", r.title)
        builder.put("notes", r.notes)
        val route = (r.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute
        if (route == null) {
            builder.put("route", JsonNull)
        } else {
            builder.put(
                "route",
                buildJsonArray {
                    route.route.forEach { p ->
                        add(
                            buildJsonObject {
                                put("t", ms(p.time))
                                put("lat", p.latitude)
                                put("lng", p.longitude)
                                put("alt", p.altitude?.inMeters)
                                put("ha", p.horizontalAccuracy?.inMeters)
                                put("va", p.verticalAccuracy?.inMeters)
                            },
                        )
                    }
                },
            )
        }
    }
    is PlannedExerciseSessionRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("et", r.exerciseType)
        builder.put("title", r.title)
        builder.put("notes", r.notes)
        builder.put(
            "blocks",
            buildJsonArray {
                r.blocks.forEach { block ->
                    add(
                        buildJsonObject {
                            put("reps", block.repetitions)
                            put("desc", block.description)
                            put(
                                "steps",
                                buildJsonArray {
                                    block.steps.forEach { step ->
                                        val goal = step.completionGoal.toCodecGoal()
                                        add(
                                            buildJsonObject {
                                                put("et", step.exerciseType)
                                                put("phase", step.exercisePhase)
                                                put("desc", step.description)
                                                put("ck", goal.kind)
                                                put("cr", goal.repetitions)
                                                put("cs", goal.seconds)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
    }
    is MindfulnessSessionRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("kind", r.mindfulnessSessionType)
        builder.put("title", r.title)
        builder.put("notes", r.notes)
    }
    is NutritionRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
        builder.put("name", r.name)
        builder.put("kcal", r.energy?.inKilocalories)
        builder.put("mealType", r.mealType)
        builder.put(
            "nutrients",
            buildJsonObject { r.nutrientGrams().forEach { (name, grams) -> put(name, grams) } },
        )
    }
    is BloodPressureRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("sys", r.systolic.inMillimetersOfMercury)
        builder.put("dia", r.diastolic.inMillimetersOfMercury)
        builder.put("bodyPos", r.bodyPosition)
        builder.put("measLoc", r.measurementLocation)
    }
    is MenstruationFlowRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("flow", r.flow)
    }
    is MenstruationPeriodRecord -> jsonObject {
        interval(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset)
    }
    is OvulationTestRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("result", r.result)
    }
    is CervicalMucusRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("appearance", r.appearance)
        builder.put("sensation", r.sensation)
    }
    is IntermenstrualBleedingRecord -> jsonObject { instant(r.time, r.zoneOffset) }
    is SexualActivityRecord -> jsonObject {
        instant(r.time, r.zoneOffset)
        builder.put("protection", r.protectionUsed)
    }
    else -> throw UnsupportedSyncRecordType(syncRecordTypeName(r))
}

// ── Decode (JSON to a typed record with the given metadata) ──────────────────

private fun decode(type: String, metadata: Metadata, j: JsonObject): Record {
    fun s() = Instant.ofEpochMilli(j.getValue("s").jsonPrimitive.long)
    fun so() = j.intOrNull("so")?.let(ZoneOffset::ofTotalSeconds)
    fun e() = Instant.ofEpochMilli(j.getValue("e").jsonPrimitive.long)
    fun eo() = j.intOrNull("eo")?.let(ZoneOffset::ofTotalSeconds)
    fun i() = Instant.ofEpochMilli(j.getValue("i").jsonPrimitive.long)
    fun io() = j.intOrNull("io")?.let(ZoneOffset::ofTotalSeconds)
    fun d(key: String) = j.getValue(key).jsonPrimitive.double
    fun dn(key: String) = j.doubleOrNull(key)
    fun n(key: String) = j.getValue(key).jsonPrimitive.long
    fun ni(key: String) = j.getValue(key).jsonPrimitive.int
    fun str(key: String) = j.strOrNull(key)
    fun sampleList(key: String): List<Pair<Instant, Double>> =
        j.getValue(key).jsonArray.map { element ->
            val o = element.jsonObject
            Instant.ofEpochMilli(o.getValue("t").jsonPrimitive.long) to
                o.getValue("v").jsonPrimitive.double
        }

    return when (type) {
        "StepsRecord" -> StepsRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            count = n("count"), metadata = metadata,
        )
        "DistanceRecord" -> DistanceRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            distance = Length.meters(d("meters")), metadata = metadata,
        )
        "ActiveCaloriesBurnedRecord" -> ActiveCaloriesBurnedRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            energy = Energy.kilocalories(d("kcal")), metadata = metadata,
        )
        "TotalCaloriesBurnedRecord" -> TotalCaloriesBurnedRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            energy = Energy.kilocalories(d("kcal")), metadata = metadata,
        )
        "BasalMetabolicRateRecord" -> BasalMetabolicRateRecord(
            time = i(), zoneOffset = io(),
            basalMetabolicRate = Power.kilocaloriesPerDay(d("kcalDay")), metadata = metadata,
        )
        "FloorsClimbedRecord" -> FloorsClimbedRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            floors = d("floors"), metadata = metadata,
        )
        "WheelchairPushesRecord" -> WheelchairPushesRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            count = n("count"), metadata = metadata,
        )
        "ElevationGainedRecord" -> ElevationGainedRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            elevation = Length.meters(d("meters")), metadata = metadata,
        )
        "SpeedRecord" -> SpeedRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            samples = sampleList("samples").map { (t, v) ->
                SpeedRecord.Sample(t, Velocity.metersPerSecond(v))
            },
            metadata = metadata,
        )
        "StepsCadenceRecord" -> StepsCadenceRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            samples = sampleList("samples").map { (t, v) -> StepsCadenceRecord.Sample(t, v) },
            metadata = metadata,
        )
        "CyclingPedalingCadenceRecord" -> CyclingPedalingCadenceRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            samples = sampleList("samples").map { (t, v) ->
                CyclingPedalingCadenceRecord.Sample(t, v)
            },
            metadata = metadata,
        )
        "PowerRecord" -> PowerRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            samples = sampleList("samples").map { (t, v) -> PowerRecord.Sample(t, Power.watts(v)) },
            metadata = metadata,
        )
        "HeartRateRecord" -> HeartRateRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            samples = j.getValue("samples").jsonArray.map { element ->
                val o = element.jsonObject
                HeartRateRecord.Sample(
                    time = Instant.ofEpochMilli(o.getValue("t").jsonPrimitive.long),
                    beatsPerMinute = o.getValue("v").jsonPrimitive.long,
                )
            },
            metadata = metadata,
        )
        "RestingHeartRateRecord" -> RestingHeartRateRecord(
            time = i(), zoneOffset = io(), beatsPerMinute = n("bpm"), metadata = metadata,
        )
        "HeartRateVariabilityRmssdRecord" -> HeartRateVariabilityRmssdRecord(
            time = i(), zoneOffset = io(),
            heartRateVariabilityMillis = d("rmssd"), metadata = metadata,
        )
        "WeightRecord" -> WeightRecord(
            time = i(), zoneOffset = io(), weight = Mass.kilograms(d("kg")), metadata = metadata,
        )
        "HeightRecord" -> HeightRecord(
            time = i(), zoneOffset = io(), height = Length.meters(d("meters")), metadata = metadata,
        )
        "BodyFatRecord" -> BodyFatRecord(
            time = i(), zoneOffset = io(), percentage = Percentage(d("pct")), metadata = metadata,
        )
        "LeanBodyMassRecord" -> LeanBodyMassRecord(
            time = i(), zoneOffset = io(), mass = Mass.kilograms(d("kg")), metadata = metadata,
        )
        "BoneMassRecord" -> BoneMassRecord(
            time = i(), zoneOffset = io(), mass = Mass.kilograms(d("kg")), metadata = metadata,
        )
        "BodyWaterMassRecord" -> BodyWaterMassRecord(
            time = i(), zoneOffset = io(), mass = Mass.kilograms(d("kg")), metadata = metadata,
        )
        "HydrationRecord" -> HydrationRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            volume = Volume.milliliters(d("ml")), metadata = metadata,
        )
        "OxygenSaturationRecord" -> OxygenSaturationRecord(
            time = i(), zoneOffset = io(), percentage = Percentage(d("pct")), metadata = metadata,
        )
        "RespiratoryRateRecord" -> RespiratoryRateRecord(
            time = i(), zoneOffset = io(), rate = d("rate"), metadata = metadata,
        )
        "BodyTemperatureRecord" -> BodyTemperatureRecord(
            time = i(),
            zoneOffset = io(),
            temperature = Temperature.celsius(d("c")),
            metadata = metadata,
        )
        "BloodGlucoseRecord" -> BloodGlucoseRecord(
            time = i(),
            zoneOffset = io(),
            level = BloodGlucose.millimolesPerLiter(d("mmol")),
            metadata = metadata,
        )
        "Vo2MaxRecord" -> Vo2MaxRecord(
            time = i(),
            zoneOffset = io(),
            vo2MillilitersPerMinuteKilogram = d("vo2"),
            metadata = metadata,
        )
        "BasalBodyTemperatureRecord" -> BasalBodyTemperatureRecord(
            time = i(),
            zoneOffset = io(),
            temperature = Temperature.celsius(d("c")),
            metadata = metadata,
        )
        "SkinTemperatureRecord" -> SkinTemperatureRecord(
            startTime = s(),
            startZoneOffset = so(),
            endTime = e(),
            endZoneOffset = eo(),
            metadata = metadata,
            deltas = sampleList("deltas").map { (t, v) ->
                SkinTemperatureRecord.Delta(t, TemperatureDelta.celsius(v))
            },
            baseline = dn("baseline")?.let(Temperature::celsius),
            measurementLocation = skinTemperatureMeasurementLocation(ni("loc")),
        )
        "SleepSessionRecord" -> SleepSessionRecord(
            startTime = s(),
            startZoneOffset = so(),
            endTime = e(),
            endZoneOffset = eo(),
            metadata = metadata,
            title = str("title"),
            notes = str("notes"),
            stages = j.getValue("stages").jsonArray.map { element ->
                val o = element.jsonObject
                SleepSessionRecord.Stage(
                    startTime = Instant.ofEpochMilli(o.getValue("s").jsonPrimitive.long),
                    endTime = Instant.ofEpochMilli(o.getValue("e").jsonPrimitive.long),
                    stage = o.getValue("stage").jsonPrimitive.int,
                )
            },
        )
        "ExerciseSessionRecord" -> {
            val route = j["route"]?.takeIf { it !is JsonNull }?.jsonArray?.map { element ->
                val o = element.jsonObject
                ExerciseRoute.Location(
                    time = Instant.ofEpochMilli(o.getValue("t").jsonPrimitive.long),
                    latitude = o.getValue("lat").jsonPrimitive.double,
                    longitude = o.getValue("lng").jsonPrimitive.double,
                    altitude = o.doubleOrNull("alt")?.let(Length::meters),
                    horizontalAccuracy = o.doubleOrNull("ha")?.let(Length::meters),
                    verticalAccuracy = o.doubleOrNull("va")?.let(Length::meters),
                )
            }
            ExerciseSessionRecord(
                startTime = s(),
                startZoneOffset = so(),
                endTime = e(),
                endZoneOffset = eo(),
                metadata = metadata,
                exerciseType = ni("exerciseType"),
                title = str("title"),
                notes = str("notes"),
                segments = emptyList(),
                laps = emptyList(),
                exerciseRoute = route?.let(::ExerciseRoute),
            )
        }
        "PlannedExerciseSessionRecord" -> PlannedExerciseSessionRecord(
            startTime = s(),
            startZoneOffset = so(),
            endTime = e(),
            endZoneOffset = eo(),
            metadata = metadata,
            blocks = j.getValue("blocks").jsonArray.map { blockElement ->
                val block = blockElement.jsonObject
                PlannedExerciseBlock(
                    repetitions = block.getValue("reps").jsonPrimitive.int,
                    description = block.strOrNull("desc"),
                    steps = block.getValue("steps").jsonArray.map { stepElement ->
                        val step = stepElement.jsonObject
                        PlannedExerciseStep(
                            exerciseType = step.getValue("et").jsonPrimitive.int,
                            exercisePhase = step.getValue("phase").jsonPrimitive.int,
                            description = step.strOrNull("desc"),
                            completionGoal = CodecCompletionGoal(
                                kind = step.getValue("ck").jsonPrimitive.int,
                                repetitions = step.intOrNull("cr"),
                                seconds = step.intOrNull("cs")?.toLong(),
                            ).toExerciseCompletionGoal(),
                            performanceTargets = emptyList(),
                        )
                    },
                )
            },
            exerciseType = ni("et"),
            title = str("title"),
            notes = str("notes"),
        )
        "MindfulnessSessionRecord" -> MindfulnessSessionRecord(
            startTime = s(),
            startZoneOffset = so(),
            endTime = e(),
            endZoneOffset = eo(),
            metadata = metadata,
            mindfulnessSessionType = ni("kind"),
            title = str("title"),
            notes = str("notes"),
        )
        "NutritionRecord" -> {
            val nutrients = j.getValue("nutrients").jsonObject
                .mapValues { (_, value) -> value.jsonPrimitive.double }
            buildNutritionRecord(
                startTime = s(),
                startZoneOffset = so(),
                endTime = e(),
                endZoneOffset = eo(),
                metadata = metadata,
                name = str("name"),
                energyKilocalories = dn("kcal"),
                mealType = j.intOrNull("mealType") ?: 0,
                nutrientGrams = nutrients,
            )
        }
        "BloodPressureRecord" -> BloodPressureRecord(
            time = i(),
            zoneOffset = io(),
            metadata = metadata,
            systolic = Pressure.millimetersOfMercury(d("sys")),
            diastolic = Pressure.millimetersOfMercury(d("dia")),
            // Both are encoded above; not reading them back silently erased
            // the posture and cuff site from every reading that crossed the
            // wire, which changes how the number should be interpreted and is
            // read by other apps sharing the same Health Connect store.
            bodyPosition = bloodPressureBodyPosition(j.intOrNull("bodyPos")),
            measurementLocation = bloodPressureMeasurementLocation(j.intOrNull("measLoc")),
        )
        "MenstruationFlowRecord" -> MenstruationFlowRecord(
            time = i(), zoneOffset = io(), metadata = metadata, flow = ni("flow"),
        )
        "MenstruationPeriodRecord" -> MenstruationPeriodRecord(
            startTime = s(), startZoneOffset = so(), endTime = e(), endZoneOffset = eo(),
            metadata = metadata,
        )
        "OvulationTestRecord" -> OvulationTestRecord(
            time = i(), zoneOffset = io(), result = ni("result"), metadata = metadata,
        )
        "CervicalMucusRecord" -> CervicalMucusRecord(
            time = i(), zoneOffset = io(), metadata = metadata,
            appearance = ni("appearance"), sensation = ni("sensation"),
        )
        "IntermenstrualBleedingRecord" -> IntermenstrualBleedingRecord(
            time = i(), zoneOffset = io(), metadata = metadata,
        )
        "SexualActivityRecord" -> SexualActivityRecord(
            time = i(), zoneOffset = io(), metadata = metadata, protectionUsed = ni("protection"),
        )
        else -> throw UnsupportedSyncRecordType(type)
    }
}

// ── Planned-exercise completion goals ────────────────────────────────────────

/**
 * The wire shape of a planned-exercise step completion goal: a small closed set
 * (repetitions / duration / manual / unknown), mirroring the Dart codec's
 * `ck`/`cr`/`cs` fields. Goal kinds outside this set degrade to `unknown` —
 * same policy as the app's own planned-exercise write path.
 */
private data class CodecCompletionGoal(val kind: Int, val repetitions: Int?, val seconds: Long?) {
    fun toExerciseCompletionGoal(): ExerciseCompletionGoal = when (kind) {
        GOAL_REPETITIONS -> ExerciseCompletionGoal.RepetitionsGoal(repetitions ?: 1)
        GOAL_DURATION ->
            ExerciseCompletionGoal.DurationGoal(Duration.ofSeconds((seconds ?: 1L).coerceAtLeast(1L)))
        GOAL_MANUAL -> ExerciseCompletionGoal.ManualCompletion
        else -> ExerciseCompletionGoal.UnknownGoal
    }
}

private const val GOAL_REPETITIONS = 0
private const val GOAL_DURATION = 1
private const val GOAL_MANUAL = 2
private const val GOAL_UNKNOWN = 3

private fun ExerciseCompletionGoal.toCodecGoal(): CodecCompletionGoal = when (this) {
    is ExerciseCompletionGoal.RepetitionsGoal -> CodecCompletionGoal(GOAL_REPETITIONS, repetitions, null)
    is ExerciseCompletionGoal.DurationGoal -> CodecCompletionGoal(GOAL_DURATION, null, duration.seconds)
    ExerciseCompletionGoal.ManualCompletion -> CodecCompletionGoal(GOAL_MANUAL, null, null)
    else -> CodecCompletionGoal(GOAL_UNKNOWN, null, null)
}

// ── Nutrition helpers ────────────────────────────────────────────────────────

/** The record's non-null nutrient masses as a name→grams map, for wire + fingerprint. */
private fun NutritionRecord.nutrientGrams(): Map<String, Double> = buildMap {
    fun add(name: String, mass: Mass?) {
        if (mass != null) put(name, mass.inGrams)
    }
    add("biotin", biotin)
    add("caffeine", caffeine)
    add("calcium", calcium)
    add("chloride", chloride)
    add("cholesterol", cholesterol)
    add("chromium", chromium)
    add("copper", copper)
    add("dietaryFiber", dietaryFiber)
    add("folate", folate)
    add("folicAcid", folicAcid)
    add("iodine", iodine)
    add("iron", iron)
    add("magnesium", magnesium)
    add("manganese", manganese)
    add("molybdenum", molybdenum)
    add("monounsaturatedFat", monounsaturatedFat)
    add("niacin", niacin)
    add("pantothenicAcid", pantothenicAcid)
    add("phosphorus", phosphorus)
    add("polyunsaturatedFat", polyunsaturatedFat)
    add("potassium", potassium)
    add("protein", protein)
    add("riboflavin", riboflavin)
    add("saturatedFat", saturatedFat)
    add("selenium", selenium)
    add("sodium", sodium)
    add("sugar", sugar)
    add("thiamin", thiamin)
    add("totalCarbohydrate", totalCarbohydrate)
    add("totalFat", totalFat)
    add("transFat", transFat)
    add("unsaturatedFat", unsaturatedFat)
    add("vitaminA", vitaminA)
    add("vitaminB12", vitaminB12)
    add("vitaminB6", vitaminB6)
    add("vitaminC", vitaminC)
    add("vitaminD", vitaminD)
    add("vitaminE", vitaminE)
    add("vitaminK", vitaminK)
    add("zinc", zinc)
}

private fun buildNutritionRecord(
    startTime: Instant,
    startZoneOffset: ZoneOffset?,
    endTime: Instant,
    endZoneOffset: ZoneOffset?,
    metadata: Metadata,
    name: String?,
    energyKilocalories: Double?,
    mealType: Int,
    nutrientGrams: Map<String, Double>,
): NutritionRecord {
    fun g(key: String): Mass? = nutrientGrams[key]?.let(Mass::grams)
    return NutritionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = endZoneOffset,
        metadata = metadata,
        biotin = g("biotin"),
        caffeine = g("caffeine"),
        calcium = g("calcium"),
        chloride = g("chloride"),
        cholesterol = g("cholesterol"),
        chromium = g("chromium"),
        copper = g("copper"),
        dietaryFiber = g("dietaryFiber"),
        energy = energyKilocalories?.let(Energy::kilocalories),
        folate = g("folate"),
        folicAcid = g("folicAcid"),
        iodine = g("iodine"),
        iron = g("iron"),
        magnesium = g("magnesium"),
        manganese = g("manganese"),
        molybdenum = g("molybdenum"),
        monounsaturatedFat = g("monounsaturatedFat"),
        niacin = g("niacin"),
        pantothenicAcid = g("pantothenicAcid"),
        phosphorus = g("phosphorus"),
        polyunsaturatedFat = g("polyunsaturatedFat"),
        potassium = g("potassium"),
        protein = g("protein"),
        riboflavin = g("riboflavin"),
        saturatedFat = g("saturatedFat"),
        selenium = g("selenium"),
        sodium = g("sodium"),
        sugar = g("sugar"),
        thiamin = g("thiamin"),
        totalCarbohydrate = g("totalCarbohydrate"),
        totalFat = g("totalFat"),
        transFat = g("transFat"),
        unsaturatedFat = g("unsaturatedFat"),
        vitaminA = g("vitaminA"),
        vitaminB12 = g("vitaminB12"),
        vitaminB6 = g("vitaminB6"),
        vitaminC = g("vitaminC"),
        vitaminD = g("vitaminD"),
        vitaminE = g("vitaminE"),
        vitaminK = g("vitaminK"),
        zinc = g("zinc"),
        name = name,
        mealType = mealType,
    )
}

// ── JSON field helpers ───────────────────────────────────────────────────────

private fun JsonObject.intOrNull(key: String): Int? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.int

private fun JsonObject.doubleOrNull(key: String): Double? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.double

private fun JsonObject.strOrNull(key: String): String? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

/**
 * A peer's skin-temperature measurement location, or UNKNOWN if it is not one
 * we recognise.
 *
 * The wire carries a bare int from another phone, and Health Connect's
 * parameter is a closed set. Passing an unrecognised value straight through
 * hands a foreign, possibly newer or simply corrupt, constant to the platform.
 * Recording "unknown" is both true and safe: the reading itself still lands,
 * only the site of it is dropped.
 */
/**
 * A peer's blood-pressure posture, or UNKNOWN if it is not one we recognise.
 *
 * Same reasoning as [skinTemperatureMeasurementLocation]: the wire carries a
 * bare int from another phone and the platform parameter is a closed set.
 */
private fun bloodPressureBodyPosition(raw: Int?): Int = when (raw) {
    BloodPressureRecord.BODY_POSITION_STANDING_UP,
    BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
    BloodPressureRecord.BODY_POSITION_LYING_DOWN,
    BloodPressureRecord.BODY_POSITION_RECLINING,
    -> raw
    else -> BloodPressureRecord.BODY_POSITION_UNKNOWN
}

/** A peer's cuff site, or UNKNOWN if it is not one we recognise. */
private fun bloodPressureMeasurementLocation(raw: Int?): Int = when (raw) {
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST,
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST,
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
    -> raw
    else -> BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN
}

private fun skinTemperatureMeasurementLocation(raw: Int?): Int = when (raw) {
    SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER,
    SkinTemperatureRecord.MEASUREMENT_LOCATION_TOE,
    SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
    -> raw
    else -> SkinTemperatureRecord.MEASUREMENT_LOCATION_UNKNOWN
}
