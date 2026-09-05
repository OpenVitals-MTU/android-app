package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange

/**
 * Every ranged read, over every range, against the real corpus.
 *
 * One table for all reads: a day, a week, a month and a year return over real data,
 * an empty day returns empty but valid, and nothing comes back from outside the window.
 * It sits at the reader layer because the repositories need Android; the window bugs
 * live here anyway.
 *
 * `today` is pinned to the corpus's last day, so windows look backwards into real records.
 */
class FixtureRangeLoadTest {

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    /**
     * One ranged read, reduced to the instants it answered with.
     *
     * @param corpusCovered the fixture holds records this read can find. It carries no resting
     *   heart rate, HRV, nutrition, mindfulness, body or cycle records.
     * @param gapFilled the read emits one row per date, so an empty window answers non-empty.
     */
    private class RangedRead(
        val name: String,
        val corpusCovered: Boolean,
        val gapFilled: Boolean = false,
        val read: suspend (Readers, Instant, Instant) -> List<Instant>,
    )

    private class Readers(support: HealthConnectReaderSupport) {
        val activity = ActivityHealthReader(support, APP_PACKAGE)
        val heart = HeartHealthReader(support, "tech.mmarca.openvitals")
        val hydration = HydrationHealthReader(support, APP_PACKAGE)
        val nutrition = NutritionHealthReader(support, APP_PACKAGE)
        val sleep = SleepHealthReader(support)
        val body = BodyHealthReader(support, APP_PACKAGE)
        val mindfulness = MindfulnessHealthReader(support, APP_PACKAGE)
        val cycle = CycleHealthReader(support, APP_PACKAGE)
    }

    private val reads = listOf(
        RangedRead("heart samples", corpusCovered = true) { r, s, e ->
            r.heart.readHeartRateSamples(s, e).map { it.time }
        },
        RangedRead("heart daily summaries", corpusCovered = true) { r, s, e ->
            r.heart.readDailyHeartRateSummaries(s.date(), e.lastDate()).map { it.date.midday() }
        },
        RangedRead("resting heart rate samples", corpusCovered = true) { r, s, e ->
            r.heart.readRestingHeartRateSamples(s, e).map { it.time }
        },
        RangedRead("daily resting heart rate", corpusCovered = true) { r, s, e ->
            r.heart.readDailyRestingHR(s.date(), e.lastDate()).map { it.date.midday() }
        },
        RangedRead("hrv samples", corpusCovered = true) { r, s, e ->
            r.heart.readHrvSamples(s, e).map { it.time }
        },
        RangedRead("daily steps", corpusCovered = true) { r, s, e ->
            r.activity.readDailySteps(s.date(), e.lastDate()).map { it.date.midday() }
        },
        RangedRead("daily hydration", corpusCovered = true, gapFilled = true) { r, s, e ->
            r.hydration.readDailyHydration(s.date(), e.lastDate()).map { it.date.midday() }
        },
        RangedRead("hydration entries", corpusCovered = true) { r, s, e ->
            r.hydration.readHydrationEntries(s, e).map { it.startTime }
        },
        RangedRead("daily macros", corpusCovered = false) { r, s, e ->
            r.nutrition.readDailyMacros(s.date(), e.lastDate()).map { it.date.midday() }
        },
        RangedRead("nutrition entries", corpusCovered = false) { r, s, e ->
            r.nutrition.readNutritionEntries(s, e).map { it.time }
        },
        RangedRead("sleep sessions", corpusCovered = true) { r, s, e ->
            r.sleep.readSleepSessions(s, e).map { it.startTime }
        },
        RangedRead("mindfulness sessions", corpusCovered = false) { r, s, e ->
            r.mindfulness.readMindfulnessSessions(s, e).map { it.startTime }
        },
        RangedRead("weight entries", corpusCovered = false) { r, s, e ->
            r.body.readWeightEntries(s, e).map { it.time }
        },
        RangedRead("body fat entries", corpusCovered = false) { r, s, e ->
            r.body.readBodyFatEntries(s, e).map { it.time }
        },
        RangedRead("menstruation flow entries", corpusCovered = false) { r, s, e ->
            r.cycle.readMenstruationFlowEntries(s, e).map { it.time }
        },
    )

    @Test
    fun `the ranged reads answer inside their window, over real data, and never shrink`() =
        runTest(timeout = 5.minutes) {
            // One walk of the table for all three guarantees; a walk over 4,370 records is not cheap.
            // Violations are collected so one broken read reports every range it broke.
            val readers = readers(seeded())
            val anchor = corpusLastDay()
            val problems = mutableListOf<String>()

            for (read in reads) {
                var narrower = 0
                for (range in TimeRange.entries) {
                    val (start, end) = range.window(anchor)
                    val answered = read.read(readers, start, end)
                    val outside = answered.count { it < start || it >= end }

                    if (outside > 0) {
                        problems += "${read.name}/${range.label}: $outside rows from outside the window"
                    }
                    if (read.corpusCovered && answered.isEmpty()) {
                        problems += "${read.name}/${range.label}: found nothing over real data"
                    }
                    // The windows nest, so their answers must too. A month with fewer rows than
                    // its week lost records to a window edge.
                    if (answered.size < narrower) {
                        problems += "${read.name}/${range.label}: returned ${answered.size}, " +
                            "fewer than the narrower range's $narrower"
                    }
                    narrower = answered.size
                }
            }

            assertWithMessage("ranged reads broke their guarantees").that(problems).isEmpty()
        }

    @Test
    fun `an empty day is empty-but-valid, never a throw`() = runTest(timeout = 5.minutes) {
        // "No data" is an answer, not a failure.
        val readers = readers(seeded())
        val empty = corpusFirstDay().minusDays(400)
        val (start, end) = TimeRange.DAY.window(empty)

        for (read in reads) {
            val answered = read.read(readers, start, end)
            if (read.gapFilled) {
                assertWithMessage("${read.name} owes one row per date even over an empty day")
                    .that(answered)
                    .hasSize(1)
            } else {
                assertWithMessage("${read.name} invented rows for a day with no records")
                    .that(answered)
                    .isEmpty()
            }
        }
    }

    /** The window a range covers, ending at the end of [anchor]. */
    private fun TimeRange.window(anchor: LocalDate): Pair<Instant, Instant> {
        val end = anchor.plusDays(1).atStartOfDay(ZONE).toInstant()
        val start = anchor.minusDays(days - 1L).atStartOfDay(ZONE).toInstant()
        return start to end
    }

    private fun Instant.date(): LocalDate = atZone(ZONE).toLocalDate()

    /** The last date a half-open window actually covers. */
    private fun Instant.lastDate(): LocalDate = minusMillis(1).atZone(ZONE).toLocalDate()

    /** Midday, so a date compares inside its own day whatever the offset. */
    private fun LocalDate.midday(): Instant = atStartOfDay(ZONE).plusHours(12).toInstant()

    /** The last day the corpus has a workout on — see the class doc on `today`. */
    private fun corpusLastDay(): LocalDate = HcFixture.exercise().maxOf { it.startTime }.date()

    private fun corpusFirstDay(): LocalDate = HcFixture.exercise().minOf { it.startTime }.date()

    private fun readers(client: FakeHealthConnectClient) = Readers(support(client))

    /** Seeds the corpus once for the class; 4,370 inserts take seconds and every case only reads. */
    private fun seeded(): FakeHealthConnectClient = corpus


    private fun support(client: FakeHealthConnectClient): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        val aggregating = AggregatingFakeHealthConnectClient(client)
        return HealthConnectReaderSupport(
            clientProvider = { aggregating },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
    }

    private companion object {
        const val APP_PACKAGE = "tech.mmarca.openvitals"

        /**
         * The zone the corpus was recorded in. The fake keeps only interval records wholly inside
         * the window, and the overnight sessions only fit a day drawn on the corpus's own clock.
         */
        val ZONE: ZoneId = HcFixture.exercise().first().startZoneOffset
            ?: error("the corpus's exercise records carry no zone offset")

        val corpus: FakeHealthConnectClient by lazy {
            runBlocking {
                FakeHealthConnectClient().also { client ->
                    HcFixture.allRecords()
                        .groupBy { it.metadata.dataOrigin.packageName }
                        .forEach { (writer, records) ->
                            client.setPackageName(writer)
                            client.insertRecords(records)
                        }
                }
            }
        }
    }
}
