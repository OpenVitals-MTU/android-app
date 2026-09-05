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
import org.w3c.dom.Element
import org.xml.sax.InputSource
import tech.mmarca.openvitals.domain.model.BleCyclingCadenceSample
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.BleSpeedSample
import tech.mmarca.openvitals.domain.model.BleStepsCadenceSample

/**
 * Training Center XML, what Strava and Garmin export an indoor activity as.
 * `Position` is optional, so a treadmill run is a complete TCX document.
 * The route is built from the trackpoints that have one.
 */
internal object TcxRouteParser {

    /** Matched on the root element, not the extension: the dispatcher sniffs content. */
    fun looksLikeTcx(text: String): Boolean = text.contains("TrainingCenterDatabase")

    fun parse(tcxText: String, fileName: String? = null): RouteFileImport {
        val document = routeDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(tcxText)))

        // `Activity` is a session; `Course` is a planned route.
        val activities = document.elementsByLocalName("Activity") +
            document.elementsByLocalName("Course")
        require(activities.isNotEmpty()) {
            "TCX file contains no activity or course."
        }
        val activity = activities.first()

        val points = mutableListOf<MutableRoutePoint>()
        val heartRates = mutableListOf<BleHeartRateSample>()
        val cadences = mutableListOf<Pair<Instant, Int>>()
        val speeds = mutableListOf<BleSpeedSample>()

        val sport = activity.getAttribute("Sport").takeIf { it.isNotBlank() }
        // TCX's sport vocabulary is Running, Biking, Other. It cannot say treadmill.
        val isCycling = sport.orEmpty().lowercase().contains("bik")

        for (trackpoint in activity.descendantsByLocalName("Trackpoint")) {
            val time = trackpoint.directChildText("Time")?.trim()?.toInstantOrNull()

            // Position is optional.
            val position = trackpoint.firstDescendantByLocalName("Position")
            if (position != null) {
                points += MutableRoutePoint(
                    latitude = position.directChildText("LatitudeDegrees")?.trim()?.toDoubleOrNull(),
                    longitude = position.directChildText("LongitudeDegrees")?.trim()?.toDoubleOrNull(),
                    elevationMeters = trackpoint.directChildText("AltitudeMeters")?.trim()?.toDoubleOrNull(),
                    time = time,
                )
            }

            if (time == null) continue

            // The value is a child element, not text.
            val heartRate = trackpoint.valueOf("HeartRateBpm")
            if (heartRate != null && heartRate > 0.0) {
                heartRates += BleHeartRateSample(
                    time = time,
                    beatsPerMinute = heartRate.roundToLong(),
                )
            }
            val cadence = trackpoint.directChildText("Cadence")?.trim()?.toIntOrNull()
            if (cadence != null && cadence >= 0) cadences += time to cadence

            // Speed and running cadence live in the `ns3:TPX` extension.
            val speed = trackpoint.extensionValue("Speed")
            if (speed != null && speed >= 0.0) {
                speeds += BleSpeedSample(
                    time = time,
                    metersPerSecond = speed,
                    isRunning = !isCycling,
                )
            }
            if (cadence == null) {
                val runCadence = trackpoint.extensionValue("RunCadence")
                if (runCadence != null && runCadence >= 0.0) {
                    cadences += time to runCadence.roundToInt()
                }
            }
        }

        val summary = activity.summarize()
        val routePoints = points.toRoutePoints()

        val startTime = summary.startTime
            ?: routePoints.firstOrNull()?.time
            ?: heartRates.firstOrNull()?.time
            ?: throw IllegalArgumentException("TCX file contains no timestamped activity data.")
        val duration = summary.durationSeconds
        val candidateEnd = if (duration != null && duration > 0L) {
            startTime.plusSeconds(duration)
        } else {
            routePoints.lastOrNull()?.time
        }
        val endTime = candidateEnd?.takeIf { startTime.isBefore(it) }
            ?: startTime.plusSeconds(1)

        val bleSamples = BleRecordingSampleBuffer(
            heartRateSamples = heartRates,
            speedSamples = speeds,
            // The sport decides which cadence record type, as in the FIT parser.
            cyclingCadenceSamples = if (isCycling) {
                cadences.map { (time, rpm) -> BleCyclingCadenceSample(time = time, rpm = rpm.toLong()) }
            } else {
                emptyList()
            },
            stepsCadenceSamples = if (isCycling) {
                emptyList()
            } else {
                cadences.map { (time, rpm) ->
                    // TCX writes running cadence as one foot: 85 means 170 steps.
                    BleStepsCadenceSample(time = time, stepsPerMinute = rpm.toLong() * 2)
                }
            },
        )

        val metadata = RouteFileMetadata(
            name = null,
            description = null,
            type = sport.tcxSportName(),
        )

        if (routePoints.size >= MinRoutePoints) {
            return buildRouteImport(
                fileName = fileName,
                points = routePoints,
                metadata = metadata,
            ).copy(
                distanceMeters = summary.distanceMeters ?: routeDistanceMeters(routePoints),
                elevationGainedMeters = routeElevationGainMeters(routePoints),
                totalCaloriesKcal = summary.caloriesKcal,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = duration,
                bleSamples = bleSamples,
                originalPointCount = routePoints.size,
            )
        }

        // The indoor case. No route, and a complete activity all the same.
        return RouteFileImport(
            fileName = fileName,
            points = emptyList(),
            distanceMeters = summary.distanceMeters ?: 0.0,
            elevationGainedMeters = 0.0,
            // TCX has no active-calorie field, so active stays unknown.
            totalCaloriesKcal = summary.caloriesKcal,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = duration,
            name = null,
            description = null,
            type = sport.tcxSportName(),
            bleSamples = bleSamples,
            originalPointCount = 0,
        )
    }
}

private data class TcxSummary(
    val startTime: Instant?,
    val durationSeconds: Long?,
    val distanceMeters: Double?,
    val caloriesKcal: Double?,
)

/** The session totals, summed across the laps. */
private fun Element.summarize(): TcxSummary {
    var startTime = directChildText("Id")?.trim()?.toInstantOrNull()
    var seconds = 0.0
    var meters = 0.0
    var calories = 0.0
    var sawLap = false

    for (lap in descendantsByLocalName("Lap")) {
        sawLap = true
        if (startTime == null) {
            startTime = lap.getAttribute("StartTime").trim().toInstantOrNull()
        }
        seconds += lap.directChildText("TotalTimeSeconds")?.toDoubleOrNull() ?: 0.0
        meters += lap.directChildText("DistanceMeters")?.toDoubleOrNull() ?: 0.0
        calories += lap.directChildText("Calories")?.toDoubleOrNull() ?: 0.0
    }

    return TcxSummary(
        startTime = startTime,
        durationSeconds = seconds.roundToLong().takeIf { sawLap && seconds > 0.0 },
        distanceMeters = meters.takeIf { sawLap && meters > 0.0 },
        caloriesKcal = calories.takeIf { sawLap && calories > 0.0 },
    )
}

/** TCX's whole sport vocabulary, in the words the type inference reads. */
private fun String?.tcxSportName(): String? {
    val value = orEmpty().lowercase()
    return when {
        value.contains("bik") || value.contains("cycl") -> "cycling"
        value.contains("run") -> "running"
        value.contains("walk") -> "walking"
        // "Other" says nothing, and lets the file name speak.
        else -> null
    }
}

private fun Element.descendantsByLocalName(localName: String): List<Element> {
    val namespaced = getElementsByTagNameNS("*", localName)
    val plain = if (namespaced.length == 0) getElementsByTagName(localName) else namespaced
    return List(plain.length) { index -> plain.item(index) }
        .filterIsInstance<Element>()
}

private fun Element.firstDescendantByLocalName(localName: String): Element? =
    descendantsByLocalName(localName).firstOrNull()

/** `<HeartRateBpm><Value>128</Value></HeartRateBpm>`. */
private fun Element.valueOf(localName: String): Double? =
    firstDescendantByLocalName(localName)
        ?.directChildText("Value")
        ?.trim()
        ?.toDoubleOrNull()

/** A value inside the vendor `Extensions` block, matched by local name. */
private fun Element.extensionValue(localName: String): Double? =
    firstDescendantByLocalName("Extensions")
        ?.firstDescendantByLocalName(localName)
        ?.textContent
        ?.trim()
        ?.toDoubleOrNull()
