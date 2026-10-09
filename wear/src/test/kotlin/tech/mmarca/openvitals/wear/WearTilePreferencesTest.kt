package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.wear.WearMetric.BLOOD_OXYGEN
import tech.mmarca.openvitals.wear.WearMetric.DISTANCE
import tech.mmarca.openvitals.wear.WearMetric.FLOORS
import tech.mmarca.openvitals.wear.WearMetric.HEART_RATE
import tech.mmarca.openvitals.wear.WearMetric.STEPS

class WearTilePreferencesTest {

    @Test
    fun decode_nothingStored_returnsDefaults() {
        assertEquals(WearMetric.DefaultTiles, decodeTiles(null))
    }

    @Test
    fun decode_emptyString_returnsNoTiles() {
        assertEquals(emptyList<WearMetric>(), decodeTiles(""))
    }

    @Test
    fun encodeThenDecode_keepsSelectionAndOrder() {
        val custom = listOf(HEART_RATE, STEPS, FLOORS)
        assertEquals(custom, decodeTiles(encodeTiles(custom)))
    }

    @Test
    fun decode_dropsUnknownAndDuplicateNames() {
        assertEquals(listOf(STEPS, HEART_RATE), decodeTiles("STEPS,NOPE,HEART_RATE,STEPS"))
    }

    @Test
    fun move_down_placesTileAfterTarget() {
        assertEquals(
            listOf(HEART_RATE, DISTANCE, STEPS, FLOORS),
            listOf(STEPS, HEART_RATE, DISTANCE, FLOORS).withTileMoved(STEPS, DISTANCE),
        )
    }

    @Test
    fun move_up_placesTileBeforeTarget() {
        assertEquals(
            listOf(FLOORS, STEPS, HEART_RATE, DISTANCE),
            listOf(STEPS, HEART_RATE, DISTANCE, FLOORS).withTileMoved(FLOORS, STEPS),
        )
    }

    @Test
    fun move_skipsTilesHiddenFromTheEditor() {
        // BLOOD_OXYGEN is chosen but has no sensor, so the editor never shows it.
        // The editor sees HEART_RATE and STEPS as neighbours and swaps them.
        assertEquals(
            listOf(HEART_RATE, STEPS, BLOOD_OXYGEN),
            listOf(STEPS, BLOOD_OXYGEN, HEART_RATE).withTileMoved(HEART_RATE, STEPS),
        )
    }
}
