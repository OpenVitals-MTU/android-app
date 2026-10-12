package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedRecordWritesTest {

    private val t0 = Instant.parse("2026-10-07T10:00:00Z")
    private val present = mutableSetOf("b")
    private val failing = mutableSetOf<String>()
    private val calls = mutableListOf<List<String>>()
    private val repository = mockk<AppleHealthImportRepository> {
        coEvery { insertImportedRecords(any()) } answers {
            val ids = firstArg<List<Record>>().map { it.metadata.clientRecordId!! }
            calls += ids
            if (ids.any { it in present }) throw IllegalStateException("clientRecordId already exists")
            if (ids.any { it in failing }) throw IllegalArgumentException("bad record")
            present += ids
        }
    }

    private fun record(id: String, minute: Long) = StepsRecord(
        startTime = t0.plusSeconds(60 * minute),
        startZoneOffset = ZoneOffset.UTC,
        endTime = t0.plusSeconds(60 * minute + 60),
        endZoneOffset = ZoneOffset.UTC,
        count = 10,
        metadata = Metadata.manualEntry(clientRecordId = id, clientRecordVersion = 1),
    )

    @Test
    fun `a batch refused for one present record is written one by one`() = runTest {
        val outcomes = repository.insertWithOutcomes(listOf(record("a", 0), record("b", 1), record("c", 2)))

        assertEquals(listOf(RecordWrite.WRITTEN, RecordWrite.PRESENT, RecordWrite.WRITTEN), outcomes)
        assertEquals(listOf(listOf("a", "b", "c"), listOf("a"), listOf("b"), listOf("c")), calls)
        assertEquals(setOf("a", "b", "c"), present)
    }

    @Test
    fun `a single present record is not tried twice`() = runTest {
        assertEquals(0, repository.insertSkippingPresent(listOf(record("b", 1))))
        assertEquals(1, calls.size)
    }

    @Test
    fun `another failure is thrown, unless the caller takes it`() = runTest {
        failing += "c"
        val thrown = runCatching { repository.insertSkippingPresent(listOf(record("c", 2))) }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)

        val failed = mutableListOf<String>()
        val outcomes = repository.insertWithOutcomes(listOf(record("b", 1), record("c", 2))) { record, _ ->
            failed += record.metadata.clientRecordId!!
        }
        assertEquals(listOf(RecordWrite.PRESENT, RecordWrite.FAILED), outcomes)
        assertEquals(listOf("c"), failed)
    }
}
