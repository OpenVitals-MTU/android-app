package tech.mmarca.openvitals.domain.medical.shc

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64
import java.util.zip.Inflater
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.medical.FhirEntry
import tech.mmarca.openvitals.domain.medical.FhirFileParser
import tech.mmarca.openvitals.domain.medical.FhirOrigin
import tech.mmarca.openvitals.domain.medical.FhirVersionDetector
import tech.mmarca.openvitals.domain.medical.OpenVitalsFhir
import tech.mmarca.openvitals.domain.medical.obj
import tech.mmarca.openvitals.domain.medical.objects
import tech.mmarca.openvitals.domain.medical.string
import tech.mmarca.openvitals.domain.medical.strings
import tech.mmarca.openvitals.domain.medical.with

/** One decoded SMART Health Card: who issued it, its FHIR version, and the Bundle inside. */
internal data class SmartHealthCard(val issuer: String, val fhirVersion: String?, val bundle: JsonObject)

/** The cards found in a file, and how many could not be read. */
internal data class CardReading(val cards: List<SmartHealthCard>, val incomplete: Int, val unreadable: Int)

/**
 * SMART Health Cards (smarthealth.cards): the QR codes pharmacies and health systems put on
 * vaccination and lab result cards.
 *
 * - A QR code holds `shc:/` and digit pairs: each pair is a character code minus 45, and the
 *   characters form a JWS. A card split over several codes has `shc:/<part>/<total>/` instead.
 * - The JWS payload is raw DEFLATE; inside, `vc.credentialSubject.fhirBundle` is a FHIR R4 Bundle.
 * - A `.smart-health-card` file lists the JWS of each card under `verifiableCredential`.
 *
 * The signature is not checked: that needs the issuer's keys from the network, and the app
 * has no internet access. Every record from a card is tagged so, and the screens say it.
 */
internal object SmartHealthCards {
    const val NumericPrefix = "shc:/"

    /** Marks a record from a card whose signature was not checked. */
    const val UnverifiedTag = "smart-health-card-unverified"

    private const val UnverifiedDisplay = "SMART Health Card, signature not verified"
    private val Chunked = Regex("""shc:/(\d+)/(\d+)/(\d+)""")
    private val Whole = Regex("""shc:/(\d+)""")
    private val JwsShape = Regex("""[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*""")
    private val json = Json { ignoreUnknownKeys = true }

    /** True for text that holds a card: QR text, a JWS, or a `.smart-health-card` file. */
    fun looksLikeCard(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith(NumericPrefix) || JwsShape.matches(trimmed.trim()) ||
            (trimmed.startsWith("{") && "verifiableCredential" in trimmed)
    }

    /** Reads every card in [texts]: QR texts, JWS strings, or the contents of `.smart-health-card` files. */
    fun read(texts: List<String>): CardReading {
        val jws = mutableListOf<String>()
        val chunks = mutableMapOf<Int, MutableMap<Int, String>>()
        for (text in texts.map { it.trim() }) {
            val chunk = Chunked.matchEntire(text)
            val whole = Whole.matchEntire(text)
            when {
                chunk != null -> {
                    val (part, total, digits) = chunk.destructured
                    chunks.getOrPut(total.toInt()) { mutableMapOf() }[part.toInt()] = digits
                }
                whole != null -> numericToJws(whole.groupValues[1])?.let(jws::add)
                text.startsWith("{") -> jws += fileCredentials(text)
                JwsShape.matches(text) -> jws += text
            }
        }
        var incomplete = 0
        for ((total, parts) in chunks) {
            if ((1..total).all { it in parts }) {
                numericToJws((1..total).joinToString("") { parts.getValue(it) })?.let(jws::add)
            } else {
                incomplete += 1
            }
        }
        val decoded = jws.distinct().map(::decode)
        return CardReading(decoded.filterNotNull(), incomplete, decoded.count { it == null })
    }

    /** The FHIR entries of [cards], each grouped under its issuer and tagged as not verified. */
    fun entries(cards: List<SmartHealthCard>): List<FhirEntry> = cards.flatMap { card ->
        val origin = FhirOrigin(
            baseUri = card.issuer.trimEnd('/'),
            displayName = runCatching { URI(card.issuer).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: card.issuer,
            fhirVersion = card.fhirVersion?.takeIf { it == FhirVersionDetector.R4 || it == FhirVersionDetector.R4B } ?: FhirVersionDetector.R4,
        )
        card.bundle.objects("entry").mapNotNull { entry ->
            val resource = entry.obj("resource") ?: return@mapNotNull null
            FhirEntry(resource = tagged(resource), fullUrl = entry.string("fullUrl"), origin = origin)
        }
    }

    /** Whether a record came from a card whose signature was not checked. */
    fun isUnverified(resource: JsonObject): Boolean =
        resource.obj("meta")?.objects("tag").orEmpty().any { it.string("system") == OpenVitalsFhir.TagSystem && it.string("code") == UnverifiedTag }

    /** Each pair of digits is one character, its code minus 45. Null for digits that do not pair up. */
    fun numericToJws(digits: String): String? {
        if (digits.length % 2 != 0) return null
        return buildString(digits.length / 2) {
            for (index in digits.indices step 2) {
                val code = digits.substring(index, index + 2).toInt() + CharOffset
                append(code.toChar())
            }
        }
    }

    private fun fileCredentials(text: String): List<String> =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()?.strings("verifiableCredential").orEmpty()

    /** Null when the JWS is not a card: bad Base64, a payload that does not inflate, or no Bundle. */
    private fun decode(jws: String): SmartHealthCard? = runCatching {
        val (headerPart, payloadPart) = jws.split('.').also { require(it.size == JwsParts) }
        val header = json.parseToJsonElement(base64Url(headerPart).decodeToString()) as JsonObject
        val raw = base64Url(payloadPart)
        val payload = if (header.string("zip") == "DEF") inflateRaw(raw) else raw
        val claims = json.parseToJsonElement(payload.decodeToString()) as JsonObject
        val subject = claims.obj("vc")?.obj("credentialSubject") ?: return null
        val bundle = subject.obj("fhirBundle")?.takeIf { it.string("resourceType") == "Bundle" } ?: return null
        SmartHealthCard(claims.string("iss") ?: return null, subject.string("fhirVersion"), bundle)
    }.getOrNull()

    private fun base64Url(part: String): ByteArray = Base64.getUrlDecoder().decode(part)

    /** A card is small; anything that inflates past the file limit is not one. */
    private fun inflateRaw(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BufferBytes)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                output.write(buffer, 0, count)
                require(output.size() <= FhirFileParser.MaxFileBytes)
            }
            return output.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private fun tagged(resource: JsonObject): JsonObject {
        val meta = resource.obj("meta") ?: JsonObject(emptyMap())
        val tag = buildJsonObject {
            put("system", OpenVitalsFhir.TagSystem)
            put("code", UnverifiedTag)
            put("display", UnverifiedDisplay)
        }
        val tags = JsonArray(meta.objects("tag") + tag)
        return resource.with("meta", meta.with("tag", tags))
    }

    private const val CharOffset = 45
    private const val JwsParts = 3
    private const val BufferBytes = 8 * 1024
}
