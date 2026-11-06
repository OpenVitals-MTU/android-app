package tech.mmarca.openvitals.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.LegacyDeviceDataCleaner
import tech.mmarca.openvitals.data.repository.contract.OnboardingPreferences

/**
 * The one-time notice that watches and scales moved to OpenWatch. Due once on
 * an install that was onboarded before the move; a fresh install marks it seen
 * at onboarding. Dismissing it, by any button or a tap outside, records the
 * flag and cleans up what the removed device code left behind.
 */
@HiltViewModel
class OpenWatchMoveNoticeViewModel @Inject constructor(
    private val preferences: OnboardingPreferences,
    private val cleaner: LegacyDeviceDataCleaner,
) : ViewModel() {

    private val _visible = MutableStateFlow(
        preferences.onboardingDone && !preferences.openWatchMoveNoticeSeen,
    )
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun onDismissed() {
        if (!_visible.value) return
        preferences.openWatchMoveNoticeSeen = true
        _visible.value = false
        viewModelScope.launch { cleaner.clean() }
    }
}
