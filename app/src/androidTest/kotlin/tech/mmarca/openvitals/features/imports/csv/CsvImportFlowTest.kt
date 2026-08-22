package tech.mmarca.openvitals.features.imports.csv

import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectSyncGate
import tech.mmarca.openvitals.healthconnect.MindfulnessIntegrationGate
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The mapping and confirm steps of Flutter's
 * `test/features/imports/csv/csv_import_screen_test.dart`, driven through a real
 * [CsvImportViewModel] over a real file on disk.
 *
 * The view model is constructed rather than injected: everything these steps
 * need — describing the picked document, sampling it, validating the mapping,
 * and previewing what would be written — happens before a single record is
 * handed to Health Connect. `startImport` is deliberately never called, because
 * the import service writes through the real Health Connect client and a test
 * that ran it would leave records on the device it ran on.
 */
class CsvImportFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir: File = File(context.cacheDir, "csv_import_flow_test").apply {
        deleteRecursively()
        mkdirs()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun pickingAFileListsOneRowPerColumnWithItsHeader() {
        // One editor per column, each naming the column it edits and showing
        // that column's own values. Collapsing those is how a user maps weight
        // onto the fat-mass column without noticing.
        pick(withingsFile())

        // The preview names every column of the file that was read.
        assertShown(DATE_HEADER)
        assertShown(WEIGHT_HEADER)
        assertShown(FAT_MASS_HEADER)

        // The sample line under a tile's header is that column's own values, so
        // it is the anchor that proves the tiles are three different columns
        // rather than the same one three times.
        listOf(
            "2026-07-01 08:12:00 · 2026-07-02 08:14:00",
            "78.4 · 78.6",
            "15.2 · 15.3",
        ).forEach { samples ->
            scrollTo(hasText(samples))
            composeRule.onNodeWithText(samples).assertIsDisplayed()
        }

        // Every header is now on screen twice: once in the preview, once as the
        // title of the tile that maps it.
        assertEquals(2, nodeCount(DATE_HEADER))
        assertEquals(2, nodeCount(WEIGHT_HEADER))
        assertEquals(2, nodeCount(FAT_MASS_HEADER))
    }

    @Test
    fun aFreshlyPickedFileCannotContinueUntilAMetricColumnIsChosen() {
        // The importer guesses the timestamp column and nothing else, so a file
        // that has only just been picked has nothing to write yet. Continuing
        // from here would run an import that imports nothing.
        pick(withingsFile())

        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_continue))
            .assertIsNotEnabled()
    }

    @Test
    fun bodyFatAsAMassWithNoWeightColumnSaysSoAndKeepsContinueDisabled() {
        // Body fat given in kilograms is only a percentage once it is divided by
        // that row's weight. With no weight column there is nothing to divide
        // by, and importing anyway would write a kilogram count as a percent.
        val viewModel = pick(withingsFile())

        onMain {
            viewModel.setColumnRole(
                columnIndex = FAT_MASS_COLUMN,
                role = CsvColumnRole.METRIC,
                metric = CsvImportMetric.BODY_FAT,
            )
        }
        composeRule.waitUntil(TIMEOUT_MS) {
            CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN in viewModel.uiState.value.issues
        }

        // The issues render at the end of the lazy list, under every tile.
        val needsWeight = string(R.string.settings_csv_import_issue_needs_weight)
        scrollTo(hasText(needsWeight))
        composeRule.onNodeWithText(needsWeight).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.settings_csv_import_continue))
            .assertIsNotEnabled()
    }

    @Test
    fun theConfirmStepShowsTheObservedRangeForEachMetric() {
        // The guard against a bad derivation: a fat mass divided by the wrong
        // column shows up here as 3% or 150%, before anything is written.
        val viewModel = pick(withingsFile())
        mapWeight(viewModel)
        continueToConfirm()

        composeRule
            .onNodeWithText(
                string(
                    R.string.settings_csv_import_confirm_range,
                    string(R.string.settings_csv_import_metric_weight),
                    "78.4",
                    "78.6",
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun theConfirmStepShowsTheDateSpanTheImportWillWrite() {
        val viewModel = pick(withingsFile())
        mapWeight(viewModel)
        continueToConfirm()

        composeRule.onNodeWithText(dateSpan("2026-07-01", "2026-07-02")).assertIsDisplayed()
    }

    @Test
    fun readingAnAmbiguousFileMonthFirstIsVisibleInTheDateSpan() {
        // The guard the single-row echo cannot give: `01/07` is plausible read
        // either way, but a span running January to March rather than three days
        // in July is not.
        val viewModel = pick(
            writeCsv(
                "ambiguous.csv",
                "Date,Weight\n01/07/2026,78.4\n02/07/2026,78.6\n03/07/2026,78.2\n",
            ),
        )

        onMain {
            viewModel.setColumnRole(columnIndex = 0, role = CsvColumnRole.TIMESTAMP)
            viewModel.setColumnRole(
                columnIndex = 1,
                role = CsvColumnRole.METRIC,
                metric = CsvImportMetric.WEIGHT,
            )
            viewModel.setDateTimeSettings(
                CsvDateTimeSettings(
                    format = CsvDateTimeFormat.MONTH_FIRST,
                    zone = CsvTimeZoneMode.UTC,
                ),
            )
        }
        continueToConfirm()

        composeRule.onNodeWithText(dateSpan("2026-01-07", "2026-03-07")).assertIsDisplayed()
    }

    /** Renders the importer, then picks [file] through the real view model. */
    private fun pick(file: File): CsvImportViewModel {
        val viewModel = newViewModel()
        setSteps(viewModel)
        onMain { viewModel.pickFile(Uri.fromFile(file)) }
        composeRule.waitUntil(TIMEOUT_MS) {
            val state = viewModel.uiState.value
            state.step == CsvImportStep.MAPPING && !state.isLoadingFile && state.mapping != null
        }
        composeRule.waitForIdle()
        return viewModel
    }

    /** Maps the weight column and pins how the timestamps are read. */
    private fun mapWeight(viewModel: CsvImportViewModel) {
        onMain {
            viewModel.setColumnRole(
                columnIndex = WEIGHT_COLUMN,
                role = CsvColumnRole.METRIC,
                metric = CsvImportMetric.WEIGHT,
            )
            viewModel.setDateTimeSettings(
                CsvDateTimeSettings(
                    format = CsvDateTimeFormat.YEAR_FIRST,
                    zone = CsvTimeZoneMode.UTC,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    /** Goes on to the confirm step the way a user does: through the step bar. */
    private fun continueToConfirm() {
        composeRule.onNodeWithText(string(R.string.settings_csv_import_continue)).performClick()
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule
                .onAllNodesWithText(string(R.string.settings_csv_import_confirm_title))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * The importer's own step switch, minus the Health Connect shell — which
     * asks a real Health Connect what it is allowed to do and cannot answer
     * inside a test. The pick step has its own test file.
     */
    private fun setSteps(viewModel: CsvImportViewModel) {
        composeRule.setContent {
            OpenVitalsTheme {
                val state by viewModel.uiState.collectAsState()
                when (state.step) {
                    CsvImportStep.MAPPING -> CsvMappingStep(state = state, viewModel = viewModel)
                    CsvImportStep.CONFIRM -> CsvConfirmStep(
                        state = state,
                        viewModel = viewModel,
                        onGrantPermissions = {},
                    )
                    else -> Unit
                }
            }
        }
    }

    /**
     * The date span as the screen renders it: the locale's own long date format,
     * so a `01/07/2026` read as January says the word January.
     */
    private fun dateSpan(from: String, to: String): String {
        val locale: Locale = context.resources.configuration.locales[0]
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        return string(
            R.string.settings_csv_import_confirm_dates,
            format.format(LocalDate.parse(from)),
            format.format(LocalDate.parse(to)),
        )
    }

    private fun writeCsv(name: String, content: String): File =
        File(dir, name).apply { writeText(content) }

    private fun withingsFile(): File = writeCsv(
        "withings.csv",
        "Date,\"Weight (kg)\",\"Fat mass (kg)\"\n" +
            "2026-07-01 08:12:00,78.4,15.2\n" +
            "2026-07-02 08:14:00,78.6,15.3\n",
    )

    private fun newViewModel(): CsvImportViewModel {
        val preferences = PreferencesRepository(context)
        // Constructible without Health Connect: the client is made lazily, and
        // nothing these cases do reaches it.
        val manager = HealthConnectManager(
            context = context,
            syncGate = HealthConnectSyncGate(preferences),
            mindfulnessGate = MindfulnessIntegrationGate(preferences),
        )
        return CsvImportViewModel(
            context = context,
            importService = CsvImportService(
                AppleHealthImportRepository(hc = manager, dispatchers = DefaultDispatcherProvider),
            ),
            healthRepository = NothingGrantedHealthRepository,
            healthConnectManager = manager,
        )
    }

    /** The lazy list, which is the only node here that can scroll to an index. */
    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
    }

    private fun assertShown(text: String) {
        composeRule.onAllNodesWithText(text).onFirst().assertIsDisplayed()
    }

    private fun nodeCount(text: String): Int =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    /** Runs [block] on the main thread, where a view model expects to be driven. */
    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    /**
     * Nothing granted and nothing to grant. The confirm step's permission
     * callout is not what these cases are about, and refusing to answer anything
     * else keeps the fake from quietly standing in for Health Connect.
     */
    private object NothingGrantedHealthRepository : HealthRepository {
        override fun availability() = error("unused")

        override fun rateLimitRetryAfterMillis(): Long = error("unused")

        override fun permissionContract() = error("unused")

        override val phase1Permissions: Set<String> = emptySet()
        override val minimumOnboardingPermissions: Set<String> = emptySet()
        override val phase2Permissions: Set<String> = emptySet()
        override val phase3Permissions: Set<String> = emptySet()
        override val phase4Permissions: Set<String> = emptySet()
        override val corePermissions: Set<String> = emptySet()
        override val routePermissions: Set<String> = emptySet()
        override val activityWritePermissions: Set<String> = emptySet()
        override val heartPermissions: Set<String> = emptySet()
        override val bodyPermissions: Set<String> = emptySet()
        override val bodyWritePermissions: Set<String> = emptySet()
        override val activityExtrasPermissions: Set<String> = emptySet()
        override val nutritionHydrationPermissions: Set<String> = emptySet()
        override val hydrationWritePermissions: Set<String> = emptySet()
        override val mindfulnessPermissions: Set<String> = emptySet()
        override val mindfulnessWritePermissions: Set<String> = emptySet()
        override val additionalDataAccessPermissions: Set<String> = emptySet()
        override val vitalsPermissions: Set<String> = emptySet()
        override val vitalsWritePermissions: Set<String> = emptySet()
        override val dataImportWritePermissions: Set<String> = emptySet()
        override val cyclePermissions: Set<String> = emptySet()
        override val manualOnlyPermissions: Set<String> = emptySet()
        override val requestableWritePermissions: Set<String> = emptySet()
        override val onboardingPermissions: Set<String> = emptySet()
        override val allPermissions: Set<String> = emptySet()
        override val managedPermissions: Set<String> = emptySet()

        override fun grantModeFor(permission: String) = error("unused")

        override fun onboardingPermissionCatalog() = error("unused")

        override fun isMindfulnessAvailable(): Boolean = true

        override suspend fun grantedPermissions(): Set<String> = emptySet()

        override suspend fun missingPhase1(): Set<String> = emptySet()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        const val DATE_HEADER = "Date"
        const val WEIGHT_HEADER = "Weight (kg)"
        const val FAT_MASS_HEADER = "Fat mass (kg)"

        const val WEIGHT_COLUMN = 1
        const val FAT_MASS_COLUMN = 2
    }
}
