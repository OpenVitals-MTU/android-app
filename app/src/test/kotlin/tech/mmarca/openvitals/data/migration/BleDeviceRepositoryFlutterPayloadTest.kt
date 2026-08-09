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
 * The Flutter registry JSON may still carry retired `kind` / `integration` /
 * `lastSyncedAt` fields. Migration copies the payload verbatim into
 * `ble_sensor_devices`, so [BleDeviceRepository] must tolerate them: sensors
 * load normally, and `"kind": "watch"` rows are dropped.
 */
class BleDeviceRepositoryFlutterPayloadTest {

    @Test
    fun `decode keeps sensors and drops flutter era watch entries`() {
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
        assertThat(devices).hasSize(1)

        val sensor = devices.single()
        assertThat(sensor.id).isEqualTo("3f0a2c9e-1111-2222-3333-444455556666")
        assertThat(sensor.displayName).isEqualTo("Chest strap")
        assertThat(sensor.address).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(sensor.capabilities).containsExactly(BleSensorCapability.HEART_RATE)
        assertThat(sensor.enabled).isTrue()
        assertThat(sensor.batteryPercent).isEqualTo(85)
    }

    private fun contextWithStoredDevices(json: String): Context {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getString("devices", null) } returns json
        val context = mockk<Context>()
        every { context.getSharedPreferences("ble_sensor_devices", Context.MODE_PRIVATE) } returns prefs
        return context
    }
}
