package tech.mmarca.openvitals.wearlink

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * The secret a phone presents to a watch on every connection, and that the
 * watch stores for that phone's address once the wearer has allowed it: 32
 * random bytes, carried as Base64 without line breaks (44 characters). The
 * Bluetooth bond says the two devices know each other; the token says this
 * app on this phone is the one the wearer allowed. Compared in constant
 * time, so a wrong token learns nothing from how long the check took.
 */
object WearLinkToken {

    const val BYTES: Int = 32

    /** The Base64 length of [BYTES] bytes. */
    const val LENGTH: Int = 44

    fun generate(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(BYTES)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /** True for a token this object could have generated: the right length, decoding to [BYTES] bytes. */
    fun isWellFormed(token: String?): Boolean = decode(token) != null

    /** Constant-time equality; false when either side is malformed. */
    fun matches(a: String?, b: String?): Boolean {
        val left = decode(a) ?: return false
        val right = decode(b) ?: return false
        return MessageDigest.isEqual(left, right)
    }

    private fun decode(token: String?): ByteArray? {
        if (token == null || token.length != LENGTH || token.any { it.isWhitespace() }) return null
        return try {
            Base64.getDecoder().decode(token).takeIf { it.size == BYTES }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
