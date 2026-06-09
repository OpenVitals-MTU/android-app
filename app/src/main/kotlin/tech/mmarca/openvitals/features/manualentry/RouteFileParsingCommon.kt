package tech.mmarca.openvitals.features.manualentry

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.w3c.dom.Document
import org.w3c.dom.Element
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint

internal data class RouteFileMetadata(
    val name: String?,
    val description: String?,
    val type: String?,
)

internal data class MutableRoutePoint(
    val latitude: Double?,
    val longitude: Double?,
    var elevationMeters: Double? = null,
    var time: Instant? = null,
)

internal fun buildRouteImport(
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

internal fun Document.elementsByLocalName(localName: String): List<Element> {
    val namespaced = getElementsByTagNameNS("*", localName)
    val plain = if (namespaced.length == 0) getElementsByTagName(localName) else namespaced
    return List(plain.length) { index -> plain.item(index) }
        .filterIsInstance<Element>()
}

internal fun Element.directChildElement(localName: String): Element? {
    return List(childNodes.length) { index -> childNodes.item(index) }
        .filterIsInstance<Element>()
        .firstOrNull { child ->
            child.localName == localName || child.nodeName == localName || child.nodeName.endsWith(":$localName")
        }
}

internal fun Element.directChildTexts(localName: String): List<String> {
    return List(childNodes.length) { index -> childNodes.item(index) }
        .filterIsInstance<Element>()
        .filter { child ->
            child.localName == localName || child.nodeName == localName || child.nodeName.endsWith(":$localName")
        }
        .mapNotNull { child -> child.textContent.cleanText() }
}

internal fun Element.directChildText(localName: String): String? =
    directChildTexts(localName).firstOrNull()

internal fun Element.ancestorByLocalName(localName: String): Element? {
    var candidate = parentNode
    while (candidate != null) {
        if (candidate is Element && (candidate.localName == localName || candidate.nodeName == localName)) {
            return candidate
        }
        candidate = candidate.parentNode
    }
    return null
}

internal fun String?.cleanText(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

internal fun routeDocumentBuilderFactory(): DocumentBuilderFactory =
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

internal fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }
        .recoverCatching { java.time.OffsetDateTime.parse(this).toInstant() }
        .getOrNull()

internal fun List<MutableRoutePoint>.toRoutePoints(): List<ExerciseRoutePoint> =
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

internal fun ByteArray.isZipArchive(): Boolean =
    size >= 4 && this[0] == 0x50.toByte() && this[1] == 0x4B.toByte()

internal fun String?.hasExtension(extension: String): Boolean =
    this?.substringAfterLast('.', missingDelimiterValue = "")
        ?.equals(extension, ignoreCase = true) == true

internal fun InputStream.readBytesBounded(maxBytes: Int, message: String): ByteArray {
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

private fun simplifyRoutePoints(points: List<ExerciseRoutePoint>): List<ExerciseRoutePoint> {
    if (points.size <= MaxImportedRoutePoints) return points
    val step = points.lastIndex.toDouble() / (MaxImportedRoutePoints - 1).toDouble()
    return List(MaxImportedRoutePoints) { index ->
        points[(index * step).toInt().coerceIn(points.indices)]
    }.distinctBy { it.time }
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

internal const val MinRoutePoints = 2
private const val MaxImportedRoutePoints = 2_000
internal const val MaxRouteFileBytes = 15 * 1024 * 1024
internal const val MaxKmzRouteEntryBytes = 15 * 1024 * 1024
private const val EarthRadiusMeters = 6_371_000.0
internal const val MinLatitude = -90.0
internal const val MaxLatitude = 90.0
internal const val MinLongitude = -180.0
internal const val MaxLongitude = 180.0
