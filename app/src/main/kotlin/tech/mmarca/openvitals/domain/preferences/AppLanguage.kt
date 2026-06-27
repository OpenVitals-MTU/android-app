package tech.mmarca.openvitals.domain.preferences

import androidx.core.os.LocaleListCompat

enum class AppLanguage(
    val languageTag: String?,
) {
    SYSTEM(null),
    ENGLISH("en"),
    SPANISH("es"),
    GERMAN("de"),
    ITALIAN("it");

    fun toLocaleListCompat(): LocaleListCompat =
        if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
}
