package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/** The `devices/` layer is wired in `di/DevicesModule.kt` only. The architecture document says so. */
class DevicesLayeringTest {

    @Test
    fun `no Hilt module is declared inside devices`() {
        val declaring = File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readLines().any { it.trim() == "@Module" } }
            .map { it.name }
            .toList()

        assertWithMessage("Hilt modules inside devices/. Move the binding to di/DevicesModule.kt.")
            .that(declaring)
            .isEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals/devices"
    }
}
