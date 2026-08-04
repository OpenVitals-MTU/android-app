package tech.mmarca.openvitals.features.devicesync.store

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * Fields the sync codec puts ON the wire but does not take back off it.
 *
 * The Dart counterparts are the `round-trip > … survives a round trip` cases of
 * test/data/source/health/import_record_mapper_test.dart, filed N/A-FRAMEWORK
 * as "pigeon mapper, Kotlin has no msg layer". The *serializer* is Dart-only;
 * the invariant it protects — a record that goes out comes back the same — is
 * not, and Kotlin's own encode/decode pair is where it lives.
 *
 * A field that is encoded and not decoded is unambiguous: the author meant to
 * carry it, the receiving phone writes the record without it.
 */
class SyncRecordCodecFieldLossTest {

    private val time: Instant = Instant.parse("2026-01-09T07:30:00Z")

    private fun meta() = Metadata.manualEntry(
        device = Device(type = Device.TYPE_PHONE),
        clientRecordId = "sync_0123456789abcdef0123456789abcdef",
    )

    private fun roundTrip(record: BloodPressureRecord): BloodPressureRecord =
        decodeSyncRecord(
            recordType = syncRecordTypeName(record),
            clientRecordId = "sync_0123456789abcdef0123456789abcdef",
            payload = encodeSyncRecordPayload(record),
        ) as BloodPressureRecord

    @Test
    fun `a blood pressure reading keeps its systolic and diastolic across the wire`() {
        val original = BloodPressureRecord(
            time = time,
            zoneOffset = null,
            metadata = meta(),
            systolic = Pressure.millimetersOfMercury(118.0),
            diastolic = Pressure.millimetersOfMercury(76.0),
        )

        val back = roundTrip(original)

        assertThat(back.systolic.inMillimetersOfMercury).isWithin(1e-9).of(118.0)
        assertThat(back.diastolic.inMillimetersOfMercury).isWithin(1e-9).of(76.0)
    }

    /**
     * FAILS TODAY. `encode` writes `bodyPos` and `measLoc`; `decode` never reads
     * them, so BloodPressureRecord's defaults (UNKNOWN) win. A reading taken
     * sitting down on the left upper arm arrives on the peer phone with the
     * posture and the cuff site erased — and those are exactly the fields that
     * make two readings comparable.
     */
    @Test
    fun `a blood pressure reading keeps its body position and cuff site across the wire`() {
        val original = BloodPressureRecord(
            time = time,
            zoneOffset = null,
            metadata = meta(),
            systolic = Pressure.millimetersOfMercury(118.0),
            diastolic = Pressure.millimetersOfMercury(76.0),
            bodyPosition = BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
            measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
        )

        val back = roundTrip(original)

        assertThat(back.bodyPosition)
            .isEqualTo(BloodPressureRecord.BODY_POSITION_SITTING_DOWN)
        assertThat(back.measurementLocation)
            .isEqualTo(BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM)
    }
}
