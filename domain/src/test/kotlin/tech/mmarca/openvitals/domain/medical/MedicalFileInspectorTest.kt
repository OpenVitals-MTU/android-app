package tech.mmarca.openvitals.domain.medical

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The failure report says what a file holds by its shape, so it can be shared: no value from the file reaches it. */
class MedicalFileInspectorTest {

    private fun describe(text: String) = MedicalFileInspector.describe(text.toByteArray())

    @Test
    fun `a file is described by its kind, shape and record types, never by a value in it`() {
        val described = mapOf(
            """{"data":{"resourceType":"Patient","name":"Jane Doe"},"exportedAt":"2026-10-01"}""" to
                "a JSON object with no resourceType, with fields data, exportedAt",
            """{"resourceType":"Bundle","type":"searchset","entry":[{"resource":{"resourceType":"Patient","name":"Jane Doe"}},{"search":{}}]}""" to
                "a FHIR Bundle (searchset) of 1 record: Patient 1, and 1 entry with no record",
            "{\"resourceType\":\"Patient\",\n\"name\": Jane Doe}" to "not JSON: it stops at line 2, column 15",
            """{"resourceType":"Patient","name":"Jane Doe"}""" + "\n" + "Jane Doe" to "NDJSON whose line 2 is not a FHIR record",
            """<Patient xmlns="http://hl7.org/fhir"><name value="Jane Doe"/></Patient>""" to
                "FHIR Patient in XML, which the import does not read: it reads FHIR JSON",
        )

        described.forEach { (file, description) -> assertThat(describe(file)).isEqualTo(description) }
        assertThat(described.keys.map(::describe).joinToString()).doesNotContain("Jane")
    }

    @Test
    fun `a zip lists each of its files the same way`() {
        val zip = zipOf(
            mapOf(
                "records/Patient.json" to """{"resourceType":"Patient"}""".toByteArray(),
                "records/labs.json" to "﻿\u0000\u0001".toByteArray(Charsets.UTF_16LE),
            ),
        )

        assertThat(MedicalFileInspector.inspect { zip.inputStream() }).containsExactly(
            "A zip of 2 files",
            "  records/Patient.json: 26 bytes, a FHIR Patient",
            "  records/labs.json: 6 bytes, UTF-16 text, which the import does not read: it reads UTF-8",
        ).inOrder()
    }
}
