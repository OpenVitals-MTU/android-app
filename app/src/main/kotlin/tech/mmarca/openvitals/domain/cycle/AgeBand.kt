package tech.mmarca.openvitals.domain.cycle

/**
 * Optional age band. It only picks the variability prior of the estimate.
 *
 * Each band carries the within-person standard deviation of cycle length from
 * the Apple Women's Health Study (Li et al. 2023, table 4). Variability is
 * U-shaped in age, so one constant cannot stand in for the band.
 */
enum class AgeBand(val id: String, val variationSdDays: Double) {
    UNDER_20("under_20", 5.33),
    AGE_20_24("age_20_24", 5.07),
    AGE_25_29("age_25_29", 4.70),
    AGE_30_34("age_30_34", 4.28),
    AGE_35_39("age_35_39", 3.79),
    AGE_40_44("age_40_44", 3.99),
    AGE_45_49("age_45_49", 5.42),
    AGE_50_PLUS("age_50_plus", 11.19),
    ;

    companion object {
        fun fromId(id: String?): AgeBand? = entries.firstOrNull { it.id == id }

        /** The band an age in years falls in. */
        fun forAge(years: Int): AgeBand = when {
            years < 20 -> UNDER_20
            years < 25 -> AGE_20_24
            years < 30 -> AGE_25_29
            years < 35 -> AGE_30_34
            years < 40 -> AGE_35_39
            years < 45 -> AGE_40_44
            years < 50 -> AGE_45_49
            else -> AGE_50_PLUS
        }

        /**
         * The prior without a declared band: the mean of the six bands for ages
         * 20 to 49. An undeclared user must not get the narrowest band by default.
         */
        const val UNDECLARED_VARIATION_SD_DAYS = 4.54
    }
}
