package tech.mmarca.openvitals.features.imports.csv

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A fresh stream over the picked file, or null when it cannot be opened.
 *
 * The reader takes this rather than a path because the file arrives through the
 * Storage Access Framework (`ContentResolver.openInputStream`) — there is no
 * filesystem path, and the file is never held in memory.
 */
fun interface CsvInputSource {
    fun open(): InputStream?
}

/** How many data rows the mapping screen samples. The stream is closed after
 * this, so picking a 400 MB export still opens instantly. */
const val CSV_PREVIEW_ROWS = 50

/** Delimiters worth guessing between. Semicolon matters: it is what a European
 * locale's spreadsheet exports, because the comma is its decimal separator. */
val CsvFieldDelimiters: List<String> = listOf(",", ";", "\t", "|")

/**
 * The separator and line ending a file actually uses.
 *
 * Both must be right. The tokenizer does not fail on a wrong line ending — it
 * returns ONE row with the entire file crammed into the last field, which
 * downstream looks like a file with a single unparsable row rather than a
 * mis-sniffed dialect. [CsvTableReader] therefore sniffs it rather than
 * assuming, and [CsvSample.looksMisparsed] catches the case anyway.
 */
data class CsvDialect(
    val fieldDelimiter: String,
    val eol: String,
)

/** What the mapping screen needs: the head of the file, already tokenised. */
data class CsvSample(
    val dialect: CsvDialect,
    /** The header cells, or synthesised `Column 1..n` labels when [hasHeaderRow] is false. */
    val headerRow: List<String>,
    /** Up to [CSV_PREVIEW_ROWS] data rows. */
    val dataRows: List<List<String>>,
    val hasHeaderRow: Boolean,
) {
    val columnCount: Int get() = headerRow.size

    val isEmpty: Boolean get() = headerRow.isEmpty() || dataRows.isEmpty()

    /**
     * A single very wide row whose cells contain line breaks — the signature of a
     * mis-sniffed line ending, which otherwise reads as "one unparsable row".
     */
    val looksMisparsed: Boolean
        get() = dataRows.isEmpty() && headerRow.size <= 2 && headerRow.any { "\n" in it }

    /** The values of column [index] across the sampled rows, skipping blanks. */
    fun columnValues(index: Int): List<String> =
        dataRows.mapNotNull { row ->
            row.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
        }
}

/**
 * Thrown when a picked file cannot be read at all. The view-model turns it into
 * a screen error; nothing below that layer catches it.
 */
class CsvReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * One tokenised data row and its 1-based line number in the file, so a
 * diagnostic can name the row the user has to go look at.
 */
data class CsvRow(
    val rowNumber: Int,
    val fields: List<String>,
    /**
     * Raw bytes consumed from the file by the time this row was emitted, for a
     * determinate progress bar. Approximate by however much the decoder buffered.
     */
    val bytesRead: Long = 0,
) {
    /** The trimmed cell at [index], or null when the row is too short or blank there. */
    fun cell(index: Int): String? {
        val value = fields.getOrNull(index)?.trim() ?: return null
        return value.ifEmpty { null }
    }
}

/** Reads CSV files. Injected so tests drive it off an in-memory stream rather than a picker. */
class CsvTableReader {

    /**
     * Guesses the dialect from the first chunk of [source].
     *
     * The delimiter is whichever candidate appears most often OUTSIDE quotes on
     * the first line — counting inside quotes would pick the comma out of
     * `"Weight (kg)","Fat mass (kg)"` in a semicolon file. The line ending is
     * CRLF when the first break is preceded by a carriage return.
     */
    fun sniffDialect(source: CsvInputSource): CsvDialect {
        val head = readHead(source)
        if (head.isEmpty()) {
            return CsvDialect(fieldDelimiter = ",", eol = "\n")
        }

        val firstBreak = head.indexOf('\n')
        val eol = if (firstBreak > 0 && head[firstBreak - 1] == '\r') "\r\n" else "\n"
        val firstLine = if (firstBreak < 0) head else head.substring(0, firstBreak).trimEnd()

        var best = ","
        var bestCount = 0
        for (candidate in CsvFieldDelimiters) {
            val count = countOutsideQuotes(firstLine, candidate)
            if (count > bestCount) {
                best = candidate
                bestCount = count
            }
        }
        return CsvDialect(fieldDelimiter = best, eol = eol)
    }

    /** Reads the header plus up to [maxRows] data rows, then stops reading. */
    fun sample(
        source: CsvInputSource,
        dialect: CsvDialect? = null,
        hasHeaderRow: Boolean = true,
        maxRows: Int = CSV_PREVIEW_ROWS,
    ): CsvSample {
        val resolved = dialect ?: sniffDialect(source)
        val rows = mutableListOf<List<String>>()
        // +1 for the header, which is not a data row.
        val limit = maxRows + if (hasHeaderRow) 1 else 0
        withTokenizer(source, resolved) { tokenizer ->
            while (rows.size < limit) {
                rows += tokenizer.nextRow() ?: break
            }
        }

        if (rows.isEmpty()) {
            return CsvSample(
                dialect = resolved,
                headerRow = emptyList(),
                dataRows = emptyList(),
                hasHeaderRow = hasHeaderRow,
            )
        }

        val width = rows.maxOf { it.size }
        val header = if (hasHeaderRow) {
            padded(rows.first(), width)
        } else {
            (1..width).map { "Column $it" }
        }
        val data = if (hasHeaderRow) rows.drop(1) else rows

        return CsvSample(
            dialect = resolved,
            headerRow = header,
            dataRows = data,
            hasHeaderRow = hasHeaderRow,
        )
    }

    /**
     * Every data row in [source], in file order, with the byte offset reached so
     * far so a caller can show determinate progress without counting rows first.
     *
     * The header row is dropped when [hasHeaderRow]. Rows are emitted as they are
     * tokenised; the file is never held in memory.
     */
    fun rows(
        source: CsvInputSource,
        dialect: CsvDialect,
        hasHeaderRow: Boolean = true,
    ): Flow<CsvRow> = flow {
        val stream = source.open() ?: throw CsvReadException("Unable to open the file.")
        try {
            val tokenizer = CsvTokenizer(stream, dialect)
            var rowNumber = 0
            while (true) {
                val fields = tokenizer.nextRow() ?: break
                rowNumber++
                if (hasHeaderRow && rowNumber == 1) continue
                emit(CsvRow(rowNumber = rowNumber, fields = fields, bytesRead = tokenizer.bytesRead))
            }
        } catch (error: IOException) {
            throw CsvReadException(error.message ?: "Unable to read the file.", error)
        } finally {
            runCatching { stream.close() }
        }
    }

    private inline fun withTokenizer(
        source: CsvInputSource,
        dialect: CsvDialect,
        block: (CsvTokenizer) -> Unit,
    ) {
        val stream = source.open() ?: throw CsvReadException("Unable to open the file.")
        try {
            block(CsvTokenizer(stream, dialect))
        } catch (error: IOException) {
            throw CsvReadException(error.message ?: "Unable to read the file.", error)
        } finally {
            runCatching { stream.close() }
        }
    }

    private fun readHead(source: CsvInputSource, bytes: Int = 64 * 1024): String {
        val stream = source.open() ?: throw CsvReadException("Unable to open the file.")
        return try {
            val buffer = ByteArray(bytes)
            var total = 0
            while (total < bytes) {
                val read = stream.read(buffer, total, bytes - total)
                if (read < 0) break
                total += read
            }
            String(buffer, 0, total, Charsets.UTF_8).removePrefix(Utf8ByteOrderMark)
        } catch (error: IOException) {
            throw CsvReadException(error.message ?: "Unable to read the file.", error)
        } finally {
            runCatching { stream.close() }
        }
    }
}

/**
 * A pull-based RFC 4180 tokenizer over one stream: quoted fields, `""` escapes,
 * embedded newlines and delimiters inside quotes, and a configurable delimiter
 * and line ending. Every cell stays a String — the column's interpretation
 * decides how to read it, and `4,5` in a semicolon file must not become a list.
 */
private class CsvTokenizer(stream: InputStream, dialect: CsvDialect) {
    private val counting = CountingInputStream(stream)

    // The default decoder replaces malformed bytes, so one bad byte cannot kill
    // an otherwise fine export.
    private val reader = InputStreamReader(counting, Charsets.UTF_8)

    private val delimiter = dialect.fieldDelimiter[0]
    private val eol = dialect.eol
    private var pushback = -1
    private var first = true
    private var exhausted = false

    /** Raw bytes consumed so far, counted before decoding. */
    val bytesRead: Long get() = counting.bytesRead

    /** The next row, or null at the end of the file. */
    fun nextRow(): List<String>? {
        if (exhausted) return null
        val field = StringBuilder()
        val fields = mutableListOf<String>()
        var inQuotes = false
        var fieldWasQuoted = false
        var anyContent = false

        fun endField() {
            fields += field.toString()
            field.setLength(0)
            fieldWasQuoted = false
        }

        while (true) {
            val c = read()
            if (c < 0) {
                exhausted = true
                // A final row without a trailing line ending still counts.
                if (!anyContent && fields.isEmpty() && field.isEmpty()) return null
                endField()
                return fields
            }

            // Strip a UTF-8 BOM so it cannot leak into the first header cell.
            if (first && c == 0xFEFF) {
                first = false
                continue
            }
            first = false

            if (inQuotes) {
                if (c == '"'.code) {
                    val next = read()
                    if (next == '"'.code) {
                        field.append('"')
                    } else {
                        inQuotes = false
                        if (next >= 0) pushback = next
                    }
                } else {
                    field.append(c.toChar())
                }
                anyContent = true
                continue
            }

            when {
                c == '"'.code && field.isEmpty() && !fieldWasQuoted -> {
                    inQuotes = true
                    fieldWasQuoted = true
                    anyContent = true
                }
                c.toChar() == delimiter -> {
                    endField()
                    anyContent = true
                }
                c.toChar() == eol[0] -> {
                    if (eol.length == 2) {
                        val next = read()
                        if (next >= 0 && next.toChar() == eol[1]) {
                            endField()
                            return fields
                        }
                        // Not the configured line ending: both characters are data.
                        field.append(c.toChar())
                        if (next >= 0) pushback = next
                        anyContent = true
                    } else {
                        endField()
                        return fields
                    }
                }
                else -> {
                    field.append(c.toChar())
                    anyContent = true
                }
            }
        }
    }

    // One-character pushback so multi-character tokens (CRLF, `""` escapes) can
    // be matched without a full lookahead buffer.
    private fun read(): Int {
        if (pushback >= 0) {
            val value = pushback
            pushback = -1
            return value
        }
        return reader.read()
    }
}

/** A raw-byte tally taken before decoding, shared with the row emitter. */
private class CountingInputStream(private val delegate: InputStream) : InputStream() {
    var bytesRead: Long = 0
        private set

    override fun read(): Int {
        val value = delegate.read()
        if (value >= 0) bytesRead++
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = delegate.read(b, off, len)
        if (read > 0) bytesRead += read
        return read
    }

    override fun close() = delegate.close()
}

private fun padded(row: List<String>, width: Int): List<String> =
    row + (row.size until width).map { "Column ${it + 1}" }

private fun countOutsideQuotes(line: String, needle: String): Int {
    var count = 0
    var inQuotes = false
    for (index in line.indices) {
        val char = line[index]
        if (char == '"') {
            inQuotes = !inQuotes
        } else if (!inQuotes && line.startsWith(needle, index)) {
            count++
        }
    }
    return count
}

/**
 * The UTF-8 byte-order mark, as an escape rather than the character itself.
 *
 * Spreadsheet exports routinely start with one, and left in place it becomes
 * part of the first column's header so no mapping matches it. Written literally
 * the file itself carries a stray BOM mid-source, which is both invisible in a
 * diff and something tooling flags.
 */
private const val Utf8ByteOrderMark = "\uFEFF"
