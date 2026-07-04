package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



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

    @Test fun `parseFile extracts timestamped FIT activity records and sport`() {
        val result = RouteFileParser.parseFile(
            fitActivityBytes(
                sport = 2,
                points = listOf(
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:30:00Z"),
                        latitude = 59.0000,
                        longitude = 24.0000,
                        altitudeMeters = 10.0,
                    ),
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:31:00Z"),
                        latitude = 59.0010,
                        longitude = 24.0020,
                        altitudeMeters = 22.0,
                    ),
                ),
            ),
            fileName = "morning-ride.fit",
        )

        assertEquals("morning-ride.fit", result.fileName)
        assertEquals("cycling", result.type)
        assertEquals(2, result.points.size)
        assertEquals(Instant.parse("2026-05-26T08:30:00Z"), result.startTime)
        assertEquals(Instant.parse("2026-05-26T08:31:00Z"), result.endTime)
        assertEquals(12.0, result.elevationGainedMeters, 0.001)
        assertTrue(result.distanceMeters > 0.0)
    }

    @Test fun `parseFile imports FIT activity without GPS route`() {
        val result = RouteFileParser.parseFile(
            fitActivityBytes(
                sport = 10,
                points = emptyList(),
                sessionTime = Instant.parse("2026-05-26T08:30:00Z"),
                elapsedSeconds = 45 * 60,
                totalCaloriesKcal = 220,
            ),
            fileName = "indoor-workout.fit",
        )

        assertEquals("indoor-workout.fit", result.fileName)
        assertEquals("training", result.type)
        assertTrue(result.points.isEmpty())
        assertEquals(0, result.originalPointCount)
        assertEquals(Instant.parse("2026-05-26T08:30:00Z"), result.startTime)
        assertEquals(Instant.parse("2026-05-26T09:15:00Z"), result.endTime)
        assertEquals(220.0, result.activeCaloriesKcal!!, 0.001)
    }

    @Test fun `parseFile imports FIT activity and ignores unusable one point route`() {
        val result = RouteFileParser.parseFile(
            fitActivityBytes(
                sport = 2,
                points = listOf(
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:30:00Z"),
                        latitude = 59.0000,
                        longitude = 24.0000,
                        altitudeMeters = 10.0,
                    ),
                ),
                sessionTime = Instant.parse("2026-05-26T08:30:00Z"),
                elapsedSeconds = 10 * 60,
                totalDistanceMeters = 2_400.0,
            ),
            fileName = "single-point.fit",
        )

        assertEquals("cycling", result.type)
        assertTrue(result.points.isEmpty())
        assertEquals(1, result.originalPointCount)
        assertEquals(2_400.0, result.distanceMeters, 0.001)
        assertEquals(Instant.parse("2026-05-26T08:40:00Z"), result.endTime)
    }

    @Test fun `parseFile imports FIT course as route without activity time range`() {
        val result = RouteFileParser.parseFile(
            fitCourseBytes(
                name = "Park Loop",
                sport = 2,
                points = listOf(
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:30:00Z"),
                        latitude = 59.0000,
                        longitude = 24.0000,
                        altitudeMeters = 10.0,
                    ),
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:35:00Z"),
                        latitude = 59.0010,
                        longitude = 24.0020,
                        altitudeMeters = 22.0,
                    ),
                ),
                totalDistanceMeters = 2_400.0,
                elapsedSeconds = 10 * 60,
            ),
            fileName = "park-loop.fit",
        )

        assertEquals("Park Loop", result.name)
        assertEquals("cycling", result.type)
        assertEquals(2, result.points.size)
        assertFalse(result.hasRecordedTimestamps)
        assertFalse(result.hasImportedTimeRange)
        assertEquals(2_400.0, result.distanceMeters, 0.001)
        assertEquals(600L, result.durationSeconds)
    }

    @Test fun `parseFile imports sparse FIT course without route geometry`() {
        val result = RouteFileParser.parseFile(
            fitCourseBytes(
                name = "Tiny Course",
                sport = 11,
                points = listOf(
                    FitTestPoint(
                        time = Instant.parse("2026-05-26T08:30:00Z"),
                        latitude = 59.0000,
                        longitude = 24.0000,
                        altitudeMeters = 10.0,
                    ),
                ),
                totalDistanceMeters = 0.0,
                elapsedSeconds = 5 * 60,
            ),
            fileName = "tiny-course.fit",
        )

        assertEquals("Tiny Course", result.name)
        assertEquals("walking", result.type)
        assertTrue(result.points.isEmpty())
        assertEquals(1, result.originalPointCount)
        assertFalse(result.hasRecordedTimestamps)
        assertFalse(result.hasImportedTimeRange)
        assertEquals(5 * 60L, result.durationSeconds)
    }

    @Test fun `parseFile imports FIT workout definition without activity session`() {
        val result = RouteFileParser.parseFile(
            fitWorkoutBytes(
                name = "Tempo Run",
                sport = 1,
                timeStepSeconds = listOf(10 * 60, 5 * 60),
            ),
            fileName = "tempo-run.fit",
        )

        assertEquals("Tempo Run", result.name)
        assertEquals("running", result.type)
        assertTrue(result.points.isEmpty())
        assertFalse(result.hasRecordedTimestamps)
        assertFalse(result.hasImportedTimeRange)
        assertEquals(15 * 60L, result.durationSeconds)
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
        assertEquals("Activity file is too large.", failure.exceptionOrNull()?.message)
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

    private fun fitActivityBytes(
        sport: Int,
        points: List<FitTestPoint>,
        sessionTime: Instant = points.firstOrNull()?.time ?: Instant.parse("2026-05-26T08:30:00Z"),
        elapsedSeconds: Long = points.lastOrNull()
            ?.time
            ?.epochSecond
            ?.minus(sessionTime.epochSecond)
            ?.coerceAtLeast(1L)
            ?: 60L,
        totalDistanceMeters: Double? = null,
        totalCaloriesKcal: Int? = null,
        totalAscentMeters: Int? = null,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 4)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 18,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 5, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 7, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 8, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 9, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 11, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 21, size = 2, baseType = 132),
            ),
        )
        data.write(1)
        data.writeUInt32(sessionTime.plusSeconds(elapsedSeconds).fitTimestamp())
        data.writeUInt32(sessionTime.fitTimestamp())
        data.write(sport)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeNullableUInt32(totalDistanceMeters?.let { Math.round(it * 100.0) })
        data.writeNullableUInt16(totalCaloriesKcal)
        data.writeNullableUInt16(totalAscentMeters)

        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 20,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 0, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 1, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 2, size = 2, baseType = 132),
            ),
        )
        points.forEach { point ->
            data.write(0)
            data.writeUInt32(point.time.fitTimestamp())
            data.writeInt32(point.latitude.semicircles())
            data.writeInt32(point.longitude.semicircles())
            data.writeUInt16(point.altitudeRaw())
        }

        val dataBytes = data.toByteArray()
        return ByteArrayOutputStream().apply {
            write(14)
            write(16)
            writeUInt16(0)
            writeUInt32(dataBytes.size.toLong())
            write(byteArrayOf('.'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte()))
            writeUInt16(0)
            write(dataBytes)
            writeUInt16(0)
        }.toByteArray()
    }

    private fun fitCourseBytes(
        name: String,
        sport: Int,
        points: List<FitTestPoint>,
        elapsedSeconds: Long,
        totalDistanceMeters: Double,
    ): ByteArray {
        val lapStart = points.firstOrNull()?.time ?: Instant.parse("2026-05-26T08:30:00Z")
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 6)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 31,
            fields = listOf(
                FitTestFieldDefinition(number = 4, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 5, size = 32, baseType = 7),
            ),
        )
        data.write(1)
        data.write(sport)
        data.writeFitString(name, size = 32)

        data.writeFitDefinition(
            localMessageType = 2,
            globalMessageNumber = 19,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 7, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 8, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 9, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 21, size = 2, baseType = 132),
            ),
        )
        data.write(2)
        data.writeUInt32(lapStart.plusSeconds(elapsedSeconds).fitTimestamp())
        data.writeUInt32(lapStart.fitTimestamp())
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(Math.round(totalDistanceMeters * 100.0))
        data.writeUInt16(12)

        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 20,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 0, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 1, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 2, size = 2, baseType = 132),
            ),
        )
        points.forEach { point ->
            data.write(0)
            data.writeUInt32(point.time.fitTimestamp())
            data.writeInt32(point.latitude.semicircles())
            data.writeInt32(point.longitude.semicircles())
            data.writeUInt16(point.altitudeRaw())
        }

        return fitFileBytes(data)
    }

    private fun fitWorkoutBytes(
        name: String,
        sport: Int,
        timeStepSeconds: List<Int>,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 5)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 26,
            fields = listOf(
                FitTestFieldDefinition(number = 4, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 6, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 8, size = 32, baseType = 7),
            ),
        )
        data.write(1)
        data.write(sport)
        data.writeUInt16(timeStepSeconds.size)
        data.writeFitString(name, size = 32)

        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 27,
            fields = listOf(
                FitTestFieldDefinition(number = 254, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 1, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
            ),
        )
        timeStepSeconds.forEachIndexed { index, seconds ->
            data.write(0)
            data.writeUInt16(index)
            data.write(0)
            data.writeUInt32(seconds * 1000L)
        }

        return fitFileBytes(data)
    }

    private fun ByteArrayOutputStream.writeFitFileId(fileType: Int) {
        writeFitDefinition(
            localMessageType = 3,
            globalMessageNumber = 0,
            fields = listOf(FitTestFieldDefinition(number = 0, size = 1, baseType = 0)),
        )
        write(3)
        write(fileType)
    }

    private fun fitFileBytes(data: ByteArrayOutputStream): ByteArray {
        val dataBytes = data.toByteArray()
        return ByteArrayOutputStream().apply {
            write(14)
            write(16)
            writeUInt16(0)
            writeUInt32(dataBytes.size.toLong())
            write(byteArrayOf('.'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte()))
            writeUInt16(0)
            write(dataBytes)
            writeUInt16(0)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeFitDefinition(
        localMessageType: Int,
        globalMessageNumber: Int,
        fields: List<FitTestFieldDefinition>,
    ) {
        write(0x40 or localMessageType)
        write(0)
        write(0)
        writeUInt16(globalMessageNumber)
        write(fields.size)
        fields.forEach { field ->
            write(field.number)
            write(field.size)
            write(field.baseType)
        }
    }

    private fun ByteArrayOutputStream.writeUInt16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Long) {
        write((value and 0xFF).toInt())
        write(((value ushr 8) and 0xFF).toInt())
        write(((value ushr 16) and 0xFF).toInt())
        write(((value ushr 24) and 0xFF).toInt())
    }

    private fun ByteArrayOutputStream.writeNullableUInt32(value: Long?) {
        writeUInt32(value ?: 0xFFFFFFFFL)
    }

    private fun ByteArrayOutputStream.writeNullableUInt16(value: Int?) {
        writeUInt16(value ?: 0xFFFF)
    }

    private fun ByteArrayOutputStream.writeInt32(value: Int) {
        writeUInt32(value.toLong() and 0xFFFFFFFFL)
    }

    private fun ByteArrayOutputStream.writeFitString(value: String, size: Int) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val output = ByteArray(size)
        bytes.copyInto(output, endIndex = minOf(bytes.size, size - 1))
        write(output)
    }

    private fun Instant.fitTimestamp(): Long =
        epochSecond - 631_065_600L

    private fun Double.semicircles(): Int =
        Math.round(this * 2_147_483_648.0 / 180.0).toInt()

    private fun FitTestPoint.altitudeRaw(): Int =
        Math.round((altitudeMeters + 500.0) * 5.0).toInt()

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

    private data class FitTestPoint(
        val time: Instant,
        val latitude: Double,
        val longitude: Double,
        val altitudeMeters: Double,
    )

    private data class FitTestFieldDefinition(
        val number: Int,
        val size: Int,
        val baseType: Int,
    )
}
