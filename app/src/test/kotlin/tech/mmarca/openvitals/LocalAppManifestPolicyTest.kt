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

    private fun manifestTags(manifest: String, tagName: String): List<String> =
        Regex("""<$tagName\b[^>]*>""").findAll(manifest).map { it.value }.toList()

    private fun String.names(value: String): Boolean =
        contains("""android:name="$value"""")

    private fun String.removesNode(): Boolean =
        contains("""tools:node="remove"""")
}
