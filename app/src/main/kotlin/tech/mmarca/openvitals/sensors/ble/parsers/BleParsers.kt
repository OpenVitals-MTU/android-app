package tech.mmarca.openvitals.sensors.ble.parsers

import android.bluetooth.BluetoothGattCharacteristic
import tech.mmarca.openvitals.sensors.ble.BleServiceMeasurementUuid

internal data class BleCrankData(
    val crankRevolutionsCount: Long,
    val crankRevolutionsTime: Int,
)

internal data class BleWheelData(
    val wheelRevolutionsCount: Long,
    val wheelRevolutionsTime: Int,
)

internal data class BleCyclingPowerData(
    val powerWatts: Int,
    val crank: BleCrankData?,
)

internal data class BleRunningSpeedCadenceData(
    val speedMetersPerSecond: Double?,
    val cadenceRpm: Long?,
)

internal object BleHeartRateParser {
    fun parse(characteristic: BluetoothGattCharacteristic): Long? {
        val raw = characteristic.value ?: return null
        return parseBytes(raw)
    }

    fun parseBytes(raw: ByteArray): Long? {
        if (raw.isEmpty()) return null
        val formatUint16 = raw[0].toInt() and 0x1 == 1
        val parsed = when {
            formatUint16 && raw.size >= 3 -> {
                ((raw[2].toInt() and 0xFF) shl 8 or (raw[1].toInt() and 0xFF)).toLong()
            }
            raw.size >= 2 -> (raw[1].toInt() and 0xFF).toLong()
            raw.size == 1 -> (raw[0].toInt() and 0xFF).toLong()
            else -> null
        }
        return parsed?.takeIf { it in 1..299 }
    }

    fun isZeroSignal(raw: ByteArray): Boolean =
        raw.size >= 2 &&
            raw[0].toInt() and 0x1 == 0 &&
            raw[1] == 0.toByte()

    fun supports(serviceMeasurement: BleServiceMeasurementUuid): Boolean =
        serviceMeasurement.measurementUuid == tech.mmarca.openvitals.sensors.ble.BleUuids.HEART_RATE.measurementUuid
}

internal object BleCyclingPowerParser {
    fun parse(characteristic: BluetoothGattCharacteristic): BleCyclingPowerData? {
        val raw = characteristic.value ?: return null
        return parsePayload(raw)
    }

    fun parsePayload(raw: ByteArray): BleCyclingPowerData? {
        if (raw.isEmpty()) return null
        var index = 0
        val flags1 = raw[index++].toInt() and 0xFF
        index++
        val hasPedalPowerBalance = flags1 and 0x01 > 0
        val hasAccumulatedTorque = flags1 and 0x04 > 0
        val hasWheel = flags1 and 16 > 0
        val hasCrank = flags1 and 32 > 0
        val power = readInt16(raw, index) ?: return null
        index += 2
        if (hasPedalPowerBalance) index += 1
        if (hasAccumulatedTorque) index += 2
        if (hasWheel) index += 4
        val crank = if (hasCrank && raw.size - index >= 4) {
            val crankCount = readUint16(raw, index)?.toLong() ?: return null
            index += 2
            val crankTime = readUint16(raw, index) ?: return null
            BleCrankData(crankCount, crankTime)
        } else {
            null
        }
        return BleCyclingPowerData(powerWatts = power, crank = crank)
    }
}

internal object BleCyclingSpeedCadenceParser {
    fun parse(characteristic: BluetoothGattCharacteristic): Pair<BleWheelData?, BleCrankData?>? {
        val raw = characteristic.value ?: return null
        return parsePayload(raw)
    }

    fun parsePayload(raw: ByteArray): Pair<BleWheelData?, BleCrankData?>? {
        if (raw.isEmpty()) return null
        val flags = raw[0].toInt() and 0xFF
        val hasWheel = flags and 0x01 > 0
        val hasCrank = flags and 0x02 > 0
        var index = 1
        val wheel = if (hasWheel && raw.size - index >= 6) {
            val wheelCount = readUint32(raw, index)?.toLong() ?: return null
            index += 4
            val wheelTime = readUint16(raw, index) ?: return null
            index += 2
            BleWheelData(wheelCount, wheelTime)
        } else {
            null
        }
        val crank = if (hasCrank && raw.size - index >= 4) {
            val crankCount = readUint16(raw, index)?.toLong() ?: return null
            index += 2
            val crankTime = readUint16(raw, index) ?: return null
            BleCrankData(crankCount, crankTime)
        } else {
            null
        }
        return wheel to crank
    }
}

internal object BleRunningSpeedCadenceParser {
    fun parse(
        characteristic: BluetoothGattCharacteristic,
        sensorName: String?,
    ): BleRunningSpeedCadenceData? {
        val raw = characteristic.value ?: return null
        return parsePayload(raw, sensorName)
    }

    fun parsePayload(raw: ByteArray, sensorName: String?): BleRunningSpeedCadenceData? {
        if (raw.isEmpty()) return null
        var index = 1
        val speed = if (raw.size - index >= 2) {
            readUint16(raw, index)?.let { it / 256.0 }
        } else {
            null
        }
        index = 3
        var cadence = if (raw.size - index >= 1) {
            raw[index].toInt() and 0xFF
        } else {
            null
        }?.toLong()
        if (sensorName != null && sensorName.startsWith("TICKR X") && cadence != null) {
            cadence /= 2
        }
        return BleRunningSpeedCadenceData(
            speedMetersPerSecond = speed,
            cadenceRpm = cadence,
        )
    }
}

private fun readInt16(raw: ByteArray, index: Int): Int? {
    if (index + 1 >= raw.size) return null
    return (raw[index + 1].toInt() shl 8) or (raw[index].toInt() and 0xFF)
}

private fun readUint16(raw: ByteArray, index: Int): Int? {
    if (index + 1 >= raw.size) return null
    return ((raw[index + 1].toInt() and 0xFF) shl 8) or (raw[index].toInt() and 0xFF)
}

private fun readUint32(raw: ByteArray, index: Int): Long? {
    if (index + 3 >= raw.size) return null
    return ((raw[index + 3].toLong() and 0xFF) shl 24) or
        ((raw[index + 2].toLong() and 0xFF) shl 16) or
        ((raw[index + 1].toLong() and 0xFF) shl 8) or
        (raw[index].toLong() and 0xFF)
}
