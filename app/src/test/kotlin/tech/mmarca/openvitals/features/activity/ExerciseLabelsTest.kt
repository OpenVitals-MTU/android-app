package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals("Actively recorded", recordingMethodLabel(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED))
        assertEquals("Automatically recorded", recordingMethodLabel(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED))
        assertEquals("Manual entry", recordingMethodLabel(Metadata.RECORDING_METHOD_MANUAL_ENTRY))
        assertEquals("Unknown", recordingMethodLabel(Metadata.RECORDING_METHOD_UNKNOWN))
        assertEquals("Not available", recordingMethodLabel(null))
    }

    @Test fun `deviceTypeLabel maps known device types and null`() {
        assertEquals("Watch", deviceTypeLabel(Device.TYPE_WATCH))
        assertEquals("Phone", deviceTypeLabel(Device.TYPE_PHONE))
        assertEquals("Fitness band", deviceTypeLabel(Device.TYPE_FITNESS_BAND))
        assertEquals("Unknown", deviceTypeLabel(Device.TYPE_UNKNOWN))
        assertEquals("Not available", deviceTypeLabel(null))
    }
}
