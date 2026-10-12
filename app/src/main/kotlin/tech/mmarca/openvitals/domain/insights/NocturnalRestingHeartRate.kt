package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * A night's resting heart rate: the lowest 30-minute mean of the per-minute
 * heart rate between sleep onset and the end of the night.
 *
 * No standard definition exists. The lowest 30-minute mean is Garmin's, as
 * reported by Dial et al. 2025 (doi:10.14814/phy2.70527): nocturnal resting
 * heart rate from consumer wearables agreed with an ECG chest strap (CCC
 * 0.86–0.98). Target value: supine, just after waking (Quer et al. 2020,
 * doi:10.1371/journal.pone.0227709).
 *
 * Not validated against a clinical seated measurement, which runs higher
 * than a sleeping one. A nocturnal estimate, not a clinical value.
 */
object NocturnalRestingHeartRate {

    const val WINDOW_MINUTES = 30

    /** Two thirds of the window must have a reading, so a lost pulse cannot make a low. */
    const val MIN_MINUTES_WITH_HEART_RATE = 20

    /**
     * The lowest window mean in `[from, to)`, rounded to whole beats. Null
     * when no window has enough readings.
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
