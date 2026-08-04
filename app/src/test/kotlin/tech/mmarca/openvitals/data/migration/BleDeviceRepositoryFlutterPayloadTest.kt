package tech.mmarca.openvitals.data.migration

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.BleSensorCapability

/**
 * The Flutter registry JSON carries extra `kind` / `integration` /
 * `lastSyncedAt` fields (added for the watch integrations) that the current
 * Kotlin decoder does not know. The migration copies the payload VERBATIM into
 * `ble_sensor_devices`, so [BleDeviceRepository] must tolerate — i.e. silently
 * ignore — those fields rather than fail parsing. Phase 7 will start reading
 * them.
 *
 * Runs on the local JVM against the real `org.json` artifact (declared as a
 * test dependency; the android.jar copy is a throwing stub).
 */
class BleDeviceRepositoryFlutterPayloadTest {

    @Test
    fun `decode tolerates flutter era kind and integration fields`() {
        val flutterPayload = """
            [
              {
                "id": "3f0a2c9e-1111-2222-3333-444455556666",
                "displayName": "Chest strap",
                "address": "AA:BB:CC:DD:EE:FF",
                "bluetoothName": "Polar H10",
                "capabilities": ["HEART_RATE"],
                "enabled": true,
                "wheelCircumferenceMm": null,
                "batteryPercent": 85,
                "batteryUpdatedAt": 1752000000000,
                "addedAt": 1710000000000,
                "kind": "sensor",
                "integration": null,
                "lastSyncedAt": null
              },
              {
                "id": "watch-1",
                "displayName": "vívoactive 5",
                "address": "11:22:33:44:55:66",
                "bluetoothName": "vivoactive 5",
                "capabilities": [],
                "enabled": true,
                "wheelCircumferenceMm": null,
                "batteryPercent": null,
                "batteryUpdatedAt": null,
                "addedAt": 1751000000000,
                "kind": "watch",
                "integration": "garmin",
                "lastSyncedAt": 1752600000000
              }
            ]
        """.trimIndent()

        val repository = BleDeviceRepository(contextWithStoredDevices(flutterPayload))

        val devices = repository.devices
        assertThat(devices).hasSize(2)

        val sensor = devices.first { it.id == "3f0a2c9e-1111-2222-3333-444455556666" }
        assertThat(sensor.displayName).isEqualTo("Chest strap")
        assertThat(sensor.address).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(sensor.capabilities).containsExactly(BleSensorCapability.HEART_RATE)
        assertThat(sensor.enabled).isTrue()
        assertThat(sensor.batteryPercent).isEqualTo(85)

        // The watch entry parses too — its unknown fields are simply ignored.
        val watch = devices.first { it.id == "watch-1" }
        assertThat(watch.displayName).isEqualTo("vívoactive 5")
        assertThat(watch.capabilities).isEmpty()
    }

    private fun contextWithStoredDevices(json: String): Context {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getString("devices", null) } returns json
        val context = mockk<Context>()
        every { context.getSharedPreferences("ble_sensor_devices", Context.MODE_PRIVATE) } returns prefs
        return context
    }
}
