package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import java.io.StringReader
import org.w3c.dom.Document
import org.xml.sax.InputSource

internal object GpxRouteParser {
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

private val PointTags = setOf("trkpt", "rtept")
