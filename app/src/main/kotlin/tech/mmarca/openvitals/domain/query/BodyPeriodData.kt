package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.WeightEntry

data class BodyPeriodData(
    val weightEntries: List<WeightEntry> = emptyList(),
    val previousWeightEntries: List<WeightEntry> = emptyList(),
    val baselineWeightEntries: List<WeightEntry> = emptyList(),
    val latestWeightKg: Double? = null,
    val heightCm: Double? = null,
    val heightEntries: List<HeightEntry> = emptyList(),
    val previousHeightEntries: List<HeightEntry> = emptyList(),
    val baselineHeightEntries: List<HeightEntry> = emptyList(),
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val previousBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val baselineBodyFatEntries: List<BodyFatEntry> = emptyList(),
    val latestBodyFatPercent: Double? = null,
    val leanMassKg: Double? = null,
    val leanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val previousLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val baselineLeanMassEntries: List<LeanBodyMassEntry> = emptyList(),
    val bmrKcal: Double? = null,
    val bmrEntries: List<BmrEntry> = emptyList(),
    val previousBmrEntries: List<BmrEntry> = emptyList(),
    val baselineBmrEntries: List<BmrEntry> = emptyList(),
    val boneMassKg: Double? = null,
    val boneMassEntries: List<BoneMassEntry> = emptyList(),
    val previousBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val baselineBoneMassEntries: List<BoneMassEntry> = emptyList(),
    val bodyWaterMassKg: Double? = null,
    val bodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val previousBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
    val baselineBodyWaterMassEntries: List<BodyWaterMassEntry> = emptyList(),
)
