package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CycleEntryKind

/** One thing to log. The chooser offers each; the day log then shows only the chosen one. */
enum class CycleEntrySection(
    val routeValue: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
    /** The Health Connect records this section writes. Empty when it writes the journal only. */
    val writeKinds: Set<CycleEntryKind>,
) {
    BLEEDING(
        routeValue = "bleeding",
        titleRes = R.string.cycle_choice_bleeding,
        summaryRes = R.string.cycle_choice_bleeding_summary,
        writeKinds = setOf(CycleEntryKind.MENSTRUATION_FLOW, CycleEntryKind.SPOTTING),
    ),
    FEELING(
        routeValue = "feeling",
        titleRes = R.string.cycle_choice_feeling,
        summaryRes = R.string.cycle_choice_feeling_summary,
        writeKinds = emptySet(),
    ),
    PREGNANCY_TEST(
        routeValue = "pregnancy_test",
        titleRes = R.string.cycle_entry_section_hcg,
        summaryRes = R.string.cycle_choice_pregnancy_test_summary,
        writeKinds = emptySet(),
    ),
    OVULATION_TEST(
        routeValue = "ovulation_test",
        titleRes = R.string.cycle_entry_section_ovulation,
        summaryRes = R.string.cycle_choice_ovulation_test_summary,
        writeKinds = setOf(CycleEntryKind.OVULATION_TEST),
    ),
    SEXUAL_ACTIVITY(
        routeValue = "sexual_activity",
        titleRes = R.string.cycle_entry_section_sexual_activity,
        summaryRes = R.string.cycle_choice_sexual_activity_summary,
        writeKinds = setOf(CycleEntryKind.SEXUAL_ACTIVITY),
    ),
    BASAL_BODY_TEMPERATURE(
        routeValue = "bbt",
        titleRes = R.string.cycle_observation_basal_body_temperature,
        summaryRes = R.string.cycle_choice_bbt_summary,
        writeKinds = setOf(CycleEntryKind.BASAL_BODY_TEMPERATURE),
    ),
    CERVICAL_MUCUS(
        routeValue = "cervical_mucus",
        titleRes = R.string.cycle_observation_cervical_mucus,
        summaryRes = R.string.cycle_choice_cervical_mucus_summary,
        writeKinds = setOf(CycleEntryKind.CERVICAL_MUCUS),
    ),
    ;

    companion object {
        /** Null for a missing or unknown value: the day log then shows every section. */
        fun fromRoute(value: String?): CycleEntrySection? = entries.firstOrNull { it.routeValue == value }
    }
}

internal val CycleEntrySection.icon: ImageVector
    get() = when (this) {
        CycleEntrySection.BLEEDING -> Icons.Outlined.WaterDrop
        CycleEntrySection.FEELING -> Icons.Outlined.Mood
        CycleEntrySection.PREGNANCY_TEST -> Icons.Outlined.Science
        CycleEntrySection.OVULATION_TEST -> Icons.Outlined.Biotech
        CycleEntrySection.SEXUAL_ACTIVITY -> Icons.Outlined.Favorite
        CycleEntrySection.BASAL_BODY_TEMPERATURE -> Icons.Outlined.DeviceThermostat
        CycleEntrySection.CERVICAL_MUCUS -> Icons.Outlined.Opacity
    }
