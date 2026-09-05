package tech.mmarca.openvitals.features.activity.maps

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The manifest deletes `INTERNET`, so a renderer that reached for a remote tile would throw
 * on the render thread. Every URL MapLibre opens comes from the style JSON, so that is pinned:
 * the template carries no URL and [expandPmtilesStyle] rebinds every source to `pmtiles://file://`.
 *
 * This does not prove no socket is opened. `LocalAppManifestPolicyTest` and `OfflineRouteMapTest`
 * hold the other two thirds of that guarantee.
 */
class OfflineMapNetworkPolicyTest {

    @Test
    fun `the bundled style names no remote host`() {
        val styleText = bundledStyleFile().readText()

        val remoteUrls = Regex("""https?://[^"\s]*""").findAll(styleText).map { it.value }.toList()
        assertEquals(
            "The offline base style must not reference a network URL.",
            emptyList<String>(),
            remoteUrls,
        )
        // `mapbox://` and `maplibre://` resolve against a hosted API, so they are remote.
        assertTrue(
            "The offline base style must not use a hosted-API scheme.",
            !styleText.contains("mapbox://") && !styleText.contains("maplibre://"),
        )
    }

    @Test
    fun `expanding a style rebinds every source to a local pack file`() {
        // A template that already carries a remote raster source. The expansion must replace the source map.
        val root = Json.parseToJsonElement(
            """
            {
              "version": 8,
              "sources": {
                "$TemplatePmtilesSourceIdForTests": {
                  "type": "vector",
                  "url": "https://tiles.example.com/planet.json"
                },
                "satellite": {
                  "type": "raster",
                  "tiles": ["https://tiles.example.com/{z}/{x}/{y}.png"]
                }
              },
              "layers": [
                {"id": "background", "type": "background"},
                {
                  "id": "roads",
                  "type": "line",
                  "source": "$TemplatePmtilesSourceIdForTests",
                  "source-layer": "roads"
                },
                {"id": "satellite", "type": "raster", "source": "satellite"}
              ]
            }
            """.trimIndent(),
        ).jsonObject

        val expanded = expandPmtilesStyle(
            root = root,
            packFileUrls = listOf("file:///data/user/0/tech.mmarca.openvitals/files/offline_maps/berlin.pmtiles"),
        )

        val sources = expanded.getValue("sources").jsonObject
        assertEquals(listOf("openvitals_pmtiles_0"), sources.keys.toList())
        sources.values.forEach { source ->
            assertEquals(
                "pmtiles://file:///data/user/0/tech.mmarca.openvitals/files/offline_maps/berlin.pmtiles",
                source.jsonObject.string("url"),
            )
        }
        assertEquals(
            "No remote URL may survive the expansion.",
            emptyList<String>(),
            Regex("""https?://[^"]*""").findAll(expanded.toString()).map { it.value }.toList(),
        )

        // The layer pointing at the remote source is left dangling, which MapLibre drops. It cannot be fed.
        val liveSourceIds = sources.keys
        val templatedLayerSources = expanded.getValue("layers").jsonArray
            .mapNotNull { it.jsonObject.string("source") }
            .filter { it in liveSourceIds }
        assertEquals(listOf("openvitals_pmtiles_0"), templatedLayerSources)
    }

    @Test
    fun `the shipped style expanded over real packs opens only local files`() {
        val root = Json.parseToJsonElement(bundledStyleFile().readText()).jsonObject

        val expanded = expandPmtilesStyle(
            root = root,
            packFileUrls = listOf("file:///packs/estonia.pmtiles", "file:///packs/latvia.pmtiles"),
        )

        val urls = expanded.getValue("sources").jsonObject.values
            .mapNotNull { it.jsonObject.string("url") }
        assertEquals(
            listOf(
                "pmtiles://file:///packs/estonia.pmtiles",
                "pmtiles://file:///packs/latvia.pmtiles",
            ),
            urls,
        )
        urls.forEach { url ->
            assertTrue("$url is not a local file URL.", url.startsWith("pmtiles://file:///"))
        }
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
