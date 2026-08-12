package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Garmin's multi-link (ML) transport — the V2 layer that carries GFDI.
 *
 * Port of Gadgetbridge's `CommunicatorV2` (AGPLv3), narrowed to the one
 * channel a read-only sync needs. **This is the layer a vívoactive 5
 * requires**: the on-device GATT probe found the multi-link service
 * `6a4e2800` with handle pairs `0x2810/0x2820`…, and no V1 service. V1
 * watches would need `CommunicatorV1` instead (a single characteristic pair,
 * no handles) — not implemented, because the device we can test says V2.
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
     * Called when the WATCH closes the GFDI handle mid-session — seen in the
     * wild on long-held links (Gadgetbridge's `CommunicatorV2` re-registers on
     * it). The handle is already cleared; the owner decides whether to
     * [reopenGfdi]. Without that, a held link goes silently deaf: outbound
     * writes target a dead handle and inbound drops as unknown-handle.
     */
    private val onGfdiClosed: (() -> Unit)? = null,
    /**
     * A packet on a NON-GFDI service, by service code — how the watch streams
     * live readings (heart rate, steps, …) once such a service is opened.
     * Raw and unframed: these carry fixed little payloads, not COBS/GFDI.
     */
    private val onServiceData: ((serviceCode: Int, payload: ByteArray) -> Unit)? = null,
    private val onLog: ((String) -> Unit)? = null,
) {

    private companion object {
        /** The GFDI service's code in the ML service table (`Service.GFDI`). */
        const val GFDI_SERVICE_CODE = 1

        /** The control channel. Registration requests and their responses ride here. */
        const val CONTROL_HANDLE = 0

        /**
         * Identifies this client to the watch, which echoes it back on every
         * control response so several apps can multiplex without confusing
         * each other.
         *
         * Deliberately Gadgetbridge's value: it is the one empirically known
         * to be accepted by a real watch, and an ID the firmware rejects
         * would fail the registration outright. The cost is that OpenVitals
         * and Gadgetbridge cannot hold ML sessions with the same watch
         * simultaneously — they would each act on the other's control
         * responses. Two apps syncing one watch at once is already broken
         * territory, so proven beats theoretically-tidy here.
         */
        const val CLIENT_ID = 2L

        /** Control request/response codes (`CommunicatorV2.RequestType` ordinals). */
        const val REGISTER_ML_REQ = 0
        const val REGISTER_ML_RESP = 1
        const val CLOSE_HANDLE_REQ = 2
        const val CLOSE_HANDLE_RESP = 3
        const val CLOSE_ALL_REQ = 5

        /**
         * Marks an inbound packet as belonging to the reliable (MLR)
         * sub-protocol. This transport registers non-reliable, so these are
         * not expected.
         */
        const val MLR_FLAG_MASK = 0x80
    }

    /**
     * Conservative default: the BLE minimum MTU of 23 minus 3 bytes of ATT
     * overhead. Raised by [onMtuChanged] once the real MTU is negotiated.
     */
    private var maxWriteSize = 20

    /** The handle the watch assigned to GFDI, or null before registration. */
    private var gfdiHandle: Int? = null

    /**
     * Handles for the other services this transport has opened, both ways:
     * the watch addresses packets by handle, and closing one needs its code.
     */
    private val handleByService = mutableMapOf<Int, Int>()
    private val serviceByHandle = mutableMapOf<Int, Int>()

    private val decoder = GarminCobsDecoder()
    private val readyDeferred = CompletableDeferred<Unit>()

    /** Resolves once the GFDI service has a handle and frames can be sent. */
    val ready: Deferred<Unit> get() = readyDeferred

    val isReady: Boolean get() = gfdiHandle != null

    /**
     * Applies a negotiated MTU. Same formula as Gadgetbridge's
     * `calcMaxWriteChunk`: clamp to the spec's 23-byte floor and 512-byte
     * ceiling, minus the 3-byte ATT write header.
     */
    fun onMtuChanged(mtu: Int) {
        val safeMtu = if (mtu < 23) 23 else mtu
        val chunk = safeMtu - 3
        maxWriteSize = if (chunk > 512) 512 else chunk
        onLog?.invoke("[GARMIN-ML] mtu=$mtu maxWrite=$maxWriteSize")
    }

    /**
     * Opens the GFDI channel: clear any handles left by a previous session,
     * then request one for GFDI. Completes [ready] when the watch answers.
     */
    suspend fun open() {
        // A watch that was mid-session (app killed, link dropped) still holds
        // the old handles; registering on top of them fails until they are
        // released.
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

    /**
     * Opens a non-GFDI service, so the watch starts streaming it. Idempotent:
     * a second request for an open service would earn a second handle and
     * double every reading.
     */
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
        // Dropped locally at once rather than on the response: the watch stops
        // either way, and a reading that races the close must not be routed to
        // a service the app already considers shut.
        handleByService.remove(serviceCode)
        serviceByHandle.remove(handle)
    }

    /**
     * A 13-byte control packet on handle 0:
     * `[handle 0][request][u64 clientId][u16 serviceCode][trailing]`.
     */
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
    suspend fun sendFrame(frame: ByteArray) {
        val handle = gfdiHandle
            ?: throw IllegalStateException("GFDI channel not open — call open() and await ready")
        val payload = GarminCobs.encode(frame)
        // One byte of every write is the handle, so the usable payload is one less.
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
            // Reliable-mode traffic. We never registered for it, and the
            // leading byte of a non-MLR handle can legitimately have the high
            // bit set, so fall through rather than dropping — matching
            // Gadgetbridge (see its #5476).
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
                // A corrupt frame is survivable: drop it and keep the stream
                // running, rather than tearing down a sync over one bad packet.
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
     * A close-handle response — which the watch also sends UNREQUESTED when it
     * decides to shut a service down. Field order differs from registration:
     * `[u16 serviceCode][handle][status]`.
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
        // Not the open channel: a stale handle from a previous session being
        // torn down (our own CLOSE_ALL on open provokes exactly these).
        if (handle != gfdiHandle) return

        onLog?.invoke("[GARMIN-ML] watch closed GFDI handle $handle (status=$status)")
        gfdiHandle = null
        onGfdiClosed?.invoke()
    }

    /**
     * Requests a fresh GFDI handle after the watch closed ours. Registration
     * only — no CLOSE_ALL first, which would tear down whatever else the
     * watch is running and is only right on a cold open.
     */
    suspend fun reopenGfdi() {
        write(
            controlPacket(
                REGISTER_ML_REQ,
                serviceCode = GFDI_SERVICE_CODE,
                trailing = 0,
            ),
        )
    }

    /**
     * Drops the channel. The watch releases the handle itself when the link
     * goes, so this only clears local state.
     */
    fun close() {
        gfdiHandle = null
        handleByService.clear()
        serviceByHandle.clear()
        readyDeferred.completeExceptionally(
            IllegalStateException("ML transport closed before it opened"),
        )
    }
}
