package tech.mmarca.openvitals.devices.garmin

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

/**
 * The day a watch sync has to invalidate the Body Energy chain from.
 *
 * Body Energy chains across midnight, so a watch handing over a week of sleep
 * and heart-rate invalidates not just those days but every day after them —
 * their seeds came from scores computed without the data. Getting this day
 * wrong by one either leaves a back-filled day frozen at its pre-sync score
 * (too late) or spends the rebuild budget on days nothing changed (too early).
 *
 * Pure and top-level precisely so it can be checked here: the sync service
 * around it needs a radio, a lease and a live GFDI session.
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
        // The "no date" sentinel is real and observed on a vivoactive 5. Treating
        // it as an instant would either invalidate from the epoch or from today,
        // and both are wrong.
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
    fun `a sync where nothing carries a date invalidates nothing`() {
        // Null, not "today": with no idea which days moved, the chain's own
        // settling window stays the only safety net — which is what it is for.
        assertThat(garminEarliestAffectedDay(listOf(file(at = null)), zone = UTC)).isNull()
        assertThat(garminEarliestAffectedDay(emptyList(), zone = UTC)).isNull()
    }

    @Test
    fun `the day is the wearer's local day, not UTC's`() {
        // 23:30 in Madrid on the 14th is 21:30 UTC the same day — but 00:30 UTC
        // on the 15th once the offset goes the other way. The chain is keyed by
        // local date, so the conversion has to use the phone's zone.
        val file = file(at = "2026-06-15T00:30:00Z")

        assertThat(garminEarliestAffectedDay(listOf(file), zone = ZoneId.of("Pacific/Auckland")))
            .isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(garminEarliestAffectedDay(listOf(file), zone = ZoneId.of("America/New_York")))
            .isEqualTo(LocalDate.of(2026, 6, 14))
    }

    private fun file(at: String?) = GarminDownloadedFile(
        entry = GarminDirectoryEntry(
            fileIndex = 1,
            type = GarminFileType.SLEEP,
            fileNumber = 7,
            specificFlags = 0,
            fileFlags = 0,
            fileSize = 128,
            fileDate = at?.let(Instant::parse),
        ),
        bytes = ByteArray(0),
    )

    private companion object {
        val UTC: ZoneId = ZoneId.of("UTC")
    }
}
