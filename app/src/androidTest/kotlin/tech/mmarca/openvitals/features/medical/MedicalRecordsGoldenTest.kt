package tech.mmarca.openvitals.features.medical

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.SummaryField
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.features.dashboard.DashboardPillWidget
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.MedicalRecordsColor

/**
 * The medical records area: the home with its counts and re-ask, one record as received, and
 * the static tile. Declined categories must say so in words, not only by a missing count.
 */
class MedicalRecordsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theHomeWithCountsADeclinedCategoryAndTheReAsk() {
        // Tall enough for the sensitive block, where the declined category says so in words.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(height = 1500.dp) {
                MedicalRecordsHomeContent(
                    state = HOME,
                    access = MedicalAccessAction.Ask(setOf("android.permission.health.READ_MEDICAL_DATA_PREGNANCY")),
                    actions = MedicalHomeActions(),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("medical_records_home")
    }

    @Test
    fun theHomeWhenHealthConnectNoLongerAsks() {
        // Refused twice: the only way on is Health Connect's settings, and the callout says why.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(height = 400.dp) {
                MedicalRecordsHomeContent(state = HOME, access = MedicalAccessAction.OpenSettings, actions = MedicalHomeActions())
            }
        }

        composeRule.assertVisualRootMatchesGolden("medical_records_home_blocked")
    }

    @Test
    fun aVaccineAsReceived() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface {
                MedicalRecordDetailContent(state = VACCINE, dateTimeFormatterProvider = DateTimeFormatterProvider { Locale.US })
            }
        }

        composeRule.assertVisualRootMatchesGolden("medical_record_detail_vaccine")
    }

    @Test
    fun theStaticTile() {
        // No value and no reads: the glyph, the title and "Tap to browse".
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 196.dp, height = 132.dp) {
                DashboardPillWidget(
                    title = string(R.string.medical_records_title),
                    value = DisplayValue("", ""),
                    icon = Icons.Outlined.MedicalInformation,
                    accentColor = MedicalRecordsColor,
                    message = string(R.string.medical_records_tile_browse),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("medical_records_tile")
    }

    private companion object {
        val HOME = MedicalRecordsHomeUiState(
            isLoading = false,
            rows = MedicalCategory.entries.map { category ->
                when (category) {
                    MedicalCategory.VACCINES -> MedicalCategoryRow(category, 3, readable = true, noAccess = false)
                    MedicalCategory.ALLERGIES -> MedicalCategoryRow(category, 1, readable = true, noAccess = false)
                    MedicalCategory.LAB_RESULTS -> MedicalCategoryRow(category, 12, readable = true, noAccess = false)
                    // Declined, but write access still lists this app's own records.
                    MedicalCategory.PREGNANCY -> MedicalCategoryRow(category, 0, readable = false, noAccess = false)
                    else -> MedicalCategoryRow(category, 0, readable = true, noAccess = false)
                }
            },
            ownSourceCount = 2,
        )

        val VACCINE = MedicalRecordDetailUiState(
            ref = MedicalRecordRef("clinic", "Immunization", "imm-1"),
            isLoading = false,
            resourceType = "Immunization",
            title = MedicalValue("Influenza, seasonal", caption = "http://hl7.org/fhir/sid/cvx"),
            status = "completed",
            date = FhirDate("2023-10-02"),
            details = listOf(
                MedicalDetailRow(SummaryField.LOT_NUMBER, MedicalValue("AAJN11K")),
                MedicalDetailRow(SummaryField.SITE, MedicalValue("Left arm")),
                MedicalDetailRow(SummaryField.PERFORMER, MedicalValue("Dr Sam Rivera")),
                MedicalDetailRow(SummaryField.DOSE_NUMBER, MedicalValue("1")),
            ),
            sourceName = "City Clinic",
            sourceUri = "https://fhir.clinic.example/r4",
            writtenByThisApp = true,
            rawJson = """{"resourceType":"Immunization","id":"imm-1"}""",
        )
    }
}
