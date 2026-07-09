package tech.mmarca.openvitals.domain.preferences

import androidx.core.os.LocaleListCompat

class AppLanguage private constructor(
    val languageTag: String?,
    val name: String,
) {
    val storageValue: String = languageTag ?: name

    fun toLocaleListCompat(): LocaleListCompat =
        if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is AppLanguage &&
            languageTag == other.languageTag &&
            name == other.name

    override fun hashCode(): Int =
        31 * (languageTag?.hashCode() ?: 0) + name.hashCode()

    override fun toString(): String =
        "AppLanguage(languageTag=$languageTag, name=$name)"

    companion object {
        val SYSTEM = AppLanguage(null, "SYSTEM")
        val ENGLISH = AppLanguage("en", "ENGLISH")
        val SPANISH = AppLanguage("es", "SPANISH")
        val GERMAN = AppLanguage("de", "GERMAN")
        val ITALIAN = AppLanguage("it", "ITALIAN")
        val ESTONIAN = AppLanguage("et", "ESTONIAN")

        private val knownByLegacyName = listOf(
            SYSTEM,
            ENGLISH,
            SPANISH,
            GERMAN,
            ITALIAN,
            ESTONIAN,
        ).associateBy { it.name }

        private val knownByLanguageTag = listOf(
            ENGLISH,
            SPANISH,
            GERMAN,
            ITALIAN,
            ESTONIAN,
        ).associateBy { checkNotNull(it.languageTag) }

        fun fromStorageValue(value: String?): AppLanguage =
            value
                ?.let { stored -> knownByLegacyName[stored] ?: forLanguageTag(stored) }
                ?: SYSTEM

        fun forLanguageTag(languageTag: String): AppLanguage =
            knownByLanguageTag[languageTag] ?: AppLanguage(languageTag, languageTag)

        fun pickerOptions(languageTags: Iterable<String>): List<AppLanguage> =
            listOf(SYSTEM) + languageTags
                .map { tag -> tag.trim() }
                .filter { tag -> tag.isNotEmpty() }
                .distinct()
                .map(::forLanguageTag)
    }
}
