package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportPhase
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportProgress
import tech.mmarca.openvitals.features.activity.maps.OfflineMapImportResult
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPack
import tech.mmarca.openvitals.features.activity.maps.OfflineMapPackFormat
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Offline maps are the only way to see a route without a network. The card must keep its
 * empty state, its pack list and its import button.
 */
class OfflineMapsCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withNoPacksTheCardSaysSoAndStillOffersTheImport() {
        setCard()

        composeRule.onNodeWithText(string(R.string.settings_offline_maps_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_offline_maps_empty))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_offline_maps_import_action))
            .performScrollTo()
            .assertIsEnabled()

        // With nothing imported the renderer picker must stay away.
        composeRule
            .onNodeWithText(string(R.string.settings_offline_maps_render_format_title))
            .assertDoesNotExist()
    }

    @Test
    fun aPackIsListedWithItsFormatAndOnlyItsOwnRendererCanBeChosen() {
        setCard(mapPacks = listOf(ESTONIA), activeFormat = OfflineMapPackFormat.PMTILES)

        composeRule.onNodeWithText(ESTONIA.displayName).performScrollTo().assertIsDisplayed()
        // The detail line tells a user which file a pack came from.
        composeRule
            .onNodeWithText(
                string(
                    R.string.settings_offline_maps_pack_detail,
                    string(R.string.settings_offline_maps_format_pmtiles),
                    ESTONIA.originalFileName,
                    "512 B",
                ),
            )
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(string(R.string.settings_offline_maps_render_format_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(formatOption(R.string.settings_offline_maps_format_pmtiles, 1))
            .performScrollTo()
            .assertIsEnabled()
        // Switching to a renderer with no packs would blank the map, so it is not offered.
        composeRule.onNodeWithText(formatOption(R.string.settings_offline_maps_format_mapsforge, 0))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun anImportInFlightLocksTheButtonAndReportsItsProgress() {
        setCard(
            isImporting = true,
            progress = OfflineMapImportProgress(
                phase = OfflineMapImportPhase.COPYING,
                bytesCopied = 256,
                totalBytes = 512,
            ),
        )

        // Tapping import twice would copy the file over itself, so the button is out of action.
        composeRule.onNodeWithText(string(R.string.settings_offline_maps_importing))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule
            .onNodeWithText(
                string(
                    R.string.settings_offline_maps_import_progress_with_percent,
                    string(R.string.settings_offline_maps_import_progress_copying),
                    50,
                ),
            )
            .performScrollTo()
            .assertIsDisplayed()
        // A multi-gigabyte region takes minutes; the user needs to know they can leave.
        composeRule.onNodeWithText(string(R.string.settings_offline_maps_import_background))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aFinishedImportNamesWhatLandedAndGivesTheButtonBack() {
        setCard(
            mapPacks = listOf(ESTONIA),
            activeFormat = OfflineMapPackFormat.PMTILES,
            result = OfflineMapImportResult(
                mapId = ESTONIA.id,
                displayName = ESTONIA.displayName,
                sizeBytes = ESTONIA.sizeBytes,
                format = OfflineMapPackFormat.PMTILES,
            ),
        )

        composeRule
            .onNodeWithText(
                string(R.string.settings_offline_maps_import_result, ESTONIA.displayName, "512 B"),
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_offline_maps_import_action))
            .performScrollTo()
            .assertIsEnabled()
    }

    private fun formatOption(labelRes: Int, packCount: Int): String = string(
        R.string.settings_offline_maps_render_format_option,
        string(labelRes),
        packCount,
    )

    private fun setCard(
        mapPacks: List<OfflineMapPack> = emptyList(),
        activeFormat: OfflineMapPackFormat? = null,
        isImporting: Boolean = false,
        progress: OfflineMapImportProgress? = null,
        result: OfflineMapImportResult? = null,
        error: String? = null,
    ) = setContent {
        OfflineMapsCard(
            mapPacks = mapPacks,
            activeFormat = activeFormat,
            isImporting = isImporting,
            progress = progress,
            result = result,
            error = error,
            onImport = {},
            onSelectActiveFormat = {},
            onDeleteMap = {},
        )
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private companion object {
        /** A sub-kilobyte pack, so the size formatter reports plain bytes in every locale. */
        val ESTONIA = OfflineMapPack(
            id = "estonia-00000001",
            displayName = "estonia",
            originalFileName = "estonia.pmtiles",
            sizeBytes = 512L,
            importedAtMillis = 1_782_950_400_000L,
            path = "/data/offline_maps/estonia-00000001.pmtiles",
            format = OfflineMapPackFormat.PMTILES,
        )
    }
}
