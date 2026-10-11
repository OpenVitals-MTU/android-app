package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.LocalDate
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.insights.NocturnalRestingHeartRate
import tech.mmarca.openvitals.domain.insights.WearSleepEstimator
import tech.mmarca.openvitals.domain.model.WearSleepMinute

/**
 * One nocturnal resting heart rate per estimated night, from the minutes the
 * sleep import already pulled; see [NocturnalRestingHeartRate]. Stamped at
 * the end of the night, keyed on the night, with the sync time as version so
 * the later estimate replaces the earlier, as the sleep session does.
 *
 * Pure: no Android, so it is testable without a device.
 */
object WearOsRestingHeartRateImport {

    const val CLIENT_ID_PREFIX = "wearos_rhr_est_"

    fun clientRecordId(night: LocalDate): String = "$CLIENT_ID_PREFIX$night"

    fun isOwnRecordId(clientRecordId: String?): Boolean = clientRecordId?.startsWith(CLIENT_ID_PREFIX) == true

    /** The record for [estimated], or null when its sleep held too few heart rate readings. */
    fun record(
        minutes: List<WearSleepMinute>,
        estimated: WearSleepEstimator.Night,
        night: LocalDate,
        offset: ZoneOffset,
        version: Long,
    ): RestingHeartRateRecord? {
        val session = estimated.session
        val bpm = NocturnalRestingHeartRate.of(minutes, session.onset, session.end) ?: return null
        return RestingHeartRateRecord(
            time = session.end,
            zoneOffset = offset,
            beatsPerMinute = bpm.toLong(),
            metadata = Metadata.manualEntry(
                device = Device(type = Device.TYPE_PHONE),
                clientRecordId = clientRecordId(night),
                clientRecordVersion = version,
            ),
        )
    }
}
