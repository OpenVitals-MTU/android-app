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
 * Whether notifications are mirrored, and whether Android allows it. Two
 * gates shown separately, or a flip that does nothing looks like a bug.
 */
@Immutable
data class WatchNotificationsUiState(
    /** Whether the user has switched forwarding on. */
    val enabled: Boolean = false,
    /** Whether Android granted notification access. Only the settings screen can, so it is polled. */
    val accessGranted: Boolean = false,
    /** Whether the user accepted the disclosure. Required before access is requested. */
    val disclosureAccepted: Boolean = false,
    /** Whether the disclosure dialog is on screen. Owned here so the ordering is testable. */
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
 * The enable flow enforces, in order, the prominent disclosure (required
 * before notification access is requested) and the settings-screen grant.
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

    /** Re-reads the permission. Android gives no callback, so the screen calls this on resume. */
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
            // The listener keeps its own copy of the configuration. Pushed on every
            // refresh: the paired watch can change without this switch moving.
            pushConfig()
        }
    }

    /** Flips forwarding. Turning on runs the gates: disclosure, then access, then the preference. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                store.enabled = false
                state.update { it.copy(enabled = false) }
                pushConfig()
                return@launch
            }

            // Consent before the permission is requested.
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
        // Re-read rather than trust the cache: Android gives no callback when access is granted.
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
