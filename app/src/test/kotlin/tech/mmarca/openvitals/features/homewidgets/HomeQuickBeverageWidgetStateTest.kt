package tech.mmarca.openvitals.features.homewidgets

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.navigation.Screen

/** The cached payload a quick-beverage refresh re-pushes; a refresh usually runs with no repository behind it. */
class HomeQuickBeverageWidgetStateTest {
    private val context = stringResourceContext()

    @Before
    fun setUp() {
        mockUriCodec()
    }

    @After
    fun tearDown() {
        unmockUriCodec()
    }

    @Test
    fun `re-pushes a configured instance from its cached payload`() {
        val snapshot = HomeQuickBeverageSnapshot(
            drinkId = "espresso",
            title = "Espresso",
            amount = "30ml",
            subtitle = "Tap to log",
            route = Screen.HydrationEntryLogDrink.createRoute("espresso"),
        )

        val preferences = mutablePreferencesOf()
        preferences.putQuickBeverageSnapshot(snapshot)

        assertEquals(snapshot, preferences.toQuickBeverageSnapshot(context))
    }

    @Test
    fun `leaves an unconfigured instance on its native state`() {
        // No title written means nothing has ever been pushed: the widget keeps its not-configured rendering.
        assertNull(mutablePreferencesOf().toQuickBeverageSnapshot(context))
        assertNull(
            mutablePreferencesOf(HomeQuickBeverageWidgetState.drinkIdKey to "espresso")
                .toQuickBeverageSnapshot(context),
        )
    }

    @Test
    fun `a half-written payload still reads back with usable defaults`() {
        val restored = mutablePreferencesOf(HomeQuickBeverageWidgetState.titleKey to "Espresso")
            .toQuickBeverageSnapshot(context)

        assertEquals("", restored?.drinkId)
        assertEquals("--", restored?.amount)
        assertEquals("", restored?.subtitle)
        assertEquals(Screen.HydrationEntry.route, restored?.route)
    }
}
