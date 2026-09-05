package tech.mmarca.openvitals.features.devicesync.protocol

import java.security.SecureRandom
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pairing and session authentication for phone-to-phone sync. Confidentiality comes
 * from Bluetooth bonding; the 6-digit code is a mutual-confirmation token, not a PAKE.
 *
 * Both sides derive `sessionKey = HMAC-SHA256(utf8(code), hostNonce || guestNonce)` and
 * prove it with `HMAC-SHA256(sessionKey, "ov-sync-auth-v2" || roleByte || peerNonce)`.
 * The role byte stops reflection.
 */

/** Length of the per-session nonce, in bytes (256-bit). */
const val SYNC_NONCE_BYTES: Int = 32

/** Domain-separation label in every proof. Bump the suffix if the construction changes. */
private const val AUTH_CONTEXT = "ov-sync-auth-v2"

/** Role byte mixed into an auth proof so host and guest proofs differ. */
const val AUTH_ROLE_HOST: Int = 0
const val AUTH_ROLE_GUEST: Int = 1

/** Number of digits in the human-checked pairing code. */
const val PAIRING_CODE_DIGITS: Int = 6

/** A fresh random nonce. [random] is injectable for tests; production must use a secure RNG. */
fun generateSyncNonce(random: Random = SecureRandom()): ByteArray {
    val bytes = ByteArray(SYNC_NONCE_BYTES)
    random.nextBytes(bytes)
    return bytes
}

/** A zero-padded [PAIRING_CODE_DIGITS]-digit code, e.g. `"042913"`. */
fun generatePairingCode(random: Random = SecureRandom()): String =
    buildString(PAIRING_CODE_DIGITS) {
        repeat(PAIRING_CODE_DIGITS) { append(random.nextInt(10)) }
    }

/** The shared session key. Nonce order is host first, so both phones agree. */
fun deriveSessionKey(code: String, hostNonce: ByteArray, guestNonce: ByteArray): ByteArray =
    hmacSha256(code.toByteArray(Charsets.UTF_8), hostNonce + guestNonce)

/**
 * The auth proof over [challengeNonce], bound to the prover's [roleByte].
 * To authenticate, pass the peer's nonce and your own role; to verify,
 * recompute over your own nonce and the peer's role.
 */
fun computeAuthProof(sessionKey: ByteArray, challengeNonce: ByteArray, roleByte: Int): ByteArray {
    val prefix = AUTH_CONTEXT.toByteArray(Charsets.UTF_8)
    val message = ByteArray(prefix.size + 1 + challengeNonce.size)
    prefix.copyInto(message)
    message[prefix.size] = roleByte.toByte()
    challengeNonce.copyInto(message, prefix.size + 1)
    return hmacSha256(sessionKey, message)
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

private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray =
    Mac.getInstance("HmacSHA256")
        .apply { init(SecretKeySpec(key, "HmacSHA256")) }
        .doFinal(message)
