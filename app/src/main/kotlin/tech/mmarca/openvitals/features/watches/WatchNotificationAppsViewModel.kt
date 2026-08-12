package tech.mmarca.openvitals.features.watches

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.WatchNotificationPrefsStore
import tech.mmarca.openvitals.devices.garmin.GarminLog

/** One app the user can silence, with its current blocklist state. */
@Immutable
data class WatchNotificationApp(
    val packageName: String,
    val label: String,
    val blocked: Boolean,
)

/**
 * Whether phone notifications are mirrored to the watch, and whether Android
 * will let them be.
 *
 * Two separate gates, deliberately surfaced separately: the user can want the
 * feature ([enabled]) while Android has not been told to allow it
 * ([accessGranted]). Collapsing them into one switch would make a flip that
 * does nothing look like a bug.
 */
@Immutable
data class WatchNotificationsUiState(
    /** Whether the user has switched forwarding on. */
    val enabled: Boolean = false,
    /**
     * Whether Android has granted notification access. There is no runtime
     * prompt for it — the only way to grant it is the system settings screen,
     * so this is polled on refresh rather than awaited.
     */
    val accessGranted: Boolean = false,
    /**
     * Whether the user has seen and accepted what the feature reads. Required
     * before access is requested; remembered, so toggling does not re-prompt.
     */
    val disclosureAccepted: Boolean = false,
    /**
     * Whether the prominent disclosure dialog is on screen right now. Owned by
     * the ViewModel (the Flutter build used a callback into the view) so the
     * "consent before permission" ordering lives in one testable place.
     */
    val showDisclosure: Boolean = false,
    /** Every launchable app, blocked flag included. Empty until loaded. */
    val apps: List<WatchNotificationApp> = emptyList(),
    val loadingApps: Boolean = false,
    val loading: Boolean = true,
) {
    /** Forwarding is only actually happening when both gates are open. */
    val active: Boolean get() = enabled && accessGranted

    val blockedCount: Int get() = apps.count { it.blocked }
}

/**
 * Port of the Flutter build's `watch_notifications_view_model.dart`.
 *
 * The enable flow enforces two things the switch itself cannot supply, in this
 * order: the user's informed consent (the prominent disclosure, which Google
 * Play requires BEFORE notification access is requested — and the order a
 * reasonable person expects anyway: say what will be read, then ask for it),
 * and a permission only Android's own settings screen can grant.
 */
@HiltViewModel
class WatchNotificationAppsViewModel @Inject constructor(
    private val store: WatchNotificationPrefsStore,
    private val gateway: WatchNotificationsGateway,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    private val ioDispatcher: CoroutineContext = dispatchers.io

    private val state = MutableStateFlow(WatchNotificationsUiState())
    val uiState: StateFlow<WatchNotificationsUiState> = state.asStateFlow()

    init {
        refresh()
    }

    /**
     * Re-reads the permission, which can only have changed while the app was
     * away at the system settings screen — Android gives no callback when
     * access is granted, so the screen calls this again on every resume.
     */
    fun refresh() {
        viewModelScope.launch {
            val granted = readAccess()
            state.update {
                it.copy(
                    enabled = store.enabled,
                    accessGranted = granted,
                    disclosureAccepted = store.disclosureAccepted,
                    loading = false,
                )
            }
            // The listener's filter keeps its own copy of the configuration so
            // it can run before any UI exists. Pushed on every refresh rather
            // than only on change, because the paired watch can change without
            // this switch moving.
            pushConfig()
        }
    }

    /**
     * Flips forwarding on or off.
     *
     * Turning it on runs the gates: disclosure first (via [uiState]'s
     * `showDisclosure` and [acceptDisclosure]/[declineDisclosure]), then
     * notification access, then the preference itself.
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                store.enabled = false
                state.update { it.copy(enabled = false) }
                pushConfig()
                return@launch
            }

            // Consent BEFORE the permission is requested — see the class doc.
            if (!store.disclosureAccepted) {
                state.update { it.copy(showDisclosure = true) }
                return@launch
            }
            proceedWithEnable()
        }
    }

    /** The user accepted the prominent disclosure; the enable flow continues. */
    fun acceptDisclosure() {
        viewModelScope.launch {
            store.disclosureAccepted = true
            state.update { it.copy(disclosureAccepted = true, showDisclosure = false) }
            proceedWithEnable()
        }
    }

    /** Dismissing the dialog counts as declining: nothing is enabled. */
    fun declineDisclosure() {
        state.update { it.copy(showDisclosure = false) }
    }

    private suspend fun proceedWithEnable() {
        // Re-read rather than trust the cached state: Android gives no
        // callback when access is granted — the user leaves for a system
        // screen and comes back — so anything cached before that is stale.
        // Trusting the cache is what made the switch refuse to move after
        // access had already been granted.
        var granted = state.value.accessGranted
        if (!granted) {
            granted = readAccess()
            state.update { it.copy(accessGranted = granted) }
        }
        if (!granted) {
            gateway.openNotificationAccessSettings()
            return
        }

        store.enabled = true
        state.update { it.copy(enabled = true) }
        pushConfig()
    }

    /** Loads the app list for the blocklist. */
    fun loadApps() {
        if (state.value.loadingApps) return
        state.update { it.copy(loadingApps = true) }
        viewModelScope.launch {
            val blocked = store.blockedPackages
            val installed = withContext(ioDispatcher) {
                try {
                    gateway.listLaunchableApps()
                } catch (error: Exception) {
                    GarminLog.log("[GARMIN-NOTIFY] could not list apps: $error")
                    emptyList()
                }
            }
            state.update {
                it.copy(
                    loadingApps = false,
                    apps = installed.map { app ->
                        WatchNotificationApp(
                            packageName = app.packageName,
                            label = app.label,
                            blocked = app.packageName in blocked,
                        )
                    },
                )
            }
        }
    }

    /** Silences an app, or un-silences it. */
    fun setBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            store.setBlocked(packageName, blocked = blocked)
            state.update {
                it.copy(
                    apps = it.apps.map { app ->
                        if (app.packageName == packageName) app.copy(blocked = blocked) else app
                    },
                )
            }
            pushConfig()
        }
    }

    fun openAccessSettings() {
        gateway.openNotificationAccessSettings()
    }

    private suspend fun pushConfig() {
        withContext(ioDispatcher) {
            try {
                gateway.pushConfiguration()
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-NOTIFY] could not push the config: $error")
            }
        }
    }

    private suspend fun readAccess(): Boolean = withContext(ioDispatcher) {
        try {
            gateway.isNotificationAccessGranted()
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-NOTIFY] could not read notification access: $error")
            false
        }
    }
}
