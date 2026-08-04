package tech.mmarca.openvitals.domain.preferences

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import java.util.Locale

/**
 * Maps a BCP-47 measurement-system token — the `-u-ms-` regional-preference
 * extension Android 14+ writes, or an ICU MeasurementSystem name — to the unit
 * system OpenVitals displays. UK maps to metric: the UK system is metric-first
 * (road distances and pints are the exceptions, and OpenVitals shows neither
 * as its primary units), and Android's measurement-system setting exposes no
 * mixed option that could distinguish further.
 */
fun unitSystemForMeasurementSystem(measurementSystem: String?): UnitSystem? =
    when (measurementSystem?.lowercase(Locale.ROOT)) {
        "ussystem", "us" -> UnitSystem.IMPERIAL
        "metric", "si", "uksystem", "uk" -> UnitSystem.METRIC
        else -> null
    }

/**
 * The single seam through which a [UnitSystemPreference.SYSTEM] choice becomes
 * a concrete [UnitSystem]. Everything that switches on the unit system reads
 * the already-resolved value from PreferencesRepository (directly or through
 * UnitFormatter) — nothing else consults ICU or the locale.
 */
fun interface SystemUnitSystemProvider {

    fun current(): UnitSystem

    companion object {
        /**
         * The `-u-ms-` extension is checked before ICU because platform ICU
         * versions differ in whether they honour the keyword; the country
         * fallback covers the local-test JVM, where android.icu is a stub.
         */
        val Default = SystemUnitSystemProvider {
            val locale = Locale.getDefault()
            unitSystemForMeasurementSystem(locale.getUnicodeLocaleType("ms"))
                ?: icuMeasurementSystem(locale)
                ?: countryFallback(locale)
        }

        private fun icuMeasurementSystem(locale: Locale): UnitSystem? {
            // LocaleData.getMeasurementSystem exists from API 28; below that
            // (minSdk is 26) the -u-ms- extension or country fallback decides.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            return runCatching {
                when (LocaleData.getMeasurementSystem(ULocale.forLocale(locale))) {
                    LocaleData.MeasurementSystem.US -> UnitSystem.IMPERIAL
                    // SI, and UK as metric-first — see unitSystemForMeasurementSystem.
                    else -> UnitSystem.METRIC
                }
            }.getOrNull()
        }

        private val ImperialCountries = setOf("US", "LR", "MM")

        private fun countryFallback(locale: Locale): UnitSystem =
            if (locale.country.uppercase(Locale.US) in ImperialCountries) {
                UnitSystem.IMPERIAL
            } else {
                UnitSystem.METRIC
            }
    }
}
