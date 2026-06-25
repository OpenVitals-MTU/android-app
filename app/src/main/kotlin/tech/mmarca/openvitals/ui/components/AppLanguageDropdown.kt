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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppLanguage

@Composable
fun AppLanguageDropdown(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

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
            AppLanguage.entries.forEach { appLanguage ->
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
    when (this) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
        AppLanguage.SPANISH -> stringResource(R.string.settings_language_spanish)
        AppLanguage.GERMAN -> stringResource(R.string.settings_language_german)
    }
