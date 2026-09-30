package tech.mmarca.openvitals.domain.medical.shc

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Writes an invented card as a PNG and as a PDF, for a check on a phone. Skipped unless
 * OPENVITALS_SHC_SAMPLE_DIR names a folder, so CI never runs it.
 */
class SmartHealthCardSampleFiles {

    @Test
    fun `the invented card is written as an image and a PDF`() {
        val folder = System.getenv("OPENVITALS_SHC_SAMPLE_DIR")?.let(::File)
        assumeTrue(folder != null && folder.isDirectory)
        val raster = SmartHealthCardFixtures.qrImage(SmartHealthCardFixtures.numeric(SmartHealthCardFixtures.jws()), size = 800)
        val image = BufferedImage(raster.width, raster.height, BufferedImage.TYPE_BYTE_GRAY)
        image.raster.setDataElements(0, 0, raster.width, raster.height, raster.luminance)
        ImageIO.write(image, "png", File(folder, "openvitals-card-sample.png"))
        File(folder!!, "openvitals-card-sample.pdf").writeBytes(pdfWithImage(raster))
    }

    /** Says how many QR codes the reader finds in each image of OPENVITALS_QR_CHECK, a list of paths split by `:`. */
    @Test
    fun `the reader is tried on images from disk`() {
        val paths = System.getenv("OPENVITALS_QR_CHECK")
        assumeTrue(paths != null)
        paths!!.split(':').forEach { path ->
            val image = ImageIO.read(File(path))
            val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            val luminance = ByteArray(pixels.size) { (pixels[it] and 0xFF).toByte() }
            println("$path ${image.width}x${image.height}: ${QrCodes.read(Raster(image.width, image.height, luminance)).size} codes")
        }
    }

    /** One A4-like page with the image drawn in its middle, as a grey-scale image object. */
    private fun pdfWithImage(raster: Raster): ByteArray {
        val gray = raster.luminance
        val drawnWidth = 360
        val drawnHeight = drawnWidth * raster.height / raster.width
        val content = "q $drawnWidth 0 0 $drawnHeight 126 300 cm /Im1 Do Q"
        val output = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        fun write(text: String) = output.write(text.toByteArray(Charsets.ISO_8859_1))
        fun obj(body: () -> Unit) {
            offsets += output.size()
            write("${offsets.size} 0 obj\n")
            body()
            write("\nendobj\n")
        }
        write("%PDF-1.4\n")
        obj { write("<< /Type /Catalog /Pages 2 0 R >>") }
        obj { write("<< /Type /Pages /Kids [3 0 R] /Count 1 >>") }
        obj { write("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /XObject << /Im1 5 0 R >> >> >>") }
        obj { write("<< /Length ${content.length} >>\nstream\n$content\nendstream") }
        obj {
            write("<< /Type /XObject /Subtype /Image /Width ${raster.width} /Height ${raster.height} /ColorSpace /DeviceGray /BitsPerComponent 8 /Length ${gray.size} >>\nstream\n")
            output.write(gray)
            write("\nendstream")
        }
        val xref = output.size()
        write("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { write("%010d 00000 n \n".format(it)) }
        write("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    }
}
