package tech.mmarca.openvitals.features.sleep

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.navigation.SLEEP_DETAIL_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class SleepDetailUiState(
    val isLoading: Boolean = true,
    val session: SleepData? = null,
    val error: ScreenError? = null,
)

@HiltViewModel
class SleepDetailViewModel(
    private val repository: SleepRepository,
    private val sleepId: String,
) : ViewModel() {

    @Inject
    constructor(
        repository: SleepRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        repository = repository,
        sleepId = savedStateHandle[SLEEP_DETAIL_ID_ARG] ?: "",
    )

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
                error = ScreenError.MissingArgument,
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
                        error = if (session == null) ScreenError.NotFound else null,
                    )
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = SleepDetailUiState(
                        isLoading = false,
                        error = it.toScreenError(),
                    )
                }
        }
    }
}
