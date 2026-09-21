package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * GarminProtobufTransport keeps plain maps. The frame pump, the requests and abort() are
 * three coroutines, so whatever hosts a session must run them one at a time. The link opens
 * a real radio and cannot be driven in a JVM test, so this checks the source: every session
 * there is hosted by the sequential dispatcher.
 */
class GarminSessionConfinementTest {

    private val link = File(
        "src/main/kotlin/tech/mmarca/openvitals/devices/garmin/GarminRadioLink.kt",
    ).readText()

    @Test
    fun `every session in the radio link is hosted on the sequential dispatcher`() {
        val sessions = Regex("""\bGarminSession\(""").findAll(link).count()
        val hosts = Regex("""=\s*onSessionThread\s*\{""").findAll(link).count()

        assertWithMessage("sessions built in GarminRadioLink").that(sessions).isGreaterThan(0)
        assertWithMessage(
            "a GarminSession is built outside onSessionThread. Its coroutines would run in " +
                "parallel on the default dispatcher and share the transport's plain maps.",
        ).that(hosts).isEqualTo(sessions)
        assertWithMessage("onSessionThread must stay sequential")
            .that(link).contains("Dispatchers.Default.limitedParallelism(1)")
    }
}
