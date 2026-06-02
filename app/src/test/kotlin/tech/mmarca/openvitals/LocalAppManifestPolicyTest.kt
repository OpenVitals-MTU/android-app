package tech.mmarca.openvitals

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalAppManifestPolicyTest {

    @Test
    fun `local app does not request internet access`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(
            "The local OpenVitals app must remain internet-free.",
            manifest.contains("android.permission.INTERNET"),
        )
    }
}
