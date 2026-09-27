package tech.mmarca.openvitals.features.cycle

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.cycle.AgeBand
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CyclePhase
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.cycle.PhaseIndeterminateReason
import tech.mmarca.openvitals.domain.cycle.TrackingContext
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.FollicularPhaseColor
import tech.mmarca.openvitals.ui.theme.LutealPhaseColor
import tech.mmarca.openvitals.ui.theme.OvulatoryPhaseColor

/** String lookups for the cycle vocabulary. Ids without copy fail `CycleCopyCatalogTest`. */

@StringRes
fun cycleSymptomLabelRes(symptom: CycleSymptom): Int = when (symptom) {
    CycleSymptom.CRAMPS -> R.string.cycle_symptom_cramps
    CycleSymptom.HEADACHE -> R.string.cycle_symptom_headache
    CycleSymptom.ABDOMINAL_PAIN -> R.string.cycle_symptom_abdominal_pain
    CycleSymptom.BACKACHE -> R.string.cycle_symptom_backache
    CycleSymptom.MUSCLE_ACHES -> R.string.cycle_symptom_muscle_aches
    CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD -> R.string.cycle_symptom_pelvic_pain_outside_period
    CycleSymptom.BLOATING -> R.string.cycle_symptom_bloating
    CycleSymptom.NAUSEA -> R.string.cycle_symptom_nausea
    CycleSymptom.DIGESTIVE_CHANGES -> R.string.cycle_symptom_digestive_changes
    CycleSymptom.BREAST_TENDERNESS -> R.string.cycle_symptom_breast_tenderness
    CycleSymptom.ACNE -> R.string.cycle_symptom_acne
    CycleSymptom.FATIGUE -> R.string.cycle_symptom_fatigue
    CycleSymptom.SLEEP_ISSUE -> R.string.cycle_symptom_sleep_issue
    CycleSymptom.MOOD_CHANGES -> R.string.cycle_symptom_mood_changes
    CycleSymptom.ANXIETY -> R.string.cycle_symptom_anxiety
}

@StringRes
fun cyclePhaseLabelRes(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.cycle_phase_menstrual
    CyclePhase.FOLLICULAR -> R.string.cycle_phase_follicular
    CyclePhase.OVULATORY -> R.string.cycle_phase_ovulatory
    CyclePhase.LUTEAL -> R.string.cycle_phase_luteal
}

/** One colour per phase, drawn on data only. */
fun cyclePhaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> CycleColor
    CyclePhase.FOLLICULAR -> FollicularPhaseColor
    CyclePhase.OVULATORY -> OvulatoryPhaseColor
    CyclePhase.LUTEAL -> LutealPhaseColor
}

@StringRes
fun phaseIndeterminateReasonRes(reason: PhaseIndeterminateReason): Int = when (reason) {
    PhaseIndeterminateReason.NO_CURRENT_CYCLE -> R.string.cycle_phase_reason_no_cycle
    PhaseIndeterminateReason.EARLY_CYCLE_WITHOUT_BLEEDING_DETAIL -> R.string.cycle_phase_reason_early_cycle
    PhaseIndeterminateReason.NEEDS_MORE_HISTORY -> R.string.cycle_phase_reason_more_history
    PhaseIndeterminateReason.INTERVALS_OUT_OF_RANGE -> R.string.cycle_phase_reason_out_of_range
    PhaseIndeterminateReason.PHASE_TRANSITION -> R.string.cycle_phase_reason_transition
    PhaseIndeterminateReason.NEXT_PERIOD_WINDOW -> R.string.cycle_phase_reason_period_window
    PhaseIndeterminateReason.ESTIMATE_EXPIRED -> R.string.cycle_phase_reason_expired
}

@StringRes
fun exclusionReasonLabelRes(reason: CycleExclusionReason): Int = when (reason) {
    CycleExclusionReason.ILLNESS -> R.string.cycle_exclusion_reason_illness
    CycleExclusionReason.MEDICAL_TREATMENT -> R.string.cycle_exclusion_reason_medical_treatment
    CycleExclusionReason.CONTRACEPTION_CHANGE -> R.string.cycle_exclusion_reason_contraception_change
    CycleExclusionReason.STRESS_OR_TRAVEL -> R.string.cycle_exclusion_reason_stress_or_travel
    CycleExclusionReason.OTHER -> R.string.cycle_exclusion_reason_other
}

@StringRes
fun trackingContextLabelRes(context: TrackingContext): Int = when (context) {
    TrackingContext.PMS -> R.string.cycle_context_pms
    TrackingContext.PMDD -> R.string.cycle_context_pmdd
    TrackingContext.ENDOMETRIOSIS -> R.string.cycle_context_endometriosis
    TrackingContext.PCOS -> R.string.cycle_context_pcos
    TrackingContext.PERIMENOPAUSE -> R.string.cycle_context_perimenopause
    TrackingContext.THYROID -> R.string.cycle_context_thyroid
}

@StringRes
fun ageBandLabelRes(band: AgeBand): Int = when (band) {
    AgeBand.UNDER_20 -> R.string.cycle_age_band_under_20
    AgeBand.AGE_20_24 -> R.string.cycle_age_band_20_24
    AgeBand.AGE_25_29 -> R.string.cycle_age_band_25_29
    AgeBand.AGE_30_34 -> R.string.cycle_age_band_30_34
    AgeBand.AGE_35_39 -> R.string.cycle_age_band_35_39
    AgeBand.AGE_40_44 -> R.string.cycle_age_band_40_44
    AgeBand.AGE_45_49 -> R.string.cycle_age_band_45_49
    AgeBand.AGE_50_PLUS -> R.string.cycle_age_band_50_plus
}

/** The tip's copy, or null for an id without a string, so the catalog and the copy cannot drift silently. */
@Composable
fun phaseTipText(id: String): String? = phaseTipTextRes(id)?.let { stringResource(it) }

@Composable
fun cycleFactText(id: String): String? = cycleFactTextRes(id)?.let { stringResource(it) }

@StringRes
fun phaseTipTextRes(id: String): Int? = when (id) {
    "menstrual_warmth" -> R.string.cycle_tip_menstrual_warmth
    "menstrual_movement" -> R.string.cycle_tip_menstrual_movement
    "menstrual_pain_support" -> R.string.cycle_tip_menstrual_pain_support
    "menstrual_hydration" -> R.string.cycle_tip_menstrual_hydration
    "menstrual_endo_pelvic_rest" -> R.string.cycle_tip_menstrual_endo_pelvic_rest
    "menstrual_endo_fatigue_pacing" -> R.string.cycle_tip_menstrual_endo_fatigue_pacing
    "menstrual_endo_radiating_pain" -> R.string.cycle_tip_menstrual_endo_radiating_pain
    "follicular_varies" -> R.string.cycle_tip_follicular_varies
    "follicular_own_history" -> R.string.cycle_tip_follicular_own_history
    "follicular_no_fixed_day" -> R.string.cycle_tip_follicular_no_fixed_day
    "follicular_pcos_elongation" -> R.string.cycle_tip_follicular_pcos_elongation
    "follicular_pcos_movement" -> R.string.cycle_tip_follicular_pcos_movement
    "follicular_thyroid_fatigue" -> R.string.cycle_tip_follicular_thyroid_fatigue
    "follicular_perimeno_fluctuation" -> R.string.cycle_tip_follicular_perimeno_fluctuation
    "ovulatory_not_confirmed" -> R.string.cycle_tip_ovulatory_not_confirmed
    "ovulatory_counts_back" -> R.string.cycle_tip_ovulatory_counts_back
    "ovulatory_not_day_14" -> R.string.cycle_tip_ovulatory_not_day_14
    "ovulatory_hydration_mucus" -> R.string.cycle_tip_ovulatory_hydration_mucus
    "luteal_diary" -> R.string.cycle_tip_luteal_diary
    "luteal_daily_support" -> R.string.cycle_tip_luteal_daily_support
    "luteal_seek_support" -> R.string.cycle_tip_luteal_seek_support
    "luteal_hydration" -> R.string.cycle_tip_luteal_hydration
    "luteal_sodium_bloating" -> R.string.cycle_tip_luteal_sodium_bloating
    "luteal_complex_carbs" -> R.string.cycle_tip_luteal_complex_carbs
    "luteal_digestive_comfort" -> R.string.cycle_tip_luteal_digestive_comfort
    "luteal_pmdd_neuro_validation" -> R.string.cycle_tip_luteal_pmdd_neuro_validation
    "luteal_pmdd_pacing" -> R.string.cycle_tip_luteal_pmdd_pacing
    "luteal_sleep_routine" -> R.string.cycle_tip_luteal_sleep_routine
    "luteal_endo_pelvic_tension" -> R.string.cycle_tip_luteal_endo_pelvic_tension
    "luteal_perimeno_sleep" -> R.string.cycle_tip_luteal_perimeno_sleep
    else -> null
}

@StringRes
fun cycleFactTextRes(id: String): Int? = when (id) {
    "no_single_normal" -> R.string.cycle_fact_no_single_normal
    "most_cycles_in_range" -> R.string.cycle_fact_most_cycles_in_range
    "variation_is_common" -> R.string.cycle_fact_variation_is_common
    "long_cycles_common" -> R.string.cycle_fact_long_cycles_common
    "short_cycles_rare" -> R.string.cycle_fact_short_cycles_rare
    "ovulation_not_day_14" -> R.string.cycle_fact_ovulation_not_day_14
    "luteal_varies" -> R.string.cycle_fact_luteal_varies
    "cycles_shorten_with_age" -> R.string.cycle_fact_cycles_shorten_with_age
    "quarter_very_regular" -> R.string.cycle_fact_quarter_very_regular
    "mean_length" -> R.string.cycle_fact_mean_length
    "variation_by_age" -> R.string.cycle_fact_variation_by_age
    "variation_after_50" -> R.string.cycle_fact_variation_after_50
    "median_iqr" -> R.string.cycle_fact_median_iqr
    "variation_late_forties" -> R.string.cycle_fact_variation_late_forties
    "same_person_varies" -> R.string.cycle_fact_same_person_varies
    "large_cohort_mean" -> R.string.cycle_fact_large_cohort_mean
    "period_duration" -> R.string.cycle_fact_period_duration
    "blood_volume" -> R.string.cycle_fact_blood_volume
    "menarche_age" -> R.string.cycle_fact_menarche_age
    "menopause_age" -> R.string.cycle_fact_menopause_age
    "ovulation_counts_backwards" -> R.string.cycle_fact_ovulation_counts_backwards
    "irregular_definition" -> R.string.cycle_fact_irregular_definition
    "health_not_hygiene" -> R.string.cycle_fact_health_not_hygiene
    "not_shameful" -> R.string.cycle_fact_not_shameful
    else -> null
}

