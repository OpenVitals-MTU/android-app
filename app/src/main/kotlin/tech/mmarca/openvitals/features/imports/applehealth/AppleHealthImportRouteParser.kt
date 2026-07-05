package tech.mmarca.openvitals.features.imports.applehealth

import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import kotlin.math.abs
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

internal object AppleHealthImportRouteParser {
    fun parse(path: String, input: InputStream): AppleWorkoutRouteFile? {
        val handler = AppleWorkoutRouteGpxHandler(path.normalizedAppleWorkoutRoutePath())
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
            setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
            setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        }
        factory.newSAXParser().parse(input, handler)
        return handler.routeFile()
    }
}

internal fun String.normalizedAppleWorkoutRoutePath(): String {
    val normalized = replace('\\', '/').trim().trimStart('/')
    val routeIndex = normalized.indexOf(AppleWorkoutRoutesDirectory)
    return if (routeIndex >= 0) {
        normalized.substring(routeIndex)
    } else {
        normalized.substringAfterLast('/')
    }.lowercase(Locale.US)
}

private class AppleWorkoutRouteGpxHandler(
    private val path: String,
) : DefaultHandler() {
    private val points = mutableListOf<MutableAppleWorkoutRoutePoint>()
    private val text = StringBuilder()
    private var currentPoint: MutableAppleWorkoutRoutePoint? = null
    private var currentElement: String? = null

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes,
    ) {
        val name = elementName(localName, qName)
        if (name in PointTags) {
            currentPoint = MutableAppleWorkoutRoutePoint(
                latitude = attributes.value("lat")?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 },
                longitude = attributes.value("lon")?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 },
            )
        }
        currentElement = name
        text.clear()
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (currentPoint != null && currentElement in PointChildTags) {
            text.append(ch, start, length)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        val name = elementName(localName, qName)
        val point = currentPoint
        if (point != null) {
            val value = text.toString().trim().toDoubleOrNull()
            when (name) {
                "ele" -> point.altitudeMeters = value
                "hAcc" -> point.horizontalAccuracyMeters = value?.takeIf { it > 0.0 }
                "vAcc" -> point.verticalAccuracyMeters = value?.takeIf { it > 0.0 }
            }
        }
        if (name in PointTags) {
            point?.takeIf { it.latitude != null && it.longitude != null }?.let(points::add)
            currentPoint = null
        }
        currentElement = null
        text.clear()
    }

    fun routeFile(): AppleWorkoutRouteFile? {
        val hasUsefulAltitude = points.any { point ->
            point.altitudeMeters?.let { abs(it) > PlaceholderAltitudeToleranceMeters } == true
        }
        val immutablePoints = points.mapNotNull { point ->
            val latitude = point.latitude ?: return@mapNotNull null
            val longitude = point.longitude ?: return@mapNotNull null
            AppleWorkoutRoutePoint(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = point.altitudeMeters?.takeIf { hasUsefulAltitude },
                horizontalAccuracyMeters = point.horizontalAccuracyMeters,
                verticalAccuracyMeters = point.verticalAccuracyMeters,
            )
        }
        return AppleWorkoutRouteFile(path = path, points = immutablePoints)
            .takeIf { it.points.size >= MinAppleWorkoutRoutePoints }
    }
}

private data class MutableAppleWorkoutRoutePoint(
    val latitude: Double?,
    val longitude: Double?,
    var altitudeMeters: Double? = null,
    var horizontalAccuracyMeters: Double? = null,
    var verticalAccuracyMeters: Double? = null,
)

private fun elementName(localName: String?, qName: String?): String =
    localName?.takeIf { it.isNotBlank() } ?: qName.orEmpty()

private fun Attributes.value(name: String): String? = getValue(name)?.takeIf { it.isNotBlank() }

private fun SAXParserFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
    runCatching { setFeature(feature, enabled) }
}

private const val AppleWorkoutRoutesDirectory = "workout-routes/"
private const val MinAppleWorkoutRoutePoints = 2
private const val PlaceholderAltitudeToleranceMeters = 0.1
private val PointTags = setOf("trkpt", "rtept")
private val PointChildTags = setOf("ele", "hAcc", "vAcc")
