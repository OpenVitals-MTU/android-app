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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Thrown when the watch cannot be reached or does not expose the V2 transport. */
class GarminGattClientException(message: String) : Exception(message)

/**
 * The one file in the Garmin stack that touches `android.bluetooth`.
 *
 * Everything above it — [GarminMlTransport], the session, the messages, the
 * framing — moves bytes and is tested with no radio. This connects, finds the
 * multi-link characteristic pair, and wires the two together:
 *
 *   notify characteristic → [GarminMlTransport.handleInbound]
 *   [GarminMlTransport]'s write callback → send characteristic
 *
 * Port of the Flutter build's `garmin_ble_transport.dart`, written against
 * `BluetoothGatt` directly. Modelled on `BleGattConnection`'s connect/notify
 * idioms but deliberately a separate class: that one is capability/aggregator
 * shaped with a reconnect loop, and a sync is a bounded operation the user
 * started, so a dropped link should end it and report, not silently redial.
 *
 * One instance drives ONE connection attempt — the await-able steps inside
 * are single-shot. Make a new instance to reconnect.
 */
class GarminGattClient(
    private val context: Context,
    private val address: String,
    private val onLog: ((String) -> Unit)? = null,
) {

    private companion object {
        /**
         * Gadgetbridge asks for 515; Android negotiates down as needed. A
         * bigger MTU is the single largest factor in sync speed, since every
         * GFDI frame is chunked to fit one write.
         */
        const val DESIRED_MTU = 515

        /**
         * Long, because a probe's connect can happen right after bonding,
         * when the watch may still be settling its encrypted link.
         */
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
     * Connects, opens the GFDI channel and returns the transport to send on.
     *
     * Throws [GarminGattClientException] when the watch is unreachable or
     * exposes no V2 characteristic pair — the latter meaning it is a V1
     * device, which this app does not implement (see [GarminMlTransport]).
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(onFrame: (GarminGfdiFrame) -> Unit): GarminMlTransport {
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
            onLog = ::log,
        )
        transport.onMtuChanged(mtu)
        ml = transport

        // Subscribe BEFORE opening the channel: the watch's registration
        // response can arrive the instant the request lands, and a late
        // subscription would miss it and hang the handshake.
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

    /**
     * Connects, enumerates the GATT table and hangs up — the probe path. No
     * GFDI traffic, no writes.
     */
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

        // Best-effort: a refused MTU request just means smaller writes.
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

    /**
     * The first receive/send pair present on the device, scanning the handle
     * window in order — the same first-match rule as
     * `CommunicatorV2.initializeDevice`.
     */
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
        // Write-without-response throughout: the ML layer carries its own
        // framing and the GFDI layer its own acks, so per-write confirmations
        // would only halve throughput on a link that already has to move
        // whole FIT files. The stack still reports buffer availability via
        // onCharacteristicWrite, which is what paces the next write.
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
            // Paced, not required: some stacks coalesce no-response writes and
            // a missing callback must not stall the whole transfer.
            withTimeoutOrNull(WRITE_TIMEOUT) { runCatching { completion.await() } }
            writeCompleted = null
        }
    }

    /** Closes the link and releases everything. Idempotent. */
    @SuppressLint("MissingPermission")
    fun close() {
        closed = true
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
