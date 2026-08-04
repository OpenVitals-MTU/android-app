package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Which receiver a quick-beverage instance is redrawn through.
 *
 * The 2x1 and the 1x1 share one storage namespace and one log action, so the
 * instance's own provider is the only thing that says which of them to push —
 * pick the wrong one and the tap updates a widget that is not on screen.
 */
class HomeQuickBeverageWidgetReceiverTest {
    private val appWidgetManager = mockk<AppWidgetManager>()
    private val context = mockk<Context>().also { context ->
        every { context.applicationContext } returns context
    }

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
    }

    @After
    fun tearDown() {
        unmockkStatic(AppWidgetManager::class)
    }

    @Test
    fun `a one-tap instance is redrawn through the one-tap receiver`() {
        placed(appWidgetId = 11, providerClass = HomeQuickBeverageOneTapWidgetReceiver::class.java.name)

        assertTrue(isQuickBeverageOneTapWidget(context, 11))
        assertEquals(
            HomeQuickBeverageOneTapWidgetReceiver::class.java,
            quickBeverageWidgetReceiverClassForAppWidgetId(context, 11),
        )
    }

    @Test
    fun `redraws the 2x1 receiver when the 2x1 owns the instance`() {
        placed(appWidgetId = 12, providerClass = HomeQuickBeverageWidgetReceiver::class.java.name)

        assertFalse(isQuickBeverageOneTapWidget(context, 12))
        assertEquals(
            HomeQuickBeverageWidgetReceiver::class.java,
            quickBeverageWidgetReceiverClassForAppWidgetId(context, 12),
        )
    }

    @Test
    fun `an appWidgetId belonging to no placed widget falls back to the 2x1`() {
        every { appWidgetManager.getAppWidgetInfo(13) } returns null

        assertFalse(hasAppWidgetInfo(context, 13))
        assertFalse(isQuickBeverageOneTapWidget(context, 13))
        assertEquals(
            HomeQuickBeverageWidgetReceiver::class.java,
            quickBeverageWidgetReceiverClassForAppWidgetId(context, 13),
        )
    }

    private fun placed(appWidgetId: Int, providerClass: String) {
        val component = mockk<ComponentName>()
        every { component.className } returns providerClass
        val info = mockk<AppWidgetProviderInfo>()
        info.provider = component
        every { appWidgetManager.getAppWidgetInfo(appWidgetId) } returns info
    }
}
