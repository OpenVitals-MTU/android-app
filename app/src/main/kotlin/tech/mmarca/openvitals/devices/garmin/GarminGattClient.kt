package tech.mmarca.openvitals.devices.garmin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Thrown when the watch cannot be reached or does not expose the V2 transport. */
class GarminGattClientException(message: String) : Exception(message)

/**
 * The one file in the Garmin stack that touches `android.bluetooth`. It
 * connects, finds the multi-link characteristic pair and wires it to
 * [GarminMlTransport]. A separate class from `BleGattConnection`: a sync is
 * bounded, so a dropped link should end it, not redial. One instance drives
 * one connection attempt.
 */
class GarminGattClient(
    private val context: Context,
    private val address: String,
    private val onLog: ((String) -> Unit)? = null,
) {

    private companion object {
        /** Gadgetbridge asks for 515. A bigger MTU is the largest factor in sync speed. */
        const val DESIRED_MTU = 515

        /** Long, because a connect right after bonding may find the watch still settling. */
        val CONNECT_TIMEOUT: Duration = 20.seconds
        val DISCOVER_TIMEOUT: Duration = 10.seconds
        val MTU_TIMEOUT: Duration = 5.seconds
        val SUBSCRIBE_TIMEOUT: Duration = 5.seconds
        val WRITE_TIMEOUT: Duration = 5.seconds
        val CHANNEL_OPEN_TIMEOUT: Duration = 15.seconds

        /** The standard CCCD, needed to switch a characteristic's notifications on. */
        val CLIENT_CHARACTERISTIC_CONFIG: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var gatt: BluetoothGatt? = null

    @Volatile
    private var closed = false

    private var ml: GarminMlTransport? = null
    private var sendCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    private var receiveUuid: UUID? = null

    /** Fires when the link drops mid-session, so the caller can abort cleanly. */
    private val disconnected = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val onDisconnected: SharedFlow<String> = disconnected

    /** Runs the GFDI re-registration when the watch closes the handle. Invisible to callers. */
    private val healScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Single-shot bridges from the GATT callback into coroutines.
    private val connectedSignal = CompletableDeferred<Unit>()
    private val servicesSignal = CompletableDeferred<List<BluetoothGattService>>()
    private val mtuSignal = CompletableDeferred<Int>()

    @Volatile
    private var descriptorWritten: CompletableDeferred<Unit>? = null

    @Volatile
    private var writeCompleted: CompletableDeferred<Unit>? = null

    /** Android GATT is one-operation-at-a-time; writes are serialised here. */
    private val writeMutex = Mutex()

    private fun log(message: String) {
        GarminLog.log(message)
        onLog?.invoke(message)
    }

    private val callback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> connectedSignal.complete(Unit)
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (!closed) {
                        log("[GARMIN-BLE] link dropped")
                        disconnected.tryEmit("link dropped")
                    }
                    val gone = GarminGattClientException("Link dropped")
                    connectedSignal.completeExceptionally(gone)
                    servicesSignal.completeExceptionally(gone)
                    descriptorWritten?.completeExceptionally(gone)
                    writeCompleted?.completeExceptionally(gone)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuSignal.complete(if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                servicesSignal.complete(gatt.services.orEmpty())
            } else {
                servicesSignal.completeExceptionally(
                    GarminGattClientException("Service discovery failed (status $status)"),
                )
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            val waiter = descriptorWritten ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                waiter.complete(Unit)
            } else {
                waiter.completeExceptionally(
                    GarminGattClientException("Descriptor write failed (status $status)"),
                )
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            writeCompleted?.complete(Unit)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotification(characteristic, value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleNotification(
                characteristic,
                @Suppress("DEPRECATION")
                characteristic.value ?: byteArrayOf(),
            )
        }
    }

    private fun handleNotification(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (characteristic.uuid == receiveUuid) {
            ml?.handleInbound(value)
        }
    }

    /**
     * Connects, opens the GFDI channel and returns the transport. Throws
     * [GarminGattClientException] when unreachable or when there is no V2
     * pair (a V1 device, not implemented).
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        onFrame: (GarminGfdiFrame) -> Unit,
        onRealtime: ((GarminRealtimeService, GarminRealtimeReading) -> Unit)? = null,
    ): GarminMlTransport {
        val (services, mtu) = connectAndDiscover()
        val pair = findMlPair(services)
        if (pair == null) {
            close()
            throw GarminGattClientException(
                "No Garmin multi-link characteristics — this watch is not V2",
            )
        }
        val (receive, send) = pair
        sendCharacteristic = send
        receiveUuid = receive.uuid
        log("[GARMIN-BLE] using receive=${receive.uuid} send=${send.uuid} mtu=$mtu")

        val transport = GarminMlTransport(
            write = { packet -> writeToCharacteristic(packet) },
            onFrame = onFrame,
            onServiceData = { serviceCode, payload ->
                // Live readings are unframed and continuous; drop unparseable ones quietly.
                val service = GarminRealtimeParser.serviceFor(serviceCode)
                if (service != null) {
                    GarminRealtimeParser.parse(service, payload)?.let { reading ->
                        onRealtime?.invoke(service, reading)
                    }
                } else {
                    log("[GARMIN-ML] data on unclaimed service $serviceCode")
                }
            },
            onGfdiClosed = {
                // Re-register rather than tear down: on a held link this is recoverable.
                healScope.launch {
                    runCatching { ml?.reopenGfdi() }
                        .onFailure { log("[GARMIN-BLE] GFDI reopen failed: $it") }
                }
            },
            onLog = ::log,
        )
        transport.onMtuChanged(mtu)
        ml = transport

        // Subscribe before opening the channel, or a fast response is missed.
        try {
            subscribe(receive)
        } catch (error: Exception) {
            close()
            throw GarminGattClientException("Could not subscribe: ${error.message}")
        }

        transport.open()
        try {
            withTimeout(CHANNEL_OPEN_TIMEOUT) { transport.ready.await() }
        } catch (error: TimeoutCancellationException) {
            close()
            throw GarminGattClientException("Watch did not open the GFDI channel")
        }
        return transport
    }

    /** Connects, enumerates the GATT table and hangs up. The probe path. */
    suspend fun enumerateServices(): List<GarminGattService> {
        val (services, _) = connectAndDiscover(requestMtu = false)
        return services.map { service ->
            GarminGattService(
                uuid = service.uuid.toString().lowercase(),
                characteristics = service.characteristics.associate { characteristic ->
                    characteristic.uuid.toString().lowercase() to
                        propertyNames(characteristic.properties)
                },
            )
        }
    }

    /** Property flags rendered the way the probe report logs them. */
    private fun propertyNames(properties: Int): List<String> = buildList {
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("read")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("write")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            add("writeNoRsp")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("notify")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("indicate")
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectAndDiscover(
        requestMtu: Boolean = true,
    ): Pair<List<BluetoothGattService>, Int> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) {
            throw GarminGattClientException("Bluetooth is unavailable")
        }
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            ?: throw GarminGattClientException("Invalid device address")

        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        val connectResult = withTimeoutOrNull(CONNECT_TIMEOUT) {
            runCatching { connectedSignal.await() }
        }
        if (connectResult == null || connectResult.isFailure) {
            close()
            throw GarminGattClientException("Could not connect")
        }
        val currentGatt = gatt ?: throw GarminGattClientException("Not connected")

        // A refused MTU request just means smaller writes.
        var mtu = 23
        if (requestMtu) {
            val requested = runCatching { currentGatt.requestMtu(DESIRED_MTU) }
                .getOrDefault(false)
            if (requested) {
                mtu = withTimeoutOrNull(MTU_TIMEOUT) { mtuSignal.await() } ?: 23
            } else {
                log("[GARMIN-BLE] MTU request failed, using default")
            }
        }

        if (!currentGatt.discoverServices()) {
            close()
            throw GarminGattClientException("Could not start service discovery")
        }
        val services = withTimeoutOrNull(DISCOVER_TIMEOUT) {
            runCatching { servicesSignal.await() }.getOrNull()
        }
        if (services == null) {
            close()
            throw GarminGattClientException("Service discovery failed")
        }
        return services to mtu
    }

    /** The first receive/send pair in the handle window, as `CommunicatorV2` does. */
    private fun findMlPair(
        services: List<BluetoothGattService>,
    ): Pair<BluetoothGattCharacteristic, BluetoothGattCharacteristic>? {
        val byUuid = mutableMapOf<String, BluetoothGattCharacteristic>()
        for (service in services) {
            for (characteristic in service.characteristics) {
                byUuid[characteristic.uuid.toString().lowercase()] = characteristic
            }
        }
        for (handle in GarminUuids.ML_FIRST_RECEIVE_HANDLE..GarminUuids.ML_LAST_RECEIVE_HANDLE) {
            val receive = byUuid[GarminUuids.uuidForHandle(handle)]
            val send = byUuid[GarminUuids.uuidForHandle(handle + GarminUuids.ML_SEND_HANDLE_OFFSET)]
            if (receive != null && send != null) return receive to send
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun subscribe(characteristic: BluetoothGattCharacteristic) {
        val currentGatt = gatt ?: throw GarminGattClientException("Not connected")
        if (!currentGatt.setCharacteristicNotification(characteristic, true)) {
            throw GarminGattClientException("setCharacteristicNotification refused")
        }
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            ?: throw GarminGattClientException("No CCCD on the receive characteristic")
        val waiter = CompletableDeferred<Unit>()
        descriptorWritten = waiter
        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt.writeDescriptor(
                descriptor,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                currentGatt.writeDescriptor(descriptor)
            }
        }
        if (!started) throw GarminGattClientException("Descriptor write refused")
        val outcome = withTimeoutOrNull(SUBSCRIBE_TIMEOUT) { waiter.await() }
        descriptorWritten = null
        if (outcome == null) throw GarminGattClientException("Descriptor write timed out")
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeToCharacteristic(packet: ByteArray) {
        val currentGatt = gatt ?: throw GarminGattClientException("Not connected")
        val characteristic = sendCharacteristic
            ?: throw GarminGattClientException("Not connected")
        // Write-without-response: ML and GFDI carry their own framing and acks,
        // and confirmations would halve throughput. onCharacteristicWrite paces writes.
        writeMutex.withLock {
            val completion = CompletableDeferred<Unit>()
            writeCompleted = completion
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentGatt.writeCharacteristic(
                    characteristic,
                    packet,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    characteristic.writeType =
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    characteristic.value = packet
                    currentGatt.writeCharacteristic(characteristic)
                }
            }
            if (!started) {
                writeCompleted = null
                throw GarminGattClientException("Characteristic write failed")
            }
            // Paced, not required: some stacks coalesce no-response writes.
            withTimeoutOrNull(WRITE_TIMEOUT) { runCatching { completion.await() } }
            writeCompleted = null
        }
    }

    /** Closes the link and releases everything. Idempotent. */
    @SuppressLint("MissingPermission")
    fun close() {
        closed = true
        healScope.cancel()
        ml?.close()
        ml = null
        sendCharacteristic = null
        receiveUuid = null
        gatt?.let { currentGatt ->
            runCatching { currentGatt.disconnect() }
            runCatching { currentGatt.close() }
        }
        gatt = null
    }
}
