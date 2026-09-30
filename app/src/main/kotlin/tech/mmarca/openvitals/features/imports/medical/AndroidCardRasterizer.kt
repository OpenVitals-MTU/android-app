package tech.mmarca.openvitals.features.imports.medical

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.max
import tech.mmarca.openvitals.domain.medical.shc.CardFileKind
import tech.mmarca.openvitals.domain.medical.shc.CardRasterizer
import tech.mmarca.openvitals.domain.medical.shc.Raster

/**
 * Pixels for the QR reader, from the platform's decoders. A photo is scaled down and a PDF page
 * rendered at about [TargetPixels] on its long side: enough for a dense card printed small,
 * and one page is held at a time.
 */
internal class AndroidCardRasterizer(private val context: Context) : CardRasterizer {

    override fun rasters(bytes: ByteArray, kind: CardFileKind): Sequence<Raster> = when (kind) {
        CardFileKind.IMAGE -> sequenceOf(image(bytes)).filterNotNull()
        CardFileKind.PDF -> pages(bytes)
    }

    private fun image(bytes: ByteArray): Raster? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= TargetPixels) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return bitmap.toRaster()
    }

    /** PdfRenderer needs a file it can seek in, so the PDF is copied to the cache first and deleted after. */
    private fun pages(bytes: ByteArray): Sequence<Raster> = sequence {
        val file = File.createTempFile("card", ".pdf", context.cacheDir)
        try {
            file.writeBytes(bytes)
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            // A password or a broken file cannot be rendered; the source then finds no card.
            val renderer = runCatching { PdfRenderer(descriptor) }.getOrElse {
                descriptor.close()
                return@sequence
            }
            try {
                for (index in 0 until minOf(renderer.pageCount, MaxPages)) {
                    val page = renderer.openPage(index)
                    val raster = try {
                        val scale = TargetPixels.toFloat() / max(page.width, page.height)
                        val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1), (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        // A PDF page is transparent where nothing is drawn; the QR reader needs white.
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.toRaster()
                    } finally {
                        page.close()
                    }
                    yield(raster)
                }
            } finally {
                renderer.close()
            }
        } finally {
            file.delete()
        }
    }

    /** Brightness per pixel, read one row at a time so the ARGB copy never exists in full. */
    private fun Bitmap.toRaster(): Raster {
        val luminance = ByteArray(width * height)
        val row = IntArray(width)
        for (y in 0 until height) {
            getPixels(row, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val pixel = row[x]
                val gray = (Color.red(pixel) * RedWeight + Color.green(pixel) * GreenWeight + Color.blue(pixel) * BlueWeight) / WeightSum
                // A transparent pixel is white paper, not black.
                val alpha = Color.alpha(pixel)
                luminance[y * width + x] = ((gray * alpha + White * (White - alpha)) / White).toByte()
            }
        }
        val raster = Raster(width, height, luminance)
        recycle()
        return raster
    }

    private companion object {
        /** A card's code printed small on a page still gets four or more pixels per module. */
        const val TargetPixels = 2400

        // The usual weights for perceived brightness.
        const val RedWeight = 299
        const val GreenWeight = 587
        const val BlueWeight = 114
        const val WeightSum = 1000
        const val White = 255

        /** A card sits on the first pages of any document that holds one. */
        const val MaxPages = 5
    }
}
