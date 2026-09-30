package tech.mmarca.openvitals.domain.medical.shc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.MedicalImportSourceResult
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason

/** A card from an image, a PDF or text: the codes are read, the file can be kept, and what is not a card is left alone. */
class SmartHealthCardImportSourceTest {

    private val jws = SmartHealthCardFixtures.jws()

    private fun read(bytes: ByteArray, rasterizer: CardRasterizer = SmartHealthCardFixtures.FixedRasterizer()) =
        SmartHealthCardImportSource(rasterizer).read { bytes.inputStream() }

    @Test
    fun `the QR code in a photo is read, and the photo can be kept`() {
        val rasterizer = SmartHealthCardFixtures.FixedRasterizer(SmartHealthCardFixtures.qrImage(SmartHealthCardFixtures.numeric(jws)))
        val result = read(SmartHealthCardFixtures.pngHeader, rasterizer) as MedicalImportSourceResult.Entries

        assertThat(rasterizer.lastKind).isEqualTo(CardFileKind.IMAGE)
        assertThat(result.file.entries.map { it.type }).containsExactly("Patient", "Immunization")
        assertThat(result.documents.single().mimeType).isEqualTo("image/png")
        assertThat(result.documents.single().extension).isEqualTo("png")
    }

    @Test
    fun `a PDF page with a card split over two codes gives one card`() {
        val (first, second) = SmartHealthCardFixtures.chunks(jws, 2)
        val rasterizer = SmartHealthCardFixtures.FixedRasterizer(SmartHealthCardFixtures.qrImage(first, second))
        val result = read(SmartHealthCardFixtures.pdfHeader, rasterizer) as MedicalImportSourceResult.Entries

        assertThat(rasterizer.lastKind).isEqualTo(CardFileKind.PDF)
        assertThat(result.file.entries).hasSize(2)
        assertThat(result.documents.single().mimeType).isEqualTo("application/pdf")
    }

    @Test
    fun `a card's QR text and a smart-health-card file are read as text`() {
        val text = read(SmartHealthCardFixtures.numeric(jws).toByteArray()) as MedicalImportSourceResult.Entries
        val file = read(SmartHealthCardFixtures.file(jws).toByteArray()) as MedicalImportSourceResult.Entries

        assertThat(text.documents.single().mimeType).isEqualTo("text/plain")
        assertThat(file.documents.single().mimeType).isEqualTo("application/smart-health-card")
        assertThat(file.file.entries).hasSize(2)
    }

    @Test
    fun `a part missing from a split card is listed, and the file is not kept`() {
        val (_, second) = SmartHealthCardFixtures.chunks(jws, 2)
        val result = read(second.toByteArray()) as MedicalImportSourceResult.Entries

        assertThat(result.file.entries).isEmpty()
        assertThat(result.skipped.single().reason).isEqualTo(MedicalSourceSkipReason.CARD_INCOMPLETE)
        assertThat(result.documents).isEmpty()
    }

    @Test
    fun `a picture with no card, and FHIR text, are not this source's`() {
        val blank = SmartHealthCardFixtures.FixedRasterizer(SmartHealthCardFixtures.blankImage())

        assertThat(read(SmartHealthCardFixtures.pngHeader, blank)).isEqualTo(MedicalImportSourceResult.NotSupported)
        assertThat(read("""{"resourceType":"Patient","id":"p"}""".toByteArray())).isEqualTo(MedicalImportSourceResult.NotSupported)
    }

    @Test
    fun `QR codes are read from pixels, several at a time`() {
        val two = QrCodes.read(SmartHealthCardFixtures.qrImage("shc:/1/2/5676", "shc:/2/2/5677"))

        assertThat(two).containsExactly("shc:/1/2/5676", "shc:/2/2/5677")
        assertThat(QrCodes.read(SmartHealthCardFixtures.blankImage())).isEmpty()
    }
}
