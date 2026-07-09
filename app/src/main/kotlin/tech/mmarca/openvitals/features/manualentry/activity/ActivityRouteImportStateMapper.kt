package tech.mmarca.openvitals.features.manualentry.activity

import java.time.Clock
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.RouteFileImport

internal fun ActivityEntryUiState.withRouteImport(
    routeImport: RouteFileImport,
    unitSystem: UnitSystem,
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
    val calorieEstimate = activityCalorieEstimate(
        activityType = selectedActivityType,
        distanceMeters = routeImport.distanceMeters,
        durationMinutesText = routeDurationMinutes,
    ).takeIf {
        activeCaloriesText.isBlank() && totalCaloriesText.isBlank()
    }
    val importedActiveCaloriesText = routeImport.activeCaloriesKcal
        ?.takeIf { it > 0.0 }
        ?.toInputText(maxFractionDigits = 1)
    val importedTotalCaloriesText = routeImport.totalCaloriesKcal
        ?.takeIf { it > 0.0 }
        ?.toInputText(maxFractionDigits = 1)

    return copy(
        mode = ActivityEntryMode.ROUTE_IMPORT,
        selectedActivityType = selectedActivityType,
        titleText = titleText.ifBlank {
            routeImport.name
                ?: routeImport.fileName?.substringBeforeLast('.', missingDelimiterValue = routeImport.fileName)
                ?: ""
        },
        notesText = notesText.ifBlank { routeImport.description.orEmpty() },
        distanceText = distanceText.ifBlank { routeDistanceInputText(routeImport, unitSystem) },
        elevationText = elevationText.ifBlank { routeElevationInputText(routeImport, unitSystem) },
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
