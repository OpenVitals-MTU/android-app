package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Non-day ranges load one point per day; the charts draw entry lists, so
 * each point is synthesised into an entry at local midnight with an empty
 * source. Cards never read these.
 */

private fun LocalDate.atDayStart(): Instant = atStartOfDay(ZoneId.systemDefault()).toInstant()

internal fun List<DailyBloodPressurePoint>.toBloodPressureEntries(): List<BloodPressureEntry> =
    map { point ->
        BloodPressureEntry(
            time = point.date.atDayStart(),
            systolicMmHg = point.systolic.roundToInt(),
            diastolicMmHg = point.diastolic.roundToInt(),
            source = "",
        )
    }

internal fun List<DailyVitalPoint>.toSpO2Entries(): List<SpO2Entry> =
    map { point -> SpO2Entry(time = point.date.atDayStart(), percent = point.value, source = "") }

internal fun List<DailyVitalPoint>.toRespiratoryRateEntries(): List<RespiratoryRateEntry> =
    map { point ->
        RespiratoryRateEntry(time = point.date.atDayStart(), breathsPerMinute = point.value, source = "")
    }

internal fun List<DailyVitalPoint>.toBodyTempEntries(): List<BodyTempEntry> =
    map { point ->
        BodyTempEntry(time = point.date.atDayStart(), temperatureCelsius = point.value, source = "")
    }

internal fun List<DailyVitalPoint>.toVo2MaxEntries(): List<Vo2MaxEntry> =
    map { point ->
        Vo2MaxEntry(time = point.date.atDayStart(), vo2MaxMlPerKgPerMin = point.value, source = "")
    }

internal fun List<DailyVitalPoint>.toBloodGlucoseEntries(): List<BloodGlucoseEntry> =
    map { point ->
        BloodGlucoseEntry(
            time = point.date.atDayStart(),
            millimolesPerLiter = point.value,
            specimenSource = 0,
            mealType = 0,
            relationToMeal = 0,
            source = "",
        )
    }

internal fun List<DailyVitalPoint>.toSkinTemperatureEntries(): List<SkinTemperatureEntry> =
    map { point ->
        val dayStart = point.date.atDayStart()
        SkinTemperatureEntry(
            startTime = dayStart,
            endTime = dayStart,
            baselineCelsius = null,
            averageDeltaCelsius = point.value,
            minDeltaCelsius = null,
            maxDeltaCelsius = null,
            measurementLocation = 0,
            source = "",
        )
    }

/**
 * Period mean with one vote per day. Not count-weighted: a night of
 * continuous monitoring must not outvote a week of spot checks.
 */
internal fun List<DailyVitalPoint>.dailyMeanOrNull(): Double? {
    if (isEmpty()) return null
    return sumOf { it.value } / size
}

internal fun List<DailyVitalPoint>.totalReadings(): Int = sumOf { it.count }
