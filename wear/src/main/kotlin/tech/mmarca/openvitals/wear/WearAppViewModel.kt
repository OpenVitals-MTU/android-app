package tech.mmarca.openvitals.wear

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.wear.features.quicklog.EntryLog
import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry
import tech.mmarca.openvitals.wear.features.quicklog.withEntries
import tech.mmarca.openvitals.wear.health.CapabilityProbe
import tech.mmarca.openvitals.wear.health.WearCapabilities
import tech.mmarca.openvitals.wear.ui.preview.SampleData

/**
 * Puts the app-wide state together: the readings (sample values in a debug
 * build, until something feeds real ones), what this watch can really
 * measure, and the entries made on it. The capabilities and the entries are
 * real in every build.
 */
class WearAppViewModel(application: Application) : AndroidViewModel(application) {

    private val entryLog = EntryLog(application)
    private val capabilities = MutableStateFlow(WearCapabilities())

    private val readings: WearUiState =
        if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            SampleData.uiState.copy(capabilities = WearCapabilities(), entries = emptyList())
        } else {
            WearUiState()
        }

    val uiState: StateFlow<WearUiState> =
        combine(capabilities, entryLog.entries) { capabilities, entries ->
            readings.copy(
                capabilities = capabilities,
                // Until the probe answers, keep offering what was on offer before.
                availableMetrics = if (capabilities.probed) capabilities.availableMetrics else readings.availableMetrics,
            ).withEntries(entries)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), readings)

    init {
        viewModelScope.launch { capabilities.value = CapabilityProbe(application).probe() }
    }

    fun log(entry: LoggedEntry) = entryLog.add(entry)

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
