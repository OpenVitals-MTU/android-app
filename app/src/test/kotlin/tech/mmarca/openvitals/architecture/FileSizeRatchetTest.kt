package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A ratchet on file length: no new file passes [MaxLines], and a listed file may shrink, never grow.
 *
 * The largest files are also the ones changed most often, so each change there costs more to review.
 * When this fails on a new file, split the file. When it fails on a listed file, move the new code
 * out. When you shrink a listed file, lower its ceiling or remove it in the same commit.
 */
class FileSizeRatchetTest {

    private val lineCounts: Map<String, Int> by lazy {
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { it.relativeTo(File(SourceRoot)).invariantSeparatorsPath to it.readLines().size }
    }

    @Test
    fun `no new file passes the limit`() {
        val unlisted = lineCounts.filter { (path, lines) -> lines > MaxLines && path !in Ceilings }

        assertWithMessage("files over $MaxLines lines that are not listed. Split them: $unlisted")
            .that(unlisted)
            .isEmpty()
    }

    @Test
    fun `a listed file does not grow past its ceiling`() {
        val grown = Ceilings.filter { (path, ceiling) -> (lineCounts[path] ?: 0) > ceiling }
            .mapValues { (path, ceiling) -> "${lineCounts[path]} lines, ceiling $ceiling" }

        assertWithMessage("listed files grew past their ceiling. Move the new code out: $grown")
            .that(grown)
            .isEmpty()
    }

    @Test
    fun `the list holds only files that still need it`() {
        // A stale entry would let a file grow back unseen.
        val stale = Ceilings.keys.filter { path -> (lineCounts[path] ?: 0) <= MaxLines }

        assertWithMessage("listed files are gone or under $MaxLines lines. Remove them from the list: $stale")
            .that(stale)
            .isEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/openvitals"
        const val MaxLines = 800

        /** The files over the limit on 2026-09-21. Each ceiling leaves 25 to 75 lines for small fixes. */
        val Ceilings: Map<String, Int> = mapOf(
            "features/settings/SettingsCards.kt" to 3050,
            "features/manualentry/hydration/HydrationEntryFormContent.kt" to 2200,
            "features/manualentry/activity/recording/ActivityRecording.kt" to 1850,
            "healthconnect/ActivityHealthReader.kt" to 1600,
            "domain/insights/CaffeineHealthDrinkCatalog.kt" to 1750,
            "features/caffeine/CaffeineScreen.kt" to 1600,
            "features/manualentry/activity/ActivityEntryViewModel.kt" to 1600,
            "features/imports/applehealth/AppleHealthImportService.kt" to 1500,
            "features/heart/HeartMetricSharedSections.kt" to 1450,
            "data/repository/PreferencesRepository.kt" to 1450,
            "features/body/BodyMetricContent.kt" to 1300,
            "devices/garmin/wellness/GarminFitWellness.kt" to 1250,
            "features/activity/ActivitiesOverviewSections.kt" to 1200,
            "features/devicesync/store/SyncRecordCodec.kt" to 1150,
            "features/watches/WatchDeviceScreen.kt" to 1100,
            "devices/garmin/GarminMessages.kt" to 1100,
            "data/repository/dashboard/DashboardDataLoader.kt" to 1050,
            "navigation/AppNavigation.kt" to 1050,
            "domain/insights/BodyEnergyTimeline.kt" to 1050,
            "ui/charts/MetricLineChart.kt" to 1000,
            "features/reports/pdf/ReportPdfWriter.kt" to 1000,
            "features/manualentry/activity/recording/ActivityRecordingDashboard.kt" to 1000,
            "features/body/BodyMetricSharedSections.kt" to 950,
            "devices/garmin/GarminSession.kt" to 950,
            "features/homewidgets/HomeReadinessWidgets.kt" to 950,
            "features/activity/maps/OfflineRouteMap.kt" to 900,
            "features/vitals/HeartVitalsOverviewScreen.kt" to 850,
        )
    }
}
