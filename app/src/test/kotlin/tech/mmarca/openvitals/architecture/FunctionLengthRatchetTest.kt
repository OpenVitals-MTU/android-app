package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A ratchet on function length: the count of long functions may fall, never rise, and the four
 * longest may not grow.
 *
 * A function ends at the next line indented no deeper than its `fun`. That holds for this code
 * base's formatting, and it counts expression bodies too. When this fails, split the function.
 * When you shorten one, lower the number in the same commit.
 */
class FunctionLengthRatchetTest {

    private data class LongFunction(val file: String, val name: String, val lines: Int)

    private val functions: List<LongFunction> by lazy {
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> functionsIn(file) }
            .toList()
    }

    private fun functionsIn(file: File): List<LongFunction> {
        val lines = file.readLines()
        return lines.mapIndexedNotNull { start, line ->
            val match = Declaration.find(line) ?: return@mapIndexedNotNull null
            val indent = match.groupValues[1].length
            var end = lines.lastIndex
            for (index in start + 1..lines.lastIndex) {
                val candidate = lines[index]
                if (candidate.isBlank()) continue
                val trimmed = candidate.trimStart()
                // A line starting with ")" closes a wrapped parameter list, not the function.
                if (candidate.length - trimmed.length <= indent && !trimmed.startsWith(")")) {
                    end = if (trimmed == "}") index else index - 1
                    break
                }
            }
            LongFunction(file.relativeTo(File(SourceRoot)).invariantSeparatorsPath, match.groupValues[2], end - start + 1)
        }
    }

    @Test
    fun `functions over the limit do not increase`() {
        val long = functions.filter { it.lines > MaxLines }

        assertWithMessage("functions over $MaxLines lines went UP. Split the new one: ${long.sortedByDescending { it.lines }.take(10)}")
            .that(long.size)
            .isAtMost(MaxLongFunctions)
    }

    @Test
    fun `only the listed giants pass the second limit, and they do not grow`() {
        val giants = functions.filter { it.lines > GiantLines }
        val grown = giants.filter { giant -> giant.lines > (GiantCeilings["${giant.file}#${giant.name}"] ?: 0) }

        assertWithMessage("functions over $GiantLines lines that are new, or grew past their ceiling: $grown")
            .that(grown)
            .isEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        const val MaxLines = 150
        const val GiantLines = 450

        /** The count on 2026-09-21, after the Settings split. */
        const val MaxLongFunctions = 55

        /** Each ceiling is the length on 2026-09-21 plus about 20 lines. */
        val GiantCeilings = mapOf(
            "navigation/AppNavigation.kt#AppNavigation" to 830,
            "data/repository/dashboard/DashboardDataLoader.kt#loadDashboardUncached" to 490,
        )

        val Declaration = Regex(
            """^(\s*)(?:(?:private|internal|public|protected|override|suspend|inline|operator|infix|tailrec|open|abstract|final)\s+)*""" +
                """fun\s+(?:<[^>]+>\s*)?(?:[\w.<>?, ]+\.)?(`[^`]+`|\w+)\s*\(""",
        )
    }
}
