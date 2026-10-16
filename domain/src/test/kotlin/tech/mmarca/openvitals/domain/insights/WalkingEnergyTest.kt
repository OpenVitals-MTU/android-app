package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingEnergyTest {

    @Test
    fun `Moore's simple equation gives about 3_2 METs at 100 steps a minute`() {
        // 1.811 + 2.014 + 7.427 = 11.252 ml/kg/min, as the paper's worked value.
        assertEquals(11.252, WalkingEnergy.oxygenUptake(100), 0.001)
    }

    @Test
    fun `the active energy is the cost above rest`() {
        // 11.252 / 3.5 − 1 = 2.215 METs above rest; × 70 kg / 60 = 2.58 kcal for the minute.
        assertEquals(2.584, WalkingEnergy.activeKcalPerMinute(100, 70.0), 0.001)
    }

    @Test
    fun `a minute too slow to cost more than rest adds nothing`() {
        assertEquals(0.0, WalkingEnergy.activeKcalPerMinute(20, 70.0), 0.0)
        assertEquals(0.0, WalkingEnergy.activeKcalPerMinute(0, 70.0), 0.0)
        assertEquals(0.0, WalkingEnergy.activeKcalPerMinute(100, 0.0), 0.0)
    }
}
