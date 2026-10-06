package tech.mmarca.openvitals.wear

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.wear.features.measure.Measurement
import tech.mmarca.openvitals.wear.health.WearCapabilities

class WearRoutesTest {

    @Test
    fun tilesMayOpenLoggingAndMeasuring_butNothingElse() {
        assertTrue(WearRoutes.isDeepLinkable(WearRoutes.QUICK_LOG))
        assertTrue(WearRoutes.isDeepLinkable(WearRoutes.measurement(Measurement.HEART_RATE)))
        assertFalse(WearRoutes.isDeepLinkable(WearRoutes.RECORDING))
        assertFalse(WearRoutes.isDeepLinkable("measure/ECG"))
    }

    @Test
    fun availableMetrics_offerPhoneAndLoggedMetrics_andOnlyMeasuredSensorOnes() {
        val available = WearCapabilities(probed = true, sensorMetrics = setOf(WearMetric.STEPS)).availableMetrics
        assertTrue(WearMetric.STEPS in available)
        assertTrue(WearMetric.SLEEP in available)
        assertTrue(WearMetric.HYDRATION in available)
        assertFalse(WearMetric.HEART_RATE in available)
        assertFalse(WearMetric.BLOOD_OXYGEN in available)
    }
}
