package tech.mmarca.openvitals.devices.garmin

import java.util.UUID
import kotlin.random.Random

/**
 * Small watch-initiated conversations that must be answered, or the watch
 * retries every half minute and queues its errands. Keeps the auth exchange.
 */
object GarminServiceResponders {

    private const val OAUTH_REQUEST = 1
    private const val OAUTH_RESPONSE = 2

    /** Handles the auth ask; null when the message is something else. */
    fun handle(payload: ByteArray): ByteArray? {
        val fields = readProtobuf(payload)
        protobufField(fields, GarminSmartService.AUTHENTICATION)?.bytes?.let { service ->
            if (protobufField(readProtobuf(service), OAUTH_REQUEST) != null) {
                return oauthResponse()
            }
        }
        return null
    }

    /** Fabricated OAuth credentials, as Gadgetbridge makes them. Never used against a real service. */
    private fun oauthResponse(): ByteArray {
        GarminLog.log("[GARMIN-AUTH] watch asked for credentials; issuing fake ones")
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        fun secret() = (1..35).map { alphabet[Random.nextInt(alphabet.length)] }
            .joinToString("")
        val keys = ProtobufWriter()
            .string(1, UUID.randomUUID().toString())
            .string(2, secret())
            .string(3, UUID.randomUUID().toString())
            .string(4, secret())
            .toBytes()
        val response = ProtobufWriter()
            .nested(1, keys)
            .varint(2, 0)
            .toBytes()
        return ProtobufWriter()
            .nested(
                GarminSmartService.AUTHENTICATION,
                ProtobufWriter().nested(OAUTH_RESPONSE, response).toBytes(),
            )
            .toBytes()
    }
}
