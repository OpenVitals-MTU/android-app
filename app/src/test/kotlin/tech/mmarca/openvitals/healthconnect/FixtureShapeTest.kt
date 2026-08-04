package tech.mmarca.openvitals.healthconnect

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Asserts the fixture still has the SHAPES the tests depend on.
 *
 * Not that the JSON parses — that a `>12 h` heart-rate record still exists and still
 * swallows a workout; that more than one app still wrote sleep on the same night;
 * that the GPS route still has enough points to compute a split from.
 *
 * Without this, the fixture can be regenerated, re-sliced, or hand-edited into
 * something that still loads, still passes every other test, and no longer contains
 * a single one of the bugs the suite exists to catch. Every test above it would stay
 * green while testing nothing at all. That is a worse position than having no
 * fixture, because it looks like coverage.
 *
 * The record-level halves of this guard (the swallowing record, the naive windowed
 * read, the provenance fields) are asserted through the real reader in
 * [FixtureReaderTest]; what is left here is the raw-JSON shape the reader never
 * looks at.
 */
class FixtureShapeTest {

    private val fixture: JsonObject by lazy {
        val stream = FixtureShapeTest::class.java.classLoader!!
            .getResourceAsStream("golden.json")
            ?: error("golden.json is not on the test classpath.")
        Gson().fromJson(stream.reader(), JsonObject::class.java)
    }

    private fun records(key: String): List<JsonObject> =
        fixture[key].asJsonArray.map { it.asJsonObject }

    @Test
    fun `more than one app wrote sleep on the same night`() {
        // Multi-writer sleep is what exercises the merge; multi-writer anything is
        // what exercises dedup. It cannot be invented by hand — this is the shape
        // real data has and synthetic data never does.
        val byNight = records("sleep").groupBy(
            keySelector = {
                Instant.ofEpochMilli(it["start"].asLong).atOffset(ZoneOffset.UTC).toLocalDate()
            },
            valueTransform = { it["writer"].asString },
        ).mapValues { (_, writers) -> writers.toSet() }

        assertTrue(
            "No night has sleep from two different writers, so the merge path is " +
                "never exercised.",
            byNight.values.any { it.size > 1 },
        )
    }

    @Test
    fun `a GPS route with enough points to compute splits from`() {
        val withRoute = records("exercise").filter { it["route"].asJsonArray.size() > 500 }

        assertTrue(
            "No exercise session has a substantial GPS route, so distance, pace and " +
                "the 1 km splits are never computed from real geometry.",
            withRoute.isNotEmpty(),
        )
    }

    @Test
    fun `the sibling records that a session does NOT carry are present`() {
        // The walking-activity bug. A Health Connect ExerciseSessionRecord carries
        // almost nothing — a watch writes the walk as a session with a duration, and
        // puts its steps, distance and calories in SEPARATE records over the same
        // window. Reading the session alone reported "Not available" for numbers the
        // watch had recorded, directly above a chart of that same activity's step
        // cadence. Without these siblings in the fixture, the fix is untestable.
        listOf("steps", "distance", "activeCalories").forEach { key ->
            assertTrue("No $key sibling records.", records(key).isNotEmpty())
        }
        // And the calorie chain's second branch (active + BMR pro-rated) is
        // unreachable without a BMR record to pro-rate.
        assertTrue(records("basalMetabolicRate").isNotEmpty())
    }

    @Test
    fun `speed is a SERIES record, so splits hit the same bug as heart rate`() {
        // Same shape, same trap: Health Connect filters SpeedRecord by the record's
        // own boundary too, which is why the 1 km splits silently fell back to
        // "estimated" on exactly the activities whose heart rate had vanished. A
        // speed record with no samples proves nothing.
        val speed = records("speed")
        assertTrue(speed.isNotEmpty())
        assertTrue(
            "The speed record has almost no samples, so no split can be computed " +
                "from it.",
            speed.first()["dt"].asJsonArray.size() > 10,
        )
    }

    @Test
    fun `the synthetic records are exactly the two we could not derive`() {
        // The export contains ZERO PowerRecords and ZERO CyclingPedalingCadenceRecords
        // — this person has no power meter. Those two are hand-authored so the power
        // fix has something to be tested against.
        //
        // Everything else must inherit its shape from real data. If a `synthetic`
        // flag ever appears on a third record type, someone has quietly started
        // inventing the thing the fixture exists to preserve.
        val synthetic = fixture.entrySet()
            .filter { (key, value) -> key != "manifest" && value is JsonArray }
            .filter { (_, value) ->
                value.asJsonArray.any { element ->
                    element.asJsonObject["synthetic"]?.takeIf { !it.isJsonNull }?.asBoolean == true
                }
            }
            .map { it.key }
            .toSet()

        assertEquals(
            "The set of INVENTED record types has changed. Every other record here " +
                "derives its shape from real data — that is the whole point.",
            setOf("power", "cyclingCadence"),
            synthetic,
        )
        assertTrue(
            "No power record, so the power read has nothing to prove itself against.",
            records("power").isNotEmpty(),
        )
    }
}
