package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import java.util.Locale

@Composable
fun AppLanguageDropdown(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val pickerLanguageTags = stringArrayResource(R.array.translation_picker_language_tags)
    val pickerOptions = AppLanguage.pickerOptions(pickerLanguageTags.asIterable())

    Box(modifier = modifier) {
        OpenVitalsOutlinedButton(onClick = { expanded = true }) {
            Text(selected.label())
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            pickerOptions.forEach { appLanguage ->
                DropdownMenuItem(
                    text = { Text(appLanguage.label()) },
                    onClick = {
                        expanded = false
                        onSelect(appLanguage)
                    },
                )
            }
        }
    }
}

/**
 * Every language reads in its own language: someone holding a phone in a
 * language they cannot read is looking for "Eesti", not "Estnisch". Only
 * "System default" is translated.
 */
@Composable
private fun AppLanguage.label(): String =
    when (languageTag) {
        null -> stringResource(R.string.settings_language_system)
        else -> autonym(languageTag)
    }

private fun autonym(languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    // Titlecased in the language's own locale: Turkish dotted I makes the display locale wrong.
    return locale
        .getDisplayLanguage(locale)
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
}
