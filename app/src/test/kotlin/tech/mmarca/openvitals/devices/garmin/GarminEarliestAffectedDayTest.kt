package tech.mmarca.openvitals.devices.garmin

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.wellness.FitW
import tech.mmarca.openvitals.devices.garmin.wellness.fitTimestamp
import tech.mmarca.openvitals.devices.garmin.wellness.fitWrap

/**
 * The day a watch sync invalidates the Body Energy chain from. Too late leaves a back-filled
 * day frozen; too early spends the rebuild budget on unchanged days.
 */
class GarminEarliestAffectedDayTest {

    @Test
    fun `the oldest dated file decides, not the first or the last`() {
        val earliest = garminEarliestAffectedDay(
            listOf(
                file(at = "2026-06-20T22:00:00Z"),
                file(at = "2026-06-14T03:30:00Z"),
                file(at = "2026-06-18T09:15:00Z"),
            ),
            zone = UTC,
        )

        assertThat(earliest).isEqualTo(LocalDate.of(2026, 6, 14))
    }

    @Test
    fun `files the watch never dated are skipped rather than counted as today`() {
        // The "no date" sentinel is observed on a vivoactive 5. As an instant it would invalidate from the epoch or today.
        val earliest = garminEarliestAffectedDay(
            listOf(
                file(at = null),
                file(at = "2026-06-14T03:30:00Z"),
                file(at = null),
            ),
            zone = UTC,
        )

        assertThat(earliest).isEqualTo(LocalDate.of(2026, 6, 14))
    }

    @Test
    fun `no dated file means no earliest day`() {
        // The date-only helper has no date; the invalidation policy handles this as a full purge.
        assertThat(garminEarliestAffectedDay(listOf(file(at = null)), zone = UTC)).isNull()
        assertThat(garminEarliestAffectedDay(emptyList(), zone = UTC)).isNull()
    }

    @Test
    fun `an undated downloaded file requires full invalidation`() {
        assertThat(
            garminNeedsFullBodyEnergyInvalidation(
                listOf(file(at = "2026-06-14T03:30:00Z"), file(at = null)),
            ),
        ).isTrue()
        assertThat(garminNeedsFullBodyEnergyInvalidation(emptyList())).isFalse()
    }

    @Test
    fun `an undated listing entry is dated from its FIT content instead of purging everything`() {
        // The legacy directory's "no date" sentinel is common; the bytes still say when.
        val dated = file(at = null, bytes = timestampedFit("2026-06-14T03:30:00Z"))

        assertThat(garminNeedsFullBodyEnergyInvalidation(listOf(dated))).isFalse()
        assertThat(garminEarliestAffectedDay(listOf(dated), zone = UTC))
            .isEqualTo(LocalDate.of(2026, 6, 14))
    }

    @Test
    fun `file date is derived from the earliest timestamp across chained FIT files`() {
        val later = timestampedFit("2026-06-18T09:15:00Z")
        val earlier = timestampedFit("2026-06-14T03:30:00Z")

        assertThat(garminFitFileDate(later + earlier))
            .isEqualTo(Instant.parse("2026-06-14T03:30:00Z"))
    }

    @Test
    fun `the day is the wearer's local day, not UTC's`() {
        // 00:30 UTC on the 15th is still the 14th in Madrid. The chain is keyed by local date.
        val file = file(at = "2026-06-15T00:30:00Z")

        assertThat(garminEarliestAffectedDay(listOf(file), zone = ZoneId.of("Pacific/Auckland")))
            .isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(garminEarliestAffectedDay(listOf(file), zone = ZoneId.of("America/New_York")))
            .isEqualTo(LocalDate.of(2026, 6, 14))
    }

    private fun file(at: String?, bytes: ByteArray = ByteArray(0)) = GarminDownloadedFile(
        entry = GarminDirectoryEntry(
            fileIndex = 1,
            type = GarminFileType.SLEEP,
            fileNumber = 7,
            specificFlags = 0,
            fileFlags = 0,
            fileSize = 128,
            fileDate = at?.let(Instant::parse),
        ),
        bytes = bytes,
    )

    private fun timestampedFit(at: String): ByteArray {
        val timestamp = fitTimestamp(Instant.parse(at))
        val data = FitW()
            .def(0, 20, listOf(listOf(253, 4, 0x86)))
            .u8(0)
            .u32(timestamp)
            .toBytes()
        return fitWrap(data)
    }

    private companion object {
        val UTC: ZoneId = ZoneId.of("UTC")
    }
}
