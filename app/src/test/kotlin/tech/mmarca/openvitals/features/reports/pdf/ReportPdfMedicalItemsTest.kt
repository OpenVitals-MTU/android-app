package tech.mmarca.openvitals.features.reports.pdf

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.ReportMedicalRow
import tech.mmarca.openvitals.domain.model.ReportMedicalSection

/** The medical section's layout: one table per category that has rows, a line for each that has none. */
class ReportPdfMedicalItemsTest {

    private val labels = ReportPdfMedicalLabels(
        title = "Medical records",
        scope = "Scope",
        categoryTitles = MedicalCategory.entries.associateWith { it.name.lowercase() },
        date = "Date",
        status = "Status",
        value = "Value",
        source = "Source",
        noneRecorded = { "$it: none recorded" },
        noneInRange = { "$it: none in this range" },
        ownOnly = { "Own only: $it" },
        leftOut = { "Left out: $it" },
        failed = { "Failed: $it" },
        disclaimer = "As received",
        statusLabel = { it.uppercase() },
        dateLabel = { "on $it" },
    )

    private fun items(section: ReportMedicalSection) = medicalItems(
        section = section,
        labels = labels,
        titleItem = LayoutItem(ReportBlock.MetricTitle(labels.title), 10f),
        notice = { LayoutItem(ReportBlock.Notice(it), 10f) },
        headerHeight = 8f,
        rowHeight = 8f,
    ).map { it.block }

    private val empty = ReportMedicalSection(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun `an empty section still says what it covers, category by category`() {
        val blocks = items(empty)

        assertThat(blocks.first()).isEqualTo(ReportBlock.MetricTitle("Medical records"))
        assertThat(blocks).contains(ReportBlock.StatusLine("allergies: none recorded"))
        assertThat(blocks).contains(ReportBlock.StatusLine("vaccines: none in this range"))
        assertThat(blocks.last()).isEqualTo(ReportBlock.StatusLine("As received"))
    }

    @Test
    fun `a record row shows its status as a word and a lab row its value with the lab's flag`() {
        val section = empty.copy(
            allergies = listOf(ReportMedicalRow("Peanut", "AllergyIntolerance", "2012-04-01", "active", "Clinic")),
            labResults = listOf(ReportMedicalRow("Glucose", "Observation", "2026-08-20", "final", "Lab", value = "7.2 mmol/L", flag = "High")),
        )

        val rows = items(section).filterIsInstance<ReportBlock.TableRow>().map { it.cells }

        assertThat(rows).containsExactly(
            listOf("Peanut", "on 2012-04-01", "ACTIVE", "Clinic"),
            listOf("Glucose", "on 2026-08-20", "7.2 mmol/L · High", "Lab"),
        )
    }

    @Test
    fun `what access kept out is named at the end`() {
        val blocks = items(empty.copy(ownOnly = setOf(MedicalCategory.ALLERGIES), leftOut = setOf(MedicalCategory.LAB_RESULTS)))

        assertThat(blocks).containsAtLeast(ReportBlock.Notice("Own only: allergies"), ReportBlock.Notice("Left out: lab_results")).inOrder()
    }
}
