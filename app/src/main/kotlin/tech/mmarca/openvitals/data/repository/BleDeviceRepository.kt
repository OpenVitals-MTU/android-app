package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

@Singleton
class BleDeviceRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val _devices = MutableStateFlow(readDevices())
    val devicesFlow: StateFlow<List<BleSensorDevice>> = _devices.asStateFlow()

    val devices: List<BleSensorDevice>
        get() = _devices.value

    val enabledDevices: List<BleSensorDevice>
        get() = devices.filter { it.enabled }

    fun refresh() {
        _devices.value = readDevices()
    }

    fun resolveCapabilityAssignments(): Map<BleSensorCapability, BleSensorDevice> {
        val assignments = linkedMapOf<BleSensorCapability, BleSensorDevice>()
        enabledDevices.forEach { device ->
            // A live sensor, an Edge bike computer, or a watch the user added
            // through the Sensors path as well — all three take part once they
            // carry capabilities. A sync-only watch has none, so it stays out
            // and the recording coordinator never connects to it to wait for
            // notifications it does not send.
            if (!device.isLiveSensorCapable) return@forEach
            device.capabilities.forEach { capability ->
                assignments.putIfAbsent(capability, device)
            }
        }
        return assignments
    }

    fun capabilityConflicts(
        capabilities: Set<BleSensorCapability>,
        excludingDeviceId: String? = null,
    ): Map<BleSensorCapability, BleSensorDevice> =
        resolveCapabilityAssignments()
            .filter { (capability, device) ->
                capability in capabilities && device.id != excludingDeviceId
            }

    /**
     * Registers [address], or folds these fields into the entry that already
     * holds it.
     *
     * The two add flows own different halves of a device: the Sensors screen
     * owns [capabilities], the watch onboarding owns [kind] and [integration].
     * So on an update each half leaves the other's alone — a null [kind] keeps
     * the stored kind, an empty [capabilities] keeps the stored set. That is
     * what lets one vívoactive be a GFDI sync watch AND a live heart-rate
     * sensor, instead of whichever flow ran last silently demoting the other.
     * A new device with no [kind] is a plain [BleDeviceKind.SENSOR].
     *
     * Clearing capabilities is the edit sheet's job, through [updateDevice].
     */
    fun addDevice(
        displayName: String,
        address: String,
        bluetoothName: String?,
        capabilities: Set<BleSensorCapability>,
        wheelCircumferenceMm: Int? = null,
        kind: BleDeviceKind? = null,
        integration: DeviceIntegration? = null,
    ): BleSensorDevice {
        val normalizedAddress = address.uppercase()
        val existing = devices.firstOrNull { it.address.equals(normalizedAddress, ignoreCase = true) }
        if (existing != null) {
            return updateDevice(
                deviceId = existing.id,
                displayName = displayName,
                bluetoothName = bluetoothName,
                capabilities = capabilities.takeIf { it.isNotEmpty() },
                enabled = true,
                wheelCircumferenceMm = wheelCircumferenceMm ?: existing.wheelCircumferenceMm,
                kind = kind,
                integration = integration,
            )
        }
        val device = BleSensorDevice(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            address = normalizedAddress,
            bluetoothName = bluetoothName,
            capabilities = capabilities,
            enabled = true,
            wheelCircumferenceMm = wheelCircumferenceMm,
            addedAt = Instant.now(),
            kind = kind ?: BleDeviceKind.SENSOR,
            integration = integration,
        ).normalized()
        persist(devices + device)
        return device
    }

    fun updateDevice(
        deviceId: String,
        displayName: String? = null,
        /** The advertised name, which a re-add refreshes; null keeps the stored one. */
        bluetoothName: String? = null,
        capabilities: Set<BleSensorCapability>? = null,
        enabled: Boolean? = null,
        wheelCircumferenceMm: Int? = null,
        kind: BleDeviceKind? = null,
        integration: DeviceIntegration? = null,
    ): BleSensorDevice {
        val current = devices.firstOrNull { it.id == deviceId }
            ?: error("Unknown BLE device: $deviceId")
        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            bluetoothName = bluetoothName ?: current.bluetoothName,
            capabilities = capabilities ?: current.capabilities,
            enabled = enabled ?: current.enabled,
            wheelCircumferenceMm = wheelCircumferenceMm ?: current.wheelCircumferenceMm,
            kind = kind ?: current.kind,
            integration = integration ?: current.integration,
        ).normalized()
        persist(devices.map { if (it.id == deviceId) updated else it })
        return updated
    }

    fun removeDevice(deviceId: String) {
        // A watch's Garmin-specific state (synced-file history + declared
        // capabilities) is cleared separately, by whoever forgets the watch,
        // via `GarminDeviceStateStore.clear` — this registry does not hold it.
        persist(devices.filterNot { it.id == deviceId })
    }

    fun setDeviceEnabled(deviceId: String, enabled: Boolean) {
        updateDevice(deviceId = deviceId, enabled = enabled)
    }

    fun updateBatteryLevel(deviceId: String, batteryPercent: Int) {
        val percent = batteryPercent.coerceIn(0, 100)
        // Only a changed reading is worth a write: a sensor that reports the
        // same percent every notification must not churn storage and the stream,
        // nor advance `batteryUpdatedAt` — the stamp says when the level last
        // moved, not when it was last heard.
        val current = devices.firstOrNull { it.id == deviceId } ?: return
        if (current.batteryPercent == percent) return
        persist(
            devices.map { device ->
                if (device.id == deviceId) {
                    device.copy(
                        batteryPercent = percent,
                        batteryUpdatedAt = Instant.now(),
                    ).normalized()
                } else {
                    device
                }
            },
        )
    }

    /** Stamps a watch's last completed file pull. A no-op for an unknown id. */
    fun markSynced(deviceId: String, at: Instant) {
        if (devices.none { it.id == deviceId }) return
        persist(
            devices.map { device ->
                if (device.id == deviceId) device.copy(lastSyncedAt = at) else device
            },
        )
    }

    private fun persist(nextDevices: List<BleSensorDevice>) {
        prefs.edit {
            putString(KEY_DEVICES, BleDeviceRegistryJson.encode(nextDevices))
        }
        _devices.value = nextDevices
    }

    private fun readDevices(): List<BleSensorDevice> =
        prefs.getString(KEY_DEVICES, null)
            ?.let(BleDeviceRegistryJson::decode)
            .orEmpty()

    private companion object {
        const val PREFS_FILE = "ble_sensor_devices"
        const val KEY_DEVICES = "devices"
    }
}

/**
 * The registry's JSON wire format, shared by the Kotlin build and the JSON the
 * retired Flutter build wrote (phase 5 copies that string over verbatim).
 *
 * Reading is deliberately tolerant:
 * - `kind` / `integration` / `lastSyncedAt` are absent on every device stored
 *   before watches existed — those are all sensors, which is exactly what the
 *   [BleDeviceKind.SENSOR] / null fallbacks say.
 * - An unknown enum value (a future build's kind) degrades to the same
 *   fallbacks rather than dropping the device or crashing the decode.
 */
internal object BleDeviceRegistryJson {

    fun encode(devices: List<BleSensorDevice>): String =
        JSONArray(
            devices.map { device ->
                JSONObject()
                    .put("id", device.id)
                    .put("displayName", device.displayName)
                    .put("address", device.address)
                    .put("bluetoothName", device.bluetoothName ?: JSONObject.NULL)
                    .put(
                        "capabilities",
                        JSONArray(device.capabilities.map { it.name }),
                    )
                    .put("enabled", device.enabled)
                    .put(
                        "wheelCircumferenceMm",
                        device.wheelCircumferenceMm ?: JSONObject.NULL,
                    )
                    .put("batteryPercent", device.batteryPercent ?: JSONObject.NULL)
                    .put(
                        "batteryUpdatedAt",
                        device.batteryUpdatedAt?.toEpochMilli() ?: JSONObject.NULL,
                    )
                    .put("addedAt", device.addedAt.toEpochMilli())
                    .put("kind", device.kind.storageName)
                    .put("integration", device.integration?.storageName ?: JSONObject.NULL)
                    .put("lastSyncedAt", device.lastSyncedAt?.toEpochMilli() ?: JSONObject.NULL)
            },
        ).toString()

    fun decode(json: String): List<BleSensorDevice> =
        runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val capabilities = buildSet {
                        val caps = item.optJSONArray("capabilities")
                        if (caps != null) {
                            for (capIndex in 0 until caps.length()) {
                                val name = caps.optString(capIndex)
                                runCatching { add(BleSensorCapability.valueOf(name)) }
                            }
                        }
                    }
                    add(
                        BleSensorDevice(
                            id = item.getString("id"),
                            displayName = item.getString("displayName"),
                            address = item.getString("address"),
                            bluetoothName = item.optString("bluetoothName").takeIf { it.isNotBlank() },
                            capabilities = capabilities,
                            enabled = item.optBoolean("enabled", true),
                            wheelCircumferenceMm = item.opt("wheelCircumferenceMm")
                                .takeIf { it != JSONObject.NULL }
                                ?.let { (it as Number).toInt() }
                                ?.takeIf { it > 0 },
                            batteryPercent = item.opt("batteryPercent")
                                .takeIf { it != null && it != JSONObject.NULL }
                                ?.let { (it as Number).toInt() },
                            batteryUpdatedAt = item.opt("batteryUpdatedAt")
                                .takeIf { it != null && it != JSONObject.NULL }
                                ?.let { Instant.ofEpochMilli((it as Number).toLong()) },
                            addedAt = Instant.ofEpochMilli(item.getLong("addedAt")),
                            // Absent for every device stored before watches
                            // existed — those are all sensors, which is exactly
                            // what the fallback says. Unknown values degrade
                            // the same way rather than crashing the decode.
                            kind = item.optString("kind")
                                .let { BleDeviceKind.fromStorage(it) }
                                ?: BleDeviceKind.SENSOR,
                            // Absent for a sensor and for a Garmin watch stored
                            // before this field — null reads back as Garmin via
                            // `isGarminWatch`, so nothing migrates.
                            integration = item.optString("integration")
                                .let { DeviceIntegration.fromStorage(it) },
                            lastSyncedAt = item.opt("lastSyncedAt")
                                .takeIf { it != null && it != JSONObject.NULL }
                                ?.let { Instant.ofEpochMilli((it as Number).toLong()) },
                        ).normalized(),
                    )
                }
            }
        }.getOrDefault(emptyList())
}
