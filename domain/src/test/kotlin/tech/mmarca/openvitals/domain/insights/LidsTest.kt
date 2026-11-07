package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LidsTest {

    @Test
    fun `stillness is one hundred, movement pulls it down, and the scale divides first`() {
        val lids = Lids.of(floatArrayOf(0f, 1f, 4f, 99f), scale = 1f, smoothMinutes = 1)

        assertEquals(100f, lids[0], 0f)
        assertEquals(50f, lids[1], 0f)
        assertEquals(20f, lids[2], 0f)
        assertEquals(1f, lids[3], 0f)
        assertEquals(50f, Lids.of(floatArrayOf(0.2f), scale = 0.2f, smoothMinutes = 1)[0], 0f)
    }

    @Test
    fun `the smoothing follows a ninety-minute cycle`() {
        val activity = FloatArray(360) { index -> if ((index / 45) % 2 == 0) 0f else 3f }

        val lids = Lids.of(activity, smoothMinutes = 30)
        val normalised = Lids.normalised(lids)

        assertTrue(lids[22] > lids[67])
        assertTrue(normalised[22] > 0f && normalised[67] < 0f)
        assertTrue(normalised.all { it in -3f..3f })
    }

    @Test
    fun `a flat night normalises to zero and NaN minutes stay out`() {
        val flat = Lids.normalised(Lids.of(FloatArray(100)))
        val gappy = Lids.of(floatArrayOf(Float.NaN, Float.NaN, 0f), smoothMinutes = 1)

        assertTrue(flat.all { it == 0f })
        assertTrue(gappy[0].isNaN())
        assertEquals(100f, gappy[2], 0f)
    }
}
