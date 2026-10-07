package tech.mmarca.openvitals.devices.xiaomi

import tech.mmarca.openvitals.devices.core.ServiceDataFilter
import tech.mmarca.openvitals.domain.model.ScaleReading

/** One decoded broadcast of the scale: whose weigh-in it belongs to, and what it adds. */
data class S400Frame(
    val profile: Int,
    val scaleTimestamp: Long,
    val reading: ScaleReading,
)

/** What a `0xFE95` service-data advertisement turned out to be. */
sealed interface S400BeaconResult {

    /** A measurement the bind key authenticated. */
    data class Frame(val frame: S400Frame) : S400BeaconResult

    /** Not this scale: another product, a mesh or legacy frame, or bytes that do not parse. */
    data object NotS400 : S400BeaconResult

    /** The scale, with nothing to trust: an idle beacon, or an object nobody signed. */
    data object NoReading : S400BeaconResult

    /** A measurement frame of the scale that the bind key does not open. */
    data object BadTag : S400BeaconResult
}

/**
 * The Xiaomi Body Composition Scale S400 on the air. The scale never takes a
 * connection: it broadcasts each weigh-in as MiBeacon v5 frames in the
 * service data of [SERVICE_UUID], sealed with AES-CCM under the bind key the
 * Xiaomi account issued when the scale was paired.
 *
 * Frame: control (2, little-endian), product id (2), counter (1), then MAC
 * and capability when their control bits say so, the sealed objects, a
 * 3-byte extended counter and a 4-byte tag.
 */
object S400Beacon {

    /** The service the frames are advertised under, as a 128-bit UUID. */
    const val SERVICE_UUID = "0000fe95-0000-1000-8000-00805f9b34fb"

    /** MJTZC01YM in its three hardware revisions, and MJTZC03YM, which sends the same object. */
    val PRODUCT_IDS: Set<Int> = setOf(0x3BD5, 0x30D9, 0x48CF, 0x4B05)

    private const val CONTROL_ENCRYPTED = 0x0008
    private const val CONTROL_HAS_MAC = 0x0010
    private const val CONTROL_HAS_CAPABILITY = 0x0020
    private const val CONTROL_HAS_OBJECT = 0x0040
    private const val CONTROL_MESH = 0x0080
    private const val CAPABILITY_HAS_IO = 0x20
    private const val MIN_VERSION = 4

    private const val HEADER_SIZE = 5
    private const val MAC_SIZE = 6
    private const val EXTENDED_COUNTER_SIZE = 3
    private const val TAG_SIZE = 4
    private const val OBJECT_HEADER_SIZE = 3

    private const val OBJECT_WEIGH_IN = 0x6E16
    private const val OBJECT_WEIGH_IN_SIZE = 9

    private val AssociatedData = byteArrayOf(0x11)

    /**
     * The service-data prefix a measurement frame of each product starts
     * with, and the mask that picks it out: an encrypted object, from that
     * product. The scale's idle beacons fail it, so they wake nobody.
     */
    val scanFilters: List<ServiceDataFilter> = PRODUCT_IDS.map { productId ->
        val measurement = CONTROL_ENCRYPTED or CONTROL_HAS_OBJECT
        ServiceDataFilter(
            data = byteArrayOf(measurement.toByte(), 0, productId.toByte(), (productId ushr 8).toByte()),
            mask = byteArrayOf(measurement.toByte(), 0, 0xFF.toByte(), 0xFF.toByte()),
        )
    }

    /** Any frame of each product, idle beacons included: what finds a scale that is merely awake. */
    val discoveryFilters: List<ServiceDataFilter> = PRODUCT_IDS.map { productId ->
        ServiceDataFilter(
            data = byteArrayOf(0, 0, productId.toByte(), (productId ushr 8).toByte()),
            mask = byteArrayOf(0, 0, 0xFF.toByte(), 0xFF.toByte()),
        )
    }

    /** [serviceData] is the value under [SERVICE_UUID]; [address] is the advertiser's, which seeds the nonce. */
    fun decode(serviceData: ByteArray, address: String, bindKey: ByteArray): S400BeaconResult {
        if (serviceData.size < HEADER_SIZE) return S400BeaconResult.NotS400
        val control = serviceData.u16(0)
        if (control ushr 12 < MIN_VERSION || control and CONTROL_MESH != 0) return S400BeaconResult.NotS400
        if (serviceData.u16(2) !in PRODUCT_IDS) return S400BeaconResult.NotS400
        val reversedMac = reversedMac(address) ?: return S400BeaconResult.NotS400

        var offset = HEADER_SIZE
        if (control and CONTROL_HAS_MAC != 0) {
            if (serviceData.size < offset + MAC_SIZE) return S400BeaconResult.NotS400
            // A frame relayed under another address would not decrypt anyway.
            val embedded = serviceData.copyOfRange(offset, offset + MAC_SIZE)
            if (!embedded.contentEquals(reversedMac)) return S400BeaconResult.NotS400
            offset += MAC_SIZE
        }
        if (control and CONTROL_HAS_CAPABILITY != 0) {
            if (serviceData.size <= offset) return S400BeaconResult.NotS400
            val capability = serviceData[offset].toInt()
            offset += if (capability and CAPABILITY_HAS_IO != 0) 2 else 1
        }
        // An unsealed object is anyone's to forge: only what the key authenticates counts.
        if (control and CONTROL_HAS_OBJECT == 0 || control and CONTROL_ENCRYPTED == 0) {
            return S400BeaconResult.NoReading
        }

        val sealedEnd = serviceData.size - EXTENDED_COUNTER_SIZE - TAG_SIZE
        if (sealedEnd < offset + OBJECT_HEADER_SIZE) return S400BeaconResult.NotS400
        val nonce = reversedMac +
            serviceData.copyOfRange(2, HEADER_SIZE) +
            serviceData.copyOfRange(sealedEnd, sealedEnd + EXTENDED_COUNTER_SIZE)
        val objects = AesCcm.decrypt(
            key = bindKey,
            nonce = nonce,
            associatedData = AssociatedData,
            ciphertext = serviceData.copyOfRange(offset, sealedEnd),
            tag = serviceData.copyOfRange(serviceData.size - TAG_SIZE, serviceData.size),
        ) ?: return S400BeaconResult.BadTag

        return weighInObject(objects)?.let { S400BeaconResult.Frame(it) } ?: S400BeaconResult.NoReading
    }

    /** Walks the `id, length, value` list for the weigh-in object. */
    private fun weighInObject(objects: ByteArray): S400Frame? {
        var offset = 0
        while (offset + OBJECT_HEADER_SIZE <= objects.size) {
            val id = objects.u16(offset)
            val size = objects[offset + 2].toInt() and 0xFF
            val value = offset + OBJECT_HEADER_SIZE
            if (value + size > objects.size) return null
            if (id == OBJECT_WEIGH_IN && size == OBJECT_WEIGH_IN_SIZE) return weighIn(objects, value)
            offset = value + size
        }
        return null
    }

    /**
     * `profile (1), packed (4), timestamp (4)`. The packed word holds the
     * weight in its low 11 bits (0.1 kg), the heart rate in the next 7 (less
     * 50) and the impedance in the top 14 (0.1 ohm). The frame with a weight
     * carries the 50 kHz impedance, the one without the 250 kHz.
     */
    private fun weighIn(objects: ByteArray, at: Int): S400Frame {
        val packed = objects.u32(at + 1)
        val weight = (packed and 0x7FF).toInt()
        val heartRate = ((packed ushr 11) and 0x7F).toInt()
        val impedance = (packed ushr 18).toInt().takeIf { it != 0 }?.let { it / 10.0 }
        val reading = if (weight != 0) {
            ScaleReading(
                weightKg = weight / 10.0,
                // 0 is "not yet", 127 is "not measured".
                heartRateBpm = heartRate.takeIf { it in 1..126 }?.let { it + 50 },
                impedanceLowOhm = impedance,
            )
        } else {
            ScaleReading(impedanceHighOhm = impedance)
        }
        return S400Frame(
            profile = objects[at].toInt() and 0xFF,
            scaleTimestamp = objects.u32(at + 5),
            reading = reading,
        )
    }

    /** The six address bytes, last first, as the frame and the nonce carry them. */
    private fun reversedMac(address: String): ByteArray? {
        val parts = address.split(':')
        if (parts.size != MAC_SIZE) return null
        val reversed = ByteArray(MAC_SIZE)
        for (index in 0 until MAC_SIZE) {
            val octet = parts[MAC_SIZE - 1 - index].toIntOrNull(16)?.takeIf { it in 0..0xFF } ?: return null
            reversed[index] = octet.toByte()
        }
        return reversed
    }

    private fun ByteArray.u16(at: Int): Int =
        (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(at: Int): Long =
        u16(at).toLong() or (u16(at + 2).toLong() shl 16)
}
