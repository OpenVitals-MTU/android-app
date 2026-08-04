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
 * The per-point series a GPX carries in its `<extensions>`: heart rate,
 * cadence, speed — the Garmin `gpxtpx:TrackPointExtension` every exporter
 * writes, and the only thing an indoor GPX has to say besides the time.
 *
 * Read off the ROUTED files too, which is a fix in its own right: a GPX with a
 * track and a heart-rate extension used to import as a bare line on a map, its
 * heart rate thrown away at the parser.
 */
internal class GpxSampleCollector(
    /**
     * Decides which Health Connect record the cadence belongs in. Pedalling
     * cadence and step cadence are different record types, and `cad` is just
     * "cad".
     */
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
                    // Running cadence is written per FOOT, as in TCX: 85 means
                    // 170 steps a minute.
                    BleStepsCadenceSample(time = time, stepsPerMinute = rpm.toLong() * 2)
                }
            } else {
                emptyList()
            },
        )

    private companion object {
        /**
         * Matched by LOCAL name, so the namespace prefix (`gpxtpx:`, `ns3:`,
         * none at all) does not matter — exporters disagree about it and none of
         * them are wrong.
         */
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
        // Every trackpoint that carries a TIME, whether or not it carries a
        // place. See [routelessImport]: this is the indoor session.
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

        // No route. That is not the same as no activity — see below.
        if (timestamps.size >= MinRoutePoints) {
            return routelessImport(
                fileName = fileName,
                metadata = metadata,
                timestamps = timestamps,
                samples = samples,
            )
        }

        throw IllegalArgumentException(
            // Now genuinely empty: no places AND no times. A file with neither
            // has nothing in it to import, and this is the guard that keeps a
            // corrupt XML (or an HTML error page saved as .gpx) from arriving as
            // a blank activity.
            "This GPX has nothing in it: no timestamped track points, with or " +
                "without locations.",
        )
    }

    /**
     * A GPX with no places, and an activity all the same.
     *
     * The app used to refuse this outright — "GPX route must contain at least 2
     * timestamped location points" — on the theory that a GPX is a list of
     * PLACES and an indoor session therefore cannot be written as one. That was
     * wrong, and two real HealthFit exports say so: a strength session of 1931
     * `<trkpt>`, and an indoor run of 1422, every one of them carrying a
     * `<time>` and NO `lat`/`lon` at all. The GPX schema does require those
     * attributes; real exporters omit them anyway, and the file that results is
     * not corrupt — it is a timestamped series with the positions left out,
     * which is exactly what an indoor activity is.
     *
     * So what a routeless GPX gives up is DISTANCE and CALORIES, not the
     * session: the timestamps give the start, the end and the duration, and the
     * extensions give the heart rate. Distance stays 0 (for a strength session
     * there is nothing to be wrong about, and for a treadmill the file simply
     * did not say), and calories are left for the entry form to estimate —
     * which it does, from duration, precisely because nothing here was measured
     * to contradict it.
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
