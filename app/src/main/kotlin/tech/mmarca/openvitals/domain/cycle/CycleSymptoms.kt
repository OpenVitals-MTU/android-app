package tech.mmarca.openvitals.domain.cycle

/** How the day log groups its symptom chips. */
enum class CycleSymptomGroup {
    PAIN,
    PHYSICAL,
    MOOD_ENERGY,
}

/**
 * The observations a user may record. Each has a stable id for storage and a
 * reviewed source: the NHS premenstrual page, the WHO endometriosis fact sheet.
 * These are things the user may record, never things the app suggests they have.
 */
enum class CycleSymptom(val id: String, val group: CycleSymptomGroup) {
    CRAMPS("cramps", CycleSymptomGroup.PAIN),
    HEADACHE("headache", CycleSymptomGroup.PAIN),
    ABDOMINAL_PAIN("abdominal_pain", CycleSymptomGroup.PAIN),
    BACKACHE("backache", CycleSymptomGroup.PAIN),
    MUSCLE_ACHES("muscle_aches", CycleSymptomGroup.PAIN),
    PELVIC_PAIN_OUTSIDE_PERIOD("pelvic_pain_outside_period", CycleSymptomGroup.PAIN),
    BLOATING("bloating", CycleSymptomGroup.PHYSICAL),
    NAUSEA("nausea", CycleSymptomGroup.PHYSICAL),
    DIGESTIVE_CHANGES("digestive_changes", CycleSymptomGroup.PHYSICAL),
    BREAST_TENDERNESS("breast_tenderness", CycleSymptomGroup.PHYSICAL),
    ACNE("acne", CycleSymptomGroup.PHYSICAL),
    FATIGUE("fatigue", CycleSymptomGroup.MOOD_ENERGY),
    SLEEP_ISSUE("sleep_issue", CycleSymptomGroup.MOOD_ENERGY),
    MOOD_CHANGES("mood_changes", CycleSymptomGroup.MOOD_ENERGY),
    ANXIETY("anxiety", CycleSymptomGroup.MOOD_ENERGY),
    ;

    companion object {
        fun fromId(id: String): CycleSymptom? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Which symptoms the day log offers, given the declared contexts. Only
 * observation contexts add vocabulary; timing contexts widen estimates instead.
 */
object ObservationCatalog {
    private val BASE = listOf(
        CycleSymptom.CRAMPS,
        CycleSymptom.HEADACHE,
        CycleSymptom.ABDOMINAL_PAIN,
        CycleSymptom.BACKACHE,
        CycleSymptom.MUSCLE_ACHES,
        CycleSymptom.FATIGUE,
        CycleSymptom.SLEEP_ISSUE,
        CycleSymptom.BLOATING,
        CycleSymptom.NAUSEA,
        CycleSymptom.DIGESTIVE_CHANGES,
    )

    /** NHS premenstrual syndrome page: breast tenderness, mood, anxiety, skin. */
    private val PREMENSTRUAL = listOf(
        CycleSymptom.BREAST_TENDERNESS,
        CycleSymptom.MOOD_CHANGES,
        CycleSymptom.ANXIETY,
        CycleSymptom.ACNE,
    )

    /** WHO endometriosis fact sheet: pelvic pain that does not end with the period. */
    private val ENDOMETRIOSIS = listOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD)

    private val BY_CONTEXT = mapOf(
        TrackingContext.PMS to PREMENSTRUAL,
        TrackingContext.PMDD to PREMENSTRUAL,
        TrackingContext.ENDOMETRIOSIS to ENDOMETRIOSIS,
    )

    /** Base first, then context additions, in a stable order. */
    fun symptomsFor(contexts: Set<TrackingContext>): List<CycleSymptom> {
        val additions = TrackingContext.entries.filter { it in contexts }.flatMap { BY_CONTEXT[it].orEmpty() }
        return (BASE + additions).distinct()
    }
}
