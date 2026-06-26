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
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

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

    fun addDevice(
        displayName: String,
        address: String,
        bluetoothName: String?,
        capabilities: Set<BleSensorCapability>,
        wheelCircumferenceMm: Int? = null,
    ): BleSensorDevice {
        val normalizedAddress = address.uppercase()
        val existing = devices.firstOrNull { it.address.equals(normalizedAddress, ignoreCase = true) }
        if (existing != null) {
            return updateDevice(
                deviceId = existing.id,
                displayName = displayName,
                capabilities = capabilities,
                enabled = true,
                wheelCircumferenceMm = wheelCircumferenceMm ?: existing.wheelCircumferenceMm,
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
        ).normalized()
        persist(devices + device)
        return device
    }

    fun updateDevice(
        deviceId: String,
        displayName: String? = null,
        capabilities: Set<BleSensorCapability>? = null,
        enabled: Boolean? = null,
        wheelCircumferenceMm: Int? = null,
    ): BleSensorDevice {
        val current = devices.firstOrNull { it.id == deviceId }
            ?: error("Unknown BLE device: $deviceId")
        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            capabilities = capabilities ?: current.capabilities,
            enabled = enabled ?: current.enabled,
            wheelCircumferenceMm = wheelCircumferenceMm ?: current.wheelCircumferenceMm,
        ).normalized()
        persist(devices.map { if (it.id == deviceId) updated else it })
        return updated
    }

    fun removeDevice(deviceId: String) {
        persist(devices.filterNot { it.id == deviceId })
    }

    fun setDeviceEnabled(deviceId: String, enabled: Boolean) {
        updateDevice(deviceId = deviceId, enabled = enabled)
    }

    private fun persist(nextDevices: List<BleSensorDevice>) {
        prefs.edit {
            putString(KEY_DEVICES, nextDevices.encodeDevices())
        }
        _devices.value = nextDevices
    }

    private fun readDevices(): List<BleSensorDevice> =
        prefs.getString(KEY_DEVICES, null)?.decodeDevices().orEmpty()

    private companion object {
        const val PREFS_FILE = "ble_sensor_devices"
        const val KEY_DEVICES = "devices"

        fun List<BleSensorDevice>.encodeDevices(): String =
            JSONArray(
                map { device ->
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
                        .put("addedAt", device.addedAt.toEpochMilli())
                },
            ).toString()

        fun String.decodeDevices(): List<BleSensorDevice> =
            runCatching {
                val array = JSONArray(this)
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
                                addedAt = Instant.ofEpochMilli(item.getLong("addedAt")),
                            ).normalized(),
                        )
                    }
                }
            }.getOrDefault(emptyList())
    }
}
