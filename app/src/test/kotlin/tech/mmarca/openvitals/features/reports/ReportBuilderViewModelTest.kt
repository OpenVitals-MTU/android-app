package tech.mmarca.openvitals.features.reports

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.report.ReportCancellation
import tech.mmarca.openvitals.data.repository.report.ReportProgress
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ReportBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var service: ReportExportService

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        service = mockk()
        every { service.supportedMetrics() } returns ReportMetric.entries.toSet()
        every { service.metricTitle(any()) } answers { firstArg<ReportMetric>().name }
        every { service.requestablePermissionsFor(any()) } returns emptySet()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun viewModel() = ReportBuilderViewModel(service)

    @Test fun `build cannot start with nothing selected`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.canBuild)
        viewModel.buildReport()
        advanceUntilIdle()

        assertEquals(ReportBuilderStep.CONFIGURE, viewModel.uiState.value.step)
        coVerify(exactly = 0) { service.build(any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `a successful build lands on DONE with the staged file`() = runTest {
        val staged = File("/tmp/openvitals-report-test.pdf")
        coEvery { service.build(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<(ReportProgress) -> Unit>(4).invoke(ReportProgress(1, 2, ReportMetric.STEPS))
            staged
        }
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)

        viewModel.buildReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportBuilderStep.DONE, state.step)
        assertEquals(staged, state.stagedFile)
        assertEquals(1, state.progress?.completed)
        coVerify(exactly = 1) {
            service.build(
                metrics = setOf(ReportMetric.STEPS),
                granularity = ReportGranularity.DAILY,
                start = any(),
                end = any(),
                onProgress = any(),
                cancellation = any(),
            )
        }
    }

    @Test fun `the preset lookback resolves to an inclusive day span ending today`() = runTest {
        val start = slot<LocalDate>()
        val end = slot<LocalDate>()
        coEvery {
            service.build(any(), any(), capture(start), capture(end), any(), any())
        } returns File("r.pdf")
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)
        viewModel.setLookback(30)

        viewModel.buildReport()
        advanceUntilIdle()

        assertEquals(LocalDate.now(), end.captured)
        assertEquals(LocalDate.now().minusDays(29), start.captured)
    }

    @Test fun `a failed build returns to CONFIGURE with the error flag`() = runTest {
        coEvery { service.build(any(), any(), any(), any(), any(), any()) } throws IllegalStateException("boom")
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)

        viewModel.buildReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportBuilderStep.CONFIGURE, state.step)
        assertTrue(state.error)
        assertNull(state.stagedFile)
    }

    @Test fun `cancel flips the cancellation flag the service was handed`() = runTest {
        var handed: ReportCancellation? = null
        coEvery { service.build(any(), any(), any(), any(), any(), any()) } coAnswers {
            val cancellation = arg<ReportCancellation>(5)
            handed = cancellation
            // A real build outlives the cancel tap; without this the fake
            // finishes before cancelBuild() runs and the test asserts nothing.
            while (!cancellation.isCancelled) kotlinx.coroutines.delay(10)
            File("r.pdf")
        }
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)
        viewModel.buildReport()
        viewModel.cancelBuild()
        advanceUntilIdle()

        assertTrue(handed!!.isCancelled)
    }

    @Test fun `any configuration change invalidates a finished report`() = runTest {
        coEvery { service.build(any(), any(), any(), any(), any(), any()) } returns File("r.pdf")
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)
        viewModel.buildReport()
        advanceUntilIdle()
        assertEquals(ReportBuilderStep.DONE, viewModel.uiState.value.step)

        viewModel.setGranularity(ReportGranularity.WEEKLY)

        val state = viewModel.uiState.value
        assertEquals(ReportBuilderStep.CONFIGURE, state.step)
        assertNull(state.stagedFile)
    }

    @Test fun `an invalid custom range blocks the build`() = runTest {
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)
        viewModel.setLookback(null)
        viewModel.setCustomStart(LocalDate.of(2026, 6, 1))
        viewModel.setCustomEnd(LocalDate.of(2026, 6, 30))
        assertTrue(viewModel.uiState.value.canBuild)

        viewModel.setCustomStart(LocalDate.of(2026, 7, 15))

        assertFalse(viewModel.uiState.value.canBuild)
    }

    @Test fun `custom ranges are clamped to the two-year cache window`() = runTest {
        val viewModel = viewModel()
        viewModel.setLookback(null)
        viewModel.setCustomEnd(LocalDate.of(2026, 8, 1))

        viewModel.setCustomStart(LocalDate.of(2020, 1, 1))

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2020, 1, 1), state.customStart)
        assertEquals(LocalDate.of(2020, 1, 1).plusDays(ReportMaxRangeDays - 1), state.customEnd)
    }

    @Test fun `missing permissions reflect the current selection`() = runTest {
        every { service.requestablePermissionsFor(setOf(ReportMetric.SLEEP)) } returns setOf("sleep.read")
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.SLEEP)

        viewModel.refreshMissingPermissions(granted = emptySet())
        assertEquals(setOf("sleep.read"), viewModel.uiState.value.missingPermissions)

        viewModel.refreshMissingPermissions(granted = setOf("sleep.read"))
        assertTrue(viewModel.uiState.value.missingPermissions.isEmpty())
    }

    @Test fun `new report clears the finished state`() = runTest {
        coEvery { service.build(any(), any(), any(), any(), any(), any()) } returns File("r.pdf")
        val viewModel = viewModel()
        viewModel.toggleMetric(ReportMetric.STEPS)
        viewModel.buildReport()
        advanceUntilIdle()

        viewModel.newReport()

        val state = viewModel.uiState.value
        assertEquals(ReportBuilderStep.CONFIGURE, state.step)
        assertNull(state.stagedFile)
        // The selection survives — only the artifact is gone.
        assertEquals(setOf(ReportMetric.STEPS), state.selectedMetrics)
    }
}
