package tech.mmarca.openvitals.domain.preferences

/** What a nutrition daily average divides by. Occasional and daily loggers mean different things. */
enum class NutritionAverageBasis {
    /** Days that carried a logged value. */
    LOGGED_DAYS,

    /** Every elapsed day, logged or not. */
    EVERY_DAY,
}
