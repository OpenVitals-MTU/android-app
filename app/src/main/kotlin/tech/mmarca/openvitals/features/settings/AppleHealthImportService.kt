package tech.mmarca.openvitals.features.settings

import android.content.Context
import android.net.Uri
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.meters
import androidx.health.connect.client.units.percent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.SAXParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import tech.mmarca.openvitals.data.repository.HealthRepository
import kotlin.math.roundToLong

data class AppleHealthImportResult(
    val parsedRecords: Int,
    val importedRecords: Int,
    val unsupportedRecords: Int,
    val failedRecords: Int,
)

@Singleton
class AppleHealthImportService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val healthRepository: HealthRepository,
    ) {
        suspend fun importAppleHealthExport(uri: Uri): AppleHealthImportResult =
            withContext(Dispatchers.IO) {
                val input =
                    context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Unable to open Apple Health export.")

                input.use { rawInput ->
                    BufferedInputStream(rawInput).use { bufferedInput ->
                        if (bufferedInput.hasZipHeader()) {
                            parseZipExport(bufferedInput)
                        } else {
                            parseXmlExport(bufferedInput)
                        }
                    }
                }
            }

        private fun parseZipExport(input: InputStream): AppleHealthImportResult {
            ZipInputStream(input).use { zipInput ->
                while (true) {
                    val entry = zipInput.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.isAppleHealthExportXml()) {
                        return parseXmlExport(zipInput)
                    }
                }
            }
            throw IllegalArgumentException("Apple Health export.zip must contain export.xml.")
        }

        private fun parseXmlExport(input: InputStream): AppleHealthImportResult {
            val handler = AppleHealthXmlHandler(::insertBatch)
            val factory =
                SAXParserFactory.newInstance().apply {
                    isNamespaceAware = false
                    setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
                    setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
                    setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                }

            factory.newSAXParser().parse(input, handler)
            return handler.result()
        }

        private suspend fun insertBatch(records: List<Record>): AppleHealthBatchResult {
            if (records.isEmpty()) return AppleHealthBatchResult()

            return runCatching {
                healthRepository.insertImportedRecords(records)
                AppleHealthBatchResult(imported = records.size)
            }.getOrElse {
                records.fold(AppleHealthBatchResult()) { result, record ->
                    runCatching { healthRepository.insertImportedRecords(listOf(record)) }
                        .fold(
                            onSuccess = { result.copy(imported = result.imported + 1) },
                            onFailure = { result.copy(failed = result.failed + 1) },
                        )
                }
            }
        }
    }

private data class AppleHealthBatchResult(
    val imported: Int = 0,
    val failed: Int = 0,
)

private class AppleHealthXmlHandler(
    private val insertBatch: suspend (List<Record>) -> AppleHealthBatchResult,
) : DefaultHandler() {
    private val pendingRecords = mutableListOf<Record>()
    private var parsedRecords = 0
    private var importedRecords = 0
    private var unsupportedRecords = 0
    private var failedRecords = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes,
    ) {
        if (qName != "Record") return

        parsedRecords += 1
        val type = attributes.value("type") ?: return failed()
        if (type !in supportedAppleRecordTypes) {
            unsupportedRecords += 1
            return
        }

        runCatching { attributes.toHealthConnectRecord(type) }
            .onSuccess { record ->
                if (record == null) {
                    failed()
                } else {
                    pendingRecords += record
                    if (pendingRecords.size >= ImportBatchSize) {
                        flushPending()
                    }
                }
            }
            .onFailure { failed() }
    }

    fun result(): AppleHealthImportResult {
        flushPending()
        return AppleHealthImportResult(
            parsedRecords = parsedRecords,
            importedRecords = importedRecords,
            unsupportedRecords = unsupportedRecords,
            failedRecords = failedRecords,
        )
    }

    private fun flushPending() {
        if (pendingRecords.isEmpty()) return

        val records = pendingRecords.toList()
        pendingRecords.clear()
        val result = runBlocking { insertBatch(records) }
        importedRecords += result.imported
        failedRecords += result.failed
    }

    private fun failed() {
        failedRecords += 1
    }
}

private fun Attributes.toHealthConnectRecord(type: String): Record? {
    val rawValue = value("value") ?: return null
    val numericValue = rawValue.toDoubleOrNull() ?: return null
    val start = value("startDate")?.toAppleDateTime() ?: return null
    val end = value("endDate")?.toAppleDateTime() ?: start
    val unit = value("unit")
    val metadata = appleMetadata(type, start, end, rawValue, value("sourceName"))

    return when (type) {
        AppleStepCount -> {
            val count = numericValue.roundToLong().takeIf { it > 0 } ?: return null
            val interval = interval(start, end)
            StepsRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                count = count,
                metadata = metadata,
            )
        }

        AppleDistanceWalkingRunning,
        AppleDistanceCycling,
        AppleDistanceSwimming,
        AppleDistanceWheelchair,
        -> {
            val meters = numericValue.toMeters(unit)?.takeIf { it > 0.0 } ?: return null
            val interval = interval(start, end)
            DistanceRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                distance = meters.meters,
                metadata = metadata,
            )
        }

        AppleActiveEnergyBurned -> {
            val kilocalories = numericValue.toKilocalories(unit)?.takeIf { it > 0.0 } ?: return null
            val interval = interval(start, end)
            ActiveCaloriesBurnedRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                energy = kilocalories.kilocalories,
                metadata = metadata,
            )
        }

        AppleFlightsClimbed -> {
            val floors = numericValue.takeIf { it > 0.0 } ?: return null
            val interval = interval(start, end)
            FloorsClimbedRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                floors = floors,
                metadata = metadata,
            )
        }

        AppleHeartRate -> {
            val bpm = numericValue.roundToLong().takeIf { it in 1..300 } ?: return null
            val interval = interval(start, end)
            HeartRateRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                samples = listOf(HeartRateRecord.Sample(time = start.instant, beatsPerMinute = bpm)),
                metadata = metadata,
            )
        }

        AppleRestingHeartRate -> {
            val bpm = numericValue.roundToLong().takeIf { it in 1..300 } ?: return null
            RestingHeartRateRecord(
                time = start.instant,
                zoneOffset = start.offset,
                beatsPerMinute = bpm,
                metadata = metadata,
            )
        }

        AppleBodyMass -> {
            val kilograms = numericValue.toKilograms(unit)?.takeIf { it > 0.0 } ?: return null
            WeightRecord(
                time = start.instant,
                zoneOffset = start.offset,
                weight = kilograms.kilograms,
                metadata = metadata,
            )
        }

        AppleHeight -> {
            val meters = numericValue.toMeters(unit)?.takeIf { it > 0.0 } ?: return null
            HeightRecord(
                time = start.instant,
                zoneOffset = start.offset,
                height = meters.meters,
                metadata = metadata,
            )
        }

        AppleBodyFatPercentage -> {
            val percentage = numericValue.toPercentage(unit)?.takeIf { it in 0.0..100.0 } ?: return null
            BodyFatRecord(
                time = start.instant,
                zoneOffset = start.offset,
                percentage = percentage.percent,
                metadata = metadata,
            )
        }

        AppleLeanBodyMass -> {
            val kilograms = numericValue.toKilograms(unit)?.takeIf { it > 0.0 } ?: return null
            LeanBodyMassRecord(
                time = start.instant,
                zoneOffset = start.offset,
                mass = kilograms.kilograms,
                metadata = metadata,
            )
        }

        AppleBoneMass -> {
            val kilograms = numericValue.toKilograms(unit)?.takeIf { it > 0.0 } ?: return null
            BoneMassRecord(
                time = start.instant,
                zoneOffset = start.offset,
                mass = kilograms.kilograms,
                metadata = metadata,
            )
        }

        AppleBodyWaterMass -> {
            val kilograms = numericValue.toKilograms(unit)?.takeIf { it > 0.0 } ?: return null
            BodyWaterMassRecord(
                time = start.instant,
                zoneOffset = start.offset,
                mass = kilograms.kilograms,
                metadata = metadata,
            )
        }

        AppleDietaryWater -> {
            val milliliters = numericValue.toMilliliters(unit)?.takeIf { it > 0.0 } ?: return null
            val interval = interval(start, end)
            HydrationRecord(
                startTime = interval.start.instant,
                startZoneOffset = interval.start.offset,
                endTime = interval.end.instant,
                endZoneOffset = interval.end.offset,
                volume = Volume.milliliters(milliliters),
                metadata = metadata,
            )
        }

        AppleOxygenSaturation -> {
            val percentage = numericValue.toPercentage(unit)?.takeIf { it in 0.0..100.0 } ?: return null
            OxygenSaturationRecord(
                time = start.instant,
                zoneOffset = start.offset,
                percentage = percentage.percent,
                metadata = metadata,
            )
        }

        AppleRespiratoryRate -> {
            val rate = numericValue.takeIf { it > 0.0 } ?: return null
            RespiratoryRateRecord(
                time = start.instant,
                zoneOffset = start.offset,
                rate = rate,
                metadata = metadata,
            )
        }

        AppleBodyTemperature -> {
            val celsius = numericValue.toCelsius(unit) ?: return null
            BodyTemperatureRecord(
                time = start.instant,
                zoneOffset = start.offset,
                temperature = celsius.celsius,
                metadata = metadata,
            )
        }

        else -> null
    }
}

private data class AppleDateTime(
    val instant: Instant,
    val offset: ZoneOffset?,
)

private data class AppleInterval(
    val start: AppleDateTime,
    val end: AppleDateTime,
)

private fun interval(start: AppleDateTime, end: AppleDateTime): AppleInterval {
    val adjustedEnd =
        if (end.instant.isAfter(start.instant)) {
            end
        } else {
            end.copy(instant = start.instant.plusSeconds(1), offset = end.offset ?: start.offset)
        }
    return AppleInterval(start = start, end = adjustedEnd)
}

private fun String.toAppleDateTime(): AppleDateTime? {
    AppleOffsetDateFormats.forEach { formatter ->
        runCatching {
            val value = OffsetDateTime.parse(this, formatter)
            return AppleDateTime(value.toInstant(), value.offset)
        }
    }

    AppleLocalDateFormats.forEach { formatter ->
        runCatching {
            val value = LocalDateTime.parse(this, formatter)
            val offsetDateTime = value.atZone(ZoneId.systemDefault()).toOffsetDateTime()
            return AppleDateTime(offsetDateTime.toInstant(), offsetDateTime.offset)
        }
    }

    return runCatching {
        val instant = Instant.parse(this)
        AppleDateTime(instant, ZoneOffset.UTC)
    }.getOrNull()
}

private fun appleMetadata(
    type: String,
    start: AppleDateTime,
    end: AppleDateTime,
    rawValue: String,
    sourceName: String?,
): Metadata {
    val stableId =
        listOf(type, start.instant, end.instant, rawValue, sourceName.orEmpty())
            .joinToString("|")
    val typeName = type.substringAfterLast("Identifier").toStableIdSegment()
    val hash = Integer.toUnsignedString(stableId.hashCode(), 36)
    return Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = "apple_health_${typeName}_$hash",
    )
}

private fun String.toStableIdSegment(): String =
    lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "record" }

private fun Double.toMeters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "m", "meter", "meters" -> this
        "km", "kilometer", "kilometers" -> this * 1_000.0
        "cm", "centimeter", "centimeters" -> this / 100.0
        "mm", "millimeter", "millimeters" -> this / 1_000.0
        "mi", "mile", "miles" -> this * 1_609.344
        "yd", "yard", "yards" -> this * 0.9144
        "ft", "foot", "feet" -> this * 0.3048
        "in", "inch", "inches" -> this * 0.0254
        else -> null
    }

private fun Double.toKilograms(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kg", "kilogram", "kilograms" -> this
        "g", "gram", "grams" -> this / 1_000.0
        "lb", "lbs", "pound", "pounds" -> this * 0.45359237
        "oz", "ounce", "ounces" -> this * 0.028349523125
        "st", "stone", "stones" -> this * 6.35029318
        else -> null
    }

private fun Double.toKilocalories(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kcal", "cal", "calorie", "calories", "calories/hour" -> this
        "kj", "kilojoule", "kilojoules" -> this / 4.184
        "j", "joule", "joules" -> this / 4_184.0
        else -> null
    }

private fun Double.toMilliliters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "ml", "milliliter", "milliliters" -> this
        "l", "liter", "liters" -> this * 1_000.0
        "fl_oz_us", "floz", "fl oz", "oz" -> this * 29.5735295625
        else -> null
    }

private fun Double.toPercentage(unit: String?): Double? =
    when (unit) {
        "%" -> if (this <= 1.0) this * 100.0 else this
        else -> null
    }

private fun Double.toCelsius(unit: String?): Double? =
    when (unit) {
        "degC", "°C" -> this
        "degF", "°F" -> (this - 32.0) * 5.0 / 9.0
        else -> null
    }

private fun Attributes.value(name: String): String? = getValue(name)?.takeIf { it.isNotBlank() }

private fun String.isAppleHealthExportXml(): Boolean {
    val normalized = replace('\\', '/').substringAfterLast('/').lowercase(Locale.US)
    return normalized == "export.xml"
}

private fun BufferedInputStream.hasZipHeader(): Boolean {
    mark(4)
    val first = read()
    val second = read()
    reset()
    return first == 0x50 && second == 0x4B
}

private fun SAXParserFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
    runCatching { setFeature(feature, enabled) }
}

private val AppleOffsetDateFormats =
    listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    )

private val AppleLocalDateFormats =
    listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US),
    )

private const val ImportBatchSize = 500

private const val AppleStepCount = "HKQuantityTypeIdentifierStepCount"
private const val AppleDistanceWalkingRunning = "HKQuantityTypeIdentifierDistanceWalkingRunning"
private const val AppleDistanceCycling = "HKQuantityTypeIdentifierDistanceCycling"
private const val AppleDistanceSwimming = "HKQuantityTypeIdentifierDistanceSwimming"
private const val AppleDistanceWheelchair = "HKQuantityTypeIdentifierDistanceWheelchair"
private const val AppleActiveEnergyBurned = "HKQuantityTypeIdentifierActiveEnergyBurned"
private const val AppleFlightsClimbed = "HKQuantityTypeIdentifierFlightsClimbed"
private const val AppleHeartRate = "HKQuantityTypeIdentifierHeartRate"
private const val AppleRestingHeartRate = "HKQuantityTypeIdentifierRestingHeartRate"
private const val AppleBodyMass = "HKQuantityTypeIdentifierBodyMass"
private const val AppleHeight = "HKQuantityTypeIdentifierHeight"
private const val AppleBodyFatPercentage = "HKQuantityTypeIdentifierBodyFatPercentage"
private const val AppleLeanBodyMass = "HKQuantityTypeIdentifierLeanBodyMass"
private const val AppleBoneMass = "HKQuantityTypeIdentifierBoneMass"
private const val AppleBodyWaterMass = "HKQuantityTypeIdentifierBodyWaterMass"
private const val AppleDietaryWater = "HKQuantityTypeIdentifierDietaryWater"
private const val AppleOxygenSaturation = "HKQuantityTypeIdentifierOxygenSaturation"
private const val AppleRespiratoryRate = "HKQuantityTypeIdentifierRespiratoryRate"
private const val AppleBodyTemperature = "HKQuantityTypeIdentifierBodyTemperature"

private val supportedAppleRecordTypes =
    setOf(
        AppleStepCount,
        AppleDistanceWalkingRunning,
        AppleDistanceCycling,
        AppleDistanceSwimming,
        AppleDistanceWheelchair,
        AppleActiveEnergyBurned,
        AppleFlightsClimbed,
        AppleHeartRate,
        AppleRestingHeartRate,
        AppleBodyMass,
        AppleHeight,
        AppleBodyFatPercentage,
        AppleLeanBodyMass,
        AppleBoneMass,
        AppleBodyWaterMass,
        AppleDietaryWater,
        AppleOxygenSaturation,
        AppleRespiratoryRate,
        AppleBodyTemperature,
    )
