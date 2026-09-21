package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * `healthconnect` sits below the repositories. Its readers, writers and mappers must not reach
 * up into `data.repository`. Five permission and gate classes already do; the list may shrink,
 * never grow. The architecture document states this rule.
 */
class HealthConnectLayeringTest {

    private val importers: Set<String> by lazy {
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readLines().any { it.startsWith(ForbiddenImport) } }
            .map { it.name }
            .toSet()
    }

    @Test
    fun `no new healthconnect file imports a repository`() {
        assertWithMessage("healthconnect files importing data.repository. Pass the value in, or move the class up.")
            .that(importers - Allowed)
            .isEmpty()
    }

    @Test
    fun `the list holds only files that still need it`() {
        assertWithMessage("files that no longer import data.repository. Remove them from the list.")
            .that(Allowed - importers)
            .isEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals/healthconnect"
        const val ForbiddenImport = "import tech.mmarca.openvitals.data.repository"

        val Allowed = setOf(
            "HealthConnectSyncGate.kt",
            "HealthConnectUiEntryPoint.kt",
            "MindfulnessIntegrationGate.kt",
            "HealthConnectPermissionUxState.kt",
            "HealthConnectScreenUxCoordinator.kt",
        )
    }
}
