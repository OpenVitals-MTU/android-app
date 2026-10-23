package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** What a declared context may change. */
enum class ContextGroup {
    /** Affects when cycles arrive. Widens the estimate; never moves its central date. */
    TIMING,

    /** Affects bleeding and pain, not timing. Adds observations to the day log only. */
    OBSERVATION,
}

/**
 * A context the user declared about themselves. It is never a diagnosis,
 * never inferred, and never shown as a conclusion.
 */
enum class TrackingContext(val id: String, val group: ContextGroup) {
    PMS("pms", ContextGroup.OBSERVATION),
    PMDD("pmdd", ContextGroup.OBSERVATION),

    /**
     * Observation only. Short cycles are a risk factor for endometriosis, not a
     * consequence of it, so using it to widen estimates would invert the evidence.
     */
    ENDOMETRIOSIS("endometriosis", ContextGroup.OBSERVATION),
    PCOS("pcos", ContextGroup.TIMING),
    PERIMENOPAUSE("perimenopause", ContextGroup.TIMING),
    THYROID("thyroid", ContextGroup.TIMING),
    ;

    companion object {
        fun fromId(id: String?): TrackingContext? = entries.firstOrNull { it.id == id }
    }
}

/** The user's declared contexts, age band and hormonal method. All are optional. */
data class CycleTrackingProfile(
    val contexts: Set<TrackingContext> = emptySet(),
    val ageBand: AgeBand? = null,
    val hormonalContraception: HormonalContraception = HormonalContraception(),
) {
    /** Nothing declared: a phone in this state accepts a profile from another. */
    val isEmpty: Boolean
        get() = contexts.isEmpty() && ageBand == null && !hormonalContraception.isDeclared

    /** Each part this profile has not declared is taken from [other]; a declared part is never overwritten. */
    fun filledFrom(other: CycleTrackingProfile): CycleTrackingProfile = copy(
        contexts = contexts.ifEmpty { other.contexts },
        ageBand = ageBand ?: other.ageBand,
        hormonalContraception = if (hormonalContraception.isDeclared) hormonalContraception else other.hormonalContraception,
    )

    /** An active pill scheme means the pill; a stop date no longer applies. */
    fun withPillScheme(): CycleTrackingProfile =
        copy(hormonalContraception = HormonalContraception(HormonalContraceptionKind.PILL))

    val hasTimingContext: Boolean
        get() = contexts.any { it.group == ContextGroup.TIMING }

    /** The body profile's birth year sets the band when there is one; the declared band is the fallback. */
    fun resolved(bodyProfile: BodyProfile?, today: LocalDate = LocalDate.now()): CycleTrackingProfile =
        copy(ageBand = bodyProfile?.ageYears(today)?.let(AgeBand::forAge) ?: ageBand)
}
