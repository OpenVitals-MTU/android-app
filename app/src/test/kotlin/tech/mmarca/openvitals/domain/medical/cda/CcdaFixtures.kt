package tech.mmarca.openvitals.domain.medical.cda

/** An invented C-CDA document in the shape a patient portal hands out. Every person, code and value is made up. */
object CcdaFixtures {

    fun document(sections: String, custodian: Boolean = true, ids: Boolean = true): String = """<?xml version="1.0" encoding="UTF-8"?>
<ClinicalDocument xmlns="urn:hl7-org:v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:sdtc="urn:hl7-org:sdtc">
  <realmCode code="US"/>
  <templateId root="2.16.840.1.113883.10.20.22.1.1"/>
  ${if (ids) """<id root="2.16.840.1.113883.19.5.99999.1" extension="DOC-0001"/>""" else ""}
  <code code="34133-9" codeSystem="2.16.840.1.113883.6.1" displayName="Summarization of Episode Note"/>
  <title>Health Summary</title>
  <effectiveTime value="20240315103000-0500"/>
  <recordTarget>
    <patientRole>
      <id root="2.16.840.1.113883.4.1" extension="999-99-9999"/>
      <addr><streetAddressLine>1 Example Street</streetAddressLine><city>Sampletown</city></addr>
      <telecom value="tel:+1-555-0100"/>
      <patient>
        <name><given>Mari</given><family>Maasikas</family></name>
        <administrativeGenderCode code="F" codeSystem="2.16.840.1.113883.5.1"/>
        <birthTime value="19900101"/>
      </patient>
    </patientRole>
  </recordTarget>
  <author>
    <time value="20240315103000-0500"/>
    <assignedAuthor>
      <id root="2.16.840.1.113883.4.6" extension="1234567893"/>
      <assignedPerson><name><given>Sample</given><family>Doctor</family></name></assignedPerson>
    </assignedAuthor>
  </author>
  ${if (custodian) """<custodian><assignedCustodian><representedCustodianOrganization>
    <id root="2.16.840.1.113883.4.6" extension="9999999999"/>
    <name>Sample Health Clinic</name>
  </representedCustodianOrganization></assignedCustodian></custodian>""" else ""}
  <component>
    <structuredBody>
$sections
    </structuredBody>
  </component>
</ClinicalDocument>
"""

    fun section(loinc: String, entries: String, narrative: String = ""): String = """
      <component><section>
        <code code="$loinc" codeSystem="2.16.840.1.113883.6.1"/>
        <title>Section $loinc</title>
        <text>$narrative</text>
$entries
      </section></component>"""

    private val Problems = section(
        "11450-4",
        """
        <entry><act classCode="ACT" moodCode="EVN">
          <statusCode code="active"/>
          <entryRelationship typeCode="SUBJ"><observation classCode="OBS" moodCode="EVN">
            <code code="55607006" codeSystem="2.16.840.1.113883.6.96" displayName="Problem"/>
            <effectiveTime><low value="20190401"/></effectiveTime>
            <value xsi:type="CD" code="195967001" codeSystem="2.16.840.1.113883.6.96" displayName="Asthma"/>
          </observation></entryRelationship>
        </act></entry>
        <entry><act classCode="ACT" moodCode="EVN">
          <statusCode code="completed"/>
          <entryRelationship typeCode="SUBJ"><observation classCode="OBS" moodCode="EVN">
            <code code="55607006" codeSystem="2.16.840.1.113883.6.96"/>
            <effectiveTime><low value="20200110"/><high value="20200125"/></effectiveTime>
            <value xsi:type="CD" nullFlavor="OTH"><originalText><reference value="#prob2"/></originalText></value>
          </observation></entryRelationship>
        </act></entry>""",
        narrative = """<paragraph ID="prob2">Sprained ankle</paragraph>""",
    )

    private val Allergies = section(
        "48765-2",
        """
        <entry><act classCode="ACT" moodCode="EVN">
          <statusCode code="active"/>
          <entryRelationship typeCode="SUBJ"><observation classCode="OBS" moodCode="EVN">
            <code code="ASSERTION" codeSystem="2.16.840.1.113883.5.4"/>
            <effectiveTime><low value="2015"/></effectiveTime>
            <value xsi:type="CD" code="416098002" codeSystem="2.16.840.1.113883.6.96" displayName="Drug allergy"/>
            <participant typeCode="CSM"><participantRole classCode="MANU"><playingEntity classCode="MMAT">
              <code code="7980" codeSystem="2.16.840.1.113883.6.88" displayName="Penicillin G"/>
            </playingEntity></participantRole></participant>
            <entryRelationship typeCode="MFST" inversionInd="true"><observation classCode="OBS" moodCode="EVN">
              <code code="ASSERTION" codeSystem="2.16.840.1.113883.5.4"/>
              <value xsi:type="CD" code="247472004" codeSystem="2.16.840.1.113883.6.96" displayName="Hives"/>
            </observation></entryRelationship>
          </observation></entryRelationship>
        </act></entry>
        <entry><act classCode="ACT" moodCode="EVN">
          <statusCode code="active"/>
          <entryRelationship typeCode="SUBJ"><observation classCode="OBS" moodCode="EVN" negationInd="true">
            <code code="ASSERTION" codeSystem="2.16.840.1.113883.5.4"/>
            <value xsi:type="CD" code="419199007" codeSystem="2.16.840.1.113883.6.96" displayName="Allergy to substance"/>
            <participant typeCode="CSM"><participantRole classCode="MANU"><playingEntity classCode="MMAT">
              <code nullFlavor="NA"/>
            </playingEntity></participantRole></participant>
          </observation></entryRelationship>
        </act></entry>""",
    )

    private val Medications = section(
        "10160-0",
        """
        <entry><substanceAdministration classCode="SBADM" moodCode="EVN">
          <text><reference value="#sig1"/></text>
          <statusCode code="active"/>
          <effectiveTime xsi:type="IVL_TS"><low value="20230101"/></effectiveTime>
          <effectiveTime xsi:type="PIVL_TS" institutionSpecified="true" operator="A"><period value="12" unit="h"/></effectiveTime>
          <routeCode code="C38216" codeSystem="2.16.840.1.113883.3.26.1.1" displayName="Respiratory (inhalation)"/>
          <doseQuantity value="2"/>
          <consumable><manufacturedProduct classCode="MANU"><manufacturedMaterial>
            <code code="745679" codeSystem="2.16.840.1.113883.6.88" displayName="Albuterol inhaler"/>
          </manufacturedMaterial></manufacturedProduct></consumable>
        </substanceAdministration></entry>""",
        narrative = """<paragraph ID="sig1">2 puffs every 12 hours</paragraph>""",
    )

    private val Immunizations = section(
        "11369-6",
        """
        <entry><substanceAdministration classCode="SBADM" moodCode="EVN" negationInd="false">
          <statusCode code="completed"/>
          <effectiveTime value="20231015"/>
          <consumable><manufacturedProduct classCode="MANU"><manufacturedMaterial>
            <code code="140" codeSystem="2.16.840.1.113883.12.292" displayName="Influenza, seasonal, injectable"/>
            <lotNumberText>LOT42</lotNumberText>
          </manufacturedMaterial></manufacturedProduct></consumable>
        </substanceAdministration></entry>
        <entry><substanceAdministration classCode="SBADM" moodCode="EVN" negationInd="true">
          <effectiveTime value="20220901"/>
          <consumable><manufacturedProduct classCode="MANU"><manufacturedMaterial>
            <code code="33" codeSystem="2.16.840.1.113883.12.292" displayName="Pneumococcal polysaccharide"/>
          </manufacturedMaterial></manufacturedProduct></consumable>
        </substanceAdministration></entry>""",
    )

    private val Results = section(
        "30954-2",
        """
        <entry><organizer classCode="BATTERY" moodCode="EVN">
          <code code="57021-8" codeSystem="2.16.840.1.113883.6.1" displayName="CBC panel"/>
          <statusCode code="completed"/>
          <effectiveTime value="20240301083000-0500"/>
          <component><observation classCode="OBS" moodCode="EVN">
            <code code="718-7" codeSystem="2.16.840.1.113883.6.1" displayName="Hemoglobin"/>
            <value xsi:type="PQ" value="13.2" unit="g/dL"/>
            <interpretationCode code="N" codeSystem="2.16.840.1.113883.5.83"/>
            <referenceRange><observationRange><value xsi:type="IVL_PQ"><low value="12.0" unit="g/dL"/><high value="15.5" unit="g/dL"/></value></observationRange></referenceRange>
          </observation></component>
          <component><observation classCode="OBS" moodCode="EVN">
            <code code="5778-6" codeSystem="2.16.840.1.113883.6.1" displayName="Color of urine"/>
            <effectiveTime value="20240302"/>
            <value xsi:type="ST">Yellow</value>
          </observation></component>
        </organizer></entry>""",
    )

    private val VitalSigns = section(
        "8716-3",
        """
        <entry><organizer classCode="CLUSTER" moodCode="EVN">
          <code code="46680005" codeSystem="2.16.840.1.113883.6.96" displayName="Vital signs"/>
          <effectiveTime value="20240315"/>
          <component><observation classCode="OBS" moodCode="EVN">
            <code code="8480-6" codeSystem="2.16.840.1.113883.6.1" displayName="Systolic blood pressure"/>
            <value xsi:type="PQ" value="118" unit="mm[Hg]"/>
          </observation></component>
        </organizer></entry>""",
    )

    private val SocialHistory = section(
        "29762-2",
        """
        <entry><observation classCode="OBS" moodCode="EVN">
          <code code="72166-2" codeSystem="2.16.840.1.113883.6.1" displayName="Tobacco smoking status"/>
          <effectiveTime value="20240315"/>
          <value xsi:type="CD" code="266919005" codeSystem="2.16.840.1.113883.6.96" displayName="Never smoked tobacco"/>
        </observation></entry>""",
    )

    private val Procedures = section(
        "47519-4",
        """
        <entry><procedure classCode="PROC" moodCode="EVN">
          <code code="80146002" codeSystem="2.16.840.1.113883.6.96" displayName="Appendectomy"/>
          <statusCode code="completed"/>
          <effectiveTime value="20101120"/>
          <targetSiteCode code="66754008" codeSystem="2.16.840.1.113883.6.96" displayName="Appendix"/>
        </procedure></entry>""",
    )

    private val Encounters = section(
        "46240-8",
        """
        <entry><encounter classCode="ENC" moodCode="EVN">
          <code code="AMB" codeSystem="2.16.840.1.113883.5.4" displayName="Ambulatory"/>
          <effectiveTime><low value="20240315090000-0500"/><high value="20240315093000-0500"/></effectiveTime>
        </encounter></entry>
        <entry><encounter classCode="ENC" moodCode="EVN">
          <code code="99213" codeSystem="2.16.840.1.113883.6.12" displayName="Office visit"/>
          <effectiveTime value="20230601140000"/>
        </encounter></entry>""",
    )

    val summary: String = document(
        Problems + Allergies + Medications + Immunizations + Results + VitalSigns + SocialHistory + Procedures + Encounters +
            section("10164-2", "", narrative = "<paragraph>History of present illness, as free text.</paragraph>"),
    )

    /** The same problems with no custodian and no document id: the user names the source, and ids come from the content. */
    val bare: String = document(Problems, custodian = false, ids = false)
}
