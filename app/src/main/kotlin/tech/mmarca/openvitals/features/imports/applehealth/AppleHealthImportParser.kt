package tech.mmarca.openvitals.features.imports.applehealth

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

internal data class AppleHealthParseOptions(
    val parseRouteFiles: Boolean = true,
    val parseRecordDetails: Boolean = true,
)

internal object AppleHealthImportParser {
    fun parse(
        input: BufferedInputStream,
        consumer: AppleHealthXmlEventConsumer? = null,
        options: AppleHealthParseOptions = AppleHealthParseOptions(),
        routeFiles: Map<String, AppleWorkoutRouteFile> = emptyMap(),
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
                val entry = zipInput.nextEntryOrThrow(entryName = null) ?: break
                if (!entry.isDirectory && entry.name.isAppleHealthExportXml()) {
                    val entryInput = CountingInputStream(NonClosingInputStream(zipInput))
                    return try {
                        parseXmlExport(entryInput, consumer, emptyMap(), parseRecordDetails)
                    } catch (error: Exception) {
                        throw error.asZipReadException(entry.name, entryInput.bytesRead)
                    }
                }
                zipInput.closeEntryOrThrow(entry.name)
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
        var exportXmlEntryName = "export.xml"
        var workoutRouteArchiveFailure: AppleWorkoutRouteArchiveFailure? = null
        try {
            try {
                ZipInputStream(input).use { zipInput ->
                    while (true) {
                        val entry = zipInput.nextEntryOrThrow(entryName = null) ?: break
                        if (!entry.isDirectory) {
                            when {
                                entry.name.isAppleHealthExportXml() -> {
                                    exportXmlEntryName = entry.name
                                    val entryInput = CountingInputStream(NonClosingInputStream(zipInput))
                                    try {
                                        exportXml.outputStream().use { output -> entryInput.copyTo(output) }
                                    } catch (error: Exception) {
                                        throw error.asZipReadException(entry.name, entryInput.bytesRead)
                                    }
                                    foundExportXml = true
                                }
                                entry.name.isAppleWorkoutRouteFile() -> {
                                    // Stream the GPX entry directly; buffering it as a byte array costs
                                    // megabytes per routed workout during the zip sweep.
                                    val entryInput = CountingInputStream(NonClosingInputStream(zipInput))
                                    try {
                                        AppleHealthImportRouteParser.parse(entry.name, entryInput)?.let { routeFile ->
                                            resolvedRouteFiles[routeFile.path] = routeFile
                                        }
                                    } catch (error: Exception) {
                                        throw error.asZipReadException(entry.name, entryInput.bytesRead)
                                    }
                                }
                            }
                        }
                        zipInput.closeEntryOrThrow(entry.name)
                    }
                }
            } catch (error: AppleHealthZipReadException) {
                val failedRoute = error.entryName?.takeIf(String::isAppleWorkoutRouteFile)
                if (!foundExportXml || failedRoute == null) throw error
                workoutRouteArchiveFailure = AppleWorkoutRouteArchiveFailure(
                    entryName = failedRoute,
                    decompressedBytesRead = error.decompressedBytesRead,
                )
            }
            if (!foundExportXml) {
                throw IllegalArgumentException("Apple Health export.zip must contain export.xml.")
            }
            return exportXml.inputStream().use { xmlInput ->
                try {
                    parseXmlExport(xmlInput, consumer, resolvedRouteFiles, parseRecordDetails).copy(
                        workoutRouteArchiveFailure = workoutRouteArchiveFailure,
                    )
                } catch (error: Exception) {
                    throw error.asZipReadException(exportXmlEntryName, exportXml.length())
                }
            }
        } finally {
            if (!exportXml.delete()) exportXml.deleteOnExit()
        }
    }

    private fun parseXmlExport(
        input: InputStream,
        consumer: AppleHealthXmlEventConsumer?,
        routeFiles: Map<String, AppleWorkoutRouteFile>,
        parseRecordDetails: Boolean,
    ): AppleParsedExport {
        val handler = AppleHealthXmlHandler(consumer, routeFiles, parseRecordDetails)
        // Apple's exporter always writes UTF-8 (declared in the XML prolog); decoding explicitly
        // lets the sanitizer inspect and repair characters before Expat ever sees them.
        val sanitizer = XmlCharacterSanitizingReader(InputStreamReader(input, StandardCharsets.UTF_8))
        try {
            secureSaxParserFactory().newSAXParser().parse(InputSource(sanitizer), handler)
        } catch (parseException: SAXParseException) {
            throw AppleHealthXmlParseException(parseException, sanitizer)
        }
        return handler.result().copy(
            sanitizedControlChars = sanitizer.strippedControlChars,
            sanitizedAmpersands = sanitizer.escapedAmpersands,
        )
    }
}

private fun ZipInputStream.nextEntryOrThrow(entryName: String?): java.util.zip.ZipEntry? =
    try {
        nextEntry
    } catch (error: Exception) {
        throw error.asZipReadException(entryName, bytesRead = null)
    }

private fun ZipInputStream.closeEntryOrThrow(entryName: String) {
    try {
        closeEntry()
    } catch (error: Exception) {
        throw error.asZipReadException(entryName, bytesRead = null)
    }
}

private fun Throwable.asZipReadException(
    entryName: String?,
    bytesRead: Long?,
): Throwable =
    if (isZipReadFailure() || isUnexpectedZipXmlEnd(entryName)) {
        AppleHealthZipReadException(entryName, bytesRead, this)
    } else {
        this
    }

private fun Throwable.isZipReadFailure(): Boolean =
    this is EOFException || this is ZipException

private fun Throwable.isUnexpectedZipXmlEnd(entryName: String?): Boolean {
    val isArchivedXmlEntry = entryName?.let { name ->
        name.isAppleHealthExportXml() || name.isAppleWorkoutRouteFile()
    } == true
    val isXmlParseFailure = this is AppleHealthXmlParseException || this is SAXParseException
    if (!isArchivedXmlEntry || !isXmlParseFailure) return false
    val normalizedMessage = message.orEmpty().lowercase(Locale.US)
    return UnexpectedXmlEndMessages.any(normalizedMessage::contains)
}

private val UnexpectedXmlEndMessages = listOf(
    "unexpected end",
    "premature end",
    "must start and end within the same entity",
    "no element found",
)

internal interface AppleHealthXmlEventConsumer {
    fun shouldMaterializeRecord(type: String): Boolean = true
    fun onParsedType(type: String)
    fun onRecordSkipped(type: String) = Unit
    fun onRecord(record: AppleRecord)
    fun onWorkout(workout: AppleWorkout)
    fun onCorrelation(correlation: AppleCorrelation)
    fun onActivitySummary()
}

private class AppleHealthXmlHandler(
    private val consumer: AppleHealthXmlEventConsumer?,
    private val routeFiles: Map<String, AppleWorkoutRouteFile>,
    private val parseRecordDetails: Boolean,
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
                val parentCorrelation = stack.lastOrNull() as? MutableAppleCorrelation
                val shouldMaterialize = parentCorrelation != null || consumer?.shouldMaterializeRecord(type) != false
                stack.addLast(
                    if (shouldMaterialize) {
                        MutableAppleRecord(attributes, parentCorrelation, parseRecordDetails)
                    } else {
                        consumer?.onRecordSkipped(type)
                        SkippedAppleRecord
                    },
                )
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
                if (stack.lastOrNull() === SkippedAppleRecord) return
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
                    date = attributes.appleDate("date", parseRecordDetails),
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
                val element = stack.removeLastOrNull()
                if (element === SkippedAppleRecord) return
                element as? MutableAppleRecord ?: return
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
                workout.addRouteReferences(referencedPaths)
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

private object SkippedAppleRecord : MutableAppleElement {
    override val metadata: MutableMap<String, String>
        get() = error("Skipped records do not collect metadata")
}

private class MutableAppleWorkoutRoute : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val paths = mutableListOf<String>()
}

private class MutableAppleRecord(
    attributes: Attributes,
    private val parentCorrelation: MutableAppleCorrelation?,
    private val parseDetails: Boolean,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    private val type = attributes.value("type") ?: "Record"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val unit = attributes.value("unit")
    private val creationDate = attributes.appleDate("creationDate", parseDetails)
    private val startDate = attributes.appleDate("startDate", parseDetails)
    private val endDate = attributes.appleDate("endDate", parseDetails)
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
    parseDetails: Boolean,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val events = mutableListOf<AppleWorkoutEvent>()
    private val routes = mutableListOf<AppleWorkoutRouteFile>()
    private val routeReferencePaths = mutableListOf<String>()
    private val workoutActivityType = attributes.value("workoutActivityType") ?: "Workout"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = attributes.appleDate("creationDate", parseDetails)
    private val startDate = attributes.appleDate("startDate", parseDetails)
    private val endDate = attributes.appleDate("endDate", parseDetails)
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

    fun addRouteReferences(paths: List<String>) {
        routeReferencePaths += paths
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
            routeReferences = routeReferencePaths.distinct().size,
            routeReferencePaths = routeReferencePaths.distinct(),
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
    parseDetails: Boolean,
) : MutableAppleElement {
    override val metadata: MutableMap<String, String> = linkedMapOf()
    val records = mutableListOf<AppleRecord>()
    val type: String = attributes.value("type") ?: "Correlation"
    private val sourceName = attributes.value("sourceName")
    private val sourceVersion = attributes.value("sourceVersion")
    private val device = attributes.value("device")
    private val creationDate = attributes.appleDate("creationDate", parseDetails)
    private val startDate = attributes.appleDate("startDate", parseDetails)
    private val endDate = attributes.appleDate("endDate", parseDetails)

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

private fun Attributes.appleDate(name: String, parseDetails: Boolean): AppleDateTime? =
    if (parseDetails) value(name)?.toAppleDateTime() else null

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
