package tech.mmarca.openvitals.features.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.report.ReportCancellation
import tech.mmarca.openvitals.data.repository.report.ReportProgress
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportSection

enum class ReportBuilderStep { CONFIGURE, BUILDING, DONE }

/** The preset lookback chips; `null` selection means a custom range. */
val ReportLookbackPresets = listOf(30, 90, 180, 365)

/**
 * Keeps custom ranges inside what the vitals daily cache can serve, so a
 * hand-picked range stays as fast as the presets.
 */
const val ReportMaxRangeDays = 730L

data class ReportBuilderState(
    val step: ReportBuilderStep = ReportBuilderStep.CONFIGURE,
    val supportedMetrics: List<ReportMetric> = emptyList(),
    val selectedMetrics: Set<ReportMetric> = emptySet(),
    val granularity: ReportGranularity = ReportGranularity.DAILY,
    val lookbackDays: Int? = 90,
    val customStart: LocalDate = LocalDate.now().minusDays(89),
    val customEnd: LocalDate = LocalDate.now(),
    val missingPermissions: Set<String> = emptySet(),
    val progress: ReportProgress? = null,
    val progressMetricTitle: String? = null,
    val stagedFile: File? = null,
    val error: Boolean = false,
) {
    val customRangeValid: Boolean get() = !customStart.isAfter(customEnd)

    val canBuild: Boolean
        get() = selectedMetrics.isNotEmpty() && (lookbackDays != null || customRangeValid)

    val metricsBySection: Map<ReportSection, List<ReportMetric>>
        get() = supportedMetrics.groupBy { it.section }

    val rangeStart: LocalDate
        get() = lookbackDays?.let { LocalDate.now().minusDays(it - 1L) } ?: customStart

    val rangeEnd: LocalDate
        get() = if (lookbackDays != null) LocalDate.now() else customEnd
}

/**
 * The report builder's three steps: configure → build (in this scope, per the
 * in-ViewModel rule for user-attended work) → share or save the staged PDF.
 * Any configuration change after a build invalidates the staged file — the
 * PDF on disk must always match the selection on screen.
 */
@HiltViewModel
class ReportBuilderViewModel @Inject constructor(
    private val exportService: ReportExportService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportBuilderState())
    val uiState: StateFlow<ReportBuilderState> = _uiState.asStateFlow()

    private var cancellation: ReportCancellation? = null

    init {
        refreshSupportedMetrics()
    }

    private fun refreshSupportedMetrics() {
        val supported = exportService.supportedMetrics()
            .sortedWith(compareBy({ it.section.ordinal }, { it.ordinal }))
        _uiState.update { state ->
            state.copy(
                supportedMetrics = supported,
                selectedMetrics = state.selectedMetrics.intersect(supported.toSet()),
            )
        }
    }

    fun metricTitle(metric: ReportMetric): String = exportService.metricTitle(metric)

    fun toggleMetric(metric: ReportMetric) = configure { state ->
        state.copy(
            selectedMetrics = if (metric in state.selectedMetrics) {
                state.selectedMetrics - metric
            } else {
                state.selectedMetrics + metric
            },
        )
    }

    fun selectAllMetrics() = configure { it.copy(selectedMetrics = it.supportedMetrics.toSet()) }

    fun clearMetrics() = configure { it.copy(selectedMetrics = emptySet()) }

    fun setGranularity(granularity: ReportGranularity) = configure { it.copy(granularity = granularity) }

    fun setLookback(days: Int?) = configure { it.copy(lookbackDays = days) }

    fun setCustomStart(date: LocalDate) = configure { state ->
        val clampedEnd = if (state.customEnd.isAfter(date.plusDays(ReportMaxRangeDays - 1))) {
            date.plusDays(ReportMaxRangeDays - 1)
        } else {
            state.customEnd
        }
        state.copy(customStart = date, customEnd = clampedEnd)
    }

    fun setCustomEnd(date: LocalDate) = configure { state ->
        val clampedStart = if (state.customStart.isBefore(date.minusDays(ReportMaxRangeDays - 1))) {
            date.minusDays(ReportMaxRangeDays - 1)
        } else {
            state.customStart
        }
        state.copy(customEnd = date, customStart = clampedStart)
    }

    /** The permissions the current selection still needs — for the callout. */
    fun refreshMissingPermissions(granted: Set<String>) {
        _uiState.update { state ->
            state.copy(
                missingPermissions = exportService.requestablePermissionsFor(state.selectedMetrics) - granted,
            )
        }
    }

    fun missingPermissionsFor(selection: Set<ReportMetric>): Set<String> =
        exportService.requestablePermissionsFor(selection)

    fun buildReport() {
        val state = _uiState.value
        if (!state.canBuild || state.step == ReportBuilderStep.BUILDING) return

        val cancel = ReportCancellation()
        cancellation = cancel
        _uiState.update {
            it.copy(
                step = ReportBuilderStep.BUILDING,
                progress = null,
                progressMetricTitle = null,
                stagedFile = null,
                error = false,
            )
        }
        viewModelScope.launch {
            try {
                val file = exportService.build(
                    metrics = state.selectedMetrics,
                    granularity = state.granularity,
                    start = state.rangeStart,
                    end = state.rangeEnd,
                    onProgress = { progress ->
                        _uiState.update {
                            it.copy(
                                progress = progress,
                                progressMetricTitle = progress.currentMetric?.let(exportService::metricTitle),
                            )
                        }
                    },
                    cancellation = cancel,
                )
                _uiState.update { it.copy(step = ReportBuilderStep.DONE, stagedFile = file) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Report build failed", error)
                _uiState.update { it.copy(step = ReportBuilderStep.CONFIGURE, error = true) }
            } finally {
                cancellation = null
            }
        }
    }

    fun cancelBuild() {
        cancellation?.cancel()
    }

    fun newReport() {
        _uiState.update {
            it.copy(
                step = ReportBuilderStep.CONFIGURE,
                progress = null,
                progressMetricTitle = null,
                stagedFile = null,
                error = false,
            )
        }
    }

    /** Every configuration change routes here so a stale staged PDF can never survive one. */
    private fun configure(transform: (ReportBuilderState) -> ReportBuilderState) {
        _uiState.update { state ->
            transform(state).copy(
                step = ReportBuilderStep.CONFIGURE,
                stagedFile = null,
                error = false,
            )
        }
    }

    private companion object {
        const val TAG = "ReportBuilderViewModel"
    }
}
