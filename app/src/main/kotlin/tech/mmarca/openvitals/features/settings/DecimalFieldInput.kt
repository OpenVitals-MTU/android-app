package tech.mmarca.openvitals.features.settings

import java.util.Locale
import kotlin.math.abs

/*
 * Text rules for the decimal fields in Settings (weight, height, stride).
 * This is input text, not display text: a '.' decimal and no grouping, in every locale,
 * so what the field shows is what the parser reads.
 */

private const val MaxDecimalFieldLength = 5

/** A unit round trip (lb to kg and back) is not exact. Half a displayed digit is close enough. */
private const val SameValueTolerance = 0.05

internal fun decimalFieldText(value: Double?): String =
    value?.let { "%.1f".format(Locale.US, it) }.orEmpty()

/** A decimal-comma keyboard may offer only ','. */
internal fun parseDecimalFieldText(text: String): Double? =
    text.replace(',', '.').toDoubleOrNull()

internal fun filterDecimalFieldInput(next: String): String =
    next.filter { it.isDigit() || it == '.' || it == ',' }.take(MaxDecimalFieldLength)

/**
 * True when [value] did not come from [text]: a reload, a reset or a unit switch. Only then
 * may the field rewrite its text. Rewriting it from the value on every keystroke turned a
 * typed "80.5" into "8.00.5", and "72,5" plus one key into 725.
 */
internal fun decimalFieldTextIsStale(text: String, value: Double?): Boolean {
    val typed = parseDecimalFieldText(text)
    return when {
        typed == null && value == null -> false
        typed == null || value == null -> true
        else -> abs(typed - value) > SameValueTolerance
    }
}
