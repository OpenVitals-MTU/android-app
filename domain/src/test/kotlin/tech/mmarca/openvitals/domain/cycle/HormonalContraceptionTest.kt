package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hormones count while a method runs and for 60 days after it stops (Wang 2025's exclusion window, not a washout). */
class HormonalContraceptionTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun `nothing declared never influences`() {
        assertFalse(HormonalContraception().isInfluencing(today))
        assertFalse(HormonalContraception(HormonalContraceptionKind.NONE, stoppedOn = today.minusDays(3)).isInfluencing(today))
    }

    @Test
    fun `a running method influences with no end`() {
        HormonalContraceptionKind.entries.filter { it != HormonalContraceptionKind.NONE }.forEach { kind ->
            assertTrue(kind.name, HormonalContraception(kind).isInfluencing(today))
        }
    }

    @Test
    fun `the influence ends after 60 days from the stop date`() {
        val stopped = HormonalContraception(HormonalContraceptionKind.IMPLANT, stoppedOn = today.minusDays(60))
        assertTrue(stopped.isInfluencing(today))
        assertFalse(stopped.isInfluencing(today.plusDays(1)))
        assertTrue(HormonalContraception(HormonalContraceptionKind.IMPLANT, stoppedOn = today).isInfluencing(today))
    }

    @Test
    fun `a stop date in the future still counts as running`() {
        assertTrue(HormonalContraception(HormonalContraceptionKind.RING_OR_PATCH, today.plusDays(10)).isInfluencing(today))
    }

    @Test
    fun `the pill scheme sets the pill and drops the stop date`() {
        val profile = CycleTrackingProfile(
            hormonalContraception = HormonalContraception(HormonalContraceptionKind.INJECTION, today.minusDays(5)),
        ).withPillScheme()

        assertEquals(HormonalContraception(HormonalContraceptionKind.PILL), profile.hormonalContraception)
    }

    @Test
    fun `a profile with only a hormonal method is not empty`() {
        assertTrue(CycleTrackingProfile().isEmpty)
        assertFalse(CycleTrackingProfile(hormonalContraception = HormonalContraception(HormonalContraceptionKind.PILL)).isEmpty)
    }
}
