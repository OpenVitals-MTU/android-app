package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.cancel
import kotlin.time.Duration.Companion.seconds

/**
 * Garmin's multi-link (ML) transport, the V2 layer that carries GFDI. Port of
 * Gadgetbridge's `CommunicatorV2` (AGPLv3). V1 is not implemented.
 *
 * Port of Gadgetbridge's `CommunicatorV2` (AGPLv3), narrowed to the one
 * channel a read-only sync needs. **This is the layer a vívoactive 5
 * requires**: the on-device GATT probe found the multi-link service
 * `6a4e2800` with handle pairs `0x2810/0x2820`…, and no V1 service. Older
 * direct-GFDI devices use [GarminV1Transport] instead.
 *
 * The protocol multiplexes several logical services over one characteristic
 * pair. Every packet's first byte is a handle:
 *   * handle 0 is the control channel (open/close services),
 *   * any other handle belongs to a service opened earlier.
 *
 * So the flow is: close everything stale, ask for a handle for the GFDI
 * service, then prefix every GFDI write with the handle we were given and
 * route inbound packets by their leading handle byte.
 *
 * Transport-agnostic by construction: it is handed a [write] callback and fed
 * bytes through [handleInbound], so the whole handshake is testable with no
 * Bluetooth.
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
    /**
     * Sends control traffic that is triggered by an inbound control response.
     *
     * In particular, Garmin requires registration to happen only after its
     * CLOSE_ALL response. [handleInbound] is deliberately synchronous, so the
     * Android owner supplies this small bridge into its serialised GATT writer.
     */
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
         * Gadgetbridge's value, the one known to be accepted. The two apps
         * cannot hold ML sessions with one watch at once.
         */
        const val CLIENT_ID = 2L

        /** Control request/response codes (`CommunicatorV2.RequestType` ordinals). */
        const val REGISTER_ML_REQ = 0
        const val REGISTER_ML_RESP = 1
        const val CLOSE_HANDLE_REQ = 2
        const val CLOSE_HANDLE_RESP = 3
        const val CLOSE_ALL_REQ = 5
        const val CLOSE_ALL_RESP = 6

        /** Marks a reliable (MLR) packet. This transport registers non-reliable. */
        const val MLR_FLAG_MASK = 0x80
        val SERVICE_OPEN_TIMEOUT = 10.seconds
    }

    /** BLE minimum MTU of 23 minus 3 bytes ATT overhead. Raised by [onMtuChanged]. */
    private var maxWriteSize = 20
    private val mlrScope = CoroutineScope(SupervisorJob())

    /** The handle the watch assigned to GFDI, or null before registration. */
    private var gfdiHandle: Int? = null

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
    fun onMtuChanged(mtu: Int) {
        val safeMtu = if (mtu < 23) 23 else mtu
        val chunk = safeMtu - 3
        maxWriteSize = if (chunk > 512) 512 else chunk
        mlrByHandle.values.forEach { it.setMaxPacketSize(maxWriteSize) }
        onLog?.invoke("[GARMIN-ML] mtu=$mtu maxWrite=$maxWriteSize")
    }

    /** Opens the GFDI channel: clear stale handles, request one. Completes [ready]. */
    suspend fun open() {
        // A watch mid-session still holds old handles; registering on top fails.
        write(controlPacket(CLOSE_ALL_REQ, serviceCode = 0))
        // Do not register yet. Gadgetbridge and Garmin firmware both treat
        // CLOSE_ALL as a request/response barrier: registering before the
        // CLOSE_ALL_RESP races stale-handle cleanup and stricter watches drop
        // or refuse the registration.
    }

    /** Whether [serviceCode] currently has a handle. */
    fun isServiceOpen(serviceCode: Int): Boolean = handleByService.containsKey(serviceCode)

    /**
     * Opens a non-GFDI service, so the watch starts streaming it. Idempotent:
     * a second request for an open service would earn a second handle and
     * double every reading.
     */
    suspend fun openService(serviceCode: Int, reliable: Boolean = false) {
        if (handleByService.containsKey(serviceCode)) return
        val opened = CompletableDeferred<Unit>()
        serviceOpened[serviceCode] = opened
        if (reliable) reliableRequested += serviceCode
        write(controlPacket(REGISTER_ML_REQ, serviceCode = serviceCode, trailing = if (reliable) 2 else 0))
        try {
            withTimeout(SERVICE_OPEN_TIMEOUT) { opened.await() }
        } finally {
            serviceOpened.remove(serviceCode)
            reliableRequested.remove(serviceCode)
        }
    }

    fun setServiceHandler(
        serviceCode: Int,
        onData: (ByteArray) -> Unit,
        onClosed: () -> Unit,
    ) {
        serviceDataHandlers[serviceCode] = onData
        serviceClosedHandlers[serviceCode] = onClosed
    }

    fun clearServiceHandler(serviceCode: Int) {
        serviceDataHandlers.remove(serviceCode)
        serviceClosedHandlers.remove(serviceCode)
    }

    suspend fun sendServiceData(serviceCode: Int, payload: ByteArray) {
        val handle = handleByService[serviceCode]
            ?: throw IllegalStateException("Garmin ML service $serviceCode is not open")
        mlrByHandle[handle]?.let { reliable ->
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
        val handle = handleByService[serviceCode] ?: return
        write(
            controlPacket(
                CLOSE_HANDLE_REQ,
                serviceCode = serviceCode,
                trailing = handle,
            ),
        )
        // Dropped locally at once, so a reading racing the close is not routed.
        handleByService.remove(serviceCode)
        serviceByHandle.remove(handle)
        mlrByHandle.remove(handle)?.close()
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

    /**
     * Sends one GFDI frame: COBS-wrap it, then split into handle-prefixed
     * writes that each fit a single characteristic write.
     */
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
            val channel = mlrByHandle.entries.firstOrNull {
                (it.key and 0x07) == encodedHandle
            }?.value
            if (channel != null) {
                channel.handlePacket(packet)
                return
            }
            // Some non-MLR handles legitimately use the high bit; fall through
            // when no reliable channel claims it, matching Gadgetbridge #5476.
        }

        if (leadingByte == CONTROL_HANDLE) {
            handleControl(packet.copyOfRange(1, packet.size))
            return
        }

        if (leadingByte != gfdiHandle) {
            val serviceCode = serviceByHandle[leadingByte]
            if (serviceCode == null) {
                onLog?.invoke("[GARMIN-ML] packet for unknown handle $leadingByte")
                return
            }
            val payload = packet.copyOfRange(1, packet.size)
            serviceDataHandlers[serviceCode]?.invoke(payload)
                ?: onServiceData?.invoke(serviceCode, payload)
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
            handleByService.clear()
            serviceByHandle.clear()
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
                reliableRequested.remove(serviceCode)
                serviceOpened.remove(serviceCode)?.completeExceptionally(
                    IllegalStateException(
                        "Watch refused Garmin ML service $serviceCode (status $status)",
                    ),
                )
                return
            }
            handleByService[serviceCode] = handle
            serviceByHandle[handle] = serviceCode
            if (reliable) {
                mlrByHandle[handle] = GarminMlrChannel(
                    handle = handle,
                    maxPacketSize = maxWriteSize,
                    scope = mlrScope,
                    write = write,
                    onData = { payload ->
                        serviceDataHandlers[serviceCode]?.invoke(payload)
                            ?: onServiceData?.invoke(serviceCode, payload)
                    },
                    onLog = onLog,
                )
            } else if (serviceCode in reliableRequested) {
                onLog?.invoke("[GARMIN-ML] service $serviceCode declined reliable mode")
            }
            reliableRequested.remove(serviceCode)
            serviceOpened.remove(serviceCode)?.complete(Unit)
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
            handleByService.remove(serviceCode)
            serviceByHandle.remove(handle)
            mlrByHandle.remove(handle)?.close()
            serviceClosedHandlers[serviceCode]?.invoke()
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

    private fun registerGfdiPacket(): ByteArray = controlPacket(
        REGISTER_ML_REQ,
        serviceCode = GFDI_SERVICE_CODE,
        trailing = 0,
    )

    /**
     * Drops the channel. The watch releases the handle itself when the link
     * goes, so this only clears local state.
     */
    fun close() {
        gfdiHandle = null
        handleByService.clear()
        serviceByHandle.clear()
        serviceOpened.values.forEach {
            it.completeExceptionally(IllegalStateException("ML transport closed"))
        }
        serviceOpened.clear()
        mlrByHandle.values.forEach { it.close() }
        mlrByHandle.clear()
        reliableRequested.clear()
        mlrScope.cancel()
        serviceClosedHandlers.values.toList().forEach { it.invoke() }
        serviceDataHandlers.clear()
        serviceClosedHandlers.clear()
        readyDeferred.completeExceptionally(
            IllegalStateException("ML transport closed before it opened"),
        )
    }

}
