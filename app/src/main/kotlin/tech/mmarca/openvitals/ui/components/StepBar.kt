package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Wizard bottom bar: an outlined Back (hidden when [onBack] is null) and a
 * filled Next (disabled when [onNext] is null).
 */
@Composable
fun StepBar(
    nextLabel: String,
    onNext: (() -> Unit)?,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LayoutMetrics.screenGutter, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null && backLabel != null) {
            OutlinedButton(onClick = onBack) {
                Text(backLabel)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OpenVitalsFilledButton(
            onClick = onNext ?: {},
            enabled = onNext != null,
        ) {
            Text(nextLabel)
        }
    }
}

/** Centered step header: a large primary icon, a title, and body copy. */
@Composable
fun StepHero(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.huge),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Numbered instruction list: primary-container discs beside each step's text. */
@Composable
fun InstructionSteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(Spacing.xxl),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Convenience alias so wizard footers keep the tonal look for inline actions. */
@Composable
fun StepInlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(text)
    }
}
