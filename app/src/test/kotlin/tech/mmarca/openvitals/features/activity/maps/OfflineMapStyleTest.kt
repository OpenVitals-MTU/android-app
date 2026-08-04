package tech.mmarca.openvitals.features.activity.maps

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The runtime expansion of the bundled single-source Protomaps style onto one
 * source per imported pack.
 *
 * Ported from test/features/activity/maps/offline_map_style_test.dart.
 */
class OfflineMapStyleTest {

    private fun baseStyle(): JsonObject = Json.parseToJsonElement(
        """
        {
          "version": 8,
          "name": "OpenVitals Offline Base",
          "sources": {
            "$TemplatePmtilesSourceIdForTests": {
              "type": "vector",
              "url": "__OPENVITALS_PMTILES_URL__"
            }
          },
          "layers": [
            {"id": "background", "type": "background"},
            {
              "id": "water",
              "type": "fill",
              "source": "$TemplatePmtilesSourceIdForTests",
              "source-layer": "water"
            }
          ]
        }
        """.trimIndent(),
    ).jsonObject

    @Test
    fun `expands one source and one templated-layer copy per pack`() {
        val expanded = expandPmtilesStyle(
            root = baseStyle(),
            packFileUrls = listOf("file:///packs/city-a.pmtiles", "file:///packs/city-b.pmtiles"),
        )

        val sources = expanded.getValue("sources").jsonObject
        assertEquals(listOf("openvitals_pmtiles_0", "openvitals_pmtiles_1"), sources.keys.toList())
        val source0 = sources.getValue("openvitals_pmtiles_0").jsonObject
        assertEquals("vector", source0.string("type"))
        assertEquals("pmtiles://file:///packs/city-a.pmtiles", source0.string("url"))
        assertEquals(PmtilesAttribution, source0.string("attribution"))

        val layers = expanded.getValue("layers").jsonArray
        // background passes through unchanged; water duplicated per pack.
        assertEquals(
            listOf("background", "water-0", "water-1"),
            layers.map { it.jsonObject.string("id") },
        )
        assertEquals("openvitals_pmtiles_0", layers[1].jsonObject.string("source"))
        assertEquals("openvitals_pmtiles_1", layers[2].jsonObject.string("source"))
        // Non-source keys of the template layer are preserved on every copy.
        assertEquals("water", layers[2].jsonObject.string("source-layer"))
        // Root keys other than sources/layers pass through.
        assertEquals(8, expanded.getValue("version").jsonPrimitive.int)
        assertEquals("OpenVitals Offline Base", expanded.string("name"))
    }

    @Test
    fun `bundled style asset parses and only references the template source`() {
        val style = Json.parseToJsonElement(bundledStyleFile().readText()).jsonObject
        val layers = style.getValue("layers").jsonArray

        assertTrue(layers.isNotEmpty())
        layers.forEach { element ->
            val layer = element.jsonObject
            val source = layer.string("source")
            // Every layer is either source-less (background) or templated: the
            // runtime expansion in expandPmtilesStyle rebinds all of them.
            assertTrue(
                "layer ${layer.string("id")} references unexpected source $source",
                source == null || source == TemplatePmtilesSourceIdForTests,
            )
            // The style must stay glyph/sprite-free: text/symbol layers would
            // need font assets the app does not bundle.
            assertFalse(
                "layer ${layer.string("id")} needs glyphs",
                layer.string("type") == "symbol",
            )
        }
        assertFalse(style.containsKey("glyphs"))
        assertFalse(style.containsKey("sprite"))
    }

    /** The bundled asset, whichever directory the test task happens to run in. */
    private fun bundledStyleFile(): File {
        val relative = "src/main/assets/offline_maps/protomaps_base_style.json"
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.isFile }
            ?: error("Bundled style asset not found from ${File("").absolutePath}")
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
