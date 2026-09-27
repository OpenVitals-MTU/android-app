package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** The band follows the body profile's birth year when there is one; the declared band is the fallback. */
class AgeBandTest {
    private val today = LocalDate.of(2026, 9, 27)

    @Test
    fun `each age lands in its band at the boundaries`() {
        assertEquals(AgeBand.UNDER_20, AgeBand.forAge(19))
        assertEquals(AgeBand.AGE_20_24, AgeBand.forAge(20))
        assertEquals(AgeBand.AGE_20_24, AgeBand.forAge(24))
        assertEquals(AgeBand.AGE_25_29, AgeBand.forAge(25))
        assertEquals(AgeBand.AGE_30_34, AgeBand.forAge(34))
        assertEquals(AgeBand.AGE_35_39, AgeBand.forAge(35))
        assertEquals(AgeBand.AGE_40_44, AgeBand.forAge(44))
        assertEquals(AgeBand.AGE_45_49, AgeBand.forAge(49))
        assertEquals(AgeBand.AGE_50_PLUS, AgeBand.forAge(50))
        assertEquals(AgeBand.AGE_50_PLUS, AgeBand.forAge(63))
    }

    @Test
    fun `a birth year in the body profile decides the band`() {
        val declared = CycleTrackingProfile(ageBand = AgeBand.AGE_50_PLUS)

        val resolved = declared.resolved(BodyProfile(birthYear = today.year - 30), today)

        assertEquals(AgeBand.AGE_30_34, resolved.ageBand)
    }

    @Test
    fun `without a birth year the declared band stands`() {
        val declared = CycleTrackingProfile(ageBand = AgeBand.AGE_50_PLUS)

        assertEquals(AgeBand.AGE_50_PLUS, declared.resolved(BodyProfile(), today).ageBand)
        assertEquals(AgeBand.AGE_50_PLUS, declared.resolved(null, today).ageBand)
        assertNull(CycleTrackingProfile().resolved(BodyProfile(), today).ageBand)
    }
}
