package tech.mmarca.openvitals.features.devicesync.protocol

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Pairing for phone-to-phone sync: a key agreement the two users confirm by eye.
 *
 * 1. The host commits to a fresh P-256 key: `SHA-256(label, hostKey, salt)`.
 * 2. The guest sends its own fresh key. Only then does the host reveal its key and salt.
 * 3. Both hash the whole handshake. HKDF turns the ECDH secret and that hash into one
 *    AES-256 key per direction and six digits.
 * 4. Both phones show the digits. The users check that they match.
 *
 * The commitment is what makes six digits enough. Someone in the middle must fix their
 * key before they see the other side's, so they cannot search for a key that gives the
 * same digits. One try in a million works, and each try needs both users at the wizard.
 *
 * Typing a code cannot do this. A short code that is proved with a MAC can be guessed
 * offline from one recorded handshake.
 */

/** Length of the per-session nonce and of the host's key salt, in bytes. */
const val SYNC_NONCE_BYTES: Int = 32

/** Number of digits in the code the two users compare. */
const val PAIRING_CODE_DIGITS: Int = 6

/** A P-256 public key on the wire: `0x04`, then X and Y, 32 bytes each. */
const val SYNC_PUBLIC_KEY_BYTES: Int = 65

/** Labels keep each hash and key to one use. Change the suffix if the construction changes. */
private const val COMMIT_LABEL = "ov-sync-v2 commit"
private const val TRANSCRIPT_LABEL = "ov-sync-v2 transcript"
private const val HOST_TO_GUEST_LABEL = "ov-sync-v2 host-to-guest"
private const val GUEST_TO_HOST_LABEL = "ov-sync-v2 guest-to-host"
private const val CODE_LABEL = "ov-sync-v2 code"

private const val COORDINATE_BYTES = 32
private const val AES_KEY_BYTES = 32
private const val GCM_TAG_BITS = 128
private const val GCM_NONCE_BYTES = 12
private const val HASH_BYTES = 32

/** Thrown when the peer's key material or a sealed frame does not check out. Fatal to the session. */
class SyncPairingException(message: String) : Exception(message)

/** A fresh random nonce. [random] is injectable for tests; production must use a secure RNG. */
fun generateSyncNonce(random: Random = SecureRandom()): ByteArray {
    val bytes = ByteArray(SYNC_NONCE_BYTES)
    random.nextBytes(bytes)
    return bytes
}

/** One session's key pair. [publicKey] is the wire form. */
class SyncKeyPair(val privateKey: PrivateKey, val publicKey: ByteArray)

/** A fresh P-256 key pair. Never reuse one across sessions. */
fun generateSyncKeyPair(random: SecureRandom = SecureRandom()): SyncKeyPair {
    val pair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1"), random) }
        .generateKeyPair()
    return SyncKeyPair(pair.private, encodePublicKey(pair.public as ECPublicKey))
}

/** What the host sends before it has seen the guest's key. [salt] keeps the key hidden until the reveal. */
fun commitToHostKey(hostPublicKey: ByteArray, salt: ByteArray): ByteArray =
    sha256(COMMIT_LABEL.toByteArray(Charsets.UTF_8), hostPublicKey, salt)

/** True when the revealed key and salt are the ones the host committed to. */
fun hostKeyMatchesCommitment(commitment: ByteArray, hostPublicKey: ByteArray, salt: ByteArray): Boolean =
    constantTimeEquals(commitment, commitToHostKey(hostPublicKey, salt))

/**
 * A hash of everything both phones said in the handshake. Both hellos go in whole, so a
 * changed name, type list or nonce changes the keys and the code.
 */
fun syncTranscriptHash(
    hostHello: ByteArray,
    guestHello: ByteArray,
    hostPublicKey: ByteArray,
    guestPublicKey: ByteArray,
    hostSalt: ByteArray,
): ByteArray = sha256(
    TRANSCRIPT_LABEL.toByteArray(Charsets.UTF_8),
    hostHello,
    guestHello,
    hostPublicKey,
    guestPublicKey,
    hostSalt,
)

/** What one handshake yields: a key per direction and the digits to compare. */
class SyncSessionKeys(
    val hostToGuest: ByteArray,
    val guestToHost: ByteArray,
    /** Zero-padded, [PAIRING_CODE_DIGITS] long, e.g. `"042913"`. */
    val code: String,
)

/**
 * Runs ECDH with the peer's key and derives the session keys, bound to [transcript].
 * Throws [SyncPairingException] when [peerPublicKey] is not a point on P-256.
 */
fun deriveSyncSessionKeys(
    own: SyncKeyPair,
    peerPublicKey: ByteArray,
    transcript: ByteArray,
): SyncSessionKeys {
    val peer = decodePublicKey(peerPublicKey)
    val secret = try {
        KeyAgreement.getInstance("ECDH")
            .apply {
                init(own.privateKey)
                doPhase(peer, true)
            }
            .generateSecret()
    } catch (e: GeneralSecurityException) {
        throw SyncPairingException("key agreement failed: ${e.message}")
    }
    fun expand(label: String, length: Int): ByteArray =
        hkdfSha256(salt = transcript, ikm = secret, info = label.toByteArray(Charsets.UTF_8), length = length)
    // 63 random bits over a million: the bias is far below one in a billion.
    val codeNumber = (ByteBuffer.wrap(expand(CODE_LABEL, Long.SIZE_BYTES)).long and Long.MAX_VALUE) % 1_000_000L
    return SyncSessionKeys(
        hostToGuest = expand(HOST_TO_GUEST_LABEL, AES_KEY_BYTES),
        guestToHost = expand(GUEST_TO_HOST_LABEL, AES_KEY_BYTES),
        code = codeNumber.toString().padStart(PAIRING_CODE_DIGITS, '0'),
    )
}

/**
 * Seals and opens frames with AES-256-GCM, one key per direction.
 *
 * The nonce is a frame counter, so a replayed, dropped or reordered frame fails to open.
 * The frame type is bound in, so a sealed frame cannot be passed off as another type.
 * [seal] must run in send order, and [open] in arrival order.
 */
class SyncFrameCipher(sendKey: ByteArray, receiveKey: ByteArray) {
    private val sendKey = SecretKeySpec(sendKey, "AES")
    private val receiveKey = SecretKeySpec(receiveKey, "AES")
    private var sealed = 0L
    private var opened = 0L

    fun seal(type: SyncFrameType, payload: ByteArray): ByteArray {
        val out = crypt(Cipher.ENCRYPT_MODE, sendKey, sealed, type, payload)
        sealed += 1
        return out
    }

    fun open(type: SyncFrameType, payload: ByteArray): ByteArray {
        val out = try {
            crypt(Cipher.DECRYPT_MODE, receiveKey, opened, type, payload)
        } catch (e: GeneralSecurityException) {
            throw SyncPairingException("frame failed its integrity check")
        }
        opened += 1
        return out
    }

    private fun crypt(mode: Int, key: SecretKeySpec, counter: Long, type: SyncFrameType, payload: ByteArray): ByteArray {
        val nonce = ByteBuffer.allocate(GCM_NONCE_BYTES).putInt(0).putLong(counter).array()
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(mode, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            updateAAD(byteArrayOf(type.ordinal.toByte()))
            doFinal(payload)
        }
    }

    companion object {
        fun forRole(role: SyncRole, keys: SyncSessionKeys): SyncFrameCipher = when (role) {
            SyncRole.HOST -> SyncFrameCipher(sendKey = keys.hostToGuest, receiveKey = keys.guestToHost)
            SyncRole.GUEST -> SyncFrameCipher(sendKey = keys.guestToHost, receiveKey = keys.hostToGuest)
        }
    }
}

/** Constant-time byte comparison. */
fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var diff = 0
    for (index in a.indices) {
        diff = diff or (a[index].toInt() xor b[index].toInt())
    }
    return diff == 0
}

/** HKDF (RFC 5869) over HMAC-SHA256. */
internal fun hkdfSha256(salt: ByteArray, ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
    require(length in 1..255 * HASH_BYTES) { "bad HKDF length $length" }
    val prk = hmacSha256(if (salt.isEmpty()) ByteArray(HASH_BYTES) else salt, ikm)
    val out = ByteArray(length)
    var block = ByteArray(0)
    var offset = 0
    var counter = 1
    while (offset < length) {
        block = hmacSha256(prk, block + info + counter.toByte())
        val take = minOf(block.size, length - offset)
        block.copyInto(out, offset, 0, take)
        offset += take
        counter += 1
    }
    return out
}

private val p256: ECParameterSpec by lazy {
    AlgorithmParameters.getInstance("EC").run {
        init(ECGenParameterSpec("secp256r1"))
        getParameterSpec(ECParameterSpec::class.java)
    }
}

private fun encodePublicKey(key: ECPublicKey): ByteArray {
    val out = ByteArray(SYNC_PUBLIC_KEY_BYTES)
    out[0] = 0x04
    key.w.affineX.toFixedBytes().copyInto(out, 1)
    key.w.affineY.toFixedBytes().copyInto(out, 1 + COORDINATE_BYTES)
    return out
}

/** The point is checked by hand, so the session never relies on the provider to reject an off-curve key. */
private fun decodePublicKey(bytes: ByteArray): PublicKey {
    if (bytes.size != SYNC_PUBLIC_KEY_BYTES || bytes[0] != 0x04.toByte()) {
        throw SyncPairingException("peer key has the wrong form")
    }
    val x = BigInteger(1, bytes.copyOfRange(1, 1 + COORDINATE_BYTES))
    val y = BigInteger(1, bytes.copyOfRange(1 + COORDINATE_BYTES, SYNC_PUBLIC_KEY_BYTES))
    val curve = p256.curve
    val p = (curve.field as ECFieldFp).p
    val onCurve = x < p && y < p &&
        y.multiply(y).mod(p) == x.pow(3).add(curve.a.multiply(x)).add(curve.b).mod(p)
    if (!onCurve) throw SyncPairingException("peer key is not on the curve")
    return try {
        KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), p256))
    } catch (e: GeneralSecurityException) {
        throw SyncPairingException("peer key was rejected: ${e.message}")
    }
}

private fun BigInteger.toFixedBytes(): ByteArray {
    val raw = toByteArray()
    val out = ByteArray(COORDINATE_BYTES)
    // toByteArray may add a sign byte or drop leading zeros.
    val from = maxOf(0, raw.size - COORDINATE_BYTES)
    raw.copyInto(out, COORDINATE_BYTES - (raw.size - from), from, raw.size)
    return out
}

/** Each part is length-prefixed, so two different part lists never hash the same bytes. */
private fun sha256(vararg parts: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    for (part in parts) {
        digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(part.size).array())
        digest.update(part)
    }
    return digest.digest()
}

private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray =
    Mac.getInstance("HmacSHA256")
        .apply { init(SecretKeySpec(key, "HmacSHA256")) }
        .doFinal(message)
