package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.R

class ExerciseLabelsTest {

    @Test fun `exerciseTypeLabel maps known exercise types and falls back`() {
        assertEquals("Running", exerciseTypeLabel(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertEquals("Biking (stationary)", exerciseTypeLabel(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
        assertEquals("Exercise", exerciseTypeLabel(-1))
    }

    @Test fun `exerciseSegmentLabel maps known segments and falls back`() {
        assertEquals("Deadlift", exerciseSegmentLabel(ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT))
        assertEquals("Running (treadmill)", exerciseSegmentLabel(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL))
        assertEquals("Unknown", exerciseSegmentLabel(-1))
    }

    @Test fun `recordingMethodLabel maps known methods and null`() {
        assertEquals(R.string.recording_actively_recorded, recordingMethodLabelRes(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED))
        assertEquals(R.string.recording_automatically_recorded, recordingMethodLabelRes(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED))
        assertEquals(R.string.recording_manual_entry, recordingMethodLabelRes(Metadata.RECORDING_METHOD_MANUAL_ENTRY))
        assertEquals(R.string.recording_unknown, recordingMethodLabelRes(Metadata.RECORDING_METHOD_UNKNOWN))
        assertEquals(R.string.not_available, recordingMethodLabelRes(null))
    }

    @Test fun `deviceTypeLabel maps known device types and null`() {
        assertEquals(R.string.device_watch, deviceTypeLabelRes(Device.TYPE_WATCH))
        assertEquals(R.string.device_phone, deviceTypeLabelRes(Device.TYPE_PHONE))
        assertEquals(R.string.device_fitness_band, deviceTypeLabelRes(Device.TYPE_FITNESS_BAND))
        assertEquals(R.string.recording_unknown, deviceTypeLabelRes(Device.TYPE_UNKNOWN))
        assertEquals(R.string.not_available, deviceTypeLabelRes(null))
    }
}
