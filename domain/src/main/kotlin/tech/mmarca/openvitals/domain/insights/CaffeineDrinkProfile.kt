package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

/**
 * One drink on its own: its peak, what is left now, when half is gone and
 * when it is effectively gone. The same arithmetic as the day curve,
 * asked about one drink.
 */
data class CaffeineDrinkProfile(
    val entry: CaffeineEntry,

    /** This drink's own rise and fall — not the day's. */
    val curve: List<CaffeinePoint>,

    /** The most of this drink ever in the body at once. Lower than the dose: elimination starts early. */
    val peakMg: Double,
    val peakTime: Instant,

    /** What is left of it now. Zero before it was drunk. */
    val currentMg: Double,

    /** When half the peak has gone. Null when the drink has not faded within [CaffeineProfileHorizon]. */
    val halfGoneTime: Instant?,
    val goneTime: Instant?,
) {
    /** Whether this drink is still doing anything worth speaking of. */
    val isActive: Boolean get() = currentMg >= CaffeineNegligibleMg
}

/** Below this a drink is finished. Not zero: the decay never reaches zero. */
const val CaffeineNegligibleMg: Double = 5.0

/** How far past a drink the profile looks. 36 hours: a 200mg drink is still over the floor a day later. */
val CaffeineProfileHorizon: Duration = Duration.ofHours(36)

/** How finely the drink's curve is sampled. */
val CaffeineProfileStep: Duration = Duration.ofMinutes(10)

/**
 * Works [entry] out against the same model the day curve uses, so the two
 * can never disagree. Preferences are normalized for the same reason.
 */
fun caffeineDrinkProfile(
    entry: CaffeineEntry,
    now: Instant,
    preferences: CaffeinePreferences,
    bodyProfile: BodyProfile = BodyProfile(),
): CaffeineDrinkProfile {
    val normalized = preferences.normalized()
    val start = entry.startTime
    val end = start.plus(CaffeineProfileHorizon)

    val curve = mutableListOf<CaffeinePoint>()
    var peakMg = 0.0
    var peakTime = start

    var time = start
    while (!time.isAfter(end)) {
        val value = CaffeineInsightCalculator.contributionMg(entry, time, normalized, bodyProfile)
        curve += CaffeinePoint(time, value)
        if (value > peakMg) {
            peakMg = value
            peakTime = time
        }
        time = time.plus(CaffeineProfileStep)
    }

    return CaffeineDrinkProfile(
        entry = entry,
        curve = curve,
        peakMg = peakMg,
        peakTime = peakTime,
        currentMg = CaffeineInsightCalculator.contributionMg(entry, now, normalized, bodyProfile),
        // Read after the peak: a threshold crossed on the way up is the drink arriving.
        halfGoneTime = curve.fallsBelow(peakTime, peakMg / 2.0),
        goneTime = curve.fallsBelow(peakTime, CaffeineNegligibleMg),
    )
}

/** The highest point of any of [profiles], for a shared y axis. */
fun caffeineProfilePeak(profiles: Iterable<CaffeineDrinkProfile>): Double =
    profiles.fold(0.0) { peak, profile -> maxOf(peak, profile.peakMg) }

/** The first moment after [afterTime] the curve drops below [threshold], or null. */
private fun List<CaffeinePoint>.fallsBelow(afterTime: Instant, threshold: Double): Instant? {
    if (threshold <= 0.0) return null
    for (point in this) {
        if (point.time.isBefore(afterTime)) continue
        if (point.valueMg < threshold) return point.time
    }
    return null
}
