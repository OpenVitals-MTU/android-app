package tech.mmarca.openvitals.features.manualentry.activity

import java.time.Clock
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport

internal fun ActivityEntryUiState.withRouteImport(
    routeImport: RouteFileImport,
    units: ActivityEntryUnits,
    clock: Clock,
): ActivityEntryUiState {
    val start = routeImport.startTime.atZone(clock.zone)
    val selectedActivityType = inferActivityType(routeImport, selectedActivityType)
    val routeDurationMinutes = if (routeImport.hasImportedTimeRange) {
        val routeDurationSeconds = Duration.between(routeImport.startTime, routeImport.endTime).seconds.coerceAtLeast(1)
        val durationSecondsForDisplay = if (routeImport.points.isNotEmpty() && routeImport.hasRecordedTimestamps) {
            routeDurationSeconds + 1
        } else {
            routeDurationSeconds
        }
        ceil(durationSecondsForDisplay.toDouble() / 60.0)
            .toLong()
            .coerceIn(1, MaxActivityDurationMinutes)
            .toString()
    } else if (routeImport.durationSeconds != null) {
        ceil(routeImport.durationSeconds.coerceAtLeast(1).toDouble() / 60.0)
            .toLong()
            .coerceIn(1, MaxActivityDurationMinutes)
            .toString()
    } else {
        durationMinutesText.ifBlank { "30" }
    }
    val importedActiveCaloriesText = routeImport.activeCaloriesKcal
        ?.takeIf { it > 0.0 }
        ?.toInputText(maxFractionDigits = 1)
    val importedTotalCaloriesText = routeImport.totalCaloriesKcal
        ?.takeIf { it > 0.0 }
        ?.toInputText(maxFractionDigits = 1)
    // The estimate fills in for a file that measured NO calories at all. It must
    // never stand beside a number the file did measure.
    //
    // A FIT session records `total_calories` and has no active-calorie field, so
    // the other calorie field came back null — and the estimate then filled it,
    // from METs and distance, with a number that had nothing to do with the
    // file's own measurement. An indoor run arrived with an invented calorie
    // figure standing beside its measured one, and the write was refused ("total
    // cannot be lower than active"): a real activity, a real measurement, and a
    // guess that contradicted it. Every FIT file with no GPS failed this way,
    // which is every treadmill run and every trainer ride.
    //
    // So: estimate both, or estimate neither. A measurement does not get a
    // guess for a neighbour.
    val fileMeasuredCalories =
        importedActiveCaloriesText != null || importedTotalCaloriesText != null
    val calorieEstimate = activityCalorieEstimate(
        activityType = selectedActivityType,
        distanceMeters = routeImport.distanceMeters,
        durationMinutesText = routeDurationMinutes,
    ).takeIf {
        activeCaloriesText.isBlank() && totalCaloriesText.isBlank() && !fileMeasuredCalories
    }

    return copy(
        mode = ActivityEntryMode.ROUTE_IMPORT,
        selectedActivityType = selectedActivityType,
        titleText = titleText.ifBlank {
            routeImport.name
                ?: routeImport.fileName?.substringBeforeLast('.', missingDelimiterValue = routeImport.fileName)
                ?: ""
        },
        notesText = notesText.ifBlank { routeImport.description.orEmpty() },
        distanceText = distanceText.ifBlank { routeDistanceInputText(routeImport, units.distance) },
        elevationText = elevationText.ifBlank { routeElevationInputText(routeImport, units.elevation) },
        activeCaloriesText = activeCaloriesText.ifBlank {
            importedActiveCaloriesText ?: calorieEstimate?.activeCaloriesText.orEmpty()
        },
        totalCaloriesText = totalCaloriesText.ifBlank {
            importedTotalCaloriesText ?: calorieEstimate?.totalCaloriesText.orEmpty()
        },
        importedRoute = routeImport,
        recordedPauseIntervals = emptyList(),
        recordedLaps = emptyList(),
        recordedMarkers = emptyList(),
        isRecordingDraft = false,
        startDateText = if (routeImport.hasImportedTimeRange) {
            DateTimeFormatter.ISO_LOCAL_DATE.format(start)
        } else {
            startDateText
        },
        startTimeText = if (routeImport.hasImportedTimeRange) {
            TimeFormatter.format(start.toLocalTime())
        } else {
            startTimeText
        },
        durationMinutesText = routeDurationMinutes,
        isImportingRoute = false,
        entryError = null,
        detailError = null,
        validationErrors = emptySet(),
    )
}
