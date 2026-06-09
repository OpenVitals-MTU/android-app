package tech.mmarca.openvitals.features.manualentry

import java.io.StringReader
import java.time.Duration
import java.time.Instant
import kotlin.math.min
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

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

private fun List<MutableRoutePoint>.withSyntheticTimes(
    startTime: Instant,
    endTime: Instant,
): List<MutableRoutePoint> {
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

private const val DefaultSyntheticRouteDurationSeconds = 30 * 60L
private const val SyntheticRoutePointSpacingSeconds = 10L
private val SyntheticRouteStartTime: Instant = Instant.EPOCH
private val WhitespaceRegex = Regex("\\s+")
