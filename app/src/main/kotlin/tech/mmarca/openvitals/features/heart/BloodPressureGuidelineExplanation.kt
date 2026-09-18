package tech.mmarca.openvitals.features.heart

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BloodPressureCategoryBounds
import tech.mmarca.openvitals.domain.insights.bloodPressureCategoryBounds
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.ReferenceLinkButton
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.Spacing

private const val AhaReadingsUrl =
    "https://www.heart.org/en/health-topics/high-blood-pressure/understanding-blood-pressure-readings"
private const val AccAha2017Url = "https://doi.org/10.1161/HYP.0000000000000065"
private const val Esh2023Url = "https://doi.org/10.1097/HJH.0000000000003480"
private const val Esc2024Url = "https://doi.org/10.1093/eurheartj/ehae178"
private const val Ish2020Url = "https://doi.org/10.1161/HYPERTENSIONAHA.120.15026"

private class BloodPressureReference(@StringRes val titleRes: Int, val url: String)

private fun BloodPressureGuideline.references(): List<BloodPressureReference> = when (this) {
    BloodPressureGuideline.ACC_AHA_2017 -> listOf(
        BloodPressureReference(R.string.interpretation_bp_reference_aha_readings, AhaReadingsUrl),
        BloodPressureReference(R.string.interpretation_bp_reference_acc_aha, AccAha2017Url),
    )
    BloodPressureGuideline.ESH_2023 -> listOf(
        BloodPressureReference(R.string.interpretation_bp_reference_esh, Esh2023Url),
    )
    BloodPressureGuideline.ESC_2024 -> listOf(
        BloodPressureReference(R.string.interpretation_bp_reference_esc, Esc2024Url),
    )
    BloodPressureGuideline.ISH_2020 -> listOf(
        BloodPressureReference(R.string.interpretation_bp_reference_ish, Ish2020Url),
    )
}

@StringRes
internal fun bloodPressureGuidelineLabelRes(guideline: BloodPressureGuideline): Int = when (guideline) {
    BloodPressureGuideline.ACC_AHA_2017 -> R.string.settings_bp_guideline_acc_aha
    BloodPressureGuideline.ESH_2023 -> R.string.settings_bp_guideline_esh
    BloodPressureGuideline.ESC_2024 -> R.string.settings_bp_guideline_esc
    BloodPressureGuideline.ISH_2020 -> R.string.settings_bp_guideline_ish
}

/** How the category is chosen, then the links that back it. Both follow the guideline in Settings. */
@Composable
internal fun BloodPressureGuidelineExplanation(guideline: BloodPressureGuideline) {
    var showCategories by rememberSaveable { mutableStateOf(false) }
    SectionHeader(stringResource(R.string.interpretation_bp_explanation_title))
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(
                    R.string.interpretation_bp_explanation_summary,
                    stringResource(bloodPressureGuidelineLabelRes(guideline)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showCategories) {
                bloodPressureCategoryBounds(guideline).forEach { bounds ->
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = bloodPressureCategoryText(bounds.category),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = bloodPressureBoundsText(bounds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.interpretation_bp_explanation_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            OpenVitalsOutlinedButton(onClick = { showCategories = !showCategories }) {
                Text(
                    stringResource(
                        if (showCategories) {
                            R.string.interpretation_bp_hide_categories
                        } else {
                            R.string.interpretation_bp_show_categories
                        },
                    ),
                )
            }
        }
    }
    SectionHeader(stringResource(R.string.references_backed_links))
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            guideline.references().forEach { reference ->
                ReferenceLinkButton(
                    title = stringResource(reference.titleRes),
                    url = reference.url,
                    modifier = Modifier.padding(vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun bloodPressureBoundsText(bounds: BloodPressureCategoryBounds): String {
    val diastolic = bounds.diastolicMmHg
    return when {
        bounds.isLowest && diastolic != null ->
            stringResource(R.string.interpretation_bp_bounds_lowest, bounds.systolicMmHg, diastolic)
        diastolic != null ->
            stringResource(R.string.interpretation_bp_bounds_either, bounds.systolicMmHg, diastolic)
        else -> stringResource(R.string.interpretation_bp_bounds_systolic, bounds.systolicMmHg)
    }
}
