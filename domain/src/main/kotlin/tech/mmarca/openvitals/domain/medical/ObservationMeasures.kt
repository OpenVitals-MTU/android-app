package tech.mmarca.openvitals.domain.medical

/** How a measure's value is typed, and which FHIR `value[x]` it is written as. */
enum class MeasureValue {
    /** A number with a unit: `valueQuantity`. */
    QUANTITY,

    /** Systolic and diastolic, as the two components of a blood pressure panel. */
    BLOOD_PRESSURE,

    /** A count: `valueInteger`. */
    INTEGER,

    /** A day, such as an estimated delivery date: `valueDateTime`. It may lie ahead. */
    DATE,

    /** One of [ObservationMeasure.answers]: `valueCodeableConcept`. */
    CODED,

    /** Free text under a name the user types: `valueString`. */
    TEXT,
}

/** A unit a measure can be typed in: its UCUM code, and the symbol shown. */
data class MeasureUnit(val ucum: String, val symbol: String)

/** An answer a coded measure takes, as a SNOMED CT code. */
data class MeasureAnswer(val snomed: String, val display: String)

/**
 * What a vital sign, social history or pregnancy record can say. [loinc] is on Health Connect's
 * own list for the kind, so the platform files the record where the kind says; null only for
 * [OTHER_SOCIAL_HISTORY], filed by its category alone. [display] is LOINC's name, written beside
 * the code; the form names a measure in the user's language.
 */
enum class ObservationMeasure(
    val kind: ManualRecordKind,
    val loinc: String?,
    val display: String,
    val value: MeasureValue,
    /** The units offered, the default first. Empty for a value without one. */
    val units: List<MeasureUnit> = emptyList(),
    val answers: List<MeasureAnswer> = emptyList(),
) {
    BLOOD_PRESSURE(
        ManualRecordKind.VITAL_SIGN, "85354-9", "Blood pressure panel with all children optional", MeasureValue.BLOOD_PRESSURE,
        listOf(MeasureUnit("mm[Hg]", "mmHg")),
    ),
    HEART_RATE(ManualRecordKind.VITAL_SIGN, "8867-4", "Heart rate", MeasureValue.QUANTITY, listOf(MeasureUnit("/min", "bpm"))),
    RESPIRATORY_RATE(ManualRecordKind.VITAL_SIGN, "9279-1", "Respiratory rate", MeasureValue.QUANTITY, listOf(MeasureUnit("/min", "/min"))),
    OXYGEN_SATURATION(
        ManualRecordKind.VITAL_SIGN, "2708-6", "Oxygen saturation in Arterial blood", MeasureValue.QUANTITY, listOf(MeasureUnit("%", "%")),
    ),
    BODY_TEMPERATURE(
        ManualRecordKind.VITAL_SIGN, "8310-5", "Body temperature", MeasureValue.QUANTITY,
        listOf(MeasureUnit("Cel", "°C"), MeasureUnit("[degF]", "°F")),
    ),
    BODY_WEIGHT(
        ManualRecordKind.VITAL_SIGN, "29463-7", "Body weight", MeasureValue.QUANTITY,
        listOf(MeasureUnit("kg", "kg"), MeasureUnit("[lb_av]", "lb")),
    ),
    BODY_HEIGHT(
        ManualRecordKind.VITAL_SIGN, "8302-2", "Body height", MeasureValue.QUANTITY,
        listOf(MeasureUnit("cm", "cm"), MeasureUnit("[in_i]", "in")),
    ),
    BMI(ManualRecordKind.VITAL_SIGN, "39156-5", "Body mass index (BMI) [Ratio]", MeasureValue.QUANTITY, listOf(MeasureUnit("kg/m2", "kg/m²"))),
    HEAD_CIRCUMFERENCE(
        ManualRecordKind.VITAL_SIGN, "9843-4", "Head Occipital-frontal circumference", MeasureValue.QUANTITY,
        listOf(MeasureUnit("cm", "cm"), MeasureUnit("[in_i]", "in")),
    ),
    SMOKING_STATUS(
        ManualRecordKind.SOCIAL_HISTORY, "72166-2", "Tobacco smoking status", MeasureValue.CODED,
        answers = listOf(
            MeasureAnswer("449868002", "Smokes tobacco daily"),
            MeasureAnswer("428041000124106", "Occasional tobacco smoker"),
            MeasureAnswer("8517006", "Ex-smoker"),
            MeasureAnswer("266919005", "Never smoked tobacco"),
            MeasureAnswer("266927001", "Tobacco smoking consumption unknown"),
        ),
    ),
    ALCOHOL_PER_DAY(
        ManualRecordKind.SOCIAL_HISTORY, "74013-4", "Alcoholic drinks per day", MeasureValue.QUANTITY,
        listOf(MeasureUnit("{drinks}/d", "/d")),
    ),
    OTHER_SOCIAL_HISTORY(ManualRecordKind.SOCIAL_HISTORY, null, "Social history", MeasureValue.TEXT),
    PREGNANCY_STATUS(
        ManualRecordKind.PREGNANCY, "82810-3", "Pregnancy status", MeasureValue.CODED,
        answers = listOf(
            MeasureAnswer("77386006", "Pregnant"),
            MeasureAnswer("60001007", "Not pregnant"),
            MeasureAnswer("261665006", "Unknown"),
        ),
    ),
    DELIVERY_DATE(ManualRecordKind.PREGNANCY, "11778-8", "Delivery date Estimated", MeasureValue.DATE),
    DELIVERY_DATE_BY_PERIOD(ManualRecordKind.PREGNANCY, "11779-6", "Delivery date Estimated from last menstrual period", MeasureValue.DATE),
    DELIVERY_DATE_BY_OVULATION(ManualRecordKind.PREGNANCY, "11780-4", "Delivery date Estimated from ovulation date", MeasureValue.DATE),
    BIRTHS_TOTAL(ManualRecordKind.PREGNANCY, "11640-0", "[#] Births total", MeasureValue.INTEGER),
    BIRTHS_LIVE(ManualRecordKind.PREGNANCY, "11636-8", "[#] Births.live", MeasureValue.INTEGER),
    BIRTHS_TERM(ManualRecordKind.PREGNANCY, "11639-2", "[#] Births.term", MeasureValue.INTEGER),
    BIRTHS_PRETERM(ManualRecordKind.PREGNANCY, "11637-6", "[#] Births.preterm", MeasureValue.INTEGER),
    CHILDREN_LIVING(ManualRecordKind.PREGNANCY, "11638-4", "[#] Births.still living", MeasureValue.INTEGER),
    ABORTIONS_TOTAL(ManualRecordKind.PREGNANCY, "11612-9", "[#] Abortions", MeasureValue.INTEGER),
    ABORTIONS_INDUCED(ManualRecordKind.PREGNANCY, "11613-7", "[#] Abortions.induced", MeasureValue.INTEGER),
    ABORTIONS_SPONTANEOUS(ManualRecordKind.PREGNANCY, "11614-5", "[#] Abortions.spontaneous", MeasureValue.INTEGER),
    ECTOPIC_PREGNANCIES(ManualRecordKind.PREGNANCY, "33065-4", "[#] Ectopic pregnancy", MeasureValue.INTEGER),
    ;

    /** The unit typed in, by its UCUM code; the first when none or an unknown one is given. */
    fun unitFor(ucum: String?): MeasureUnit = units.firstOrNull { it.ucum == ucum } ?: units.first()

    companion object {
        fun of(kind: ManualRecordKind): List<ObservationMeasure> = entries.filter { it.kind == kind }

        /** The measure a stored record's LOINC code names, or the free one for a social history record without. */
        fun ofLoinc(kind: ManualRecordKind, loinc: String?): ObservationMeasure? =
            of(kind).firstOrNull { it.loinc != null && it.loinc == loinc }
                ?: OTHER_SOCIAL_HISTORY.takeIf { kind == ManualRecordKind.SOCIAL_HISTORY }
    }
}
