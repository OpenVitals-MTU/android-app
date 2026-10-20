package tech.mmarca.openvitals.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.cycle.NightlySignal
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.domain.model.SleepData

class LoadCycleNightlyVitalsUseCaseTest {

    private val zone = ZoneId.of("UTC")
    private val start = LocalDate.parse("2026-03-01")
    private val today = LocalDate.parse("2026-03-05")

    private fun at(date: String, hour: Int) = LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant()

    private val sleep = mockk<SleepRepository>().also {
        coEvery { it.loadSleepSessions(any(), any()) } returns listOf(
            SleepData(
                id = "s1",
                startTime = at("2026-03-02", 0).minusSeconds(3600),
                endTime = at("2026-03-02", 7),
                durationMs = 8 * 3_600_000L,
                source = "watch",
            ),
        )
    }

    @Test
    fun `a signal the repositories cannot read is missing while the others are placed`() = runTest {
        val heart = mockk<HeartRepository>()
        coEvery { heart.loadRestingHeartRateSamples(any(), any()) } returns listOf(
            RestingHeartRateSample(at("2026-03-02", 3), 54, "watch"),
        )
        // No read permission: the repository answers with nothing.
        coEvery { heart.loadHrvSamples(any(), any()) } returns emptyList()
        val vitals = mockk<VitalsRepository>()
        coEvery { vitals.loadRespiratoryRate(any(), any()) } returns listOf(
            RespiratoryRateEntry(at("2026-03-02", 4), 14.0, "watch"),
        )

        val result = LoadCycleNightlyVitalsUseCase(sleep, heart, vitals)(start, today, zone)

        assertEquals(1, result.nightCount)
        assertEquals(listOf(NightlySignal.HRV_RMSSD), result.missingSignals)
        assertEquals(54.0, result.series.first { it.signal == NightlySignal.RESTING_HEART_RATE }.points.single().value, 0.0)
    }

    @Test
    fun `a failing read costs that signal and not the card`() = runTest {
        val heart = mockk<HeartRepository>()
        coEvery { heart.loadRestingHeartRateSamples(any(), any()) } throws IllegalStateException("boom")
        coEvery { heart.loadHrvSamples(any(), any()) } returns listOf(HrvSample(at("2026-03-02", 3), 48.0, "watch"))
        val vitals = mockk<VitalsRepository>()
        coEvery { vitals.loadRespiratoryRate(any(), any()) } throws SecurityException("no permission")

        val result = LoadCycleNightlyVitalsUseCase(sleep, heart, vitals)(start, today, zone)

        assertTrue(result.hasAnySignal)
        assertEquals(
            listOf(NightlySignal.RESTING_HEART_RATE, NightlySignal.RESPIRATORY_RATE),
            result.missingSignals,
        )
    }

    @Test
    fun `a missing permission is told apart from no data`() = runTest {
        val heart = mockk<HeartRepository>()
        coEvery { heart.loadRestingHeartRateSamples(any(), any()) } throws SecurityException("no permission")
        coEvery { heart.loadHrvSamples(any(), any()) } throws IllegalStateException("boom")
        val vitals = mockk<VitalsRepository>()
        coEvery { vitals.loadRespiratoryRate(any(), any()) } returns emptyList()

        val result = LoadCycleNightlyVitalsUseCase(sleep, heart, vitals)(start, today, zone)

        fun denied(signal: NightlySignal) = result.series.first { it.signal == signal }.permissionDenied
        assertTrue(denied(NightlySignal.RESTING_HEART_RATE))
        assertFalse(denied(NightlySignal.HRV_RMSSD))
        assertFalse(denied(NightlySignal.RESPIRATORY_RATE))
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        val heart = mockk<HeartRepository>()
        coEvery { heart.loadRestingHeartRateSamples(any(), any()) } throws CancellationException("stop")
        coEvery { heart.loadHrvSamples(any(), any()) } returns emptyList()
        val vitals = mockk<VitalsRepository>()
        coEvery { vitals.loadRespiratoryRate(any(), any()) } returns emptyList()

        val outcome = runCatching { LoadCycleNightlyVitalsUseCase(sleep, heart, vitals)(start, today, zone) }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }
}
