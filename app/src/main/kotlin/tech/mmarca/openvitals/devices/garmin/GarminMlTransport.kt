package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Garmin's multi-link (ML) transport, the V2 layer that carries GFDI. Port of
 * Gadgetbridge's `CommunicatorV2` (AGPLv3). V1 is not implemented.
 *
 * Every packet's first byte is a handle: 0 is the control channel, others belong to a
 * service opened earlier. Fed a [write] callback and bytes through [handleInbound].
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
    private val onLog: ((String) -> Unit)? = null,
) {

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

        /** Marks a reliable (MLR) packet. This transport registers non-reliable. */
        const val MLR_FLAG_MASK = 0x80
    }

    /** BLE minimum MTU of 23 minus 3 bytes ATT overhead. Raised by [onMtuChanged]. */
    private var maxWriteSize = 20

    /** The handle the watch assigned to GFDI, or null before registration. */
    private var gfdiHandle: Int? = null

    /** Handles for the other open services, both ways. */
    private val handleByService = mutableMapOf<Int, Int>()
    private val serviceByHandle = mutableMapOf<Int, Int>()

    private val decoder = GarminCobsDecoder()
    private val readyDeferred = CompletableDeferred<Unit>()

    /** Resolves once the GFDI service has a handle and frames can be sent. */
    val ready: Deferred<Unit> get() = readyDeferred

    val isReady: Boolean get() = gfdiHandle != null

    /** Applies a negotiated MTU: clamp to 23..512, minus the 3-byte ATT header. */
    fun onMtuChanged(mtu: Int) {
        val safeMtu = if (mtu < 23) 23 else mtu
        val chunk = safeMtu - 3
        maxWriteSize = if (chunk > 512) 512 else chunk
        onLog?.invoke("[GARMIN-ML] mtu=$mtu maxWrite=$maxWriteSize")
    }

    /** Opens the GFDI channel: clear stale handles, request one. Completes [ready]. */
    suspend fun open() {
        // A watch mid-session still holds old handles; registering on top fails.
        write(controlPacket(CLOSE_ALL_REQ, serviceCode = 0))
        write(
            controlPacket(
                REGISTER_ML_REQ,
                serviceCode = GFDI_SERVICE_CODE,
                trailing = 0, // 0 = plain ML; 2 would request the reliable (MLR) variant.
            ),
        )
    }

    /** Whether [serviceCode] currently has a handle. */
    fun isServiceOpen(serviceCode: Int): Boolean = handleByService.containsKey(serviceCode)

    /** Opens a non-GFDI service. Idempotent: a second handle would double every reading. */
    suspend fun openService(serviceCode: Int) {
        if (handleByService.containsKey(serviceCode)) return
        write(controlPacket(REGISTER_ML_REQ, serviceCode = serviceCode, trailing = 0))
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
    suspend fun sendFrame(frame: ByteArray) {
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
            // Reliable-mode traffic we never registered for. Fall through rather
            // than drop, as Gadgetbridge does (#5476).
            onLog?.invoke(
                "[GARMIN-ML] MLR-flagged packet, handle byte 0x${leadingByte.toString(16)}",
            )
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
            onServiceData?.invoke(serviceCode, packet.copyOfRange(1, packet.size))
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
        if (requestType != REGISTER_ML_RESP) return
        if (reader.remaining < 4) return

        val serviceCode = reader.readShort()
        val status = reader.readByte()
        val handle = reader.readByte()

        if (serviceCode != GFDI_SERVICE_CODE) {
            if (status != 0) {
                onLog?.invoke("[GARMIN-ML] service $serviceCode refused, status=$status")
                return
            }
            handleByService[serviceCode] = handle
            serviceByHandle[handle] = serviceCode
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
        write(
            controlPacket(
                REGISTER_ML_REQ,
                serviceCode = GFDI_SERVICE_CODE,
                trailing = 0,
            ),
        )
    }

    /** Clears local state. The watch releases the handle when the link goes. */
    fun close() {
        gfdiHandle = null
        handleByService.clear()
        serviceByHandle.clear()
        readyDeferred.completeExceptionally(
            IllegalStateException("ML transport closed before it opened"),
        )
    }
}
