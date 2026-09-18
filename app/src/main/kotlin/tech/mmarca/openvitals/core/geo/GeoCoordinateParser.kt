package tech.mmarca.openvitals.core.geo

/** Why a typed position could not be used. The wording is the screen's. */
enum class GeoCoordinateError {
    EMPTY,
    UNREADABLE,
    LATITUDE_RANGE,
    LONGITUDE_RANGE,
    MINUTES_RANGE,
    HEMISPHERE_CONFLICT,
}

sealed interface GeoCoordinateParseResult {
    /** Degrees, WGS 84. South and west are negative. */
    data class Parsed(val latitude: Double, val longitude: Double) : GeoCoordinateParseResult
    data class Invalid(val reason: GeoCoordinateError) : GeoCoordinateParseResult
}

/**
 * Reads a position a person typed or pasted. Accepts decimal degrees
 * (`48.8584, 2.2945`), degrees and decimal minutes as geocaching writes them
 * (`N 48° 51.504 E 002° 17.670`), and degrees, minutes, seconds. Latitude
 * comes first unless hemisphere letters say otherwise.
 */
object GeoCoordinateParser {

    fun parse(text: String): GeoCoordinateParseResult {
        if (text.isBlank()) return invalid(GeoCoordinateError.EMPTY)
        val tokens = tokenize(text.withDecimalPoints()) ?: return invalid(GeoCoordinateError.UNREADABLE)
        val numbers = tokens.filterIsInstance<Token.Number>()
        val letters = tokens.filterIsInstance<Token.Hemisphere>()
        if (numbers.size !in setOf(2, 4, 6)) return invalid(GeoCoordinateError.UNREADABLE)
        val perAxis = numbers.size / 2

        val axes = when (letters.size) {
            0 -> listOf(Axis(numbers.take(perAxis), null), Axis(numbers.drop(perAxis), null))
            2 -> lettered(tokens, perAxis) ?: return invalid(GeoCoordinateError.UNREADABLE)
            else -> return invalid(GeoCoordinateError.UNREADABLE)
        }
        val values = axes.map { axis ->
            axis.degrees() ?: return invalid(axis.error ?: GeoCoordinateError.UNREADABLE)
        }

        val first = axes[0].letter
        val second = axes[1].letter
        if (first != null && second != null && first.isLatitude == second.isLatitude) {
            return invalid(GeoCoordinateError.HEMISPHERE_CONFLICT)
        }
        val swapped = first != null && !first.isLatitude
        val latitude = if (swapped) values[1] else values[0]
        val longitude = if (swapped) values[0] else values[1]
        if (latitude !in -90.0..90.0) return invalid(GeoCoordinateError.LATITUDE_RANGE)
        if (longitude !in -180.0..180.0) return invalid(GeoCoordinateError.LONGITUDE_RANGE)
        return GeoCoordinateParseResult.Parsed(latitude, longitude)
    }

    /** Letters lead both axes (`N 48 E 2`) or trail both (`48 N 2 E`). Anything else is unreadable. */
    private fun lettered(tokens: List<Token>, perAxis: Int): List<Axis>? {
        val leading = tokens.first() is Token.Hemisphere
        val axisSize = perAxis + 1
        if (tokens.size != axisSize * 2) return null
        return tokens.chunked(axisSize).map { chunk ->
            val letter = (if (leading) chunk.first() else chunk.last()) as? Token.Hemisphere ?: return null
            val numbers = (if (leading) chunk.drop(1) else chunk.dropLast(1))
                .map { it as? Token.Number ?: return null }
            Axis(numbers, letter)
        }
    }

    private fun invalid(reason: GeoCoordinateError) = GeoCoordinateParseResult.Invalid(reason)
}

private sealed interface Token {
    /** [negative] is kept apart from the value so `-0° 30'` stays negative. */
    data class Number(val magnitude: Double, val negative: Boolean, val signed: Boolean) : Token
    data class Hemisphere(val letter: Char) : Token {
        val isLatitude get() = letter == 'N' || letter == 'S'
        val isNegative get() = letter == 'S' || letter == 'W'
    }
}

/** One axis: degrees, then optional minutes and seconds, with an optional hemisphere letter. */
private class Axis(val numbers: List<Token.Number>, val letter: Token.Hemisphere?) {
    var error: GeoCoordinateError? = null
        private set

    fun degrees(): Double? {
        val head = numbers.first()
        val parts = numbers.drop(1)
        // Only the degrees may carry a sign.
        if (parts.any { it.signed }) return null
        if (parts.any { it.magnitude >= 60.0 }) return fail(GeoCoordinateError.MINUTES_RANGE)
        // A sign and a letter together can contradict, so neither is trusted.
        if (letter != null && head.signed) return fail(GeoCoordinateError.HEMISPHERE_CONFLICT)
        val magnitude = head.magnitude +
            (parts.getOrNull(0)?.magnitude ?: 0.0) / 60.0 +
            (parts.getOrNull(1)?.magnitude ?: 0.0) / 3600.0
        val negative = letter?.isNegative ?: head.negative
        return if (negative) -magnitude else magnitude
    }

    private fun fail(reason: GeoCoordinateError): Double? {
        error = reason
        return null
    }
}

/**
 * A comma between two digits is a decimal comma when the text has no point
 * at all, as in `48,8584, 2,2945` or `N 48° 51,504`.
 */
private fun String.withDecimalPoints(): String =
    if ('.' in this) this else replace(DecimalComma, "$1.$2")

private fun tokenize(text: String): List<Token>? {
    val tokens = mutableListOf<Token>()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char.isDigit() || char.isSignAt(text, index) -> {
                val match = NumberPattern.matchAt(text, index) ?: return null
                val signed = char == '-' || char == '+'
                val magnitude = match.value.trimStart('+', '-').toDoubleOrNull() ?: return null
                tokens += Token.Number(magnitude, negative = char == '-', signed = signed)
                index = match.range.last + 1
            }
            char.uppercaseChar() in HemisphereLetters -> {
                tokens += Token.Hemisphere(char.uppercaseChar())
                index++
            }
            char.isWhitespace() || char in Separators -> index++
            else -> return null
        }
    }
    return tokens
}

/** A sign starts a number. Between two digits, as in `48-51-30`, it separates. */
private fun Char.isSignAt(text: String, index: Int): Boolean =
    (this == '-' || this == '+') &&
        text.getOrNull(index + 1)?.isDigit() == true &&
        text.getOrNull(index - 1)?.isDigit() != true

private val NumberPattern = Regex("""[+-]?\d+(?:\.\d+)?""")
private val DecimalComma = Regex("""(\d),(\d)""")

/** Only these. `O` is west in Spanish and east in German. */
private const val HemisphereLetters = "NSEW"
private const val Separators = "°º'′’\"″,;:/|-"
