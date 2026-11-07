package tech.mmarca.openvitals.devices.garmin

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Garmin's multi-link (ML) transport, the V2 layer that carries GFDI and
 * multiplexes service channels over one characteristic pair. Watches with
 * no V2 pair use [GarminV1Transport].
 *
 * Every packet's first byte is a handle: 0 is the control channel, any other
 * belongs to a service opened earlier. Startup closes stale handles, asks for
 * a GFDI handle, then prefixes every GFDI write with it and routes inbound
 * packets by their leading byte.
 *
 * Transport-free: it is handed a [write] callback and fed bytes through
 * [handleInbound], so the whole handshake is testable with no Bluetooth.
 */
class GarminMlTransport(
    /** Writes one packet to the send characteristic. */
    private val write: suspend (ByteArray) -> Unit,
    /** Called with each fully-reassembled GFDI frame. */
    private val onFrame: (GarminGfdiFrame) -> Unit,
    /**
     * Called when the watch closes the GFDI handle mid-session. The owner
     * decides whether to [reopenGfdi]; otherwise a held link goes deaf.
     */
    private val onGfdiClosed: (() -> Unit)? = null,
    /** A packet on a non-GFDI service, by service code: live readings. Raw, unframed. */
    private val onServiceData: ((serviceCode: Int, payload: ByteArray) -> Unit)? = null,
    private val onControlPacket: ((ByteArray) -> Unit)? = null,
    private val onLog: ((String) -> Unit)? = null,
) : GarminFrameTransport {

    private companion object {
        /** The GFDI service's code in the ML service table (`Service.GFDI`). */
        const val GFDI_SERVICE_CODE = 1

        /** The control channel. Registration requests and their responses ride here. */
        const val CONTROL_HANDLE = 0

        /**
         * Identifies this client; echoed on every control response.
         * The value known to be accepted.
         */
        const val CLIENT_ID = 2L

        /** Control request/response codes (`CommunicatorV2.RequestType` ordinals). */
        const val REGISTER_ML_REQ = 0
        const val REGISTER_ML_RESP = 1
        const val CLOSE_HANDLE_REQ = 2
        const val CLOSE_HANDLE_RESP = 3
        const val CLOSE_ALL_REQ = 5
        const val CLOSE_ALL_RESP = 6

        /** Marks a reliable (MLR) packet. */
        const val MLR_FLAG_MASK = 0x80
        val SERVICE_OPEN_TIMEOUT = 10.seconds
    }

    /** BLE minimum MTU of 23 minus 3 bytes ATT overhead. Raised by [onMtuChanged]. */
    private var maxWriteSize = 20
    private val mlrScope = CoroutineScope(SupervisorJob())

    /** The handle the watch assigned to GFDI, or null before registration. */
    @Volatile
    private var gfdiHandle: Int? = null

    /**
     * Guards the service tables. Inbound packets arrive on the Bluetooth
     * binder thread while callers open and close services from coroutines.
     */
    private val lock = Any()

    /** Handles for the other open services, both ways. */
    private val handleByService = mutableMapOf<Int, Int>()
    private val serviceByHandle = mutableMapOf<Int, Int>()
    private val serviceOpened = mutableMapOf<Int, CompletableDeferred<Unit>>()
    private val serviceDataHandlers = mutableMapOf<Int, (ByteArray) -> Unit>()
    private val serviceClosedHandlers = mutableMapOf<Int, () -> Unit>()
    private val mlrByHandle = mutableMapOf<Int, GarminMlrChannel>()
    private val reliableRequested = mutableSetOf<Int>()

    private val decoder = GarminCobsDecoder()
    private val gfdiSendMutex = Mutex()
    private val readyDeferred = CompletableDeferred<Unit>()

    /** Resolves once the GFDI service has a handle and frames can be sent. */
    val ready: Deferred<Unit> get() = readyDeferred

    val isReady: Boolean get() = gfdiHandle != null

    /** Applies a negotiated MTU: clamp to 23..512, minus the 3-byte ATT header. */
    override fun onMtuChanged(mtu: Int) {
        val safeMtu = if (mtu < 23) 23 else mtu
        val chunk = safeMtu - 3
        maxWriteSize = if (chunk > 512) 512 else chunk
        val channels = synchronized(lock) { mlrByHandle.values.toList() }
        channels.forEach { it.setMaxPacketSize(maxWriteSize) }
        onLog?.invoke("[GARMIN-ML] mtu=$mtu maxWrite=$maxWriteSize")
    }

    /**
     * Starts the GFDI handshake by closing stale handles. Registration is
     * sent from the CLOSE_ALL response through [onControlPacket]; [ready]
     * completes when the handle arrives.
     */
    suspend fun open() {
        // A watch mid-session still holds old handles; registering on top fails.
        write(controlPacket(CLOSE_ALL_REQ, serviceCode = 0))
    }

    /** Whether [serviceCode] currently has a handle. */
    fun isServiceOpen(serviceCode: Int): Boolean =
        synchronized(lock) { handleByService.containsKey(serviceCode) }

    /** Opens a non-GFDI service, so the watch starts streaming it. */
    suspend fun openService(serviceCode: Int, reliable: Boolean = false) {
        val opened = CompletableDeferred<Unit>()
        synchronized(lock) {
            if (handleByService.containsKey(serviceCode)) return
            serviceOpened[serviceCode] = opened
            if (reliable) reliableRequested += serviceCode
        }
        val trailing = if (reliable) 2 else 0
        write(controlPacket(REGISTER_ML_REQ, serviceCode = serviceCode, trailing = trailing))
        try {
            // A plain failure, not a cancellation: callers report it instead of vanishing.
            withTimeoutOrNull(SERVICE_OPEN_TIMEOUT) { opened.await() }
                ?: throw IllegalStateException("Watch did not open Garmin ML service $serviceCode")
        } finally {
            synchronized(lock) {
                serviceOpened.remove(serviceCode)
                reliableRequested.remove(serviceCode)
            }
        }
    }

    internal fun setServiceHandler(
        serviceCode: Int,
        onData: (ByteArray) -> Unit,
        onClosed: () -> Unit,
    ): Unit = synchronized(lock) {
        serviceDataHandlers[serviceCode] = onData
        serviceClosedHandlers[serviceCode] = onClosed
    }

    internal fun clearServiceHandler(serviceCode: Int): Unit = synchronized(lock) {
        serviceDataHandlers.remove(serviceCode)
        serviceClosedHandlers.remove(serviceCode)
    }

    internal suspend fun sendServiceData(serviceCode: Int, payload: ByteArray) {
        val (handle, reliable) = synchronized(lock) {
            val handle = handleByService[serviceCode]
                ?: throw IllegalStateException("Garmin ML service $serviceCode is not open")
            handle to mlrByHandle[handle]
        }
        if (reliable != null) {
            reliable.sendMessage(payload)
            return
        }
        val chunkSize = maxWriteSize - 1
        var offset = 0
        while (offset < payload.size) {
            val end = (offset + chunkSize).coerceAtMost(payload.size)
            write(byteArrayOf(handle.toByte()) + payload.copyOfRange(offset, end))
            offset = end
        }
    }

    /** Closes a service, so the watch stops streaming and stops spending on it. */
    suspend fun closeService(serviceCode: Int) {
        val handle = synchronized(lock) { handleByService[serviceCode] } ?: return
        write(
            controlPacket(
                CLOSE_HANDLE_REQ,
                serviceCode = serviceCode,
                trailing = handle,
            ),
        )
        // Dropped locally at once, so a reading racing the close is not routed.
        val reliable = synchronized(lock) {
            handleByService.remove(serviceCode)
            serviceByHandle.remove(handle)
            mlrByHandle.remove(handle)
        }
        reliable?.close()
    }

    /** A 13-byte control packet: `[handle 0][request][u64 clientId][u16 serviceCode][trailing]`. */
    private fun controlPacket(
        request: Int,
        serviceCode: Int,
        trailing: Int = 0,
    ): ByteArray = GarminByteWriter(13)
        .writeByte(CONTROL_HANDLE)
        .writeByte(request)
        .writeLong(CLIENT_ID)
        .writeShort(serviceCode)
        .writeByte(trailing)
        .toBytes()

    /** Sends one GFDI frame: COBS-wrap, then split into handle-prefixed writes. */
    override suspend fun sendFrame(frame: ByteArray) = gfdiSendMutex.withLock {
        val handle = gfdiHandle
            ?: throw IllegalStateException("GFDI channel not open — call open() and await ready")
        val payload = GarminCobs.encode(frame)
        // One byte of every write is the handle.
        val chunkSize = maxWriteSize - 1
        var offset = 0
        while (offset < payload.size) {
            val end = if (offset + chunkSize < payload.size) offset + chunkSize else payload.size
            val packet = GarminByteWriter(end - offset + 1)
                .writeByte(handle)
                .writeBytes(payload.copyOfRange(offset, end))
                .toBytes()
            write(packet)
            offset += chunkSize
        }
    }

    /** Feeds one packet from the receive characteristic in. */
    fun handleInbound(packet: ByteArray) {
        if (packet.isEmpty()) return
        val leadingByte = packet[0].toInt() and 0xFF

        if ((leadingByte and MLR_FLAG_MASK) != 0) {
            val encodedHandle = (leadingByte and 0x70) ushr 4
            val channel = synchronized(lock) {
                mlrByHandle.entries.firstOrNull { (it.key and 0x07) == encodedHandle }?.value
            }
            if (channel != null) {
                channel.handlePacket(packet)
                return
            }
            // Plain handles may use the high bit too: fall through when no reliable channel claims it.
        }

        if (leadingByte == CONTROL_HANDLE) {
            handleControl(packet.copyOfRange(1, packet.size))
            return
        }

        if (leadingByte != gfdiHandle) {
            val serviceCode = synchronized(lock) { serviceByHandle[leadingByte] }
            if (serviceCode == null) {
                onLog?.invoke("[GARMIN-ML] packet for unknown handle $leadingByte")
                return
            }
            deliverServiceData(serviceCode, packet.copyOfRange(1, packet.size))
            return
        }

        // GFDI payload: feed the COBS decoder and emit whatever frames complete.
        decoder.addBytes(packet.copyOfRange(1, packet.size))
        var raw = decoder.pull()
        while (raw != null) {
            try {
                onFrame(GarminGfdiFrame.parse(raw))
            } catch (error: GarminGfdiFrameException) {
                // A corrupt frame is survivable: drop it, keep the stream.
                onLog?.invoke("[GARMIN-ML] dropped bad frame: ${error.message}")
            }
            raw = decoder.pull()
        }
    }

    /** Routes a service payload to its transfer handler, else to the live-readings hook. */
    private fun deliverServiceData(serviceCode: Int, payload: ByteArray) {
        val handler = synchronized(lock) { serviceDataHandlers[serviceCode] }
        if (handler != null) handler(payload) else onServiceData?.invoke(serviceCode, payload)
    }

    private fun handleControl(body: ByteArray) {
        if (body.size < 9) return
        val reader = GarminByteReader(body)
        val requestType = reader.readByte()
        val clientId = reader.readLong()
        if (clientId != CLIENT_ID) {
            // Another app's control traffic on the same watch.
            onLog?.invoke("[GARMIN-ML] ignoring control for client $clientId")
            return
        }
        if (requestType == CLOSE_HANDLE_RESP) {
            handleCloseResponse(reader)
            return
        }
        if (requestType == CLOSE_ALL_RESP) {
            gfdiHandle = null
            synchronized(lock) {
                handleByService.clear()
                serviceByHandle.clear()
            }
            onLog?.invoke("[GARMIN-ML] all stale handles closed; registering GFDI")
            val sender = onControlPacket
            if (sender == null) {
                readyDeferred.completeExceptionally(
                    IllegalStateException("No control writer available for GFDI registration"),
                )
            } else {
                sender(registerGfdiPacket())
            }
            return
        }
        if (requestType != REGISTER_ML_RESP) return
        if (reader.remaining < 4) return

        val serviceCode = reader.readShort()
        val status = reader.readByte()
        val handle = reader.readByte()
        val reliable = if (reader.remaining > 0) reader.readByte() != 0 else false

        if (serviceCode != GFDI_SERVICE_CODE) {
            if (status != 0) {
                onLog?.invoke("[GARMIN-ML] service $serviceCode refused, status=$status")
                val waiter = synchronized(lock) {
                    reliableRequested.remove(serviceCode)
                    val opener = serviceOpened.remove(serviceCode)
                    opener
                }
                waiter?.completeExceptionally(
                    IllegalStateException(
                        "Watch refused Garmin ML service $serviceCode (status $status)",
                    ),
                )
                return
            }
            val waiter = synchronized(lock) {
                handleByService[serviceCode] = handle
                serviceByHandle[handle] = serviceCode
                if (reliable) {
                    mlrByHandle[handle] = GarminMlrChannel(
                        handle = handle,
                        maxPacketSize = maxWriteSize,
                        scope = mlrScope,
                        write = write,
                        onData = { payload -> deliverServiceData(serviceCode, payload) },
                        onLog = onLog,
                    )
                } else if (serviceCode in reliableRequested) {
                    onLog?.invoke("[GARMIN-ML] service $serviceCode declined reliable mode")
                }
                reliableRequested.remove(serviceCode)
                val opener = serviceOpened.remove(serviceCode)
                opener
            }
            waiter?.complete(Unit)
            onLog?.invoke("[GARMIN-ML] service $serviceCode open on handle $handle")
            return
        }
        if (status != 0) {
            onLog?.invoke("[GARMIN-ML] GFDI registration refused, status=$status")
            readyDeferred.completeExceptionally(
                IllegalStateException(
                    "Watch refused the GFDI service registration (status $status)",
                ),
            )
            return
        }

        gfdiHandle = handle
        onLog?.invoke("[GARMIN-ML] GFDI open on handle $handle")
        readyDeferred.complete(Unit)
    }

    /**
     * A close-handle response, also sent unrequested when the watch shuts a
     * service. Field order: `[u16 serviceCode][handle][status]`.
     */
    private fun handleCloseResponse(reader: GarminByteReader) {
        if (reader.remaining < 4) return
        val serviceCode = reader.readShort()
        val handle = reader.readByte()
        val status = reader.readByte()
        if (serviceCode != GFDI_SERVICE_CODE) {
            val (reliable, onClosed) = synchronized(lock) {
                handleByService.remove(serviceCode)
                serviceByHandle.remove(handle)
                val closing = mlrByHandle.remove(handle) to serviceClosedHandlers[serviceCode]
                closing
            }
            reliable?.close()
            onClosed?.invoke()
            return
        }
        // A stale handle from a previous session; our own CLOSE_ALL provokes these.
        if (handle != gfdiHandle) return

        onLog?.invoke("[GARMIN-ML] watch closed GFDI handle $handle (status=$status)")
        gfdiHandle = null
        onGfdiClosed?.invoke()
    }

    /** Requests a fresh GFDI handle after the watch closed ours. No CLOSE_ALL first. */
    suspend fun reopenGfdi() {
        write(registerGfdiPacket())
    }

    /** The GFDI registration write failed: fail [ready] now instead of at its timeout. */
    fun failOpen(error: Throwable) {
        readyDeferred.completeExceptionally(
            IllegalStateException("GFDI registration write failed: ${error.message}", error),
        )
    }

    private fun registerGfdiPacket(): ByteArray = controlPacket(
        REGISTER_ML_REQ,
        serviceCode = GFDI_SERVICE_CODE,
        trailing = 0,
    )

    /** Clears local state. The watch releases the handle when the link goes. */
    fun close() {
        gfdiHandle = null
        val waiters: List<CompletableDeferred<Unit>>
        val channels: List<GarminMlrChannel>
        val closedHandlers: List<() -> Unit>
        synchronized(lock) {
            handleByService.clear()
            serviceByHandle.clear()
            waiters = serviceOpened.values.toList()
            serviceOpened.clear()
            channels = mlrByHandle.values.toList()
            mlrByHandle.clear()
            reliableRequested.clear()
            closedHandlers = serviceClosedHandlers.values.toList()
            serviceDataHandlers.clear()
            serviceClosedHandlers.clear()
        }
        waiters.forEach { it.completeExceptionally(IllegalStateException("ML transport closed")) }
        channels.forEach { it.close() }
        mlrScope.cancel()
        closedHandlers.forEach { it.invoke() }
        readyDeferred.completeExceptionally(
            IllegalStateException("ML transport closed before it opened"),
        )
    }
}
