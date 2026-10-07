package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.units.Percentage
import java.time.Instant
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric
import tech.mmarca.openvitals.domain.model.GarminWellnessSample

/**
 * Pulse Ox and the five-minute HRV values have Health Connect types. Ids are
 * keyed on the instant and namespaced apart from the Health Snapshot SpO2
 * and the nightly HRV summary, which stay their own records.
 */
fun fitExtrasImportRecords(extras: FitWellnessExtras): List<Record> = buildList {
    for ((at, percent) in extras.spo2) {
        add(
            OxygenSaturationRecord(
                time = at,
                zoneOffset = null,
                percentage = Percentage(percent.toDouble()),
                metadata = importMetadata("garmin_fit_spo2_${at.toEpochMilli()}"),
            ),
        )
    }
    for ((at, rmssd) in extras.hrvValues) {
        add(
            HeartRateVariabilityRmssdRecord(
                time = at,
                zoneOffset = null,
                heartRateVariabilityMillis = rmssd,
                metadata = importMetadata("garmin_fit_hrv_value_${at.toEpochMilli()}"),
            ),
        )
    }
}

/**
 * The thresholds and scores, which Health Connect has no type for, as
 * watch-only samples at the watch's own time. Restless moments are keyed to
 * [nightStart], beside the night's other verdicts, and dropped without one.
 */
fun fitExtrasWatchOnlySamples(extras: FitWellnessExtras, nightStart: Instant?): List<GarminWellnessSample> =
    buildList {
        fun add(metric: GarminWellnessMetric, at: Instant, value: Int?) {
            if (value != null) add(GarminWellnessSample(metric, at, value.toLong()))
        }
        extras.functionalMetrics?.let { m ->
            add(GarminWellnessMetric.FUNCTIONAL_THRESHOLD_POWER, m.time, m.functionalThresholdPowerWatts)
            add(GarminWellnessMetric.LACTATE_THRESHOLD_POWER, m.time, m.runningLactateThresholdPowerWatts)
            add(GarminWellnessMetric.LACTATE_THRESHOLD_HEART_RATE, m.time, m.runningLactateThresholdHeartRateBpm)
        }
        extras.hillScore?.let { h ->
            add(GarminWellnessMetric.HILL_SCORE, h.time, h.score)
            add(GarminWellnessMetric.HILL_STRENGTH, h.time, h.strength)
            add(GarminWellnessMetric.HILL_ENDURANCE, h.time, h.endurance)
        }
        extras.enduranceScore?.let { add(GarminWellnessMetric.ENDURANCE_SCORE, it.time, it.score) }
        if (nightStart != null) add(GarminWellnessMetric.SLEEP_RESTLESS_MOMENTS, nightStart, extras.restlessMoments)
    }
