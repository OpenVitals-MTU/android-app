package tech.mmarca.openvitals.core.geo

import java.net.URLDecoder

/**
 * What another app shared. A search-only link has a [name] and no position,
 * so every part is optional.
 */
data class SharedGeoPoint(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val name: String? = null,
) {
    val hasPosition: Boolean get() = latitude != null && longitude != null
}

/**
 * Reads a `geo:` link: `geo:lat,lon[,alt][;crs=…][?q=lat,lon(label)]`.
 * Works on the raw string: a `geo:` link is opaque, so Android's
 * `Uri.getQueryParameter` throws on it.
 */
object GeoUri {

    fun parse(uri: String): SharedGeoPoint? {
        val trimmed = uri.trim()
        if (!trimmed.startsWith(Scheme, ignoreCase = true)) return null
        val body = trimmed.substring(Scheme.length)
        val path = body.substringBefore('?').substringBefore(';')
        val query = body.substringAfter('?', missingDelimiterValue = "")

        val pathNumbers = path.split(',').map { it.trim().toDoubleOrNull() }
        val pathPosition = position(pathNumbers.getOrNull(0), pathNumbers.getOrNull(1))
        val altitude = pathNumbers.getOrNull(2)

        var queryPosition: Pair<Double, Double>? = null
        var name: String? = null
        query.split('&').filter { it.isNotEmpty() }.forEach { parameter ->
            val key = parameter.substringBefore('=')
            val value = decode(parameter.substringAfter('=', missingDelimiterValue = ""))
            when {
                key == "q" -> {
                    val match = LabelledPosition.matchEntire(value)
                    if (match == null) {
                        name = name ?: value.cleanName()
                    } else {
                        queryPosition = position(
                            match.groupValues[1].toDoubleOrNull(),
                            match.groupValues[2].toDoubleOrNull(),
                        )
                        name = match.groupValues[3].cleanName() ?: name
                    }
                }
                // geo:37.78,-122.40?z=14&(Wikimedia+Foundation)
                value.isEmpty() && key.startsWith("(") && key.endsWith(")") ->
                    name = name ?: decode(key).removeSurrounding("(", ")").cleanName()
            }
        }

        // `geo:0,0?q=…` means "the position is in the query".
        val isPlaceholder = pathPosition == 0.0 to 0.0
        val chosen = if (isPlaceholder) queryPosition else pathPosition ?: queryPosition
        if (chosen == null && name == null) return null
        return SharedGeoPoint(
            latitude = chosen?.first,
            longitude = chosen?.second,
            altitudeMeters = altitude.takeIf { chosen != null && chosen == pathPosition },
            name = name,
        )
    }

    private fun decode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: IllegalArgumentException) {
            value
        }

    private const val Scheme = "geo:"
    private val LabelledPosition =
        Regex("""\s*([+-]?\d+(?:\.\d+)?)\s*,\s*([+-]?\d+(?:\.\d+)?)\s*(?:\((.*)\))?\s*""")
}

/** A pair only when both are present and on the globe. */
private fun position(latitude: Double?, longitude: Double?): Pair<Double, Double>? =
    if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
        latitude to longitude
    } else {
        null
    }

private fun String.cleanName(): String? = trim().takeIf { it.isNotEmpty() }

/**
 * Finds a position in text another app shared: a `geo:` link, a maps URL, or
 * bare coordinates. A short link such as `maps.app.goo.gl` holds no position,
 * and this app has no network to resolve it.
 */
object SharedLocationText {

    fun parse(text: String): SharedGeoPoint? {
        val name = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.hasLink() && !it.isPosition() }
            ?.cleanName()

        GeoLinkPattern.find(text)?.let { match ->
            GeoUri.parse(match.value)?.takeIf { it.hasPosition }?.let { point ->
                return point.copy(name = point.name ?: name)
            }
        }
        UrlPattern.findAll(text).forEach { url ->
            UrlPositionPatterns.forEach { pattern ->
                val match = pattern.find(url.value)
                val found = match?.let {
                    position(it.groupValues[1].toDoubleOrNull(), it.groupValues[2].toDoubleOrNull())
                }
                if (found != null) return SharedGeoPoint(found.first, found.second, name = name)
            }
        }
        val plain = text.replace(UrlPattern, " ").lineSequence()
            .map { GeoCoordinateParser.parse(it) }
            .filterIsInstance<GeoCoordinateParseResult.Parsed>()
            .firstOrNull()
        if (plain != null) return SharedGeoPoint(plain.latitude, plain.longitude, name = name)
        return name?.let { SharedGeoPoint(name = it) }
    }

    private fun String.hasLink(): Boolean =
        UrlPattern.containsMatchIn(this) || GeoLinkPattern.containsMatchIn(this)

    private fun String.isPosition(): Boolean =
        GeoCoordinateParser.parse(this) is GeoCoordinateParseResult.Parsed

    private val UrlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val GeoLinkPattern = Regex("""geo:[0-9+-][^\s]*""", RegexOption.IGNORE_CASE)

    private const val Number = """([+-]?\d+(?:\.\d+)?)"""
    private const val Comma = """(?:,|%2C)"""

    /** Most specific first: a pin or query names the place, `@` is only the map centre. */
    private val UrlPositionPatterns = listOf(
        // OpenStreetMap marker.
        Regex("""[?&]mlat=$Number&mlon=$Number"""),
        // Google, Apple, OsmAnd and others: ?q=lat,lon and its relatives.
        Regex(
            """[?&#](?:q|query|ll|sll|daddr|destination|center|pin|loc)=(?:loc:)?$Number$Comma\+?$Number""",
            RegexOption.IGNORE_CASE,
        ),
        // Google place data: !3dLAT!4dLON.
        Regex("""!3d$Number!4d$Number"""),
        // Google map centre: /@lat,lon,zoom.
        Regex("""/@$Number,$Number"""),
        // OpenStreetMap and OsmAnd fragments: #map=zoom/lat/lon, #zoom/lat/lon.
        Regex("""#(?:map=)?\d+(?:\.\d+)?/$Number/$Number"""),
    )
}
