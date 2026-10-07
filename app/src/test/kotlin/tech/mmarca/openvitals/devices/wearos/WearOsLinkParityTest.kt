package tech.mmarca.openvitals.devices.wearos

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone and the watch share no module, so the link protocol lives in
 * both as one file copied twice. A drift fails silently on the wrist; this
 * fails it here instead.
 */
class WearOsLinkParityTest {

    private val phoneCopy = File("src/main/kotlin/tech/mmarca/openvitals/devices/wearos/WearLinkProtocol.kt")
    private val watchCopy = File("../wear/src/main/kotlin/tech/mmarca/openvitals/wear/WearLinkProtocol.kt")
    private val watchService = File("../wear/src/main/kotlin/tech/mmarca/openvitals/wear/WearAppService.kt")

    @Test
    fun `the protocol file is the same on both sides, package line aside`() {
        assertEquals(body(phoneCopy), body(watchCopy))
    }

    @Test
    fun `both sides take the UUID and the words from the protocol file`() {
        val service = watchService.readText()
        val phone = File("src/main/kotlin/tech/mmarca/openvitals/devices/wearos/BluetoothWearOsNodePort.kt").readText()

        assertTrue(service.contains("WearLinkProtocol.SERVICE_UUID"))
        assertTrue(phone.contains("WearLinkProtocol.SERVICE_UUID"))
        assertTrue(service.contains("WearLinkProtocol.PONG"))
        assertTrue(phone.contains("WearLinkProtocol.PING"))
    }

    private fun body(file: File): String =
        file.readLines().filterNot { it.startsWith("package ") }.joinToString("\n").trim()
}
