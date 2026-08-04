package tech.mmarca.openvitals.features.imports.garmin

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.FitHrvReading

class FitHrvImportServiceTest {

    private val writePermission = HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun healthRepo(granted: Set<String> = setOf(writePermission)): HealthRepository =
        mockk<HealthRepository>().also { coEvery { it.grantedPermissions() } returns granted }

    private fun reading(secondsOffset: Long = 0) =
        FitHrvReading(time = Instant.parse("2026-07-01T02:00:00Z").plusSeconds(secondsOffset), rmssdMillis = 62.5)

    @Test fun `all files land in one insert call`() = runTest {
        val importRepository = mockk<AppleHealthImportRepository>()
        coEvery { importRepository.insertImportedRecords(any()) } returns Unit
        val service = FitHrvImportService(importRepository, healthRepo())

        val outcome = service.writeFiles(listOf(listOf(reading(0)), listOf(reading(86_400))))

        assertEquals(2, outcome.importedFiles)
        assertEquals(0, outcome.failedFiles)
        coVerify(exactly = 1) { importRepository.insertImportedRecords(match { it.size == 2 }) }
    }

    @Test fun `a duplicate rejection counts as imported because the id is deterministic`() = runTest {
        val importRepository = mockk<AppleHealthImportRepository>()
        coEvery { importRepository.insertImportedRecords(match { it.size == 2 }) } throws
            IllegalStateException("A record with this clientRecordId already exists")
        coEvery { importRepository.insertImportedRecords(match { it.size == 1 }) } throws
            IllegalStateException("A record with this clientRecordId already exists")
        val service = FitHrvImportService(importRepository, healthRepo())

        val outcome = service.writeFiles(listOf(listOf(reading(0)), listOf(reading(86_400))))

        assertEquals(2, outcome.importedFiles)
        assertEquals(0, outcome.failedFiles)
    }

    @Test fun `a rate limit stops the run without blaming unattempted files`() = runTest {
        val importRepository = mockk<AppleHealthImportRepository>()
        coEvery { importRepository.insertImportedRecords(any()) } throws
            IllegalStateException("Quota has been exceeded")
        val service = FitHrvImportService(importRepository, healthRepo())

        val outcome = service.writeFiles(listOf(listOf(reading(0))))

        assertTrue(outcome.rateLimited)
        assertEquals(0, outcome.importedFiles)
        assertEquals(0, outcome.failedFiles)
    }

    @Test fun `missing write permission throws before any insert`() = runTest {
        val importRepository = mockk<AppleHealthImportRepository>()
        val service = FitHrvImportService(importRepository, healthRepo(granted = emptySet()))

        assertThrows(SecurityException::class.java) {
            kotlinx.coroutines.runBlocking { service.writeFiles(listOf(listOf(reading()))) }
        }
        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
    }
}
