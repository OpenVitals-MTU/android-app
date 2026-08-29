package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.fit.FitCrc
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitRouteParser
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.TcxRouteParser

/**
 * The workout export's one promise is the inverse of the route export's:
 * everything about the session, NOTHING about where it happened. The TCX
 * side is verified by round-tripping through the app's own [TcxRouteParser],
 * which reads a positionless TCX as "the indoor case" — so an export the
 * app itself can re-import is one Strava and Garmin accept too.
 */
class ActivityWorkoutExportTest {

    @Test fun `tcx export round-trips through the app's own parser without a route`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutTcx(
            workout = workout(),
            heartRateSamples = heartRateSamples(),
            output = output,
            serializer = recordingXmlSerializer(),
        )

        val tcx = output.toString(Charsets.UTF_8.name())
        assertTrue(tcx.contains("""Sport="Running""""))
        // The point of the format: no coordinates, ever.
        assertFalse(tcx.contains("Position"))
        assertFalse(tcx.contains("LatitudeDegrees"))

        val parsed = TcxRouteParser.parse(tcx, fileName = "morning-run.tcx")
        assertTrue(parsed.points.isEmpty())
        assertEquals(Instant.parse("2026-05-26T08:30:00Z"), parsed.startTime)
        assertEquals(3600L, parsed.durationSeconds)
        assertEquals(10250.5, parsed.distanceMeters, 0.001)
        assertEquals(640.0, parsed.totalCaloriesKcal!!, 0.001)
        val heartRates = parsed.bleSamples.heartRateSamples
        assertEquals(listOf(140L, 172L), heartRates.map { it.beatsPerMinute })
        assertEquals(Instant.parse("2026-05-26T08:40:00Z"), heartRates.first().time)
    }

    @Test fun `tcx drops samples outside the session and zero readings`() {
        val output = ByteArrayOutputStream()
        val samples = heartRateSamples() + listOf(
            sample("2026-05-26T07:00:00Z", 130), // before the session
            sample("2026-05-26T10:00:00Z", 120), // after it
            sample("2026-05-26T08:45:00Z", 0), // sensor dropout, not a heart
        )

        writeActivityWorkoutTcx(
            workout = workout(),
            heartRateSamples = samples,
            output = output,
            serializer = recordingXmlSerializer(),
        )

        val parsed = TcxRouteParser.parse(output.toString(Charsets.UTF_8.name()))
        assertEquals(listOf(140L, 172L), parsed.bleSamples.heartRateSamples.map { it.beatsPerMinute })
    }

    @Test fun `tcx carries the recorded average and the sampled maximum`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutTcx(
            workout = workout(averageHeartRateBpm = 151),
            heartRateSamples = heartRateSamples(),
            output = output,
            serializer = recordingXmlSerializer(),
        )

        val tcx = output.toString(Charsets.UTF_8.name())
        assertTrue(tcx.contains("<AverageHeartRateBpm><Value>151</Value></AverageHeartRateBpm>"))
        assertTrue(tcx.contains("<MaximumHeartRateBpm><Value>172</Value></MaximumHeartRateBpm>"))
    }

    @Test fun `tcx without heart rate writes no track at all`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutTcx(
            workout = workout(),
            heartRateSamples = emptyList(),
            output = output,
            serializer = recordingXmlSerializer(),
        )

        val tcx = output.toString(Charsets.UTF_8.name())
        assertFalse(tcx.contains("<Track>"))
        // Still a complete activity to the app's own parser.
        val parsed = TcxRouteParser.parse(tcx)
        assertEquals(3600L, parsed.durationSeconds)
    }

    @Test fun `tcx puts escaped title and notes into Notes`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutTcx(
            workout = workout(title = """Run <with> "friends" & family""", notes = "it's <fine>"),
            heartRateSamples = emptyList(),
            output = output,
            serializer = recordingXmlSerializer(),
        )

        val tcx = output.toString(Charsets.UTF_8.name())
        assertTrue(tcx.contains("<Notes>Run &lt;with&gt; &quot;friends&quot; &amp; family\n\nit's &lt;fine&gt;</Notes>"))
    }

    @Test fun `sports outside TCX's vocabulary export as Other`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutTcx(
            workout = workout(exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING),
            heartRateSamples = emptyList(),
            output = output,
            serializer = recordingXmlSerializer(),
        )

        assertTrue(output.toString(Charsets.UTF_8.name()).contains("""Sport="Other""""))
    }

    @Test fun `csv writes one header row and one escaped value row`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutCsv(
            workout = workout(
                title = """Morning, "Run"""",
                notes = "line1\nline2",
                averageHeartRateBpm = 151,
            ),
            heartRateSamples = heartRateSamples(),
            output = output,
        )

        val expectedHeader = "title,activity_type,start_time,end_time,duration_seconds," +
            "distance_meters,elevation_gained_meters,steps,total_calories_kcal,active_calories_kcal," +
            "average_heart_rate_bpm,max_heart_rate_bpm,average_speed_meters_per_second," +
            "average_power_watts,average_steps_cadence_spm,average_cycling_cadence_rpm," +
            "floors_climbed,wheelchair_pushes,source,notes"
        val expectedRow = "\"Morning, \"\"Run\"\"\",running," +
            "2026-05-26T08:30:00Z,2026-05-26T09:30:00Z,3600,10250.50,,9800,640.25,," +
            "151,172,,,,,,,test,\"line1\nline2\""
        assertEquals(
            expectedHeader + "\r\n" + expectedRow + "\r\n",
            output.toString(Charsets.UTF_8.name()),
        )
    }

    @Test fun `csv average heart rate falls back to the samples`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutCsv(
            workout = workout(averageHeartRateBpm = null),
            heartRateSamples = heartRateSamples(),
            output = output,
        )

        val row = output.toString(Charsets.UTF_8.name()).trim().lines().last()
        // (140 + 172) / 2 = 156
        assertTrue(row.contains(",156,172,"))
    }

    @Test fun `fit export round-trips through the app's own parser without a route`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutFit(
            workout = workout(),
            heartRateSamples = heartRateSamples(),
            output = output,
        )

        val parsed = FitRouteParser.parse(output.toByteArray(), fileName = "morning-run.fit")
        assertTrue(parsed.points.isEmpty())
        assertEquals(Instant.parse("2026-05-26T08:30:00Z"), parsed.startTime)
        assertEquals(3600L, parsed.durationSeconds)
        assertEquals(10250.5, parsed.distanceMeters, 0.001)
        assertEquals(640.0, parsed.totalCaloriesKcal!!, 0.001)
        assertEquals("running", parsed.type)
        assertEquals("Morning run", parsed.name)
        val heartRates = parsed.bleSamples.heartRateSamples
        assertEquals(listOf(140L, 172L), heartRates.map { it.beatsPerMinute })
        assertEquals(Instant.parse("2026-05-26T08:40:00Z"), heartRates.first().time)
    }

    @Test fun `fit container is framed with real CRCs`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutFit(
            workout = workout(),
            heartRateSamples = heartRateSamples(),
            output = output,
        )

        // The app's own decoder never verifies CRCs, but Garmin-class
        // importers do — the framing has to hold up outside this codebase.
        val bytes = output.toByteArray()
        assertEquals(14, bytes[0].toInt())
        assertEquals(".FIT", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals(FitCrc.compute(bytes, offset = 0, length = 12), bytes.uint16At(12))
        assertEquals(
            FitCrc.compute(bytes, offset = 0, length = bytes.size - 2),
            bytes.uint16At(bytes.size - 2),
        )
    }

    @Test fun `fit without heart rate writes no record messages at all`() {
        val output = ByteArrayOutputStream()

        writeActivityWorkoutFit(
            workout = workout(),
            heartRateSamples = emptyList(),
            output = output,
        )

        val parsed = FitRouteParser.parse(output.toByteArray())
        assertTrue(parsed.bleSamples.heartRateSamples.isEmpty())
        // Still a complete activity to the app's own parser.
        assertEquals(3600L, parsed.durationSeconds)
    }

    @Test fun `fit drops samples outside the session and zero readings`() {
        val output = ByteArrayOutputStream()
        val samples = heartRateSamples() + listOf(
            sample("2026-05-26T07:00:00Z", 130), // before the session
            sample("2026-05-26T10:00:00Z", 120), // after it
            sample("2026-05-26T08:45:00Z", 0), // sensor dropout, not a heart
        )

        writeActivityWorkoutFit(
            workout = workout(),
            heartRateSamples = samples,
            output = output,
        )

        val parsed = FitRouteParser.parse(output.toByteArray())
        assertEquals(listOf(140L, 172L), parsed.bleSamples.heartRateSamples.map { it.beatsPerMinute })
    }

    @Test fun `fit sport mapping survives the round trip`() {
        assertEquals("treadmill", fitTypeFor(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertEquals("strength training", fitTypeFor(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING))
        // Outside the FIT vocabulary: generic sport, and generic says nothing.
        assertEquals(null, fitTypeFor(ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON))
    }

    @Test fun `fit absent metrics come back absent`() {
        val output = ByteArrayOutputStream()
        val bare = workout().copy(
            totalDistanceMeters = null,
            totalCaloriesKcal = null,
            elevationGainedMeters = null,
        )

        writeActivityWorkoutFit(bare, heartRateSamples = emptyList(), output = output)

        val parsed = FitRouteParser.parse(output.toByteArray())
        assertEquals(0.0, parsed.distanceMeters, 0.001)
        assertEquals(null, parsed.totalCaloriesKcal)
        assertEquals(0.0, parsed.elevationGainedMeters, 0.001)
        assertEquals(3600L, parsed.durationSeconds)
    }

    @Test fun `workout export file names use selected format extension`() {
        val tcxName = workout(title = "Morning Run!").workoutExportFileName(ActivityWorkoutExportFormat.TCX)
        val csvName = workout(title = "Morning Run!").workoutExportFileName(ActivityWorkoutExportFormat.CSV)
        val fitName = workout(title = "Morning Run!").workoutExportFileName(ActivityWorkoutExportFormat.FIT)

        assertTrue(tcxName.startsWith("morning-run-"))
        assertTrue(tcxName.endsWith(".tcx"))
        assertTrue(csvName.startsWith("morning-run-"))
        assertTrue(csvName.endsWith(".csv"))
        assertTrue(fitName.startsWith("morning-run-"))
        assertTrue(fitName.endsWith(".fit"))
    }

    @Test fun `blank title falls back to workout`() {
        val name = workout(title = "  !!! ").workoutExportFileName(ActivityWorkoutExportFormat.CSV)

        assertTrue(name.startsWith("workout-"))
    }

    private fun workout(
        title: String? = "Morning run",
        notes: String? = null,
        exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        averageHeartRateBpm: Long? = null,
    ) = ExerciseData(
        id = "activity-1",
        title = title,
        exerciseType = exerciseType,
        startTime = Instant.parse("2026-05-26T08:30:00Z"),
        endTime = Instant.parse("2026-05-26T09:30:00Z"),
        durationMs = 3_600_000,
        source = "test",
        totalDistanceMeters = 10250.5,
        totalCaloriesKcal = 640.25,
        steps = 9_800,
        averageHeartRateBpm = averageHeartRateBpm,
        notes = notes,
    )

    private fun heartRateSamples() = listOf(
        sample("2026-05-26T08:40:00Z", 140),
        sample("2026-05-26T08:50:00Z", 172),
    )

    private fun fitTypeFor(exerciseType: Int): String? {
        val output = ByteArrayOutputStream()
        writeActivityWorkoutFit(
            workout = workout(exerciseType = exerciseType),
            heartRateSamples = emptyList(),
            output = output,
        )
        return FitRouteParser.parse(output.toByteArray()).type
    }

    private fun ByteArray.uint16At(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun sample(time: String, beatsPerMinute: Long) = HeartRateSample(
        time = Instant.parse(time),
        beatsPerMinute = beatsPerMinute,
        source = "test",
    )
}
