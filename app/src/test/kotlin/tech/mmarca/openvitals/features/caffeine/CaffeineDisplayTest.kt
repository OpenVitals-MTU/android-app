package tech.mmarca.openvitals.features.caffeine

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CaffeineDistributionSlice
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.model.CaffeineTimeBucket
import tech.mmarca.openvitals.domain.model.CaffeineTimeOfDayBucket

/** The caffeine cards' derivations as pure functions: the sleep verdict, the top six slices, the curve's axis maximum. */
class CaffeineDisplayTest {

    private val morning: Instant =
        LocalDateTime.of(2026, 3, 2, 8, 0).atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun `an empty load leaves the bars empty and the scale non-zero`() {
        val home = CaffeineInsights()
        val analytics = CaffeineInsights()

        assertEquals(CaffeineSleepImpactStatus.UNLIKELY, caffeineSleepImpactStatus(home))
        assertTrue(home.entryInsights.isEmpty())
        // With no threshold and a flat curve the scale floors at 1 mg, probed with the smallest curve the chart draws.
        assertEquals(
            1.0,
            caffeineCurveMaxMg(
                points = listOf(
                    CaffeinePoint(time = morning, valueMg = 0.0),
                    CaffeinePoint(time = morning.plusSeconds(3600), valueMg = 0.0),
                ),
                thresholdMg = 0.0,
            ),
            0.0001,
        )
        assertTrue(caffeineDistributionBars(analytics.sourceTotals).isEmpty())
        assertNull(analytics.sourceTotals.firstOrNull()?.label)
    }

    @Test
    fun `the sleep verdict compares bedtime first, then right now`() {
        assertEquals(
            CaffeineSleepImpactStatus.MAY_AFFECT_SLEEP,
            statusFor(currentMg = 120.0, bedtimeMg = 80.0),
        )
        // Over the line now, but back under it by bedtime.
        assertEquals(
            CaffeineSleepImpactStatus.ELEVATED_NOW,
            statusFor(currentMg = 120.0, bedtimeMg = 30.0),
        )
        assertEquals(
            CaffeineSleepImpactStatus.UNLIKELY,
            statusFor(currentMg = 20.0, bedtimeMg = 5.0),
        )
    }

    @Test
    fun `the bedtime card is safe exactly at the threshold`() {
        val insights = CaffeineInsights(bedtimeMg = 50.0, sleepThresholdMg = 50)

        assertTrue(caffeineBedtimeIsSafe(insights))
        // …and the verdict agrees: 50 is not ABOVE 50.
        assertEquals(CaffeineSleepImpactStatus.UNLIKELY, caffeineSleepImpactStatus(insights))
    }

    @Test
    fun `the curve maximum fits the tallest point and the threshold`() {
        val maxMg = caffeineCurveMaxMg(
            points = listOf(
                CaffeinePoint(time = morning, valueMg = 90.0),
                CaffeinePoint(time = morning.plusSeconds(2 * 3600), valueMg = 140.0),
            ),
            thresholdMg = 50.0,
        )

        assertEquals(140.0, maxMg, 0.0001)
    }

    @Test
    fun `a distribution card shows six bars, scaled against their own tallest`() {
        val sourceTotals = listOf(
            slice("Coffee", 400.0),
            slice("Tea", 200.0),
            slice("Cola", 100.0),
            slice("Chocolate", 50.0),
            slice("Energy drink", 40.0),
            slice("Matcha", 30.0),
            // The seventh does not fit on the card.
            slice("Yerba mate", 20.0),
        )

        val bars = caffeineDistributionBars(sourceTotals)
        assertEquals(6, bars.size)
        assertEquals("Coffee", bars.first().label)
        assertEquals(1.0, bars.first().fraction, 0.0001)
        assertEquals(0.5, bars[1].fraction, 0.0001)
        assertEquals("Coffee", sourceTotals.firstOrNull()?.label)

        val buckets = caffeineTimeBucketBars(
            listOf(
                CaffeineTimeBucket(bucket = CaffeineTimeOfDayBucket.MORNING, valueMg = 300.0),
                CaffeineTimeBucket(bucket = CaffeineTimeOfDayBucket.EVENING, valueMg = 150.0),
            )
        )
        assertEquals(CaffeineTimeOfDayBucket.MORNING, buckets.first().bucket)
        assertEquals(1.0, buckets.first().fraction, 0.0001)
        assertEquals(0.5, buckets.last().fraction, 0.0001)
    }

    private fun statusFor(currentMg: Double, bedtimeMg: Double): CaffeineSleepImpactStatus =
        caffeineSleepImpactStatus(
            CaffeineInsights(
                currentMg = currentMg,
                bedtimeMg = bedtimeMg,
                sleepThresholdMg = 50,
                bedtime = LocalTime.of(23, 0),
            )
        )

    private fun slice(label: String, mg: Double): CaffeineDistributionSlice =
        CaffeineDistributionSlice(label = label, valueMg = mg)
}
