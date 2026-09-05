package tech.mmarca.openvitals.features.devicesync.protocol

import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPairingTest {

    // generatePairingCode.

    @Test
    fun `pairing code is always six digits, zero-padded`() {
        val code = generatePairingCode(Random(0))

        assertEquals(PAIRING_CODE_DIGITS, code.length)
        assertTrue(Regex("^\\d{6}$").matches(code))
    }

    // generateSyncNonce.

    @Test
    fun `nonce is 32 bytes`() {
        assertEquals(SYNC_NONCE_BYTES, generateSyncNonce(Random(1)).size)
    }

    // deriveSessionKey.

    private val hostNonce = ByteArray(SYNC_NONCE_BYTES) { 0xA1.toByte() }
    private val guestNonce = ByteArray(SYNC_NONCE_BYTES) { 0xB2.toByte() }

    @Test
    fun `both phones derive the same key from the same inputs`() {
        val onHost = deriveSessionKey("042913", hostNonce, guestNonce)
        val onGuest = deriveSessionKey("042913", hostNonce, guestNonce)

        assertArrayEquals(onHost, onGuest)
        assertEquals(32, onHost.size)
    }

    @Test
    fun `a different code yields a different key`() {
        val right = deriveSessionKey("042913", hostNonce, guestNonce)
        val wrong = deriveSessionKey("999999", hostNonce, guestNonce)

        assertFalse(constantTimeEquals(right, wrong))
    }

    @Test
    fun `nonce order is fixed, so host and guest roles agree`() {
        // Swapping which arg is "host" changes the key, so both sides must agree on who is host.
        val ab = deriveSessionKey("111111", hostNonce, guestNonce)
        val ba = deriveSessionKey("111111", guestNonce, hostNonce)

        assertFalse(constantTimeEquals(ab, ba))
    }

    // Auth proof exchange.

    private val exchangeHostNonce = generateSyncNonce(Random(2))
    private val exchangeGuestNonce = generateSyncNonce(Random(3))

    private fun keyFor(code: String): ByteArray =
        deriveSessionKey(code, exchangeHostNonce, exchangeGuestNonce)

    @Test
    fun `matching codes - each side verifies the peer proof`() {
        val hostKey = keyFor("424242")
        val guestKey = keyFor("424242")

        // Host authenticates over the guest's nonce with the host role; guest verifies the same.
        val hostProof = computeAuthProof(hostKey, exchangeGuestNonce, AUTH_ROLE_HOST)
        val guestExpectsHost = computeAuthProof(guestKey, exchangeGuestNonce, AUTH_ROLE_HOST)
        assertTrue(constantTimeEquals(hostProof, guestExpectsHost))

        // And symmetrically, with the guest role.
        val guestProof = computeAuthProof(guestKey, exchangeHostNonce, AUTH_ROLE_GUEST)
        val hostExpectsGuest = computeAuthProof(hostKey, exchangeHostNonce, AUTH_ROLE_GUEST)
        assertTrue(constantTimeEquals(guestProof, hostExpectsGuest))
    }

    @Test
    fun `a reflected proof does not validate (role binding)`() {
        val key = keyFor("424242")
        // The attacker echoes the host's proof back. The host now expects a guest-role proof over its nonce.
        val hostProof = computeAuthProof(key, exchangeGuestNonce, AUTH_ROLE_HOST)
        val hostExpectsGuest = computeAuthProof(key, exchangeHostNonce, AUTH_ROLE_GUEST)

        assertFalse(constantTimeEquals(hostProof, hostExpectsGuest))
    }

    @Test
    fun `wrong code on the guest fails verification`() {
        val hostKey = keyFor("424242")
        val guestKey = keyFor("000000") // user mistyped

        val hostProof = computeAuthProof(hostKey, exchangeGuestNonce, AUTH_ROLE_HOST)
        val guestExpectsHost = computeAuthProof(guestKey, exchangeGuestNonce, AUTH_ROLE_HOST)

        assertFalse(constantTimeEquals(hostProof, guestExpectsHost))
    }

    // constantTimeEquals.

    @Test
    fun `constantTimeEquals is true only for identical byte arrays`() {
        assertTrue(constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    }
}
