package tech.mmarca.openvitals.domain.insights

/**
 * Walking energy per minute from cadence alone. Moore et al. 2021,
 * doi:10.1249/MSS.0000000000002430, Table 2, simple equation:
 *
 *     VO2 (ml/kg/min) = 1.811 + 0.02014 · cadence + 7.427e-4 · cadence²
 *
 * Fitted on 235 adults aged 21–84, level treadmill walking; RMSE 0.60 METs.
 * Active energy is the METs above rest (MET = VO2 / 3.5):
 *
 *     kcal = (VO2 / 3.5 − 1) · kg / 60
 *
 * Rest is subtracted so a daily total with the basal rate does not count it
 * twice. One MET overstates many people's resting cost (Byrne et al. 2005,
 * doi:10.1152/japplphysiol.00023.2004), so this slightly understates.
 *
 * Limits from the paper: level walking only, no hills, stairs or running, a
 * whole minute of steady walking. Below about 36 steps a minute the result
 * is under one MET and the minute adds nothing.
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
