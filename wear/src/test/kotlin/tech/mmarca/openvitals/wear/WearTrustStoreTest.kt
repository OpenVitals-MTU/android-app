package tech.mmarca.openvitals.wear

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkToken

class WearTrustStoreTest {

    private val prefs = FakeSharedPreferences()
    private var now = 1_000_000L
    private val store = WearTrustStore(prefs, clock = { now })
    private val pendings = ArrayList<PendingPhone>()
    private val phone = "AA:BB:CC:DD:EE:01"
    private val token = WearLinkToken.generate()

    @Before
    fun listen() {
        WearLinkState.update { WearLinkUiState() }
        store.onPending = { pendings += it }
    }

    @After
    fun reset() {
        WearLinkState.update { WearLinkUiState() }
    }

    @Test
    fun `an unknown phone is pending once, then trusted with its token`() {
        store.notePending(phone, token, "Pixel")
        store.notePending(phone, token, "Pixel")

        assertEquals(1, pendings.size)
        assertEquals(PendingPhone(phone, token, "Pixel", now), WearLinkState.state.value.pending)
        assertNull(store.tokenFor(phone))

        store.trust(phone, token, "Pixel")

        assertEquals(token, store.tokenFor(phone))
        assertNull(WearLinkState.state.value.pending)
        assertEquals(listOf(TrustedPhone(phone, "Pixel", now)), store.trusted())
        assertEquals(store.trusted(), WearLinkState.state.value.trusted)
    }

    @Test
    fun `another phone replaces the pending one, and an expired request is gone`() {
        store.notePending(phone, token, "Pixel")
        store.notePending("AA:BB:CC:DD:EE:02", WearLinkToken.generate(), "OnePlus")

        assertEquals("OnePlus", WearLinkState.state.value.pending?.name)
        assertEquals(2, pendings.size)

        now += WearLinkState.PENDING_TTL_MILLIS + 1
        assertNull(WearLinkState.pendingIfFresh(now))
        WearLinkState.expirePending(now)
        assertNull(WearLinkState.state.value.pending)
    }

    @Test
    fun `block drops the token, unblock and forget bring the phone back to unknown`() {
        store.trust(phone, token, "Pixel")
        store.block(phone)

        assertTrue(store.isBlocked(phone))
        assertNull(store.tokenFor(phone))
        assertTrue(store.trusted().isEmpty())

        store.forget(phone)

        assertFalse(store.isBlocked(phone))
        assertNull(store.tokenFor(phone))
    }

    @Test
    fun `a mismatch is shown until the phone is forgotten`() {
        store.trust(phone, token, "Pixel")
        store.noteMismatch(phone, "Pixel")

        assertNotNull(WearLinkState.state.value.refused)
        store.forget(phone)
        assertNull(WearLinkState.state.value.refused)
    }

    @Test
    fun `addresses are matched regardless of case and survive a restart`() {
        store.trust(phone.lowercase(), token, "Pixel")

        assertEquals(token, WearTrustStore(prefs).tokenFor(phone))
    }
}
