package tech.mmarca.openvitals.features.manualentry

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint

data class RouteFileImport(
    val fileName: String?,
    val points: List<ExerciseRoutePoint>,
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
    val startTime: Instant,
    val endTime: Instant,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val hasRecordedTimestamps: Boolean = true,
    val hasImportedTimeRange: Boolean = true,
    val originalPointCount: Int = points.size,
)

@Singleton
class RouteFileImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun import(uri: Uri): RouteFileImport = withContext(Dispatchers.IO) {
        val fileName = uri.displayName(context)
        val routeBytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytesBounded(MaxRouteFileBytes, "Route file is too large.") }
            ?: throw IllegalArgumentException("Unable to read route file.")

        RouteFileParser.parseFile(routeBytes, fileName = fileName)
    }
}

internal object RouteFileParser {
    fun parseFile(fileBytes: ByteArray, fileName: String? = null): RouteFileImport {
        require(fileBytes.size <= MaxRouteFileBytes) {
            "Route file is too large."
        }
        try {
            if (fileBytes.isZipArchive() || fileName.hasExtension("kmz")) {
                return KmzRouteParser.parse(fileBytes, fileName = fileName)
            }

            val routeText = fileBytes.toString(Charsets.UTF_8)
            return if (fileName.hasExtension("kml") || routeText.contains("<kml", ignoreCase = true)) {
                KmlRouteParser.parse(routeText, fileName = fileName)
            } else {
                parse(routeText, fileName = fileName)
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalArgumentException("Route file is not a valid GPX, KML, or KMZ file.", error)
        }
    }

    fun parse(gpxText: String, fileName: String? = null): RouteFileImport {
        val document = routeDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(gpxText)))
        val metadata = document.routeMetadata()
        val routePoints = PointTags.flatMap { tag ->
            document.elementsByLocalName(tag).map { element ->
                MutableRoutePoint(
                    latitude = element.getAttribute("lat").toDoubleOrNull(),
                    longitude = element.getAttribute("lon").toDoubleOrNull(),
                    elevationMeters = element.directChildText("ele")?.trim()?.toDoubleOrNull(),
                    time = element.directChildText("time")?.trim()?.toInstantOrNull(),
                )
            }
        }.toRoutePoints()

        require(routePoints.size >= MinRoutePoints) {
            "GPX route must contain at least $MinRoutePoints timestamped location points."
        }

        return buildRouteImport(
            fileName = fileName,
            points = routePoints,
            metadata = metadata,
        )
    }
}

internal object KmzRouteParser {
    fun parse(kmzBytes: ByteArray, fileName: String? = null): RouteFileImport {
        val candidates = kmzBytes.zipRouteCandidates()
        require(candidates.isNotEmpty()) {
            "KMZ file must contain a .gpx or .kml route file."
        }

        val failures = mutableListOf<Throwable>()
        candidates.sortedWith(routeCandidateComparator()).forEach { candidate ->
            val result = runCatching {
                if (candidate.name.endsWith(".gpx", ignoreCase = true)) {
                    RouteFileParser.parse(candidate.bytes.toString(Charsets.UTF_8), fileName = fileName ?: candidate.name)
                } else {
                    KmlRouteParser.parse(candidate.bytes.toString(Charsets.UTF_8), fileName = fileName ?: candidate.name)
                }
            }
            result.onSuccess { return it }
            result.onFailure(failures::add)
        }

        throw IllegalArgumentException(
            failures.firstOrNull()?.message
                ?: "KMZ file must contain at least $MinRoutePoints route coordinates.",
        )
    }
}

internal object KmlRouteParser {
    fun parse(kmlText: String, fileName: String? = null): RouteFileImport {
        val document = routeDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(kmlText)))
        val tracks = document.elementsByLocalName("Track")
        val trackPoints = tracks.flatMap { track ->
            val times = track.directChildTexts("when").mapNotNull { it.trim().toInstantOrNull() }
            val coords = track.directChildTexts("coord")
            val pointCount = min(times.size, coords.size)
            List(pointCount) { index ->
                coords[index].toKmlTrackPoint(times[index])
            }
        }.toRoutePoints()

        if (trackPoints.isNotEmpty()) {
            require(trackPoints.size >= MinRoutePoints) {
                "KML/KMZ gx:Track route must contain at least $MinRoutePoints timestamped location points."
            }

            return buildRouteImport(
                fileName = fileName,
                points = trackPoints,
                metadata = tracks.firstOrNull().kmlRouteMetadata(document),
            )
        }

        val lineStrings = document.elementsByLocalName("LineString")
        val lineStringPoints = lineStrings.flatMap { lineString ->
            lineString.directChildText("coordinates")
                .orEmpty()
                .toKmlLineStringPoints()
        }
        require(lineStringPoints.size >= MinRoutePoints) {
            "KML/KMZ route must contain a timestamped gx:Track or LineString with at least $MinRoutePoints coordinates."
        }

        val timeRange = lineStrings.firstOrNull().kmlTimeRange(document)
        val startTime = timeRange?.start ?: SyntheticRouteStartTime
        val endTime = timeRange?.end
            ?: startTime.plusSeconds((lineStringPoints.size - 1).coerceAtLeast(1).toLong() * SyntheticRoutePointSpacingSeconds)
        return buildRouteImport(
            fileName = fileName,
            points = lineStringPoints.withSyntheticTimes(startTime, endTime).toRoutePoints(),
            metadata = lineStrings.firstOrNull().kmlRouteMetadata(document),
            hasRecordedTimestamps = false,
            hasImportedTimeRange = timeRange != null,
        )
    }
}

private fun buildRouteImport(
    fileName: String?,
    points: List<ExerciseRoutePoint>,
    metadata: RouteFileMetadata,
    hasRecordedTimestamps: Boolean = true,
    hasImportedTimeRange: Boolean = true,
): RouteFileImport {
    val sortedPoints = points
        .sortedBy { it.time }
        .distinctBy { it.time }
    require(sortedPoints.size >= MinRoutePoints) {
        "Route must contain at least $MinRoutePoints unique location points."
    }
    val simplifiedPoints = simplifyRoutePoints(sortedPoints)
    return RouteFileImport(
        fileName = fileName,
        points = simplifiedPoints,
        distanceMeters = routeDistanceMeters(sortedPoints),
        elevationGainedMeters = routeElevationGainMeters(sortedPoints),
        startTime = sortedPoints.first().time,
        endTime = sortedPoints.last().time,
        name = metadata.name,
        description = metadata.description,
        type = metadata.type,
        hasRecordedTimestamps = hasRecordedTimestamps,
        hasImportedTimeRange = hasImportedTimeRange,
        originalPointCount = sortedPoints.size,
    )
}

private data class RouteFileMetadata(
    val name: String?,
    val description: String?,
    val type: String?,
)

private data class MutableRoutePoint(
    val latitude: Double?,
    val longitude: Double?,
    var elevationMeters: Double? = null,
    var time: Instant? = null,
)

private data class RouteTimeRange(
    val start: Instant,
    val end: Instant,
) {
    companion object {
        fun of(start: Instant?, end: Instant?): RouteTimeRange? {
            val rangeStart = start ?: return null
            val rangeEnd = end ?: rangeStart.plusSeconds(DefaultSyntheticRouteDurationSeconds)
            return RouteTimeRange(rangeStart, rangeEnd)
                .takeIf { it.start.isBefore(it.end) }
        }
    }
}

private data class ZipRouteCandidate(
    val name: String,
    val bytes: ByteArray,
)

private fun Uri.displayName(context: Context): String? {
    context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
    return lastPathSegment
}

private fun Document.routeMetadata(): RouteFileMetadata {
    val routeElement = elementsByLocalName("trk").firstOrNull()
        ?: elementsByLocalName("rte").firstOrNull()
    val metadataElement = elementsByLocalName("metadata").firstOrNull()
    return RouteFileMetadata(
        name = routeElement?.directChildText("name").cleanText()
            ?: metadataElement?.directChildText("name").cleanText(),
        description = routeElement?.directChildText("desc").cleanText()
            ?: metadataElement?.directChildText("desc").cleanText(),
        type = routeElement?.directChildText("type").cleanText(),
    )
}

private fun Element?.kmlRouteMetadata(document: Document): RouteFileMetadata {
    val placemarkElement = this?.ancestorByLocalName("Placemark")
    val documentElement = document.elementsByLocalName("Document").firstOrNull()
    return RouteFileMetadata(
        name = placemarkElement?.directChildText("name").cleanText()
            ?: documentElement?.directChildText("name").cleanText(),
        description = placemarkElement?.directChildText("description").cleanText()
            ?: documentElement?.directChildText("description").cleanText(),
        type = null,
    )
}

private fun Element?.kmlTimeRange(document: Document): RouteTimeRange? {
    val placemarkElement = this?.ancestorByLocalName("Placemark")
    val documentElement = document.elementsByLocalName("Document").firstOrNull()
    return listOfNotNull(placemarkElement, documentElement)
        .firstNotNullOfOrNull { element ->
            val timeSpan = element.directChildElement("TimeSpan")
            val timeStamp = element.directChildElement("TimeStamp")
            RouteTimeRange.of(
                start = timeSpan?.directChildText("begin")?.toInstantOrNull()
                    ?: timeStamp?.directChildText("when")?.toInstantOrNull(),
                end = timeSpan?.directChildText("end")?.toInstantOrNull(),
            )
        }
}

private fun Document.elementsByLocalName(localName: String): List<Element> {
    val namespaced = getElementsByTagNameNS("*", localName)
    val plain = if (namespaced.length == 0) getElementsByTagName(localName) else namespaced
    return List(plain.length) { index -> plain.item(index) }
        .filterIsInstance<Element>()
}

private fun Element.directChildElement(localName: String): Element? {
    return List(childNodes.length) { index -> childNodes.item(index) }
        .filterIsInstance<Element>()
        .firstOrNull { child ->
            child.localName == localName || child.nodeName == localName || child.nodeName.endsWith(":$localName")
        }
}

private fun Element.directChildTexts(localName: String): List<String> {
    return List(childNodes.length) { index -> childNodes.item(index) }
        .filterIsInstance<Element>()
        .filter { child ->
            child.localName == localName || child.nodeName == localName || child.nodeName.endsWith(":$localName")
        }
        .mapNotNull { child -> child.textContent.cleanText() }
}

private fun Element.directChildText(localName: String): String? {
    return directChildTexts(localName).firstOrNull()
}

private fun Element.ancestorByLocalName(localName: String): Element? {
    var candidate = parentNode
    while (candidate != null) {
        if (candidate is Element && (candidate.localName == localName || candidate.nodeName == localName)) {
            return candidate
        }
        candidate = candidate.parentNode
    }
    return null
}

private fun String?.cleanText(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun routeDocumentBuilderFactory(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature("http://xml.org/sax/features/external-general-entities", false)
        trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
        trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }

private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
    runCatching { setFeature(name, value) }
}

private fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }
        .recoverCatching { OffsetDateTime.parse(this).toInstant() }
        .getOrNull()

private fun String.toKmlTrackPoint(time: Instant): MutableRoutePoint {
    val parts = trim().split(WhitespaceRegex)
    return MutableRoutePoint(
        longitude = parts.getOrNull(0)?.toDoubleOrNull(),
        latitude = parts.getOrNull(1)?.toDoubleOrNull(),
        elevationMeters = parts.getOrNull(2)?.toDoubleOrNull(),
        time = time,
    )
}

private fun String.toKmlLineStringPoints(): List<MutableRoutePoint> {
    return trim()
        .split(WhitespaceRegex)
        .mapNotNull { coordinate ->
            val parts = coordinate.split(',')
            MutableRoutePoint(
                longitude = parts.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null,
                latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null,
                elevationMeters = parts.getOrNull(2)?.toDoubleOrNull(),
            )
        }
}

private fun List<MutableRoutePoint>.withSyntheticTimes(startTime: Instant, endTime: Instant): List<MutableRoutePoint> {
    if (isEmpty()) return emptyList()
    val totalMillis = Duration.between(startTime, endTime)
        .toMillis()
        .coerceAtLeast(size.toLong())
    val lastOffset = (totalMillis - 1).coerceAtLeast(0L)
    return mapIndexed { index, point ->
        val offset = if (size == 1) {
            0L
        } else {
            lastOffset * index / (size - 1)
        }
        point.copy(time = startTime.plusMillis(offset))
    }
}

private fun List<MutableRoutePoint>.toRoutePoints(): List<ExerciseRoutePoint> =
    mapNotNull { point ->
        val time = point.time ?: return@mapNotNull null
        val latitude = point.latitude?.takeIf { it in MinLatitude..MaxLatitude } ?: return@mapNotNull null
        val longitude = point.longitude?.takeIf { it in MinLongitude..MaxLongitude } ?: return@mapNotNull null
        ExerciseRoutePoint(
            time = time,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = point.elevationMeters,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )
    }

private fun ByteArray.isZipArchive(): Boolean =
    size >= 4 && this[0] == 0x50.toByte() && this[1] == 0x4B.toByte()

private fun ByteArray.zipRouteCandidates(): List<ZipRouteCandidate> {
    val candidates = mutableListOf<ZipRouteCandidate>()
    ZipInputStream(ByteArrayInputStream(this)).use { zipInput ->
        while (true) {
            val entry = zipInput.nextEntry ?: break
            if (!entry.isDirectory && (entry.name.hasExtension("gpx") || entry.name.hasExtension("kml"))) {
                require(entry.size <= MaxKmzRouteEntryBytes || entry.size == -1L) {
                    "KMZ route entry is too large."
                }
                candidates += ZipRouteCandidate(
                    name = entry.name,
                    bytes = zipInput.readBytesBounded(MaxKmzRouteEntryBytes, "KMZ route entry is too large."),
                )
            }
            zipInput.closeEntry()
        }
    }
    return candidates
}

private fun routeCandidateComparator(): Comparator<ZipRouteCandidate> =
    compareBy<ZipRouteCandidate>(
        { if (it.name.equals("doc.kml", ignoreCase = true)) 0 else 1 },
        { if (it.name.endsWith(".gpx", ignoreCase = true)) 0 else 1 },
        { it.name },
    )

private fun String?.hasExtension(extension: String): Boolean =
    this?.substringAfterLast('.', missingDelimiterValue = "")
        ?.equals(extension, ignoreCase = true) == true

private fun InputStream.readBytesBounded(maxBytes: Int, message: String): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        if (total > maxBytes) {
            throw IllegalArgumentException(message)
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun simplifyRoutePoints(points: List<ExerciseRoutePoint>): List<ExerciseRoutePoint> {
    if (points.size <= MaxImportedRoutePoints) return points
    val step = points.lastIndex.toDouble() / (MaxImportedRoutePoints - 1).toDouble()
    return List(MaxImportedRoutePoints) { index ->
        points[(index * step).toInt().coerceIn(points.indices)]
    }.distinctBy { it.time }
}

internal fun routeDistanceMeters(points: List<ExerciseRoutePoint>): Double =
    points.zipWithNext().sumOf { (start, end) ->
        haversineMeters(
            startLatitude = start.latitude,
            startLongitude = start.longitude,
            endLatitude = end.latitude,
            endLongitude = end.longitude,
        )
    }

internal fun routeElevationGainMeters(points: List<ExerciseRoutePoint>): Double =
    points.zipWithNext().sumOf { (start, end) ->
        val startAltitude = start.altitudeMeters
        val endAltitude = end.altitudeMeters
        if (startAltitude != null && endAltitude != null) {
            (endAltitude - startAltitude).coerceAtLeast(0.0)
        } else {
            0.0
        }
    }

private fun haversineMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
): Double {
    val dLat = Math.toRadians(endLatitude - startLatitude)
    val dLon = Math.toRadians(endLongitude - startLongitude)
    val lat1 = Math.toRadians(startLatitude)
    val lat2 = Math.toRadians(endLatitude)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * EarthRadiusMeters * asin(sqrt(a))
}

private val PointTags = setOf("trkpt", "rtept")
private const val MinRoutePoints = 2
private const val MaxImportedRoutePoints = 2_000
internal const val MaxRouteFileBytes = 15 * 1024 * 1024
internal const val MaxKmzRouteEntryBytes = 15 * 1024 * 1024
private const val DefaultSyntheticRouteDurationSeconds = 30 * 60L
private const val SyntheticRoutePointSpacingSeconds = 10L
private val SyntheticRouteStartTime: Instant = Instant.EPOCH
private const val EarthRadiusMeters = 6_371_000.0
private const val MinLatitude = -90.0
private const val MaxLatitude = 90.0
private const val MinLongitude = -180.0
private const val MaxLongitude = 180.0
private val WhitespaceRegex = Regex("\\s+")
