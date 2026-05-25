package tech.mmarca.openvitals.features.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepDetailUiState(
    val isLoading: Boolean = true,
    val session: SleepData? = null,
    val error: String? = null,
)

class SleepDetailViewModel(
    private val repository: SleepRepository,
    private val sleepId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepDetailUiState())
    val uiState: StateFlow<SleepDetailUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        load()
    }

    fun load() {
        if (sleepId.isBlank()) {
            _uiState.value = SleepDetailUiState(
                isLoading = false,
                error = "Missing sleep id.",
            )
            return
        }

        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadSleepSession(sleepId) }
                .onSuccess { session ->
                    if (!isCurrent) return@load
                    _uiState.value = SleepDetailUiState(
                        isLoading = false,
                        session = session,
                        error = if (session == null) "Sleep session not found." else null,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = SleepDetailUiState(
                        isLoading = false,
                        error = it.message ?: "Unable to load sleep session.",
                    )
                }
        }
    }
}
