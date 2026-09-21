package tech.mmarca.openvitals.features.manualentry.activity.recording

import org.junit.Assert.assertEquals
import org.junit.Test

/** The wake lock costs battery, so the rule for holding it is written down and pinned. */
class RecordingWakeLockRuleTest {

    @Test
    fun `the CPU is kept awake while recording and during a timed rest, and at no other time`() {
        val awake = ActivityRecordingStatus.entries.filter { it.needsCpuAwake() }.toSet()

        assertEquals(setOf(ActivityRecordingStatus.RECORDING, ActivityRecordingStatus.RESTING), awake)
    }
}
