package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Mass
import java.util.Locale

internal fun Double.toMeters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "m", "meter", "meters" -> this
        "km", "kilometer", "kilometers" -> this * 1_000.0
        "cm", "centimeter", "centimeters" -> this / 100.0
        "mm", "millimeter", "millimeters" -> this / 1_000.0
        "mi", "mile", "miles" -> this * 1_609.344
        "yd", "yard", "yards" -> this * 0.9144
        "ft", "foot", "feet" -> this * 0.3048
        "in", "inch", "inches" -> this * 0.0254
        else -> null
    }

internal fun Double.toKilograms(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kg", "kilogram", "kilograms" -> this
        "g", "gram", "grams" -> this / 1_000.0
        "lb", "lbs", "pound", "pounds" -> this * 0.45359237
        "oz", "ounce", "ounces" -> this * 0.028349523125
        "st", "stone", "stones" -> this * 6.35029318
        else -> null
    }

internal fun Double.toKilocalories(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "kcal", "cal", "calorie", "calories", "calories/hour", "calories/hr" -> this
        "kj", "kilojoule", "kilojoules" -> this / 4.184
        "j", "joule", "joules" -> this / 4_184.0
        else -> null
    }

internal fun Double.toMilliliters(unit: String?): Double? =
    when (unit?.lowercase(Locale.US)) {
        "ml", "milliliter", "milliliters" -> this
        "l", "liter", "liters" -> this * 1_000.0
        "fl_oz_us", "floz", "fl oz", "oz" -> this * 29.5735295625
        else -> null
    }

internal fun Double.toPercentage(unit: String?): Double? =
    when (unit) {
        "%" -> if (this <= 1.0) this * 100.0 else this
        else -> null
    }

internal fun Double.toCelsius(unit: String?): Double? =
    when (unit) {
        "degC", "\u00B0C" -> this
        "degF", "\u00B0F" -> (this - 32.0) * 5.0 / 9.0
        else -> null
    }

internal fun Double.toMass(unit: String?): Mass? =
    when (unit?.lowercase(Locale.US)) {
        "kg", "kilogram", "kilograms" -> Mass.kilograms(this)
        "g", "gram", "grams" -> Mass.grams(this)
        "mg", "milligram", "milligrams" -> Mass.milligrams(this)
        "mcg", "ug", "\u00B5g", "microgram", "micrograms" -> Mass.micrograms(this)
        "oz", "ounce", "ounces" -> Mass.ounces(this)
        "lb", "lbs", "pound", "pounds" -> Mass.pounds(this)
        else -> null
    }

internal fun Double.toBloodGlucose(unit: String?): BloodGlucose? =
    when (unit?.lowercase(Locale.US)) {
        "mg/dl", "mg/dL".lowercase(Locale.US) -> BloodGlucose.milligramsPerDeciliter(this)
        "mmol/l", "mmol/L".lowercase(Locale.US) -> BloodGlucose.millimolesPerLiter(this)
        else -> null
    }
