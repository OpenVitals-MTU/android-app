package tech.mmarca.openvitals.devices.core

/**
 * A scan filter on service data, free of the framework: the bytes to find,
 * under the bits that matter. The radio applies the same test in hardware.
 */
class ServiceDataFilter(val data: ByteArray, val mask: ByteArray) {

    /** Whether [serviceData] would pass this filter. */
    fun matches(serviceData: ByteArray): Boolean =
        serviceData.size >= data.size &&
            data.indices.all { (serviceData[it].toInt() and mask[it].toInt()) == (data[it].toInt() and mask[it].toInt()) }
}
