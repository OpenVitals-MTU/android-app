package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

class CaffeineDrinkProfileTest {

    private val preferences = CaffeinePreferences()
    private val start = Instant.parse("2026-07-01T08:00:00Z")

    private fun entry(caffeineMg: Double, id: String = "drink"): CaffeineEntry =
        CaffeineEntry(
            id = id,
            startTime = start,
            endTime = start.plusSeconds(10 * 60L),
            caffeineMg = caffeineMg,
            name = "Coffee",
            source = "test.source",
            mealType = 0,
        )

    private fun profileOf(caffeineMg: Double, now: Instant = start.plusSeconds(3600)) =
        caffeineDrinkProfile(entry = entry(caffeineMg), now = now, preferences = preferences)

    @Test
    fun `curve spans the horizon at the sampling step`() {
        val profile = profileOf(100.0)

        assertEquals(start, profile.curve.first().time)
        assertEquals(start.plus(CaffeineProfileHorizon), profile.curve.last().time)
        assertEquals(
            CaffeineProfileHorizon.toMinutes() / CaffeineProfileStep.toMinutes() + 1,
            profile.curve.size.toLong(),
        )
    }

    @Test
    fun `peak is below the dose and is the highest point of the curve`() {
        val profile = profileOf(100.0)

        assertTrue(profile.peakMg > 0.0)
        // Elimination starts before absorption finishes, so a 100mg coffee never puts 100mg in you at once.
        assertTrue(profile.peakMg < 100.0)
        assertEquals(profile.curve.maxOf { it.valueMg }, profile.peakMg, 0.0001)
        assertEquals(profile.curve.first { it.valueMg == profile.peakMg }.time, profile.peakTime)
        assertTrue(profile.peakTime.isAfter(start))
    }

    @Test
    fun `half gone and gone are read after the peak`() {
        val profile = profileOf(100.0)

        assertNotNull(profile.halfGoneTime)
        assertNotNull(profile.goneTime)
        // The rise crosses both thresholds on its way up; neither may be reported before the peak.
        assertFalse(profile.halfGoneTime!!.isBefore(profile.peakTime))
        assertFalse(profile.goneTime!!.isBefore(profile.peakTime))
        assertTrue(profile.goneTime!!.isAfter(profile.halfGoneTime))
    }

    @Test
    fun `half gone marks the first point under half the peak`() {
        val profile = profileOf(100.0)
        val halfGone = profile.curve.first { it.time == profile.halfGoneTime }
        val justBefore = profile.curve.last { it.time.isBefore(profile.halfGoneTime) }

        assertTrue(halfGone.valueMg < profile.peakMg / 2.0)
        assertTrue(justBefore.valueMg >= profile.peakMg / 2.0)
    }

    @Test
    fun `a dose that never fades within the horizon reports no gone time`() {
        // 5000mg still carries far more than the negligible threshold 36 hours later.
        val profile = profileOf(5_000.0)

        assertNotNull(profile.halfGoneTime)
        assertNull(profile.goneTime)
    }

    @Test
    fun `a zero dose has no half to be gone and is over before it started`() {
        val profile = profileOf(0.0)

        assertEquals(0.0, profile.peakMg, 0.0001)
        // Half of nothing is not a threshold anything can fall below.
        assertNull(profile.halfGoneTime)
        assertEquals(start, profile.goneTime)
        assertFalse(profile.isActive)
    }

    @Test
    fun `isActive follows the negligible threshold`() {
        assertFalse(profileOf(100.0, now = start.minusSeconds(60)).isActive)
        assertTrue(profileOf(100.0, now = start.plusSeconds(3600)).isActive)
        // A day and a half on, a single coffee is finished.
        assertFalse(profileOf(100.0, now = start.plus(CaffeineProfileHorizon)).isActive)

        val fading = profileOf(100.0, now = start.plus(CaffeineProfileHorizon))
        assertTrue(fading.currentMg < CaffeineNegligibleMg)
    }

    @Test
    fun `shared peak is the largest of the profiles`() {
        val small = profileOf(50.0)
        val large = profileOf(200.0)

        assertEquals(large.peakMg, caffeineProfilePeak(listOf(small, large)), 0.0001)
        assertEquals(0.0, caffeineProfilePeak(emptyList()), 0.0001)
    }

    @Test
    fun `the profile agrees with the model the day curve is built from`() {
        val drink = entry(120.0)
        val at = start.plusSeconds(90 * 60L)
        val profile = caffeineDrinkProfile(entry = drink, now = at, preferences = preferences)

        assertEquals(
            CaffeineInsightCalculator.contributionMg(drink, at, preferences.normalized()),
            profile.currentMg,
            0.0001,
        )
    }
}
