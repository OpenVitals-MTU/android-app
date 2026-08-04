package tech.mmarca.openvitals.sensors.ble

import java.util.UUID

internal data class BleServiceMeasurementUuid(
    val serviceUuid: UUID,
    val measurementUuid: UUID,
)

internal object BleUuids {
    val CLIENT_CHARACTERISTIC_CONFIG: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val BATTERY_SERVICE: UUID =
        UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

    val BATTERY_LEVEL: UUID =
        UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    val HEART_RATE = BleServiceMeasurementUuid(
        serviceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
        measurementUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
    )

    val HEART_RATE_MIBAND = BleServiceMeasurementUuid(
        serviceUuid = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"),
        measurementUuid = HEART_RATE.measurementUuid,
    )

    val CYCLING_SPEED_CADENCE = BleServiceMeasurementUuid(
        serviceUuid = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb"),
        measurementUuid = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb"),
    )

    val CYCLING_POWER = BleServiceMeasurementUuid(
        serviceUuid = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb"),
        measurementUuid = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb"),
    )

    val RUNNING_SPEED_CADENCE = BleServiceMeasurementUuid(
        serviceUuid = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb"),
        measurementUuid = UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb"),
    )

    val SCAN_SERVICE_UUIDS = listOf(
        HEART_RATE.serviceUuid,
        HEART_RATE_MIBAND.serviceUuid,
        CYCLING_SPEED_CADENCE.serviceUuid,
        CYCLING_POWER.serviceUuid,
        RUNNING_SPEED_CADENCE.serviceUuid,
    )

    fun capabilitiesForService(serviceUuid: UUID): Set<tech.mmarca.openvitals.domain.model.BleSensorCapability> =
        when (serviceUuid) {
            HEART_RATE.serviceUuid,
            HEART_RATE_MIBAND.serviceUuid,
            -> setOf(tech.mmarca.openvitals.domain.model.BleSensorCapability.HEART_RATE)
            CYCLING_SPEED_CADENCE.serviceUuid -> setOf(
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_CADENCE,
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_SPEED_DISTANCE,
            )
            CYCLING_POWER.serviceUuid -> setOf(
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_POWER,
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_CADENCE,
            )
            RUNNING_SPEED_CADENCE.serviceUuid -> setOf(
                tech.mmarca.openvitals.domain.model.BleSensorCapability.RUNNING_SPEED_CADENCE,
            )
            else -> emptySet()
        }

    fun capabilitiesForCharacteristic(characteristicUuid: UUID): Set<tech.mmarca.openvitals.domain.model.BleSensorCapability> =
        when (characteristicUuid) {
            HEART_RATE.measurementUuid ->
                setOf(tech.mmarca.openvitals.domain.model.BleSensorCapability.HEART_RATE)
            CYCLING_SPEED_CADENCE.measurementUuid -> setOf(
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_CADENCE,
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_SPEED_DISTANCE,
            )
            CYCLING_POWER.measurementUuid -> setOf(
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_POWER,
                tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_CADENCE,
            )
            RUNNING_SPEED_CADENCE.measurementUuid ->
                setOf(tech.mmarca.openvitals.domain.model.BleSensorCapability.RUNNING_SPEED_CADENCE)
            else -> emptySet()
        }

    fun measurementUuidsForCapability(
        capability: tech.mmarca.openvitals.domain.model.BleSensorCapability,
    ): List<BleServiceMeasurementUuid> =
        when (capability) {
            tech.mmarca.openvitals.domain.model.BleSensorCapability.HEART_RATE ->
                listOf(HEART_RATE, HEART_RATE_MIBAND)
            tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_CADENCE ->
                listOf(CYCLING_SPEED_CADENCE, CYCLING_POWER)
            tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_POWER ->
                listOf(CYCLING_POWER)
            tech.mmarca.openvitals.domain.model.BleSensorCapability.CYCLING_SPEED_DISTANCE ->
                listOf(CYCLING_SPEED_CADENCE)
            tech.mmarca.openvitals.domain.model.BleSensorCapability.RUNNING_SPEED_CADENCE ->
                listOf(RUNNING_SPEED_CADENCE)
        }
}
