package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * A night's resting heart rate: the lowest 30-minute mean of the per-minute
 * heart rate between sleep onset and the end of the night.
 *
 * There is no standard definition of a wearable's daily resting heart rate.
 * The lowest 30-minute average is how Garmin defines it, as reported by Dial
 * and colleagues, "Validation of nocturnal resting heart rate and heart rate
 * variability in consumer wearables", Physiol Rep 2025,
 * doi:10.14814/phy2.70527, who found nocturnal resting heart rate from
 * consumer wearables close to an ECG chest strap (CCC 0.86–0.98). Quer and
 * colleagues, PLOS ONE 2020, doi:10.1371/journal.pone.0227709, describe the
 * value a daily wearable resting heart rate is meant to approach: supine,
 * just after waking. Night-time heart rate carries prognostic weight of its
 * own (Johansen and colleagues, Eur Heart J 2013, doi:10.1093/eurheartj/ehs449).
 *
 * Not validated as such: no peer-reviewed study compares this exact rule
 * with a clinical seated measurement, and a sleeping heart rate sits below
 * one taken in a clinic. It is labelled a nocturnal estimate, not a
 * clinical value.
 */
object NocturnalRestingHeartRate {

    const val WINDOW_MINUTES = 30

    /** Two thirds of the window must carry a reading, so a lost pulse cannot make a low. */
    const val MIN_MINUTES_WITH_HEART_RATE = 20

    /**
     * The lowest window mean in `[from, to)`, rounded to whole beats, or
     * null when no window has enough readings.
     */
    fun of(minutes: List<WearSleepMinute>, from: Instant, to: Instant): Int? {
        val byMinute = HashMap<Long, Float>()
        for (minute in minutes) {
            val bpm = minute.heartRate ?: continue
            if (minute.time.isBefore(from) || !minute.time.isBefore(to)) continue
            byMinute[minute.time.epochSecond / SECONDS_PER_MINUTE] = bpm
        }
        if (byMinute.size < MIN_MINUTES_WITH_HEART_RATE) return null
        val first = from.epochSecond / SECONDS_PER_MINUTE
        val last = (to.epochSecond - 1) / SECONDS_PER_MINUTE
        var lowest: Double? = null
        var start = first
        while (start + WINDOW_MINUTES - 1 <= last) {
            var sum = 0.0
            var count = 0
            for (minute in start until start + WINDOW_MINUTES) {
                val bpm = byMinute[minute] ?: continue
                sum += bpm
                count++
            }
            if (count >= MIN_MINUTES_WITH_HEART_RATE) {
                val mean = sum / count
                if (lowest == null || mean < lowest) lowest = mean
            }
            start++
        }
        return lowest?.roundToInt()
    }

    private const val SECONDS_PER_MINUTE = 60L
}
