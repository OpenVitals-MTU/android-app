package tech.mmarca.openvitals.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleDayLogWrite
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.DayBleedingChoice
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry

/**
 * A day-log save touches only the record kinds whose value changed, and only
 * the app's own records. Another app's record is context, never a target.
 */
class CycleDayLogWriterTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 8, 5)
    private val now = today.atTime(LocalTime.of(18, 30)).atZone(zone).toInstant()
    private val repository = mockk<CycleRepository>(relaxed = true).also { repo ->
        coEvery { repo.writeCycleEntry(any()) } returns "new"
    }
    private val writer = CycleDayLogWriter(repository, zone) { now }

    @Test
    fun `an unchanged kind is not written, updated or deleted`() = runTest {
        val existing = CycleDayLog(date = today, ownFlow = ownFlow(CycleRecordValues.FLOW_MEDIUM))

        writer.write(existing, write(bleeding = DayBleedingChoice.Flow(CycleRecordValues.FLOW_MEDIUM)))

        coVerify(exactly = 0) { repository.writeCycleEntry(any()) }
        coVerify(exactly = 0) { repository.updateCycleEntry(any(), any()) }
        coVerify(exactly = 0) { repository.deleteCycleEntry(any(), any()) }
    }

    @Test
    fun `a changed level updates the app's own record in place, keeping its time`() = runTest {
        val existing = CycleDayLog(date = today, ownFlow = ownFlow(CycleRecordValues.FLOW_LIGHT))
        val request = slot<CycleEntryWriteRequest>()

        writer.write(existing, write(bleeding = DayBleedingChoice.Flow(CycleRecordValues.FLOW_HEAVY)))

        coVerify(exactly = 1) { repository.updateCycleEntry("own-flow", capture(request)) }
        assertEquals(CycleRecordValues.FLOW_HEAVY, request.captured.flow)
        assertEquals(existing.ownFlow?.time, request.captured.time)
        coVerify(exactly = 0) { repository.writeCycleEntry(any()) }
    }

    @Test
    fun `another app's flow is never modified, the app inserts its own record`() = runTest {
        val existing = CycleDayLog(date = today, foreignFlowLevel = CycleRecordValues.FLOW_MEDIUM)
        val request = slot<CycleEntryWriteRequest>()

        writer.write(existing, write(bleeding = DayBleedingChoice.Flow(CycleRecordValues.FLOW_LIGHT)))

        coVerify(exactly = 1) { repository.writeCycleEntry(capture(request)) }
        assertEquals(CycleEntryKind.MENSTRUATION_FLOW, request.captured.kind)
        coVerify(exactly = 0) { repository.updateCycleEntry(any(), any()) }
        coVerify(exactly = 0) { repository.deleteCycleEntry(any(), any()) }
    }

    @Test
    fun `a cleared value deletes the app's own record`() = runTest {
        val existing = CycleDayLog(date = today, ownFlow = ownFlow(CycleRecordValues.FLOW_MEDIUM))

        writer.write(existing, write(bleeding = null))

        coVerify(exactly = 1) { repository.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "own-flow") }
        coVerify(exactly = 0) { repository.writeCycleEntry(any()) }
    }

    @Test
    fun `a new record is stamped now for today and noon for a past day`() = runTest {
        val yesterday = today.minusDays(1)
        val requests = mutableListOf<CycleEntryWriteRequest>()

        writer.write(CycleDayLog(date = today), write(bleeding = DayBleedingChoice.Spotting))
        writer.write(CycleDayLog(date = yesterday), write(date = yesterday, bleeding = DayBleedingChoice.Spotting))

        coVerify(exactly = 2) { repository.writeCycleEntry(capture(requests)) }
        assertEquals(now, requests[0].time)
        assertEquals(yesterday.atTime(LocalTime.NOON).atZone(zone).toInstant(), requests[1].time)
    }

    @Test
    fun `a temperature that only moved its time is updated, not re-inserted`() = runTest {
        val measuredAt = today.atTime(LocalTime.of(7, 0)).atZone(zone).toInstant()
        val existing = CycleDayLog(
            date = today,
            ownBasalBodyTemperature = BasalBodyTemperatureEntry(
                time = measuredAt,
                temperatureCelsius = 36.6,
                measurementLocation = CycleRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
                source = "OpenVitals",
                id = "own-bbt",
                isOpenVitalsEntry = true,
            ),
        )
        val request = slot<CycleEntryWriteRequest>()

        writer.write(
            existing,
            write(basalBodyTemperatureCelsius = 36.6, basalBodyTemperatureTime = LocalTime.of(7, 30)),
        )

        coVerify(exactly = 1) { repository.updateCycleEntry("own-bbt", capture(request)) }
        assertEquals(today.atTime(LocalTime.of(7, 30)).atZone(zone).toInstant(), request.captured.time)
        coVerify(exactly = 0) { repository.writeCycleEntry(any()) }
    }

    private fun ownFlow(level: Int) = MenstruationFlowEntry(
        time = today.atTime(LocalTime.NOON).atZone(zone).toInstant(),
        flow = level,
        source = "OpenVitals",
        id = "own-flow",
        isOpenVitalsEntry = true,
    )

    private fun write(
        date: LocalDate = today,
        bleeding: DayBleedingChoice? = null,
        basalBodyTemperatureCelsius: Double? = null,
        basalBodyTemperatureTime: LocalTime? = null,
    ) = CycleDayLogWrite(
        bleeding = bleeding,
        basalBodyTemperatureCelsius = basalBodyTemperatureCelsius,
        basalBodyTemperatureLocation = null,
        basalBodyTemperatureTime = basalBodyTemperatureTime,
        mucusAppearance = null,
        mucusAmount = null,
        ovulationTestResult = null,
        sexualActivityProtection = null,
        journal = CycleJournalEntry(date = date),
    )
}
