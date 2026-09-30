package tech.mmarca.openvitals.domain.medical.shc

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater

/** Invented SMART Health Cards, built the way an issuer builds them. The signature is not a real one. */
object SmartHealthCardFixtures {
    const val Issuer = "https://cards.example/issuer"

    val vaccinationBundle = """{"resourceType":"Bundle","type":"collection","entry":[""" +
        """{"fullUrl":"resource:0","resource":{"resourceType":"Patient","name":[{"family":"Maasikas","given":["Mari"]}],"birthDate":"1990-01-01"}},""" +
        """{"fullUrl":"resource:1","resource":{"resourceType":"Immunization","status":"completed",""" +
        """"vaccineCode":{"coding":[{"system":"http://hl7.org/fhir/sid/cvx","code":"207"}]},"patient":{"reference":"resource:0"},""" +
        """"occurrenceDateTime":"2021-01-01","performer":[{"actor":{"display":"Näidiskliinik"}}],"lotNumber":"0000001"}}]}"""

    fun jws(bundle: String = vaccinationBundle, issuer: String = Issuer, compressed: Boolean = true): String {
        val claims = """{"iss":"$issuer","nbf":1609459200,"vc":{"type":["https://smarthealth.cards#health-card"],""" +
            """"credentialSubject":{"fhirVersion":"4.0.1","fhirBundle":$bundle}}}"""
        val header = if (compressed) """{"zip":"DEF","alg":"ES256","kid":"test"}""" else """{"alg":"ES256","kid":"test"}"""
        val payload = if (compressed) deflateRaw(claims.toByteArray()) else claims.toByteArray()
        return listOf(base64Url(header.toByteArray()), base64Url(payload), base64Url("not a signature".toByteArray())).joinToString(".")
    }

    /** The QR text: each character as two digits, its code minus 45. */
    fun numeric(jws: String): String = "shc:/" + digits(jws)

    /** A card split over [parts] QR codes, as older issuers did. */
    fun chunks(jws: String, parts: Int): List<String> {
        val size = (jws.length + parts - 1) / parts
        return jws.chunked(size).mapIndexed { index, piece -> "shc:/${index + 1}/$parts/${digits(piece)}" }
    }

    fun file(vararg jws: String): String = """{"verifiableCredential":[${jws.joinToString(",") { "\"$it\"" }}]}"""

    /** QR codes side by side on one white image, as a PDF page or a photo of two codes shows them. */
    fun qrImage(vararg texts: String, size: Int = 600): Raster {
        val codes = texts.map { QRCodeWriter().encode(it, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 4)) }
        val gap = size / 4
        val width = codes.size * size + (codes.size + 1) * gap
        val height = size + 2 * gap
        val pixels = ByteArray(width * height) { White }
        codes.forEachIndexed { index, matrix ->
            val left = gap + index * (size + gap)
            for (y in 0 until size) for (x in 0 until size) {
                if (matrix[x, y]) pixels[(gap + y) * width + left + x] = Black
            }
        }
        return Raster(width, height, pixels)
    }

    /** A blank white image, with no code on it. */
    fun blankImage(): Raster = Raster(200, 200, ByteArray(200 * 200) { White })

    /** Hands back fixed pixels for any image or PDF, as the platform decoders would. */
    class FixedRasterizer(private vararg val rasters: Raster) : CardRasterizer {
        var lastKind: CardFileKind? = null
            private set

        override fun rasters(bytes: ByteArray, kind: CardFileKind): Sequence<Raster> {
            lastKind = kind
            return rasters.asSequence()
        }
    }

    /** First bytes of real files, enough for the type check. */
    val pngHeader = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
    val pdfHeader = "%PDF-1.4\n".toByteArray()

    private fun digits(text: String): String = text.map { "%02d".format(it.code - 45) }.joinToString("")

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun deflateRaw(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(bytes)
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) output.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return output.toByteArray()
    }

    private const val White: Byte = -1
    private const val Black: Byte = 0
}
