package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.DailyBloodPressurePoint
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric

data class VitalsPeriodData(
    val missingVitalsPermissions: Set<String> = emptySet(),
    /**
     * Overview metrics whose daily read blew its per-metric budget on this
     * range. Their lists arrive empty; the UI says so instead of blanking the
     * whole screen.
     */
    val timedOutMetrics: Set<VitalsPeriodMetric> = emptySet(),
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
    // Non-day overview loads carry one aggregated point per local day instead of
    // raw entries, plus the window's true latest reading per metric for the cards.
    val bloodPressureDaily: List<DailyBloodPressurePoint> = emptyList(),
    val spO2Daily: List<DailyVitalPoint> = emptyList(),
    val respiratoryRateDaily: List<DailyVitalPoint> = emptyList(),
    val bodyTemperatureDaily: List<DailyVitalPoint> = emptyList(),
    val vo2MaxDaily: List<DailyVitalPoint> = emptyList(),
    val bloodGlucoseDaily: List<DailyVitalPoint> = emptyList(),
    val skinTemperatureDaily: List<DailyVitalPoint> = emptyList(),
    val latestBloodPressure: BloodPressureEntry? = null,
    val latestSpO2: SpO2Entry? = null,
    val latestRespiratoryRate: RespiratoryRateEntry? = null,
    val latestBodyTemperature: BodyTempEntry? = null,
    val latestVo2Max: Vo2MaxEntry? = null,
    val latestBloodGlucose: BloodGlucoseEntry? = null,
    val latestSkinTemperature: SkinTemperatureEntry? = null,
)
