package tech.mmarca.openvitals.features.manualentry.body

import androidx.compose.runtime.Immutable
import java.util.Locale
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.accepts
import tech.mmarca.openvitals.domain.preferences.UnitSystem

private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54

/** The values the body composition form offers, in the order a body analysis scale shows them. */
val BodyCompositionTypes: List<BodyMeasurementType> = listOf(
    BodyMeasurementType.WEIGHT,
    BodyMeasurementType.BODY_FAT,
    BodyMeasurementType.LEAN_MASS,
    BodyMeasurementType.BODY_WATER_MASS,
    BodyMeasurementType.BONE_MASS,
    BodyMeasurementType.BMR,
)

/** How body water is typed in. Most scales show a share of body weight; Health Connect keeps a mass. */
enum class BodyWaterInput {
    PERCENT,
    MASS,
}

/** The unit systems the form reads and shows values in. Every mass follows the weight setting. */
@Immutable
data class BodyEntryUnits(
    val mass: UnitSystem = UnitSystem.METRIC,
    val height: UnitSystem = UnitSystem.METRIC,
) {
    fun systemFor(type: BodyMeasurementType): UnitSystem =
        if (type == BodyMeasurementType.HEIGHT) height else mass
}

/** What a filled form saves, in the form's order, or why it cannot. */
internal sealed interface BodyEntryValues {
    data class Valid(val values: Map<BodyMeasurementType, Double>) : BodyEntryValues

    data class Invalid(
        val error: BodyMeasurementEntryError,
        val types: Set<BodyMeasurementType>,
    ) : BodyEntryValues
}

/**
 * The canonical values of the filled fields among [types]. Blank fields are skipped. Body water
 * typed as a percentage becomes a mass of the weight typed beside it, never of an older weight.
 */
internal fun bodyEntryValues(
    types: List<BodyMeasurementType>,
    inputs: Map<BodyMeasurementType, String>,
    bodyWaterInput: BodyWaterInput,
    units: BodyEntryUnits,
): BodyEntryValues {
    val filled = types.filter { inputs[it].orEmpty().isNotBlank() }
    if (filled.isEmpty()) {
        return BodyEntryValues.Invalid(BodyMeasurementEntryError.NOTHING_TO_SAVE, emptySet())
    }
    val waterPercent = bodyWaterInput == BodyWaterInput.PERCENT && BodyMeasurementType.BODY_WATER_MASS in filled
    val values = linkedMapOf<BodyMeasurementType, Double>()
    val invalid = mutableSetOf<BodyMeasurementType>()
    filled.forEach { type ->
        val input = inputs.getValue(type)
        val value = if (type == BodyMeasurementType.BODY_WATER_MASS && waterPercent) {
            input.toDecimalOrNull()?.takeIf { it > 0.0 && it <= 100.0 }
        } else {
            canonicalBodyMeasurementValue(input, type, units.systemFor(type))?.takeIf(type::accepts)
        }
        if (value == null) invalid += type else values[type] = value
    }
    if (invalid.isNotEmpty()) {
        return BodyEntryValues.Invalid(BodyMeasurementEntryError.INVALID_VALUE, invalid)
    }
    if (waterPercent) {
        val weightKg = values[BodyMeasurementType.WEIGHT]
            ?: return BodyEntryValues.Invalid(
                BodyMeasurementEntryError.BODY_WATER_NEEDS_WEIGHT,
                setOf(BodyMeasurementType.WEIGHT),
            )
        values[BodyMeasurementType.BODY_WATER_MASS] = weightKg * values.getValue(BodyMeasurementType.BODY_WATER_MASS) / 100.0
    }
    return BodyEntryValues.Valid(values)
}

internal fun canonicalBodyMeasurementValue(
    input: String,
    type: BodyMeasurementType,
    unitSystem: UnitSystem,
): Double? {
    val value = input.toDecimalOrNull() ?: return null
    return when (type) {
        BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) value * CentimetersPerInch else value
        BodyMeasurementType.BODY_FAT,
        BodyMeasurementType.BMR,
        -> value
        BodyMeasurementType.WEIGHT,
        BodyMeasurementType.LEAN_MASS,
        BodyMeasurementType.BODY_WATER_MASS,
        BodyMeasurementType.BONE_MASS,
        -> if (unitSystem == UnitSystem.IMPERIAL) value / PoundsPerKilogram else value
    }
}

/** A stored value as the form shows it for editing. */
internal fun Double.toBodyMeasurementInput(type: BodyMeasurementType, unitSystem: UnitSystem): String {
    val displayValue = when (type) {
        BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) this / CentimetersPerInch else this
        BodyMeasurementType.BODY_FAT,
        BodyMeasurementType.BMR,
        -> this
        BodyMeasurementType.WEIGHT,
        BodyMeasurementType.LEAN_MASS,
        BodyMeasurementType.BODY_WATER_MASS,
        BodyMeasurementType.BONE_MASS,
        -> if (unitSystem == UnitSystem.IMPERIAL) this * PoundsPerKilogram else this
    }
    return "%.2f"
        .format(Locale.US, displayValue)
        .trimEnd('0')
        .trimEnd('.')
}

internal fun BodyMeasurementType.inputUnitLabel(unitSystem: UnitSystem): String = when (this) {
    BodyMeasurementType.HEIGHT -> if (unitSystem == UnitSystem.IMPERIAL) "in" else "cm"
    BodyMeasurementType.BODY_FAT -> "%"
    BodyMeasurementType.BMR -> "kcal"
    BodyMeasurementType.WEIGHT,
    BodyMeasurementType.LEAN_MASS,
    BodyMeasurementType.BODY_WATER_MASS,
    BodyMeasurementType.BONE_MASS,
    -> if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
}

private fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()
