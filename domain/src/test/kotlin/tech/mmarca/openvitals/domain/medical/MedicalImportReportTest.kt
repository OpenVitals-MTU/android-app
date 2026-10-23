package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/**
 * Both reports are meant to be shared: counts, record types and reasons only.
 * No record's content or id, no file, source or app name, no address.
 */
class MedicalImportReportTest {

    private val recordId = "mrn-${PrivacySentinels.Uuid}"
    private val heldBack = FhirRejectedResource(
        PreparedResource(
            "Observation",
            recordId,
            FhirTestFiles.json("""{"resourceType":"Observation","id":"$recordId","valueString":"${PrivacySentinels.Sentence}"}"""),
        ),
        FhirPreflightProblem(FhirRejection.UNSUPPORTED_TYPE, field = "code"),
    )
    private val group = FhirImportGroup(
        baseUri = PrivacySentinels.Uri,
        suggestedName = PrivacySentinels.Name,
        fhirVersion = "4.0.1",
        ready = emptyList(),
        rejected = listOf(heldBack, heldBack),
        skippedTypes = mapOf("Basic" to 2),
        notes = listOf(FhirIdNote.IdAssigned("Observation", previousId = PrivacySentinels.Date, id = recordId)),
    )
    private val clinic = MedicalRecordSource("s1", "com.example.${PrivacySentinels.Name.replace(" ", "")}", PrivacySentinels.Uri, "${PrivacySentinels.Name}'s clinic", "4.0.1", null)
    private val plan = MedicalImportGroupPlan(group, MedicalImportTarget.Existing(clinic), "4.0.1")
    private val skips = listOf(
        MedicalSourceSkip(MedicalSourceSkipReason.FILE_MISSING, PrivacySentinels.Path),
        MedicalSourceSkip(MedicalSourceSkipReason.FILE_MISSING, "clinical-records/${PrivacySentinels.Name}.json"),
    )

    @Test
    fun `an import's report counts what was written and refused, and holds none of it`() {
        val refusal = IllegalArgumentException("java.lang.IllegalArgumentException: value ${PrivacySentinels.Value} at ${PrivacySentinels.Instant}")
        val result = MedicalImportResult(
            groups = listOf(
                MedicalImportGroupResult(
                    plan = plan,
                    sourceName = "${PrivacySentinels.Name}'s clinic",
                    written = 3,
                    updated = 1,
                    rejected = listOf(MedicalWriteRejection("Observation", recordId, refusal.platformReason())),
                ),
            ),
            excluded = listOf(plan.copy(otherApp = clinic, include = false)),
            stoppedBy = refusal.platformReason(),
            sourceSkips = skips,
        )

        val report = MedicalImportReport.text(result, Instant.parse("2026-10-23T12:00:00Z"))

        PrivacySentinels.assertNoneIn(report)
        assertThat(report).doesNotContain(recordId)
        assertThat(report).contains("Source 1: an existing source, FHIR 4.0.1")
        assertThat(report).contains("Held back: Observation x2: UNSUPPORTED_TYPE (code)")
        assertThat(report).contains("Refused by Health Connect: Observation x1: value # at [redacted]")
        assertThat(report).contains("Left out before import: FILE_MISSING x2")
        assertThat(report).contains("Id assigned: Observation x1")
        assertThat(report).contains("Source 2: left out, 0 records already held by another app")
    }

    @Test
    fun `a failure's report says what stopped it, and holds none of the file`() {
        val report = MedicalImportReport.failureText(
            problem = "NOTHING_TO_WRITE",
            readAt = Instant.parse("2026-10-23T12:00:00Z"),
            contents = listOf("A zip of 1 file", "  file 1 (.json): 57 bytes, a FHIR Observation"),
            cause = IllegalStateException(PrivacySentinels.Sentence),
            plans = listOf(plan),
            sourceSkips = skips,
        )

        PrivacySentinels.assertNoneIn(report)
        assertThat(report).doesNotContain(recordId)
        assertThat(report).contains("Error: java.lang.IllegalStateException")
        assertThat(report).contains("Ready 0, held back 2")
    }
}
