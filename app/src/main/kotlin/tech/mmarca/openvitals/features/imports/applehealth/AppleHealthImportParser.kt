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
        if (input.hasZipHeader()) {
            parseZipExport(input)
        } else {
            parseXmlExport(input)
        }

    private fun parseZipExport(input: InputStream): AppleParsedExport {
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

    private fun parseXmlExport(input: InputStream): AppleParsedExport {
        val handler = AppleHealthXmlHandler()
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

private class AppleHealthXmlHandler : DefaultHandler() {
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
                stack.addLast(MutableAppleRecord(attributes.snapshot(), stack.lastOrNull() as? MutableAppleCorrelation))
            }
            "Workout" -> {
                parsedWorkouts += 1
                val type = attributes.value("workoutActivityType") ?: "Workout"
                countType(type)
                stack.addLast(MutableAppleWorkout(attributes.snapshot()))
            }
            "Correlation" -> {
                parsedCorrelations += 1
                val type = attributes.value("type") ?: "Correlation"
                countType(type)
                stack.addLast(MutableAppleCorrelation(attributes.snapshot()))
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
                    records += record
                }
            }
            "Workout" -> {
                val element = stack.removeLastOrNull() as? MutableAppleWorkout ?: return
                workouts += element.toWorkout()
            }
            "Correlation" -> {
                val element = stack.removeLastOrNull() as? MutableAppleCorrelation ?: return
                correlations += element.toCorrelation()
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
    private val attributes: Map<String, String>,
    private val parentCorrelation: MutableAppleCorrelation?,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()

    fun toRecord(): AppleRecord {
        val rawValue = attributes.value("value")
        return AppleRecord(
            type = attributes.value("type") ?: "Record",
            sourceName = attributes.value("sourceName"),
            sourceVersion = attributes.value("sourceVersion"),
            device = attributes.value("device"),
            unit = attributes.value("unit"),
            creationDate = attributes.value("creationDate")?.toAppleDateTime(),
            startDate = attributes.value("startDate")?.toAppleDateTime(),
            endDate = attributes.value("endDate")?.toAppleDateTime(),
            rawValue = rawValue,
            numericValue = rawValue?.toDoubleOrNull(),
            metadata = metadata.toMap(),
            correlationType = parentCorrelation?.type,
        )
    }
}

private class MutableAppleWorkout(
    private val attributes: Map<String, String>,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val events = mutableListOf<AppleWorkoutEvent>()

    fun toWorkout(): AppleWorkout =
        AppleWorkout(
            workoutActivityType = attributes.value("workoutActivityType") ?: "Workout",
            sourceName = attributes.value("sourceName"),
            sourceVersion = attributes.value("sourceVersion"),
            device = attributes.value("device"),
            creationDate = attributes.value("creationDate")?.toAppleDateTime(),
            startDate = attributes.value("startDate")?.toAppleDateTime(),
            endDate = attributes.value("endDate")?.toAppleDateTime(),
            duration = attributes.value("duration")?.toDoubleOrNull(),
            durationUnit = attributes.value("durationUnit"),
            totalDistance = attributes.value("totalDistance")?.toDoubleOrNull(),
            totalDistanceUnit = attributes.value("totalDistanceUnit"),
            totalEnergyBurned = attributes.value("totalEnergyBurned")?.toDoubleOrNull(),
            totalEnergyBurnedUnit = attributes.value("totalEnergyBurnedUnit"),
            metadata = metadata.toMap(),
            events = events.toList(),
        )
}

private class MutableAppleCorrelation(
    private val attributes: Map<String, String>,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val records = mutableListOf<AppleRecord>()
    val type: String get() = attributes.value("type") ?: "Correlation"

    fun toCorrelation(): AppleCorrelation =
        AppleCorrelation(
            type = type,
            sourceName = attributes.value("sourceName"),
            sourceVersion = attributes.value("sourceVersion"),
            device = attributes.value("device"),
            creationDate = attributes.value("creationDate")?.toAppleDateTime(),
            startDate = attributes.value("startDate")?.toAppleDateTime(),
            endDate = attributes.value("endDate")?.toAppleDateTime(),
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

private fun Attributes.snapshot(): Map<String, String> =
    buildMap {
        repeat(length) { index ->
            val name = getQName(index).ifBlank { getLocalName(index) }
            val value = getValue(index)
            if (name.isNotBlank() && value.isNotBlank()) {
                put(name, value)
            }
        }
    }

private fun Map<String, String>.value(name: String): String? = get(name)?.takeIf { it.isNotBlank() }

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
