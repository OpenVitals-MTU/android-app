package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate

/** Sourced guidance for an established phase. Its text is the string resource `cycle_tip_<id>`. */
data class PhaseTip(
    val id: String,
    val phase: CyclePhase,
    val source: CycleSource,
    /** Offered only when this context is declared. */
    val targetContext: TrackingContext? = null,
    /** Preferred when one of these was recorded in the last days. */
    val targetSymptoms: Set<CycleSymptom> = emptySet(),
)

object PhaseTips {
    val ALL: List<PhaseTip> = listOf(
        PhaseTip("menstrual_warmth", CyclePhase.MENSTRUAL, CycleSources.NHS_PERIOD_PAIN),
        PhaseTip("menstrual_movement", CyclePhase.MENSTRUAL, CycleSources.NHS_PERIOD_PAIN),
        PhaseTip("menstrual_pain_support", CyclePhase.MENSTRUAL, CycleSources.NHS_PERIOD_PAIN),
        PhaseTip("menstrual_hydration", CyclePhase.MENSTRUAL, CycleSources.NHS_PERIODS),
        PhaseTip(
            "menstrual_endo_pelvic_rest",
            CyclePhase.MENSTRUAL,
            CycleSources.HAS_ENDOMETRIOSIS,
            targetContext = TrackingContext.ENDOMETRIOSIS,
            targetSymptoms = setOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD, CycleSymptom.CRAMPS),
        ),
        PhaseTip(
            "menstrual_endo_fatigue_pacing",
            CyclePhase.MENSTRUAL,
            CycleSources.CNGOF_PAIN,
            targetContext = TrackingContext.ENDOMETRIOSIS,
            targetSymptoms = setOf(CycleSymptom.FATIGUE),
        ),
        PhaseTip(
            "menstrual_endo_radiating_pain",
            CyclePhase.MENSTRUAL,
            CycleSources.HAS_ENDOMETRIOSIS,
            targetContext = TrackingContext.ENDOMETRIOSIS,
            targetSymptoms = setOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD, CycleSymptom.BACKACHE),
        ),
        PhaseTip("follicular_varies", CyclePhase.FOLLICULAR, CycleSources.MIHM),
        PhaseTip("follicular_own_history", CyclePhase.FOLLICULAR, CycleSources.MIHM),
        PhaseTip("follicular_no_fixed_day", CyclePhase.FOLLICULAR, CycleSources.GRIEGER),
        PhaseTip(
            "follicular_pcos_elongation",
            CyclePhase.FOLLICULAR,
            CycleSources.MONASH_PCOS,
            targetContext = TrackingContext.PCOS,
        ),
        PhaseTip(
            "follicular_pcos_movement",
            CyclePhase.FOLLICULAR,
            CycleSources.MONASH_PCOS,
            targetContext = TrackingContext.PCOS,
        ),
        PhaseTip(
            "follicular_thyroid_fatigue",
            CyclePhase.FOLLICULAR,
            CycleSources.GUNGOR_THYROID,
            targetContext = TrackingContext.THYROID,
            targetSymptoms = setOf(CycleSymptom.FATIGUE),
        ),
        PhaseTip(
            "follicular_perimeno_fluctuation",
            CyclePhase.FOLLICULAR,
            CycleSources.BMS_PERIMENOPAUSE,
            targetContext = TrackingContext.PERIMENOPAUSE,
        ),
        PhaseTip("ovulatory_not_confirmed", CyclePhase.OVULATORY, CycleSources.FEHRING),
        PhaseTip("ovulatory_counts_back", CyclePhase.OVULATORY, CycleSources.NHS_PERIODS),
        PhaseTip("ovulatory_not_day_14", CyclePhase.OVULATORY, CycleSources.GRIEGER),
        PhaseTip("ovulatory_hydration_mucus", CyclePhase.OVULATORY, CycleSources.ACOG_PMS),
        PhaseTip("luteal_diary", CyclePhase.LUTEAL, CycleSources.ACOG_PREMENSTRUAL_GUIDELINE),
        PhaseTip("luteal_daily_support", CyclePhase.LUTEAL, CycleSources.NHS_PMS),
        PhaseTip("luteal_seek_support", CyclePhase.LUTEAL, CycleSources.NHS_PMS),
        PhaseTip("luteal_hydration", CyclePhase.LUTEAL, CycleSources.NHS_PMS),
        PhaseTip(
            "luteal_sodium_bloating",
            CyclePhase.LUTEAL,
            CycleSources.ACOG_PMS,
            targetSymptoms = setOf(CycleSymptom.BLOATING, CycleSymptom.BREAST_TENDERNESS),
        ),
        PhaseTip(
            "luteal_complex_carbs",
            CyclePhase.LUTEAL,
            CycleSources.ACOG_PMS,
            targetSymptoms = setOf(CycleSymptom.FATIGUE, CycleSymptom.MOOD_CHANGES),
        ),
        PhaseTip(
            "luteal_digestive_comfort",
            CyclePhase.LUTEAL,
            CycleSources.ACOG_PMS,
            targetSymptoms = setOf(CycleSymptom.NAUSEA, CycleSymptom.DIGESTIVE_CHANGES),
        ),
        PhaseTip(
            "luteal_pmdd_neuro_validation",
            CyclePhase.LUTEAL,
            CycleSources.INSERM_PMDD,
            targetContext = TrackingContext.PMDD,
        ),
        PhaseTip(
            "luteal_pmdd_pacing",
            CyclePhase.LUTEAL,
            CycleSources.ACOG_PREMENSTRUAL_GUIDELINE,
            targetContext = TrackingContext.PMDD,
            targetSymptoms = setOf(CycleSymptom.MOOD_CHANGES, CycleSymptom.ANXIETY),
        ),
        PhaseTip(
            "luteal_sleep_routine",
            CyclePhase.LUTEAL,
            CycleSources.ACOG_PREMENSTRUAL_GUIDELINE,
            targetSymptoms = setOf(CycleSymptom.SLEEP_ISSUE),
        ),
        PhaseTip(
            "luteal_endo_pelvic_tension",
            CyclePhase.LUTEAL,
            CycleSources.HAS_ENDOMETRIOSIS,
            targetContext = TrackingContext.ENDOMETRIOSIS,
            targetSymptoms = setOf(CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD, CycleSymptom.ABDOMINAL_PAIN),
        ),
        PhaseTip(
            "luteal_perimeno_sleep",
            CyclePhase.LUTEAL,
            CycleSources.BMS_PERIMENOPAUSE,
            targetContext = TrackingContext.PERIMENOPAUSE,
            targetSymptoms = setOf(CycleSymptom.SLEEP_ISSUE),
        ),
    )

    /**
     * The tip for [date]: a declared context scores 3, a recent symptom 2; ties
     * are broken by the date so the card is stable all day.
     */
    fun forDate(
        phase: CyclePhase,
        date: LocalDate,
        declaredContexts: Set<TrackingContext> = emptySet(),
        recentSymptoms: Set<CycleSymptom> = emptySet(),
    ): PhaseTip {
        val eligible = ALL.filter { it.phase == phase && (it.targetContext == null || it.targetContext in declaredContexts) }
        check(eligible.isNotEmpty()) { "No tips registered for $phase" }
        val scored = eligible.map { tip ->
            var score = 0
            if (tip.targetContext != null && tip.targetContext in declaredContexts) score += 3
            if (tip.targetSymptoms.any { it in recentSymptoms }) score += 2
            tip to score
        }
        val best = scored.maxOf { it.second }
        val candidates = scored.filter { it.second == best }.map { it.first }
        return candidates[stableIndexFor(date, candidates.size)]
    }
}
