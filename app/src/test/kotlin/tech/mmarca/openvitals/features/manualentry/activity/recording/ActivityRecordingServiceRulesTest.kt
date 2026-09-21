package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRepetitionUnit

/** The two decision tables of the recording service. They were private members with no test. */
class ActivityRecordingServiceRulesTest {

    private val run = ActivityEntryType(exerciseType = 56, labelRes = 0, repetitionUnit = ActivityRepetitionUnit.STEPS)
    private val ride = ActivityEntryType(exerciseType = 8, labelRes = 0)
    private val yoga = ActivityEntryType(exerciseType = 83, labelRes = 0, supportsGpsRoute = false)

    private fun plan(location: Boolean, pressure: Boolean, sensor: RecordingSensorUse) =
        RecordingSensorPlan(location = location, pressure = pressure, sensor = sensor)

    private val off = plan(location = false, pressure = false, sensor = RecordingSensorUse.OFF)

    @Test
    fun `nothing runs unless the state is recording`() {
        ActivityRecordingStatus.entries
            .filter { it != ActivityRecordingStatus.RECORDING }
            .forEach { status ->
                ActivityRecordingKind.entries.forEach { kind ->
                    assertEquals("$status $kind", off, recordingSensorPlan(status, kind, run))
                }
            }
    }

    @Test
    fun `a GPS recording reads position and pressure, and steps for a type that counts them`() {
        val recording = ActivityRecordingStatus.RECORDING
        val gps = ActivityRecordingKind.GPS_ROUTE

        assertEquals(plan(true, true, RecordingSensorUse.STEPS), recordingSensorPlan(recording, gps, run))
        assertEquals(plan(true, true, RecordingSensorUse.OFF), recordingSensorPlan(recording, gps, ride))
        assertEquals(plan(true, true, RecordingSensorUse.OFF), recordingSensorPlan(recording, gps, null))
    }

    @Test
    fun `a repetition recording runs a recognizer and nothing else`() {
        val expected = plan(false, false, RecordingSensorUse.PER_STEP_RECOGNIZER)

        listOf(run, ride, yoga, null).forEach { type ->
            assertEquals(
                expected,
                recordingSensorPlan(ActivityRecordingStatus.RECORDING, ActivityRecordingKind.REPETITION, type),
            )
        }
    }

    @Test
    fun `a timed recording keeps pressure and steps only for a type that could have used GPS`() {
        val recording = ActivityRecordingStatus.RECORDING
        val timed = ActivityRecordingKind.TIMED

        // A run recorded without GPS still climbs stairs and takes steps.
        assertEquals(plan(false, true, RecordingSensorUse.STEPS), recordingSensorPlan(recording, timed, run))
        assertEquals(plan(false, true, RecordingSensorUse.OFF), recordingSensorPlan(recording, timed, ride))
        assertEquals(off, recordingSensorPlan(recording, timed, yoga))
        assertEquals(off, recordingSensorPlan(recording, timed, null))
    }

    @Test
    fun `the foreground service type follows the kind, the sensors and the Android version`() {
        val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        val health = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        val device = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        val gps = ActivityRecordingKind.GPS_ROUTE
        val reps = ActivityRecordingKind.REPETITION
        val timed = ActivityRecordingKind.TIMED

        // kind, counts steps, BLE sensors, SDK, expected.
        val table = listOf(
            // Before Android 10 a service has no type.
            Case(gps, countsSteps = true, ble = true, sdk = 28, expected = 0),
            // Android 10 to 13: location only. The other two types do not exist yet.
            Case(gps, countsSteps = true, ble = true, sdk = 29, expected = location),
            Case(gps, countsSteps = true, ble = true, sdk = 33, expected = location),
            Case(reps, countsSteps = false, ble = true, sdk = 33, expected = 0),
            Case(timed, countsSteps = true, ble = false, sdk = 33, expected = 0),
            // Android 14: one bit per thing in use.
            Case(gps, countsSteps = false, ble = false, sdk = 34, expected = location),
            Case(gps, countsSteps = true, ble = false, sdk = 34, expected = location or health),
            Case(gps, countsSteps = false, ble = true, sdk = 34, expected = location or device),
            Case(gps, countsSteps = true, ble = true, sdk = 34, expected = location or health or device),
            Case(reps, countsSteps = false, ble = false, sdk = 34, expected = health),
            Case(reps, countsSteps = false, ble = true, sdk = 34, expected = health or device),
            Case(timed, countsSteps = true, ble = false, sdk = 34, expected = health),
            Case(timed, countsSteps = false, ble = true, sdk = 35, expected = health or device),
        )

        table.forEach { case ->
            assertEquals(
                case.toString(),
                case.expected,
                recordingForegroundServiceType(case.kind, case.countsSteps, case.ble, case.sdk),
            )
        }
    }

    private data class Case(
        val kind: ActivityRecordingKind,
        val countsSteps: Boolean,
        val ble: Boolean,
        val sdk: Int,
        val expected: Int,
    )
}
