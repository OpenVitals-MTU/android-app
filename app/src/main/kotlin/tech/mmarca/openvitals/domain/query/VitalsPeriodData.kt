package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry

data class VitalsPeriodData(
    val missingVitalsPermissions: Set<String> = emptySet(),
    val bloodPressure: List<BloodPressureEntry> = emptyList(),
    val previousBloodPressure: List<BloodPressureEntry> = emptyList(),
    val baselineBloodPressure: List<BloodPressureEntry> = emptyList(),
    val spO2: List<SpO2Entry> = emptyList(),
    val previousSpO2: List<SpO2Entry> = emptyList(),
    val baselineSpO2: List<SpO2Entry> = emptyList(),
    val respiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val previousRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val baselineRespiratoryRate: List<RespiratoryRateEntry> = emptyList(),
    val bodyTemperature: List<BodyTempEntry> = emptyList(),
    val previousBodyTemperature: List<BodyTempEntry> = emptyList(),
    val baselineBodyTemperature: List<BodyTempEntry> = emptyList(),
    val vo2Max: List<Vo2MaxEntry> = emptyList(),
    val previousVo2Max: List<Vo2MaxEntry> = emptyList(),
    val baselineVo2Max: List<Vo2MaxEntry> = emptyList(),
    val bloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val previousBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val baselineBloodGlucose: List<BloodGlucoseEntry> = emptyList(),
    val skinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val previousSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
    val baselineSkinTemperature: List<SkinTemperatureEntry> = emptyList(),
)
