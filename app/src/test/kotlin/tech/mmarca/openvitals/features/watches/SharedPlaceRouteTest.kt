package tech.mmarca.openvitals.features.watches

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.features.homewidgets.mockUriCodec
import tech.mmarca.openvitals.features.homewidgets.unmockUriCodec
import tech.mmarca.openvitals.isSupportedOpenVitalsRoute
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.sendPointRoute
import tech.mmarca.openvitals.sharedPlaceRoute

/** A place shared by another app becomes a send-a-point route, built from parsed numbers only. */
class SharedPlaceRouteTest {

    @Before
    fun setUp() = mockUriCodec()

    @After
    fun tearDown() = unmockUriCodec()

    private fun intent(action: String, data: String? = null, type: String? = null, text: String? = null): Intent =
        mockk<Intent>().also {
            every { it.action } returns action
            every { it.dataString } returns data
            every { it.type } returns type
            every { it.getCharSequenceExtra(Intent.EXTRA_TEXT) } returns text
        }

    @Test
    fun `a geo link opens the form with its position and label`() {
        val route = intent(Intent.ACTION_VIEW, data = "geo:0,0?q=48.8584,2.2945(Eiffel+Tower)").sendPointRoute()

        assertEquals(
            Screen.WatchSendPoint.createRoute(latitude = 48.8584, longitude = 2.2945, name = "Eiffel Tower"),
            route,
        )
    }

    @Test
    fun `shared text with a maps URL opens the form with its position`() {
        val route = intent(
            Intent.ACTION_SEND,
            type = "text/plain",
            text = "Opera House\nhttps://maps.google.com/?q=-33.8568,151.2153",
        ).sendPointRoute()

        assertEquals(
            Screen.WatchSendPoint.createRoute(latitude = -33.8568, longitude = 151.2153, name = "Opera House"),
            route,
        )
    }

    @Test
    fun `shared text with no position still opens the form, flagged`() {
        val shortLink = sharedPlaceRoute(geoLink = null, text = "Eiffel Tower\nhttps://maps.app.goo.gl/AbCd")
        val nothing = sharedPlaceRoute(geoLink = null, text = "https://example.org/page")

        assertEquals(Screen.WatchSendPoint.createRoute(name = "Eiffel Tower", unreadable = true), shortLink)
        assertEquals(Screen.WatchSendPoint.createRoute(unreadable = true), nothing)
    }

    @Test
    fun `other intents are not places`() {
        // A route file opened from a file manager.
        assertNull(intent(Intent.ACTION_VIEW, data = "content://files/ride.gpx").sendPointRoute())
        assertNull(intent(Intent.ACTION_VIEW, data = "geo:not-a-position").sendPointRoute())
        // A shared file has a type that is not text.
        assertNull(intent(Intent.ACTION_SEND, type = "application/gpx+xml", text = "48.8, 2.2").sendPointRoute())
        assertNull(intent(Intent.ACTION_SEND, type = "text/plain", text = null).sendPointRoute())
        assertNull(intent(Intent.ACTION_MAIN).sendPointRoute())
    }

    @Test
    fun `a long shared line is cut before it becomes a route`() {
        val route = sharedPlaceRoute(geoLink = null, text = "x".repeat(500) + "\n48.8584, 2.2945")

        assertEquals(
            Screen.WatchSendPoint.createRoute(latitude = 48.8584, longitude = 2.2945, name = "x".repeat(64)),
            route,
        )
    }

    @Test
    fun `the form is not reachable through the widget route extra`() {
        // Only MainActivity builds this route. The extra's allow-list must keep refusing it.
        val route = Screen.WatchSendPoint.createRoute(latitude = 1.0, longitude = 2.0)

        assertEquals(false, isSupportedOpenVitalsRoute(route))
    }
}
