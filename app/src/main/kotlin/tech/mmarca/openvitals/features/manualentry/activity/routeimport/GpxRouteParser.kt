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
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import tech.mmarca.openvitals.domain.model.BleCyclingCadenceSample
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.BleSpeedSample
import tech.mmarca.openvitals.domain.model.BleStepsCadenceSample

/**
 * The per-point series in a GPX's `<extensions>`: heart rate, cadence,
 * speed. Read off routed files too.
 */
internal class GpxSampleCollector(
    /** Decides which cadence record type: `cad` is just "cad". */
    private val isRunning: Boolean,
) {
    private val heartRates = mutableListOf<BleHeartRateSample>()
    private val speeds = mutableListOf<BleSpeedSample>()
    private val cadences = mutableListOf<Pair<Instant, Int>>()

    fun read(point: Element, time: Instant) {
        val heartRate = point.extensionValue("hr")
        if (heartRate != null && heartRate > 0.0) {
            heartRates += BleHeartRateSample(time = time, beatsPerMinute = heartRate.roundToLong())
        }
        val cadence = point.extensionValue("cad")
        if (cadence != null && cadence >= 0.0) cadences += time to cadence.roundToInt()

        val speed = point.extensionValue("speed")
        if (speed != null && speed >= 0.0) {
            speeds += BleSpeedSample(
                time = time,
                metersPerSecond = speed,
                isRunning = isRunning,
            )
        }
    }

    val buffer: BleRecordingSampleBuffer
        get() = BleRecordingSampleBuffer(
            heartRateSamples = heartRates,
            speedSamples = speeds,
            cyclingCadenceSamples = if (isRunning) {
                emptyList()
            } else {
                cadences.map { (time, rpm) -> BleCyclingCadenceSample(time = time, rpm = rpm.toLong()) }
            },
            stepsCadenceSamples = if (isRunning) {
                cadences.map { (time, rpm) ->
                    // Running cadence is per foot, as in TCX.
                    BleStepsCadenceSample(time = time, stepsPerMinute = rpm.toLong() * 2)
                }
            } else {
                emptyList()
            },
        )

    private companion object {
        /** Matched by local name: exporters disagree about the prefix. */
        fun Element.extensionValue(localName: String): Double? {
            val namespaced = getElementsByTagNameNS("*", localName)
            val plain = if (namespaced.length == 0) getElementsByTagName(localName) else namespaced
            return List(plain.length) { index -> plain.item(index) }
                .filterIsInstance<Element>()
                .firstOrNull()
                ?.textContent
                ?.trim()
                ?.toDoubleOrNull()
        }
    }
}

internal object GpxRouteParser {
    fun parse(gpxText: String, fileName: String? = null): RouteFileImport {
        val document = routeDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(gpxText)))
        val metadata = document.routeMetadata()
        val mutablePoints = mutableListOf<MutableRoutePoint>()
        // Every trackpoint with a time, whether or not it has a place.
        val timestamps = mutableListOf<Instant>()
        val samples = GpxSampleCollector(
            isRunning = !metadata.type.orEmpty().lowercase().contains("bik") &&
                !metadata.type.orEmpty().lowercase().contains("cycl"),
        )

        for (tag in PointTags) {
            for (element in document.elementsByLocalName(tag)) {
                val time = element.directChildText("time")?.trim()?.toInstantOrNull()
                mutablePoints += MutableRoutePoint(
                    latitude = element.getAttribute("lat").toDoubleOrNull(),
                    longitude = element.getAttribute("lon").toDoubleOrNull(),
                    elevationMeters = element.directChildText("ele")?.trim()?.toDoubleOrNull(),
                    time = time,
                )
                if (time != null) {
                    timestamps += time
                    samples.read(element, time)
                }
            }
        }

        val routePoints = mutablePoints.toRoutePoints()
        if (routePoints.size >= MinRoutePoints) {
            return buildRouteImport(
                fileName = fileName,
                points = routePoints,
                metadata = metadata,
            ).copy(bleSamples = samples.buffer)
        }

        // No route is not the same as no activity.
        if (timestamps.size >= MinRoutePoints) {
            return routelessImport(
                fileName = fileName,
                metadata = metadata,
                timestamps = timestamps,
                samples = samples,
            )
        }

        throw IllegalArgumentException(
            // No places and no times: nothing to import. Keeps a corrupt XML from arriving blank.
            "This GPX has nothing in it: no timestamped track points, with or " +
                "without locations.",
        )
    }

    /**
     * A GPX with no places, and an activity all the same. Real exporters
     * write indoor sessions as timestamped `<trkpt>`s with no `lat`/`lon`.
     * Distance stays 0 and calories are left for the form to estimate.
     */
    private fun routelessImport(
        fileName: String?,
        metadata: RouteFileMetadata,
        timestamps: List<Instant>,
        samples: GpxSampleCollector,
    ): RouteFileImport {
        val ordered = timestamps.sorted()
        val startTime = ordered.first()
        val last = ordered.last()
        val endTime = if (last.isAfter(startTime)) last else startTime.plusSeconds(1)

        return RouteFileImport(
            fileName = fileName,
            points = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = java.time.Duration.between(startTime, endTime).seconds,
            name = metadata.name,
            description = metadata.description,
            type = metadata.type,
            bleSamples = samples.buffer,
            originalPointCount = 0,
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
