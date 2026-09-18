package tech.mmarca.openvitals.healthconnect

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The packed index must answer exactly as the `Map<String, String>` it replaced. */
class SyncedOriginIndexTest {
    private fun id(n: Long): String = "sync_%016x%016x".format(n * 0x9E3779B97F4A7C15uL.toLong(), n)

    @Test
    fun `every stored id comes back, across several resizes`() {
        val index = SyncedOriginIndex()
        val packages = listOf("com.gadgetbridge", "com.polar", "com.fitbit.FitbitMobile")

        repeat(20_000) { n -> index.put(id(n.toLong()), packages[n % 3]) }

        assertThat(index.size).isEqualTo(20_000)
        repeat(20_000) { n -> assertThat(index[id(n.toLong())]).isEqualTo(packages[n % 3]) }
        assertThat(index[id(20_001)]).isNull()
    }

    @Test
    fun `a later origin replaces the earlier one without a second row`() {
        val index = SyncedOriginIndex()

        index.put(id(1), "com.gadgetbridge")
        index.put(id(1), "com.polar")

        assertThat(index[id(1)]).isEqualTo("com.polar")
        assertThat(index.size).isEqualTo(1)
    }

    @Test
    fun `ids with the sign bit set or all zeros are ordinary ids`() {
        val index = SyncedOriginIndex()
        val zeros = "sync_" + "0".repeat(32)
        val ones = "sync_" + "f".repeat(32)

        index.put(zeros, "com.zero")
        index.put(ones, "com.ones")

        assertThat(index[zeros]).isEqualTo("com.zero")
        assertThat(index[ones]).isEqualTo("com.ones")
        assertThat(index["sync_" + "0".repeat(31) + "1"]).isNull()
    }

    @Test
    fun `an id that does not pack is still kept, exactly`() {
        val index = SyncedOriginIndex()
        val upper = "sync_" + "A".repeat(32)

        index.putAll(mapOf("sync_abc" to "com.short", upper to "com.upper", "openvitals_manual_1" to "com.other"))

        assertThat(index["sync_abc"]).isEqualTo("com.short")
        assertThat(index[upper]).isEqualTo("com.upper")
        // Case matters: the lowercase twin is a different id.
        assertThat(index["sync_" + "a".repeat(32)]).isNull()
        assertThat(index.size).isEqualTo(3)
    }

    @Test
    fun `a pre-sized index takes its rows without growing wrong`() {
        val index = SyncedOriginIndex(expectedRows = 1_000)

        repeat(1_000) { n -> index.put(id(n.toLong()), "com.gadgetbridge") }

        assertThat(index.size).isEqualTo(1_000)
        assertThat(index[id(999)]).isEqualTo("com.gadgetbridge")
    }
}
