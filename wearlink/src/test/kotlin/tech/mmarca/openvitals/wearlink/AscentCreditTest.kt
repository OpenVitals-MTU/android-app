package tech.mmarca.openvitals.wearlink

import org.junit.Assert.assertEquals
import org.junit.Test

class AscentCreditTest {

    @Test
    fun `ten steps carry one floor's height, and no more than was climbed`() {
        assertEquals(6.0, AscentCredit.creditedMetres(climbedMetres = 15.0, steps = 20), 1e-9)
        assertEquals(4.0, AscentCredit.creditedMetres(climbedMetres = 4.0, steps = 60), 1e-9)
        assertEquals(0.0, AscentCredit.creditedMetres(climbedMetres = 9.0, steps = 0), 1e-9)
    }
}
