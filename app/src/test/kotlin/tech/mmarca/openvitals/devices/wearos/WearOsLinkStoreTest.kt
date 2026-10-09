package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.wearlink.WearLinkToken

class WearOsLinkStoreTest {

    private val prefs = FakeSharedPreferences()
    private val store = WearOsLinkStore(prefs)

    @Test
    fun `one token per watch, stable across calls and restarts, until forgotten`() {
        val token = store.tokenFor("a8:d1:62:be:3a:3b")

        assertTrue(WearLinkToken.isWellFormed(token))
        assertEquals(token, store.tokenFor("A8:D1:62:BE:3A:3B"))
        assertEquals(token, WearOsLinkStore(prefs).tokenFor("A8:D1:62:BE:3A:3B"))
        assertNotEquals(token, store.tokenFor("11:22:33:44:55:66"))

        store.forgetToken("A8:D1:62:BE:3A:3B")

        assertNotEquals(token, store.tokenFor("A8:D1:62:BE:3A:3B"))
    }

    @Test
    fun `the link address is kept per registered address`() {
        assertNull(store.linkAddress("7F:12:34:56:78:9A"))

        store.setLinkAddress("7F:12:34:56:78:9A", "a8:d1:62:be:3a:3b")

        assertEquals("A8:D1:62:BE:3A:3B", store.linkAddress("7f:12:34:56:78:9a"))
        store.clear("watch-1", "7F:12:34:56:78:9A")
        assertNull(store.linkAddress("7F:12:34:56:78:9A"))
    }

    @Test
    fun `snapshots are per device and the latest wins`() {
        val t = Instant.parse("2026-10-09T07:00:00Z")
        store.record("watch-1", WearOsLinkSnapshot(WearOsAppStatus.NO_ANSWER, checkedAt = t))
        store.record("watch-1", WearOsLinkSnapshot(WearOsAppStatus.APP_RUNNING, watchName = "Galaxy Watch8", checkedAt = t.plusSeconds(60)))

        assertEquals("Galaxy Watch8", store.snapshots.value.getValue("watch-1").watchName)
        store.clear("watch-1", null)
        assertTrue(store.snapshots.value.isEmpty())
    }
}
