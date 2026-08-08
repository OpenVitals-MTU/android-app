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
 * Kotlin counterpart of Flutter's `test/integration/use_cases_test.dart` — one
 * table rather than one file per read, because they all owe the same
 * guarantees: a day, a week, a month and a year all return over real data; an
 * empty day returns empty-but-VALID rather than throwing; and nothing comes
 * back from outside the window that was asked for.
 *
 * ## Why this sits at the reader layer and not, as in Flutter, at the use case
 *
 * Flutter boots its whole Riverpod graph with only the Health Connect boundary
 * faked, so its table drives the use cases. The Kotlin equivalent cannot: the
 * repositories above these readers need a `Context`, `SharedPreferences` and
 * Room, none of which exist on a bare JVM, and this module has no Robolectric.
 * So the table stops where the Android dependencies start, and the composition
 * above it stays covered by the per-ViewModel tests.
 *
 * That still covers the layer that matters most. Everything below this line is
 * real — the readers, their chunking and windowing, the aggregate bucketing and
 * every mapper — and it is where the window bugs actually live: the clipped
 * DST tail bucket that blanked a day was in exactly this code, invisible to
 * anything that mocked a reader out.
 *
 * `today` is pinned to the corpus's own last day, so that a week or a year
 * window looks BACKWARDS into real records rather than forwards into an empty
 * present. Without that, every range case would pass over no data at all.
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
     * The reads return a dozen unrelated model types; what every one of them
     * owes is the same, and all of it is expressible in terms of when the
     * things it returned happened.
     *
     * The two flags are statements of fact about the corpus and the read, not
     * preferences — they are what makes an empty answer either a pass or a
     * failure, and both are pinned so a read that quietly stops returning
     * anything fails here instead of passing as a thin corpus.
     *
     * @param corpusCovered the fixture holds records this read can find. The
     *   fixture is an activity/heart/sleep export: it carries no resting heart
     *   rate, HRV, nutrition, mindfulness, body or cycle records at all.
     * @param gapFilled the read emits one row per date in the range whether or
     *   not that date had data, so even an empty window answers non-empty.
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
            // One walk of the table asserting all three guarantees, rather than
            // three walks: the corpus is 4,370 records and a walk is not cheap.
            // Violations are collected instead of thrown so one broken read
            // reports every range it broke, not just the first.
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
                    // The windows nest, so their answers must too. A read whose
                    // month returns fewer rows than its week has lost records to
                    // a window edge — which is how the clipped DST bucket first
                    // showed itself.
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
        // "No data" is an answer, not a failure — every screen branches on the
        // difference. A day well before the corpus begins has nothing in it.
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

    /**
     * Seeds the corpus one writer at a time; see `FixtureReaderTest.seeded`.
     *
     * Seeded once for the whole class rather than per test: 4,370 records is
     * several seconds of inserts, every case here only reads, and four
     * identical corpora prove nothing a shared one does not.
     */
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
         * The zone the corpus was RECORDED in, not the machine's. The fake keeps
         * only interval records wholly inside an instant window, and the corpus's
         * overnight sleep sessions only fit wholly inside a day drawn on the
         * corpus's own clock — a UTC machine's day window starts 43 minutes into
         * the anchor night's session and the read comes back empty.
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
