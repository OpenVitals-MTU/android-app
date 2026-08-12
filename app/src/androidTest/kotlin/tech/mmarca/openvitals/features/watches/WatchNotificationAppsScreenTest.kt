package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.edit
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.data.repository.WatchNotificationPrefsStore
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the empty-state case of Flutter's
 * `test/features/settings/watch_notification_apps_screen_test.dart`.
 *
 * The per-app blocklist is a list of every launchable app. When the platform
 * hands back nothing — a locked-down profile, or a host that refused the query
 * — the screen has to say so. Blank space under a switch that reads "on" is
 * indistinguishable from an app whose list is still loading, and the user waits
 * for something that is never coming.
 */
class WatchNotificationAppsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeGateway(
        private val apps: List<InstalledApp>,
        private val throwsOnList: Boolean = false,
    ) : WatchNotificationsGateway {
        override fun isNotificationAccessGranted(): Boolean = true

        override fun openNotificationAccessSettings() = Unit

        override fun listLaunchableApps(): List<InstalledApp> {
            if (throwsOnList) error("no host")
            return apps
        }

        override fun pushConfiguration() = Unit
    }

    @Test
    fun aPhoneWithNoLaunchableAppsSaysSoRatherThanShowingAnEmptyList() {
        setScreen(FakeGateway(apps = emptyList()))

        awaitEmptyState()
        composeRule
            .onNodeWithText(string(R.string.settings_watch_notifications_apps_empty))
            .assertIsDisplayed()
    }

    @Test
    fun anAppListThatCannotBeReadLeavesTheScreenUsableRatherThanStuckLoading() {
        // A host that cannot answer the query is the same situation from the
        // user's side as a phone with nothing to list — and the one thing it
        // must not do is spin forever.
        setScreen(FakeGateway(apps = emptyList(), throwsOnList = true))

        awaitEmptyState()
        composeRule
            .onNodeWithText(string(R.string.settings_watch_notifications_apps_empty))
            .assertIsDisplayed()
    }

    private fun awaitEmptyState() {
        val empty = string(R.string.settings_watch_notifications_apps_empty)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(empty).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun setScreen(gateway: WatchNotificationsGateway) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A throwaway preferences file, so the test never edits the real one.
        val prefs = context.getSharedPreferences(TEST_PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit { clear() }
        val store = WatchNotificationPrefsStore(prefs)
        // Forwarding already on and access already granted: the app list only
        // exists once both gates are open.
        store.enabled = true
        store.disclosureAccepted = true

        val viewModel = WatchNotificationAppsViewModel(
            store = store,
            gateway = gateway,
            dispatchers = DefaultDispatcherProvider,
        )
        composeRule.setContent {
            OpenVitalsTheme { WatchNotificationAppsScreen(viewModel = viewModel) }
        }
    }

    private companion object {
        const val TEST_PREFS_FILE = "openvitals_test_watch_notifications"
    }
}
