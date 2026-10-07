package tech.mmarca.openvitals.devices.xiaomi

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences

class XiaomiScaleStoreTest {

    private val prefs = FakeSharedPreferences()
    private val store = XiaomiScaleStore(prefs)
    private val key = "0728974d657a4b60964c1b1677f35f7c".hexToByteArray()

    @Test
    fun `what the store learned is there after a restart`() {
        store.setUp("8c:d0:b2:f6:be:ef", "Bathroom scale", key)
        store.setProfile(1)
        store.setKeyRejected(true)
        store.setWriteFailure(ScaleWriteFailure.PERMISSION)
        store.noteIgnoredProfile(profile = 2, atMillis = 5_000)
        store.setDeletedScaleTimestamp(1744250605)

        val restarted = XiaomiScaleStore(prefs)

        assertArrayEquals(key, restarted.bindKey())
        assertEquals(
            XiaomiScaleConfig(
                hasKey = true,
                address = "8C:D0:B2:F6:BE:EF",
                name = "Bathroom scale",
                profile = 1,
                keyRejected = true,
                writeFailure = ScaleWriteFailure.PERMISSION,
                ignoredProfile = IgnoredScaleProfile(profile = 2, atMillis = 5_000),
                deletedScaleTimestamp = 1744250605,
            ),
            restarted.config.value,
        )
    }

    @Test
    fun `a new key keeps the scale, since the scale is known by its address`() {
        store.setUp("8C:D0:B2:F6:BE:EF", "Bathroom scale", key)
        store.setProfile(1)
        store.setKeyRejected(true)

        store.changeKey(ByteArray(16) { 1 })

        assertArrayEquals(ByteArray(16) { 1 }, store.bindKey())
        assertEquals(
            XiaomiScaleConfig(hasKey = true, address = "8C:D0:B2:F6:BE:EF", name = "Bathroom scale", profile = 1),
            store.config.value,
        )
    }

    @Test
    fun `adding a scale forgets whatever an earlier one left`() {
        store.setUp("8C:D0:B2:F6:BE:EF", "Old", key)
        store.setProfile(1)
        store.setDeletedScaleTimestamp(5)

        store.setUp("84:46:93:64:A5:E6", "New", ByteArray(16) { 1 })

        assertEquals(XiaomiScaleConfig(hasKey = true, address = "84:46:93:64:A5:E6", name = "New"), store.config.value)
    }

    @Test
    fun `taking over another user slot drops the note that it was ignored`() {
        store.setUp("8C:D0:B2:F6:BE:EF", "Bathroom scale", key)
        store.setProfile(1)
        store.noteIgnoredProfile(profile = 2, atMillis = 5_000)

        store.setProfile(2)

        assertEquals(2, store.config.value.profile)
        assertNull(store.config.value.ignoredProfile)
    }

    @Test
    fun `removing the scale leaves nothing, the key included`() {
        store.setUp("8C:D0:B2:F6:BE:EF", "Bathroom scale", key)
        store.setProfile(1)

        store.clear()

        assertNull(store.bindKey())
        assertEquals(XiaomiScaleConfig(), XiaomiScaleStore(prefs).config.value)
    }

    @Test
    fun `a key is 32 hex digits however the tool printed it`() {
        val accepted = listOf(
            "0728974d657a4b60964c1b1677f35f7c",
            "0728974D657A4B60964C1B1677F35F7C",
            " 0728974d 657a4b60 964c1b16 77f35f7c\n",
            "07:28:97:4d:65:7a:4b:60:96:4c:1b:16:77:f3:5f:7c",
        )
        val refused = listOf(
            "",
            "0728974d657a4b60964c1b1677f35f7",
            "0728974d657a4b60964c1b1677f35f7c00",
            "0728974d657a4b60964c1b1677f35f7g",
            // The 12-byte token the same tools print beside the key.
            "0728974d657a4b60964c1b16",
        )

        assertEquals(accepted.map { key.toList() }, accepted.map { XiaomiScaleStore.parseBindKey(it)?.toList() })
        assertEquals(refused.map { null }, refused.map { XiaomiScaleStore.parseBindKey(it) })
    }
}
