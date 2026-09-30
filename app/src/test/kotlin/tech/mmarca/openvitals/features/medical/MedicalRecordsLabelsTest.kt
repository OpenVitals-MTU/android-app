package tech.mmarca.openvitals.features.medical

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.medical.FhirDate
import tech.mmarca.openvitals.domain.medical.SummaryField

class MedicalRecordsLabelsTest {

    private val formatters = DateTimeFormatterProvider()

    @Test
    fun `a code system shows its short name`() {
        assertThat(codeSystemLabel("http://snomed.info/sct")).isEqualTo("SNOMED CT")
        assertThat(codeSystemLabel("http://loinc.org/")).isEqualTo("LOINC")
        assertThat(codeSystemLabel("http://terminology.hl7.org/CodeSystem/v3-ActCode")).isEqualTo("v3-ActCode")
        assertThat(codeSystemLabel("urn:oid:2.16.840.1.113883.6.73")).isEqualTo("2.16.840.1.113883.6.73")
    }

    @Test
    fun `a full date uses the locale's format and a partial one shows as written`() {
        assertThat(FhirDate("2019-03-14").displayText(formatters.mediumDate()))
            .isEqualTo(formatters.mediumDate().format(LocalDate.of(2019, 3, 14)))
        assertThat(FhirDate("2015-06").displayText(formatters.mediumDate())).isEqualTo("2015-06")
        assertThat(dateFieldText(SummaryField.ONSET, "2005", formatters)).isEqualTo("2005")
    }

    @Test
    fun `a period's ends are formatted in local time, and other fields are left alone`() {
        val start = "2024-01-20T09:00:00Z"
        val expected = formatters.mediumDateTime().format(OffsetDateTime.parse(start).atZoneSameInstant(ZoneId.systemDefault()))

        assertThat(dateFieldText(SummaryField.PERIOD, "$start – 2024", formatters)).isEqualTo("$expected – 2024")
        assertThat(dateFieldText(SummaryField.LOT_NUMBER, "2024-01-20", formatters)).isEqualTo("2024-01-20")
    }
}
