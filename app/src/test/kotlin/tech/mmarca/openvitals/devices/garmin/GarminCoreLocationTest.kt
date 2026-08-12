package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The watch's location asks — the gate its weather fetch waits behind. */
class GarminCoreLocationTest {

    private val tallinn = GarminPhoneLocation(
        latitude = 59.437,
        longitude = 24.7536,
        altitudeMeters = 40.0,
        time = Instant.parse("2026-08-12T10:00:00Z"),
        horizontalAccuracyMeters = 12f,
        verticalAccuracyMeters = 3f,
        bearingDegrees = 0f,
        speedMetersPerSecond = 0f,
    )

    private fun smartCore(service: ByteArray): ByteArray =
        ProtobufWriter().nested(GarminSmartService.CORE, service).toBytes()

    @Test
    fun `a location ask is answered with the phone's position`() {
        val responder = GarminCoreLocation { tallinn }
        // GetLocationRequest{request_type: STANDARD}
        val ask = smartCore(
            ProtobufWriter().nested(3, ProtobufWriter().varint(1, 0).toBytes()).toBytes(),
        )

        val reply = responder.handle(ask)!!.payload
        val core = protobufField(readProtobuf(reply), GarminSmartService.CORE)!!.bytes!!
        val response = readProtobuf(protobufField(readProtobuf(core), 4)!!.bytes!!)
        assertEquals(1L, protobufField(response, 1)?.varint) // OK

        val data = readProtobuf(protobufField(response, 2)!!.bytes!!)
        val latLon = readProtobuf(protobufField(data, 1)!!.bytes!!)
        // sint32 zigzag round-trip back to degrees.
        fun unzigzag(v: Long) = ((v ushr 1).toInt()) xor -((v and 1).toInt())
        val lat = unzigzag(protobufField(latLon, 1)!!.varint!!) / (2147483648.0 / 180.0)
        val lon = unzigzag(protobufField(latLon, 2)!!.varint!!) / (2147483648.0 / 180.0)
        assertEquals(59.437, lat, 1e-6)
        assertEquals(24.7536, lon, 1e-6)
    }

    @Test
    fun `no position answers NO_VALID_LOCATION, never a zero island`() {
        val responder = GarminCoreLocation { null }
        val ask = smartCore(
            ProtobufWriter().nested(3, ProtobufWriter().varint(1, 0).toBytes()).toBytes(),
        )

        val reply = responder.handle(ask)!!.payload
        val core = protobufField(readProtobuf(reply), GarminSmartService.CORE)!!.bytes!!
        val response = readProtobuf(protobufField(readProtobuf(core), 4)!!.bytes!!)
        assertEquals(2L, protobufField(response, 1)?.varint) // NO_VALID_LOCATION
        assertNull(protobufField(response, 2)) // and no fabricated (0,0)
    }

    @Test
    fun `an update subscription is granted per requested stream`() {
        val responder = GarminCoreLocation { tallinn }
        // SetEnabledRequest{enabled: true, requests: [GENERAL(1), REALTIME(2)]}
        val request = ProtobufWriter()
            .varint(1, 1)
            .nested(2, ProtobufWriter().varint(1, 1).toBytes())
            .nested(2, ProtobufWriter().varint(1, 2).toBytes())
            .toBytes()
        val ask = smartCore(ProtobufWriter().nested(5, request).toBytes())

        val reply = responder.handle(ask)!!.payload
        val core = protobufField(readProtobuf(reply), GarminSmartService.CORE)!!.bytes!!
        val response = readProtobuf(protobufField(readProtobuf(core), 6)!!.bytes!!)
        assertEquals(1L, protobufField(response, 1)?.varint) // OK
        assertEquals(2, response.count { it.field == 2 })
    }

    @Test
    fun `granting a subscription is followed by a first position`() {
        val responder = GarminCoreLocation { tallinn }
        val ask = smartCore(
            ProtobufWriter().nested(5, ProtobufWriter().varint(1, 1).toBytes()).toBytes(),
        )

        val followUp = responder.handle(ask)!!.followUp!!
        val core = protobufField(readProtobuf(followUp), GarminSmartService.CORE)!!.bytes!!
        val notification = protobufField(readProtobuf(core), 7)!!.bytes!!
        // One pushed LocationData — the stream is live, not merely granted.
        assertEquals(1, readProtobuf(notification).count { it.field == 1 })
    }

    @Test
    fun `other services are left alone`() {
        val responder = GarminCoreLocation { tallinn }
        assertNull(
            responder.handle(
                ProtobufWriter().nested(GarminSmartService.SETTINGS, ByteArray(0)).toBytes(),
            ),
        )
    }
}
