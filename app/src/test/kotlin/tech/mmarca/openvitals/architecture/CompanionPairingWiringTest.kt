package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * `CompanionDevicePairing.associate` shows its dialog through a launcher that only
 * `attachToActivity` sets. The native rewrite dropped the one caller, and every new watch
 * then paired with no companion association. Nothing failed, so nothing noticed.
 */
class CompanionPairingWiringTest {

    @Test
    fun `an Activity attaches the companion pairing launcher`() {
        val callers = File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "CompanionDevicePairing.kt" }
            .filter { file -> file.readLines().any { AttachCall.containsMatchIn(it.substringBefore("//")) } }
            .map { it.name }
            .toList()

        assertWithMessage("No production code calls attachToActivity, so associate() always returns false.")
            .that(callers)
            .isNotEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        val AttachCall = Regex("""\.attachToActivity\s*\(""")
    }
}
