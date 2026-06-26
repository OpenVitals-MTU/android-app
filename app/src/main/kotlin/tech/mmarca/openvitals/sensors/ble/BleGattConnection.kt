package tech.mmarca.openvitals.sensors.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.UUID
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.sensors.ble.aggregators.BleCyclingCadenceAggregator
import tech.mmarca.openvitals.sensors.ble.aggregators.BleCyclingSpeedAggregator
import tech.mmarca.openvitals.sensors.ble.aggregators.BleHeartRateAggregator
import tech.mmarca.openvitals.sensors.ble.aggregators.BlePowerAggregator
import tech.mmarca.openvitals.sensors.ble.aggregators.BleRunningSpeedCadenceAggregator
import tech.mmarca.openvitals.sensors.ble.parsers.BleCyclingPowerParser
import tech.mmarca.openvitals.sensors.ble.parsers.BleCyclingSpeedCadenceParser
import tech.mmarca.openvitals.sensors.ble.parsers.BleHeartRateParser
import tech.mmarca.openvitals.sensors.ble.parsers.BleRunningSpeedCadenceParser

internal interface BleConnectionListener {
    fun onConnectionStatusChanged(status: BleConnectionStatus)
    fun onMetricsUpdated()
}

internal class BleGattConnection(
    private val context: Context,
    private val deviceId: String,
    private val displayName: String,
    private val address: String,
    private val capabilities: Set<BleSensorCapability>,
    private val wheelCircumferenceMm: Int?,
    private val listener: BleConnectionListener,
    private val callbackHandler: Handler,
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    private var closed = false
    var connectionStatus: BleConnectionStatus = BleConnectionStatus.DISCONNECTED
        private set

    val heartRateAggregator = BleHeartRateAggregator()
    val powerAggregator = BlePowerAggregator()
    val cyclingCadenceAggregator = BleCyclingCadenceAggregator()
    val cyclingSpeedAggregator = BleCyclingSpeedAggregator(
        wheelCircumferenceMeters = (wheelCircumferenceMm ?: 2_100) / 1_000.0,
    )
    val runningAggregator = BleRunningSpeedCadenceAggregator()

    private val subscribedCharacteristics = mutableSetOf<UUID>()
    var heartRateNoSignal: Boolean = false
        private set

    @SuppressLint("MissingPermission")
    fun connect() {
        if (closed) return
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            updateStatus(BleConnectionStatus.DISCONNECTED)
            return
        }
        if (gatt != null) return
        updateStatus(BleConnectionStatus.CONNECTING)
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        gatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE,
            BluetoothDevice.PHY_LE_1M,
            callbackHandler,
        )
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        closed = true
        subscribedCharacteristics.clear()
        resetAggregators()
        gatt?.let { currentGatt ->
            runCatching { currentGatt.disconnect() }
            runCatching { currentGatt.close() }
        }
        gatt = null
        updateStatus(BleConnectionStatus.DISCONNECTED)
    }

    private fun updateStatus(status: BleConnectionStatus) {
        connectionStatus = status
        listener.onConnectionStatusChanged(status)
    }

    fun resetAggregators() {
        heartRateAggregator.reset()
        powerAggregator.reset()
        cyclingCadenceAggregator.reset()
        cyclingSpeedAggregator.reset()
        runningAggregator.reset()
        heartRateNoSignal = false
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        if (!subscribedCharacteristics.add(characteristic.uuid)) return
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BleUuids.CLIENT_CHARACTERISTIC_CONFIG) ?: return
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTING ->
                    updateStatus(BleConnectionStatus.CONNECTING)
                BluetoothProfile.STATE_CONNECTED -> {
                    updateStatus(BleConnectionStatus.CONNECTED)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    subscribedCharacteristics.clear()
                    resetAggregators()
                    if (!closed) {
                        updateStatus(BleConnectionStatus.RECONNECTING)
                        gatt.connect()
                    } else {
                        updateStatus(BleConnectionStatus.DISCONNECTED)
                        runCatching { gatt.close() }
                        this@BleGattConnection.gatt = null
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed status=$status")
                return
            }
            gatt.services.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    val charCapabilities = BleUuids.capabilitiesForCharacteristic(characteristic.uuid)
                    if (charCapabilities.any { it in capabilities }) {
                        enableNotifications(gatt, characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleCharacteristicChanged(
                gatt,
                characteristic,
                @Suppress("DEPRECATION")
                characteristic.value ?: byteArrayOf(),
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (value.isEmpty()) return
        val now = java.time.Instant.now()
        val sensorName = gatt.device.name
        when {
            BleSensorCapability.HEART_RATE in capabilities &&
                characteristic.uuid == BleUuids.HEART_RATE.measurementUuid -> {
                val heartRate = BleHeartRateParser.parseBytes(value)
                when {
                    heartRate != null -> {
                        heartRateNoSignal = false
                        heartRateAggregator.add(now, heartRate)
                        listener.onMetricsUpdated()
                    }
                    BleHeartRateParser.isZeroSignal(value) -> {
                        heartRateNoSignal = true
                        listener.onMetricsUpdated()
                    }
                    else -> Log.d(
                        TAG,
                        "Heart rate notification unparsed bytes=${value.size}",
                    )
                }
            }
            BleSensorCapability.CYCLING_POWER in capabilities &&
                characteristic.uuid == BleUuids.CYCLING_POWER.measurementUuid -> {
                BleCyclingPowerParser.parsePayload(value)?.let { data ->
                    powerAggregator.add(now, data)
                    data.crank?.let { cyclingCadenceAggregator.add(now, it) }
                    listener.onMetricsUpdated()
                }
            }
            (BleSensorCapability.CYCLING_CADENCE in capabilities ||
                BleSensorCapability.CYCLING_SPEED_DISTANCE in capabilities) &&
                characteristic.uuid == BleUuids.CYCLING_SPEED_CADENCE.measurementUuid -> {
                BleCyclingSpeedCadenceParser.parsePayload(value)?.let { (wheel, crank) ->
                    wheel?.let { cyclingSpeedAggregator.add(now, it) }
                    crank?.let { cyclingCadenceAggregator.add(now, it) }
                    listener.onMetricsUpdated()
                }
            }
            BleSensorCapability.RUNNING_SPEED_CADENCE in capabilities &&
                characteristic.uuid == BleUuids.RUNNING_SPEED_CADENCE.measurementUuid -> {
                BleRunningSpeedCadenceParser.parsePayload(value, sensorName)?.let {
                    runningAggregator.add(now, it)
                    listener.onMetricsUpdated()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BleGattConnection"
    }
}
