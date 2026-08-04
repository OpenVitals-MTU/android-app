package tech.mmarca.openvitals.features.hydration.reminders

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * The quick-add offer and its button labels — the millilitre volumes a
 * reminder notification proposes, and how each one reads per unit system.
 */
class HydrationReminderQuickAddTest {

    @Test
    fun `quick add amounts fall back to a glass and a bottle for a fresh install`() {
        assertEquals(
            listOf(250.0, 500.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = emptyList(),
                lastCustomAmountMilliliters = null,
            ),
        )
    }

    @Test
    fun `quick add amounts offer the last two used sizes, newest first`() {
        assertEquals(
            listOf(350.0, 250.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = listOf(350.0, 250.0),
                lastCustomAmountMilliliters = null,
            ),
        )
    }

    @Test
    fun `quick add amounts pad a single recent with the last custom amount, then defaults`() {
        // No last custom amount: the first default fills the second slot.
        assertEquals(
            listOf(330.0, 250.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = listOf(330.0),
                lastCustomAmountMilliliters = null,
            ),
        )

        // With one (pre-recents installs have it), it wins over the default.
        assertEquals(
            listOf(330.0, 120.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = listOf(330.0),
                lastCustomAmountMilliliters = 120.0,
            ),
        )
    }

    @Test
    fun `quick add amounts never offer the same size twice`() {
        // 250 fills slot one; the duplicate last-custom and the duplicate 250
        // fallback are both skipped, so the bottle default lands in slot two.
        assertEquals(
            listOf(250.0, 500.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = listOf(250.0),
                lastCustomAmountMilliliters = 250.0,
            ),
        )
    }

    @Test
    fun `quick add amounts skip volumes outside the container range`() {
        assertEquals(
            listOf(250.0, 500.0),
            hydrationQuickAddAmountsMilliliters(
                recentAmountsMilliliters = listOf(0.0, -50.0, Double.NaN),
                lastCustomAmountMilliliters = null,
            ),
        )
    }

    @Test
    fun `quick add actions are labelled in millilitres for metric`() {
        val amounts = hydrationQuickAddAmountsMilliliters(
            recentAmountsMilliliters = listOf(350.0, 250.0),
            lastCustomAmountMilliliters = null,
        )

        assertEquals(2, amounts.size)
        assertEquals(
            listOf("350 ml", "250 ml"),
            amounts.map { milliliters ->
                hydrationQuickAddLabel(
                    milliliters = milliliters,
                    unitSystem = UnitSystem.METRIC,
                    unitFormatter = formatter(UnitSystem.METRIC),
                )
            },
        )
    }

    @Test
    fun `quick add actions are labelled in fluid ounces for imperial`() {
        // The volume itself stays in millilitres — the storage unit, not the
        // display unit — so a unit-system change cannot corrupt what a tap logs.
        assertEquals(
            "12 fl oz",
            hydrationQuickAddLabel(
                milliliters = 350.0,
                unitSystem = UnitSystem.IMPERIAL,
                unitFormatter = formatter(UnitSystem.IMPERIAL),
            ),
        )
    }

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
