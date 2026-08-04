package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxHeartRateTest {

    @Test
    fun `the trust bar is 150 bpm and 60 above resting`() {
        assertEquals(150, observedMaxHeartRateMinimumBpm)
        assertEquals(60, observedMaxHeartRateRestingDeltaBpm)
    }

    @Test
    fun `an observed max at the 150 floor is trusted when resting is low`() {
        // Resting 55: the resting-delta bar is 115, so the 150 floor is the binding one.
        assertTrue(isObservedMaxHeartRateTrustworthy(150, 55))
    }

    @Test
    fun `an observed max below the 150 floor is not trusted`() {
        assertFalse(isObservedMaxHeartRateTrustworthy(149, 55))
    }

    @Test
    fun `a high resting rate raises the bar above the floor`() {
        // Resting 100: the bar is max(150, 160) = 160.
        assertFalse(isObservedMaxHeartRateTrustworthy(155, 100))
        assertTrue(isObservedMaxHeartRateTrustworthy(160, 100))
    }

    @Test
    fun `exactly on the resting-delta bar counts`() {
        // Resting 95: bar is 155; 155 clears it, 154 does not.
        assertTrue(isObservedMaxHeartRateTrustworthy(155, 95))
        assertFalse(isObservedMaxHeartRateTrustworthy(154, 95))
    }
}
