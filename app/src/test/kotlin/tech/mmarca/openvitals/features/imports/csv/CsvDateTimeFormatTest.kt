package tech.mmarca.openvitals.features.imports.csv

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every assertion pins a fixed offset. [CsvTimeZoneMode.DEVICE] resolves against the host,
 * so the one device-mode test asserts the relationship, which holds in every zone.
 */
class CsvDateTimeFormatTest {

    // resolveCsvInstant.

    @Test
    fun `a timezone-less timestamp at a fixed plus two resolves two hours earlier in UTC`() {
        val resolved = resolveCsvInstant(
            "2026-07-01 08:12:00",
            CsvDateTimeSettings(
                format = CsvDateTimeFormat.YEAR_FIRST,
                zone = CsvTimeZoneMode.FIXED_OFFSET,
                fixedOffset = ZoneOffset.ofHours(2),
            ),
        )

        assertNotNull(resolved)
        assertEquals(Instant.parse("2026-07-01T06:12:00Z"), resolved!!.utc)
        assertEquals(ZoneOffset.ofHours(2), resolved.offset)
    }

    @Test
    fun `a timestamp read as UTC keeps its wall clock and a zero offset`() {
        val resolved = resolveCsvInstant(
            "2026-07-01 08:12:00",
            CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
        )

        assertEquals(Instant.parse("2026-07-01T08:12:00Z"), resolved!!.utc)
        assertEquals(ZoneOffset.UTC, resolved.offset)
    }

    @Test
    fun `an ISO timestamp carrying an offset overrides the selected UTC mode`() {
        val resolved = resolveCsvInstant(
            "2026-07-01T08:12:00+05:30",
            CsvDateTimeSettings(format = CsvDateTimeFormat.ISO_8601, zone = CsvTimeZoneMode.UTC),
        )

        assertEquals(Instant.parse("2026-07-01T02:42:00Z"), resolved!!.utc)
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), resolved.offset)
    }

    @Test
    fun `an ISO timestamp ending in Z resolves to that instant with no offset`() {
        val resolved = resolveCsvInstant(
            "2026-07-01T08:12:00Z",
            CsvDateTimeSettings(
                format = CsvDateTimeFormat.ISO_8601,
                zone = CsvTimeZoneMode.FIXED_OFFSET,
                fixedOffset = ZoneOffset.ofHours(9),
            ),
        )

        assertEquals(Instant.parse("2026-07-01T08:12:00Z"), resolved!!.utc)
        assertEquals(ZoneOffset.UTC, resolved.offset)
    }

    @Test
    fun `the device zone reports an offset that maps the instant back to the wall clock in the file`() {
        val resolved = resolveCsvInstant(
            "2026-07-01 08:12:00",
            CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.DEVICE),
        )

        assertNotNull(resolved)
        val wallClock = LocalDateTime.ofInstant(resolved!!.utc, resolved.offset)
        assertEquals(LocalDateTime.of(2026, 7, 1, 8, 12), wallClock)
    }

    @Test
    fun `epoch seconds resolve to the matching UTC instant`() {
        val resolved = resolveCsvInstant(
            "1782000000",
            CsvDateTimeSettings(format = CsvDateTimeFormat.EPOCH_SECONDS, zone = CsvTimeZoneMode.UTC),
        )

        assertEquals(Instant.ofEpochMilli(1_782_000_000L * 1000), resolved!!.utc)
    }

    @Test
    fun `a date-only cell resolves to midnight of that day`() {
        val resolved = resolveCsvInstant(
            "2026-07-01",
            CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST, zone = CsvTimeZoneMode.UTC),
        )

        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), resolved!!.utc)
    }

    @Test
    fun `a cell that does not match the chosen format resolves to null`() {
        val resolved = resolveCsvInstant(
            "not a date",
            CsvDateTimeSettings(format = CsvDateTimeFormat.YEAR_FIRST),
        )

        assertNull(resolved)
    }

    @Test
    fun `a custom pattern parses a shape none of the families cover`() {
        val resolved = resolveCsvInstant(
            "01 Jul 2026 08:12",
            CsvDateTimeSettings(
                format = CsvDateTimeFormat.CUSTOM,
                customPattern = "dd MMM yyyy HH:mm",
                zone = CsvTimeZoneMode.UTC,
            ),
        )

        assertEquals(Instant.parse("2026-07-01T08:12:00Z"), resolved!!.utc)
    }

    // parseCsvWallClock.

    @Test
    fun `a date-only pattern does not silently swallow a trailing time`() {
        // The leftover ' 08:12:00' must be rejected, not dropped, or every reading moves to midnight.
        assertEquals(
            LocalDateTime.of(2026, 7, 1, 8, 12),
            parseCsvWallClock("2026-07-01 08:12:00", CsvDateTimeFormat.YEAR_FIRST),
        )
    }

    @Test
    fun `a ten-digit epoch value is not misread as milliseconds`() {
        assertNull(parseCsvWallClock("1782000000", CsvDateTimeFormat.EPOCH_MILLIS))
    }

    @Test
    fun `a small counting number is not accepted as an epoch timestamp`() {
        // Without a plausibility bound, a column of step counts would be detected as epoch seconds.
        assertNull(parseCsvWallClock("1", CsvDateTimeFormat.EPOCH_SECONDS))
        assertNull(parseCsvWallClock("250", CsvDateTimeFormat.EPOCH_SECONDS))
        assertNull(parseCsvWallClock("1", CsvDateTimeFormat.AUTO))
    }

    @Test
    fun `a real epoch second inside the plausible window still parses`() {
        assertNotNull(parseCsvWallClock("1782000000", CsvDateTimeFormat.EPOCH_SECONDS))
    }

    // csvTimestampHasExplicitOffset.

    @Test
    fun `a bare ISO date is not mistaken for carrying an offset`() {
        // '2026-07-01' ends in '-01', which looks like an offset to a naive regex.
        assertFalse(csvTimestampHasExplicitOffset("2026-07-01"))
    }

    @Test
    fun `an offset suffix on a full timestamp is detected`() {
        assertTrue(csvTimestampHasExplicitOffset("2026-07-01T08:12:00+05:30"))
        assertTrue(csvTimestampHasExplicitOffset("2026-07-01T08:12:00Z"))
    }

    @Test
    fun `a timestamp with no offset suffix is reported as carrying none`() {
        assertFalse(csvTimestampHasExplicitOffset("2026-07-01 08:12:00"))
    }

    // detectCsvDateTimeFormat.

    @Test
    fun `a year-first sample is detected as year-first`() {
        val detection = detectCsvDateTimeFormat(
            listOf("2026-07-01 08:12:00", "2026-07-02 08:14:00", "2026-07-03 08:11:00"),
        )

        assertEquals(CsvDateTimeFormat.YEAR_FIRST, detection.format)
        assertEquals(3, detection.matchedRows)
        assertFalse(detection.ambiguousDayMonth)
    }

    @Test
    fun `a sample where both day-first and month-first parse every row is reported ambiguous rather than guessed`() {
        val detection = detectCsvDateTimeFormat(
            listOf("01/07/2026", "02/08/2026", "03/09/2026"),
        )

        assertTrue(detection.ambiguousDayMonth)
    }

    @Test
    fun `a day above twelve resolves the ordering to day-first`() {
        val detection = detectCsvDateTimeFormat(
            listOf("01/07/2026", "25/07/2026", "13/08/2026"),
        )

        assertEquals(CsvDateTimeFormat.DAY_FIRST, detection.format)
        assertFalse(detection.ambiguousDayMonth)
    }

    @Test
    fun `an unparsable sample reports that nothing matched`() {
        val detection = detectCsvDateTimeFormat(listOf("banana", "apple"))

        assertTrue(detection.matchedNothing)
    }

    @Test
    fun `an empty sample reports that nothing matched`() {
        val detection = detectCsvDateTimeFormat(emptyList())

        assertTrue(detection.matchedNothing)
        assertEquals(0, detection.totalRows)
    }
}
