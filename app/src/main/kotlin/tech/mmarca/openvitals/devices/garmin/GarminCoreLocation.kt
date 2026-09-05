package tech.mmarca.openvitals.devices.garmin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The phone's position, in the units the watch's `LocationData` wants. */
data class GarminPhoneLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val time: Instant,
    val horizontalAccuracyMeters: Float,
    val verticalAccuracyMeters: Float,
    val bearingDegrees: Float,
    val speedMetersPerSecond: Float,
)

/** Last-known phone position for the watch. A passive read; no fix is ever requested. */
@Singleton
class GarminPhoneLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun lastKnown(): GarminPhoneLocation? {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val best = manager.allProviders
            .mapNotNull { provider ->
                runCatching {
                    @Suppress("MissingPermission")
                    manager.getLastKnownLocation(provider)
                }.getOrNull()
            }
            .maxByOrNull { it.time }
            ?: return null
        return GarminPhoneLocation(
            latitude = best.latitude,
            longitude = best.longitude,
            altitudeMeters = best.altitude,
            time = Instant.ofEpochMilli(best.time),
            horizontalAccuracyMeters = best.accuracy,
            verticalAccuracyMeters = if (android.os.Build.VERSION.SDK_INT >= 26) {
                best.verticalAccuracyMeters
            } else {
                0f
            },
            bearingDegrees = best.bearing,
            speedMetersPerSecond = best.speed,
        )
    }
}

/**
 * Answers the watch's `CoreService` location conversation, the
 * prerequisite for its weather fetch. Mirrors Gadgetbridge.
 */
class GarminCoreLocation(
    private val locationProvider: () -> GarminPhoneLocation?,
) {
    /** A reply, and optionally a follow-up: a granted subscription must deliver an update at once. */
    class Reply(val payload: ByteArray, val followUp: ByteArray? = null)

    private companion object {
        const val GET_LOCATION_REQUEST = 3
        const val GET_LOCATION_RESPONSE = 4
        const val SET_ENABLED_REQUEST = 5
        const val SET_ENABLED_RESPONSE = 6
        const val LOCATION_UPDATED_NOTIFICATION = 7

        const val STATUS_OK = 1
        const val STATUS_NO_VALID_LOCATION = 2

        const val SEMICIRCLES_PER_DEGREE = 2147483648.0 / 180.0
    }

    /** Returns the reply for a core-service message, or null when not ours. */
    fun handle(payload: ByteArray): Reply? {
        val service = protobufField(
            readProtobuf(payload),
            GarminSmartService.CORE,
        )?.bytes ?: return null
        val fields = readProtobuf(service)

        protobufField(fields, GET_LOCATION_REQUEST)?.let {
            return Reply(smartCore(getLocationResponse()))
        }
        protobufField(fields, SET_ENABLED_REQUEST)?.bytes?.let { request ->
            // The grant, then a first update: the subscription is live only once something arrives.
            return Reply(
                payload = smartCore(setEnabledResponse(request)),
                followUp = locationUpdateNotification(),
            )
        }
        return null
    }

    /** One pushed position, or null when the phone has none to give. */
    fun locationUpdateNotification(): ByteArray? {
        val location = locationProvider() ?: return null
        val notification = ProtobufWriter()
            .nested(1, locationData(location))
            .toBytes()
        return smartCore(
            ProtobufWriter().nested(LOCATION_UPDATED_NOTIFICATION, notification).toBytes(),
        )
    }

    private fun getLocationResponse(): ByteArray {
        val location = locationProvider()
        GarminLog.log(
            "[GARMIN-CORE] watch asked where the phone is: " +
                if (location == null) "no position to give" else "answered",
        )
        val response = ProtobufWriter()
        if (location == null) {
            response.varint(1, STATUS_NO_VALID_LOCATION)
        } else {
            response.varint(1, STATUS_OK)
            response.nested(2, locationData(location))
        }
        return ProtobufWriter().nested(GET_LOCATION_RESPONSE, response.toBytes()).toBytes()
    }

    /**
     * Acknowledges a subscription on paper; no stream is sent. The watch
     * re-asks with `GetLocationRequest` when it wants a fix.
     */
    private fun setEnabledResponse(request: ByteArray): ByteArray {
        val requested = readProtobuf(request)
            .filter { it.field == 2 }
            .mapNotNull { entry ->
                entry.bytes?.let { bytes ->
                    protobufField(readProtobuf(bytes), 1)?.varint
                }
            }
        GarminLog.log("[GARMIN-CORE] location updates asked for ${requested.size} stream(s)")
        val response = ProtobufWriter().varint(1, STATUS_OK)
        for (dataType in requested) {
            response.nested(
                2,
                ProtobufWriter()
                    .varint(1, dataType)
                    .varint(2, STATUS_OK)
                    .toBytes(),
            )
        }
        return ProtobufWriter().nested(SET_ENABLED_RESPONSE, response.toBytes()).toBytes()
    }

    private fun locationData(location: GarminPhoneLocation): ByteArray {
        val latLon = ProtobufWriter()
            .sint32(1, (location.latitude * SEMICIRCLES_PER_DEGREE).toInt())
            .sint32(2, (location.longitude * SEMICIRCLES_PER_DEGREE).toInt())
            .toBytes()
        return ProtobufWriter()
            .nested(1, latLon)
            .fixed32(2, location.altitudeMeters.toFloat())
            .varint(3, GarminTime.fromInstant(location.time))
            .fixed32(4, location.horizontalAccuracyMeters)
            .fixed32(5, location.verticalAccuracyMeters)
            .varint(6, 1) // DataType.GENERAL_LOCATION
            .fixed32(9, location.bearingDegrees)
            .fixed32(10, location.speedMetersPerSecond)
            .toBytes()
    }

    private fun smartCore(service: ByteArray): ByteArray =
        ProtobufWriter().nested(GarminSmartService.CORE, service).toBytes()
}
