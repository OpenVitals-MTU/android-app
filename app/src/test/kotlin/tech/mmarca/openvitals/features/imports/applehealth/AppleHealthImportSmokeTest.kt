package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.xml.sax.helpers.DefaultHandler

class AppleHealthImportSmokeTest {

    @Test
    fun `local Apple Health export parses completely`() {
        val exportPath = localExportPath()
        assumeTrue(
            "Set -PappleHealthExport=/path/to/export.zip, export.xml, or an unzipped Apple Health export directory.",
            exportPath != null,
        )
        requireNotNull(exportPath)
        require(Files.exists(exportPath)) { "Apple Health export path does not exist: $exportPath" }

        val consumer = StreamingAppleHealthSmokeConsumer()
        val parsed = openExportXml(exportPath) { input, routeFiles ->
            AppleHealthImportParser.parse(BufferedInputStream(input, SmokeTestInputBufferSize), consumer, routeFiles)
        }
        val conversion = consumer.finishConversion()

        assertEquals(consumer.records, parsed.parsedRecords)
        assertEquals(consumer.workouts, parsed.parsedWorkouts)
        assertEquals(consumer.correlations, parsed.parsedCorrelations)
        assertEquals(consumer.activitySummaries, parsed.parsedActivitySummaries)
        assertTrue(parsed.parsedRecords + parsed.parsedWorkouts + parsed.parsedCorrelations > 0)

        val gpxFiles = parseGpxFilesIfPresent(exportPath)
        println(
            "Apple Health smoke parse OK: records=${parsed.parsedRecords}, " +
                "workouts=${parsed.parsedWorkouts}, correlations=${parsed.parsedCorrelations}, " +
                "activitySummaries=${parsed.parsedActivitySummaries}, " +
                "converted=${conversion.convertedRecords}, unsupported=${conversion.unsupportedElements}, " +
                "skipped=${conversion.skippedRecords}, failed=${conversion.failedRecords}, " +
                "routeSessions=${conversion.routeSessions}, gpxFiles=$gpxFiles",
        )
    }

    private fun localExportPath(): Path? =
        System.getProperty("appleHealthExport")
            ?.takeIf { it.isNotBlank() }
            ?.let(::resolveLocalPath)

    private fun resolveLocalPath(rawPath: String): Path {
        val path = Path.of(rawPath)
        if (path.isAbsolute) return path.normalize()

        val userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val candidates = listOfNotNull(
            userDir.resolve(path).normalize(),
            userDir.parent?.resolve(path)?.normalize(),
        )
        return candidates.firstOrNull { Files.exists(it) } ?: candidates.first()
    }

    private fun <T> openExportXml(path: Path, parse: (InputStream, Map<String, AppleWorkoutRouteFile>) -> T): T =
        when {
            Files.isDirectory(path) -> {
                val exportXml = findExportXml(path)
                val routeFiles = parseDirectoryRouteFiles(path)
                Files.newInputStream(exportXml).use { parse(it, routeFiles) }
            }
            path.fileName.toString().lowercase(Locale.US).endsWith(".zip") -> {
                Files.newInputStream(path).use { parse(it, emptyMap()) }
            }
            else -> Files.newInputStream(path).use { parse(it, emptyMap()) }
        }

    private fun findExportXml(directory: Path): Path =
        Files.walk(directory, MaxExportSearchDepth).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().equals("export.xml", ignoreCase = true) }
                .findFirst()
                .orElseThrow { IllegalArgumentException("No export.xml found under $directory") }
        }

    private fun parseGpxFilesIfPresent(path: Path): Int =
        when {
            Files.isDirectory(path) -> parseDirectoryGpxFiles(path)
            path.fileName.toString().lowercase(Locale.US).endsWith(".zip") -> parseZipGpxFiles(path)
            else -> 0
        }

    private fun parseDirectoryGpxFiles(directory: Path): Int =
        Files.walk(directory, MaxRouteSearchDepth).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().lowercase(Locale.US).endsWith(".gpx") }
                .mapToInt { file ->
                    Files.newInputStream(file).use(::parseXmlFully)
                    1
                }
                .sum()
        }

    private fun parseDirectoryRouteFiles(directory: Path): Map<String, AppleWorkoutRouteFile> =
        Files.walk(directory, MaxRouteSearchDepth).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().lowercase(Locale.US).endsWith(".gpx") }
                .map { file ->
                    Files.newInputStream(file).use { input ->
                        AppleHealthImportRouteParser.parse(directory.relativize(file).toString(), input)
                    }
                }
                .filter { it != null }
                .map { requireNotNull(it) }
                .toList()
                .associateBy { it.path }
        }

    private fun parseZipGpxFiles(zipPath: Path): Int =
        ZipFile(zipPath.toFile()).use { zip ->
            zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.lowercase(Locale.US).endsWith(".gpx") }
                .map { entry ->
                    zip.getInputStream(entry).use(::parseXmlFully)
                    1
                }
                .sum()
        }

    private fun parseXmlFully(input: InputStream) {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
            setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
            setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
        factory.newSAXParser().parse(input, DefaultHandler())
    }

    private class StreamingAppleHealthSmokeConsumer : AppleHealthXmlEventConsumer {
        private val converter = AppleHealthImportConverter(
            mindfulnessAvailable = true,
            diagnosticLimit = SmokeDiagnosticLimit,
        )
        private val bufferedRecords = mutableListOf<AppleRecord>()
        private val overlapDedupRecords = mutableListOf<AppleRecord>()
        private val bufferedWorkouts = mutableListOf<AppleWorkout>()
        private var convertedRecords: Int = 0
        private var routeSessions: Int = 0

        var records: Int = 0
            private set
        var workouts: Int = 0
            private set
        var correlations: Int = 0
            private set
        var activitySummaries: Int = 0
            private set

        override fun onParsedType(type: String) {
            converter.markParsed(type)
        }

        override fun onRecord(record: AppleRecord) {
            records += 1
            converter.noteWorkoutOverlap(record)
            when {
                converter.shouldBufferForOverlapDedup(record) -> overlapDedupRecords += record
                converter.shouldBufferRecord(record) -> {
                    bufferedRecords += record
                    if (bufferedRecords.size >= SmokeBufferedRecordBatchSize) {
                        flushBufferedRecords()
                    }
                }
                else -> converter.convertStreamingRecord(record)?.let { convertedRecords += 1 }
            }
        }

        override fun onWorkout(workout: AppleWorkout) {
            workouts += 1
            bufferedWorkouts += workout
        }

        override fun onCorrelation(correlation: AppleCorrelation) {
            correlations += 1
            acceptConverted(
                converter.convertBufferedGroups(
                    records = emptyList(),
                    workouts = emptyList(),
                    correlations = listOf(correlation),
                    parsedActivitySummaries = 0,
                ),
            )
        }

        override fun onActivitySummary() {
            activitySummaries += 1
        }

        fun finishConversion(): SmokeConversionSummary {
            flushBufferedRecords()
            acceptConverted(
                converter.convertBufferedGroups(
                    records = overlapDedupRecords,
                    workouts = bufferedWorkouts,
                    correlations = emptyList(),
                    parsedActivitySummaries = activitySummaries,
                ),
            )
            overlapDedupRecords.clear()
            bufferedWorkouts.clear()

            val stats = converter.typeStats.values
            val statsConverted = stats.sumOf { it.converted }
            check(statsConverted == convertedRecords) {
                "Converted callback count $convertedRecords did not match type stats $statsConverted."
            }
            return SmokeConversionSummary(
                convertedRecords = convertedRecords,
                unsupportedElements = stats.sumOf { it.unsupported },
                skippedRecords = stats.sumOf { it.skipped },
                failedRecords = stats.sumOf { it.failed },
                routeSessions = routeSessions,
            )
        }

        private fun flushBufferedRecords() {
            if (bufferedRecords.isEmpty()) return
            acceptConverted(
                converter.convertBufferedGroups(
                    records = bufferedRecords,
                    workouts = emptyList(),
                    correlations = emptyList(),
                    parsedActivitySummaries = 0,
                ),
            )
            bufferedRecords.clear()
        }

        private fun acceptConverted(records: List<ConvertedAppleRecord>) {
            convertedRecords += records.size
            routeSessions += records.count { converted ->
                (converted.record as? ExerciseSessionRecord)?.exerciseRouteResult is ExerciseRouteResult.Data
            }
        }
    }

    private data class SmokeConversionSummary(
        val convertedRecords: Int,
        val unsupportedElements: Int,
        val skippedRecords: Int,
        val failedRecords: Int,
        val routeSessions: Int,
    )

    private companion object {
        const val SmokeTestInputBufferSize = 1024 * 1024
        const val SmokeBufferedRecordBatchSize = 2_000
        const val SmokeDiagnosticLimit = 20
        const val MaxExportSearchDepth = 4
        const val MaxRouteSearchDepth = 6
    }
}

private fun SAXParserFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
    runCatching { setFeature(feature, enabled) }
}
