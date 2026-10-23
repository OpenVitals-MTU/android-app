package tech.mmarca.openvitals.domain.medical

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tech.mmarca.openvitals.core.privacy.PrivacyRedactor
import tech.mmarca.openvitals.domain.medical.cda.Hl7Namespace
import tech.mmarca.openvitals.domain.medical.cda.isClinicalDocument
import tech.mmarca.openvitals.domain.medical.cda.parseCda
import tech.mmarca.openvitals.domain.medical.shc.CardFileKind
import tech.mmarca.openvitals.domain.medical.shc.CardFileType

/**
 * What a file holds, for the report of an import that did not go through: its kind, and for
 * JSON its shape and record types. Never a value from it: the report is meant to be shared,
 * and a medical file is made of names, dates and results. Field names and resource types are
 * the file's structure, not its content. A zip lists each of its files the same way, by its
 * place in the zip and its extension: a file's name can be a patient's.
 */
object MedicalFileInspector {

    /** One line for the picked file, or a line for the zip and one for each file in it. */
    fun inspect(open: () -> InputStream): List<String> {
        val zipped = open().use { it.read() == 'P'.code && it.read() == 'K'.code }
        return if (zipped) inspectZip(open) else listOf(open().use { sample(it) }.description())
    }

    /** One file's content, in words. */
    fun describe(bytes: ByteArray): String = signature(bytes) ?: text(bytes)

    private fun inspectZip(open: () -> InputStream): List<String> {
        val lines = mutableListOf<String>()
        var files = 0
        val stopped = runCatching {
            ZipInputStream(BufferedInputStream(open())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        files++
                        if (files <= MaxListedFiles) lines += "  file $files${extensionOf(entry.name)}: ${sample(zip).description()}"
                    }
                    zip.closeEntry()
                }
            }
        }.exceptionOrNull()
        if (files > MaxListedFiles) lines += "  and ${count(files - MaxListedFiles, "more file")}"
        stopped?.let { lines += "  The zip could not be read past this point: ${it::class.simpleName}" }
        return listOf("A zip of ${count(files, "file")}") + lines
    }

    /** The first [FhirFileParser.MaxFileBytes] of [input], which is all the import reads, and its whole size. */
    private fun sample(input: InputStream): Sample {
        val head = ByteArrayOutputStream()
        val buffer = ByteArray(BufferBytes)
        var size = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            val room = FhirFileParser.MaxFileBytes - size
            if (room > 0) head.write(buffer, 0, minOf(read.toLong(), room).toInt())
            size += read
        }
        return Sample(head.toByteArray(), size)
    }

    private class Sample(val head: ByteArray, val size: Long)

    private fun Sample.description(): String =
        if (head.size.toLong() == size) {
            "$size bytes, ${describe(head)}"
        } else {
            "$size bytes, ${signature(head) ?: "text"}, larger than the ${FhirFileParser.MaxFileBytes / Megabyte} MB the import reads"
        }

    /** The kinds a file's first bytes give away. Null for text, which needs reading. */
    private fun signature(bytes: ByteArray): String? = when {
        bytes.isEmpty() -> "empty"
        bytes.startsWith(0x50, 0x4B) -> "a zip"
        bytes.startsWith(0x1F, 0x8B) -> "gzip-compressed, which the import does not open"
        bytes.startsWith(0xFE, 0xFF) || bytes.startsWith(0xFF, 0xFE) -> "UTF-16 text, which the import does not read: it reads UTF-8"
        else -> when (CardFileType.of(bytes).kind) {
            CardFileKind.PDF -> "a PDF"
            CardFileKind.IMAGE -> "an image"
            null -> if (bytes.take(BinarySniffBytes).any { it == 0.toByte() }) "binary data" else null
        }
    }

    private fun text(bytes: ByteArray): String {
        val text = bytes.decodeFhirText()
        if (text.isBlank()) return "blank text"
        if (text.trimStart().startsWith("<")) return xml(bytes)
        val whole = runCatching { json.parseToJsonElement(text) }
        whole.getOrNull()?.let { return json(it) }
        return ndjson(text) ?: "not JSON${position(text, whole.exceptionOrNull())}"
    }

    private fun xml(bytes: ByteArray): String {
        val root = parseCda(bytes) ?: return "XML that is not well-formed"
        return when {
            isClinicalDocument(root) -> "a CDA document"
            root.namespaceURI == FhirXmlNamespace -> "FHIR ${root.localName} in XML, which the import does not read: it reads FHIR JSON"
            else -> "XML with root element ${root.localName}" + (root.namespaceURI?.let { " ($it)" } ?: "") +
                if (root.namespaceURI == Hl7Namespace) ", not a CDA document" else ""
        }
    }

    private fun json(element: JsonElement): String = when (element) {
        is JsonObject -> if (element.resourceType != null) fhir(element) else "a JSON object with no resourceType, with fields ${fields(element)}"
        is JsonArray -> "a JSON array of ${element.size}: ${items(element)}"
        else -> "a single JSON value, not an object"
    }

    private fun fhir(root: JsonObject): String {
        if (root.resourceType != "Bundle") return "a FHIR ${root.resourceType}"
        val entries = root.objects("entry")
        val resources = entries.mapNotNull { it.obj("resource")?.takeIf { resource -> resource.resourceType != null } }
        val type = root.string("type")?.let { " ($it)" }.orEmpty()
        val empty = (entries.size - resources.size).takeIf { it > 0 }?.let { ", and ${count(it, "entry", "entries")} with no record" }.orEmpty()
        return "a FHIR Bundle$type of ${count(resources.size, "record")}${counts(resources)}$empty"
    }

    /** What a top-level array holds: FHIR's JSON is an object, so even an array of records is not read. */
    private fun items(array: JsonArray): String {
        val objects = array.filterIsInstance<JsonObject>()
        val (records, others) = objects.partition { it.resourceType != null }
        return buildList {
            if (records.isNotEmpty()) add("${count(records.size, "record")}${counts(records)}")
            others.firstOrNull()?.let { add("${count(others.size, "object")} with no resourceType, the first with fields ${fields(it)}") }
            (array.size - objects.size).takeIf { it > 0 }?.let { add("${count(it, "value")} that are not objects") }
        }.joinToString("; ").ifEmpty { "nothing" }
    }

    /** NDJSON, when the first line is an object: either every line is a record, or the first that is not. */
    private fun ndjson(text: String): String? {
        val lines = text.lines().withIndex().filter { it.value.isNotBlank() }
        if (lines.size < 2 || runCatching { json.parseToJsonElement(lines.first().value) as? JsonObject }.getOrNull() == null) return null
        val records = mutableListOf<JsonObject>()
        lines.forEach { (index, line) ->
            val record = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull()?.takeIf { it.resourceType != null }
                ?: return "NDJSON whose line ${index + 1} is not a FHIR record"
            records += record
        }
        return "NDJSON of ${count(records.size, "record")}${counts(records)}"
    }

    /** Where the JSON parser stopped. Its own message quotes the file, so only the offset is kept. */
    private fun position(text: String, failure: Throwable?): String {
        val offset = failure?.message?.let { OffsetPattern.find(it) }?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, text.length)
            ?: return ""
        val line = text.substring(0, offset).count { it == '\n' } + 1
        val column = offset - (text.lastIndexOf('\n', offset - 1) + 1) + 1
        return ": it stops at line $line, column $column"
    }

    private fun counts(records: List<JsonObject>): String {
        if (records.isEmpty()) return ""
        val counts = records.groupingBy { it.resourceType.orEmpty() }.eachCount().entries.sortedByDescending { it.value }
        val shown = counts.take(MaxListedTypes).joinToString { "${it.key} ${it.value}" }
        return ": $shown" + if (counts.size > MaxListedTypes) ", and ${counts.size - MaxListedTypes} more types" else ""
    }

    private fun count(n: Int, one: String, many: String = "${one}s"): String = "$n ${if (n == 1) one else many}"

    // A key is usually a field name, but a map can key by a date or an id.
    private fun fields(obj: JsonObject): String =
        obj.keys.take(MaxListedFields).joinToString { PrivacyRedactor.maskMessage(it).orEmpty() } +
            if (obj.size > MaxListedFields) ", and ${obj.size - MaxListedFields} more" else ""

    /** " (.json)" for a short, plain extension; nothing for one that could say more. */
    private fun extensionOf(name: String): String =
        name.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
            ?.let { " (.${it.lowercase()})" }
            .orEmpty()

    private fun ByteArray.startsWith(vararg signature: Int): Boolean =
        size >= signature.size && signature.indices.all { (this[it].toInt() and 0xFF) == signature[it] }

    private val json = Json { isLenient = false }
    private val OffsetPattern = Regex("""at offset (\d+)""")
    private const val FhirXmlNamespace = "http://hl7.org/fhir"
    private const val MaxListedFiles = 200
    private const val MaxListedTypes = 12
    private const val MaxListedFields = 10
    private const val BinarySniffBytes = 512
    private const val BufferBytes = 8 * 1024
    private const val Megabyte = 1024 * 1024
}
