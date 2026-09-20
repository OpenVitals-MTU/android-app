package tech.mmarca.openvitals.features.devicesync.protocol

import java.math.BigInteger
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPairingTest {

    // generateSyncNonce.

    @Test
    fun `nonce is 32 bytes`() {
        assertEquals(SYNC_NONCE_BYTES, generateSyncNonce(Random(1)).size)
    }

    // Key pairs.

    @Test
    fun `a public key has the 65-byte wire form`() {
        val key = generateSyncKeyPair().publicKey

        assertEquals(SYNC_PUBLIC_KEY_BYTES, key.size)
        assertEquals(0x04, key[0].toInt())
    }

    @Test
    fun `every session gets a new key pair`() {
        assertFalse(generateSyncKeyPair().publicKey.contentEquals(generateSyncKeyPair().publicKey))
    }

    // The host's commitment.

    private val salt = ByteArray(SYNC_NONCE_BYTES) { 0x5A }

    @Test
    fun `a commitment opens with the key and salt it was made from`() {
        val hostKey = generateSyncKeyPair().publicKey

        assertTrue(hostKeyMatchesCommitment(commitToHostKey(hostKey, salt), hostKey, salt))
    }

    @Test
    fun `a commitment does not open with another key or another salt`() {
        val hostKey = generateSyncKeyPair().publicKey
        val commitment = commitToHostKey(hostKey, salt)

        assertFalse(hostKeyMatchesCommitment(commitment, generateSyncKeyPair().publicKey, salt))
        assertFalse(hostKeyMatchesCommitment(commitment, hostKey, ByteArray(SYNC_NONCE_BYTES) { 0x5B }))
    }

    // Session keys.

    private val host = generateSyncKeyPair()
    private val guest = generateSyncKeyPair()
    private val transcript = syncTranscriptHash(
        hostHello = "host hello".toByteArray(),
        guestHello = "guest hello".toByteArray(),
        hostPublicKey = host.publicKey,
        guestPublicKey = guest.publicKey,
        hostSalt = salt,
    )

    @Test
    fun `both phones derive the same keys and the same code`() {
        val onHost = deriveSyncSessionKeys(host, guest.publicKey, transcript)
        val onGuest = deriveSyncSessionKeys(guest, host.publicKey, transcript)

        assertArrayEquals(onHost.hostToGuest, onGuest.hostToGuest)
        assertArrayEquals(onHost.guestToHost, onGuest.guestToHost)
        assertEquals(onHost.code, onGuest.code)
        assertEquals(32, onHost.hostToGuest.size)
    }

    @Test
    fun `the code is always six digits, zero-padded`() {
        repeat(20) {
            val code = deriveSyncSessionKeys(generateSyncKeyPair(), guest.publicKey, transcript).code

            assertTrue(code, Regex("^\\d{6}$").matches(code))
        }
    }

    @Test
    fun `each direction has its own key`() {
        val keys = deriveSyncSessionKeys(host, guest.publicKey, transcript)

        assertFalse(constantTimeEquals(keys.hostToGuest, keys.guestToHost))
    }

    @Test
    fun `a changed hello changes the keys`() {
        val tampered = syncTranscriptHash(
            hostHello = "host hello".toByteArray(),
            guestHello = "guest hellp".toByteArray(),
            hostPublicKey = host.publicKey,
            guestPublicKey = guest.publicKey,
            hostSalt = salt,
        )

        val honest = deriveSyncSessionKeys(host, guest.publicKey, transcript)
        val changed = deriveSyncSessionKeys(host, guest.publicKey, tampered)

        assertFalse(constantTimeEquals(honest.hostToGuest, changed.hostToGuest))
    }

    @Test
    fun `transcript parts cannot slide into each other`() {
        val a = syncTranscriptHash("ab".toByteArray(), "c".toByteArray(), host.publicKey, guest.publicKey, salt)
        val b = syncTranscriptHash("a".toByteArray(), "bc".toByteArray(), host.publicKey, guest.publicKey, salt)

        assertFalse(constantTimeEquals(a, b))
    }

    @Test
    fun `someone in the middle ends up with two different secrets`() {
        // The attacker runs one exchange with each phone. Neither phone shares a key with the other.
        val attacker = generateSyncKeyPair()

        val hostSide = deriveSyncSessionKeys(host, attacker.publicKey, transcript)
        val guestSide = deriveSyncSessionKeys(guest, attacker.publicKey, transcript)

        assertFalse(constantTimeEquals(hostSide.hostToGuest, guestSide.hostToGuest))
    }

    // Hostile keys.

    @Test
    fun `a key that is not on the curve is rejected`() {
        val offCurve = host.publicKey.copyOf().also { it[SYNC_PUBLIC_KEY_BYTES - 1] = (it.last() + 1).toByte() }

        val error = assertThrows(SyncPairingException::class.java) {
            deriveSyncSessionKeys(guest, offCurve, transcript)
        }
        // Our own check said no, not the provider further down.
        assertTrue(error.message.orEmpty().contains("not on the curve"))
    }

    @Test
    fun `a coordinate that is not reduced is rejected`() {
        // X = p is 0 mod p, and (0, sqrt(b)) is a real point. Only the range check tells them apart.
        val p = BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16)
        val b = BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16)
        val y = b.modPow(p.add(BigInteger.ONE).shiftRight(2), p)
        assertEquals("b must be a square for this test to mean anything", b, y.multiply(y).mod(p))
        val unreduced = byteArrayOf(0x04) + fixed32(p) + fixed32(y)

        val error = assertThrows(SyncPairingException::class.java) {
            deriveSyncSessionKeys(guest, unreduced, transcript)
        }
        assertTrue(error.message.orEmpty().contains("not on the curve"))
    }

    @Test
    fun `a key of the wrong form is rejected`() {
        assertThrows(SyncPairingException::class.java) {
            deriveSyncSessionKeys(guest, host.publicKey.copyOf(SYNC_PUBLIC_KEY_BYTES - 1), transcript)
        }
        assertThrows(SyncPairingException::class.java) {
            deriveSyncSessionKeys(guest, host.publicKey.copyOf().also { it[0] = 0x02 }, transcript)
        }
        assertThrows(SyncPairingException::class.java) {
            deriveSyncSessionKeys(guest, ByteArray(SYNC_PUBLIC_KEY_BYTES).also { it[0] = 0x04 }, transcript)
        }
    }

    // HKDF.

    @Test
    fun `hkdf matches RFC 5869 test case 1`() {
        val okm = hkdfSha256(
            salt = hex("000102030405060708090a0b0c"),
            ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
            info = hex("f0f1f2f3f4f5f6f7f8f9"),
            length = 42,
        )

        assertArrayEquals(
            hex("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"),
            okm,
        )
    }

    // Sealed frames.

    private val keys = deriveSyncSessionKeys(host, guest.publicKey, transcript)
    private val payload = "forty-two steps".toByteArray()

    @Test
    fun `a sealed frame opens on the other phone and hides its content`() {
        val sealed = SyncFrameCipher.forRole(SyncRole.HOST, keys).seal(SyncFrameType.BATCH, payload)

        assertFalse(String(sealed, Charsets.ISO_8859_1).contains("steps"))
        assertArrayEquals(payload, SyncFrameCipher.forRole(SyncRole.GUEST, keys).open(SyncFrameType.BATCH, sealed))
    }

    @Test
    fun `a changed frame does not open`() {
        val sealed = SyncFrameCipher.forRole(SyncRole.HOST, keys).seal(SyncFrameType.BATCH, payload)
        sealed[0] = (sealed[0] + 1).toByte()

        assertThrows(SyncPairingException::class.java) {
            SyncFrameCipher.forRole(SyncRole.GUEST, keys).open(SyncFrameType.BATCH, sealed)
        }
    }

    @Test
    fun `a replayed frame does not open`() {
        val sealed = SyncFrameCipher.forRole(SyncRole.HOST, keys).seal(SyncFrameType.BATCH, payload)
        val guestCipher = SyncFrameCipher.forRole(SyncRole.GUEST, keys)
        guestCipher.open(SyncFrameType.BATCH, sealed)

        assertThrows(SyncPairingException::class.java) { guestCipher.open(SyncFrameType.BATCH, sealed) }
    }

    @Test
    fun `a frame passed off as another type does not open`() {
        val sealed = SyncFrameCipher.forRole(SyncRole.HOST, keys).seal(SyncFrameType.BATCH_ACK, payload)

        assertThrows(SyncPairingException::class.java) {
            SyncFrameCipher.forRole(SyncRole.GUEST, keys).open(SyncFrameType.CONFIRM, sealed)
        }
    }

    @Test
    fun `a frame sent back to its sender does not open`() {
        val hostCipher = SyncFrameCipher.forRole(SyncRole.HOST, keys)
        val sealed = hostCipher.seal(SyncFrameType.CONFIRM, ByteArray(0))

        assertThrows(SyncPairingException::class.java) { hostCipher.open(SyncFrameType.CONFIRM, sealed) }
    }

    @Test
    fun `a plain frame does not open`() {
        assertThrows(SyncPairingException::class.java) {
            SyncFrameCipher.forRole(SyncRole.GUEST, keys).open(SyncFrameType.BATCH, payload)
        }
    }

    // constantTimeEquals.

    @Test
    fun `constantTimeEquals is true only for identical byte arrays`() {
        assertTrue(constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    }

    /** Big-endian, exactly 32 bytes: no sign byte, leading zeros kept. */
    private fun fixed32(value: BigInteger): ByteArray {
        val raw = value.toByteArray().takeLast(32).toByteArray()
        return ByteArray(32 - raw.size) + raw
    }

    private fun hex(text: String): ByteArray =
        text.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
