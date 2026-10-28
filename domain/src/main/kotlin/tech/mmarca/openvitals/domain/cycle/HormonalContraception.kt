package tech.mmarca.openvitals.domain.cycle

import java.time.LocalDate

/**
 * The hormonal method a person declared. Only methods that carry hormones are
 * listed: copper IUDs, condoms and diaphragms do not change the cycle. This is
 * context for the cycle features, never a statement about contraception.
 */
enum class HormonalContraceptionKind(val id: String) {
    NONE("none"),
    PILL("pill"),
    HORMONAL_IUD("hormonal_iud"),
    IMPLANT("implant"),
    RING_OR_PATCH("ring_or_patch"),
    INJECTION("injection"),
    OTHER_HORMONAL("other_hormonal"),
    ;

    companion object {
        fun fromId(id: String?): HormonalContraceptionKind? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The declared hormonal method and, when it ended, the day it was stopped.
 * [isInfluencing] tells later cycle features whether hormones may still shape
 * the temperature rhythm.
 */
data class HormonalContraception(
    val kind: HormonalContraceptionKind = HormonalContraceptionKind.NONE,
    val stoppedOn: LocalDate? = null,
) {
    val isDeclared: Boolean get() = kind != HormonalContraceptionKind.NONE

    /**
     * True while a hormonal kind is set with no stop date, and for
     * [InfluenceDaysAfterStop] days after the stop date. The window follows the
     * exclusion criterion of Wang 2025 (Hum Reprod, 10.1093/humrep/deaf005),
     * not a measured washout.
     */
    fun isInfluencing(today: LocalDate): Boolean {
        if (!isDeclared) return false
        val stopped = stoppedOn ?: return true
        return !today.isAfter(stopped.plusDays(InfluenceDaysAfterStop))
    }

    companion object {
        /** Two months, taken as 60 days. */
        const val InfluenceDaysAfterStop = 60L
    }
}
