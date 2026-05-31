package tech.mmarca.openvitals.features.manualentry

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFileParserTest {

    @Test fun `parse extracts timestamped GPX track points and summaries`() {
        val result = RouteFileParser.parse(
            """
            <gpx version="1.1" creator="OpenTracks">
              <trk>
                <name>Morning ride</name>
                <desc>Easy commute</desc>
                <type>cycling</type>
                <trkseg>
                  <trkpt lat="59.0000" lon="24.0000">
                    <ele>10.0</ele>
                    <time>2026-05-26T08:30:00Z</time>
                  </trkpt>
                  <trkpt lat="59.0010" lon="24.0020">
                    <ele>18.0</ele>
                    <time>2026-05-26T08:31:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """.trimIndent(),
            fileName = "run.gpx",
        )

        assertEquals("run.gpx", result.fileName)
        assertEquals("Morning ride", result.name)
        assertEquals("Easy commute", result.description)
        assertEquals("cycling", result.type)
        assertEquals(2, result.points.size)
        assertEquals(8.0, result.elevationGainedMeters, 0.001)
        assertTrue(result.distanceMeters > 0.0)
    }

    @Test fun `parseFile extracts timestamped KML gx track from KMZ`() {
        val result = RouteFileParser.parseFile(
            kmzBytes(
                """
                <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
                  <Document>
                    <name>Archive route</name>
                    <Placemark>
                      <name>Evening run</name>
                      <description>Progression effort</description>
                      <gx:Track>
                        <when>2026-05-26T18:00:00Z</when>
                        <when>2026-05-26T18:01:00Z</when>
                        <gx:coord>24.0000 59.0000 10.0</gx:coord>
                        <gx:coord>24.0020 59.0010 22.0</gx:coord>
                      </gx:Track>
                    </Placemark>
                  </Document>
                </kml>
                """.trimIndent(),
            ),
            fileName = "run.kmz",
        )

        assertEquals("run.kmz", result.fileName)
        assertEquals("Evening run", result.name)
        assertEquals("Progression effort", result.description)
        assertEquals(2, result.points.size)
        assertEquals(12.0, result.elevationGainedMeters, 0.001)
        assertTrue(result.distanceMeters > 0.0)
    }

    @Test fun `parseFile extracts untimestamped KML line string with synthetic timing`() {
        val result = RouteFileParser.parseFile(
            """
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <Placemark>
                  <name>Manual route</name>
                  <description>Imported path</description>
                  <LineString>
                    <coordinates>
                      24.0000,59.0000,10.0
                      24.0020,59.0010,22.0
                      24.0040,59.0020,20.0
                    </coordinates>
                  </LineString>
                </Placemark>
              </Document>
            </kml>
            """.trimIndent().toByteArray(Charsets.UTF_8),
            fileName = "route.kml",
        )

        assertEquals("Manual route", result.name)
        assertFalse(result.hasRecordedTimestamps)
        assertFalse(result.hasImportedTimeRange)
        assertEquals(3, result.points.size)
        assertTrue(result.distanceMeters > 0.0)
    }

    @Test fun `parse simplifies very large route files`() {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val points = (0 until 2_100).joinToString(separator = "\n") { index ->
            """
            <trkpt lat="${59.0 + index * 0.00001}" lon="${24.0 + index * 0.00001}">
              <time>${start.plusSeconds(index.toLong())}</time>
            </trkpt>
            """.trimIndent()
        }
        val result = RouteFileParser.parse(
            """
            <gpx version="1.1">
              <trk><trkseg>
                $points
              </trkseg></trk>
            </gpx>
            """.trimIndent(),
            fileName = "large.gpx",
        )

        assertEquals(2_100, result.originalPointCount)
        assertTrue(result.points.size < result.originalPointCount)
        assertEquals(2_000, result.points.size)
    }

    @Test fun `parse rejects GPX without two timestamped points`() {
        val failure = runCatching {
            RouteFileParser.parse(
                """
                <gpx version="1.1">
                  <trk><trkseg><trkpt lat="59.0" lon="24.0" /></trkseg></trk>
                </gpx>
                """.trimIndent(),
            )
        }

        assertTrue(failure.isFailure)
    }

    @Test fun `parseFile rejects oversized raw route file before parsing`() {
        val failure = runCatching {
            RouteFileParser.parseFile(ByteArray(MaxRouteFileBytes + 1), fileName = "large.gpx")
        }

        assertTrue(failure.isFailure)
        assertEquals("Route file is too large.", failure.exceptionOrNull()?.message)
    }

    @Test fun `parseFile rejects oversized KMZ route entry before XML parsing`() {
        val failure = runCatching {
            RouteFileParser.parseFile(oversizedKmzBytes(), fileName = "large.kmz")
        }

        assertTrue(failure.isFailure)
        assertEquals("KMZ route entry is too large.", failure.exceptionOrNull()?.message)
    }

    private fun kmzBytes(kmlText: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"))
            zip.write(kmlText.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun oversizedKmzBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024) { 'a'.code.toByte() }
        var remaining = MaxKmzRouteEntryBytes + 1
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"))
            while (remaining > 0) {
                val size = minOf(remaining, chunk.size)
                zip.write(chunk, 0, size)
                remaining -= size
            }
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
