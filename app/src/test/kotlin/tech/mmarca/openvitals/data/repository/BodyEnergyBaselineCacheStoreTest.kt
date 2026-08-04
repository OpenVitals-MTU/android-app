package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences

/**
 * Port of the Flutter `body_energy_baseline_cache_store_test.dart` suite. The
 * Kotlin store is SharedPreferences-backed, so the Dart
 * `SharedPreferences.setMockInitialValues` seeding maps onto a seeded
 * [FakeSharedPreferences].
 */
class BodyEnergyBaselineCacheStoreTest {

    private val date = LocalDate.of(2026, 7, 6)
    private val signature = "perm|calib|v2"

    private fun newStore(
        initial: Map<String, Any> = emptyMap(),
    ): Pair<BodyEnergyBaselineCacheStore, FakeSharedPreferences> {
        val prefs = FakeSharedPreferences()
        val editor = prefs.edit()
        initial.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                else -> editor.putString(key, value as String)
            }
        }
        editor.apply()
        val context: Context = mockk {
            every { getSharedPreferences(any(), any()) } returns (prefs as SharedPreferences)
        }
        return BodyEnergyBaselineCacheStore(context) to prefs
    }

    @Test fun `missing entry returns null`() {
        val (store, _) = newStore()
        assertNull(store.loadBaseline(date, signature))
    }

    @Test fun `baseline entry round-trips including nulls`() {
        val (store, _) = newStore()
        val generatedAt = Instant.ofEpochMilli(1_699_999_000_000L)
        val baseline = BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = 54L,
            observedMaxHeartRateBpm = null,
            hrvBaselineRmssdMs = 42.5,
            respiratoryRateBaseline = null,
            generatedAt = generatedAt,
        )

        store.saveBaseline(date, signature, baseline)
        val loaded = store.loadBaseline(date, signature)

        assertNotNull(loaded)
        assertEquals(54L, loaded!!.baselineRestingHeartRateBpm)
        assertNull(loaded.observedMaxHeartRateBpm)
        assertEquals(42.5, loaded.hrvBaselineRmssdMs!!, 1e-6)
        assertNull(loaded.respiratoryRateBaseline)
        assertEquals(generatedAt, loaded.generatedAt)
    }

    @Test fun `a blank signature is not persisted`() {
        val (store, _) = newStore()
        store.saveBaseline(
            date,
            "   ",
            BodyEnergyBaselineCacheEntry(
                baselineRestingHeartRateBpm = 54L,
                observedMaxHeartRateBpm = null,
                hrvBaselineRmssdMs = null,
                respiratoryRateBaseline = null,
            ),
        )
        assertNull(store.loadBaseline(date, "   "))
    }

    @Test fun `purgeLegacyTimelineEntries removes the retired timeline keys and nothing else`() {
        // The timeline half wrote `<date>|<signatureHash>`; the baselines it
        // shared the file with carry a `baseline|` prefix, and neither may be
        // confused with an ordinary preference.
        val (store, prefs) = newStore(
            mapOf(
                "2026-07-06|-1234567" to "encoded timeline",
                "2026-07-05|889900" to "encoded timeline",
                "baseline|2026-07-06|-1234567" to "54||42.5||1699999000000",
                "unit_system" to "metric",
                "body_energy_sleep_charge_gain" to 1.2,
            ),
        )

        store.purgeLegacyTimelineEntries()

        val keys = prefs.all.keys
        assertTrue(
            keys.containsAll(
                listOf(
                    "baseline|2026-07-06|-1234567",
                    "unit_system",
                    "body_energy_sleep_charge_gain",
                ),
            ),
        )
        assertFalse(keys.contains("2026-07-06|-1234567"))
        assertFalse(keys.contains("2026-07-05|889900"))
    }

    @Test fun `purgeLegacyTimelineEntries runs once - a key written afterwards survives`() {
        val (store, prefs) = newStore(mapOf("2026-07-06|-1" to "encoded"))
        store.purgeLegacyTimelineEntries()

        prefs.edit().putString("2026-07-07|-2", "written after the purge").apply()
        store.purgeLegacyTimelineEntries()

        assertEquals("written after the purge", prefs.getString("2026-07-07|-2", null))
    }
}
