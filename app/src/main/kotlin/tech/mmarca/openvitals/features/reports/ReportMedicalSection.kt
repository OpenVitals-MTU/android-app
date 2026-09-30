package tech.mmarca.openvitals.features.reports

import android.content.Context
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.FhirSummaries
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.ReportMedicalRow
import tech.mmarca.openvitals.domain.model.ReportMedicalSection
import tech.mmarca.openvitals.features.medical.displayText
import tech.mmarca.openvitals.features.medical.fhirStatusLabelRes
import tech.mmarca.openvitals.features.medical.medicalRecordRows
import tech.mmarca.openvitals.features.medical.readOrNothing
import tech.mmarca.openvitals.features.medical.sourceNames
import tech.mmarca.openvitals.features.medical.titleRes
import tech.mmarca.openvitals.features.reports.pdf.ReportPdfMedicalLabels

/** The categories the report carries: the ones a doctor asks about first. */
internal val ReportMedicalCategories = listOf(
    MedicalCategory.ALLERGIES,
    MedicalCategory.CONDITIONS,
    MedicalCategory.MEDICATIONS,
    MedicalCategory.VACCINES,
    MedicalCategory.LAB_RESULTS,
)

/**
 * Reads the report's medical section. It asks for no permission: it uses what the medical
 * area was granted, and the section says what the lack of access kept out.
 */
class ReportMedicalLoader @Inject constructor(
    private val repository: MedicalRecordsRepository,
) {
    fun isAvailable(): Boolean = repository.isAvailable()

    suspend fun load(start: LocalDate, end: LocalDate): ReportMedicalSection {
        val readable = readOrNothing { repository.readableCategories() }.toSet()
        val sources = sourceNames(readOrNothing { repository.allSources() } + readOrNothing { repository.ownSources() })
        val ownOnly = mutableSetOf<MedicalCategory>()
        val leftOut = mutableSetOf<MedicalCategory>()
        val failed = mutableSetOf<MedicalCategory>()
        val records = ReportMedicalCategories.associateWith { category ->
            try {
                repository.readCategory(category).also { if (category !in readable) ownOnly += category }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                if (failure.isPermissionFailure()) leftOut += category else failed += category
                emptyList()
            }
        }
        return medicalReportSection(records, sources, start, end).copy(ownOnly = ownOnly, leftOut = leftOut, failed = failed)
    }
}

/**
 * The rows for each category, newest first, titled as the records screens title them.
 * Vaccines and lab results keep only those dated in [start]..[end]; an undated one cannot be placed.
 */
internal fun medicalReportSection(
    records: Map<MedicalCategory, List<MedicalRecord>>,
    sourceNames: Map<String, String>,
    start: LocalDate,
    end: LocalDate,
): ReportMedicalSection {
    fun rows(category: MedicalCategory, inRange: Boolean): List<ReportMedicalRow> {
        val list = records[category].orEmpty()
        val summaries = list.associate { it.ref to FhirSummaries.summarize(it.json) }
        return medicalRecordRows(list, sourceNames)
            .filter { row -> !inRange || row.date?.localDate()?.let { !it.isBefore(start) && !it.isAfter(end) } == true }
            .map { row ->
                val summary = summaries[row.ref]
                ReportMedicalRow(
                    title = row.title,
                    resourceType = row.ref.resourceType,
                    date = row.date?.text,
                    status = row.status,
                    source = row.sourceName,
                    value = summary?.value,
                    flag = summary?.flag,
                )
            }
    }
    return ReportMedicalSection(
        vaccines = rows(MedicalCategory.VACCINES, inRange = true),
        allergies = rows(MedicalCategory.ALLERGIES, inRange = false),
        medications = rows(MedicalCategory.MEDICATIONS, inRange = false),
        conditions = rows(MedicalCategory.CONDITIONS, inRange = false),
        labResults = rows(MedicalCategory.LAB_RESULTS, inRange = true),
    )
}

/** The day a FHIR date names. A year alone counts as its first day, as the list sorts it. */
private fun FhirDate.localDate(): LocalDate? = runCatching { LocalDate.parse(sortKey.take(10)) }.getOrNull()

internal fun medicalLabels(context: Context): ReportPdfMedicalLabels {
    val dates = DateTimeFormatterProvider().mediumDate()
    return ReportPdfMedicalLabels(
        title = context.getString(R.string.medical_records_title),
        scope = context.getString(R.string.medical_report_scope),
        categoryTitles = ReportMedicalCategories.associateWith { context.getString(it.titleRes) },
        date = context.getString(R.string.medical_record_date),
        status = context.getString(R.string.medical_record_status),
        value = context.getString(R.string.medical_record_value),
        source = context.getString(R.string.medical_record_source),
        noneRecorded = { context.getString(R.string.medical_report_none_recorded, it) },
        noneInRange = { context.getString(R.string.medical_report_none_in_range, it) },
        ownOnly = { context.getString(R.string.medical_export_own_only, it) },
        leftOut = { context.getString(R.string.medical_export_left_out, it) },
        failed = { context.getString(R.string.medical_report_failed, it) },
        disclaimer = context.getString(R.string.medical_report_disclaimer),
        statusLabel = { code -> fhirStatusLabelRes(code)?.let(context::getString) ?: code },
        dateLabel = { text -> FhirDate(text).displayText(dates) },
    )
}
