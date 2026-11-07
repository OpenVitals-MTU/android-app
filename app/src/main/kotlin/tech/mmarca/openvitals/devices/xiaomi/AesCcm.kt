package tech.mmarca.openvitals.devices.xiaomi

import android.annotation.SuppressLint
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * AES-CCM decryption (RFC 3610), the mode a MiBeacon frame is sealed with.
 * Android ships no CCM cipher, so this builds it from the AES block cipher:
 * counter mode for the payload, CBC-MAC for the tag.
 */
internal object AesCcm {

    private const val BLOCK = 16

    /** The plaintext, or null when [tag] does not authenticate it. */
    fun decrypt(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        ciphertext: ByteArray,
        tag: ByteArray,
    ): ByteArray? {
        val aes = blockCipher(key)
        // The length field takes what the nonce leaves of a block, after the flags byte.
        val lengthBytes = BLOCK - 1 - nonce.size

        val plaintext = ByteArray(ciphertext.size)
        for (offset in ciphertext.indices step BLOCK) {
            val keystream = aes.doFinal(counterBlock(nonce, lengthBytes, offset / BLOCK + 1))
            for (i in offset until minOf(offset + BLOCK, ciphertext.size)) {
                plaintext[i] = (ciphertext[i].toInt() xor keystream[i - offset].toInt()).toByte()
            }
        }

        var mac = aes.doFinal(firstBlock(nonce, lengthBytes, associatedData, plaintext.size, tag.size))
        for (block in (lengthPrefixed(associatedData) + padded(plaintext)).asIterable().chunked(BLOCK)) {
            mac = aes.doFinal(ByteArray(BLOCK) { (mac[it].toInt() xor block[it].toInt()).toByte() })
        }

        val tagMask = aes.doFinal(counterBlock(nonce, lengthBytes, 0))
        var difference = 0
        for (i in tag.indices) {
            difference = difference or (mac[i].toInt() xor tagMask[i].toInt() xor tag[i].toInt())
        }
        return plaintext.takeIf { difference == 0 }
    }

    // CCM is built on the bare block cipher: ECB here is the primitive, not the mode of the message.
    @SuppressLint("GetInstance")
    private fun blockCipher(key: ByteArray): Cipher =
        Cipher.getInstance("AES/ECB/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        }

    private fun counterBlock(nonce: ByteArray, lengthBytes: Int, counter: Int): ByteArray =
        ByteArray(BLOCK).also { block ->
            block[0] = (lengthBytes - 1).toByte()
            nonce.copyInto(block, 1)
            block.putBigEndian(counter, lengthBytes)
        }

    private fun firstBlock(
        nonce: ByteArray,
        lengthBytes: Int,
        associatedData: ByteArray,
        messageSize: Int,
        tagSize: Int,
    ): ByteArray = ByteArray(BLOCK).also { block ->
        val hasAssociatedData = if (associatedData.isEmpty()) 0 else 0x40
        block[0] = (hasAssociatedData or ((tagSize - 2) / 2 shl 3) or (lengthBytes - 1)).toByte()
        nonce.copyInto(block, 1)
        block.putBigEndian(messageSize, lengthBytes)
    }

    /** The associated data behind its two-byte length, zero-padded to whole blocks. */
    private fun lengthPrefixed(associatedData: ByteArray): ByteArray {
        if (associatedData.isEmpty()) return associatedData
        val prefixed = byteArrayOf((associatedData.size ushr 8).toByte(), associatedData.size.toByte()) +
            associatedData
        return padded(prefixed)
    }

    private fun padded(bytes: ByteArray): ByteArray = bytes.copyOf((bytes.size + BLOCK - 1) / BLOCK * BLOCK)

    /** Writes [value] into the last [width] bytes. */
    private fun ByteArray.putBigEndian(value: Int, width: Int) {
        for (i in 0 until minOf(width, Int.SIZE_BYTES)) {
            this[size - 1 - i] = (value ushr (8 * i)).toByte()
        }
    }
}
