package tech.mmarca.openvitals.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Non-day ranges load one aggregated point per local day instead of raw
 * entries. The overview charts, though, draw entry lists — so each point is
 * synthesised back into one entry stamped at its day's local midnight. The
 * charts' own per-day averaging then passes these through unchanged.
 *
 * Synthetic entries carry an empty [source]: a day's aggregate has no single
 * writer. Cards never read these — their latest value and source come from the
 * true window-latest reads.
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

/** Count-weighted period mean: `Σ(value·count) / Σ(count)`, null with no readings. */
internal fun List<DailyVitalPoint>.weightedMeanOrNull(): Double? {
    val readings = sumOf { it.count }
    if (readings == 0) return null
    return sumOf { it.value * it.count } / readings
}

internal fun List<DailyVitalPoint>.totalReadings(): Int = sumOf { it.count }
