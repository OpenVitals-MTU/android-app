package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.cycle.HormonalContraception
import tech.mmarca.openvitals.domain.cycle.HormonalContraceptionKind
import tech.mmarca.openvitals.domain.model.PillPlan

/** The hormonal method survives a restart, and clearing the cycle settings forgets it. */
class CyclePreferencesRepositoryTest {

    private fun repository(prefs: SharedPreferences) = CyclePreferencesRepository(
        mockk<Context> {
            every { getSharedPreferences("openvitals_cycle_preferences", Context.MODE_PRIVATE) } returns prefs
        },
    )

    @Test
    fun `the hormonal method and its stop date round-trip through a fresh instance`() {
        val prefs = FakeSharedPreferences()
        val profile = CycleTrackingProfile(
            ageBand = AgeBand.AGE_25_29,
            hormonalContraception = HormonalContraception(HormonalContraceptionKind.HORMONAL_IUD, LocalDate.of(2026, 8, 1)),
        )

        repository(prefs).setCycleTrackingProfile(profile)

        assertEquals(profile, repository(prefs).cycleTrackingProfile())
    }

    @Test
    fun `choosing none forgets the stop date`() {
        val prefs = FakeSharedPreferences()
        val repository = repository(prefs)
        repository.setCycleTrackingProfile(
            CycleTrackingProfile(hormonalContraception = HormonalContraception(HormonalContraceptionKind.PILL, LocalDate.of(2026, 8, 1))),
        )

        repository.setCycleTrackingProfile(CycleTrackingProfile())

        assertEquals(CycleTrackingProfile(), repository(prefs).cycleTrackingProfile())
    }

    @Test
    fun `a stored kind this build does not know reads as none`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("hormonal_contraception_kind", "from_a_newer_build").putString("hormonal_contraception_stopped_on", "garbage").commit()

        assertEquals(CycleTrackingProfile(), repository(prefs).cycleTrackingProfile())
    }

    @Test
    fun `clearing the cycle preferences erases the hormonal method`() {
        val prefs = FakeSharedPreferences()
        val repository = repository(prefs)
        repository.setCycleTrackingProfile(
            CycleTrackingProfile(hormonalContraception = HormonalContraception(HormonalContraceptionKind.IMPLANT)),
        )

        repository.clearCyclePreferences()

        assertEquals(CycleTrackingProfile(), repository(prefs).cycleTrackingProfile())
    }

    @Test
    fun `an enabled pill scheme reads as the pill even when no method was ever stored`() {
        val prefs = FakeSharedPreferences()
        val repository = repository(prefs)
        repository.setCycleTrackingProfile(
            CycleTrackingProfile(hormonalContraception = HormonalContraception(HormonalContraceptionKind.IMPLANT, LocalDate.of(2026, 8, 1))),
        )

        repository.setPillPlan(PillPlan(enabled = true, packStart = LocalDate.of(2026, 9, 1), updatedAt = Instant.parse("2026-09-01T08:00:00Z")))

        val expected = HormonalContraception(HormonalContraceptionKind.PILL)
        assertEquals(expected, repository.cycleTrackingProfile().hormonalContraception)
        assertEquals(expected, repository(prefs).cycleTrackingProfile().hormonalContraception)
    }

    @Test
    fun `a pill scheme stored by an older build reads as the pill`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putBoolean("pill_enabled", true).commit()

        assertEquals(HormonalContraceptionKind.PILL, repository(prefs).cycleTrackingProfile().hormonalContraception.kind)
    }
}
