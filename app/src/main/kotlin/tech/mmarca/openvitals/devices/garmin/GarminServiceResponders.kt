package tech.mmarca.openvitals.devices.garmin

import java.util.UUID
import kotlin.random.Random

/**
 * Small watch-initiated service conversations that must be ANSWERED for the
 * watch to get on with its life — it retries an unanswered service request
 * every half minute, and its startup errands (the weather fetch among them)
 * queue behind the ones it considers essential. Calendar asks have their own
 * responder ([GarminCalendarResponder]); this one keeps the auth exchange.
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

    /**
     * Fabricated OAuth credentials, exactly as Gadgetbridge's "fake OAuth"
     * makes them. There is no Garmin account anywhere in this app, but the
     * watch gates its online-flavoured features — the weather fetch through
     * the phone included — on believing it has credentials. The values are
     * never used against any real service: every request the watch makes with
     * them terminates in this app's own HTTP responder.
     */
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
