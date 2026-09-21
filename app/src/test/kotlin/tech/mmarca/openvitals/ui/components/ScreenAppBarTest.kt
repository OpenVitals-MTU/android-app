package tech.mmarca.openvitals.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app bar reads what the screen on the current destination declared. Both screens are
 * composed during a navigation animation, so a declaration must never reach the other one.
 */
class ScreenAppBarTest {

    private val state = AppBarState()
    private val dashboard = Any()
    private val settings = Any()

    private fun appBar(title: String) = ScreenAppBar(
        title = title,
        actions = listOf(
            AppBarAction(
                icon = Icons.Outlined.Check,
                contentDescription = 0,
                onClick = {},
            ),
        ),
    )

    @Test
    fun `a screen that declared nothing has nothing`() {
        assertNull(state.of(dashboard))
        assertNull(state.of(null))
    }

    @Test
    fun `a declaration is read back for its own destination`() {
        val declared = appBar("Dashboard")

        state.declare(dashboard, declared)

        assertEquals(declared, state.of(dashboard))
    }

    @Test
    fun `the screen being left cannot lend its chrome to the one arriving`() {
        state.declare(dashboard, appBar("Dashboard"))

        // The arriving screen declares nothing of its own.
        assertNull(state.of(settings))
    }

    @Test
    fun `a screen replaces its own declaration`() {
        state.declare(dashboard, appBar("First"))
        state.declare(dashboard, appBar("Second"))

        assertEquals("Second", state.of(dashboard)?.title)
    }

    @Test
    fun `leaving withdraws only that screen's declaration`() {
        state.declare(dashboard, appBar("Dashboard"))
        state.declare(settings, appBar("Settings"))

        state.withdraw(dashboard)

        assertNull(state.of(dashboard))
        assertEquals("Settings", state.of(settings)?.title)
    }

    @Test
    fun `declaring the same chrome again is not a change`() {
        // A screen recomposes often. The app bar must not recompose with it.
        state.declare(dashboard, appBar("Dashboard"))
        val changes = mutableListOf<Any>()
        val registration = Snapshot.registerApplyObserver { changed, _ -> changes.addAll(changed) }

        try {
            Snapshot.withMutableSnapshot { state.declare(dashboard, appBar("Dashboard")) }
            assertTrue("an equal declaration was recorded as a change: $changes", changes.isEmpty())

            Snapshot.withMutableSnapshot { state.declare(dashboard, appBar("Renamed")) }
            assertTrue("a new declaration was not recorded as a change", changes.isNotEmpty())
        } finally {
            registration.dispose()
        }
    }
}
