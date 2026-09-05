package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.features.manualentry.activity.ActivityRecordingSensor

/** A sensor kind with no Android type never reaches SensorManager. */
class ActivityRecordingSensorTypeTest {

    @Test
    fun `each hardware sensor kind maps to its Android sensor type`() {
        assertEquals(Sensor.TYPE_PROXIMITY, ActivityRecordingSensor.PROXIMITY.toAndroidSensorType())
        assertEquals(
            Sensor.TYPE_ACCELEROMETER,
            ActivityRecordingSensor.ACCELEROMETER.toAndroidSensorType(),
        )
        assertEquals(
            Sensor.TYPE_STEP_DETECTOR,
            ActivityRecordingSensor.STEP_DETECTOR.toAndroidSensorType(),
        )
    }

    @Test
    fun `sensor kinds with no Android sensor type are null without asking`() {
        assertNull(ActivityRecordingSensor.GPS.toAndroidSensorType())
        assertNull(ActivityRecordingSensor.BLE.toAndroidSensorType())
        assertNull(ActivityRecordingSensor.NONE.toAndroidSensorType())
    }
}
