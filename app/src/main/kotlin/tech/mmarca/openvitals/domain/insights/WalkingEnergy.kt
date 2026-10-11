package tech.mmarca.openvitals.domain.insights

/**
 * Walking energy from cadence alone, one minute at a time: the "simple"
 * cadence equation of Moore and colleagues, "Development of a Cadence-Based
 * Metabolic Equation for Walking", Med Sci Sports Exerc 2021;53(1):165–173,
 * doi:10.1249/MSS.0000000000002430 (Table 2):
 *
 *     VO2 (ml/kg/min) = 1.811 + 0.02014 · cadence + 7.427e-4 · cadence²
 *
 * fitted on 235 adults aged 21–84 walking level on a treadmill; RMSE 0.60
 * METs at normal speeds. METs are VO2 / 3.5. The minute's active energy is
 * the net cost, the METs above rest, at about 1 kcal per kg per hour per MET:
 *
 *     kcal = (VO2 / 3.5 − 1) · kg / 60
 *
 * Subtracting the resting MET keeps rest from being counted twice when the
 * basal rate is added for a daily total; one MET overstates the resting
 * cost of many people (Byrne and colleagues, J Appl Physiol 2005,
 * doi:10.1152/japplphysiol.00023.2004), so the active share is, if
 * anything, slightly understated.
 *
 * Limits, as the paper states them: level walking only, no hills or stairs,
 * no running, and a whole minute of sustained walking. A minute of a few
 * scattered steps is not that; below about 36 steps a minute the equation
 * stays under one MET and the minute adds nothing.
 */
object WalkingEnergy {

    /** VO2 in ml/kg/min for a minute walked at [stepsPerMinute]. */
    fun oxygenUptake(stepsPerMinute: Int): Double {
        val cadence = stepsPerMinute.toDouble()
        return 1.811 + 0.02014 * cadence + 7.427e-4 * cadence * cadence
    }

    /** Active kilocalories for one minute at [stepsPerMinute], never below zero. */
    fun activeKcalPerMinute(stepsPerMinute: Int, weightKg: Double): Double {
        if (stepsPerMinute <= 0 || weightKg <= 0.0) return 0.0
        val netMets = oxygenUptake(stepsPerMinute) / ML_O2_PER_KG_MIN_PER_MET - 1.0
        return (netMets * weightKg / MINUTES_PER_HOUR).coerceAtLeast(0.0)
    }

    private const val ML_O2_PER_KG_MIN_PER_MET = 3.5
    private const val MINUTES_PER_HOUR = 60.0
}
