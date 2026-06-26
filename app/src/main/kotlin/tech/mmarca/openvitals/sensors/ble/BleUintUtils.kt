package tech.mmarca.openvitals.sensors.ble

internal object BleUintUtils {
    const val UINT16_MAX = 0xFFFF
    const val UINT32_MAX = 0xFFFFFFFFL

    fun diff(a: Long, b: Long, uintMax: Long): Long {
        require(a >= 0 && b >= 0) { "Values must be non-negative" }
        require(a <= uintMax && b <= uintMax) { "Values outside uint range" }
        return if (a >= b) {
            a - b
        } else {
            (uintMax + 1 - b) + a
        }
    }
}
