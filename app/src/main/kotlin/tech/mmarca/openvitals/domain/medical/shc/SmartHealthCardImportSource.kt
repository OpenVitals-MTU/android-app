package tech.mmarca.openvitals.domain.medical.shc

import java.io.InputStream
import tech.mmarca.openvitals.domain.medical.FhirFile
import tech.mmarca.openvitals.domain.medical.MedicalImportSource
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceDocument
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkip
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.decodeFhirText
import tech.mmarca.openvitals.domain.medical.readUpTo

/**
 * A SMART Health Card from a file: a photo or screenshot of its QR code, a PDF that shows it, a
 * `.smart-health-card` file, or the QR text itself. Everything in the file is read: several
 * cards, and a card split over several QR codes. The file can be kept as the original.
 */
class SmartHealthCardImportSource(private val rasterizer: CardRasterizer) : MedicalImportSource {

    override fun read(open: () -> InputStream): MedicalImportSourceResult {
        val bytes = open().use { it.readUpTo(MaxFileBytes) } ?: return MedicalImportSourceResult.TooLarge
        val type = CardFileType.of(bytes)
        val texts = when (type.kind) {
            null -> bytes.decodeFhirText().takeIf(SmartHealthCards::looksLikeCard)?.lines()?.filter { it.isNotBlank() }
                ?: return MedicalImportSourceResult.NotSupported
            else -> rasterizer.rasters(bytes, type.kind).flatMap { QrCodes.read(it) }.toList()
        }
        val cardTexts = texts.filter(SmartHealthCards::looksLikeCard)
        if (cardTexts.isEmpty()) return MedicalImportSourceResult.NotSupported
        val reading = SmartHealthCards.read(cardTexts)
        val skipped = List(reading.incomplete) { MedicalSourceSkip(MedicalSourceSkipReason.CARD_INCOMPLETE, type.label) } +
            List(reading.unreadable) { MedicalSourceSkip(MedicalSourceSkipReason.UNREADABLE_DOCUMENT, type.label) }
        val entries = SmartHealthCards.entries(reading.cards)
        if (entries.isEmpty() && skipped.isEmpty()) return MedicalImportSourceResult.Empty
        val document = MedicalSourceDocument(bytes, type.mimeType, type.extension).takeIf { entries.isNotEmpty() }
        return MedicalImportSourceResult.Entries(FhirFile(entries), skipped, listOfNotNull(document))
    }

    private companion object {
        /** A phone photo can be large; the card inside it never is. */
        const val MaxFileBytes = 32 * 1024 * 1024
    }
}

/** What a file is, by its first bytes: an image, a PDF, or text. */
internal class CardFileType(val kind: CardFileKind?, val mimeType: String, val extension: String, val label: String) {
    companion object {
        fun of(bytes: ByteArray): CardFileType = when {
            bytes.matches(0, "\u0089PNG") -> image("image/png", "png")
            bytes.matches(0, "\u00FF\u00D8\u00FF") -> image("image/jpeg", "jpg")
            bytes.matches(0, "GIF8") -> image("image/gif", "gif")
            bytes.matches(0, "RIFF") && bytes.matches(8, "WEBP") -> image("image/webp", "webp")
            bytes.matches(4, "ftyp") && HeifBrands.any { bytes.matches(8, it) } -> image("image/heic", "heic")
            isPdf(bytes) -> CardFileType(CardFileKind.PDF, "application/pdf", "pdf", "PDF")
            bytes.copyOf(minOf(bytes.size, TextSniffBytes)).decodeToString().trimStart().startsWith("{") ->
                CardFileType(null, "application/smart-health-card", "smart-health-card", "smart-health-card")
            else -> CardFileType(null, "text/plain", "txt", "text")
        }

        /** True for an image or a PDF, which only a card source reads. */
        fun isPictureOrPdf(bytes: ByteArray): Boolean = of(bytes).kind != null

        fun isPdf(bytes: ByteArray): Boolean = bytes.matches(0, "%PDF")

        private fun image(mimeType: String, extension: String) = CardFileType(CardFileKind.IMAGE, mimeType, extension, "image")

        private val HeifBrands = listOf("heic", "heix", "hevc", "mif1")

        /** Enough of a text file to see how it starts. */
        private const val TextSniffBytes = 256

        /** Whether the bytes at [offset] are [signature], one byte per character. */
        private fun ByteArray.matches(offset: Int, signature: String): Boolean =
            size >= offset + signature.length && signature.indices.all { (this[offset + it].toInt() and 0xFF) == signature[it].code }
    }
}
