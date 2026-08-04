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
 * Training Center XML — what Strava and Garmin export an INDOOR activity as.
 *
 * The app could read GPX, KML and FIT, and a GPX cannot carry an indoor session
 * at all: it is a list of places, so a treadmill run has nothing to put in it,
 * and a routeless GPX is correctly refused for having no start, no duration and
 * no distance. TCX is the format that solves exactly that problem — its `Lap`
 * carries `TotalTimeSeconds`, `DistanceMeters` and `Calories`, and its
 * `Trackpoint` carries heart rate, cadence and speed with the `Position`
 * OPTIONAL. So a treadmill run is a first-class TCX document, and reporting one
 * as broken was the app's limitation, not the file's.
 *
 * The route is therefore built from whichever trackpoints HAVE a position, and
 * an activity with none is still a complete activity. This is the same shape as
 * the FIT parser, for the same reason.
 */
internal object TcxRouteParser {

    /**
     * Roughly: is this a TCX? Matched on the ROOT element rather than the
     * extension, because the dispatcher sniffs content — a `.tcx` renamed to
     * `.gpx` is still a TCX, and would otherwise die in the GPX parser with a
     * message about location points.
     */
    fun looksLikeTcx(text: String): Boolean = text.contains("TrainingCenterDatabase")

    fun parse(tcxText: String, fileName: String? = null): RouteFileImport {
        val document = routeDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(tcxText)))

        // `Activity` is a recorded session; `Course` is a planned route. Both
        // hold Laps and Tracks, and either may be what the user picked.
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
        // TCX names the sport on the Activity and nowhere else, and its
        // vocabulary is three words wide: Running, Biking, Other. It cannot say
        // "treadmill" — so an indoor run imports as a run, which is what the
        // file actually claims. Better a true statement than a clever guess: a
        // ride with no GPS is not necessarily a trainer ride, it may be a ride
        // whose GPS failed.
        val isCycling = sport.orEmpty().lowercase().contains("bik")

        for (trackpoint in activity.descendantsByLocalName("Trackpoint")) {
            val time = trackpoint.directChildText("Time")?.trim()?.toInstantOrNull()

            // Position is OPTIONAL, and its absence is the whole point of this
            // parser.
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

            // `<HeartRateBpm><Value>128</Value></HeartRateBpm>` — the value is a
            // child, not the text of the element.
            val heartRate = trackpoint.valueOf("HeartRateBpm")
            if (heartRate != null && heartRate > 0.0) {
                heartRates += BleHeartRateSample(
                    time = time,
                    beatsPerMinute = heartRate.roundToLong(),
                )
            }
            val cadence = trackpoint.directChildText("Cadence")?.trim()?.toIntOrNull()
            if (cadence != null && cadence >= 0) cadences += time to cadence

            // Speed and running cadence live in the vendor extension namespace
            // (`ns3:TPX`), which is where every exporter in practice puts them.
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
            // Which record the cadence belongs in is decided by the sport,
            // exactly as the FIT parser decides it: pedalling cadence and step
            // cadence are different Health Connect record types, and `Cadence`
            // is just "cadence".
            cyclingCadenceSamples = if (isCycling) {
                cadences.map { (time, rpm) -> BleCyclingCadenceSample(time = time, rpm = rpm.toLong()) }
            } else {
                emptyList()
            },
            stepsCadenceSamples = if (isCycling) {
                emptyList()
            } else {
                cadences.map { (time, rpm) ->
                    // TCX writes RUNNING cadence as one foot: 85 means 170 steps
                    // a minute, and every watch that reads it doubles it back.
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
            // TCX `Calories` is the session TOTAL, and TCX has no active-calorie
            // field — so active is left unknown rather than invented. Filling it
            // with an estimate is exactly what made every routeless FIT file
            // unsavable.
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

/**
 * The session totals, summed across the laps — a TCX writes one `Lap` per lap
 * and the activity's distance/duration/calories are their sums, not any one of
 * them.
 */
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
        // "Other" says nothing, and saying nothing lets the file NAME speak —
        // which for a TCX is usually the only thing that can.
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

/**
 * A value inside the vendor `Extensions` block (`ns3:TPX` → `Speed`, `Watts`,
 * `RunCadence`), matched by local name so the namespace prefix does not matter.
 */
private fun Element.extensionValue(localName: String): Double? =
    firstDescendantByLocalName("Extensions")
        ?.firstDescendantByLocalName(localName)
        ?.textContent
        ?.trim()
        ?.toDoubleOrNull()
