package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.R

class ExerciseLabelsTest {

    @Test fun `exerciseTypeLabelRes maps known exercise types and falls back`() {
        assertEquals(R.string.hc_exercise_type_running, exerciseTypeLabelRes(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertEquals(R.string.hc_exercise_type_biking_stationary, exerciseTypeLabelRes(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
        assertEquals(R.string.hc_exercise_type_fallback, exerciseTypeLabelRes(-1))
    }

    @Test fun `exerciseSegmentLabelRes maps known segments and falls back`() {
        assertEquals(R.string.hc_segment_deadlift, exerciseSegmentLabelRes(ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT))
        assertEquals(R.string.hc_segment_running_treadmill, exerciseSegmentLabelRes(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL))
        assertEquals(R.string.hc_segment_unknown, exerciseSegmentLabelRes(-1))
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
