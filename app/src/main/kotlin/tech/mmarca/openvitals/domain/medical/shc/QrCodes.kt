package tech.mmarca.openvitals.domain.medical.shc

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.google.zxing.qrcode.QRCodeReader

/** An image as one brightness byte per pixel, row by row: a quarter of the memory of ARGB. */
class Raster(val width: Int, val height: Int, val luminance: ByteArray)

/** Turns an image or the pages of a PDF into pixels. The platform decodes them; the QR reader is pure Java. */
interface CardRasterizer {
    /** One raster per image, or per page of a PDF up to a limit. Empty when the bytes cannot be decoded. */
    fun rasters(bytes: ByteArray, kind: CardFileKind): Sequence<Raster>
}

enum class CardFileKind { IMAGE, PDF }

/** Reads QR codes with ZXing. Several codes on one page are all read, as a card split in parts needs. */
internal object QrCodes {
    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )

    /** The text of every QR code in [raster]; empty when there is none. */
    fun read(raster: Raster): List<String> {
        val source = PlanarYUVLuminanceSource(raster.luminance, raster.width, raster.height, 0, 0, raster.width, raster.height, false)
        // A photo reads best with the local binarizer; a clean render or a faint print with the global one.
        for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
            val found = runCatching { QRCodeMultiReader().decodeMultiple(BinaryBitmap(binarizer), hints) }.getOrNull()
            if (!found.isNullOrEmpty()) return found.map { it.text }.distinct()
        }
        return listOfNotNull(runCatching { QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text }.getOrNull())
    }
}
