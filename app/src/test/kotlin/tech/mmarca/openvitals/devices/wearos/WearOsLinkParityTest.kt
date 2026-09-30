package tech.mmarca.openvitals.devices.wearos

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone and the watch share no module, so the link's constants live in
 * both. A drift fails silently on the wrist; this fails it here instead.
 */
class WearOsLinkParityTest {

    private val watchService = File("../wear/src/main/kotlin/tech/mmarca/openvitals/wear/WearAppService.kt")

    @Test
    fun `the watch listens on the UUID the phone connects to`() {
        val source = watchService.readText()
        val uuid = BluetoothWearOsNodePort.OPENVITALS_WEAR_APP_UUID.toString()

        assertTrue("WearAppService must use $uuid", source.contains("\"$uuid\""))
    }

    @Test
    fun `the watch speaks the same ping and pong`() {
        val source = watchService.readText()

        assertTrue(source.contains("\"${BluetoothWearOsNodePort.PING}\""))
        assertTrue(source.contains("\"${BluetoothWearOsNodePort.PONG}\""))
    }
}
