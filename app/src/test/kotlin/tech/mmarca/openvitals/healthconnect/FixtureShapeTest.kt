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
 * Asserts the fixture still has the shapes the tests depend on: a swallowing heart-rate
 * record, multi-writer sleep, a route long enough to split. Without this the fixture
 * could be regenerated into something that loads and tests nothing.
 * The record-level halves are asserted in [FixtureReaderTest]; this is the raw-JSON shape.
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
        // Multi-writer sleep exercises the merge; multi-writer anything exercises dedup.
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
        // A watch writes a walk as a session and puts steps, distance and calories in separate
        // records. Without these siblings the walking-activity fix is untestable.
        listOf("steps", "distance", "activeCalories").forEach { key ->
            assertTrue("No $key sibling records.", records(key).isNotEmpty())
        }
        // The calorie chain's second branch needs a BMR record to pro-rate.
        assertTrue(records("basalMetabolicRate").isNotEmpty())
    }

    @Test
    fun `speed is a SERIES record, so splits hit the same bug as heart rate`() {
        // Health Connect filters SpeedRecord by the record's boundary too. A speed record with no samples proves nothing.
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
        // The export has no PowerRecords or CyclingPedalingCadenceRecords; those two are hand-authored.
        // Everything else must inherit its shape from real data.
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
