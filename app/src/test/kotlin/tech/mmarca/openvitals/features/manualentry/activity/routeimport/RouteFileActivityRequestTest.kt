package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityRecordSource
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes

/** A file imported with nobody to review it must reach Health Connect as the file says, once. */
class RouteFileActivityRequestTest {

    private val cycling = DefaultActivityEntryTypes.first {
        it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    }

    private fun ride(startSecond: Int = 17, endSecond: Int = 43): ByteArray = FitTestFiles.activity(
        sport = 2,
        points = listOf(
            FitTestPoint(Instant.parse("2026-05-26T08:30:%02dZ".format(startSecond)), 59.5000, 24.5000, 10.0),
            FitTestPoint(Instant.parse("2026-05-26T09:12:%02dZ".format(endSecond)), 59.5100, 24.5200, 22.0),
        ),
        totalAscentMeters = 12,
    )

    @Test
    fun `the start keeps its seconds and the end is not rounded up to a minute`() {
        val request = RouteFileParser.parseFile(ride()).toImportWriteRequest(cycling, ActivityRecordSource.WATCH)!!

        // Through the entry form this became 08:30:00 to 09:13:00.
        assertEquals(Instant.parse("2026-05-26T08:30:17Z"), request.startTime)
        assertEquals(Instant.parse("2026-05-26T09:12:44Z"), request.endTime)
        assertEquals(request.routePoints.first().time, request.startTime)
    }

    @Test
    fun `the same file gives the same key whatever it is called`() {
        val first = RouteFileParser.parseFile(ride(), fileName = "activity_7.fit")
            .toImportWriteRequest(cycling, ActivityRecordSource.WATCH)!!
        val again = RouteFileParser.parseFile(ride(), fileName = "2026-05-26 ride.fit")
            .toImportWriteRequest(cycling, ActivityRecordSource.FILE)!!
        val other = RouteFileParser.parseFile(ride(endSecond = 44))
            .toImportWriteRequest(cycling, ActivityRecordSource.WATCH)!!

        assertNotNull(first.importKey)
        assertEquals(first.importKey, again.importKey)
        assertNotEquals(first.importKey, other.importKey)
    }

    @Test
    fun `the request says where the file came from`() {
        val parsed = RouteFileParser.parseFile(ride())

        assertEquals(ActivityRecordSource.WATCH, parsed.toImportWriteRequest(cycling, ActivityRecordSource.WATCH)!!.source)
        assertEquals(ActivityRecordSource.FILE, parsed.toImportWriteRequest(cycling, ActivityRecordSource.FILE)!!.source)
    }

    @Test
    fun `a route with no time of its own is left to the entry form`() {
        val untimed = RouteFileParser.parseFile(ride()).copy(hasImportedTimeRange = false)

        assertNull(untimed.toImportWriteRequest(cycling, ActivityRecordSource.FILE))
    }

    @Test
    fun `a file that was not parsed from bytes has no key and is refused`() {
        val keyless = RouteFileParser.parseFile(ride()).copy(contentKey = null)

        // A random id here would bring the duplicates back.
        assertNull(keyless.toImportWriteRequest(cycling, ActivityRecordSource.FILE))
    }
}
