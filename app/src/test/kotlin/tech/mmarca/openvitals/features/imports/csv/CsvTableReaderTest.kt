package tech.mmarca.openvitals.features.imports.csv

import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTableReaderTest {

    private val reader = CsvTableReader()

    private fun sourceOf(content: String): CsvInputSource =
        CsvInputSource { ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)) }

    private val commaLf = CsvDialect(fieldDelimiter = ",", eol = "\n")

    // sniffDialect.

    @Test
    fun `a comma file with quoted headers is sniffed as comma-delimited`() {
        val source = sourceOf(
            "Date,\"Weight (kg)\",\"Fat mass (kg)\",Comments\n2026-07-01,78.4,15.2,\n",
        )

        assertEquals(",", reader.sniffDialect(source).fieldDelimiter)
    }

    @Test
    fun `a semicolon file whose quoted headers contain commas is sniffed as semicolon-delimited`() {
        // The commas are inside quotes; counting them would pick the wrong delimiter.
        val source = sourceOf(
            "Datum;\"Gewicht (kg), netto\";\"Fett (kg), gesamt\"\n2026-07-01;78,4;15,2\n",
        )

        assertEquals(";", reader.sniffDialect(source).fieldDelimiter)
    }

    @Test
    fun `a CRLF file is sniffed as CRLF`() {
        val source = sourceOf("Date,Weight\r\n2026-07-01,78.4\r\n")

        assertEquals("\r\n", reader.sniffDialect(source).eol)
    }

    @Test
    fun `an LF file is sniffed as LF`() {
        // Getting this wrong returns one row with the whole file in the last field, so the line ending is sniffed.
        val source = sourceOf("Date,Weight\n2026-07-01,78.4\n")

        assertEquals("\n", reader.sniffDialect(source).eol)
    }

    // sample.

    @Test
    fun `a quoted header containing a comma reads as a single column`() {
        val sample = reader.sample(
            sourceOf("Date,\"Weight (kg)\",\"Fat mass (kg)\",Comments\n2026-07-01,78.4,15.2,\n"),
        )

        assertEquals(listOf("Date", "Weight (kg)", "Fat mass (kg)", "Comments"), sample.headerRow)
        assertEquals(4, sample.columnCount)
    }

    @Test
    fun `a UTF-8 BOM does not leak into the first header cell`() {
        val sample = reader.sample(sourceOf("﻿Date,Weight\n2026-07-01,78.4\n"))

        assertEquals("Date", sample.headerRow.first())
    }

    @Test
    fun `a quoted field containing newlines survives a chunk boundary intact`() = runTest {
        // The interesting row sits past the 64 KB sniff boundary, so state must carry across refills.
        val buffer = StringBuilder("Date,Weight,Comments\n")
        repeat(4000) { i ->
            buffer.append("2026-07-01,78.4,filler row $i padding padding padding\n")
        }
        buffer.append("2026-07-02,79.0,\"multi\nline\ncomment, with comma\"\n")

        val rows = reader.rows(sourceOf(buffer.toString()), dialect = commaLf).toList()

        assertEquals(4001, rows.size)
        assertEquals(3, rows.last().fields.size)
        assertEquals("multi\nline\ncomment, with comma", rows.last().fields[2])
    }

    @Test
    fun `sampling a file with thousands of rows stops at the preview limit`() {
        val buffer = StringBuilder("Date,Weight\n")
        repeat(5000) { i -> buffer.append("2026-07-01,7$i\n") }

        val sample = reader.sample(sourceOf(buffer.toString()))

        assertEquals(CSV_PREVIEW_ROWS, sample.dataRows.size)
    }

    @Test
    fun `a file with no header row gets synthesised column labels`() {
        val sample = reader.sample(
            sourceOf("2026-07-01,78.4\n2026-07-02,79.0\n"),
            hasHeaderRow = false,
        )

        assertEquals(listOf("Column 1", "Column 2"), sample.headerRow)
        assertEquals(2, sample.dataRows.size)
    }

    @Test
    fun `a file containing only a header row samples as empty`() {
        val sample = reader.sample(sourceOf("Date,Weight\n"))

        assertTrue(sample.isEmpty)
    }

    @Test
    fun `columnValues skips blank cells in the requested column`() {
        val sample = reader.sample(
            sourceOf("Date,Fat\n2026-07-01,15.2\n2026-07-02,\n2026-07-03,15.4\n"),
        )

        assertEquals(listOf("15.2", "15.4"), sample.columnValues(1))
    }

    // rows.

    @Test
    fun `the header row is not emitted as data`() = runTest {
        val rows = reader.rows(sourceOf("Date,Weight\n2026-07-01,78.4\n"), dialect = commaLf).toList()

        assertEquals(1, rows.size)
        assertEquals(listOf("2026-07-01", "78.4"), rows.single().fields)
        // 1-based and counted over the file, so a diagnostic names the line the user opens.
        assertEquals(2, rows.single().rowNumber)
    }

    @Test
    fun `bytes read grow as rows are emitted`() = runTest {
        val buffer = StringBuilder("Date,Weight\n")
        repeat(2000) { i -> buffer.append("2026-07-01,7$i\n") }
        val content = buffer.toString()

        val rows = reader.rows(sourceOf(content), dialect = commaLf).toList()

        val totalBytes = content.toByteArray(Charsets.UTF_8).size.toLong()
        assertTrue(rows.last().bytesRead > 0)
        assertTrue(rows.last().bytesRead <= totalBytes)
        // The numerator has to grow as the file is consumed, or the bar pins at 0%.
        assertTrue(rows.last().bytesRead > rows.first().bytesRead)
        val readings = rows.map { it.bytesRead }
        assertEquals(readings.sorted(), readings)
        // And it reaches the end of the file, so the bar finishes at 100%.
        assertEquals(totalBytes, rows.last().bytesRead)
    }

    @Test
    fun `a missing file reports a read failure rather than hanging`() = runTest {
        val missing = CsvInputSource { null }

        assertThrows(CsvReadException::class.java) {
            kotlinx.coroutines.runBlocking { reader.rows(missing, dialect = commaLf).toList() }
        }
    }

    @Test
    fun `a missing file fails the sample too`() {
        assertThrows(CsvReadException::class.java) {
            reader.sample(CsvInputSource { null })
        }
    }

    // CsvRow cell.

    @Test
    fun `a short row reports null rather than throwing`() {
        val row = CsvRow(rowNumber = 2, fields = listOf("2026-07-01", "78.4"))

        assertNull(row.cell(5))
        assertEquals("78.4", row.cell(1))
    }

    @Test
    fun `a blank cell reads as null so a gap is not parsed as zero`() {
        val row = CsvRow(rowNumber = 2, fields = listOf("2026-07-01", "   "))

        assertNull(row.cell(1))
    }
}
