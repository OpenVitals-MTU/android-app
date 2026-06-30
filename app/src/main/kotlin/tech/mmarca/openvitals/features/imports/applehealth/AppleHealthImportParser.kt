package tech.mmarca.openvitals.features.imports.applehealth

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
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

internal object AppleHealthImportParser {
    fun parse(input: BufferedInputStream): AppleParsedExport =
        parseInternal(input, consumer = null)

    fun parse(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer,
    ): AppleParsedExport =
        parseInternal(input, consumer = consumer)

    private fun parseInternal(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer?,
    ): AppleParsedExport =
        if (input.hasZipHeader()) {
            parseZipExport(input, consumer)
        } else {
            parseXmlExport(input, consumer)
        }

    private fun parseZipExport(input: InputStream, consumer: AppleHealthXmlEventConsumer?): AppleParsedExport {
        ZipInputStream(input).use { zipInput ->
            while (true) {
                val entry = zipInput.nextEntry ?: break
                if (!entry.isDirectory && entry.name.isAppleHealthExportXml()) {
                    return parseXmlExport(zipInput, consumer)
                }
            }
        }
        throw IllegalArgumentException("Apple Health export.zip must contain export.xml.")
    }

    private fun parseXmlExport(input: InputStream, consumer: AppleHealthXmlEventConsumer?): AppleParsedExport {
        val handler = AppleHealthXmlHandler(consumer)
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
}

internal interface AppleHealthXmlEventConsumer {
    fun onParsedType(type: String)
    fun onRecord(record: AppleRecord)
    fun onWorkout(workout: AppleWorkout)
    fun onCorrelation(correlation: AppleCorrelation)
    fun onActivitySummary()
}

private class AppleHealthXmlHandler(
    private val consumer: AppleHealthXmlEventConsumer?,
) : DefaultHandler() {
    private val stack = ArrayDeque<MutableAppleElement>()
    private val records = mutableListOf<AppleRecord>()
    private val workouts = mutableListOf<AppleWorkout>()
    private val correlations = mutableListOf<AppleCorrelation>()
    private val typeCounts = linkedMapOf<String, Int>()
    private var parsedRecords = 0
    private var parsedWorkouts = 0
    private var parsedCorrelations = 0
    private var parsedActivitySummaries = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes,
    ) {
        when (qName) {
            "Record" -> {
                parsedRecords += 1
                val type = attributes.value("type") ?: "Record"
                countType(type)
                consumer?.onParsedType(type)
                stack.addLast(MutableAppleRecord(attributes, stack.lastOrNull() as? MutableAppleCorrelation))
            }
            "Workout" -> {
                parsedWorkouts += 1
                val type = attributes.value("workoutActivityType") ?: "Workout"
                countType(type)
                consumer?.onParsedType(type)
                stack.addLast(MutableAppleWorkout(attributes))
            }
            "Correlation" -> {
                parsedCorrelations += 1
                val type = attributes.value("type") ?: "Correlation"
                countType(type)
                consumer?.onParsedType(type)
                stack.addLast(MutableAppleCorrelation(attributes))
            }
            "MetadataEntry" -> {
                val key = attributes.value("key")
                val value = attributes.value("value")
                if (key != null && value != null) {
                    stack.lastOrNull()?.metadata?.set(key, value)
                }
            }
            "WorkoutEvent" -> {
                val workout = stack.lastOrNull() as? MutableAppleWorkout ?: return
                workout.events += AppleWorkoutEvent(
                    type = attributes.value("type"),
                    date = attributes.value("date")?.toAppleDateTime(),
                    duration = attributes.value("duration")?.toDoubleOrNull(),
                    durationUnit = attributes.value("durationUnit"),
                )
            }
            "ActivitySummary" -> {
                parsedActivitySummaries += 1
                countType("ActivitySummary")
                consumer?.onParsedType("ActivitySummary")
                consumer?.onActivitySummary()
            }
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "Record" -> {
                val element = stack.removeLastOrNull() as? MutableAppleRecord ?: return
                val record = element.toRecord()
                val parent = stack.lastOrNull() as? MutableAppleCorrelation
                if (parent != null) {
                    parent.records += record.copy(correlationType = parent.type)
                } else {
                    if (consumer != null) {
                        consumer.onRecord(record)
                    } else {
                        records += record
                    }
                }
            }
            "Workout" -> {
                val element = stack.removeLastOrNull() as? MutableAppleWorkout ?: return
                val workout = element.toWorkout()
                if (consumer != null) {
                    consumer.onWorkout(workout)
                } else {
                    workouts += workout
                }
            }
            "Correlation" -> {
                val element = stack.removeLastOrNull() as? MutableAppleCorrelation ?: return
                val correlation = element.toCorrelation()
                if (consumer != null) {
                    consumer.onCorrelation(correlation)
                } else {
                    correlations += correlation
                }
            }
        }
    }

    fun result(): AppleParsedExport =
        AppleParsedExport(
            records = records,
            workouts = workouts,
            correlations = correlations,
            parsedRecords = parsedRecords,
            parsedWorkouts = parsedWorkouts,
            parsedCorrelations = parsedCorrelations,
            parsedActivitySummaries = parsedActivitySummaries,
            parsedTypeCounts = typeCounts,
        )

    private fun countType(type: String) {
        typeCounts[type] = (typeCounts[type] ?: 0) + 1
    }
}

private sealed interface MutableAppleElement {
    val metadata: MutableMap<String, String>
}

private class MutableAppleRecord(
    attributes: Attributes,
    private val parentCorrelation: MutableAppleCorrelation?,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    private val type = attributes.value("type") ?: "Record"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val unit = attributes.value("unit")
    private val creationDate = attributes.value("creationDate")?.toAppleDateTime()
    private val startDate = attributes.value("startDate")?.toAppleDateTime()
    private val endDate = attributes.value("endDate")?.toAppleDateTime()
    private val rawValue = attributes.value("value")

    fun toRecord(): AppleRecord {
        return AppleRecord(
            type = type,
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            device = device,
            unit = unit,
            creationDate = creationDate,
            startDate = startDate,
            endDate = endDate,
            rawValue = rawValue,
            numericValue = rawValue?.toDoubleOrNull(),
            metadata = metadata.toMap(),
            correlationType = parentCorrelation?.type,
        )
    }
}

private class MutableAppleWorkout(
    attributes: Attributes,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val events = mutableListOf<AppleWorkoutEvent>()
    private val workoutActivityType = attributes.value("workoutActivityType") ?: "Workout"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = attributes.value("creationDate")?.toAppleDateTime()
    private val startDate = attributes.value("startDate")?.toAppleDateTime()
    private val endDate = attributes.value("endDate")?.toAppleDateTime()
    private val duration = attributes.value("duration")?.toDoubleOrNull()
    private val durationUnit = attributes.value("durationUnit")
    private val totalDistance = attributes.value("totalDistance")?.toDoubleOrNull()
    private val totalDistanceUnit = attributes.value("totalDistanceUnit")
    private val totalEnergyBurned = attributes.value("totalEnergyBurned")?.toDoubleOrNull()
    private val totalEnergyBurnedUnit = attributes.value("totalEnergyBurnedUnit")

    fun toWorkout(): AppleWorkout =
        AppleWorkout(
            workoutActivityType = workoutActivityType,
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            device = device,
            creationDate = creationDate,
            startDate = startDate,
            endDate = endDate,
            duration = duration,
            durationUnit = durationUnit,
            totalDistance = totalDistance,
            totalDistanceUnit = totalDistanceUnit,
            totalEnergyBurned = totalEnergyBurned,
            totalEnergyBurnedUnit = totalEnergyBurnedUnit,
            metadata = metadata.toMap(),
            events = events.toList(),
        )
}

private class MutableAppleCorrelation(
    attributes: Attributes,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val records = mutableListOf<AppleRecord>()
    val type: String = attributes.value("type") ?: "Correlation"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = attributes.value("creationDate")?.toAppleDateTime()
    private val startDate = attributes.value("startDate")?.toAppleDateTime()
    private val endDate = attributes.value("endDate")?.toAppleDateTime()

    fun toCorrelation(): AppleCorrelation =
        AppleCorrelation(
            type = type,
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            device = device,
            creationDate = creationDate,
            startDate = startDate,
            endDate = endDate,
            metadata = metadata.toMap(),
            records = records.toList(),
        )
}

internal fun String.toAppleDateTime(): AppleDateTime? {
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
        AppleDateTime(Instant.parse(this), ZoneOffset.UTC)
    }.getOrNull()
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
