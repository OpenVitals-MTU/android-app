package tech.mmarca.openvitals.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences

/** The key names are load-bearing: the migrator copies the Flutter values under exactly these names. */
class WatchNotificationPrefsStoreTest {

    private val prefs = FakeSharedPreferences()

    private fun store() = WatchNotificationPrefsStore(prefs)

    @Test
    fun `everything defaults to off and empty`() {
        val store = store()
        assertFalse(store.enabled)
        assertTrue(store.blockedPackages.isEmpty())
        assertFalse(store.disclosureAccepted)
    }

    @Test
    fun `enabled round-trips through storage`() {
        store().enabled = true
        // A second store over the same prefs is the real round-trip.
        assertTrue(store().enabled)
    }

    @Test
    fun `the blocklist round-trips as a set`() {
        store().blockedPackages = setOf("com.spam.app", "com.other.app")
        assertEquals(setOf("com.spam.app", "com.other.app"), store().blockedPackages)
    }

    @Test
    fun `setBlocked adds and removes one package`() {
        val store = store()
        store.setBlocked("com.spam.app", blocked = true)
        store.setBlocked("com.other.app", blocked = true)
        assertEquals(setOf("com.spam.app", "com.other.app"), store.blockedPackages)

        store.setBlocked("com.spam.app", blocked = false)
        assertEquals(setOf("com.other.app"), store.blockedPackages)
    }

    @Test
    fun `disclosure acceptance is remembered independently of enabled`() {
        // Consent is given once; switching off and on must not re-prompt.
        val store = store()
        store.disclosureAccepted = true
        store.enabled = true
        store.enabled = false
        assertTrue(store().disclosureAccepted)
    }

    @Test
    fun `the exact Flutter key names are what lands on disk`() {
        val store = store()
        store.enabled = true
        store.blockedPackages = setOf("com.spam.app")
        store.disclosureAccepted = true

        assertTrue(prefs.getBoolean("garmin_notifications_enabled", false))
        assertEquals(
            setOf("com.spam.app"),
            prefs.getStringSet("garmin_notifications_blocked_packages", null),
        )
        assertTrue(prefs.getBoolean("garmin_notifications_disclosure_accepted", false))
    }

    // Adoption of phase-5 migrated values.

    private fun legacyMain(): FakeSharedPreferences = FakeSharedPreferences().apply {
        edit()
            .putBoolean("garmin_notifications_enabled", true)
            .putStringSet("garmin_notifications_blocked_packages", setOf("com.spam.app"))
            .putBoolean("garmin_notifications_disclosure_accepted", true)
            .apply()
    }

    @Test
    fun `values the migrator left in the main prefs file are adopted on first construction`() {
        val store = WatchNotificationPrefsStore(prefs, legacyMainPrefs = legacyMain())

        assertTrue(store.enabled)
        assertEquals(setOf("com.spam.app"), store.blockedPackages)
        assertTrue(store.disclosureAccepted)
        // And they were copied into the store's own file, not read through.
        assertTrue(store().enabled)
    }

    @Test
    fun `a value already in the store's own file wins over the migrated copy`() {
        prefs.edit().putBoolean("garmin_notifications_enabled", false).apply()

        val store = WatchNotificationPrefsStore(prefs, legacyMainPrefs = legacyMain())

        assertFalse(store.enabled)
        // The other keys, absent from the own file, are still adopted.
        assertTrue(store.disclosureAccepted)
    }

    @Test
    fun `the adoption never writes to the main prefs file`() {
        val main = legacyMain()
        WatchNotificationPrefsStore(prefs, legacyMainPrefs = main).enabled = false

        // The migrated copies are untouched; this store owns its own file.
        assertTrue(main.getBoolean("garmin_notifications_enabled", false))
    }

    @Test
    fun `an empty main prefs file adopts nothing`() {
        val store = WatchNotificationPrefsStore(prefs, legacyMainPrefs = FakeSharedPreferences())
        assertFalse(store.enabled)
        assertTrue(store.blockedPackages.isEmpty())
    }
}
