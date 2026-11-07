package tech.mmarca.openvitals.features.watches

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.WatchNotificationPrefsStore
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.util.MainDispatcherRule

/** Disclosure before the permission, the permission before the preference, and the blocklist round-trip. */
class WatchNotificationAppsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeGateway : WatchNotificationsGateway {
        var accessGranted = false
        var openedSettings = 0
        var pushedConfig = 0

        /** The app query throws. The screen must stay usable rather than stuck loading. */
        var appListThrows = false
        var apps = listOf(
            InstalledApp("com.example.chat", "Chat"),
            InstalledApp("com.example.mail", "Mail"),
        )

        override fun isNotificationAccessGranted(): Boolean = accessGranted

        override fun openNotificationAccessSettings() {
            openedSettings++
        }

        override fun listLaunchableApps(): List<InstalledApp> {
            if (appListThrows) error("no host")
            return apps
        }

        override fun pushConfiguration() {
            pushedConfig++
        }
    }

    private val prefs = FakeSharedPreferences()
    private val store = WatchNotificationPrefsStore(prefs)
    private val gateway = FakeGateway()

    private fun viewModel() = WatchNotificationAppsViewModel(
        store = store,
        gateway = gateway,
        dispatchers = mainDispatcherRule.dispatcherProvider,
    )

    @Test
    fun `enabling without prior consent shows the disclosure and enables nothing`() = runTest {
        gateway.accessGranted = true
        val vm = viewModel()

        vm.setEnabled(true)

        assertTrue(vm.uiState.value.showDisclosure)
        assertFalse(vm.uiState.value.enabled)
        assertFalse(store.enabled)
    }

    @Test
    fun `declining the disclosure leaves the feature off and consent unset`() = runTest {
        gateway.accessGranted = true
        val vm = viewModel()

        vm.setEnabled(true)
        vm.declineDisclosure()

        assertFalse(vm.uiState.value.showDisclosure)
        assertFalse(vm.uiState.value.enabled)
        assertFalse(store.disclosureAccepted)
    }

    @Test
    fun `accepting the disclosure continues straight into enabling`() = runTest {
        gateway.accessGranted = true
        val vm = viewModel()

        vm.setEnabled(true)
        vm.acceptDisclosure()

        assertTrue(store.disclosureAccepted)
        assertTrue(vm.uiState.value.enabled)
        assertTrue(store.enabled)
        assertFalse(vm.uiState.value.showDisclosure)
    }

    @Test
    fun `consent is remembered, so a second enable does not re-prompt`() = runTest {
        gateway.accessGranted = true
        store.disclosureAccepted = true
        val vm = viewModel()

        vm.setEnabled(true)

        assertFalse(vm.uiState.value.showDisclosure)
        assertTrue(vm.uiState.value.enabled)
    }

    @Test
    fun `without notification access the system screen opens and nothing is enabled`() = runTest {
        // Consent alone is not enough: the permission is granted on Android's settings screen.
        store.disclosureAccepted = true
        gateway.accessGranted = false
        val vm = viewModel()

        vm.setEnabled(true)

        assertEquals(1, gateway.openedSettings)
        assertFalse(vm.uiState.value.enabled)
        assertFalse(store.enabled)
    }

    @Test
    fun `access is re-read at enable time, not trusted from stale state`() = runTest {
        // Android gives no callback when access is granted, so the cached flag is stale by construction.
        store.disclosureAccepted = true
        gateway.accessGranted = false
        val vm = viewModel()
        assertFalse(vm.uiState.value.accessGranted)

        gateway.accessGranted = true // granted while the app was away
        vm.setEnabled(true)

        assertTrue(vm.uiState.value.enabled)
        assertEquals(0, gateway.openedSettings)
    }

    @Test
    fun `disabling needs no gates and takes effect at once`() = runTest {
        store.disclosureAccepted = true
        store.enabled = true
        gateway.accessGranted = true
        val vm = viewModel()

        vm.setEnabled(false)

        assertFalse(vm.uiState.value.enabled)
        assertFalse(store.enabled)
    }

    @Test
    fun `refresh reads both gates and mirrors the config to the filter`() = runTest {
        store.enabled = true
        gateway.accessGranted = true
        val vm = viewModel()

        assertTrue(vm.uiState.value.enabled)
        assertTrue(vm.uiState.value.accessGranted)
        assertTrue(vm.uiState.value.active)
        assertFalse(vm.uiState.value.loading)
        // Pushed on every refresh, because the paired watch can change without the switch moving.
        assertTrue(gateway.pushedConfig >= 1)
    }

    @Test
    fun `the app list carries the stored blocklist`() = runTest {
        store.blockedPackages = setOf("com.example.chat")
        val vm = viewModel()

        vm.loadApps()

        val apps = vm.uiState.value.apps
        assertEquals(listOf("Chat", "Mail"), apps.map { it.label })
        assertTrue(apps.first { it.packageName == "com.example.chat" }.blocked)
        assertFalse(apps.first { it.packageName == "com.example.mail" }.blocked)
    }

    @Test
    fun `blocking an app persists it and pushes the config`() = runTest {
        val vm = viewModel()
        vm.loadApps()
        val pushesBefore = gateway.pushedConfig

        vm.setBlocked("com.example.chat", blocked = true)

        assertTrue(store.blockedPackages.contains("com.example.chat"))
        assertTrue(vm.uiState.value.apps.first { it.packageName == "com.example.chat" }.blocked)
        assertTrue(gateway.pushedConfig > pushesBefore)

        vm.setBlocked("com.example.chat", blocked = false)
        assertFalse(store.blockedPackages.contains("com.example.chat"))
    }

    @Test
    fun `an app list that cannot be read leaves the screen usable, not stuck loading`() = runTest {
        // A host that cannot answer the query: an empty list, not a stuck spinner.
        gateway.appListThrows = true
        val vm = viewModel()

        vm.loadApps()

        assertFalse(vm.uiState.value.loadingApps)
        assertTrue(vm.uiState.value.apps.isEmpty())
    }

    @Test
    fun `blocked count counts only blocked apps`() = runTest {
        store.blockedPackages = setOf("com.example.chat")
        val vm = viewModel()
        vm.loadApps()

        assertEquals(1, vm.uiState.value.blockedCount)
    }

    @Test
    fun `active means both gates open, not just the switch`() = runTest {
        store.enabled = true
        gateway.accessGranted = false
        val vm = viewModel()

        assertTrue(vm.uiState.value.enabled)
        assertFalse(vm.uiState.value.active)
    }
}
