package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate

/** A sourced population fact. Its text is the string resource `cycle_fact_<id>`. */
data class CycleFact(val id: String, val source: CycleSource)

/**
 * Population facts about cycle length and variability. Shown when no phase can
 * be named. They are never chosen from the reader's own data: a fact picked
 * because someone is on day 24 would be an inference about their phase.
 * Condition prevalence is left out on purpose; it would read as screening.
 */
object CycleFacts {
    val ALL: List<CycleFact> = listOf(
        CycleFact("no_single_normal", CycleSources.GRIEGER),
        CycleFact("most_cycles_in_range", CycleSources.GRIEGER),
        CycleFact("variation_is_common", CycleSources.GRIEGER),
        CycleFact("long_cycles_common", CycleSources.GRIEGER),
        CycleFact("short_cycles_rare", CycleSources.GRIEGER),
        CycleFact("ovulation_not_day_14", CycleSources.GRIEGER),
        CycleFact("luteal_varies", CycleSources.GRIEGER),
        CycleFact("cycles_shorten_with_age", CycleSources.GRIEGER),
        CycleFact("quarter_very_regular", CycleSources.GRIEGER),
        CycleFact("mean_length", CycleSources.LI_APPLE),
        CycleFact("variation_by_age", CycleSources.LI_APPLE),
        CycleFact("variation_after_50", CycleSources.LI_APPLE),
        CycleFact("median_iqr", CycleSources.LI_APPLE),
        CycleFact("variation_late_forties", CycleSources.LI_APPLE),
        CycleFact("same_person_varies", CycleSources.BULL),
        CycleFact("large_cohort_mean", CycleSources.BULL),
        CycleFact("period_duration", CycleSources.NHS_PERIODS),
        CycleFact("blood_volume", CycleSources.NHS_PERIODS),
        CycleFact("menarche_age", CycleSources.NHS_PERIODS),
        CycleFact("menopause_age", CycleSources.NHS_PERIODS),
        CycleFact("ovulation_counts_backwards", CycleSources.NHS_PERIODS),
        CycleFact("irregular_definition", CycleSources.NHS_IRREGULAR),
        CycleFact("health_not_hygiene", CycleSources.WHO_MENSTRUAL_HEALTH),
        CycleFact("not_shameful", CycleSources.WHO_MENSTRUAL_HEALTH),
    )

    /** The same fact all day and across relaunches. */
    fun forDate(date: LocalDate): CycleFact = ALL[stableIndexFor(date, ALL.size)]
}
