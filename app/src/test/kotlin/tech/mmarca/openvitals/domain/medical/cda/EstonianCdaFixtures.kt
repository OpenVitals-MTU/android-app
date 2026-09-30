package tech.mmarca.openvitals.domain.medical.cda

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Invented Estonian CDA documents in the shape of a real portal export. Every person, code,
 * value and date here is made up; Mari Maasikas is the Estonian placeholder name.
 */
object EstonianCdaFixtures {

    fun document(
        typeCode: String,
        typeName: String,
        sections: String,
        series: String = "900001",
        version: Int = 1,
        clinicCode: String = "90000001",
        clinicName: String = "Näidiskliinik OÜ",
    ): String = """<?xml version="1.0" encoding="UTF-8"?>
<ClinicalDocument xmlns="urn:hl7-org:v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="urn:hl7-EE-DL-Ext:v1">
  <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
  <templateId root="1.3.6.1.4.1.28284.6.1.1" extension="1.3.6.1.4.1.28284.6.1.1.5.9"/>
  <id root="1.3.6.1.4.1.28284.1.9999.2.2.3" extension="$series.$version"/>
  <code code="$typeCode" codeSystem="1.3.6.1.4.1.28284.6.2.1.3.8" codeSystemName="Dokumendi tüüp" displayName="$typeName"/>
  <title>$typeName</title>
  <effectiveTime value="20240315103000"/>
  <setId root="1.3.6.1.4.1.28284.1.9999.2.1" extension="$series"/>
  <versionNumber value="$version"/>
  <recordTarget typeCode="RCT">
    <patientRole classCode="PAT">
      <id root="1.3.6.1.4.1.28284.6.2.2.1" extension="49001019999"/>
      <addr use="PHYS"><city>Näidislinn</city><country>EST</country></addr>
      <telecom value="tel:+37200000000" use="MC"/>
      <patient classCode="PSN" determinerCode="INSTANCE">
        <name><given>Mari</given><family>Maasikas</family></name>
        <administrativeGenderCode code="N" codeSystem="1.3.6.1.4.1.28284.6.2.3.16.2" displayName="naine"/>
        <birthTime value="19900101"/>
      </patient>
    </patientRole>
  </recordTarget>
  <author typeCode="AUT">
    <time value="20240315103000"/>
    <assignedAuthor classCode="ASSIGNED">
      <id root="1.3.6.1.4.1.28284.6.2.4.9" extension="D99999"/>
      <code code="DOCTOR" codeSystem="1.3.6.1.4.1.28284.6.2.2.15.1" displayName="arst"/>
      <assignedPerson classCode="PSN" determinerCode="INSTANCE"><name><given>Test</given><family>Arst</family></name></assignedPerson>
      <representedOrganization classCode="ORG" determinerCode="INSTANCE">
        <id root="1.3.6.1.4.1.28284.4" extension="$clinicCode"/>
        <name>$clinicName</name>
      </representedOrganization>
    </assignedAuthor>
  </author>
  <custodian typeCode="CST">
    <assignedCustodian>
      <representedCustodianOrganization>
        <id root="1.3.6.1.4.1.28284.4" extension="$clinicCode"/>
        <name>$clinicName</name>
      </representedCustodianOrganization>
    </assignedCustodian>
  </custodian>
  <component typeCode="COMP">
    <structuredBody classCode="DOCBODY" moodCode="EVN">
$sections
    </structuredBody>
  </component>
</ClinicalDocument>
"""

    fun section(code: String, entries: String, subsections: String = ""): String = """
      <component typeCode="COMP">
        <section classCode="DOCSECT" moodCode="EVN">
          <code code="$code" codeSystem="1.3.6.1.4.1.28284.6.2.2.11.9" codeSystemName="Sektsiooni kodeering"/>
          <title>$code</title>
          <text><paragraph>Kirjeldus</paragraph></text>
$entries
$subsections
        </section>
      </component>"""

    private const val Visit = """
          <entry typeCode="COMP"><encounter classCode="ENC" moodCode="EVN">
            <code code="AMB" codeSystem="1.3.6.1.4.1.28284.6.2.2.3.1" displayName="ambulatoorne haigusjuht">
              <qualifier><value xsi:type="CD" code="1" codeSystem="1.3.6.1.4.1.28284.6.2.1.32.1" displayName="visiit"/></qualifier>
            </code>
            <effectiveTime value="20240315103000"/>
          </encounter></entry>"""

    private const val Diagnoses = """
          <entry typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
            <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1" displayName="Diagnoos"/>
            <statusCode code="completed"/>
            <value xsi:type="CD" code="J06.9" codeSystem="1.3.6.1.4.1.28284.6.2.1.13.6" codeSystemName="RHK-10" displayName="Äge ülemiste hingamisteede infektsioon">
              <originalText>Äge ülemiste hingamisteede infektsioon</originalText>
              <qualifier><value code="1" codeSystem="1.3.6.1.4.1.28284.6.2.1.1.2" displayName="esmajuhtum"/></qualifier>
            </value>
            <interpretationCode code="MAIN" codeSystem="1.3.6.1.4.1.28284.6.2.1.2.1" displayName="Põhihaigus"/>
          </observation></entry>
          <entry typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
            <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1" displayName="Diagnoos"/>
            <effectiveTime value="20240314"/>
            <value xsi:type="CD" code="R05" codeSystem="1.3.6.1.4.1.28284.6.2.1.13.6" displayName="Köha">
              <qualifier><value code="3" codeSystem="1.3.6.1.4.1.28284.6.2.1.1.2" displayName="esialgne"/></qualifier>
            </value>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.12.4"/>
              <text>Köha kestnud nädal.</text>
            </observation></entryRelationship>
          </observation></entry>"""

    private const val Drug = """
          <entry typeCode="COMP"><substanceAdministration classCode="SBADM" moodCode="EVN">
            <id root="1.3.6.1.4.1.28284.6.2.4.4" extension="1234567890"/>
            <code code="PRE" codeSystem="1.3.6.1.4.1.28284.6.2.2.25.1" displayName="Retsepti andmed"/>
            <doseQuantity value="1" unit="TA"/>
            <rateQuantity value="3" unit="PV"/>
            <administrationUnitCode code="738" codeSystem="1.3.6.1.4.1.28284.6.2.1.12.9" displayName="tablett"/>
            <consumable typeCode="CSM"><manufacturedProduct classCode="MANU"><manufacturedMaterial classCode="MMAT" determinerCode="KIND">
              <code code="M01AE01" codeSystem="2.16.840.1.113883.6.73" codeSystemName="ATC" displayName="Ibuprofeen"/>
            </manufacturedMaterial></manufacturedProduct></consumable>
          </substanceAdministration></entry>"""

    private const val Labs = """
          <entry typeCode="COMP"><procedure classCode="PROC" moodCode="EVN">
            <code code="58410-2" codeSystem="2.16.840.1.113883.6.1" codeSystemName="LOINC" displayName="Hemogramm"/>
            <statusCode code="completed"/>
            <effectiveTime value="20240315120000"/>
            <ns1:specimen><ns1:productOf><ns1:process moodCode="EVN"><ns1:effectiveTime value="20240315090000"/></ns1:process></ns1:productOf></ns1:specimen>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="718-7" codeSystem="2.16.840.1.113883.6.1" displayName="Hemoglobiin"/>
              <entryRelationship typeCode="REFR"><observation classCode="OBS" moodCode="EVN">
                <code code="ANA" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1"/>
                <value xsi:type="PQ" value="142" unit="g/l"/>
              </observation></entryRelationship>
              <referenceRange typeCode="REFV"><observationRange classCode="OBS" moodCode="EVN.CRT">
                <value xsi:type="IVL_PQ" unit="g/l"><low value="117"/><high value="153"/></value>
              </observationRange></referenceRange>
            </observation></entryRelationship>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="6690-2" codeSystem="2.16.840.1.113883.6.1" displayName="Leukotsüüdid"/>
              <value xsi:type="ns3:PQ" xmlns:ns3="urn:hl7-org:v3" value="11,4" unit="E9/L"/>
              <interpretationCode code="H"/>
              <referenceRange typeCode="REFV"><observationRange classCode="OBS" moodCode="EVN.CRT"><text>3,5 .. 8,8</text></observationRange></referenceRange>
            </observation></entryRelationship>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="1988-5" codeSystem="2.16.840.1.113883.6.1" displayName="CRP"/>
              <value xsi:type="IVL_PQ" unit="mg/l"><high value="5" inclusive="false"/></value>
            </observation></entryRelationship>
            <entryRelationship typeCode="COMP"><procedure classCode="PROC" moodCode="EVN">
              <code code="24357-6" codeSystem="2.16.840.1.113883.6.1" displayName="Uriini analüüs"/>
              <entryRelationship typeCode="REFR"><observation classCode="OBS" moodCode="EVN">
                <code code="5811-5" codeSystem="2.16.840.1.113883.6.1" displayName="Erikaal"/>
                <value xsi:type="ED">1.015</value>
              </observation></entryRelationship>
            </procedure></entryRelationship>
          </procedure></entry>
          <entry typeCode="COMP"><procedure classCode="PROC" moodCode="EVN">
            <code code="94500-6" codeSystem="2.16.840.1.113883.6.1" displayName="SARS-CoV-2 RNA"/>
            <effectiveTime value="20240315"/>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="ANA" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1"/>
              <entryRelationship typeCode="REFR"><observation classCode="OBS" moodCode="EVN">
                <code code="ANA" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1"/>
                <value xsi:type="CD" code="N" codeSystem="1.3.6.1.4.1.28284.6.2.1.266.4" displayName="negatiivne"/>
              </observation></entryRelationship>
            </observation></entryRelationship>
          </procedure></entry>"""

    private const val Study = """
          <entry typeCode="COMP"><procedure classCode="PROC" moodCode="EVN">
            <code code="7968" codeSystem="1.3.6.1.4.1.28284.6.2.1.6.1" codeSystemName="Haigekassa hinnakiri" displayName="Kopsude röntgen (üks&#160;ülesvõte)"/>
            <text>Kopsuväljad puhtad.</text>
            <effectiveTime value="20240315110000"/>
          </procedure></entry>
          <entry typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
            <code code="X7968" codeSystem="1.3.6.1.4.1.28284.1.1774.2.14" displayName="Kopsude röntgen">
              <translation code="7968" codeSystem="1.3.6.1.4.1.28284.6.2.1.6" displayName="Kopsude röntgen"/>
            </code>
            <text>Järeldus: norm.</text>
          </observation></entry>"""

    val epicrisis: String = document(
        "2",
        "Ambulatoorne epikriis",
        section("AMBS", Visit) + section("DGN", Diagnoses) + section("DRUG", Drug) + section("ANA", Labs) +
            section("PROC", Study) + section("ANAM", ""),
    )

    val immunisation: String = document(
        "84",
        "Immuniseerimise teatis",
        section(
            "IMM",
            """
          <entry typeCode="COMP"><procedure classCode="PROC" moodCode="EVN">
            <code code="IMM" codeSystem="1.3.6.1.4.1.28284.6.2.2.10.1" displayName="Immuniseerimine"/>
            <statusCode code="completed"/>
            <effectiveTime value="20210601"/>
            <methodCode code="101" codeSystem="1.3.6.1.4.1.28284.6.2.1.278.4" displayName="COVID-19"/>
            <entryRelationship typeCode="REFR">
              <sequenceNumber value="2"/>
              <substanceAdministration classCode="SBADM" moodCode="EVN">
                <doseQuantity value="0.3" unit="ml"/>
                <consumable typeCode="CSM"><manufacturedProduct classCode="MANU"><manufacturedMaterial classCode="MMAT" determinerCode="KIND">
                  <code code="J07BX03" codeSystem="2.16.840.1.113883.6.73" codeSystemName="ATC kood" displayName="COVID-19 vaktsiin"/>
                  <name>Näidisvaktsiin</name>
                  <lotNumberText>AB1234</lotNumberText>
                </manufacturedMaterial></manufacturedProduct></consumable>
              </substanceAdministration>
            </entryRelationship>
            <entryRelationship typeCode="REFR"><procedure classCode="PROC" moodCode="INT">
              <code code="999999999" codeSystem="2.16.840.1.113883.6.96" displayName="Järgmine doos"/>
            </procedure></entryRelationship>
          </procedure></entry>""",
        ),
        series = "900002",
    )

    val dental: String = document(
        "34",
        "Hambaravikaart",
        section(
            "DENTDISE",
            """
          <entry><encounter classCode="ENC" moodCode="EVN">
            <code code="DENT" codeSystem="1.3.6.1.4.1.28284.6.2.2.3.3" displayName="Hambaravi ravijuhtum"/>
            <effectiveTime value="20230910"/>
            <entryRelationship typeCode="CAUS"><observation moodCode="EVN" classCode="OBS">
              <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1" displayName="Diagnoos"/>
              <value xsi:type="CD" code="K02.1" codeSystem="1.3.6.1.4.1.28284.6.2.1.13.5" codeSystemName="RHK-10" displayName="Dentiini kaaries"/>
              <targetSiteCode code="16" codeSystem="1.3.6.1.4.1.28284.6.2.1.92.1" codeSystemName="Hambavalem"/>
            </observation></entryRelationship>
            <entryRelationship typeCode="COMP"><procedure moodCode="EVN" classCode="PROC">
              <code code="7010" codeSystem="1.3.6.1.4.1.28284.6.2.1.6.7" codeSystemVersion="Versioon 7" displayName="Täidis"/>
              <targetSiteCode code="16" codeSystem="1.3.6.1.4.1.28284.6.2.1.92.1"/>
            </procedure></entryRelationship>
          </encounter></entry>""",
        ),
        series = "900003",
        clinicCode = "90000002",
        clinicName = "Näidishambaravi OÜ",
    )

    fun referralResponse(version: Int = 1, finding: String = "Leid: kopsud puhtad."): String = document(
        "64",
        "Saatekirja vastus",
        section(
            "RG_PROC",
            """
          <entry typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
            <code code="7968" codeSystem="1.3.6.1.4.1.28284.6.2.1.6.49" displayName="Kopsude röntgen"/>
            <effectiveTime value="20240401100000"/>
            <methodCode code="RTG" codeSystem="1.3.6.1.4.1.28284.6.2.1.298.2" displayName="Röntgenuuring"/>
            <targetSiteCode code="12345678" codeSystem="1.3.6.1.4.1.28284.6.2.1.299.4" displayName="Rindkere"/>
            <entryRelationship typeCode="GEVL"><observation classCode="OBS" moodCode="EVN">
              <code code="PROC" codeSystem="1.3.6.1.4.1.28284.6.2.2.12.4"/>
              <text>$finding</text>
            </observation></entryRelationship>
          </observation></entry>""",
        ) + section(
            "PAT_PROC",
            "",
            subsections = section(
                "PAT_DGN",
                """
          <entry typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
            <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1"/>
            <text>Koetükk</text>
            <entryRelationship typeCode="COMP"><observation classCode="OBS" moodCode="EVN">
              <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1"/>
              <value code="87200" codeSystem="1.3.6.1.4.1.28284.6.2.1.275.8" codeSystemName="Patomorfoloogiline lõppdiagnoos" displayName="Healoomuline nevus"/>
              <targetSiteCode code="23456789" codeSystem="1.3.6.1.4.1.28284.6.2.1.274.9" displayName="Nahk"/>
            </observation></entryRelationship>
          </observation></entry>""",
            ),
        ),
        series = "900004",
        version = version,
    )

    val infectionNotice: String = document(
        "38",
        "Nakkushaige teatis",
        """
      <component typeCode="COMP"><section classCode="DOCSECT" moodCode="EVN">
        <code code="64572001" codeSystem="2.16.840.1.113883.6.96" codeSystemName="SNOMED CT" displayName="Diagnoos"/>
        <entry><observation classCode="OBS" moodCode="EVN">
          <code code="DGN" codeSystem="1.3.6.1.4.1.28284.6.2.2.5.1" displayName="Diagnoos"/>
          <effectiveTime value="20220705"/>
          <value xsi:type="CD" code="A69.2" codeSystem="1.3.6.1.4.1.28284.6.2.1.117.8" displayName="Lyme'i tõbi"/>
        </observation></entry>
        <entry><act classCode="INC" moodCode="EVN">
          <code code="12345678" codeSystem="2.16.840.1.113883.6.96" displayName="Puugihammustus"/>
          <subject typeCode="SBJ"><relatedSubject><addr><city>Näidisküla</city></addr></relatedSubject></subject>
        </act></entry>
      </section></component>""",
        series = "900005",
    )

    val referral: String = document(
        "63.5",
        "Saatekiri uuringule",
        section("DGN", Diagnoses),
        series = "900006",
    )

    val declaration: String = document("18.3", "Tahteavaldus", section("WILL", ""), series = "900007")

    /** A one-page PDF that shows [label], so a viewer can open it. The importer never reads its content. */
    fun pdf(label: String): ByteArray {
        val stream = "BT /F1 18 Tf 72 720 Td ($label) Tj ET"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length ${stream.length} >>\nstream\n$stream\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        )
        val out = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { index, body -> out.length.also { out.append("${index + 1} 0 obj\n$body\nendobj\n") } }
        val xref = out.length
        out.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { out.append("%010d 00000 n \n".format(it)) }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toString().toByteArray(Charsets.ISO_8859_1)
    }

    /** A portal zip: each document as `xml/<name>.xml` with `pdf/<name>.pdf` beside it. */
    fun zip(documents: Map<String, String>, withPdfs: Boolean = true): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            documents.forEach { (name, xml) ->
                zip.putNextEntry(ZipEntry("xml/$name.xml"))
                zip.write(xml.toByteArray())
                zip.closeEntry()
                if (withPdfs) {
                    zip.putNextEntry(ZipEntry("pdf/$name.pdf"))
                    zip.write(pdf(name))
                    zip.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }

    val all: Map<String, String>
        get() = mapOf(
            "epikriis" to epicrisis,
            "immuniseerimine" to immunisation,
            "hambaravi" to dental,
            "saatekirja-vastus" to referralResponse(),
            "nakkushaigus" to infectionNotice,
            "saatekiri" to referral,
            "tahteavaldus" to declaration,
        )
}
