package tech.mmarca.openvitals.core.presentation

import tech.mmarca.openvitals.domain.preferences.UnitSystem
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class UnitFormatter(
    private val unitSystemProvider: () -> UnitSystem,
    private val localeProvider: () -> Locale = { Locale.getDefault() },
) {
    fun unitSystem(): UnitSystem = unitSystemProvider()

    fun count(value: Long): String = integerFormat().format(value)

    fun count(value: Int): String = integerFormat().format(value)

    fun distance(meters: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> metricDistance(meters)
            UnitSystem.IMPERIAL -> imperialDistance(meters)
        }

    fun elevation(meters: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> metricDistance(meters)
            UnitSystem.IMPERIAL -> DisplayValue(count(metersToFeet(meters).roundToInt()), "ft")
        }

    fun weight(kg: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(kg, 1), "kg")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(kgToPounds(kg), 1), "lb")
        }

    fun height(centimeters: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(centimeters, 0), "cm")
            UnitSystem.IMPERIAL -> {
                val totalInches = (centimeters / 2.54).roundToInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                DisplayValue("$feet' $inches\"", "")
            }
        }

    fun bodyMass(kg: Double, decimals: Int = 1): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(kg, decimals), "kg")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(kgToPounds(kg), decimals), "lb")
        }

    fun hydration(liters: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(liters, 2), "L")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(litersToFluidOunces(liters), 0), "fl oz")
        }

    fun energy(kcal: Double): DisplayValue = DisplayValue(count(kcal.roundToInt()), "kcal")

    fun temperature(celsius: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(celsius, 1), "deg C")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(celsiusToFahrenheit(celsius), 1), "deg F")
        }

    fun temperatureDelta(celsius: Double): DisplayValue {
        val value = when (unitSystem()) {
            UnitSystem.METRIC -> celsius
            UnitSystem.IMPERIAL -> celsius * 9.0 / 5.0
        }
        val prefix = if (value > 0.0) "+" else ""
        val unit = when (unitSystem()) {
            UnitSystem.METRIC -> "deg C"
            UnitSystem.IMPERIAL -> "deg F"
        }
        return DisplayValue("$prefix${decimal(value, 1)}", unit)
    }

    fun bloodGlucose(millimolesPerLiter: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(millimolesPerLiter, 1), "mmol/L")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(millimolesPerLiter * 18.0, 0), "mg/dL")
        }

    fun percent(value: Double, decimals: Int = 1): DisplayValue =
        DisplayValue(decimal(value, decimals), "%")

    fun heartRate(bpm: Long): DisplayValue = DisplayValue(bpm.toString(), "bpm")

    fun hrv(milliseconds: Double): DisplayValue = DisplayValue(decimal(milliseconds, 1), "ms")

    fun bloodPressure(systolic: Int, diastolic: Int): DisplayValue =
        DisplayValue("$systolic/$diastolic", "mmHg")

    fun respiratoryRate(value: Double): DisplayValue = DisplayValue(decimal(value, 1), "br/min")

    fun vo2Max(value: Double): DisplayValue = DisplayValue(decimal(value, 1), "mL/kg/min")

    fun duration(durationMs: Long): String {
        val hours = durationMs / 3_600_000
        val minutes = (durationMs % 3_600_000) / 60_000
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    fun averageSpeed(distanceMeters: Double, durationMs: Long): DisplayValue {
        val hours = durationMs.coerceAtLeast(0L).toDouble() / 3_600_000.0
        val metersPerHour = if (distanceMeters > 0.0 && hours > 0.0) {
            distanceMeters / hours
        } else {
            0.0
        }
        return when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(metersPerHour / 1000.0, 1), "km/h")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(metersPerHour / 1609.344, 1), "mph")
        }
    }

    fun speed(metersPerSecond: Double): DisplayValue =
        when (unitSystem()) {
            UnitSystem.METRIC -> DisplayValue(decimal(metersPerSecond * 3.6, 1), "km/h")
            UnitSystem.IMPERIAL -> DisplayValue(decimal(metersPerSecond * 2.2369362921, 1), "mph")
        }

    fun power(watts: Double): DisplayValue = DisplayValue(decimal(watts, 0), "W")

    fun cadence(value: Double): DisplayValue = DisplayValue(decimal(value, 1), "rpm")

    fun averagePace(distanceMeters: Double, durationMs: Long): DisplayValue? {
        if (distanceMeters <= 0.0 || durationMs <= 0L) return null
        val distanceUnitMeters = when (unitSystem()) {
            UnitSystem.METRIC -> 1000.0
            UnitSystem.IMPERIAL -> 1609.344
        }
        val distanceUnits = distanceMeters / distanceUnitMeters
        if (distanceUnits <= 0.0 || !distanceUnits.isFinite()) return null

        val secondsPerUnit = ((durationMs / 1_000.0) / distanceUnits)
            .roundToInt()
            .coerceAtLeast(0)
        val minutes = secondsPerUnit / 60
        val seconds = secondsPerUnit % 60
        val unit = when (unitSystem()) {
            UnitSystem.METRIC -> "min/km"
            UnitSystem.IMPERIAL -> "min/mi"
        }
        return DisplayValue(String.format(Locale.US, "%d:%02d", minutes, seconds), unit)
    }

    fun minutes(minutes: Long): DisplayValue = DisplayValue(count(minutes), "min")

    fun decimal(value: Double, decimals: Int): String =
        NumberFormat.getNumberInstance(localeProvider()).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    private fun metricDistance(meters: Double): DisplayValue =
        if (meters >= 1000.0) {
            DisplayValue(decimal(meters / 1000.0, 1), "km")
        } else {
            DisplayValue(count(meters.roundToInt()), "m")
        }

    private fun imperialDistance(meters: Double): DisplayValue {
        val miles = metersToMiles(meters)
        return if (miles >= 0.1) {
            DisplayValue(decimal(miles, 1), "mi")
        } else {
            DisplayValue(count(metersToFeet(meters).roundToInt()), "ft")
        }
    }

    private fun integerFormat(): NumberFormat =
        NumberFormat.getIntegerInstance(localeProvider()).apply {
            maximumFractionDigits = 0
        }

    private fun kgToPounds(kg: Double): Double = kg * 2.2046226218

    private fun metersToFeet(meters: Double): Double = meters * 3.280839895

    private fun metersToMiles(meters: Double): Double = meters / 1609.344

    private fun litersToFluidOunces(liters: Double): Double = liters * 33.8140227018

    private fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0
}
