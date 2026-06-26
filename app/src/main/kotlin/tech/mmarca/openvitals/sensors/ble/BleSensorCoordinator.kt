package tech.mmarca.openvitals.sensors.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

@Singleton
class BleSensorCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: BleDeviceRepository,
) {
    private val bleThread = HandlerThread("OpenVitalsBle").apply { start() }
    private val bleHandler = Handler(bleThread.looper)
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val connections = linkedMapOf<String, BleGattConnection>()
    private val capabilityOwners = mutableMapOf<BleSensorCapability, BleSensorDevice>()
    private var sampleBuffer = BleRecordingSampleBuffer()
    private var recordingActive = false

    private val _metrics = MutableStateFlow(BleRecordingMetrics())
    val metrics: StateFlow<BleRecordingMetrics> = _metrics.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var scanCallback: ScanCallback? = null
    private val scanResults = ConcurrentHashMap<String, BleDiscoveredDevice>()

    fun currentSampleBuffer(): BleRecordingSampleBuffer = sampleBuffer

    fun startRecording() {
        recordingActive = true
        sampleBuffer = BleRecordingSampleBuffer()
        val desiredAssignments = deviceRepository.resolveCapabilityAssignments()
        if (connections.isEmpty() || capabilityOwners.toMap() != desiredAssignments) {
            refreshConnections()
        } else {
            publishMetrics()
        }
    }

    fun stopRecording(): BleRecordingSampleBuffer {
        recordingActive = false
        disconnectAll()
        val buffer = sampleBuffer.trimmed()
        sampleBuffer = BleRecordingSampleBuffer()
        _metrics.value = BleRecordingMetrics()
        return buffer
    }

    fun refreshConnections() {
        disconnectAll()
        capabilityOwners.clear()
        deviceRepository.resolveCapabilityAssignments().forEach { (capability, device) ->
            capabilityOwners[capability] = device
        }
        val grouped = capabilityOwners.entries.groupBy({ it.value.address }, { it.key })
        grouped.forEach { (address, capabilities) ->
            val device = capabilityOwners.values.first { it.address == address }
            val connection = BleGattConnection(
                context = context,
                deviceId = device.id,
                displayName = device.displayName,
                address = address,
                capabilities = capabilities.toSet(),
                wheelCircumferenceMm = device.wheelCircumferenceMm,
                listener = connectionListener,
                callbackHandler = bleHandler,
            )
            connections[address] = connection
            connection.connect()
        }
        publishMetrics()
    }

    fun disconnectAll() {
        connections.values.forEach { it.disconnect() }
        connections.clear()
        capabilityOwners.clear()
        publishMetrics()
    }

    @SuppressLint("MissingPermission")
    fun startScan(showAllDevices: Boolean = false) {
        stopScan()
        scanResults.clear()
        publishScanResults()
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        val filters = if (showAllDevices) {
            emptyList()
        } else {
            BleUuids.SCAN_SERVICE_UUIDS.map { serviceUuid ->
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(serviceUuid))
                    .build()
            }
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                addScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { addScanResult(it) }
            }
        }
        scanCallback = callback
        adapter.bluetoothLeScanner?.startScan(
            filters,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            callback,
        )
        bondedDevices().forEach { device ->
            scanResults.putIfAbsent(
                device.address.uppercase(),
                BleDiscoveredDevice(
                    address = device.address.uppercase(),
                    name = device.name,
                    rssi = null,
                    suggestedCapabilities = emptySet(),
                ),
            )
        }
        publishScanResults()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let { callback ->
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
        }
        scanCallback = null
    }

    @SuppressLint("MissingPermission")
    suspend fun discoverCapabilities(address: String): Set<BleSensorCapability> =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val adapter = bluetoothAdapter
                if (adapter == null || !adapter.isEnabled) {
                    continuation.resume(emptySet())
                    return@suspendCancellableCoroutine
                }
                val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
                if (device == null) {
                    continuation.resume(emptySet())
                    return@suspendCancellableCoroutine
                }

                val discovered = mutableSetOf<BleSensorCapability>()
                var gatt: BluetoothGatt? = null
                var finished = false
                var timeoutRunnable: Runnable? = null

                fun complete(result: Set<BleSensorCapability>) {
                    if (finished || !continuation.isActive) return
                    finished = true
                    timeoutRunnable?.let { bleHandler.removeCallbacks(it) }
                    runCatching { gatt?.close() }
                    gatt = null
                    continuation.resume(result)
                }

                timeoutRunnable = Runnable { complete(discovered.toSet()) }

                continuation.invokeOnCancellation {
                    finished = true
                    timeoutRunnable?.let { bleHandler.removeCallbacks(it) }
                    runCatching { gatt?.disconnect() }
                    runCatching { gatt?.close() }
                    gatt = null
                }

                gatt = device.connectGatt(
                    context,
                    false,
                    object : BluetoothGattCallback() {
                        @SuppressLint("MissingPermission")
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt,
                            status: Int,
                            newState: Int,
                        ) {
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                                BluetoothProfile.STATE_DISCONNECTED -> complete(discovered.toSet())
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                gatt.services.forEach { service ->
                                    discovered.addAll(BleUuids.capabilitiesForService(service.uuid))
                                }
                            }
                            runCatching { gatt.disconnect() }
                        }
                    },
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M,
                    bleHandler,
                )
                bleHandler.postDelayed(timeoutRunnable!!, CAPABILITY_DISCOVERY_TIMEOUT_MS)
            }
        }

    @SuppressLint("MissingPermission")
    private fun bondedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.filter {
            it.type == BluetoothDevice.DEVICE_TYPE_LE ||
                it.type == BluetoothDevice.DEVICE_TYPE_DUAL
        }.orEmpty()

    @SuppressLint("MissingPermission")
    private fun addScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address.uppercase()
        val serviceCapabilities = result.scanRecord?.serviceUuids
            ?.flatMap { BleUuids.capabilitiesForService(it.uuid) }
            ?.toSet()
            .orEmpty()
        val existing = scanResults[address]
        scanResults[address] = BleDiscoveredDevice(
            address = address,
            name = device.name ?: existing?.name,
            rssi = result.rssi,
            suggestedCapabilities = (existing?.suggestedCapabilities.orEmpty() + serviceCapabilities).toSet(),
        )
        publishScanResults()
    }

    private fun publishScanResults() {
        _discoveredDevices.value = scanResults.values.sortedWith(
            compareByDescending<BleDiscoveredDevice> { it.rssi ?: Int.MIN_VALUE }
                .thenBy { it.displayLabel() },
        )
    }

    private val connectionListener = object : BleConnectionListener {
        override fun onConnectionStatusChanged(status: BleConnectionStatus) {
            publishMetrics()
        }

        override fun onMetricsUpdated() {
            val now = Instant.now()
            val metrics = collectMetrics(now)
            _metrics.value = metrics
            if (recordingActive) {
                appendSamples(now, metrics)
            }
        }
    }

    private fun collectMetrics(now: Instant = Instant.now()): BleRecordingMetrics {
        val statuses = connections.map { (address, connection) ->
            val device = capabilityOwners.values.firstOrNull { it.address == address }
            BleDeviceConnectionStatus(
                deviceId = device?.id.orEmpty(),
                displayName = device?.displayName ?: address,
                address = address,
                status = connection.connectionStatus,
                capabilities = capabilityOwners.filterValues { it.address == address }.keys,
            )
        }
        val hrConnection = connectionForCapability(BleSensorCapability.HEART_RATE)
        val cadenceConnection = connectionForCapability(BleSensorCapability.CYCLING_CADENCE)
            ?: connectionForCapability(BleSensorCapability.CYCLING_POWER)
        val powerConnection = connectionForCapability(BleSensorCapability.CYCLING_POWER)
        val speedConnection = connectionForCapability(BleSensorCapability.CYCLING_SPEED_DISTANCE)
        val runningConnection = connectionForCapability(BleSensorCapability.RUNNING_SPEED_CADENCE)
        val running = runningConnection?.runningAggregator?.current(now)
        return BleRecordingMetrics(
            heartRateBpm = hrConnection?.heartRateAggregator?.current(now),
            heartRateNoSignal = hrConnection?.heartRateNoSignal == true,
            cyclingCadenceRpm = cadenceConnection?.cyclingCadenceAggregator?.current(now),
            powerWatts = powerConnection?.powerAggregator?.current(now),
            cyclingSpeedMetersPerSecond = speedConnection?.cyclingSpeedAggregator?.current(now),
            runningSpeedMetersPerSecond = running?.first,
            runningCadenceRpm = running?.second,
            deviceStatuses = statuses,
        )
    }

    private fun appendSamples(now: Instant, metrics: BleRecordingMetrics) {
        var next = sampleBuffer
        metrics.heartRateBpm?.let { next = next.withHeartRateSample(now, it) }
        metrics.powerWatts?.let { next = next.withPowerSample(now, it) }
        metrics.cyclingCadenceRpm?.let { next = next.withCyclingCadenceSample(now, it) }
        metrics.cyclingSpeedMetersPerSecond?.let {
            next = next.withSpeedSample(now, it, isRunning = false)
        }
        metrics.runningSpeedMetersPerSecond?.let {
            next = next.withSpeedSample(now, it, isRunning = true)
        }
        metrics.runningCadenceRpm?.let {
            next = next.withStepsCadenceSample(now, it)
        }
        sampleBuffer = next.trimmed()
    }

    private fun connectionForCapability(capability: BleSensorCapability): BleGattConnection? {
        val device = capabilityOwners[capability] ?: return null
        return connections[device.address]
    }

    private fun publishMetrics() {
        _metrics.value = collectMetrics()
    }

    private fun BleDiscoveredDevice.displayLabel(): String =
        name?.ifBlank { address } ?: address

    companion object {
        private const val CAPABILITY_DISCOVERY_TIMEOUT_MS = 8_000L
    }
}
