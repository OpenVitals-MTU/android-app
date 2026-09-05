package tech.mmarca.openvitals

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Shared reader for the `strings.xml` under every values directory. A resource is keyed by `name`; a `<string>`
 * has the pseudo-quantity [PLAIN], a `<plurals>` one entry per quantity.
 */
internal object TranslationResources {

    /** The pseudo-quantity a plain `<string>` is filed under. */
    const val PLAIN = "value"

    /** `values/` — the source of truth every locale is measured against. */
    val baseDirectory: File = File("src/main/res/values")

    /** `values/` plus every `values-XX/` that actually ships a strings.xml. */
    fun localeDirectories(): List<File> =
        File("src/main/res").listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("values") }
            ?.filter { File(it, "strings.xml").isFile }
            ?.sortedBy { it.name }
            .orEmpty()

    /** `name -> (quantity -> text)` for the `strings.xml` in [dir]. */
    fun read(dir: File): Map<String, Map<String, String>> {
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(dir, "strings.xml"))
            .documentElement
        val out = LinkedHashMap<String, Map<String, String>>()
        for (element in root.childElements()) {
            val name = element.getAttribute("name").takeIf { it.isNotEmpty() } ?: continue
            when (element.tagName) {
                "string" -> out[name] = mapOf(PLAIN to element.textContent)
                "plurals" -> out[name] = element.childElements()
                    .filter { it.tagName == "item" }
                    .associate { it.getAttribute("quantity") to it.textContent }
                else -> Unit
            }
        }
        return out
    }

    /** The `values/` names marked `translatable="false"`, which no locale owns. */
    fun nonTranslatableNames(dir: File): Set<String> {
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(dir, "strings.xml"))
            .documentElement
        return root.childElements()
            .filter { it.tagName == "string" || it.tagName == "plurals" }
            .filter { it.getAttribute("translatable") == "false" }
            .mapNotNull { it.getAttribute("name").takeIf(String::isNotEmpty) }
            .toSet()
    }

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) }
            .filter { it.nodeType == Node.ELEMENT_NODE }
            .map { it as Element }
    }

    /**
     * Every `%`-thing in [text], split into specifiers `String.format` runs and ones it chokes on.
     * `%%` is skipped. A specifier with a space flag is a bare percent, so it is a problem.
     */
    fun scan(text: String): Scan {
        val specifiers = mutableListOf<String>()
        val problems = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            if (text[index] != '%') {
                index++
                continue
            }
            if (text.startsWith("%%", index)) {
                index += 2
                continue
            }
            val match = SPECIFIER.find(text, index)?.takeIf { it.range.first == index }
            if (match == null) {
                problems += "unterminated % at $index"
                break
            }
            if (match.value.dropLast(1).contains(' ')) {
                problems += "'${match.value}' reads as a conversion"
            } else {
                specifiers += match.value
            }
            index = match.range.last + 1
        }
        return Scan(specifiers, problems)
    }

    /**
     * The `values/` names Android runs through `String.format`.
     * Decided from the base file: "Counts as hydration (%)" is never formatted, `%d activity` is.
     */
    fun formattedBaseNames(base: Map<String, Map<String, String>>): Set<String> =
        base.filterValues { values -> values.values.any { scan(it).specifiers.isNotEmpty() } }.keys

    internal data class Scan(val specifiers: List<String>, val problems: List<String>)

    /** A full `java.util.Formatter` specifier. The precision arm is what `scripts/verify-translations.py` misses. */
    private val SPECIFIER = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]""")
}
