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

@Composable
private fun AppLanguage.label(): String =
    when (languageTag) {
        null -> stringResource(R.string.settings_language_system)
        "en" -> stringResource(R.string.settings_language_english)
        "es" -> stringResource(R.string.settings_language_spanish)
        "de" -> stringResource(R.string.settings_language_german)
        "it" -> stringResource(R.string.settings_language_italian)
        "et" -> stringResource(R.string.settings_language_estonian)
        else -> localizedDisplayName(languageTag)
    }

@Composable
private fun localizedDisplayName(languageTag: String): String {
    val displayLocale = LocalLocale.current.platformLocale
    val locale = Locale.forLanguageTag(languageTag)
    return locale
        .getDisplayName(displayLocale)
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString()
        }
}
