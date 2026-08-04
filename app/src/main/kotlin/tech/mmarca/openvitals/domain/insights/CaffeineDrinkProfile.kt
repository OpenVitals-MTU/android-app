package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePoint
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences

/**
 * One drink, on its own: what it did, what it is still doing, and when it will be done.
 *
 * Nothing here is new arithmetic. [CaffeineInsightCalculator.contributionMg] already answers
 * "how much of THIS drink is still in you at that moment" — it has to, because the whole-day
 * curve is the sum of exactly these. All this does is ask it about one drink, repeatedly,
 * which is the difference between a number in a total and a thing you can understand.
 *
 * The question a drinker actually has is "will this coffee still be in me at bedtime", so
 * that is the question this answers: the peak and when it fell, what is left right now, when
 * half of it will be gone, and when it will effectively be gone.
 */
data class CaffeineDrinkProfile(
    val entry: CaffeineEntry,

    /** This drink's own rise and fall — not the day's. */
    val curve: List<CaffeinePoint>,

    /**
     * The most of this drink that was ever in the body at once, and when.
     *
     * Lower than the dose, always: absorption takes time, and elimination has begun before
     * absorption has finished. A 95mg coffee never puts 95mg in you at once.
     */
    val peakMg: Double,
    val peakTime: Instant,

    /** What is left of it now. Zero before it was drunk. */
    val currentMg: Double,

    /**
     * When half of the peak has gone, and when what remains stops mattering.
     * Null when the drink has not faded within [CaffeineProfileHorizon].
     */
    val halfGoneTime: Instant?,
    val goneTime: Instant?,
) {
    /** Whether this drink is still doing anything worth speaking of. */
    val isActive: Boolean get() = currentMg >= CaffeineNegligibleMg
}

/**
 * Below this, a drink is finished. Not zero: the model decays exponentially and never
 * reaches zero, so a "gone" that waited for zero would never come.
 */
const val CaffeineNegligibleMg: Double = 5.0

/**
 * How far past a drink the profile looks before giving up on it fading.
 *
 * Thirty-six hours, not twenty-four. A large dose really does take longer than a day to fall
 * below [CaffeineNegligibleMg] — a 200mg energy drink is still carrying more than that a full
 * day later — and a 24-hour horizon simply reported "we do not know when this goes away" for
 * exactly the drinks whose staying power is most worth knowing.
 */
val CaffeineProfileHorizon: Duration = Duration.ofHours(36)

/** How finely the drink's curve is sampled. */
val CaffeineProfileStep: Duration = Duration.ofMinutes(10)

/**
 * Works [entry] out on its own, against the same model the whole-day curve uses — so the
 * number a drink shows here and the bump it makes in the day's curve are the same number, and
 * can never disagree. Preferences are normalized for the same reason: the day curve normalizes
 * before it samples.
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
        // Both are read off the curve AFTER the peak. Before it the drink is still being
        // absorbed and is on its way up — a threshold crossed on the way up is not the drink
        // fading, it is the drink arriving.
        halfGoneTime = curve.fallsBelow(peakTime, peakMg / 2.0),
        goneTime = curve.fallsBelow(peakTime, CaffeineNegligibleMg),
    )
}

/**
 * The highest point of any of [profiles] — what a set of per-drink charts should share as
 * their y axis, so a small drink next to a large one LOOKS small.
 */
fun caffeineProfilePeak(profiles: Iterable<CaffeineDrinkProfile>): Double =
    profiles.fold(0.0) { peak, profile -> maxOf(peak, profile.peakMg) }

/**
 * The first moment after [afterTime] that the curve drops below [threshold], or null if it
 * never does within the horizon.
 */
private fun List<CaffeinePoint>.fallsBelow(afterTime: Instant, threshold: Double): Instant? {
    if (threshold <= 0.0) return null
    for (point in this) {
        if (point.time.isBefore(afterTime)) continue
        if (point.valueMg < threshold) return point.time
    }
    return null
}
