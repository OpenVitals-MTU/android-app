package tech.mmarca.openvitals.domain.preferences

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import java.util.Locale

/**
 * Maps a BCP-47 `-u-ms-` token or an ICU MeasurementSystem name to a unit
 * system. UK maps to metric: it is metric-first for everything shown here.
 */
fun unitSystemForMeasurementSystem(measurementSystem: String?): UnitSystem? =
    when (measurementSystem?.lowercase(Locale.ROOT)) {
        "ussystem", "us" -> UnitSystem.IMPERIAL
        "metric", "si", "uksystem", "uk" -> UnitSystem.METRIC
        else -> null
    }

/** The single seam through which a SYSTEM choice becomes a [UnitSystem]. */
fun interface SystemUnitSystemProvider {

    fun current(): UnitSystem

    companion object {
        /** `-u-ms-` first, then ICU, then the country fallback (the test JVM stubs ICU). */
        val Default = SystemUnitSystemProvider {
            val locale = Locale.getDefault()
            unitSystemForMeasurementSystem(locale.getUnicodeLocaleType("ms"))
                ?: icuMeasurementSystem(locale)
                ?: countryFallback(locale)
        }

        private fun icuMeasurementSystem(locale: Locale): UnitSystem? {
            // LocaleData.getMeasurementSystem exists from API 28.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            return runCatching {
                when (LocaleData.getMeasurementSystem(ULocale.forLocale(locale))) {
                    LocaleData.MeasurementSystem.US -> UnitSystem.IMPERIAL
                    // SI, and UK as metric-first.
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
