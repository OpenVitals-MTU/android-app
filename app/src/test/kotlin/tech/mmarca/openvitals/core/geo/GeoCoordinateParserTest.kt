package tech.mmarca.openvitals.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GeoCoordinateParserTest {

    private fun assertParses(text: String, latitude: Double, longitude: Double) {
        when (val result = GeoCoordinateParser.parse(text)) {
            is GeoCoordinateParseResult.Parsed -> {
                assertEquals("latitude of \"$text\"", latitude, result.latitude, 1e-5)
                assertEquals("longitude of \"$text\"", longitude, result.longitude, 1e-5)
            }
            is GeoCoordinateParseResult.Invalid -> fail("\"$text\" was ${result.reason}")
        }
    }

    private fun assertInvalid(text: String, reason: GeoCoordinateError) {
        assertEquals("\"$text\"", GeoCoordinateParseResult.Invalid(reason), GeoCoordinateParser.parse(text))
    }

    @Test fun `decimal degrees in the usual spellings`() {
        assertParses("48.8584, 2.2945", 48.8584, 2.2945)
        assertParses("48.8584 2.2945", 48.8584, 2.2945)
        assertParses("48.8584,2.2945", 48.8584, 2.2945)
        assertParses("  48.8584;2.2945  ", 48.8584, 2.2945)
        assertParses("-33.8568, 151.2153", -33.8568, 151.2153)
        assertParses("-33.8568 -70.6483", -33.8568, -70.6483)
        assertParses("+48.8584, +2.2945", 48.8584, 2.2945)
        assertParses("48.8584°, 2.2945°", 48.8584, 2.2945)
        assertParses("48, 2", 48.0, 2.0)
    }

    @Test fun `decimal commas when the text has no point`() {
        assertParses("48,8584, 2,2945", 48.8584, 2.2945)
        assertParses("48,8584 2,2945", 48.8584, 2.2945)
        assertParses("N 48° 51,504 E 002° 17,670", 48.8584, 2.2945)
    }

    @Test fun `hemisphere letters, leading or trailing, any case`() {
        assertParses("N 48.8584 E 2.2945", 48.8584, 2.2945)
        assertParses("48.8584 N, 2.2945 E", 48.8584, 2.2945)
        assertParses("s 33.8568 w 70.6483", -33.8568, -70.6483)
        assertParses("33.8568S 70.6483W", -33.8568, -70.6483)
        // Letters can put longitude first.
        assertParses("E 2.2945 N 48.8584", 48.8584, 2.2945)
    }

    @Test fun `degrees and decimal minutes, the geocaching format`() {
        assertParses("N 48° 51.504 E 002° 17.670", 48.8584, 2.2945)
        assertParses("N48°51.504' E002°17.670'", 48.8584, 2.2945)
        assertParses("S 33° 51.408 W 070° 38.898", -33.8568, -70.6483)
        assertParses("48 51.504, 2 17.670", 48.8584, 2.2945)
        assertParses("-33 51.408 -70 38.898", -33.8568, -70.6483)
        // A negative zero degree keeps its sign.
        assertParses("-0 30.0, -0 15.0", -0.5, -0.25)
    }

    @Test fun `degrees, minutes and seconds`() {
        assertParses("48°51'30.24\"N 2°17'40.2\"E", 48.8584, 2.2945)
        assertParses("48º 51′ 30.24″ N, 2º 17′ 40.2″ E", 48.8584, 2.2945)
        assertParses("48-51-30.24 N 2-17-40.2 E", 48.8584, 2.2945)
        assertParses("48:51:30.24, 2:17:40.2", 48.8584, 2.2945)
    }

    @Test fun `the edges of the globe are allowed`() {
        assertParses("90, 180", 90.0, 180.0)
        assertParses("-90, -180", -90.0, -180.0)
    }

    @Test fun `what cannot be used says why`() {
        assertInvalid("", GeoCoordinateError.EMPTY)
        assertInvalid("   ", GeoCoordinateError.EMPTY)
        assertInvalid("Eiffel Tower", GeoCoordinateError.UNREADABLE)
        assertInvalid("48.8584", GeoCoordinateError.UNREADABLE)
        assertInvalid("48 51 30", GeoCoordinateError.UNREADABLE)
        assertInvalid("N 48.8584 2.2945", GeoCoordinateError.UNREADABLE)
        assertInvalid("48.8584 N E 2.2945", GeoCoordinateError.UNREADABLE)
        assertInvalid("48 -51.504, 2 17.670", GeoCoordinateError.UNREADABLE)
        assertInvalid("91, 2", GeoCoordinateError.LATITUDE_RANGE)
        assertInvalid("48, 181", GeoCoordinateError.LONGITUDE_RANGE)
        assertInvalid("N 48° 61.504 E 002° 17.670", GeoCoordinateError.MINUTES_RANGE)
        assertInvalid("48°51'60\"N 2°17'40\"E", GeoCoordinateError.MINUTES_RANGE)
        assertInvalid("N 48.85 N 2.29", GeoCoordinateError.HEMISPHERE_CONFLICT)
        assertInvalid("-48.85 N 2.29 E", GeoCoordinateError.HEMISPHERE_CONFLICT)
    }

    @Test fun `O is refused because it is west in Spanish and east in German`() {
        assertInvalid("N 48.8584 O 2.2945", GeoCoordinateError.UNREADABLE)
    }
}
