package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class StrideLengthTest {

    @Test
    fun `values inside the range pass through`() {
        assertEquals(0.75, StrideLength.normalize(0.75), 0.0)
    }

    @Test
    fun `values outside the range clamp`() {
        assertEquals(StrideLength.minMeters, StrideLength.normalize(0.1), 0.0)
        assertEquals(StrideLength.maxMeters, StrideLength.normalize(3.0), 0.0)
    }

    @Test
    fun `garbage falls back to the default`() {
        assertEquals(StrideLength.defaultMeters, StrideLength.normalize(Double.NaN), 0.0)
        assertEquals(StrideLength.defaultMeters, StrideLength.normalize(0.0), 0.0)
        assertEquals(StrideLength.defaultMeters, StrideLength.normalize(-1.0), 0.0)
    }
}
