package tech.mmarca.openvitals.domain.medical.shc

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test

/** What the camera scanner does with each QR code it reads, and how it takes the brightness out of a frame. */
class CardScanTest {

    private val jws = SmartHealthCardFixtures.jws()

    @Test
    fun `a card in one code is complete at once, and its text imports`() {
        val progress = CardScan().add(SmartHealthCardFixtures.numeric(jws))

        assertThat(progress).isInstanceOf(CardScan.Progress.Complete::class.java)
        val text = (progress as CardScan.Progress.Complete).text
        assertThat(SmartHealthCards.read(text.lines()).cards).hasSize(1)
    }

    @Test
    fun `a card in three codes is complete after the third, in any order, however often a part is seen`() {
        val (first, second, third) = SmartHealthCardFixtures.chunks(jws, 3)
        val scan = CardScan()

        assertThat(scan.add(third)).isEqualTo(CardScan.Progress.Partial(1, 3))
        assertThat(scan.add(third)).isEqualTo(CardScan.Progress.Partial(1, 3))
        assertThat(scan.add(first)).isEqualTo(CardScan.Progress.Partial(2, 3))
        val complete = scan.add(second) as CardScan.Progress.Complete

        assertThat(SmartHealthCards.read(complete.text.lines()).cards.single().issuer).isEqualTo(SmartHealthCardFixtures.Issuer)
    }

    @Test
    fun `a code that is not a card is said to be none, and parts of another card start over`() {
        val scan = CardScan()

        assertThat(scan.add("https://example.org")).isEqualTo(CardScan.Progress.NotACard)
        assertThat(scan.add("shc:/1/3/5676")).isEqualTo(CardScan.Progress.Partial(1, 3))
        assertThat(scan.add("shc:/2/2/5676")).isEqualTo(CardScan.Progress.Partial(1, 2))
    }

    @Test
    fun `a frame's row padding is left out of the brightness`() {
        // Two rows of three pixels, each padded to a stride of five.
        val plane = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 0, 0, 4, 5, 6))

        assertThat(luminanceOf(plane, rowStride = 5, width = 3, height = 2).toList()).containsExactly(1.toByte(), 2.toByte(), 3.toByte(), 4.toByte(), 5.toByte(), 6.toByte()).inOrder()
    }

    @Test
    fun `a QR code in a camera frame is read from its brightness plane`() {
        val raster = SmartHealthCardFixtures.qrImage(SmartHealthCardFixtures.numeric(jws))
        // The frame's rows are padded by eight bytes, as camera buffers often are.
        val stride = raster.width + 8
        val plane = ByteBuffer.allocate(stride * raster.height)
        for (row in 0 until raster.height) {
            plane.position(row * stride)
            plane.put(raster.luminance, row * raster.width, raster.width)
        }

        val read = QrCodes.read(Raster(raster.width, raster.height, luminanceOf(plane, stride, raster.width, raster.height)))

        assertThat(read).containsExactly(SmartHealthCardFixtures.numeric(jws))
    }
}
