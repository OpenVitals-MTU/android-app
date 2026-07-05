package tech.mmarca.openvitals.features.imports.applehealth

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FilterInputStream
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

internal data class AppleHealthParseOptions(
    val parseRouteFiles: Boolean = true,
    val parseRecordDetails: Boolean = true,
)

internal object AppleHealthImportParser {
    fun parse(input: BufferedInputStream): AppleParsedExport =
        parseInternal(input, consumer = null, routeFiles = emptyMap(), options = AppleHealthParseOptions())

    fun parse(
        input: BufferedInputStream,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
    ): AppleParsedExport =
        parseInternal(input, consumer = null, routeFiles = routeFiles, options = AppleHealthParseOptions())

    fun parse(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer,
    ): AppleParsedExport =
        parseInternal(input, consumer = consumer, routeFiles = emptyMap(), options = AppleHealthParseOptions())

    fun parse(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer,
        options: AppleHealthParseOptions,
    ): AppleParsedExport =
        parseInternal(input, consumer = consumer, routeFiles = emptyMap(), options = options)

    fun parse(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
    ): AppleParsedExport =
        parseInternal(input, consumer = consumer, routeFiles = routeFiles, options = AppleHealthParseOptions())

    private fun parseInternal(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer?,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
        options: AppleHealthParseOptions,
    ): AppleParsedExport =
        if (input.hasZipHeader()) {
            parseZipExport(input, consumer, routeFiles, options)
        } else {
            parseXmlExport(input, consumer, routeFiles, options.parseRecordDetails)
        }

    private fun parseZipExport(
        input: InputStream,
        consumer: AppleHealthXmlEventConsumer?,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
        options: AppleHealthParseOptions,
    ): AppleParsedExport =
        if (options.parseRouteFiles) {
            parseZipExportWithRouteFiles(input, consumer, routeFiles, options.parseRecordDetails)
        } else {
            parseZipExportStreaming(input, consumer, options.parseRecordDetails)
        }

    /**
     * Parses export.xml directly off the ZIP stream without extracting it to a temp file.
     * Only valid when workout route files are not needed, since those may appear after
     * export.xml in the archive.
     */
    private fun parseZipExportStreaming(
        input: InputStream,
        consumer: AppleHealthXmlEventConsumer?,
        parseRecordDetails: Boolean,
    ): AppleParsedExport {
        ZipInputStream(input).use { zipInput ->
            while (true) {
                val entry = zipInput.nextEntry ?: break
                if (!entry.isDirectory && entry.name.isAppleHealthExportXml()) {
                    return parseXmlExport(NonClosingInputStream(zipInput), consumer, emptyMap(), parseRecordDetails)
                }
                zipInput.closeEntry()
            }
        }
        throw IllegalArgumentException("Apple Health export.zip must contain export.xml.")
    }

    private fun parseZipExportWithRouteFiles(
        input: InputStream,
        consumer: AppleHealthXmlEventConsumer?,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
        parseRecordDetails: Boolean,
    ): AppleParsedExport {
        val exportXml = File.createTempFile("openvitals_apple_health_export", ".xml")
        val resolvedRouteFiles = routeFiles.toMutableMap()
        var foundExportXml = false
        try {
            ZipInputStream(input).use { zipInput ->
                while (true) {
                    val entry = zipInput.nextEntry ?: break
                    if (!entry.isDirectory) {
                        when {
                            entry.name.isAppleHealthExportXml() -> {
                                exportXml.outputStream().use { output -> zipInput.copyTo(output) }
                                foundExportXml = true
                            }
                            entry.name.isAppleWorkoutRouteFile() -> {
                                val routeBytes = zipInput.readBytes()
                                AppleHealthImportRouteParser.parse(entry.name, ByteArrayInputStream(routeBytes))?.let { routeFile ->
                                    resolvedRouteFiles[routeFile.path] = routeFile
                                }
                            }
                        }
                    }
                    zipInput.closeEntry()
                }
            }
            if (!foundExportXml) {
                throw IllegalArgumentException("Apple Health export.zip must contain export.xml.")
            }
            return exportXml.inputStream().use { xmlInput ->
                parseXmlExport(xmlInput, consumer, resolvedRouteFiles, parseRecordDetails)
            }
        } finally {
            exportXml.delete()
        }
    }

    private fun parseXmlExport(
        input: InputStream,
        consumer: AppleHealthXmlEventConsumer?,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
        parseRecordDetails: Boolean = true,
    ): AppleParsedExport {
        val handler = AppleHealthXmlHandler(consumer, routeFiles, parseRecordDetails)
        val factory =
            SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
                setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
                setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
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

/** Prevents the SAX parser from closing the underlying ZipInputStream when streaming an entry. */
private class NonClosingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    override fun close() = Unit
}

private class AppleHealthXmlHandler(
    private val consumer: AppleHealthXmlEventConsumer?,
    private val routeFiles: Map<String, AppleWorkoutRouteFile>,
    private val parseRecordDetails: Boolean = true,
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
                stack.addLast(MutableAppleRecord(attributes, stack.lastOrNull() as? MutableAppleCorrelation, parseRecordDetails))
            }
            "Workout" -> {
                parsedWorkouts += 1
                val type = attributes.value("workoutActivityType") ?: "Workout"
                countType(type)
                consumer?.onParsedType(type)
                stack.addLast(MutableAppleWorkout(attributes, parseRecordDetails))
            }
            "Correlation" -> {
                parsedCorrelations += 1
                val type = attributes.value("type") ?: "Correlation"
                countType(type)
                consumer?.onParsedType(type)
                stack.addLast(MutableAppleCorrelation(attributes, parseRecordDetails))
            }
            "MetadataEntry" -> {
                if (!parseRecordDetails) return
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
                    date = if (parseRecordDetails) attributes.value("date")?.toAppleDateTime() else null,
                    duration = attributes.value("duration")?.toDoubleOrNull(),
                    durationUnit = attributes.value("durationUnit"),
                )
            }
            "WorkoutStatistics" -> {
                val workout = stack.lastOrNull() as? MutableAppleWorkout ?: return
                workout.addStatistic(attributes)
            }
            "WorkoutRoute" -> {
                if (stack.lastOrNull() is MutableAppleWorkout) {
                    stack.addLast(MutableAppleWorkoutRoute())
                }
            }
            "FileReference" -> {
                val route = stack.lastOrNull() as? MutableAppleWorkoutRoute ?: return
                attributes.value("path")?.let { route.paths += it }
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
            "WorkoutRoute" -> {
                val route = stack.removeLastOrNull() as? MutableAppleWorkoutRoute ?: return
                val workout = stack.lastOrNull() as? MutableAppleWorkout ?: return
                val referencedPaths = route.paths
                    .map { it.normalizedAppleWorkoutRoutePath() }
                    .distinct()
                workout.addRouteReferences(referencedPaths.size)
                referencedPaths
                    .mapNotNull(routeFiles::get)
                    .forEach(workout::addRoute)
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

private class MutableAppleWorkoutRoute : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val paths = mutableListOf<String>()
}

private class MutableAppleRecord(
    attributes: Attributes,
    private val parentCorrelation: MutableAppleCorrelation?,
    private val parseDetails: Boolean = true,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    private val type = attributes.value("type") ?: "Record"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val unit = attributes.value("unit")
    private val creationDate = if (parseDetails) attributes.value("creationDate")?.toAppleDateTime() else null
    private val startDate = if (parseDetails) attributes.value("startDate")?.toAppleDateTime() else null
    private val endDate = if (parseDetails) attributes.value("endDate")?.toAppleDateTime() else null
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
            numericValue = if (parseDetails) rawValue?.toDoubleOrNull() else null,
            metadata = metadata.toMap(),
            correlationType = parentCorrelation?.type,
        )
    }
}

private class MutableAppleWorkout(
    attributes: Attributes,
    parseDetails: Boolean = true,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val events = mutableListOf<AppleWorkoutEvent>()
    private val routes = mutableListOf<AppleWorkoutRouteFile>()
    private var routeReferences = 0
    private val workoutActivityType = attributes.value("workoutActivityType") ?: "Workout"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = if (parseDetails) attributes.value("creationDate")?.toAppleDateTime() else null
    private val startDate = if (parseDetails) attributes.value("startDate")?.toAppleDateTime() else null
    private val endDate = if (parseDetails) attributes.value("endDate")?.toAppleDateTime() else null
    private val duration = attributes.value("duration")?.toDoubleOrNull()
    private val durationUnit = attributes.value("durationUnit")
    private var totalDistance = attributes.value("totalDistance")?.toDoubleOrNull()
    private var totalDistanceUnit = attributes.value("totalDistanceUnit")
    private var totalEnergyBurned = attributes.value("totalEnergyBurned")?.toDoubleOrNull()
    private var totalEnergyBurnedUnit = attributes.value("totalEnergyBurnedUnit")
    private val hasTotalDistanceAttribute = totalDistance != null
    private val hasTotalEnergyBurnedAttribute = totalEnergyBurned != null

    fun addStatistic(attributes: Attributes) {
        val type = attributes.value("type") ?: return
        val sum = attributes.value("sum")?.toDoubleOrNull() ?: return
        val unit = attributes.value("unit")
        when (type) {
            in AppleDistanceTypes -> {
                if (!hasTotalDistanceAttribute) {
                    totalDistance = totalDistance.addCompatible(sum, totalDistanceUnit, unit)
                }
                totalDistanceUnit = totalDistanceUnit ?: unit
            }
            AppleActiveEnergyBurned -> {
                if (!hasTotalEnergyBurnedAttribute) {
                    totalEnergyBurned = totalEnergyBurned.addCompatible(sum, totalEnergyBurnedUnit, unit)
                }
                totalEnergyBurnedUnit = totalEnergyBurnedUnit ?: unit
            }
        }
    }

    fun addRoute(route: AppleWorkoutRouteFile) {
        routes += route
    }

    fun addRouteReferences(count: Int) {
        routeReferences += count
    }

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
            routes = routes.distinctBy { it.path },
            routeReferences = routeReferences,
        )
}

private fun Double?.addCompatible(value: Double, currentUnit: String?, valueUnit: String?): Double =
    if (this == null || currentUnit == null || currentUnit == valueUnit) {
        (this ?: 0.0) + value
    } else {
        this
    }

private class MutableAppleCorrelation(
    attributes: Attributes,
    parseDetails: Boolean = true,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val records = mutableListOf<AppleRecord>()
    val type: String = attributes.value("type") ?: "Correlation"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = if (parseDetails) attributes.value("creationDate")?.toAppleDateTime() else null
    private val startDate = if (parseDetails) attributes.value("startDate")?.toAppleDateTime() else null
    private val endDate = if (parseDetails) attributes.value("endDate")?.toAppleDateTime() else null

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

private fun String.isAppleWorkoutRouteFile(): Boolean =
    normalizedAppleWorkoutRoutePath().let { path ->
        path.startsWith("workout-routes/") && path.endsWith(".gpx")
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
