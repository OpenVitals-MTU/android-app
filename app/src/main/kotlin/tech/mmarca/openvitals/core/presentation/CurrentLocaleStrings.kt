package tech.mmarca.openvitals.core.presentation

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Strings in the app's language, for code outside Compose. [Locale.getDefault]
 * follows the in-app language, as [UnitFormatter]'s number formats do.
 */
class CurrentLocaleStrings(private val context: Context) {
    @Volatile
    private var cache: Pair<Locale, Context>? = null

    fun get(@StringRes id: Int): String = localizedContext().getString(id)

    private fun localizedContext(): Context {
        val locale = Locale.getDefault()
        cache?.let { (cachedLocale, cachedContext) -> if (cachedLocale == locale) return cachedContext }
        val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(configuration).also { cache = locale to it }
    }
}
