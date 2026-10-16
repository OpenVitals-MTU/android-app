package tech.mmarca.openvitals.domain.medical.shc

import java.nio.ByteBuffer

/**
 * What the camera has read of one SMART Health Card. Most cards are one QR code. An older card
 * is split over several, scanned one after the other in any order.
 */
class CardScan {
    private val parts = sortedMapOf<Int, String>()
    private var total = 0

    sealed interface Progress {
        /** A QR code that is not a SMART Health Card. */
        data object NotACard : Progress

        /** [have] of [total] parts are read; the camera keeps looking for the rest. */
        data class Partial(val have: Int, val total: Int) : Progress

        /** The whole card, as the text the import reads: one QR text per line. */
        data class Complete(val text: String) : Progress
    }

    fun add(qrText: String): Progress {
        val text = qrText.trim()
        val chunk = Chunk.matchEntire(text)
        if (chunk == null) return if (Whole.matches(text)) Progress.Complete(text) else Progress.NotACard
        val (part, of) = chunk.destructured
        // Parts of another card would never complete this one: start over with the new card.
        if (of.toInt() != total) {
            parts.clear()
            total = of.toInt()
        }
        parts[part.toInt()] = text
        return if ((1..total).all { it in parts }) Progress.Complete(parts.values.joinToString("\n")) else Progress.Partial(parts.size, total)
    }

    private companion object {
        val Chunk = Regex("""shc:/(\d+)/(\d+)/\d+""")
        val Whole = Regex("""shc:/\d+""")
    }
}

/**
 * The brightness plane of a camera frame as one byte per pixel. A frame's rows can be longer
 * than its width ([rowStride]); the padding is left out.
 */
fun luminanceOf(plane: ByteBuffer, rowStride: Int, width: Int, height: Int): ByteArray {
    val luminance = ByteArray(width * height)
    val source = plane.duplicate()
    for (row in 0 until height) {
        source.position(row * rowStride)
        source.get(luminance, row * width, width)
    }
    return luminance
}
