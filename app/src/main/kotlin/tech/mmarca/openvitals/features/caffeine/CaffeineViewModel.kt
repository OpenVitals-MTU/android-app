package tech.mmarca.openvitals.features.caffeine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.domain.insights.CaffeineInsightCalculator
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeineInsights
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

@Immutable
data class CaffeineUiState(
    val isLoading: Boolean = true,
    val analyticsRange: CaffeineAnalyticsRange = CaffeineAnalyticsRange.LAST_30_DAYS,
    val entries: List<CaffeineEntry> = emptyList(),
    val homeDisplay: CaffeineInsights = CaffeineInsights(),
    val analyticsDisplay: CaffeineInsights = CaffeineInsights(),
    val preferences: CaffeinePreferences = CaffeinePreferences(),
    val bodyProfile: BodyProfile = BodyProfile(),
    val showSetup: Boolean = false,
    val selectedEntryId: String? = null,
    val error: ScreenError? = null,
) {
    val display: CaffeineInsights get() = homeDisplay
}

enum class CaffeineAnalyticsRange {
    TODAY,
    YESTERDAY,
    LAST_30_DAYS,
    LAST_90_DAYS,
}

@HiltViewModel
class CaffeineViewModel(
    private val repository: CaffeineRepository,
    private val preferencesRepository: PreferencesRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    initialAnalyticsRange: CaffeineAnalyticsRange = CaffeineAnalyticsRange.LAST_30_DAYS,
    private val preferenceChanges: Flow<CaffeinePreferences> = emptyFlow(),
    private val bodyProfileChanges: Flow<BodyProfile> = emptyFlow(),
) : ViewModel() {

    @Inject
    constructor(
        repository: CaffeineRepository,
        preferencesRepository: PreferencesRepository,
        dispatchers: DispatcherProvider,
    ) : this(
        repository = repository,
        preferencesRepository = preferencesRepository,
        dispatchers = dispatchers,
        preferenceChanges = preferencesRepository.caffeinePreferencesFlow,
        bodyProfileChanges = preferencesRepository.bodyProfileFlow,
    )

    private val _uiState = MutableStateFlow(
        CaffeineUiState(
            analyticsRange = initialAnalyticsRange,
            preferences = preferencesRepository.caffeinePreferences(),
            bodyProfile = preferencesRepository.bodyProfile(),
        )
    )
    val uiState: StateFlow<CaffeineUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()

    init {
        observePreferences()
        observeBodyProfile()
        load()
    }

    fun selectAnalyticsRange(range: CaffeineAnalyticsRange) {
        if (_uiState.value.analyticsRange == range) return
        _uiState.value = _uiState.value.copy(analyticsRange = range)
        load()
    }

    fun refresh() {
        load(RefreshMode.FORCE)
    }

    fun load(refreshMode: RefreshMode = RefreshMode.NORMAL) {
        loadCoordinator.launch(viewModelScope) load@{
            val state = _uiState.value
            val today = LocalDate.now()
            val homePeriod = DatePeriod(today, today)
            val analyticsPeriod = state.analyticsRange.periodEnding(today)
            val loadPeriod = homePeriod.union(analyticsPeriod)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.loadCaffeineData(loadPeriod, refreshMode)
            }.onSuccess { result ->
                if (!isCurrent) return@load
                val preferences = _uiState.value.preferences
                val bodyProfile = _uiState.value.bodyProfile
                val displays = withContext(dispatchers.default) {
                    val home = CaffeineInsightCalculator.build(
                        entries = result.entries,
                        period = homePeriod,
                        preferences = preferences,
                        bodyProfile = bodyProfile,
                    )
                    val analytics = CaffeineInsightCalculator.build(
                        entries = result.entries,
                        period = analyticsPeriod,
                        preferences = preferences,
                        bodyProfile = bodyProfile,
                    )
                    home to analytics
                }
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    entries = result.entries,
                    homeDisplay = displays.first,
                    analyticsDisplay = displays.second,
                    showSetup = shouldShowSetup(preferences, result.entries),
                )
            }.onFailure { error ->
                if (!isCurrent) return@load
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.toScreenError(),
                )
            }
        }
    }

    fun completeSetup(preferences: CaffeinePreferences) {
        preferencesRepository.setCaffeinePreferences(preferences.copy(profileCompleted = true))
    }

    fun skipSetup() {
        preferencesRepository.setCaffeinePreferences(
            _uiState.value.preferences.copy(profileCompleted = true)
        )
    }

    fun selectEntry(entryId: String?) {
        _uiState.value = _uiState.value.copy(selectedEntryId = entryId)
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferenceChanges.drop(1).collect { preferences ->
                _uiState.value = _uiState.value.copy(preferences = preferences)
                rebuildDisplay()
            }
        }
    }

    private fun observeBodyProfile() {
        viewModelScope.launch {
            bodyProfileChanges.drop(1).collect { bodyProfile ->
                _uiState.value = _uiState.value.copy(bodyProfile = bodyProfile)
                rebuildDisplay()
            }
        }
    }

    private suspend fun rebuildDisplay() {
        val state = _uiState.value
        val today = LocalDate.now()
        val homePeriod = DatePeriod(today, today)
        val analyticsPeriod = state.analyticsRange.periodEnding(today)
        val displays = withContext(dispatchers.default) {
            val home = CaffeineInsightCalculator.build(
                entries = state.entries,
                period = homePeriod,
                preferences = state.preferences,
                now = Instant.now(),
                bodyProfile = state.bodyProfile,
            )
            val analytics = CaffeineInsightCalculator.build(
                entries = state.entries,
                period = analyticsPeriod,
                preferences = state.preferences,
                now = Instant.now(),
                bodyProfile = state.bodyProfile,
            )
            home to analytics
        }
        _uiState.value = _uiState.value.copy(
            homeDisplay = displays.first,
            analyticsDisplay = displays.second,
            showSetup = shouldShowSetup(state.preferences, state.entries),
        )
    }

    private fun shouldShowSetup(
        preferences: CaffeinePreferences,
        entries: List<CaffeineEntry>,
    ): Boolean {
        if (preferences.profileCompleted) return false
        return entries.isNotEmpty()
    }

    private fun DatePeriod.union(other: DatePeriod): DatePeriod =
        DatePeriod(
            start = minOf(start, other.start),
            end = maxOf(end, other.end),
        )
}

fun CaffeineAnalyticsRange.periodEnding(today: LocalDate): DatePeriod =
    when (this) {
        CaffeineAnalyticsRange.TODAY -> DatePeriod(today, today)
        CaffeineAnalyticsRange.YESTERDAY -> {
            val yesterday = today.minusDays(1)
            DatePeriod(yesterday, yesterday)
        }
        CaffeineAnalyticsRange.LAST_30_DAYS -> DatePeriod(today.minusDays(29), today)
        CaffeineAnalyticsRange.LAST_90_DAYS -> DatePeriod(today.minusDays(89), today)
    }
