package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.fit.FitBaseType
import tech.mmarca.openvitals.core.fit.FitEncoder
import tech.mmarca.openvitals.core.fit.FitEncoderField
import tech.mmarca.openvitals.core.fit.fitSemicircles
import tech.mmarca.openvitals.core.fit.fitTimestamp

/** A named point for the watch's Saved Locations. Degrees, WGS 84. */
data class GarminWaypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
)

/**
 * The FIT location file the watch stores as a saved location, from upstream's
 * `WaypointHelper.generateFitLocationFile` (AGPLv3).
 */
object GarminLocationFile {

    fun build(point: GarminWaypoint, now: Instant): ByteArray {
        val timestamp = fitTimestamp(now)
        val encoder = FitEncoder()

        encoder.defineMessage(
            FitLocalFileId, FitFileIdMessage,
            listOf(
                FitEncoderField(FitFileIdTypeField, FitBaseType.ENUM),
                FitEncoderField(FitFileIdManufacturerField, FitBaseType.UINT16),
                FitEncoderField(FitFileIdProductField, FitBaseType.UINT16),
                FitEncoderField(FitFileIdSerialNumberField, FitBaseType.UINT32Z),
                FitEncoderField(FitFileIdTimeCreatedField, FitBaseType.UINT32),
                FitEncoderField(FitFileIdNumberField, FitBaseType.UINT16),
                FitEncoderField(FitFileIdProductNameField, FitBaseType.STRING, FitProductNameSize),
            ),
        )
        encoder.writeMessage(
            FitLocalFileId,
            values = mapOf(
                FitFileIdTypeField to FitFileTypeLocation,
                FitFileIdManufacturerField to FitManufacturerGarmin,
                FitFileIdProductField to FitProductConnect,
                FitFileIdSerialNumberField to 1L,
                FitFileIdTimeCreatedField to timestamp,
                FitFileIdNumberField to 1L,
            ),
            strings = mapOf(FitFileIdProductNameField to FitProductName),
        )

        encoder.defineMessage(
            FitLocalFileCreator, FitFileCreatorMessage,
            listOf(FitEncoderField(FitFileCreatorSoftwareVersionField, FitBaseType.UINT16)),
        )
        encoder.writeMessage(
            FitLocalFileCreator,
            values = mapOf(FitFileCreatorSoftwareVersionField to FitSoftwareVersion),
        )

        encoder.defineMessage(
            FitLocalLocation, FitLocationMessage,
            listOf(
                FitEncoderField(FitTimestampField, FitBaseType.UINT32),
                FitEncoderField(FitLocationNameField, FitBaseType.STRING, FitLocationNameSize),
                FitEncoderField(FitLocationLatitudeField, FitBaseType.SINT32),
                FitEncoderField(FitLocationLongitudeField, FitBaseType.SINT32),
                FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
                FitEncoderField(FitLocationAltitudeField, FitBaseType.UINT16),
            ),
        )
        encoder.writeMessage(
            FitLocalLocation,
            values = mapOf(
                FitTimestampField to timestamp,
                FitLocationLatitudeField to fitSemicircles(point.latitude),
                FitLocationLongitudeField to fitSemicircles(point.longitude),
                FitMessageIndexField to 0L,
                FitLocationAltitudeField to point.altitudeMeters?.let(::fitAltitude),
            ),
            strings = mapOf(FitLocationNameField to point.name.withoutControlCharacters()),
        )

        return ByteArrayOutputStream().also(encoder::writeTo).toByteArray()
    }
}

/** Scaled altitude, or null when the field cannot hold it. */
private fun fitAltitude(meters: Double): Long? =
    ((meters + FitAltitudeOffsetMeters) * FitAltitudeScale).roundToLong()
        .takeIf { it in 0L..FitAltitudeMax }

private fun String.withoutControlCharacters(): String =
    filterNot(Char::isISOControl).trim()

// The FIT vocabulary this file writes, from Garmin's FIT profile.
private const val FitLocalFileId = 0
private const val FitLocalFileCreator = 1
private const val FitLocalLocation = 2

private const val FitFileIdMessage = 0
private const val FitLocationMessage = 29
private const val FitFileCreatorMessage = 49

private const val FitTimestampField = 253
private const val FitMessageIndexField = 254

private const val FitFileIdTypeField = 0
private const val FitFileIdManufacturerField = 1
private const val FitFileIdProductField = 2
private const val FitFileIdSerialNumberField = 3
private const val FitFileIdTimeCreatedField = 4
private const val FitFileIdNumberField = 5
private const val FitFileIdProductNameField = 8

private const val FitFileCreatorSoftwareVersionField = 0

private const val FitLocationNameField = 0
private const val FitLocationLatitudeField = 1
private const val FitLocationLongitudeField = 2
private const val FitLocationAltitudeField = 4

private const val FitFileTypeLocation = 8L

/** Upstream's values: the watch is known to accept a file that claims them. */
private const val FitManufacturerGarmin = 1L
private const val FitProductConnect = 65534L
private const val FitSoftwareVersion = 1L
private const val FitProductName = "OpenVitals"

/** Field sizes from the FIT profile, NUL included. */
private const val FitProductNameSize = 20
private const val FitLocationNameSize = 32

private const val FitAltitudeScale = 5.0
private const val FitAltitudeOffsetMeters = 500.0
private const val FitAltitudeMax = 0xFFFEL
