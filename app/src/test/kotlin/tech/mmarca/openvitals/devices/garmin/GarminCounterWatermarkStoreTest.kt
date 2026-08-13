package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONArray
import tech.mmarca.openvitals.devices.FakeSharedPreferences

/**
 * The watermark is what lets a day's counters be written as intraday intervals
 * across many syncs: each one differences from where the last stopped. It has
 * to survive a restart, and it has to be right — a wrong watermark either
 * loses a stretch of the day or writes it twice.
 */
class GarminCounterWatermarkStoreTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: GarminCounterWatermarkStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = GarminCounterWatermarkStore(prefs)
    }

    private fun mark(time: Instant, steps: Int): FitCounterWatermark =
        FitCounterWatermark(time = time, steps = steps, distance = 10, calories = 5)

    /** Seeds raw stored lines, the restart-shaped way the store reads them. */
    private fun seedLines(vararg lines: String) {
        prefs.edit()
            .putString("garmin_counter_watermarks", JSONArray(lines.toList()).toString())
            .apply()
        store = GarminCounterWatermarkStore(prefs)
    }

    @Test
    fun `starts empty and round-trips through storage`() {
        assertTrue(store.load().isEmpty())

        val at = Instant.ofEpochMilli(1_784_000_000_000)
        store.save(mapOf("2026-07-25" to mark(at, 24843)))

        // A second store over the same prefs is the real round-trip.
        val reloaded = GarminCounterWatermarkStore(prefs).load()
        assertEquals(at, reloaded.getValue("2026-07-25").time)
        assertEquals(24843, reloaded.getValue("2026-07-25").steps)
        assertEquals(10, reloaded.getValue("2026-07-25").distance)
        assertEquals(5, reloaded.getValue("2026-07-25").calories)
    }

    @Test
    fun `a later sync of the same day moves the watermark forward`() {
        store.save(mapOf("2026-07-25" to mark(Instant.ofEpochMilli(1_784_000_000_000), 24000)))
        store.save(mapOf("2026-07-25" to mark(Instant.ofEpochMilli(1_784_003_600_000), 24843)))

        assertEquals(24843, store.load().getValue("2026-07-25").steps)
    }

    @Test
    fun `saving one day does not forget the others`() {
        // A sync touches the days its files covered. The rest are still true.
        store.save(
            mapOf(
                "2026-07-24" to mark(Instant.ofEpochMilli(1_783_900_000_000), 9000),
                "2026-07-25" to mark(Instant.ofEpochMilli(1_784_000_000_000), 24000),
            ),
        )
        store.save(mapOf("2026-07-25" to mark(Instant.ofEpochMilli(1_784_003_600_000), 24843)))

        val marks = store.load()
        assertTrue(marks.keys.containsAll(listOf("2026-07-24", "2026-07-25")))
        assertEquals(9000, marks.getValue("2026-07-24").steps)
    }

    @Test
    fun `keeps the most recent days and drops the oldest`() {
        store.save(
            (1..70).associate { day ->
                "2026-05-${day.toString().padStart(2, '0')}" to
                    mark(Instant.ofEpochMilli(1_777_000_000_000), day)
            },
        )

        val marks = store.load()
        assertEquals(60, marks.size)
        // Sorted by day key, so what goes is the oldest.
        assertFalse(marks.containsKey("2026-05-01"))
        assertTrue(marks.containsKey("2026-05-70"))
    }

    @Test
    fun `an unreadable entry is dropped rather than guessed at`() {
        // Half a watermark would either lose a day's steps or write them
        // twice. Re-importing the day from its start is the safer of the two
        // mistakes.
        seedLines(
            "2026-07-25|not-a-number|1|2|3",
            "2026-07-24|too|few",
            "2026-07-23|1784000000000|100|200|300",
        )

        val marks = store.load()
        assertEquals(setOf("2026-07-23"), marks.keys)
        assertEquals(100, marks.getValue("2026-07-23").steps)
    }

    @Test
    fun `a watermark written before the legacy flag reads as not retired`() {
        // Five fields is the pre-legacyRetired form, and dropping those lines
        // would re-import each day from midnight. Reading them as NOT retired
        // is both lossless and correct: a day written under the old form still
        // has its whole-day record, and is exactly the day whose next sync
        // must supersede it.
        seedLines(
            "2026-07-25|1784000000000|100|200|300",
            "2026-07-24|1784000000000|100|200|300|1",
        )

        val marks = store.load()
        assertFalse(marks.getValue("2026-07-25").legacyRetired)
        assertEquals(100, marks.getValue("2026-07-25").steps)
        assertTrue(marks.getValue("2026-07-24").legacyRetired)
    }

    @Test
    fun `the legacy flag survives a save and reload`() {
        store.save(
            mapOf(
                "2026-07-25" to FitCounterWatermark(
                    time = Instant.ofEpochMilli(1_784_000_000_000),
                    steps = 24843,
                    legacyRetired = true,
                ),
            ),
        )

        assertTrue(store.load().getValue("2026-07-25").legacyRetired)
    }

    @Test
    fun `the per-type maps survive a round trip`() {
        store.save(
            mapOf(
                "2026-07-30" to FitCounterWatermark(
                    time = Instant.ofEpochMilli(1_785_000_000_000),
                    steps = 3400,
                    distance = 250000,
                    calories = 120,
                    stepsByType = mapOf(0 to 400, 6 to 3000),
                    distanceByType = mapOf(6 to 250000),
                    caloriesByType = emptyMap(),
                ),
            ),
        )

        val mark = GarminCounterWatermarkStore(prefs).load().getValue("2026-07-30")
        assertEquals(mapOf(0 to 400, 6 to 3000), mark.stepsByType)
        assertEquals(mapOf(6 to 250000), mark.distanceByType)
        assertEquals(emptyMap<Int, Int>(), mark.caloriesByType)
    }

    @Test
    fun `the open-bucket seed values survive a round trip`() {
        store.save(
            mapOf(
                "2026-07-31" to FitCounterWatermark(
                    time = Instant.ofEpochMilli(1_785_100_000_000),
                    steps = 3400,
                    stepsByType = mapOf(6 to 3400),
                    openBucketSteps = 120,
                    openBucketDistance = 9500,
                    openBucketCalories = 8,
                ),
            ),
        )

        val mark = GarminCounterWatermarkStore(prefs).load().getValue("2026-07-31")
        assertEquals(120, mark.openBucketSteps)
        assertEquals(9500, mark.openBucketDistance)
        assertEquals(8, mark.openBucketCalories)
    }

    @Test
    fun `the open bucket's own start survives a round trip`() {
        store.save(
            mapOf(
                "2026-07-31" to FitCounterWatermark(
                    time = Instant.ofEpochMilli(1_785_100_000_000),
                    openBucketSteps = 120,
                    openBucketStart = Instant.ofEpochMilli(1_785_099_400_000),
                ),
            ),
        )

        val mark = GarminCounterWatermarkStore(prefs).load().getValue("2026-07-31")
        assertEquals(Instant.ofEpochMilli(1_785_099_400_000), mark.openBucketStart)
    }

    @Test
    fun `a line from before the open-bucket start loads it as null`() {
        // Correct, not merely tolerated: those versions began every bucket on
        // the grid, which is exactly what null means to the importer.
        seedLines("2026-07-30|1753822800000|3400|0|0|1|6:3400|-|-|120|9500|8")

        val mark = store.load().getValue("2026-07-30")
        assertEquals(120, mark.openBucketSteps)
        assertNull(mark.openBucketStart)
    }

    @Test
    fun `a line from before the open-bucket seeds loads them as zero`() {
        // Correct, not merely tolerated: those versions withheld the open
        // bucket, so there is nothing already written for the seed to restate.
        seedLines("2026-07-30|1753822800000|3400|0|0|1|6:3400|-|-")

        val mark = store.load().getValue("2026-07-30")
        assertEquals(mapOf(6 to 3400), mark.stepsByType)
        assertEquals(0, mark.openBucketSteps)
        assertEquals(0, mark.openBucketDistance)
        assertEquals(0, mark.openBucketCalories)
    }

    @Test
    fun `a line from before the maps loads with them null, and stays null`() {
        // Null is "types unknowable", an empty map is "no types" — the
        // importer adopts silently on the first and counts fully on the
        // second, so a re-save must not flatten one into the other.
        seedLines("2026-07-29|1753822800000|6123|0|0|1")

        assertNull(store.load().getValue("2026-07-29").stepsByType)

        store.save(mapOf("2026-07-30" to mark(Instant.ofEpochMilli(1_785_000_000_000), 100)))
        assertNull(store.load().getValue("2026-07-29").stepsByType)
        assertEquals(6123, store.load().getValue("2026-07-29").steps)
    }

    @Test
    fun `an unreadable type map drops the line, not just the map`() {
        seedLines("2026-07-30|1753822800000|100|0|0|0|not-a-map|-|-")

        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `clear forgets everything`() {
        // For a Health Connect wipe: the records the watermarks describe are
        // gone, so trusting them would leave every day short forever.
        store.save(mapOf("2026-07-25" to mark(Instant.ofEpochMilli(1_784_000_000_000), 24843)))

        store.clear()

        assertTrue(store.load().isEmpty())
    }
}
