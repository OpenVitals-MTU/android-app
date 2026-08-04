package tech.mmarca.openvitals.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GarminWellnessMetricTest {

    @Test
    fun `storage names match the Flutter drift rows exactly`() {
        // Phase 5 imports the preserved drift file 1:1, so these strings must
        // be byte-identical to what the Flutter build stored.
        val expected = mapOf(
            GarminWellnessMetric.STRESS to "stress",
            GarminWellnessMetric.BODY_ENERGY to "body_energy",
            GarminWellnessMetric.MODERATE_MINUTES to "moderate_minutes",
            GarminWellnessMetric.VIGOROUS_MINUTES to "vigorous_minutes",
            GarminWellnessMetric.RECOVERY_TIME to "recovery_time",
            GarminWellnessMetric.TRAINING_READINESS to "training_readiness",
            GarminWellnessMetric.TRAINING_LOAD_ACUTE to "training_load_acute",
            GarminWellnessMetric.TRAINING_LOAD_CHRONIC to "training_load_chronic",
            GarminWellnessMetric.SLEEP_SCORE to "sleep_score",
            GarminWellnessMetric.SLEEP_AWAKENINGS to "sleep_awakenings",
            GarminWellnessMetric.SLEEP_AWAKE_SECONDS to "sleep_awake_seconds",
            GarminWellnessMetric.SLEEP_PRESSURE to "sleep_pressure",
            GarminWellnessMetric.SLEEP_NEED_NORMAL_MINUTES to "sleep_need_normal_minutes",
            GarminWellnessMetric.SLEEP_NEED_MINUTES to "sleep_need_minutes",
        )
        assertEquals(14, GarminWellnessMetric.entries.size)
        expected.forEach { (metric, storageName) ->
            assertEquals(storageName, metric.storageName)
        }
    }

    @Test
    fun `fromStorage resolves stored names and rejects unknown ones`() {
        GarminWellnessMetric.entries.forEach { metric ->
            assertEquals(metric, GarminWellnessMetric.fromStorage(metric.storageName))
        }
        assertNull(GarminWellnessMetric.fromStorage("STRESS"))
        assertNull(GarminWellnessMetric.fromStorage("unknown_metric"))
    }
}
