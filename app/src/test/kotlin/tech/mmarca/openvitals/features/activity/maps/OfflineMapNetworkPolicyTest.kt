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
 * The privacy half of Flutter's
 * `test/features/activity/maps/route_map_view_test.dart` case "default
 * (offline) render draws no tile layer and fetches no network tiles".
 *
 * OpenVitals is local-first: the manifest deletes `INTERNET` outright, so a map
 * renderer that reached for a remote tile would not degrade politely, it would
 * throw somewhere inside the render thread. The Flutter test could assert this
 * directly because `TileLayer` was a widget it could look for. On Android the
 * equivalent decision is not in the view tree at all — it is in the style JSON
 * handed to MapLibre, which is where every URL the renderer will ever open
 * comes from. So that is where it is pinned here.
 *
 * What this proves: no bundled or generated style configuration can name a
 * remote host. The template asset carries no URL of its own, and
 * [expandPmtilesStyle] rebinds *every* source to a `pmtiles://file://` URL of
 * an imported pack, discarding whatever the template declared — so even a
 * template that grew an `https://` tile source could not put one in front of
 * MapLibre.
 *
 * What this does not prove: that no socket is ever opened. Nothing at this
 * layer can. The two facts that back that up are elsewhere and are asserted
 * elsewhere: `LocalAppManifestPolicyTest` (the app removes INTERNET,
 * ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE from every merged manifest) and
 * `OfflineRouteMapTest` (with no pack imported no tile-rendering view is built
 * at all). Together they are the guarantee; individually each is a third of it.
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
        // `mapbox://`/`maplibre://` resolve against a hosted API, so they are as
        // remote as http in practice.
        assertTrue(
            "The offline base style must not use a hosted-API scheme.",
            !styleText.contains("mapbox://") && !styleText.contains("maplibre://"),
        )
    }

    @Test
    fun `expanding a style rebinds every source to a local pack file`() {
        // Deliberately hostile input: a template that already carries a remote
        // raster source next to the templated vector one. The expansion must
        // replace the source map, not merge into it.
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

        // The layer that pointed at the remote source is left pointing at a
        // source that no longer exists, which MapLibre drops. What matters is
        // that it cannot be fed: nothing outside the generated pack ids is a
        // live source any more.
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
