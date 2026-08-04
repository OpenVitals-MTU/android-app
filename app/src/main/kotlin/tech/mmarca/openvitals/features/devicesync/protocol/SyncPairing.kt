package tech.mmarca.openvitals.features.devicesync.protocol

import java.security.SecureRandom
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pairing and session authentication for phone-to-phone sync.
 *
 * WHAT THE 6-DIGIT CODE ACTUALLY BUYS
 * -----------------------------------
 * The confidentiality and MITM resistance of the channel come from **Bluetooth
 * bonding** (Secure Simple Pairing), which encrypts the RFCOMM link at the OS
 * level. The app-layer 6-digit code shown on the host and typed on the guest is
 * a *mutual-confirmation + anti-mixup* token: it proves both phones are the two
 * the users are looking at, and it fails the session before any health data
 * moves if the user picked the wrong device or fat-fingered the code.
 *
 * Because the link is already bonded-encrypted, an attacker cannot observe the
 * handshake to brute-force the low-entropy (10^6) code offline — so app-layer
 * HMAC over the code is adequate here. It is NOT an independent PAKE and does
 * not by itself defend an unbonded link. (If the code itself must become
 * cryptographically load-bearing, a real SPAKE2/X25519 exchange is the
 * documented upgrade path; out of scope for v1.)
 *
 * SCHEME
 * ------
 * Each side generates a 256-bit nonce and exchanges it in the Hello frame.
 * Both derive the same session key regardless of who computes it by fixing the
 * nonce order to (host, guest):
 *
 *   sessionKey = HMAC-SHA256(key = utf8(code), msg = hostNonce ‖ guestNonce)
 *
 * Each side then proves it holds the session key with a challenge bound to the
 * PEER's nonce (so a proof can't be replayed for a different nonce) and to the
 * PROVER's role (so the two sides' proofs differ — the anti-reflection bit):
 *
 *   proof(challengeNonce, role) = HMAC-SHA256(key = sessionKey,
 *       msg = "ov-sync-auth-v2" ‖ roleByte ‖ challengeNonce)
 *
 * A side sends `proof(peerNonce, ownRole)` and verifies the received proof
 * against `proof(ownNonce, peerRole)`. A wrong code yields a different
 * sessionKey on the two phones, so the proofs mismatch and the session aborts.
 * The session also rejects a peer whose nonce equals ours (a reflection).
 */

/** Length of the per-session nonce, in bytes (256-bit). */
const val SYNC_NONCE_BYTES: Int = 32

/**
 * Domain-separation label mixed into every auth proof. Bump the suffix if the
 * proof construction ever changes so old and new clients can't cross-validate.
 * v2 adds the prover's role byte to the message (see [computeAuthProof]).
 */
private const val AUTH_CONTEXT = "ov-sync-auth-v2"

/** Role byte mixed into an auth proof so host and guest proofs differ. */
const val AUTH_ROLE_HOST: Int = 0
const val AUTH_ROLE_GUEST: Int = 1

/** Number of digits in the human-checked pairing code. */
const val PAIRING_CODE_DIGITS: Int = 6

/**
 * Generates a fresh random nonce. Injectable [random] (default [SecureRandom])
 * so tests can be deterministic; production MUST use a secure RNG.
 */
fun generateSyncNonce(random: Random = SecureRandom()): ByteArray {
    val bytes = ByteArray(SYNC_NONCE_BYTES)
    random.nextBytes(bytes)
    return bytes
}

/**
 * Generates a zero-padded [PAIRING_CODE_DIGITS]-digit pairing code, e.g.
 * `"042913"`. Injectable [random] for tests.
 */
fun generatePairingCode(random: Random = SecureRandom()): String =
    buildString(PAIRING_CODE_DIGITS) {
        repeat(PAIRING_CODE_DIGITS) { append(random.nextInt(10)) }
    }

/**
 * Derives the shared session key from the [code] and both nonces. The nonce
 * order is fixed (host first, guest second) so both phones compute an identical
 * key no matter which one calls this.
 */
fun deriveSessionKey(code: String, hostNonce: ByteArray, guestNonce: ByteArray): ByteArray =
    hmacSha256(code.toByteArray(Charsets.UTF_8), hostNonce + guestNonce)

/**
 * Computes the auth proof over [challengeNonce] under [sessionKey], bound to
 * the PROVER's [roleByte] ([AUTH_ROLE_HOST]/[AUTH_ROLE_GUEST]). To authenticate
 * to the peer, pass the PEER's nonce and your OWN role; to verify the peer's
 * proof, recompute this over your OWN nonce and the PEER's role and compare.
 *
 * The role byte is what stops a reflection attack: without it both sides'
 * proofs are the same HMAC, so a peer that echoed our nonce and then our proof
 * back would validate without knowing the code. With the role mixed in, our
 * proof (our role) never equals the proof we expect from the peer (their role).
 */
fun computeAuthProof(sessionKey: ByteArray, challengeNonce: ByteArray, roleByte: Int): ByteArray {
    val prefix = AUTH_CONTEXT.toByteArray(Charsets.UTF_8)
    val message = ByteArray(prefix.size + 1 + challengeNonce.size)
    prefix.copyInto(message)
    message[prefix.size] = roleByte.toByte()
    challengeNonce.copyInto(message, prefix.size + 1)
    return hmacSha256(sessionKey, message)
}

/**
 * Constant-time byte comparison — avoids leaking, via early-exit timing, how
 * many leading bytes of a proof matched.
 */
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
