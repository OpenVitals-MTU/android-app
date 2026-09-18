package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * `runBlocking` freezes its thread. On the main thread that is an "app not
 * responding" dialog; on a coroutine worker it starves the pool. The files
 * listed here may keep theirs. Nothing else may add one.
 */
class NoRunBlockingRatchetTest {

    @Test
    fun `runBlocking stays inside the allow-list`() {
        val offenders = File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readLines().any { line -> RunBlockingCall.containsMatchIn(line.substringBefore("//")) } }
            .map { it.name }
            .filterNot { it in Allowed }
            .toList()

        assertWithMessage(
            "runBlocking outside the allow-list. Make the function suspend and hop with " +
                "withContext(dispatchers.io). See Threading in docs/engineering/architecture.md.",
        )
            .that(offenders)
            .isEmpty()
    }

    @Test
    fun `the allow-list holds no stale names`() {
        val present = File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.name in Allowed }
            .filter { file -> file.readLines().any { RunBlockingCall.containsMatchIn(it.substringBefore("//")) } }
            .map { it.name }
            .toSet()

        assertWithMessage("remove a file from the allow-list once its runBlocking is gone")
            .that(present)
            .isEqualTo(Allowed)
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        val RunBlockingCall = Regex("""\brunBlocking\s*[({]""")

        val Allowed = setOf(
            // One-time Flutter migration, before any UI exists.
            "FlutterDatabaseImporter.kt",
            // SAX callbacks on a worker thread cannot suspend.
            "AppleHealthImportService.kt",
        )
    }
}
