package tech.mmarca.openvitals

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAppManifestPolicyTest {

    @Test
    fun `local app removes inherited network access`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val permissionTags = manifestTags(manifest, "uses-permission")
        val featureTags = manifestTags(manifest, "uses-feature")

        listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
        ).forEach { permission ->
            assertTrue(
                "$permission must be removed from inherited manifests.",
                permissionTags.any { it.names(permission) && it.removesNode() },
            )
            assertFalse(
                "$permission must not be requested directly.",
                permissionTags.any { it.names(permission) && !it.removesNode() },
            )
        }

        assertTrue(
            "android.hardware.wifi must be removed from inherited manifests.",
            featureTags.any { it.names("android.hardware.wifi") && it.removesNode() },
        )
    }

    @Test
    fun `legacy Bluetooth permissions are declared for Android 11 and older`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val permissionTags = manifestTags(manifest, "uses-permission")

        listOf(
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
        ).forEach { permission ->
            assertTrue(
                "$permission must be declared with maxSdkVersion 30, or Bluetooth throws below API 31.",
                permissionTags.any { it.names(permission) && it.contains("""android:maxSdkVersion="30"""") },
            )
        }
    }

    @Test
    fun `no intent filter accepts a file link`() {
        // Another app chooses the link. A file:// one can name a file in our own private storage.
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(Regex("""android:scheme\s*=\s*"file"""").containsMatchIn(manifest))
    }

    @Test
    fun `the diagnostic files stay behind when the phone is transferred`() {
        // allowBackup="false" does not cover a device-to-device transfer from Android 12 on.
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val rules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        val transfer = rules.substringAfter("<device-transfer>").substringBefore("</device-transfer>")

        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        listOf("crash_reports/", "import_reports/", "device_sync/").forEach { directory ->
            assertTrue(
                "$directory must be excluded from device transfer.",
                transfer.contains("""<exclude domain="file" path="$directory" />"""),
            )
        }
    }

    private fun manifestTags(manifest: String, tagName: String): List<String> =
        Regex("""<$tagName\b[^>]*>""").findAll(manifest).map { it.value }.toList()

    private fun String.names(value: String): Boolean =
        contains("""android:name="$value"""")

    private fun String.removesNode(): Boolean =
        contains("""tools:node="remove"""")
}
