package tech.mmarca.openvitals.features.settings

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.NutritionAverageBasis
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

@Immutable
data class NutritionSettingsUiState(
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val nutritionAverageLoggedDaysOnly: Boolean = true,
    val hydrationDailyGoalLiters: Double = PreferencesRepository.DEFAULT_HYDRATION_DAILY_GOAL_LITERS,
    /** What the drinking goal displays in: the HYDRATION override, else the base unit. */
    val hydrationUnitSystem: UnitSystem = UnitSystem.METRIC,
    val caffeinePreferences: CaffeinePreferences = CaffeinePreferences(),
    /** With the latest Health Connect weight folded in; the caffeine model reads it. */
    val bodyProfile: BodyProfile = BodyProfile(),
)

/** The Nutrition section: calories, averages, the drinking goal and the caffeine model. */
@HiltViewModel
class NutritionSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val bodyRepository: BodyRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "NutritionSettingsViewModel"
    }

    private val _uiState = MutableStateFlow(readPreferences(NutritionSettingsUiState()))
    val uiState: StateFlow<NutritionSettingsUiState> = _uiState.asStateFlow()

    init {
        resolveBodyProfileFromHealthConnect()
    }

    fun refresh() {
        _uiState.value = readPreferences(_uiState.value)
        resolveBodyProfileFromHealthConnect()
    }

    private fun readPreferences(state: NutritionSettingsUiState): NutritionSettingsUiState =
        state.copy(
            showOpenVitalsCalculatedCalories = preferencesRepository.showOpenVitalsCalculatedCalories,
            nutritionAverageLoggedDaysOnly =
                preferencesRepository.nutritionAverageBasis == NutritionAverageBasis.LOGGED_DAYS,
            hydrationDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
            hydrationUnitSystem = preferencesRepository.unitOverride(UnitQuantity.HYDRATION)
                ?: preferencesRepository.unitSystem,
            caffeinePreferences = preferencesRepository.caffeinePreferences(),
            bodyProfile = preferencesRepository.bodyProfile(),
        )

    private fun resolveBodyProfileFromHealthConnect() {
        viewModelScope.launch {
            val declared = preferencesRepository.bodyProfile()
            val resolved = runCatching { bodyRepository.resolveBodyProfile(declared) }
                .getOrElse { error ->
                    Log.w(TAG, "resolveBodyProfile failed", error)
                    return@launch
                }
            _uiState.value = _uiState.value.copy(bodyProfile = resolved)
        }
    }

    fun setShowOpenVitalsCalculatedCalories(enabled: Boolean) {
        preferencesRepository.showOpenVitalsCalculatedCalories = enabled
        _uiState.value = _uiState.value.copy(showOpenVitalsCalculatedCalories = enabled)
    }

    fun setNutritionAverageLoggedDaysOnly(enabled: Boolean) {
        preferencesRepository.nutritionAverageBasis = if (enabled) {
            NutritionAverageBasis.LOGGED_DAYS
        } else {
            NutritionAverageBasis.EVERY_DAY
        }
        _uiState.value = _uiState.value.copy(nutritionAverageLoggedDaysOnly = enabled)
    }

    fun setHydrationDailyGoalLiters(liters: Double) {
        preferencesRepository.hydrationDailyGoalLiters = liters
        _uiState.value = _uiState.value.copy(
            hydrationDailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters,
        )
    }

    /** Same store as the Metabolism card under Body profile. */
    fun updateCaffeinePreferences(preferences: CaffeinePreferences) {
        preferencesRepository.setCaffeinePreferences(preferences)
        _uiState.value = _uiState.value.copy(caffeinePreferences = preferencesRepository.caffeinePreferences())
    }
}
