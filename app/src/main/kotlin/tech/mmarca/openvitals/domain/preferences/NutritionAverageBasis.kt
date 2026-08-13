package tech.mmarca.openvitals.domain.preferences

/**
 * What a nutrition daily average divides by.
 *
 * A period total answers "how much in these weeks", which nobody eats by; the
 * useful figure is the daily one. Which daily figure depends on what the eater
 * means by a day, and the two readings genuinely differ — someone logging three
 * days out of seven means something different by "my daily calories" than
 * someone who logs every day and skipped one.
 */
enum class NutritionAverageBasis {
    /**
     * Days that carried a logged value. What someone who logs occasionally
     * means: the average of the meals they actually recorded, undiluted by the
     * days they did not.
     */
    LOGGED_DAYS,

    /**
     * Every elapsed day of the period, logged or not. What someone who logs
     * daily means: a day with nothing recorded is a day they ate little,
     * not a day to leave out.
     */
    EVERY_DAY,
}
